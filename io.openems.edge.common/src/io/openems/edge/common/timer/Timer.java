package io.openems.edge.common.timer;

/**
 * The Timer interface. Provides easy access to a timer.
 * 
 * @see io.openems.edge.core.timer.TimerHandler
 */
public interface Timer {

    /**
     * check if the Time is up.
     * 
     * @return true if the time is up, false else
     */
    public boolean check();

    /**
     * Restarts the Timer.
     * 
     * @implNote method will remove a configured startDelay immediately.
     */
    public void reset();

    /**
     * check and restart the timer, when the timer is up.
     * 
     * @return when the timer is up method will return true once, in that case the
     *         timer is restarted. In any other case method returns false.
     */
    public default boolean checkAndReset() {
	if (this.check()) {
	    this.reset();
	    return true;
	}
	return false;
    }
}
