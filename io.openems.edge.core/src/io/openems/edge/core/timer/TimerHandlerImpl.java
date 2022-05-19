package io.openems.edge.core.timer;

import org.osgi.service.cm.ConfigurationException;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * The TimerHandler makes the use of Timer easier. OpenemsComponents, that wants
 * to use the {@link Timer} should use this, to avoid a bulk of implementation.
 * Components cann add an Identifier to the TimerHandler by giving it the
 * identifier id, the TimerString they want to use and the maxTime Everything's
 * usually from config, except the identifier, this can be a static final
 * string.
 */
public class TimerHandlerImpl implements TimerHandler {

    TimerByTime timerByTime;
    TimerByCounting timerByCounting;
    TimerByCycles timerByCycles;


    private final Map<String, Timer> identifierToTimerMap = new HashMap<>();
    private final String id;

    public TimerHandlerImpl(String id, Timer... timer) {
        this.id = id;
        Arrays.stream(timer).forEach(entry -> {
            if (entry instanceof TimerByTime) {
                timerByTime = (TimerByTime) entry;
            } else if (entry instanceof TimerByCycles) {
                timerByCycles = (TimerByCycles) entry;
            } else if (entry instanceof TimerByCounting) {
                timerByCounting = (TimerByCounting) entry;
            }
        });
    }

    /**
     * This method lets you add an identifier with its configuration to a Timer.
     * After that you can ask the TimerHandler if a time is up or reset an
     * identifier->Timer.
     *
     * @param identifier 1 of n identifier a component can have.
     * @param timer      the {@link TimerType} previously configured.
     * @param maxTime    the max Allowed Time. (InitTime + MaxTime in Seconds)
     * @throws ConfigurationException if Id not an instance of Timer
     */
    @Override
    public void addOneIdentifier(String identifier, TimerType timer, int maxTime)
            throws ConfigurationException {
        var timerToAdd = this.getValidTimer(timer);
        if (timerToAdd != null) {
            this.identifierToTimerMap.put(identifier, timerToAdd);
            timerToAdd.addIdentifierToTimer(this.id, identifier, maxTime);
        }
    }

    /**
     * Private Method to validate the Timer. Checks if the TimerType is correct.
     * The Timer should be added to the TimerHandler before (constructor).
     *
     * @param timer the {@link TimerType}.
     * @return the {@link Timer} if the {@link TimerType} is correct
     * @throws ConfigurationException if Id not an instance of Timer
     */
    private Timer getValidTimer(TimerType timer) throws ConfigurationException {
        switch (timer) {

            case COUNTING:
                return this.timerByCounting;
            case TIME:
                return this.timerByTime;
            case CYCLES:
                return this.timerByCycles;
        }
        throw new ConfigurationException("GetValidTimer: " + this.id, "TimerType is not supported " + timer);
    }

    /**
     * Resets the Timer determined by the Identifier. This will call the
     * {@link Timer#reset(String, String)} indirectly
     *
     * @param identifier the identifier that in combination with the stored
     *                   component id, will be used to determine if the Time for the
     *                   identifier is up.
     */
    @Override
    public void resetTimer(String identifier) {
        this.identifierToTimerMap.get(identifier).reset(this.id, identifier);
    }

    /**
     * Removes all identifier/the component from the timer.
     */
    @Override
    public void removeComponent() {
        this.identifierToTimerMap.forEach((identifier, timer) -> {
            timer.removeComponent(this.id);
        });
    }

    /**
     * Checks if the Time is up equivalent to the
     * {@link Timer#checkIsTimeUp(String, String)} method.
     *
     * @param identifier one of the identifier of the component.
     * @return true if Time is up.
     */
    @Override
    public boolean checkTimeIsUp(String identifier) {
        return this.identifierToTimerMap.get(identifier).checkIsTimeUp(this.id, identifier);
    }

    /**
     * Overrides the Initial Time. Use with caution.
     *
     * @param time           the new initial Time.
     * @param identifierSwap one of the identifier of the component.
     */
    @Override
    public void setInitialTime(Instant time, String identifierSwap) {
        this.identifierToTimerMap.get(identifierSwap).setInitTime(this.id, identifierSwap, time);
    }

    /**
     * Overrides the Initial Time. Use with caution.
     *
     * @param count          new initial SetPoint of the counter.
     * @param identifierSwap one of the identifier of the component.
     */
    @Override
    public void setInitialTime(Integer count, String identifierSwap) {
        this.identifierToTimerMap.get(identifierSwap).setInitTime(this.id, identifierSwap, count);
    }

}
