package io.openems.edge.controller.heatnetwork.communication.api;

public interface CommunicationController {
    /**
     * manually
     * Starts a CommunicationController initially.
     */
    void start();

    /**
     * manually
     * Stops the Communicationcontroller.
     */
    void stop();

    /**
     * true if it is not stopped.
     *
     * @return a boolean.
     */
    boolean isRunning();

    /**
     * Returns the ConnectionType.
     *
     * @return the connectiontype.
     */
    ConnectionType connectionType();

    /**
     * If you want to set the controller to Autorun.
     *
     * @param autorun sets to auto.
     */
    void setAutoRun(boolean autorun);

    /**
     * Executes the Specific controller Logic.
     */
    void executeLogic();

    /**
     * Connection ok.?
     *
     * @return connectionState.
     */

    boolean communicationAvailable();

    /**
     * The ConnectionType of the Controller
     * @return the ConnectionType.
     */
    ConnectionType getConnectionType();

    RequestManager getRequestManager();

    void setMaxWaittime(int maxWaittime);
}
