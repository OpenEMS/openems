package io.openems.edge.rest.remote.device.general;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.rest.communcation.api.RestBridge;
import io.openems.edge.bridge.rest.communcation.task.RestRequest;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.rest.remote.device.general.api.RestRemoteChannel;
import io.openems.edge.rest.remote.device.general.api.RestRemoteDevice;
import io.openems.edge.rest.remote.device.general.task.RestRemoteReadTask;
import io.openems.edge.rest.remote.device.general.task.RestRemoteWriteTask;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Rest.Remote.Device", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class RestRemoteDeviceImpl extends AbstractOpenemsComponent implements OpenemsComponent, RestRemoteDevice, RestRemoteChannel {

    @Reference
    ComponentManager cpm;

    private RestBridge restBridge;

    private String restBridgeId;
    private RestRequest task;
    private boolean isRead;

    public RestRemoteDeviceImpl() {

        super(OpenemsComponent.ChannelId.values(),
                RestRemoteChannel.ChannelId.values());
    }


    @Activate
    public void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        if (config.enabled()) {

            if (cpm.getComponent(config.restBridgeId()) instanceof RestBridge) {
                restBridge = cpm.getComponent(config.restBridgeId());
                this.restBridgeId = config.restBridgeId();

                restBridge.addRestRequest(super.id(), createNewTask(config.deviceType(), config.deviceChannel(),
                        config.id(), config.realDeviceId(), config.deviceMode()));
            } else {
                throw new ConfigurationException(config.restBridgeId(), "Master Slave Id Incorrect or not configured yet!");
            }
            this.getAllowRequest().setNextValue(true);
            this.getUnit().setNextValue(config.deviceUnit());
        }
    }

    /**
     * This Method creates a RestRequest with it's given parameters coming from config. Usually called by the @Activate
     *
     * @param deviceType     usually from Config, declares the DeviceType e.g. Temperature or Relays.
     * @param deviceChannel  usually from Config, declares the Channel of the actual Device you want to access.
     * @param remoteDeviceId usually from Config, is the Remote Device Id.
     * @param realDeviceId   usually from Config, is the Unique Id of the Device you want to access.
     * @param deviceMode     usually from Config, declares if you want to Read or Write.
     * @return RestRequest if creation of Instance was successful.
     * @throws ConfigurationException if TemperatureSensor is set to Write; or if an impossible Case occurs (deviceMode neither Read/Write)
     */
    private RestRequest createNewTask(String deviceType, String deviceChannel, String remoteDeviceId,
                                      String realDeviceId, String deviceMode) throws ConfigurationException {

        if (deviceMode.equals("Write")) {

            if (deviceType.toLowerCase().equals("temperaturesensor")) {
                throw new ConfigurationException("TemperatureSensor write not allowed", "Warning!"
                        + " TemperatureSensor does not support Write Tasks!");
            } else {
                this.getTypeSet().setNextValue("Write");
                this.isRead = false;
                task = new RestRemoteWriteTask(remoteDeviceId, realDeviceId, deviceChannel, getWriteValue(),
                        deviceType, this.getAllowRequest(), this.getUnit());
                return task;
            }
        } else if (deviceMode.equals("Read")) {
            this.getTypeSet().setNextValue("Read");
            this.isRead = true;
            //String deviceId, String masterSlaveId, boolean master, String realTemperatureSensor, Channel<Integer> temperature
            task = new RestRemoteReadTask(remoteDeviceId, realDeviceId, deviceChannel,
                    getReadValue(), deviceType, this.getUnit());
            return task;
        }

        throw new ConfigurationException("Impossible Error", "Error shouldn't Occur because of Fix options");
    }


    @Deactivate
    public void deactivate() {
        super.deactivate();
        restBridge.removeRestRemoteDevice(super.id());
    }

    @Override
    public String debugLog() {
        if (restBridge.getRemoteRequest(super.id()) != null) {
            return task.getDeviceType() + " " + this.getValue() + " of " + super.id() + " \n";
        }
        return "";
    }

    /**
     * SetsValue of Remote Device, if Remote Device TypeSet is set to "Write".
     *
     * @param value Value which will be Written to Device configured by the Remote Device.
     * @return boolean depending if setNextWriteValue was successful or not (and depending if TypeSet is Read or Write).
     */
    @Override
    public boolean setValue(String value) {
        if (!this.getTypeSet().getNextValue().isDefined()) {
            System.out.println("Not Defined Yet");
            return false;
        }
        if (this.getTypeSet().getNextValue().get().equals("Read")) {
            System.out.println("Can't write into ReadTasks");
        }

        try {
            this.getWriteValue().setNextWriteValue(value);
        } catch (OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }

    @Override
    public String getValue() {
        if (this.getTypeSet().value().get().equals("Write")) {
            if (this.getWriteValue().value().isDefined()) {
                if (this.getUnit().value().get().equals("None")) {
                    return this.getWriteValue().value().get();
                }
                return this.getWriteValue().value().get() + " " + this.getUnit().value().get();
            } else {
                return "Value not available yet!";
            }
        } else if (this.getReadValue().value().isDefined()) {
            if (this.getUnit().value().get().equals("None")) {
                return this.getReadValue().value().get();
            }
            return this.getReadValue().value().get() + " " + this.getUnit().value().get();
        }
        return "Read Value not available yet";
    }

    @Override
    public String getType() {
        return this.getTypeSet().value().get();
    }

    @Override
    public boolean setAllowRequest(boolean allow) {
        try {
            this.getAllowRequest().setNextWriteValue(allow);
            return true;
        } catch (OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String getRemoteUnit() {
        return this.getUnit().getNextValue().get();
    }

    @Override
    public String getId() {
        return this.id();
    }

    @Override
    public boolean isWrite() {
        return !this.isRead;
    }

    @Override
    public boolean isRead() {
        return this.isRead;
    }

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
}
