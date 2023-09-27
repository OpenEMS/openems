package io.openems.edge.core.timer;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.timer.Timer;

/**
 * TimerManager - This package provides easy access to different kind of timers.
 * Use it to
 * 
 * <ul>
 * <li>getTimerCount() - count each call to the timer.check() method.
 * <li>getTimerCoreCycles() - count each core cycle.
 * <li>getTimerTimer() - count a given time in s.
 * </ul>
 * 
 */
public interface TimerManager extends OpenemsComponent {

    public static final String SINGLETON_SERVICE_PID = "Core.TimerManager";
    public static final String SINGLETON_COMPONENT_ID = "_timermanager";

    /**
     * Get a timer to count every call to the timer.check() or the
     * timer.checkAndRestart() method.
     * 
     * @param countCheckCalls the count to use.
     * 
     * @return true if the given count has been reached, false else
     */
    public default Timer getTimerByCount(int countCheckCalls) {
	return this.getTimerByCount(null, countCheckCalls, 0);
    }

    /**
     * Get a timer to count every call to the timer.check() or the
     * timer.checkAndRestart() method.
     * 
     * @param countCheckCalls  the count to use.
     * @param startDelayInSecs the timer will return false, as long as the
     *                         startDelayInSecs is not reached. This is especially
     *                         useful for heaters which are very sensitive against
     *                         frequent condition changes (can happen in systems
     *                         which restart OpenEMS frequently).
     * @return true if the given count has been reached, false else
     */
    public default Timer getTimerByCount(int countCheckCalls, int startDelayInSecs) {
	return this.getTimerByCount(null, countCheckCalls, startDelayInSecs);
    }

    /**
     * Get a timer to count every call to the timer.check() or the
     * timer.checkAndRestart() method.
     * 
     * @param channel         if this channel is provided, this channel is used to
     *                        hold the state of the timer.
     * @param countCheckCalls the count to use.
     * @return true if the given count has been reached, false else
     */
    public default Timer getTimerByCount(Channel<Integer> channel, int countCheckCalls) {
	return this.getTimerByCount(channel, countCheckCalls, 0);
    }

    /**
     * Get a timer to count every call to the timer.check() or the
     * timer.checkAndRestart() method.
     * 
     * @param channel          if this channel is provided, this channel is used to
     *                         hold the state of the timer.
     * @param countCheckCalls  the count to use.
     * @param startDelayInSecs the timer will return false, as long as the
     *                         startDelayInSecs is not reached. This is especially
     *                         useful for heaters which are very sensitive against
     *                         frequent condition changes (can happen in systems
     *                         which restart OpenEMS frequently).
     * @return true if the given count has been reached, false else
     */
    public Timer getTimerByCount(Channel<Integer> channel, int countCheckCalls, int startDelayInSecs);

    /**
     * Get a timer to count every core cycle.
     * 
     * @param count the count to use.
     * 
     * @return true if the given count has been reached, false else
     */
    public default Timer getTimerByCoreCycles(int count) {
	return this.getTimerByCoreCycles(null, count, 0);
    }

    /**
     * Get a timer to count every core cycle.
     * 
     * @param count            the count to use.
     * @param startDelayInSecs the timer will return false, as long as the
     *                         startDelayInSecs is not reached. This is especially
     *                         useful for heaters which are very sensitive against
     *                         frequent condition changes (can happen in systems
     *                         which restart OpenEMS frequently).
     * @return true if the given count has been reached, false else
     */
    public default Timer getTimerByCoreCycles(int count, int startDelayInSecs) {
	return this.getTimerByCoreCycles(null, count, startDelayInSecs);
    }

    /**
     * Get a timer to count every core cycle.
     * 
     * @param channel if this channel is provided, this channel is used to hold the
     *                state of the timer.
     * @param count   the count to use.
     * @return true if the given count has been reached, false else
     */
    public default Timer getTimerByCoreCycles(Channel<Integer> channel, int count) {
	return this.getTimerByCoreCycles(channel, count, 0);
    }

    /**
     * Get a timer to count every core cycle.
     * 
     * @param channel          if this channel is provided, this channel is used to
     *                         hold the state of the timer.
     * @param count            the count to use.
     * @param startDelayInSecs the timer will return false, as long as the
     *                         startDelayInSecs is not reached. This is especially
     *                         useful for heaters which are very sensitive against
     *                         frequent condition changes (can happen in systems
     *                         which restart OpenEMS frequently).
     * @return true if the given count has been reached, false else
     */
    public Timer getTimerByCoreCycles(Channel<Integer> channel, int count, int startDelayInSecs);

    /**
     * Get a timer to count the time in seconds.
     * 
     * @param seconds the count in seconds to use.
     * 
     * @return true if the given count has been reached, false else
     */
    public default Timer getTimerByTime(int seconds) {
	return this.getTimerByTime(null,  seconds, 0);
    }

    /**
     * Get a timer to count the time in seconds.
     * 
     * @param seconds          the count in seconds to use.
     * @param startDelayInSecs the timer will return false, as long as the
     *                         startDelayInSecs is not reached. This is especially
     *                         useful for heaters which are very sensitive against
     *                         frequent condition changes (can happen in systems
     *                         which restart OpenEMS frequently).
     * @return true if the given count has been reached, false else
     */
    public default Timer getTimerByTime(int seconds, int startDelayInSecs) {
	return this.getTimerByTime(null,  seconds, startDelayInSecs);
    }

    /**
     * Get a timer to count the time in seconds.
     * 
     * @param channel if this channel is provided, this channel is used to hold the
     *                state of the timer.
     * @param seconds the count in seconds to use.
     * @return true if the given count has been reached, false else
     */
    public default Timer getTimerByTime(Channel<Integer> channel, int seconds) {
	return this.getTimerByTime(channel,  seconds, 0);
    }

    /**
     * Get a timer to count the time in seconds.
     * 
     * @param channel          if this channel is provided, this channel is used to
     *                         hold the state of the timer.
     * @param seconds          the count in seconds to use.
     * @param startDelayInSecs the timer will return false, as long as the
     *                         startDelayInSecs is not reached. This is especially
     *                         useful for heaters which are very sensitive against
     *                         frequent condition changes (can happen in systems
     *                         which restart OpenEMS frequently).
     * @return true if the given count has been reached, false else
     */
    public Timer getTimerByTime(Channel<Integer> channel, int seconds, int startDelayInSecs);
}
