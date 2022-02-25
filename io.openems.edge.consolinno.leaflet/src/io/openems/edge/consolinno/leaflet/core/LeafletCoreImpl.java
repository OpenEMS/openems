package io.openems.edge.consolinno.leaflet.core;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.consolinno.leaflet.core.api.Error;
import io.openems.edge.consolinno.leaflet.core.api.LeafletCore;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.ConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configurator for Consolinno Modbus modules. Reads the CSV Register source file, sets the general Modbus Protocol and configures
 * module specific values (e.g Relay inversion status).
 * This must be configured, otherwise Leaflet-Modules/Devices within OpenEMS won't work.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Consolinno.Leaflet.Configurator", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)

public class LeafletCoreImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, LeafletCore, EventHandler, ModbusComponent {

    private List<List<String>> source;
    private final Map<ModuleRegister, Integer> discreteOutputCoils = new HashMap<>();
    private final Map<ModuleRegister, Integer> discreteInputContacts = new HashMap<>();
    private final Map<ModuleRegister, Integer> analogInputRegisters = new HashMap<>();
    private final Map<ModuleRegister, Integer> analogOutputHoldingRegisters = new HashMap<>();
    private final Map<String, PinOwner> ownerMap = new HashMap<>();
    private final Map<ModuleType, PositionMap> positionMap = new HashMap<>();
    private static final int HEADER_INFORMATION_OFFSET = 1;
    private static final int GROUP_SIZE = 3;
    private static final int REGISTER_TYPE_COUNT = 4;
    private static final ModuleRegister LEAFLET_AIO_CONNECTION_STATUS = new ModuleRegister(ModuleType.LEAFLET, 0, 11);
    private static final ModuleRegister LEAFLET_TMP_CONNECTION_STATUS = new ModuleRegister(ModuleType.LEAFLET, 0, 10);
    private static final ModuleRegister LEAFLET_REL_CONNECTION_STATUS = new ModuleRegister(ModuleType.LEAFLET, 0, 9);
    private static final ModuleRegister LEAFLET_PWM_CONNECTION_STATUS = new ModuleRegister(ModuleType.LEAFLET, 0, 8);
    private static final ModuleRegister LEAFLET_CONFIG_REGISTER = new ModuleRegister(ModuleType.LEAFLET, 0, 0);
    private static final String MODULE_TYPE = "Modul Typ";
    private static final String MODULE_NR = "ModulNr";
    private static final String M_REG = "Mreg";
    private final Logger log = LoggerFactory.getLogger(LeafletCoreImpl.class);
    private boolean configFlag;
    private int pwmConfigRegisterOne;
    private int pwmConfigRegisterTwo;
    private int pwmConfigRegisterThree;
    private int pwmConfigRegisterFour;
    private int pwmConfigRegisterFive;
    private int pwmConfigRegisterSix;
    private int pwmConfigRegisterSeven;
    private int pwmConfigRegisterEight;
    // AIO is still in development and doesn't exists for now
    private int aioConfigOne = 0;
    private int aioConfigTwo = 0;
    private int aioConfigThree = 0;
    private int aioConfigFour = 0;
    private int aioConfigFive = 0;
    private int aioConfigSix = 0;
    private int aioConfigSeven = 0;
    private int aioConfigRegisterOne;
    private int aioConfigRegisterTwo;
    private int aioConfigRegisterThree;
    private int aioConfigRegisterFour;
    private int aioConfigRegisterFive;
    private int aioConfigRegisterSix;
    private int aioConfigRegisterSeven;
    //leafletModule_x are Binary Representations of the Decimal ModuleNumber
    private static final int LEAFLET_MODULE_ONE = 1;
    private static final int LEAFLET_MODULE_TWO = 2;
    private static final int LEAFLET_MODULE_THREE = 4;
    private static final int LEAFLET_MODULE_FOUR = 8;
    private static final int LEAFLET_MODULE_FIVE = 16;
    private static final int LEAFLET_MODULE_SIX = 32;
    private static final int LEAFLET_MODULE_SEVEN = 64;
    private static final int LEAFLET_MODULE_EIGHT = 128;
    private int moduleTypeOffset;
    private int moduleNumberOffset;
    private int mRegOffset;
    //True if we are compatible
    private boolean compatible;
    //True if the compatibility Check was done once
    private boolean compatibleFlag;


    public LeafletCoreImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ModbusComponent.ChannelId.values(),
                LeafletCore.ChannelId.values());
    }

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    @Reference
    protected ConfigurationAdmin cm;

    protected SourceReader sourceReader = new SourceReader();

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsException, ConfigurationException {
        //Reads Source file CSV with the Register information
        this.source = this.sourceReader.readCsv(config.source());
        if (this.source.size() == 1) {
            throw new ConfigurationException("The Source file could not be found! Check Config!");
        }
        if (this.checkFirmwareCompatibility()) {
            //Splits the big CSV Output into the different Modbus Types(OutputCoil,...)
            this.splitArrayIntoType();
            //Sets the Register variables for the Configuration
            this.setPwmConfigurationAddresses();
            this.setAioConfigurationAddresses();
            super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                    "Modbus", config.modbusBridgeId());
        } else {
                this.log.error("Firmware incompatible or not Running!");
            this.log.info("The Configurator will now deactivate itself.");
            this.deactivate();
        }
    }

    /**
     * Splits the Source file of all modules into the correct type (Discrete Output Coil, Discrete Input Contact,
     * Analog Input Register , Analog Output Holding Register).
     */
    private void splitArrayIntoType() {
        this.getSourceHeaderOrder();
        AtomicInteger currentGroup = new AtomicInteger(0);
        //Modbus can address 4 different types of Registers. So the for loop sorts the values into those 4.
        for (int group = 0; group <= REGISTER_TYPE_COUNT; group++) {
            this.source.forEach(row -> {
                if (!(row.get(0).equals("") || row.get(0).equals("Modbus Offset") || row.toString().contains("Register") || row.toString().contains("Version"))) {
                    if (currentGroup.get() < REGISTER_TYPE_COUNT && !this.checkForLastGroupMember(row, currentGroup.get())) {
                        switch (currentGroup.get()) {
                            case (0): {
                                this.putElementInCorrectMap(this.discreteOutputCoils, currentGroup.get(), row);
                            }
                            break;
                            case (1): {
                                this.putElementInCorrectMap(this.discreteInputContacts, currentGroup.get(), row);
                            }
                            break;
                            case (2): {
                                this.putElementInCorrectMap(this.analogInputRegisters, currentGroup.get(), row);
                            }
                            break;
                            case (3): {
                                this.putElementInCorrectMap(this.analogOutputHoldingRegisters, currentGroup.get(), row);
                            }
                            break;
                        }
                    }
                }
            });
            currentGroup.getAndIncrement();
        }
    }

    /**
     * Help method for the above Switch case.
     *
     * @param map   The Map this Element has to be put in (DiscreteOutputCoil etc.)
     * @param group The Group number of the above map (0 for DiscreteOutputCoil,etc.)
     * @param row   The current Row of the Big Output list
     */
    private void putElementInCorrectMap(Map<ModuleRegister, Integer> map, int group, List<String> row) {
        map.put(new ModuleRegister(this.stringToType(
                row.get(this.moduleTypeOffset + (group * GROUP_SIZE))),
                        Integer.parseInt(row.get(this.moduleNumberOffset + (group * GROUP_SIZE))),
                        Integer.parseInt(row.get(this.mRegOffset + (group * GROUP_SIZE)))),
                Integer.parseInt(row.get(0)));
    }

    /**
     * Searches through the Big Source file and writes in the appropriate variable which column contains the types.
     */
    private void getSourceHeaderOrder() {
        //The Csv source file will hold the header either in line 1 or 2
        for (int n = 0; n < 2; n++) {
            for (int i = HEADER_INFORMATION_OFFSET; i <= GROUP_SIZE; i++) {
                String current = (this.source.get(n).get(i));
                if (this.moduleNumberOffset == 0 && current.contains(MODULE_NR)) {
                    this.moduleNumberOffset = i;
                } else if (this.moduleTypeOffset == 0 && current.contains(MODULE_TYPE)) {
                    this.moduleTypeOffset = i;
                } else if (this.mRegOffset == 0 && current.contains(M_REG)) {
                    this.mRegOffset = i;
                }
            }
        }
    }

    /**
     * Simple Check if the current Group doesn't have more members.
     *
     * @param row   The line of the big csvOutput List
     * @param group The number of the Group (DiscreteOutputCoil=0,DiscreteInputContact=1,...)
     * @return true if this is part of the Header
     */
    private boolean checkForLastGroupMember(List<String> row, int group) {
        //Checks with +1 if the next member exists
        return (row.get(group * GROUP_SIZE + 1).equals("") || row.get(group * GROUP_SIZE + 1).equals("0"));
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    /**
     * Converts a String into a ModuleType.
     *
     * @param cast The String that has to be converted
     * @return ModuleType (ERROR if the String is not a ModuleType)
     */
    private ModuleType stringToType(String cast) {
        if (ModuleType.contains(cast.toUpperCase().trim())) {
            return ModuleType.valueOf(cast.toUpperCase().trim());
        } else {
            return ModuleType.ERROR;
        }
    }

    /**
     * Gets the Modules which are present over modbus.
     * Writes Configuration Registers.
     *
     * @return Writes the Information over the present Modules
     */

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        if (checkFirmwareCompatibility() == false) {
            this.log.error("Incompatible Firmware Version. Please Update the Firmware.");
            this.deactivate();
            return null;
        } else {
            return new ModbusProtocol(this,
                    //Read Module Connection Status
                    new FC4ReadInputRegistersTask(this.analogInputRegisters.get(LEAFLET_TMP_CONNECTION_STATUS), Priority.HIGH,
                            m(LeafletCore.ChannelId.TEMPERATURE_MODULES, new UnsignedWordElement(
                                            this.analogInputRegisters.get(LEAFLET_TMP_CONNECTION_STATUS)),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC4ReadInputRegistersTask(this.analogInputRegisters.get(LEAFLET_REL_CONNECTION_STATUS), Priority.HIGH,
                            m(LeafletCore.ChannelId.RELAY_MODULES, new UnsignedWordElement(
                                            this.analogInputRegisters.get(LEAFLET_REL_CONNECTION_STATUS)),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC4ReadInputRegistersTask(this.analogInputRegisters.get(LEAFLET_PWM_CONNECTION_STATUS), Priority.HIGH,
                            m(LeafletCore.ChannelId.PWM_MODULES, new UnsignedWordElement(
                                            this.analogInputRegisters.get(LEAFLET_PWM_CONNECTION_STATUS)),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC4ReadInputRegistersTask(this.analogInputRegisters.get(LEAFLET_AIO_CONNECTION_STATUS), Priority.HIGH,
                            m(LeafletCore.ChannelId.AIO_MODULES, new UnsignedWordElement(
                                            this.analogInputRegisters.get(LEAFLET_AIO_CONNECTION_STATUS)),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),

                    new FC4ReadInputRegistersTask(this.analogOutputHoldingRegisters.get(LEAFLET_CONFIG_REGISTER), Priority.HIGH,
                            m(LeafletCore.ChannelId.READ_LEAFLET_CONFIG, new UnsignedWordElement(
                                            this.analogOutputHoldingRegisters.get(LEAFLET_CONFIG_REGISTER)),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC6WriteRegisterTask(this.analogOutputHoldingRegisters.get(LEAFLET_CONFIG_REGISTER),
                            m(LeafletCore.ChannelId.WRITE_LEAFLET_CONFIG,
                                    new UnsignedWordElement(this.analogOutputHoldingRegisters.get(LEAFLET_CONFIG_REGISTER)),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    //PWM Frequency Configuration
                    new FC6WriteRegisterTask(this.pwmConfigRegisterOne,
                            m(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_ONE,
                                    new SignedWordElement(this.pwmConfigRegisterOne),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC6WriteRegisterTask(this.pwmConfigRegisterTwo,
                            m(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_TWO,
                                    new UnsignedWordElement(this.pwmConfigRegisterTwo),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC6WriteRegisterTask(this.pwmConfigRegisterThree,
                            m(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_THREE,
                                    new UnsignedWordElement(this.pwmConfigRegisterThree),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC6WriteRegisterTask(this.pwmConfigRegisterFour,
                            m(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_FOUR,
                                    new UnsignedWordElement(this.pwmConfigRegisterFour),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC6WriteRegisterTask(this.pwmConfigRegisterFive,
                            m(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_FIVE,
                                    new SignedWordElement(this.pwmConfigRegisterFive),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC6WriteRegisterTask(this.pwmConfigRegisterSix,
                            m(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_SIX,
                                    new UnsignedWordElement(this.pwmConfigRegisterSix),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC6WriteRegisterTask(this.pwmConfigRegisterSeven,
                            m(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_SEVEN,
                                    new UnsignedWordElement(this.pwmConfigRegisterSeven),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC6WriteRegisterTask(this.pwmConfigRegisterEight,
                            m(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_EIGHT,
                                    new UnsignedWordElement(this.pwmConfigRegisterEight),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    //AIO Configuration
                    new FC6WriteRegisterTask(this.aioConfigRegisterOne,
                            m(LeafletCore.ChannelId.WRITE_AIO_CONFIG_ONE,
                                    new SignedWordElement(this.aioConfigRegisterOne),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC6WriteRegisterTask(this.aioConfigRegisterTwo,
                            m(LeafletCore.ChannelId.WRITE_AIO_CONFIG_TWO,
                                    new UnsignedWordElement(this.aioConfigRegisterTwo),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC6WriteRegisterTask(this.aioConfigRegisterThree,
                            m(LeafletCore.ChannelId.WRITE_AIO_CONFIG_THREE,
                                    new UnsignedWordElement(this.aioConfigRegisterThree),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC6WriteRegisterTask(this.aioConfigRegisterFour,
                            m(LeafletCore.ChannelId.WRITE_AIO_CONFIG_FOUR,
                                    new UnsignedWordElement(this.aioConfigRegisterFour),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC6WriteRegisterTask(this.aioConfigRegisterFive,
                            m(LeafletCore.ChannelId.WRITE_AIO_CONFIG_FIVE,
                                    new SignedWordElement(this.aioConfigRegisterFive),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC6WriteRegisterTask(this.aioConfigRegisterSix,
                            m(LeafletCore.ChannelId.WRITE_AIO_CONFIG_SIX,
                                    new UnsignedWordElement(this.aioConfigRegisterSix),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC6WriteRegisterTask(this.aioConfigRegisterSeven,
                            m(LeafletCore.ChannelId.WRITE_AIO_CONFIG_SEVEN,
                                    new UnsignedWordElement(this.aioConfigRegisterSeven),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),

                    new FC3ReadRegistersTask(this.pwmConfigRegisterOne, Priority.LOW,
                            m(LeafletCore.ChannelId.READ_PWM_FREQUENCY_ONE,
                                    new SignedWordElement(this.pwmConfigRegisterOne),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC3ReadRegistersTask(this.pwmConfigRegisterTwo, Priority.LOW,
                            m(LeafletCore.ChannelId.READ_PWM_FREQUENCY_TWO,
                                    new UnsignedWordElement(this.pwmConfigRegisterTwo),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC3ReadRegistersTask(this.pwmConfigRegisterThree, Priority.LOW,
                            m(LeafletCore.ChannelId.READ_PWM_FREQUENCY_THREE,
                                    new UnsignedWordElement(this.pwmConfigRegisterThree),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC3ReadRegistersTask(this.pwmConfigRegisterFour, Priority.LOW,
                            m(LeafletCore.ChannelId.READ_PWM_FREQUENCY_FOUR,
                                    new UnsignedWordElement(this.pwmConfigRegisterFour),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC3ReadRegistersTask(this.pwmConfigRegisterFive, Priority.LOW,
                            m(LeafletCore.ChannelId.READ_PWM_FREQUENCY_FIVE,
                                    new SignedWordElement(this.pwmConfigRegisterFive),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC3ReadRegistersTask(this.pwmConfigRegisterSix, Priority.LOW,
                            m(LeafletCore.ChannelId.READ_PWM_FREQUENCY_SIX,
                                    new UnsignedWordElement(this.pwmConfigRegisterSix),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC3ReadRegistersTask(this.pwmConfigRegisterSeven, Priority.LOW,
                            m(LeafletCore.ChannelId.READ_PWM_FREQUENCY_SEVEN,
                                    new UnsignedWordElement(this.pwmConfigRegisterSeven),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC3ReadRegistersTask(this.pwmConfigRegisterEight, Priority.LOW,
                            m(LeafletCore.ChannelId.READ_PWM_FREQUENCY_EIGHT,
                                    new UnsignedWordElement(this.pwmConfigRegisterEight),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),

                    new FC3ReadRegistersTask(this.aioConfigRegisterOne, Priority.LOW,
                            m(LeafletCore.ChannelId.READ_AIO_CONFIG_ONE,
                                    new SignedWordElement(this.aioConfigRegisterOne),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC3ReadRegistersTask(this.aioConfigRegisterTwo, Priority.LOW,
                            m(LeafletCore.ChannelId.READ_AIO_CONFIG_TWO,
                                    new UnsignedWordElement(this.aioConfigRegisterTwo),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC3ReadRegistersTask(this.aioConfigRegisterThree, Priority.LOW,
                            m(LeafletCore.ChannelId.READ_AIO_CONFIG_THREE,
                                    new UnsignedWordElement(this.aioConfigRegisterThree),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC3ReadRegistersTask(this.aioConfigRegisterFour, Priority.LOW,
                            m(LeafletCore.ChannelId.READ_AIO_CONFIG_FOUR,
                                    new UnsignedWordElement(this.aioConfigRegisterFour),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC3ReadRegistersTask(this.aioConfigRegisterFive, Priority.LOW,
                            m(LeafletCore.ChannelId.READ_AIO_CONFIG_FIVE,
                                    new SignedWordElement(this.aioConfigRegisterFive),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC3ReadRegistersTask(this.aioConfigRegisterSix, Priority.LOW,
                            m(LeafletCore.ChannelId.READ_AIO_CONFIG_SIX,
                                    new UnsignedWordElement(this.aioConfigRegisterSix),
                                    ElementToChannelConverter.DIRECT_1_TO_1)),
                    new FC3ReadRegistersTask(this.aioConfigRegisterSeven, Priority.LOW,
                            m(LeafletCore.ChannelId.READ_AIO_CONFIG_SEVEN,
                                    new UnsignedWordElement(this.aioConfigRegisterSeven),
                                    ElementToChannelConverter.DIRECT_1_TO_1))
            );

        }
    }

    /**
     * Checks if the Firmware version is at least the minimum required version for the Configurator to run properly.
     *
     * @return true if the Firmware is compatible
     */
    public boolean checkFirmwareCompatibility() {
        if (!this.compatible && !this.compatibleFlag) {
            String response = "";
            try {
                Process p = Runtime.getRuntime().exec("LeafletBaseSoftware -v");


                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(p.getInputStream()));

                String line = "";
                StringBuilder responseBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                response = responseBuilder.toString();
            } catch (IOException e) {
                this.log.error("The Firmware is not Running!");
            }
            if (response.equals("") == false) {
                String[] partOne = response.split("V");
                String[] partTwo = partOne[1].split(" ");
                if (MinimumBaseSoftwareVersion.VERSION.getValue() <= Integer.parseInt(partTwo[0].replace(".", ""))) {
                    this.compatible = true;
                }
            }
            this.compatibleFlag = true;
        }
        return this.compatible;
    }


    @Override
    public String debugLog() {
        return "LeafletConfigurator: Found Temp: " + Integer.toBinaryString(this.getTemperatureModules())
                + " Found Relay: " + Integer.toBinaryString(this.getRelayModules()) + " Found PWM: " + Integer.toBinaryString(this.getPwmModules())
                + " PWM Frequency : " + this.getReadPwmFrequency() + " Found AIO: " + Integer.toBinaryString(this.getAioModules());
    }

    /**
     * Check if the Module that is trying to activate is physically present.
     *
     * @param moduleType   TMP,RELAY,PWM
     * @param moduleNumber Internal Number of the module
     * @param position     Pin position of the Module
     * @param id           Unique Id of the Device
     * @return boolean true if present
     */
    @Override
    public boolean modbusModuleCheckout(ModuleType moduleType, int moduleNumber, int position, String id) {
        switch (moduleType) {
            case TMP:
                switch (moduleNumber) {
                    case 1:
                        //0b001
                        if (((getTemperatureModules() & LEAFLET_MODULE_ONE) == LEAFLET_MODULE_ONE)
                                && this.checkPin(moduleType, moduleNumber, position, id)) {
                            this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;
                    case 2:
                        //0b010
                        if (((getTemperatureModules() & LEAFLET_MODULE_TWO) == LEAFLET_MODULE_TWO)
                                && this.checkPin(moduleType, moduleNumber, position, id)) {
                            this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;
                    case 3:
                        //0b100
                        if (((getTemperatureModules() & LEAFLET_MODULE_THREE) == LEAFLET_MODULE_THREE)
                                && this.checkPin(moduleType, moduleNumber, position, id)) {
                            this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;
                }
                break;
            case REL:
                switch (moduleNumber) {
                    case 1:
                        //0b0001
                        if (((getRelayModules() & LEAFLET_MODULE_ONE) == LEAFLET_MODULE_ONE)
                                && this.checkPin(moduleType, moduleNumber, position, id)) {
                            this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;

                    case 2:
                        //0b0010
                        if (((getRelayModules() & LEAFLET_MODULE_TWO) == LEAFLET_MODULE_TWO)
                                && this.checkPin(moduleType, moduleNumber, position, id)) {
                            this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;

                    case 3:
                        //0b0100
                        if (((getRelayModules() & LEAFLET_MODULE_THREE) == LEAFLET_MODULE_THREE)
                                && this.checkPin(moduleType, moduleNumber, position, id)) {
                            this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;
                    case 4:
                        //0b1000
                        if (((getRelayModules() & LEAFLET_MODULE_FOUR) == LEAFLET_MODULE_FOUR)
                                && this.checkPin(moduleType, moduleNumber, position, id)) {
                            this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;

                }
                break;
            case PWM:
                switch (moduleNumber) {
                    case 1:
                        //0b00000001
                        if (((getPwmModules() & LEAFLET_MODULE_ONE) == LEAFLET_MODULE_ONE)
                                && this.checkPin(moduleType, moduleNumber, position, id)) {
                            this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;

                    case 2:
                        //0b00000010
                        if (((getPwmModules() & LEAFLET_MODULE_TWO) == LEAFLET_MODULE_TWO)
                                && this.checkPin(moduleType, moduleNumber, position, id)) {
                            this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;

                    case 3:
                        //0b00000100
                        if (((getPwmModules() & LEAFLET_MODULE_THREE) == LEAFLET_MODULE_THREE)
                                && this.checkPin(moduleType, moduleNumber, position, id)) {
                            this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;
                    case 4:
                        //0b00001000
                        if (((getPwmModules() & LEAFLET_MODULE_FOUR) == LEAFLET_MODULE_FOUR)
                                && this.checkPin(moduleType, moduleNumber, position, id)) {
                            this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;
                    case 5:
                        //0b00010000
                        if (((getPwmModules() & LEAFLET_MODULE_FIVE) == LEAFLET_MODULE_FIVE)
                                && this.checkPin(moduleType, moduleNumber, position, id)) {
                            this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;

                    case 6:
                        //0b00100000
                        if (((getPwmModules() & LEAFLET_MODULE_SIX) == LEAFLET_MODULE_SIX)
                                && this.checkPin(moduleType, moduleNumber, position, id)) {
                            this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;

                    case 7:
                        //0b01000000
                        if (((getPwmModules() & LEAFLET_MODULE_SEVEN) == LEAFLET_MODULE_SEVEN)
                                && this.checkPin(moduleType, moduleNumber, position, id)) {
                            this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;
                    case 8:
                        //0b10000000
                        if (((getPwmModules() & LEAFLET_MODULE_EIGHT) == LEAFLET_MODULE_EIGHT)
                                && this.checkPin(moduleType, moduleNumber, position, id)) {
                            this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;
                }

                break;

            case AIO:
                switch (moduleNumber) {
                    case 1:
                        //0b00000001
                        if (((getAioModules() & LEAFLET_MODULE_ONE) == LEAFLET_MODULE_ONE)
                                && this.checkPin(moduleType, moduleNumber, position, id)) {
                            this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;

                    case 2:
                        //0b00000010
                        if (((getAioModules() & LEAFLET_MODULE_TWO) == LEAFLET_MODULE_TWO)
                                && this.checkPin(moduleType, moduleNumber, position, id)) {
                            this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;

                    case 3:
                        //0b00000100
                        if (((getAioModules() & LEAFLET_MODULE_THREE) == LEAFLET_MODULE_THREE)
                                && this.checkPin(moduleType, moduleNumber, position, id)) {
                            this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;
                    case 4:
                        //0b00001000
                        if (((getAioModules() & LEAFLET_MODULE_FOUR) == LEAFLET_MODULE_FOUR)
                                && this.checkPin(moduleType, moduleNumber, position, id)) {
                            this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;
                    case 5:
                        //0b00010000
                        if (((getAioModules() & LEAFLET_MODULE_FIVE) == LEAFLET_MODULE_FIVE)
                                && this.checkPin(moduleType, moduleNumber, position, id)) {
                            this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;

                    case 6:
                        //0b00100000
                        if (((getAioModules() & LEAFLET_MODULE_SIX) == LEAFLET_MODULE_SIX)
                                && this.checkPin(moduleType, moduleNumber, position, id)) {
                            this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;

                    case 7:
                        //0b01000000
                        if (((getAioModules() & LEAFLET_MODULE_SEVEN) == LEAFLET_MODULE_SEVEN)
                                && this.checkPin(moduleType, moduleNumber, position, id)) {
                            this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;
                }
                break;
            case LEAFLET: {
                this.log.error("This should never happen. LEAFLET called modbusModuleCheckout");
                break;
            }
            case ERROR: {
                this.log.error("This should never happen. ERROR called modbusModuleCheckout");
                break;
            }

        }
        return false;
    }

    /**
     * Checks if the Pin is already in use by another Modbus device.
     *
     * @param type         Type of the Module (TMP,RELAY,etc.)
     * @param moduleNumber Module number specified on the Device
     * @param position     Position of the device on the module
     * @param id           identifier of the device (e.g. TMP0)
     * @return boolean
     */
    private boolean checkPin(ModuleType type, int moduleNumber, int position, String id) {
        if (!this.positionMap.containsKey(type)) {
            this.positionMap.put(type, new PositionMap(moduleNumber, position));
            PinOwner firstRun = new PinOwner(type, moduleNumber, position);
            this.ownerMap.put(id, firstRun);
            return true;
        } else if (!this.positionMap.get(type).getPositionMap().containsKey(moduleNumber)) {
            List<Integer> initList = new ArrayList<>(position);
            this.positionMap.get(type).getPositionMap().put(moduleNumber, initList);
            PinOwner firstRun = new PinOwner(type, moduleNumber, position);
            this.ownerMap.put(id, firstRun);
            return true;
        } else if (!this.positionMap.get(type).getPositionMap().get(moduleNumber).contains(position)) {
            this.positionMap.get(type).getPositionMap().get(moduleNumber).add(position);
            PinOwner firstRun = new PinOwner(type, moduleNumber, position);
            this.ownerMap.put(id, firstRun);
            return true;
        } else {
            if (this.ownerMap.containsKey(id)) {
                return this.ownerMap.get(id).equals(new PinOwner(type, moduleNumber, position));
            } else {
                return false;
            }
        }
    }

    /**
     * Returns the Address from the Source file which is usually needed for operation.
     *
     * @param moduleType   Type of the module (TMP,RELAY,etc.)
     * @param moduleNumber Module number specified on the Device
     * @param mReg         Usually the Position of the device but sometimes position-1. Check Register map
     * @return Modbus Offset as integer
     */
    @Override
    public int getFunctionAddress(ModuleType moduleType, int moduleNumber, int mReg) {
        switch (moduleType) {
            case TMP:
                return this.analogInputRegisters.get(new ModuleRegister(moduleType, moduleNumber, mReg));
            case REL:
                return this.discreteOutputCoils.get(new ModuleRegister(moduleType, moduleNumber, mReg));
            case PWM:
                return this.analogOutputHoldingRegisters.get(new ModuleRegister(moduleType, moduleNumber, mReg));
            //Error state (0xFFFF) should never happen.
            default:
                return Error.ERROR.getValue();
        }
    }

    /**
     * Return the Address from the Source file which is usually needed for operation.
     * This method is only for the AIO module.
     *
     * @param moduleType   Type of the module (TMP,RELAY,etc.)
     * @param moduleNumber Module number specified on the Device
     * @param position     Pin position of the device on the module
     * @param input        true if the Register needed is an input Register | false if an output
     * @return Modbus Offset as integer
     */
    @Override
    public int getFunctionAddress(ModuleType moduleType, int moduleNumber, int position, boolean input) {
        if (moduleType != ModuleType.AIO) {
            return this.getFunctionAddress(moduleType, moduleNumber, position);
        } else {
            if (input) {
                return this.analogInputRegisters.get(new ModuleRegister(moduleType, moduleNumber, position));
            } else {
                return this.analogOutputHoldingRegisters.get(new ModuleRegister(moduleType, moduleNumber, position));
            }
        }

    }

    /**
     * Returns the Address from the Source file of the configuration register.
     *
     * @param moduleType   Type of the module (TMP,RELAY,etc.)
     * @param moduleNumber Module number specified on the Device
     * @return Configuration Register address for the module
     */
    @Override
    public int getConfigurationAddress(ModuleType moduleType, int moduleNumber) {
        return this.analogOutputHoldingRegisters.get(new ModuleRegister(moduleType, moduleNumber, 0));
    }

    /**
     * Removes Module from internal Position map.
     *
     * @param moduleType   Type of the module (TMP,RELAY,etc.)
     * @param moduleNumber Module number specified on the Device
     * @param position     Pin position of the device on the module
     */
    @Override
    public void removeModule(ModuleType moduleType, int moduleNumber, int position) {
        this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).remove((Integer) position);
    }

    /**
     * Sets the Frequency of a Pwm Module.
     *
     * @param moduleNumber Module Number of the Pwm Module that is getting configured
     * @param frequency    Frequency value (between 24 and 1500hz)
     */
    @Override
    public void setPwmConfiguration(int moduleNumber, int frequency) {
        try {
            switch (moduleNumber) {
                case 1:
                    getWritePwmFrequencyOne().setNextWriteValue(frequency);
                    break;
                case 2:
                    getWritePwmFrequencyTwo().setNextWriteValue(frequency);
                    break;
                case 3:
                    getWritePwmFrequencyThree().setNextWriteValue(frequency);
                    break;
                case 4:
                    getWritePwmFrequencyFour().setNextWriteValue(frequency);
                    break;
                case 5:
                    getWritePwmFrequencyFive().setNextWriteValue(frequency);
                    break;
                case 6:
                    getWritePwmFrequencySix().setNextWriteValue(frequency);
                    break;
                case 7:
                    getWritePwmFrequencySeven().setNextWriteValue(frequency);
                    break;
                case 8:
                    getWritePwmFrequencyEight().setNextWriteValue(frequency);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + moduleNumber);
            }

        } catch (OpenemsError.OpenemsNamedException ignored) {
            this.log.error("Error in setPwmConfiguration");

        }
    }

    /**
     * Returns the Register Address for the Discrete Output needed for the inversion of a Pwm Module.
     *
     * @param pwmModule Module number specified on the Pwm Module
     * @param mReg      Pin Position of the Pwm Device
     * @return Invert Register for the Pwm Device
     */
    @Override
    public int getPwmDiscreteOutputAddress(int pwmModule, int mReg) {
        return this.discreteOutputCoils.get(new ModuleRegister(ModuleType.PWM, pwmModule, mReg));
    }

    /**
     * Configures the AIO Modules.
     *
     * @param moduleNumber Module number specified on the Aio Module
     * @param position     Pin Position of the Aio Device
     * @param config       The configuration, of the specific AIO Output (e.g. 0-20mA_in)
     */
    @Override
    public void setAioConfig(int moduleNumber, int position, String config) {
        int configInt = this.convertAioConfigToInt(config);
        this.enterConfigMode();
        try {
            switch (moduleNumber) {
                case 1:
                    this.aioConfigOne = this.aioConfigOne | (configInt << 4 * (position - 1));
                    getAioConfigOne().setNextWriteValue(this.aioConfigOne);
                    break;
                case 2:
                    this.aioConfigTwo = this.aioConfigTwo | (configInt << 4 * (position - 1));
                    getAioConfigTwo().setNextWriteValue(this.aioConfigTwo);
                    break;
                case 3:
                    this.aioConfigThree = this.aioConfigThree | (configInt << 4 * (position - 1));
                    getAioConfigThree().setNextWriteValue(this.aioConfigThree);
                    break;
                case 4:
                    this.aioConfigFour = this.aioConfigFour | (configInt << 4 * (position - 1));
                    getAioConfigFour().setNextWriteValue(this.aioConfigFour);
                    break;
                case 5:
                    this.aioConfigFive = this.aioConfigFive | (configInt << 4 * (position - 1));
                    getAioConfigFive().setNextWriteValue(this.aioConfigFive);
                    break;
                case 6:
                    this.aioConfigSix = this.aioConfigSix | (configInt << 4 * (position - 1));
                    getAioConfigSix().setNextWriteValue(this.aioConfigSix);
                    break;
                case 7:
                    this.aioConfigSeven = this.aioConfigSeven | (configInt << 4 * (position - 1));
                    getAioConfigSeven().setNextWriteValue(this.aioConfigSeven);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + moduleNumber);
            }

        } catch (OpenemsError.OpenemsNamedException ignored) {
            this.log.error("Error in setAioConfig");
        }
    }

    /**
     * Return the Register Address where the AIO module outputs a percentage value of whatever is configured.
     *
     * @param moduleType   Type of the module (TMP,RELAY,etc.)
     * @param moduleNumber Module number specified on the Device
     * @param mReg         Position of the AIO device
     * @param input        true if Input | false if output
     * @return Address of the Percent conversion Register
     */
    @Override
    public int getAioPercentAddress(ModuleType moduleType, int moduleNumber, int mReg, boolean input) {
        if (moduleType != ModuleType.AIO) {
            return Error.ERROR.getValue();
        } else {
            if (input) {
                return this.analogInputRegisters.get(new ModuleRegister(moduleType, moduleNumber, mReg));
            } else {
                return this.analogOutputHoldingRegisters.get(new ModuleRegister(moduleType, moduleNumber, mReg));
            }
        }

    }

    /**
     * Converts the Readable Type String of the AIO module into the internal Int value.
     * Should only be used for the Aio configuration.
     *
     * @param config String of the configuration set for that AIO (e.g 0-20mA_in)
     * @return Internal int value that has to be written into the Register
     */
    private int convertAioConfigToInt(String config) {
        switch (config) {
            case ("10V_out"):
                return 1;
            case ("10V_in"):
                return 2;
            case ("0-20mA_out"):
                return 3;
            case ("0-20mA_in"):
                return 4;
            case ("4-20mA_out"):
                return 5;
            case ("4-20mA_in"):
                return 6;
            case ("Temp_in"):
                return 7;
            case ("Digital_in"):
                return 8;

        }
        return 0;
    }

    /**
     * Changes the content of the Leaflet Config Register to enter configuration mode.
     * !IMPORTANT NOTE! Config Mode HAS to be exited when the configuration is done to avoid hardware damages.
     */
    private void enterConfigMode() {
        try {
            getWriteLeafletConfigChannel().setNextWriteValue(1337);
            this.configFlag = true;
        } catch (OpenemsError.OpenemsNamedException ignored) {
            this.log.error("Error in enterConfigMode");
        }

    }

    /**
     * Stores the pwmConfig Register in a local variable.
     */
    private void setPwmConfigurationAddresses() {
        this.pwmConfigRegisterOne = this.getConfigurationAddress(ModuleType.PWM, 1);
        this.pwmConfigRegisterTwo = this.getConfigurationAddress(ModuleType.PWM, 2);
        this.pwmConfigRegisterThree = this.getConfigurationAddress(ModuleType.PWM, 3);
        this.pwmConfigRegisterFour = this.getConfigurationAddress(ModuleType.PWM, 4);
        this.pwmConfigRegisterFive = this.getConfigurationAddress(ModuleType.PWM, 5);
        this.pwmConfigRegisterSix = this.getConfigurationAddress(ModuleType.PWM, 6);
        this.pwmConfigRegisterSeven = this.getConfigurationAddress(ModuleType.PWM, 7);
        this.pwmConfigRegisterEight = this.getConfigurationAddress(ModuleType.PWM, 8);
    }

    /**
     * Stores the aioConfig Register in a local variable.
     */
    private void setAioConfigurationAddresses() {
        this.aioConfigRegisterOne = this.getConfigurationAddress(ModuleType.AIO, 1);
        this.aioConfigRegisterTwo = this.getConfigurationAddress(ModuleType.AIO, 2);
        this.aioConfigRegisterThree = this.getConfigurationAddress(ModuleType.AIO, 3);
        this.aioConfigRegisterFour = this.getConfigurationAddress(ModuleType.AIO, 4);
        this.aioConfigRegisterFive = this.getConfigurationAddress(ModuleType.AIO, 5);
        this.aioConfigRegisterSix = this.getConfigurationAddress(ModuleType.AIO, 6);
        this.aioConfigRegisterSeven = this.getConfigurationAddress(ModuleType.AIO, 7);
    }

    /**
     * Checks if we are still in Config mode and the Configuration has already taken place.
     *
     * @param event BEFORE_PROCESS_IMAGE
     */
    @Override
    public void handleEvent(Event event) {
        if (this.configFlag && (getReadAioConfig() == (this.aioConfigOne | this.aioConfigTwo | this.aioConfigThree
                | this.aioConfigFour | this.aioConfigFive | this.aioConfigSix | this.aioConfigSeven))) {
            this.exitConfigMode();
        }

    }

    /**
     * Exits Configuration mode by changing the Config register back.
     */
    private void exitConfigMode() {
        try {
            getWriteLeafletConfigChannel().setNextWriteValue(7331);
            this.configFlag = false;
        } catch (OpenemsError.OpenemsNamedException ignored) {
            this.log.error("Error in exitConfigMode");
        }

    }

}




