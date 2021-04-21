package io.openems.edge.controller.heatnetwork.communication.master;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.communication.RestLeafletCommunicationControllerImpl;
import io.openems.edge.controller.heatnetwork.communication.api.CommunicationController;
import io.openems.edge.controller.heatnetwork.communication.api.CommunicationMasterController;
import io.openems.edge.controller.heatnetwork.communication.api.ConnectionType;
import io.openems.edge.controller.heatnetwork.communication.api.FallbackHandling;
import io.openems.edge.controller.heatnetwork.communication.api.ManageType;
import io.openems.edge.controller.heatnetwork.communication.api.Request;
import io.openems.edge.controller.heatnetwork.communication.api.RequestType;
import io.openems.edge.controller.heatnetwork.communication.api.RestLeafletCommunicationController;
import io.openems.edge.controller.heatnetwork.communication.api.RestRequest;
import io.openems.edge.controller.heatnetwork.communication.request.rest.RestRequestImpl;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.api.HydraulicLineHeater;
import io.openems.edge.heatsystem.components.Pump;
import io.openems.edge.rest.remote.device.general.api.RestRemoteDevice;
import io.openems.edge.thermometer.api.ThermometerThreshold;
import org.joda.time.DateTime;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.CommunicationMaster",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class CommunicationMasterControllerImpl extends AbstractOpenemsComponent implements OpenemsComponent, Controller, CommunicationMasterController {

    @Reference
    ComponentManager cpm;
    //Configured communicationController handling one remoteCommunication
    CommunicationController communicationController;
    //ThresholdTemperature to Write into so another Component can react in response correctly
    private ThermometerThreshold thresholdTemperature;
    //The Optional HydraulicLineHeater
    private HydraulicLineHeater hydraulicLineHeater;
    //The Optional HeatPump
    private Pump heatPump;
    //Will be Set if Connection not ok
    private DateTime initalTimeStampFallback;
    //Is set to true if connection was ok
    //else set to false if fallback
    private boolean wasOkBefore;
    //For Subclasses -> CommunicationController and manager
    private boolean forcing;
    private boolean autorun;
    private boolean extraHeat = false;
    private static final int REMOTE_REQUEST_CONFIGURATION_SIZE = 4;
    private int maxAllowedRequests;
    private int maxWaittime;
    //Current request size --> IF Size is empty -> deactivate extra component else activate them.
    //Is declared as an Integer bc. of future implementation : Do XYZ at Certain Size
    //resets every run
    private AtomicInteger requestSizeThisRun = new AtomicInteger(0);
    private Config config;


    public CommunicationMasterControllerImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values(),
                CommunicationMasterController.ChannelId.values());
    }


    @Activate
    public void activate(ComponentContext context, Config config) throws Exception {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.config = config;
        //ForceHeating == Heat ALL remote Heater/handle ALL requests/Enable all requests even if they don't have a request
        this.setForceHeating(config.forceHeating());
        this.setAutoRun(config.autoRun());
        maxWaittime = config.maxWaittimeAllowed();
        forcing = this.getForceHeating();
        autorun = this.getAutoRun();
        //Creates the Controller responsible for handling RemoteRequests (REST requests)
        createCommunicationController(config);
        createRemoteRequestsAndAddToCommunicationController(config);
        OpenemsComponent optionalComponent;
        if (config.usePump()) {
            optionalComponent = cpm.getComponent(config.pumpId());
            if (optionalComponent instanceof Pump) {
                this.heatPump = (Pump) optionalComponent;
            } else {
                throw new ConfigurationException("CommunicationMaster - Activate - Pump", "PumpId Component - Not an instance of Pump; PumpId: " + config.pumpId());
            }
        }
        if (config.useHydraulicLineHeater()) {
            optionalComponent = cpm.getComponent(config.hydraulicLineHeaterId());
            if (optionalComponent instanceof HydraulicLineHeater) {
                this.hydraulicLineHeater = (HydraulicLineHeater) optionalComponent;
            } else {
                throw new ConfigurationException("CommunicationMaster - Activate - HydraulicLineHeater",
                        "HydraulicLineHeaterId Component - Not an Instance of HydraulicLineHeater : " + config.hydraulicLineHeaterId());
            }
        }

        OpenemsComponent threshold = cpm.getComponent(config.thresholdId());
        if (threshold instanceof ThermometerThreshold) {
            this.thresholdTemperature = (ThermometerThreshold) threshold;
        } else {
            throw new ConfigurationException("CommunicationMaster - Activate - ThresholdThermometer",
                    "Given ID Not an instance of ThresholdThermometer: " + config.thresholdId());
        }

        this.setForceHeating(config.forceHeating());
        this.setAutoRun(config.autoRun());
        this.setKeepAlive(config.keepAlive());
        String fallback = config.fallback();
        if (FallbackHandling.contains(fallback.trim().toUpperCase()) == false) {
            fallback = "DEFAULT";
        }
        this.setFallbackLogic(fallback);
        this.maxAllowedRequests = config.maxRequestAllowedAtOnce();
        this.getMaximumRequestChannel().setNextValue(this.maxAllowedRequests);
    }

    /**
     * Creates the RemoteRequests from Config. --> e.g. RestRemoteComponents will be handled bei a RestCommunicationController
     * get the RequestConfig, and split them.
     * Map the Configured Requests to the corresponding requestMaps.
     * add them to the CommunicationController if they are correct. Else throw Exception.
     *
     * @param config config of the Component
     * @throws ConfigurationException             if there's an error within config
     * @throws OpenemsError.OpenemsNamedException if cpm couldn't find OpenemsComponent
     */
    private void createRemoteRequestsAndAddToCommunicationController(Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        //ErrorHandling in Streams..you can't simply throw exceptions in Streams in Java 8
        ConfigurationException[] ex = {null};
        OpenemsError.OpenemsNamedException[] exceptions = {null};
        List<String> requestConfig = Arrays.asList(config.requestMap());
        //Collection of all Requests, can be any  (sub) Type of request, important for future impl. -> Different Requesttypes
        Map<Integer, List<? super Request>> requestMap = new HashMap<>();

        requestConfig.forEach(entry -> {
            try {
                if (ex[0] == null) {
                    createRequestFromConfigEntry(entry, requestMap, this.communicationController.getConnectionType());
                }
            } catch (ConfigurationException e) {
                ex[0] = e;
            } catch (OpenemsError.OpenemsNamedException e) {
                exceptions[0] = e;
            }
        });
        if (ex[0] != null) {
            throw ex[0];
        }
        if (exceptions[0] != null) {
            throw exceptions[0];
        }
        //Add RestRequests to RestCommunicationController
        if (this.communicationController.getConnectionType().equals(ConnectionType.REST)) {
            Map<Integer, List<RestRequest>> createdRestRequests = new HashMap<>();
            requestMap.forEach((key, value) -> {
                List<RestRequest> requestsOfKey = new ArrayList<>();
                value.forEach(entry -> requestsOfKey.add((RestRequest) entry));
                createdRestRequests.put(key, requestsOfKey);
            });
            ((RestLeafletCommunicationController) communicationController).addRestRequests(createdRestRequests);
        } else {
            throw new ConfigurationException(this.communicationController.getConnectionType().toString(), "Is not supported by this controller");
        }

    }

    /**
     * Creates a Requests from a configuration and puts it into the requestMap.
     * First Split the entires by ":", then check each split param if correct, if params are ok, create corresponding RestRequest.
     *
     * @param entry      the entry usually from Config or Channel.
     * @param requestMap the requestMap coming from calling method (if config done it will be added to the controller)
     * @throws ConfigurationException             if Somethings wrong with the configuration.
     * @throws OpenemsError.OpenemsNamedException if the cpm couldn't find the component.
     */
    private void createRequestFromConfigEntry(String entry, Map<Integer, List<? super Request>> requestMap, ConnectionType connectionType)
            throws ConfigurationException, OpenemsError.OpenemsNamedException {
        String[] entries = entry.split(":");
        if (entries.length != REMOTE_REQUEST_CONFIGURATION_SIZE) {
            throw new ConfigurationException("" + entries.length, "Length not ok expected " + REMOTE_REQUEST_CONFIGURATION_SIZE);
        }
        AtomicInteger configurationCounter = new AtomicInteger(0);
        //REQUEST (Pos 0), CALLBACK (Pos 1), KEY (Pos 2)
        OpenemsComponent request = cpm.getComponent(entries[configurationCounter.getAndIncrement()]);
        OpenemsComponent callback = cpm.getComponent(entries[configurationCounter.getAndIncrement()]);
        int keyForMap = Integer.parseInt(entries[configurationCounter.getAndIncrement()]);
        String requestTypeString = entries[configurationCounter.getAndIncrement()].toUpperCase().trim();
        RequestType type;
        if (RequestType.contains(requestTypeString)) {
            type = RequestType.valueOf(requestTypeString);
        } else {
            throw new ConfigurationException(requestTypeString, "Wrong request Type, allowed Request types are: " + Arrays.toString(RequestType.values()));
        }
        switch (connectionType) {
            case REST:
                if (request instanceof RestRemoteDevice && callback instanceof RestRemoteDevice) {
                    RestRequest requestToAdd = new RestRequestImpl((RestRemoteDevice) request, (RestRemoteDevice) callback, type);
                    if (requestMap.containsKey(keyForMap)) {
                        requestMap.get(keyForMap).add(requestToAdd);
                    } else {
                        List<Request> requestListForMap = new ArrayList<>();
                        requestListForMap.add(requestToAdd);
                        requestMap.put(keyForMap, requestListForMap);
                    }
                } else {
                    throw new ConfigurationException("ConfigurationError",
                            "Request and Callback have to be from the same type");
                }
                break;
        }

    }


    /**
     * Creates The CommunicationController by Config.
     * First get the Connection Type and Manage type as well as the maximumRequest
     * (how many Requests are allowed at once-->Integer count NOT Listentry; Integer represents a Heatstorage/Heater etc)
     * (Map<Integer, List< ? super Request>).
     *
     * @param config config of this component
     * @throws Exception thrown if somethings wrong with
     */
    private void createCommunicationController(Config config) throws Exception {
        String connectionTypeString = config.connectionType().trim().toUpperCase();
        String manageTypeString = config.manageType().toUpperCase().trim();
        int maxRequestsAllowedAtOnce = config.maxRequestAllowedAtOnce();
        if (ConnectionType.contains(connectionTypeString) && ManageType.contains(manageTypeString)) {
            ConnectionType connectionType = ConnectionType.valueOf(connectionTypeString);
            ManageType manageType = ManageType.valueOf(manageTypeString);
            switch (connectionType) {
                case REST:
                default:
                    this.communicationController = new RestLeafletCommunicationControllerImpl(connectionType,
                            manageType, maxRequestsAllowedAtOnce,
                            this.forcing, this.autorun);
                    this.communicationController.setMaxWaittime(maxWaittime);
            }
        }

    }

    @Deactivate
    public void deactivate() {
        this.deactivateComponents();
        this.communicationController.setAutoRun(false);
        this.communicationController.stop();
        super.deactivate();
    }

    @Override
    public void run() throws OpenemsError.OpenemsNamedException {
        //Check if Requests have to be added/removed and if components are still enabled
        this.checkChangesAndApply();
        //Connections ok?
        AtomicBoolean connectionOk = new AtomicBoolean(true);

        connectionOk.getAndSet(this.communicationController.communicationAvailable());

        if (connectionOk.get()) {
            //Release the Id if this instance was the "owner" of the SetPoint Temperature
            //E.g. : SetPointTemperature of this instance was 750 dC; other components wanted to set SetPoint to 700dC --> not possible
            //lower temp. won't be accpeted; now after release other components can set the temperature to 700dC
            if (wasOkBefore == false) {
                this.thresholdTemperature.releaseSetPointTemperatureId(super.id());
            }
            //Reset wasOkBefore, for the case Connection is lost --> Boolean to set Initial Time again
            wasOkBefore = true;
            this.checkRequestSizeAndEnableCallbackActivateComponents();
        } else if (checkTimeIsUp()) {
            this.fallbackLogic();
        }
        this.requestSizeThisRun.set(0);
    }

    /**
     * Checks if hydraulicHeater and Pump are still enabled (if Present).
     */
    private void checkChangesAndApply() {
        //Check if Components are still enabled
        try {
            if (this.hydraulicLineHeater != null && this.hydraulicLineHeater.isEnabled() == false) {
                if (this.cpm.getComponent(this.hydraulicLineHeater.id()) instanceof HydraulicLineHeater) {
                    this.hydraulicLineHeater = cpm.getComponent(this.hydraulicLineHeater.id());
                }
            }
            if (this.heatPump != null && this.heatPump.isEnabled() == false) {
                if (this.cpm.getComponent(this.heatPump.id()) instanceof Pump) {
                    this.heatPump = cpm.getComponent(this.heatPump.id());
                }
            }

            if (this.thresholdTemperature == null || this.thresholdTemperature.isEnabled() == false) {
                if (this.cpm.getComponent(this.config.thresholdId()) instanceof ThermometerThreshold) {
                    this.thresholdTemperature = this.cpm.getComponent(config.thresholdId());
                }
            }

            if (this.getSetMaximumRequestChannel().value().isDefined()) {
                this.setMaximumRequests(this.getSetMaximumRequestChannel().value().get());

                this.getSetMaximumRequestChannel().setNextWriteValue(null);

            }
        } catch (OpenemsError.OpenemsNamedException ignored) {
        }

        Optional<Integer> isSetMaximumWritten = this.getSetMaximumRequestChannel().getNextWriteValueAndReset();
        isSetMaximumWritten.ifPresent(integer -> this.getMaximumRequestChannel().setNextValue(integer));
        //Sets the Maximum allowed Requests at once in Master and Manager.
        this.maxAllowedRequests = this.getMaximumRequests();
        if (this.maxAllowedRequests != this.communicationController.getRequestManager().getMaxRequestsAtOnce()) {
            this.communicationController.getRequestManager().setMaxManagedRequests(this.maxAllowedRequests);
        }
    }

    /**
     * Checks if time is up. Depending on.:
     * Was the run before ok? if Was OK -->set Initial Timestamp and set before ok to false
     * Else --> Check if saved Datetime + Keep alive is after current time
     * Checks if Calculated "FallbackTime" (Initial Time + WaitTime) is After the Current Time
     *
     * @return a boolean.
     */

    private boolean checkTimeIsUp() {
        if (this.wasOkBefore) {
            this.wasOkBefore = false;
            this.initalTimeStampFallback = new DateTime();
            return false;
        } else {
            DateTime now = new DateTime();
            DateTime compare = new DateTime(this.initalTimeStampFallback);
            compare.plusSeconds(this.getKeepAlive());
            return now.isAfter(compare);
        }
    }

    /**
     * Fallback Logic of this controller, depending on the set FallbackLogic.
     * Default case is to activate the Components (Heatpump/Lineheater) and
     * set the ThresholdTemperature as well as enabling/activate it, set The Id to this Component.
     */
    private void fallbackLogic() {
        String fallback = this.getExecutionOnFallback().toUpperCase().trim();

        if (fallback.equals("") || FallbackHandling.contains(fallback) == false) {
            fallback = "DEFAULT";
        }
        switch (FallbackHandling.valueOf(fallback)) {

            case HEAT:
                break;
            case OPEN:
                break;
            case CLOSE:
                break;
            case DEFAULT:
            default:
                activateComponents();
                if (this.thresholdTemperature != null && this.thresholdTemperature.isEnabled()) {
                    int setPoint = this.getSetPointTemperature();
                    if (setPoint > 0) {
                        this.thresholdTemperature.setSetPointTemperatureAndActivate(setPoint, super.id());
                    }
                }
                this.getCurrentRequestsChannel().setNextValue(this.getMaximumRequests());
                break;
        }
    }

    /**
     * Standard Logic of the Controller.
     * Get the communicationController and execute their Logic.
     * Get all the currentManagedRequests and check for "true" Requests. Set Callback to true,
     * so RemoteComponents are allowed to react.
     * If extra Heat is requested set to true for later handling.
     */
    private void checkRequestSizeAndEnableCallbackActivateComponents() {
        //Handle Requests
        this.communicationController.executeLogic();
        AtomicBoolean extraHeatThisRun = new AtomicBoolean(false);
        if (this.communicationController instanceof RestLeafletCommunicationController) {
            Map<Integer, List<RestRequest>> currentRestRequests =
                    ((RestLeafletCommunicationController) this.communicationController).getRestManager().getManagedRequests();
            if (currentRestRequests.size() > 0) {
                currentRestRequests.forEach((key, value) -> {
                    value.forEach(restRequest -> {
                        if (restRequest.getRequest().getValue().equals("1") || this.forcing) {
                            restRequest.getCallbackRequest().setValue("1");
                        }
                        if (restRequest.getRequestType().equals(RequestType.MOREHEAT)) {
                            extraHeatThisRun.set(true);
                        }
                    });
                });
            }
            this.requestSizeThisRun.getAndAdd(currentRestRequests.size());
        }
        this.extraHeat = extraHeatThisRun.get();

        if (requestSizeThisRun.get() > 0) {
            activateComponents();
        } else {
            deactivateComponents();
        }
        this.getCurrentRequestsChannel().setNextValue(requestSizeThisRun.get());

    }

    private void deactivateComponents() {
        if (this.heatPump != null && this.heatPump.isEnabled()) {
            this.heatPump.setPowerLevel(0);
        }
        if (this.hydraulicLineHeater != null && this.hydraulicLineHeater.isEnabled()) {
            try {
                this.hydraulicLineHeater.enableSignal().setNextWriteValue(false);
            } catch (OpenemsError.OpenemsNamedException ignored) {

            }
        }
    }

    private void activateComponents() {
        if (this.heatPump != null && this.heatPump.isEnabled()) {
            this.heatPump.setPowerLevel(100);
        }
        if (this.hydraulicLineHeater != null && this.hydraulicLineHeater.isEnabled()) {
            try {
                this.hydraulicLineHeater.enableSignal().setNextWriteValue(true);
            } catch (OpenemsError.OpenemsNamedException ignored) {

            }

            if (this.extraHeat) {
                //TODO ACTIVATE && SET THRESHOLD (?)
            } else {
                //TODO DEACTIVATE && DEACTIVATE THRESHOLD (?)
            }
        }
    }

    @Override
    public CommunicationController getCommunicationController() {
        return this.communicationController;
    }
}
