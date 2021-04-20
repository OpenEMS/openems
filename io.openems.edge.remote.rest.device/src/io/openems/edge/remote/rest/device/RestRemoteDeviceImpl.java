package io.openems.edge.remote.rest.device;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.communication.remote.rest.api.RestBridge;
import io.openems.edge.bridge.communication.remote.rest.api.RestRequest;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.remote.rest.device.api.RestRemoteDevice;
import io.openems.edge.remote.rest.device.task.RestRemoteReadTask;
import io.openems.edge.remote.rest.device.task.RestRemoteWriteTask;
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

/**
 * A Remote Device Communicating via REST.
 * One can configure a Channel to get Information from / write into.
 * Note: ATM Only Numeric Values are possible to read from!
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Rest.Remote.Device", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class RestRemoteDeviceImpl extends AbstractOpenemsComponent implements OpenemsComponent, RestRemoteDevice {

    private final Logger log = LoggerFactory.getLogger(RestRemoteDeviceImpl.class);

    @Reference
    ComponentManager cpm;

    private RestBridge restBridge;
    private boolean isRead;

    public RestRemoteDeviceImpl() {

        super(OpenemsComponent.ChannelId.values(),
                RestRemoteDevice.ChannelId.values());
    }

    /**
     * Activates the Component, get the Rest Bridge and add the RestRemoteDevice.
     *
     * @param context the context of the Component.
     * @param config  the Config.
     * @throws ConfigurationException             if the Rest Bridge is incorrect or the Connection gets an Error.
     * @throws OpenemsError.OpenemsNamedException if the Id is no available.
     */
    @Activate
    public void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        if (config.enabled()) {

            if (this.cpm.getComponent(config.restBridgeId()) instanceof RestBridge) {
                this.restBridge = this.cpm.getComponent(config.restBridgeId());

                this.restBridge.addRestRequest(super.id(), this.createNewTask(config.deviceChannel(),
                        config.id(), config.realDeviceId(), config.deviceMode()));
            } else {
                throw new ConfigurationException(config.restBridgeId(), "Master Slave Id Incorrect or not configured yet!");
            }
            this.getAllowRequestChannel().setNextValue(true);
        }
    }

    /**
     * This Method creates a RestRequest with it's given parameters coming from config. Usually called by the @Activate
     *
     * @param deviceChannel  usually from Config, declares the Channel of the actual Device you want to access.
     * @param remoteDeviceId usually from Config, is the Remote Device Id.
     * @param realDeviceId   usually from Config, is the Unique Id of the Device you want to access.
     * @param deviceMode     usually from Config, declares if you want to Read or Write.
     * @return RestRequest if creation of Instance was successful.
     * @throws ConfigurationException if TemperatureSensor is set to Write; or if an impossible Case occurs (deviceMode neither Read/Write)
     */
    private RestRequest createNewTask(String deviceChannel, String remoteDeviceId,
                                      String realDeviceId, String deviceMode) throws ConfigurationException, OpenemsError.OpenemsNamedException {

        RestRequest task;
        if (deviceMode.equals("Write")) {

            this.getTypeSetChannel().setNextValue("Write");
            this.isRead = false;
            task = new RestRemoteWriteTask(remoteDeviceId, realDeviceId, deviceChannel,
                    ChannelAddress.fromString(super.id() + "/" + this.getWriteValueChannel().channelId().id()),
                    ChannelAddress.fromString(super.id() + "/" + this.getAllowRequestChannel().channelId().id()),
                    this.log, this.cpm);
            return task;

        } else if (deviceMode.equals("Read")) {
            this.getTypeSetChannel().setNextValue("Read");
            this.isRead = true;
            //String deviceId, String masterSlaveId, boolean master, String realTemperatureSensor, Channel<Integer> temperature
            task = new RestRemoteReadTask(remoteDeviceId, realDeviceId, deviceChannel,
                    ChannelAddress.fromString(super.id() + "/" + this.getReadValueChannel().channelId().id()), this.log, this.cpm);
            return task;
        }

        throw new ConfigurationException("Impossible Error", "Error shouldn't Occur because of Fix options");
    }


    /**
     * SetsValue of Remote Device, if Remote Device TypeSet is set to "Write".
     *
     * @param value Value which will be Written to Device configured by the Remote Device.
     */
    @Override
    public void setValue(String value) {
        if (this.getTypeSetChannel().getNextValue().isDefined() == false) {
            this.log.warn("The Type of the Remote Device: " + super.id() + " is not available yet");
            return;
        }
        if (this.getTypeSetChannel().getNextValue().get().equals("Read")) {
            this.log.warn("Can't write into ReadTasks: " + super.id());
        }

        try {
            this.getWriteValueChannel().setNextWriteValue(value);
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't write the Value for : " + super.id());
        }

    }

    /**
     * Gets the Current Value of the Remote device depending if it's either read or write.
     *
     * @return the Value as a String.
     */
    @Override
    public String getValue() {
        if (this.getTypeSetChannel().value().get().equals("Write")) {
            if (this.getWriteValueChannel().value().isDefined()) {
                return this.getWriteValueChannel().value().get();
            } else {
                return "Write Value not available yet for " + super.id();
            }
        } else if (this.getReadValueChannel().value().isDefined()) {
            return this.getReadValueChannel().value().get();
        }
        return "Read Value not available yet";
    }

    /**
     * Get the Unique Id.
     *
     * @return the Id.
     */

    @Override
    public String getId() {
        return this.id();
    }

    /**
     * Check if this Device is a Write Remote Device.
     *
     * @return a boolean.
     */

    @Override
    public boolean isWrite() {
        return this.isRead == false;
    }

    /**
     * Checks if this Device is a Read Remote Device.
     *
     * @return a boolean.
     */

    @Override
    public boolean isRead() {
        return this.isRead;
    }

    /**
     * Checks/Asks if the Connection via Rest is ok.
     *
     * @return a boolean.
     */

    @Override
    public boolean connectionOk() {
        return this.restBridge.connectionOk();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof RestRemoteDeviceImpl) {
            RestRemoteDeviceImpl other = (RestRemoteDeviceImpl) o;
            return other.id().equals(this.id());
        }

        return false;
    }

    /**
     * Deactivates the component and removes it's task from the bridge.
     */
    @Deactivate
    public void deactivate() {
        this.restBridge.removeRestRemoteDevice(super.id());
        super.deactivate();
    }
}
