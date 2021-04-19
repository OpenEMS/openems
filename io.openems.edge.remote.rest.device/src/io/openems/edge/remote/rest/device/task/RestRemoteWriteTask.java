package io.openems.edge.remote.rest.device.task;

import io.openems.edge.bridge.communication.remote.rest.api.RestWriteRequest;
import io.openems.edge.common.channel.WriteChannel;
import org.slf4j.Logger;

public class RestRemoteWriteTask extends AbstractRestRemoteDeviceTask implements RestWriteRequest {

    private final WriteChannel<String> value;
    private final WriteChannel<Boolean> allowRequest;


    public RestRemoteWriteTask(String remoteDeviceId, String realDeviceId,
                               String deviceChannel, WriteChannel<String> value,
                               WriteChannel<Boolean> allowRequest, Logger log) {
        super(remoteDeviceId, realDeviceId, deviceChannel, log);

        this.value = value;
        this.allowRequest = allowRequest;
    }

    /**
     * Creates the msg String for the REST POST Method.
     *
     * @return <p>PostMessage String if Value is defined.
     * If no Value is Defined Return NoValueDefined.
     * If the Value is not readyToWrite return "NotReadyToWrite".</p>
     */
    @Override
    public String getPostMessage() {
        if (this.readyToWrite()) {

            if (this.value.getNextValue().isDefined()) {
                String msg = "{\"value\":";
                msg += this.value.getNextValue().get() + "}";
                return msg;

            }
            return "NoValueDefined";


        }
        return "NotReadyToWrite";
    }

    /**
     * If POST Method was successful, hasBeenset = true and print the success, otherwise print failure.
     *
     * @param succ     successful POST call.
     * @param response Response of the REST Method.
     */
    @Override
    public void wasSuccess(boolean succ, String response) {
        if (succ) {
            super.getLogger().info("The Device corresponding to this device: " + super.getDeviceId() + "Was successfully set " + response);
        } else {
            super.getLogger().warn("Error while Posting Value, please try again! " + super.getDeviceId());
        }

    }

    /**
     * Checks if the Component is Ready To Write/ Allowed to Write (Write false in Channel ALLOW_REQUEST).
     *
     * @return a boolean.
     */
    @Override
    public boolean readyToWrite() {
        return this.allowRequest.value().get();
    }

    /**
     * Updates the Channel.
     */
    @Override
    public void nextValueSet() {

        if (this.allowRequest.getNextWriteValue().isPresent()) {
            this.allowRequest.setNextValue(this.allowRequest.getNextWriteValueAndReset());
        }
        if (this.value.getNextWriteValue().isPresent()) {
            this.value.setNextValue(this.value.getNextWriteValueAndReset());
        }

    }
}
