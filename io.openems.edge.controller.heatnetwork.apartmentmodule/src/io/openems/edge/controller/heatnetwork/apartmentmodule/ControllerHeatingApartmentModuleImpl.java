package io.openems.edge.controller.heatnetwork.apartmentmodule;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.apartmentmodule.api.ApartmentModule;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.apartmentmodule.api.ControllerHeatingApartmentModule;
import io.openems.edge.controller.heatnetwork.apartmentmodule.api.State;
import io.openems.edge.heatsystem.components.Pump;
import io.openems.edge.thermometer.api.ThresholdThermometer;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.ApartmentModule",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ControllerHeatingApartmentModuleImpl extends AbstractOpenemsComponent implements OpenemsComponent, Controller, ControllerHeatingApartmentModule {

    @Reference
    ComponentManager cpm;

    private static final String HEAT_PUMP = "HEAT_PUMP";
    private static final String THRESHOLD = "THRESHOLD";
    private final Logger log = LoggerFactory.getLogger(ControllerHeatingApartmentModuleImpl.class);


    //Behind each integer is a ApartmentCord --> One has to be a ApartmentModule with a Relay.
    private final Map<Integer, List<ApartmentModule>> apartmentCords = new HashMap<>();
    private final Map<Integer, List<ChannelAddress>> responseToCords = new HashMap<>();
    private Map<Integer, ThresholdThermometer> thresholdThermometerMap = new HashMap<>();
    private final List<Integer> cordsToHeatUp = new ArrayList<>();
    private Pump heatResponsePump;

    public ControllerHeatingApartmentModuleImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values(),
                ControllerHeatingApartmentModule.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        if (config.apartmentCords().length != config.apartmentResponse().length || config.apartmentResponse().length != config.thresholdId().length) {
            throw new ConfigurationException("Activate of ControllerHeatingApartmentModule",
                    "Expected the same Length for ApartmentCords (got:  " + config.apartmentCords().length
                            + ") and ApartmentResponse(got: " + config.apartmentResponse().length
                            + ") and ThresholdIds (got: " + config.thresholdId().length);
        }
        this.allocateComponent(config, HEAT_PUMP);
        this.allocateComponent(config, THRESHOLD);
        this.applyApartmentModules(config.apartmentCords());
        this.applyResponseToCords(config.apartmentResponse());
        this.setSetPointPowerLevel(config.powerLevelPump());
        this.setSetPointTemperature(config.setPointTemperature());
    }

    /**
     * Allocates the Component to their corresponding configuration.
     *
     * @param config        the config of the component.
     * @param allocatorType static string, defined by this component.
     * @throws ConfigurationException             is thrown if the user typed in something wrong.
     * @throws OpenemsError.OpenemsNamedException if the component couldn't be found.
     */
    private void allocateComponent(Config config, String allocatorType) throws ConfigurationException, OpenemsError.OpenemsNamedException {

        switch (allocatorType) {
            case HEAT_PUMP:
                OpenemsComponent component;
                String deviceToGet;
                deviceToGet = config.heatPumpId();
                component = this.cpm.getComponent(deviceToGet);
                if (component instanceof Pump) {
                    this.heatResponsePump = (Pump) component;
                } else {
                    throw new ConfigurationException("Allocate Componenten in Controller Heating Apartment Module",
                            "Component with id: " + component.id() + " is not from an expected Type");
                }
                break;
            case THRESHOLD:
                allocateAllThreshold(config);
                break;
            default:
                throw new ConfigurationException("This shouldn't occur", "Weird...");
        }
    }

    private void allocateAllThreshold(Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        ConfigurationException[] exConf = {null};
        OpenemsError.OpenemsNamedException[] exNamed = {null};
        List<String> thresholdIds = Arrays.asList(config.thresholdId());
        Set<String> duplicates = this.foundDuplicates(thresholdIds);
        if (duplicates.size() > 0) {
            throw new ConfigurationException("Allocate ThresholdThermometer: Duplicates", "Duplicates were found in Config for Thresholds: " + duplicates.toString());
        }

        thresholdIds.forEach(threshold -> {
            if (exConf[0] == null && exNamed[0] == null) {
                try {
                    OpenemsComponent component = this.cpm.getComponent(threshold);
                    if (component instanceof ThresholdThermometer) {
                        this.thresholdThermometerMap.put(thresholdIds.indexOf(threshold), (ThresholdThermometer) component);
                    } else {
                        exConf[0] = new ConfigurationException("allocateAllThreshold",
                                "Component not an instance of ThresholdThermometer: " + component.id());
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    exNamed[0] = e;
                }
            }
        });

        if (exConf[0] != null) {
            throw exConf[0];
        }
        if (exNamed[0] != null) {
            throw exNamed[0];
        }
    }


    private void applyResponseToCords(String[] apartmentResponse) throws ConfigurationException {
        //Split entry into corresponding cords
        Map<Integer, String> keyEntryMap = new HashMap<>();
        for (int x = 0; x < apartmentResponse.length; x++) {
            keyEntryMap.put(x, apartmentResponse[x]);
        }
        ConfigurationException[] ex = {null};

        keyEntryMap.forEach((key, value) -> {
            if (ex[0] == null) {
                //Check if Entries are valid ChannelAddresses
                List<String> response = Arrays.asList(value.split(":"));
                response.forEach(string -> {
                    if (ex[0] == null) {
                        try {
                            ChannelAddress channelAddressToPut = ChannelAddress.fromString(string);
                            if (this.responseToCords.containsKey(key)) {
                                this.responseToCords.get(key).add(channelAddressToPut);
                            } else {
                                List<ChannelAddress> channelList = new ArrayList<>();
                                channelList.add(channelAddressToPut);
                                this.responseToCords.put(key, channelList);
                            }
                        } catch (OpenemsError.OpenemsNamedException e) {
                            ex[0] = new ConfigurationException("ApplyResponseToChords", "Couldn't find channelAddress: " + string);
                        }
                    }
                });
            }
        });

        if (ex[0] != null) {
            throw ex[0];
        }
    }

    private void applyApartmentModules(String[] apartmentCords) throws ConfigurationException {
        //Split Map to corresponding Cord
        List<String> everyApartmentCord = new ArrayList<>();
        Map<Integer, String> keyEntryMap = new HashMap<>();
        for (int x = 0; x < apartmentCords.length; x++) {
            keyEntryMap.put(x, apartmentCords[x]);
            everyApartmentCord.addAll(Arrays.asList(apartmentCords[x].split(":")));
        }
        Set<String> duplicates = this.foundDuplicates(everyApartmentCord);
        if (duplicates.size() > 0) {
            throw new ConfigurationException("ApartmentModules", "Duplications in Config found of Apartmentmodules: " + duplicates.toString());
        }
        ConfigurationException[] ex = {null};
        keyEntryMap.forEach((key, value) -> {
            if (ex[0] == null) {
                //Check each entry of cord
                List<String> apartmentmoduleIds = Arrays.asList(value.split(":"));
                List<ApartmentModule> apartmentModuleList = new ArrayList<>();
                apartmentmoduleIds.forEach(apartmentmodule -> {
                    if (ex[0] == null) {
                        try {
                            OpenemsComponent component = this.cpm.getComponent(apartmentmodule.trim());
                            if (component instanceof ApartmentModule) {
                                apartmentModuleList.add((ApartmentModule) component);
                            } else {
                                ex[0] = new ConfigurationException("Applay ApartmentModules ", "Couldn't find Component with id: " + apartmentmodule);
                            }
                        } catch (OpenemsError.OpenemsNamedException e) {
                            ex[0] = new ConfigurationException("Applay ApartmentModules ", "Couldn't find Component with id: " + apartmentmodule);
                        }
                    }
                });
                //Check if only 1 top-module exists
                int topModuleCounter = (int) apartmentModuleList.stream().filter(ApartmentModule::isModuleWithRelays).count();
                if (topModuleCounter != 1) {
                    ex[0] = new ConfigurationException("Apply apartmentModules", "Size of Relaysmodules: " + topModuleCounter);
                } else {
                    this.apartmentCords.put(key, apartmentModuleList);
                }
            }
        });

        if (ex[0] != null) {
            throw ex[0];
        }

    }


    @Override
    public void run() throws OpenemsError.OpenemsNamedException {
        boolean emergencyStop = this.getEmergencyStopChannel().getNextWriteValue().isPresent()
                && this.getEmergencyStopChannel().getNextWriteValue().get();
        if (checkMissingComponents() == false || emergencyStop) {
            this.heatResponsePump.setPowerLevel(0);
            //Note: EnableSignals will be set automatically to false/they getNextWriteValueAndReset --> if no enable --> they will go offline
            this.setState(State.EMERGENCY_STOP);
        } else {
            heatRoutine();
        }
    }

    /**
     * Usual Routine that runs if no EmergencyStop is active.
     * Check for heatrequests and if response requirements are met --> respondchannels are set to true.
     * Also activate the heatpump on requests.
     *
     * @throws OpenemsError.OpenemsNamedException if the Corresponding respond channel couldn't be found.
     */
    private void heatRoutine() throws OpenemsError.OpenemsNamedException {

        boolean emergencyEnablePump = this.getEmergencyPumpStartChannel().getNextWriteValue().isPresent()
                && this.getEmergencyPumpStartChannel().getNextWriteValue().get();
        boolean emergencyResponse = this.getEmergencyEnableEveryResponseChannel().getNextWriteValue().isPresent()
                && this.getEmergencyEnableEveryResponseChannel().getNextWriteValue().get();

        AtomicReference<List<Integer>> keysThatHadResponse = new AtomicReference<>(new ArrayList<>());
        List<OpenemsError.OpenemsNamedException> errors = new ArrayList<>();
        //check for Requests and put into keysThatHadResponse
        checkHeatRequests(emergencyResponse, keysThatHadResponse);
        addKeysToCordsToHeatUp(keysThatHadResponse);
        //Activate pump on requests or on Emergency
        activatePumpOnRequestsOrEmergency(keysThatHadResponse, emergencyEnablePump);
        //check if Temperature < setpoint or if emergency Response needs to react
        checkResponseRequirementAndRespond(emergencyResponse || emergencyEnablePump, emergencyResponse, errors);
        if (errors.size() > 0) {
            throw errors.get(0);
        }

    }

    /**
     * Adds the configured Keys to the CordsToHeatUp.
     *
     * @param keysThatHadResponse usually from run method. Contains all the keys to add.
     */
    private void addKeysToCordsToHeatUp(AtomicReference<List<Integer>> keysThatHadResponse) {
        keysThatHadResponse.get().forEach(key -> {
            if (this.cordsToHeatUp.contains(key) == false) {
                this.cordsToHeatUp.add(key);
            }
        });
    }

    /**
     * Checks if the ResponseRequirements are met and then respond to the Channel of this.respondsToChords. Keys determined by keysThatHadRepsonse.
     *
     * @param emergencyOccurred checks if any Emergency occured --> If not--> Set State to extra Heat if required
     * @param emergencyResponse Contains the emergencyResponse Boolean. Usually from own channel.
     * @param errors            the List of Errors that will be filled in case of any error --> thrown later.
     */
    private void checkResponseRequirementAndRespond(boolean emergencyOccurred, boolean emergencyResponse,
                                                    List<OpenemsError.OpenemsNamedException> errors) {


        this.thresholdThermometerMap.forEach((key, threshold) -> {
            if (this.cordsToHeatUp.contains(key)) {
                if (threshold.thermometerBelowGivenTemperature(this.getSetPointTemperature()) || emergencyResponse) {
                    this.responseToCords.get(key).forEach(channel -> {
                        Channel<?> channelToGet;
                        try {
                            channelToGet = this.cpm.getChannel(channel);
                            if (channelToGet instanceof WriteChannel<?> && channelToGet.getType().equals(OpenemsType.BOOLEAN)) {
                                ((WriteChannel<Boolean>) channelToGet).setNextWriteValue(true);
                            } else {
                                log.error("Channel: " + channelToGet
                                        + " Not the correct Channel! Is either not WriteChannel or not Boolean!"
                                        + channelToGet.channelDoc().getAccessMode() + " " + channelToGet.getType());
                            }
                        } catch (OpenemsError.OpenemsNamedException e) {
                            errors.add(e);
                        }

                    });
                } else {
                    this.cordsToHeatUp.remove(key);
                }
            }
        });
        if (emergencyOccurred == false) {
            this.setState(State.EXTRA_HEAT);
        }

    }

    /**
     * Activates the Heatpump in case of emergency or on HeatRequest.
     *
     * @param keysThatHadResponse List to check if any Requests are given. Usually filled by checkHeatRequest Method
     * @param emergencyEnablePump a Boolean to check if there is a case of emergency.
     */
    private void activatePumpOnRequestsOrEmergency(AtomicReference<List<Integer>> keysThatHadResponse, boolean emergencyEnablePump) {
        if (keysThatHadResponse.get().size() > 0 || emergencyEnablePump || this.cordsToHeatUp.size() > 0) {
            this.heatResponsePump.setPowerLevel(this.getSetPointPowerLevel());
            this.setState(State.HEAT_PUMP_ACTIVE);
            if (emergencyEnablePump) {
                this.setState(State.EMERGENCY_ON);
            }
        } else {
            this.setState(State.IDLE);
        }
    }

    /**
     * Checks if there are any HeatRequests available and add them to the List.
     *
     * @param emergencyResponse   In Case of an emergency this was set to true --> all responses will be set to true
     * @param keysThatHadResponse the List that will be filled with keys of the heatRequest cords.
     */
    private void checkHeatRequests(boolean emergencyResponse, AtomicReference<List<Integer>> keysThatHadResponse) {
        if (emergencyResponse) {
            keysThatHadResponse.set(new ArrayList<>(this.responseToCords.keySet()));
            this.setState(State.EMERGENCY_ON);
        } else {
            this.apartmentCords.forEach((key, value) ->
                    value.stream().filter(apartment -> apartment.hasHeatRequest() || apartment.isHeatRequestFlag()).findAny()
                            .ifPresent(module -> keysThatHadResponse.get().add(key)));
        }
    }

    /**
     * Checks for Missing Components --> e.g. Happens on Reactivation of subcomponents.
     *
     * @return aboolean true if everythings ok false if component is missing or something.
     */

    private boolean checkMissingComponents() {
        OpenemsComponent component;
        String id = null;
        AtomicBoolean componentNotFound = new AtomicBoolean(false);
        Map<Integer, ThresholdThermometer> copiedMap = new HashMap<>();
        try {
            this.thresholdThermometerMap.forEach((key, thresholdThermometer) -> {
                if (thresholdThermometer.isEnabled() == false) {
                    String idThreshold;
                    OpenemsComponent componentOfThermometer;
                    idThreshold = thresholdThermometer.id();
                    try {
                        componentOfThermometer = this.cpm.getComponent(idThreshold);
                        if (componentOfThermometer.isEnabled() && componentOfThermometer instanceof ThresholdThermometer) {
                            copiedMap.put(key, (ThresholdThermometer) componentOfThermometer);
                        } else {
                            componentNotFound.set(true);
                            copiedMap.put(key, thresholdThermometer);
                        }
                    } catch (OpenemsError.OpenemsNamedException e) {
                        componentNotFound.set(true);
                        copiedMap.put(key, thresholdThermometer);
                    }
                } else {
                    copiedMap.put(key, thresholdThermometer);
                }
            });
            this.thresholdThermometerMap = copiedMap;

            if (this.heatResponsePump.isEnabled() == false) {
                id = this.heatResponsePump.id();
                component = this.cpm.getComponent(id);
                if (component.isEnabled() && component instanceof Pump) {
                    this.heatResponsePump = (Pump) component;
                } else {
                    return false;
                }
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            log.error("Couldn't find OpenemsComponent with id: " + id);
            log.error("Component: " + this.id() + " will stop: EmergencyStop");
            return false;
        }
        return componentNotFound.get() == false;
    }

    @Override
    public String debugLog() {
        return this.getState().toString();
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    private Set<String> foundDuplicates(List<String> checkList) {
        Set<String> duplications = new HashSet<>();
        Set<String> normal = new HashSet<>();
        checkList.forEach(entry -> {
            if (normal.add(entry) == false) {
                duplications.add(entry);
            }
        });
        return duplications;
    }
}
