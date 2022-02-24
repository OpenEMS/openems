package io.openems.edge.lucidcontrol.api;


import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.lucidcontrol.device.LucidControlInputDeviceImpl;
import io.openems.edge.lucidcontrol.device.LucidControlOutputDeviceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The AbstractLucidControlBridgeTask. It holds the Logger, module ID and the cpm. Used by
 * {@link LucidControlInputDeviceImpl} and
 * {@link LucidControlOutputDeviceImpl}
 */
public abstract class AbstractLucidControlBridgeTask implements LucidControlBridgeTask {

    protected Logger log = LoggerFactory.getLogger(AbstractLucidControlBridgeTask.class);

    private final String moduleId;

    protected ComponentManager cpm;


    public AbstractLucidControlBridgeTask(String moduleId, ComponentManager cpm) {
        this.moduleId = moduleId;
        this.cpm = cpm;
    }


    /**
     * Returns the LucidControl Module Id.
     * Needed when Module is removed --> remove connected Tasks.
     *
     * @return the ModuleId the Device/Task ist connected to.
     */

    public String getModuleId() {
        return this.moduleId;
    }

    /**
     * Calculate the Pressure using the given param and setNextValue in the PressureChannel.
     *
     * @param voltageRead voltage Value measured in LucidControlBridge at concrete Device.
     */
    public void setResponse(double voltageRead){
        //Nothing to do on default
    }

}
