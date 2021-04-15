package io.openems.edge.consolinno.modbus.configurator;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.consolinno.modbus.configurator.api.LeafletConfigurator;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configurator for Modbus Modules.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "io.openems.edge.consolinno.modbus.configurator", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)

public class LeafletConfiguratorImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, LeafletConfigurator, EventHandler {

    private List<List<String>> source;
    private final Map<ModuleRegister, Integer> discreteOutputCoils = new HashMap<>();
    private final Map<ModuleRegister, Integer> discreteInputContacts = new HashMap<>();
    private final Map<ModuleRegister, Integer> analogInputRegisters = new HashMap<>();
    private final Map<ModuleRegister, Integer> analogOutputHoldingRegisters = new HashMap<>();
    private final Map<String, PinOwner> ownerMap = new HashMap<>();
    private final Map<ModuleType, PositionMap> positionMap = new HashMap<>();
    private final int[] relayInverseRegisters = new int[4];
    private static final int HEADER_INFORMATION_OFFSET = 1;
    private static final int GROUP_SIZE = 3;
    private static final ModuleRegister LEAFLET_AIO_CONNECTION_STATUS = new ModuleRegister(ModuleType.LEAFLET, 0, 11);
    private static final ModuleRegister LEAFLET_TMP_CONNECTION_STATUS = new ModuleRegister(ModuleType.LEAFLET, 0, 10);
    private static final ModuleRegister LEAFLET_REL_CONNECTION_STATUS = new ModuleRegister(ModuleType.LEAFLET, 0, 9);
    private static final ModuleRegister LEAFLET_PWM_CONNECTION_STATUS = new ModuleRegister(ModuleType.LEAFLET, 0, 8);
    private static final ModuleRegister LEAFLET_CONFIG_REGISTER = new ModuleRegister(ModuleType.LEAFLET, 0, 0);
    private static final String MODULE_TYPE = "Modul Typ";
    private static final String MODULE_NR = "ModulNr";
    private static final String M_REG = "Mreg";
    private final Logger log = LoggerFactory.getLogger(LeafletConfiguratorImpl.class);
    private boolean configFlag;
    private int relayOneInvertStatus;
    private int relayTwoInvertStatus;
    private int relayThreeInvertStatus;
    private int relayFourInvertStatus;
    private int pwmConfigRegisterOne;
    private int pwmConfigRegisterTwo;
    private int pwmConfigRegisterThree;
    private int pwmConfigRegisterFour;
    private int pwmConfigRegisterFive;
    private int pwmConfigRegisterSix;
    private int pwmConfigRegisterSeven;
    private int pwmConfigRegisterEight;
    private int aioConfigOne = 0;
    private int aioConfigTwo = 0;
    private int aioConfigThree = 0;
    private int aioConfigFour = 0;
    private int aioConfigFive = 0;
    private int aioConfigSix = 0;
    private int aioConfigSeven = 0;
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


    public LeafletConfiguratorImpl() {
        super(OpenemsComponent.ChannelId.values(), LeafletConfigurator.ChannelId.values());
    }

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    @Reference
    protected ConfigurationAdmin cm;


    protected SourceReader sourceReader = new SourceReader();

    @Activate
    public void activate(ComponentContext context, Config config) {
        //Reads Source file CSV with the Register information
        source = sourceReader.readCsv(config.source());
        //Splits the big CSV Output into the different Modbus Types(OutputCoil,...)
        splitArrayIntoType();
        //Sets the Register variables for the Configuration
        createRelayInverseRegisterArray();
        setPwmConfigurationAddresses();
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId());
    }

    /**
     * Creates an Array with all of the Configuration Registers for inverting Relays.
     */
    private void createRelayInverseRegisterArray() {
        for (int i = 0; i < 4; i++) {
            relayInverseRegisters[i] = analogOutputHoldingRegisters.get(new ModuleRegister(ModuleType.REL, i + 1, 0));
        }
    }

    /**
     * Splits the Source file of all modules into the correct type (Discrete Output Coil, Discrete Input Contact,
     * Analog Input Register , Analog Output Holding Register).
     */
    private void splitArrayIntoType() {
        getSourceHeaderOrder();
        AtomicInteger currentGroup = new AtomicInteger(0);
        for (int group = 0; group <= 4; group++) {
            source.forEach(row -> {
                if (!(row.get(0).equals("") || row.get(0).equals("Modbus Offset") || row.toString().contains("Register"))) {
                    if (currentGroup.get() < 4 && !checkForLastGroupMember(row, currentGroup.get())) {
                        switch (currentGroup.get()) {
                            case (0): {
                                putElementInCorrectMap(discreteOutputCoils, currentGroup.get(), row);
                            }
                            break;
                            case (1): {
                                putElementInCorrectMap(discreteInputContacts, currentGroup.get(), row);
                            }
                            break;
                            case (2): {
                                putElementInCorrectMap(analogInputRegisters, currentGroup.get(), row);
                            }
                            break;
                            case (3): {
                                putElementInCorrectMap(analogOutputHoldingRegisters, currentGroup.get(), row);
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
        map.put(new ModuleRegister(stringToType(
                row.get(moduleTypeOffset + (group * GROUP_SIZE))),
                        Integer.parseInt(row.get(moduleNumberOffset + (group * GROUP_SIZE))),
                        Integer.parseInt(row.get(mRegOffset + (group * GROUP_SIZE)))),
                Integer.parseInt(row.get(0)));
    }

    /**
     * Searches through the Big Source file and writes in the appropriate variable which column contains the types.
     */
    private void getSourceHeaderOrder() {
        for (int n = 0; n < 2; n++) {
            for (int i = HEADER_INFORMATION_OFFSET; i <= GROUP_SIZE; i++) {
                String current = (source.get(n).get(i));
                if (moduleNumberOffset == 0 && current.contains(MODULE_NR)) {
                    moduleNumberOffset = i;
                } else if (moduleTypeOffset == 0 && current.contains(MODULE_TYPE)) {
                    moduleTypeOffset = i;
                } else if (mRegOffset == 0 && current.contains(M_REG)) {
                    mRegOffset = i;
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
        return (row.get(group * GROUP_SIZE + 1).equals("") || row.get(group * GROUP_SIZE + 1).equals("0"));
    }

    @Deactivate
    public void deactivate() {
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
    protected ModbusProtocol defineModbusProtocol() {
        return new ModbusProtocol(this,
                //Read Module Connection Status
                new FC4ReadInputRegistersTask(analogInputRegisters.get(LEAFLET_TMP_CONNECTION_STATUS), Priority.HIGH,
                        m(LeafletConfigurator.ChannelId.TEMPERATURE_MODULES, new UnsignedWordElement(
                                        analogInputRegisters.get(LEAFLET_TMP_CONNECTION_STATUS)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(analogInputRegisters.get(LEAFLET_REL_CONNECTION_STATUS), Priority.HIGH,
                        m(LeafletConfigurator.ChannelId.RELAY_MODULES, new UnsignedWordElement(
                                        analogInputRegisters.get(LEAFLET_REL_CONNECTION_STATUS)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(analogInputRegisters.get(LEAFLET_PWM_CONNECTION_STATUS), Priority.HIGH,
                        m(LeafletConfigurator.ChannelId.PWM_MODULES, new UnsignedWordElement(
                                        analogInputRegisters.get(LEAFLET_PWM_CONNECTION_STATUS)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(analogInputRegisters.get(LEAFLET_AIO_CONNECTION_STATUS), Priority.HIGH,
                        m(LeafletConfigurator.ChannelId.AIO_MODULES, new UnsignedWordElement(
                                        analogInputRegisters.get(LEAFLET_AIO_CONNECTION_STATUS)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),

                new FC4ReadInputRegistersTask(analogOutputHoldingRegisters.get(LEAFLET_CONFIG_REGISTER), Priority.HIGH,
                        m(LeafletConfigurator.ChannelId.READ_LEAFLET_CONFIG, new UnsignedWordElement(
                                        analogOutputHoldingRegisters.get(LEAFLET_CONFIG_REGISTER)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(analogOutputHoldingRegisters.get(LEAFLET_CONFIG_REGISTER),
                        m(LeafletConfigurator.ChannelId.WRITE_LEAFLET_CONFIG,
                                new UnsignedWordElement(analogOutputHoldingRegisters.get(LEAFLET_CONFIG_REGISTER)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),

                //Relay invert Configuration
                new FC6WriteRegisterTask(relayInverseRegisters[0],
                        m(LeafletConfigurator.ChannelId.WRITE_RELAY_ONE_INVERT_STATUS,
                                new SignedWordElement(relayInverseRegisters[0]),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(relayInverseRegisters[1],
                        m(LeafletConfigurator.ChannelId.WRITE_RELAY_TWO_INVERT_STATUS,
                                new UnsignedWordElement(relayInverseRegisters[1]),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(relayInverseRegisters[2],
                        m(LeafletConfigurator.ChannelId.WRITE_RELAY_THREE_INVERT_STATUS,
                                new UnsignedWordElement(relayInverseRegisters[2]),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(relayInverseRegisters[3],
                        m(LeafletConfigurator.ChannelId.WRITE_RELAY_FOUR_INVERT_STATUS,
                                new UnsignedWordElement(relayInverseRegisters[3]),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                //PWM Frequency Configuration
                new FC6WriteRegisterTask(pwmConfigRegisterOne,
                        m(LeafletConfigurator.ChannelId.WRITE_PWM_FREQUENCY_ONE,
                                new SignedWordElement(pwmConfigRegisterOne),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(pwmConfigRegisterTwo,
                        m(LeafletConfigurator.ChannelId.WRITE_PWM_FREQUENCY_TWO,
                                new UnsignedWordElement(pwmConfigRegisterTwo),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(pwmConfigRegisterThree,
                        m(LeafletConfigurator.ChannelId.WRITE_PWM_FREQUENCY_THREE,
                                new UnsignedWordElement(pwmConfigRegisterThree),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(pwmConfigRegisterFour,
                        m(LeafletConfigurator.ChannelId.WRITE_PWM_FREQUENCY_FOUR,
                                new UnsignedWordElement(pwmConfigRegisterFour),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(pwmConfigRegisterFive,
                        m(LeafletConfigurator.ChannelId.WRITE_PWM_FREQUENCY_FIVE,
                                new SignedWordElement(pwmConfigRegisterFive),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(pwmConfigRegisterSix,
                        m(LeafletConfigurator.ChannelId.WRITE_PWM_FREQUENCY_SIX,
                                new UnsignedWordElement(pwmConfigRegisterSix),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(pwmConfigRegisterSeven,
                        m(LeafletConfigurator.ChannelId.WRITE_PWM_FREQUENCY_SEVEN,
                                new UnsignedWordElement(pwmConfigRegisterSeven),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(pwmConfigRegisterEight,
                        m(LeafletConfigurator.ChannelId.WRITE_PWM_FREQUENCY_EIGHT,
                                new UnsignedWordElement(pwmConfigRegisterEight),
                                ElementToChannelConverter.DIRECT_1_TO_1)),

                new FC3ReadRegistersTask(pwmConfigRegisterOne, Priority.LOW,
                        m(LeafletConfigurator.ChannelId.READ_PWM_FREQUENCY_ONE,
                                new SignedWordElement(pwmConfigRegisterOne),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(pwmConfigRegisterTwo, Priority.LOW,
                        m(LeafletConfigurator.ChannelId.READ_PWM_FREQUENCY_TWO,
                                new UnsignedWordElement(pwmConfigRegisterTwo),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(pwmConfigRegisterThree, Priority.LOW,
                        m(LeafletConfigurator.ChannelId.READ_PWM_FREQUENCY_THREE,
                                new UnsignedWordElement(pwmConfigRegisterThree),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(pwmConfigRegisterFour, Priority.LOW,
                        m(LeafletConfigurator.ChannelId.READ_PWM_FREQUENCY_FOUR,
                                new UnsignedWordElement(pwmConfigRegisterFour),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(pwmConfigRegisterFive, Priority.LOW,
                        m(LeafletConfigurator.ChannelId.READ_PWM_FREQUENCY_FIVE,
                                new SignedWordElement(pwmConfigRegisterFive),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(pwmConfigRegisterSix, Priority.LOW,
                        m(LeafletConfigurator.ChannelId.READ_PWM_FREQUENCY_SIX,
                                new UnsignedWordElement(pwmConfigRegisterSix),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(pwmConfigRegisterSeven, Priority.LOW,
                        m(LeafletConfigurator.ChannelId.READ_PWM_FREQUENCY_SEVEN,
                                new UnsignedWordElement(pwmConfigRegisterSeven),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(pwmConfigRegisterEight, Priority.LOW,
                        m(LeafletConfigurator.ChannelId.READ_PWM_FREQUENCY_EIGHT,
                                new UnsignedWordElement(pwmConfigRegisterEight),
                                ElementToChannelConverter.DIRECT_1_TO_1)),


                new FC3ReadRegistersTask(relayInverseRegisters[0], Priority.LOW,
                        m(LeafletConfigurator.ChannelId.READ_RELAY_ONE_INVERT_STATUS,
                                new UnsignedWordElement(relayInverseRegisters[0]),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(relayInverseRegisters[1], Priority.LOW,
                        m(LeafletConfigurator.ChannelId.READ_RELAY_TWO_INVERT_STATUS,
                                new UnsignedWordElement(relayInverseRegisters[1]),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(relayInverseRegisters[2], Priority.LOW,
                        m(LeafletConfigurator.ChannelId.READ_RELAY_THREE_INVERT_STATUS,
                                new UnsignedWordElement(relayInverseRegisters[2]),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(relayInverseRegisters[3], Priority.LOW,
                        m(LeafletConfigurator.ChannelId.READ_RELAY_FOUR_INVERT_STATUS,
                                new UnsignedWordElement(relayInverseRegisters[3]),
                                ElementToChannelConverter.DIRECT_1_TO_1))

        );


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
                                && checkPin(moduleType, moduleNumber, position, id)) {
                            positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;
                    case 2:
                        //0b010
                        if (((getTemperatureModules() & LEAFLET_MODULE_TWO) == LEAFLET_MODULE_TWO)
                                && checkPin(moduleType, moduleNumber, position, id)) {
                            positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;
                    case 3:
                        //0b100
                        if (((getTemperatureModules() & LEAFLET_MODULE_THREE) == LEAFLET_MODULE_THREE)
                                && checkPin(moduleType, moduleNumber, position, id)) {
                            positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
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
                                && checkPin(moduleType, moduleNumber, position, id)) {
                            positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;

                    case 2:
                        //0b0010
                        if (((getRelayModules() & LEAFLET_MODULE_TWO) == LEAFLET_MODULE_TWO)
                                && checkPin(moduleType, moduleNumber, position, id)) {
                            positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;

                    case 3:
                        //0b0100
                        if (((getRelayModules() & LEAFLET_MODULE_THREE) == LEAFLET_MODULE_THREE)
                                && checkPin(moduleType, moduleNumber, position, id)) {
                            positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;
                    case 4:
                        //0b1000
                        if (((getRelayModules() & LEAFLET_MODULE_FOUR) == LEAFLET_MODULE_FOUR)
                                && checkPin(moduleType, moduleNumber, position, id)) {
                            positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
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
                                && checkPin(moduleType, moduleNumber, position, id)) {
                            positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;

                    case 2:
                        //0b00000010
                        if (((getPwmModules() & LEAFLET_MODULE_TWO) == LEAFLET_MODULE_TWO)
                                && checkPin(moduleType, moduleNumber, position, id)) {
                            positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;

                    case 3:
                        //0b00000100
                        if (((getPwmModules() & LEAFLET_MODULE_THREE) == LEAFLET_MODULE_THREE)
                                && checkPin(moduleType, moduleNumber, position, id)) {
                            positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;
                    case 4:
                        //0b00001000
                        if (((getPwmModules() & LEAFLET_MODULE_FOUR) == LEAFLET_MODULE_FOUR)
                                && checkPin(moduleType, moduleNumber, position, id)) {
                            positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;
                    case 5:
                        //0b00010000
                        if (((getPwmModules() & LEAFLET_MODULE_FIVE) == LEAFLET_MODULE_FIVE)
                                && checkPin(moduleType, moduleNumber, position, id)) {
                            positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;

                    case 6:
                        //0b00100000
                        if (((getPwmModules() & LEAFLET_MODULE_SIX) == LEAFLET_MODULE_SIX)
                                && checkPin(moduleType, moduleNumber, position, id)) {
                            positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;

                    case 7:
                        //0b01000000
                        if (((getPwmModules() & LEAFLET_MODULE_SEVEN) == LEAFLET_MODULE_SEVEN)
                                && checkPin(moduleType, moduleNumber, position, id)) {
                            positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;
                    case 8:
                        //0b10000000
                        if (((getPwmModules() & LEAFLET_MODULE_EIGHT) == LEAFLET_MODULE_EIGHT)
                                && checkPin(moduleType, moduleNumber, position, id)) {
                            positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
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
                                && checkPin(moduleType, moduleNumber, position, id)) {
                            positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;

                    case 2:
                        //0b00000010
                        if (((getAioModules() & LEAFLET_MODULE_TWO) == LEAFLET_MODULE_TWO)
                                && checkPin(moduleType, moduleNumber, position, id)) {
                            positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;

                    case 3:
                        //0b00000100
                        if (((getAioModules() & LEAFLET_MODULE_THREE) == LEAFLET_MODULE_THREE)
                                && checkPin(moduleType, moduleNumber, position, id)) {
                            positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;
                    case 4:
                        //0b00001000
                        if (((getAioModules() & LEAFLET_MODULE_FOUR) == LEAFLET_MODULE_FOUR)
                                && checkPin(moduleType, moduleNumber, position, id)) {
                            positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;
                    case 5:
                        //0b00010000
                        if (((getAioModules() & LEAFLET_MODULE_FIVE) == LEAFLET_MODULE_FIVE)
                                && checkPin(moduleType, moduleNumber, position, id)) {
                            positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;

                    case 6:
                        //0b00100000
                        if (((getAioModules() & LEAFLET_MODULE_SIX) == LEAFLET_MODULE_SIX)
                                && checkPin(moduleType, moduleNumber, position, id)) {
                            positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
                        break;

                    case 7:
                        //0b01000000
                        if (((getAioModules() & LEAFLET_MODULE_SEVEN) == LEAFLET_MODULE_SEVEN)
                                && checkPin(moduleType, moduleNumber, position, id)) {
                            positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
                            return true;
                        }
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
        if (!positionMap.containsKey(type)) {
            positionMap.put(type, new PositionMap(moduleNumber, position));
            PinOwner firstRun = new PinOwner(type, moduleNumber, position);
            ownerMap.put(id, firstRun);
            return true;
        } else if (!positionMap.get(type).getPositionMap().containsKey(moduleNumber)) {
            List<Integer> initList = new ArrayList<>(position);
            positionMap.get(type).getPositionMap().put(moduleNumber, initList);
            PinOwner firstRun = new PinOwner(type, moduleNumber, position);
            ownerMap.put(id, firstRun);
            return true;
        } else if (!positionMap.get(type).getPositionMap().get(moduleNumber).contains(position)) {
            positionMap.get(type).getPositionMap().get(moduleNumber).add(position);
            PinOwner firstRun = new PinOwner(type, moduleNumber, position);
            ownerMap.put(id, firstRun);
            return true;
        } else {
            return ownerMap.get(id).equals(new PinOwner(type, moduleNumber, position));
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
                return analogInputRegisters.get(new ModuleRegister(moduleType, moduleNumber, mReg));
            case REL:
                return discreteOutputCoils.get(new ModuleRegister(moduleType, moduleNumber, mReg));
            case PWM:
                return analogOutputHoldingRegisters.get(new ModuleRegister(moduleType, moduleNumber, mReg));
            //Error state (0xFFFF) should never happen.
            default:
                return 65535;
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
            return getFunctionAddress(moduleType, moduleNumber, position);
        } else {
            if (input) {
                return analogInputRegisters.get(new ModuleRegister(moduleType, moduleNumber, position));
            } else {
                return analogOutputHoldingRegisters.get(new ModuleRegister(moduleType, moduleNumber, position));
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
        return analogOutputHoldingRegisters.get(new ModuleRegister(moduleType, moduleNumber, 0));
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
        positionMap.get(moduleType).getPositionMap().get(moduleNumber).remove((Integer) position);
    }

    /**
     * Invert the Relay Functionality.
     *
     * @param moduleNumber Module Number specified on the Relay module
     * @param position     Position of the Relay on the Module
     */
    @Override
    public void invertRelay(int moduleNumber, int position) {
        try {
            switch (moduleNumber) {
                case 1:
                    relayOneInvertStatus = relayOneInvertStatus | position;
                    getWriteInvertRelayOneStatus().setNextWriteValue(relayOneInvertStatus);
                    break;
                case 2:
                    relayTwoInvertStatus = relayTwoInvertStatus | position;
                    getWriteInvertRelayTwoStatus().setNextWriteValue(relayTwoInvertStatus);
                    break;
                case 3:
                    relayThreeInvertStatus = relayThreeInvertStatus | position;
                    getWriteInvertRelayThreeStatus().setNextWriteValue(relayThreeInvertStatus);
                    break;
                case 4:
                    relayFourInvertStatus = relayFourInvertStatus | position;
                    getWriteInvertRelayFourStatus().setNextWriteValue(relayFourInvertStatus);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + moduleNumber);
            }
        } catch (OpenemsError.OpenemsNamedException ignored) {
            this.log.error("Error in invertRelay");
        }
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
        return discreteOutputCoils.get(new ModuleRegister(ModuleType.PWM, pwmModule, mReg));
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
        int configInt = convertAioConfigToInt(config);
        enterConfigMode();
        try {
            switch (moduleNumber) {
                case 1:
                    aioConfigOne = aioConfigOne | (position + configInt);
                    getAioConfigOne().setNextWriteValue(aioConfigOne);
                    break;
                case 2:
                    aioConfigTwo = aioConfigTwo | (position + configInt);
                    getAioConfigTwo().setNextWriteValue(aioConfigTwo);
                    break;
                case 3:
                    aioConfigThree = aioConfigThree | (position + configInt);
                    getAioConfigThree().setNextWriteValue(aioConfigThree);
                    break;
                case 4:
                    aioConfigFour = aioConfigFour | (position + configInt);
                    getAioConfigFour().setNextWriteValue(aioConfigFour);
                    break;
                case 5:
                    aioConfigFive = aioConfigFive | (position + configInt);
                    getAioConfigFive().setNextWriteValue(aioConfigFive);
                    break;
                case 6:
                    aioConfigSix = aioConfigSix | (position + configInt);
                    getAioConfigSix().setNextWriteValue(aioConfigSix);
                    break;
                case 7:
                    aioConfigSeven = aioConfigSeven | (position + configInt);
                    getAioConfigSeven().setNextWriteValue(aioConfigSeven);
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
            return 65535;
        } else {
            if (input) {
                return analogInputRegisters.get(new ModuleRegister(moduleType, moduleNumber, mReg));
            } else {
                return analogOutputHoldingRegisters.get(new ModuleRegister(moduleType, moduleNumber, mReg));
            }
        }

    }

    @Override
    public void revertInversion(int moduleNumber, int position) {
        try {
            switch (moduleNumber) {
                case 1:
                    relayOneInvertStatus = relayOneInvertStatus ^ position;
                    getWriteInvertRelayOneStatus().setNextWriteValue(relayOneInvertStatus);
                    break;
                case 2:
                    relayTwoInvertStatus = relayTwoInvertStatus ^ position;
                    getWriteInvertRelayTwoStatus().setNextWriteValue(relayTwoInvertStatus);
                    break;
                case 3:
                    relayThreeInvertStatus = relayThreeInvertStatus ^ position;
                    getWriteInvertRelayThreeStatus().setNextWriteValue(relayThreeInvertStatus);
                    break;
                case 4:
                    relayFourInvertStatus = relayFourInvertStatus ^ position;
                    getWriteInvertRelayFourStatus().setNextWriteValue(relayFourInvertStatus);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + moduleNumber);
            }
        } catch (OpenemsError.OpenemsNamedException ignored) {
            this.log.error("Error in revertInversion");
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
            getWriteLeafletConfigChannel().setNextWriteValue(7331);
            configFlag = true;
        } catch (OpenemsError.OpenemsNamedException ignored) {
            this.log.error("Error in enterConfigMode");
        }

    }

    /**
     * Stores the pwmConfig Register in a local variable.
     */
    private void setPwmConfigurationAddresses() {
        pwmConfigRegisterOne = getConfigurationAddress(ModuleType.PWM, 1);
        pwmConfigRegisterTwo = getConfigurationAddress(ModuleType.PWM, 2);
        pwmConfigRegisterThree = getConfigurationAddress(ModuleType.PWM, 3);
        pwmConfigRegisterFour = getConfigurationAddress(ModuleType.PWM, 4);
        pwmConfigRegisterFive = getConfigurationAddress(ModuleType.PWM, 5);
        pwmConfigRegisterSix = getConfigurationAddress(ModuleType.PWM, 6);
        pwmConfigRegisterSeven = getConfigurationAddress(ModuleType.PWM, 7);
        pwmConfigRegisterEight = getConfigurationAddress(ModuleType.PWM, 8);
    }

    /**
     * Writes the Error channel, if the Communication with the Firmware is interrupted.
     * Checks if we are still in Config mode and the Configuration has already taken place.
     *
     * @param event BEFORE_PROCESS_IMAGE
     */
    @Override
    public void handleEvent(Event event) {
        //BridgeModbus.ChannelId.SLAVE_COMMUNICATION_FAILED;
        if (this.modbus != null) {
            this.getErrorChannel().setNextValue(this.modbus.get().channel(BridgeModbus.ChannelId.SLAVE_COMMUNICATION_FAILED).value());
        }
        if (configFlag && (getReadAioConfig() == (aioConfigOne | aioConfigTwo | aioConfigThree | aioConfigFour | aioConfigFive | aioConfigSix | aioConfigSeven))) {
            exitConfigMode();
        }

    }

    /**
     * Exits Configuration mode by changing the Config register back.
     */
    private void exitConfigMode() {
        try {
            getWriteLeafletConfigChannel().setNextWriteValue(1337);
            configFlag = false;
        } catch (OpenemsError.OpenemsNamedException ignored) {
            this.log.error("Error in exitConfigMode");
        }

    }

}




