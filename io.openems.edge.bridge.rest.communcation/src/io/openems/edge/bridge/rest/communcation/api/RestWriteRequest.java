package io.openems.edge.bridge.rest.communcation.api;

public interface RestWriteRequest extends RestRequest {
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
    String getPostMessage();

    /**
     * If POST Method was successful, hasBeenset = true and print the success, otherwise print failure.
     *
     * @param succ     successful POST call.
     * @param response Response of the REST Method.
     */

    void wasSuccess(boolean succ, String response);

    boolean readyToWrite();

    /**
     * Updates the Channel.
     */
    void nextValueSet();

    //allow Requests
    boolean setReadyToWrite(boolean ready);

    /**
     * This is just for Idle Write Method. If the Value hasn't changed to the last Loop, no POST Method will be generated.
     * Just for performance purposes and not un necessary writes.
     * return true if the Value has changed.
     * &hasBeenSet is set to true if the POST method was successful.
     *
     * @return boolean true if value Has been Changed to last time.
     */

    boolean valueHasChanged();
}
