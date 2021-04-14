package io.openems.edge.controller.heatnetwork.communication.master;


import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;


@ObjectClassDefinition(
        name = "Communication Master",
        description = "Controller to react to External (Heat) Requests and react to them by activating it's devices."
)
@interface Config {
    String service_pid();

    @AttributeDefinition(name = "CommunicationMaster Id", description = "ID of the CommunicationMaster")
    String id() default "CommunicationMaster0";

    @AttributeDefinition(name = "Alias", description = "Human readable Name.")
    String alias() default "";

    @AttributeDefinition(name = "ConnectionType", description = "ConnectionTypes",
            options = {
                    @Option(label = "REST", value = "REST")
            })
    String connectionType();

    @AttributeDefinition(name = "Maximum Requests", description = "Maximum requests handled at once, mapped to Connection type")
    int maxRequestAllowedAtOnce() default 3;

    @AttributeDefinition(name = "Maximum Waiting Time", description = "Maximum Time in minutes an element is Allowed to wait " +
            "before it gets swapped by a member of the activelist")
    int maxWaittimeAllowed() default 30;

    @AttributeDefinition(name = "ManagingType", description = "How To Manage Requests Each Entry will be Mapped to Connection type. "
            + "Available ManageRequests are: FIFO")
    String manageType() default "FIFO";

    @AttributeDefinition(name = "KeepAlive Time", description = "Time needs to past till fallback activates")
    int keepAlive() default 300;

    @AttributeDefinition(name = "Logic on Fallback", description = "What Logic to Execute on Fallback")
    String fallback() default "DEFAULT";

    @AttributeDefinition(name = "RequestMapper", description = "Type in RequestComponent:CallbackComponent:Position:RequestType"
            + "Available RequestTypes are:  HEAT, MOREHEAT, GENERIC")
    String[] requestMap() default "RestRemoteComponent0:RestRemoteComponent1:1:HEAT";


    @AttributeDefinition(name = "ThresholdTemperatureId", description = "Threshold temperature containing a Thermometer and a setPointTemperature")
    String thresholdId() default "ThresholdTemperature0";

    boolean useHydraulicLineHeater() default true;

    @AttributeDefinition(name = "Optional HydraulicLineHeater", description = "Optional Hydraulic LineHeater which activates if more Heat is requests")
    String hydraulicLineHeaterId() default "hydraulicLineHeater0";

    boolean usePump() default true;

    @AttributeDefinition(name = "Optional Pump", description = "An Optional Component which activates additionally")
    String pumpId() default "Pump0";

    @AttributeDefinition(name = "AutoRun", description = "Should the communication and management of Requests run on AutoMode")
    boolean autoRun() default true;

    boolean enabled() default true;

    boolean forceHeating() default false;

    String webconsole_configurationFactory_nameHint() default "CommunicationMaster [{id}]";
}