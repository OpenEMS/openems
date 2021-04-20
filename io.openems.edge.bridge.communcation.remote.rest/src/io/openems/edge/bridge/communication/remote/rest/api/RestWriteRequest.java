package io.openems.edge.bridge.communication.remote.rest.api;

/**
 * The Interface of the Write Tasks. It allows the Bridge to get the PostMessage and check if the task is allowed to do
 * it's task (write it's value to remote device).
 */

public interface RestWriteRequest extends RestRequest {
    /**
     * Creates the msg String for the REST POST Method.
     *
     * @return PostMessage String if Value is defined.
     */
    String getPostMessage();

    /**
     * If POST Method was successful, hasBeenSet = true and print the success, otherwise print failure.
     *
     * @param succ     successful POST call.
     * @param response Response of the REST Method.
     */

    void wasSuccess(boolean succ, String response);

    /**
     * Checks if the Component is Ready To Write/ Allowed to Write (Write false in Channel ALLOW_REQUEST).
     *
     * @return a boolean.
     */

    boolean allowedToWrite();
}
