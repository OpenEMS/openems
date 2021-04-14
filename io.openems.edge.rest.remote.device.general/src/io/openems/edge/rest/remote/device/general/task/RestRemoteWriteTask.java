package io.openems.edge.rest.remote.device.general.task;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.rest.communcation.task.RestWriteRequest;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;

public class RestRemoteWriteTask extends AbstractRestRemoteDeviceTask implements RestWriteRequest {

    private WriteChannel<String> value;
    private WriteChannel<Boolean> allowRequest;
    private String lastValue = "Nothing set";
    private boolean hasBeenSet = false;


    public RestRemoteWriteTask(String remoteDeviceId, String realDeviceId,
                               String deviceChannel,  WriteChannel<String> value, String deviceType,
                               WriteChannel<Boolean> allowRequest, Channel<String> unit) {
        super(remoteDeviceId, realDeviceId, deviceChannel,  deviceType, unit);

        this.value = value;
        this.allowRequest = allowRequest;
    }

    /**
     * Creates the msg String for the REST POST Method.
     *
     * @return PostMessage String if Value is defined.
     *
     * <p>If AutoAdapt is active --> Inverse will be considered, either swap true and false or Not.
     * Depending if the Logic is Inverse.
     * If no Value is Defined Return NoValueDefined.
     * If the Value is not readyToWrite return "NotReadyToWrite"
     * </p>
     */
    @Override
    public String getPostMessage() {
        if (readyToWrite()) {

            if (this.value.getNextValue().isDefined()) {
                String msg = "{\"value\":";
                    msg += this.value.getNextValue().get() + "}";
                return msg;

            }
            System.out.println("NoValueDefined");
            return "NoValueDefined";


        }
        System.out.println("NotReadyToWrite");
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
            hasBeenSet = true;
            System.out.println("Was successfully set to " + response);
        } else {
            System.out.println("Error while Posting Value, please try again!");
        }

    }

    @Override
    public boolean readyToWrite() {
        return this.allowRequest.value().get();
    }

    @Override
    public boolean setReadyToWrite(boolean ready) {
        try {
            this.allowRequest.setNextWriteValue(ready);
            return true;
        } catch (OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * This is just for Idle Write Method. If the Value hasn't changed to the last Loop, no POST Method will be generated.
     * Just for performance purposes and not un necessary writes.
     * return true if the Value has changed.
     * &hasBeenSet is set to true if the POST method was successful.
     */
    @Override
    public boolean valueHasChanged() {
        if (this.value.getNextValue().isDefined()) {
            if (this.lastValue.equals(this.value.getNextValue().get()) && hasBeenSet) {
                return false;
            } else {
                this.lastValue = this.value.getNextValue().get();
                hasBeenSet = false;
                return true;
            }
        } else {
            return false;
        }
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
