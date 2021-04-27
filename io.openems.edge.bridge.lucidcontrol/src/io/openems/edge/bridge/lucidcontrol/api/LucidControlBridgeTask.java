package io.openems.edge.bridge.lucidcontrol.api;

public interface LucidControlBridgeTask {
    /**
     * Calculate the Pressure using the given param and setNextValue in the PressureChannel.
     *
     * @param voltageRead voltage Value measured in LucidControlBridge at concrete Device.
     */
    void setResponse(double voltageRead);

    /**
     * Returns the LucidControl Id, not needed yet.
     */
    String getDeviceId();

    /**
     * Returns the LucidControl Module Id. Needed when Module is removed --> remove connected Tasks.
     */
    String getModuleId();


    /**
     * Gets the Path of the LucidControl Module. Used for CommandLine in forever() Method of Bridge.
     */
    String getPath();

    boolean writeTaskDefined();

    String getRequest();

    boolean isRead();

}
