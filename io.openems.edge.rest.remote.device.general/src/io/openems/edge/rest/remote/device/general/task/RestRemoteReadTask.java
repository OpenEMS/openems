package io.openems.edge.rest.remote.device.general.task;

import io.openems.edge.bridge.communication.remote.rest.api.RestReadRequest;
import io.openems.edge.common.channel.Channel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestRemoteReadTask extends AbstractRestRemoteDeviceTask implements RestReadRequest {


    private Channel<String> value;

    public RestRemoteReadTask(String remoteDeviceId, String realDeviceId, String deviceChannel,
                               Channel<String> value, String deviceType, Channel<String> unit) {

        super(remoteDeviceId, realDeviceId, deviceChannel, deviceType, unit);
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
            setResponseValue(answer);
            if (!super.unitWasSet()) {
                super.setUnit(true, answer);
            }
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
        String newParts = parts[1].substring(0, parts[1].indexOf("\""));
        Pattern p = Pattern.compile("[-+]?([0-9]*[.][0-9]+|[0-9]+)");
        Matcher m = p.matcher(newParts);
        StringBuilder answerNumeric = new StringBuilder();
        while (m.find()) {
            answerNumeric.append(m.group());
        }
        if (!answerNumeric.toString().equals("")) {
            this.value.setNextValue(answerNumeric);
        }
    }

}
