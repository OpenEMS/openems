package io.openems.edge.lucidcontrol.api;

import io.openems.edge.lucidcontrol.bridge.LucidControlBridgeImpl;

/**
 * The LucidControlBridgeTask. This is implemented by the Input and Output tasks holding ChannelAddresses as well as
 * Request Strings to Read and Write to/from LucidControlDevices using a shell command + the LucidControl Software.
 */
public interface LucidControlBridgeTask {
    /**
     * Calculate the Pressure using the given param and setNextValue in the PressureChannel.
     *
     * @param voltageRead voltage Value measured in LucidControlBridge at concrete Device.
     */
    void setResponse(double voltageRead);

    /**
     * Returns the LucidControl Module Id. Needed when Module is removed --> remove connected Tasks.
     *
     * @return the ModuleId the Device/Task ist connected to.
     */
    String getModuleId();

    /**
     * Gets the Path of the LucidControl Module. Used for CommandLine in
     * {@link LucidControlBridgeImpl} Method of Bridge.
     *
     * @return the Path where the Module is connected to (usually dev/ttyACM0...x)
     */
    String getPath();

    /**
     * Tells the LucidControlBridge if the Task is a OutputTask and a PercentValue is defined.
     *
     * @return true if OutputTask.
     */
    boolean isWriteDefined();

    /**
     * Gets the Path / Information that the Bridge needs to call the LucidControl Software and read the Input/write Output.
     *
     * @return the Request / Shell String input
     */
    String getRequest();

    /**
     * Tells the Bridge if this Task is a Read Task.
     *
     * @return true if readTask.
     */

    boolean isRead();

}
