package io.openems.edge.remote.rest.device.task;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.communication.remote.rest.api.RestReadRequest;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.ComponentManager;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This Task gets the value of a remote Channel and sets the value into the ValueReadChannel of the RestRemoteDevice.
 * The GetRequest will be handled by the RestBridge.
 */
public class RestRemoteReadTask extends AbstractRestRemoteDeviceTask implements RestReadRequest {


    private final ChannelAddress value;

    public RestRemoteReadTask(String remoteDeviceId, String realDeviceId, String deviceChannel,
                              ChannelAddress value, Logger log, ComponentManager cpm) {

        super(remoteDeviceId, realDeviceId, deviceChannel, log, cpm);
        this.value = value;
    }

    /**
     * Called by the Rest Bridge sets answer after successful REST Communication.
     *
     * @param succ   declares successful communication.
     * @param answer the REST Response from the GET Method.
     */
    @Override
    public void setResponse(boolean succ, String answer) {

        if (succ) {
            this.setResponseValue(answer);
        }
    }

    /**
     * Sets the Value of the REST GET Method.
     *
     * @param answer REST response
     *               <p>Get only Number Value and set that value to the Value of the Remote Device.
     *               Splits after "value" and get the substring of 0 and "\"" ---> only the Value number Part will be
     *               considered. ---> Get Only Numbers (with optional floatingpoint) .
     *               Example: {"value": 1023.42345, "Unit":WATT}
     *               split at "value" till first " is found
     *               temp result=  :1023.42345, "
     *               only compile numbers and the decimal .
     *               --> result is 1023.42345
     *               will be written into channel
     *               NOTE: ONLY NUMBERS ARE POSSIBLE WITH THIS METHOD! NO STRINGS
     *               </p>
     */
    private void setResponseValue(String answer) {
        String[] parts = answer.split("\"value\"");
        if (parts.length <= 1) {
            return;
        }
        boolean nullValue = Arrays.asList(parts).get(1).contains("null");
        if (nullValue) {
            return;
        }
        String newParts = parts[1];
        Pattern p = Pattern.compile("[-+]?([0-9]*[.][0-9]+|[0-9]+)");
        Matcher m = p.matcher(newParts);
        StringBuilder answerNumeric = new StringBuilder();
        while (m.find()) {
            answerNumeric.append(m.group());
        }
        if (!answerNumeric.toString().equals("")) {
            try {
                this.getCpm().getChannel(this.value).setNextValue(answerNumeric);
            } catch (OpenemsError.OpenemsNamedException e) {
                this.getLogger().warn("This error shouldn't occur, this is it's own Channel: " + this.getDeviceId());
            }
        }
    }

}
