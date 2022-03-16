package io.openems.edge.core.timer;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.cm.ConfigurationException;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.timer.Timer;

/**
 * The TimerHandler makes the use of Timer easier. OpenemsComponents, that wants
 * to use the {@link Timer} should use this, to avoid a bulk of implementation.
 * Components cann add an Identifier to the TimerHandler by giving it the
 * identifier id, the TimerString they want to use and the maxTime Everything's
 * usually from config, except the identifier, this can be a static final
 * string.
 */
public class TimerHandlerImpl implements TimerHandler {
	private final Map<String, Timer> identifierToTimerMap = new HashMap<>();
	private final ComponentManager cpm;
	private final String id;

	public TimerHandlerImpl(String id, ComponentManager cpm, Map<String, String> identifierToTimerIdStringMap,
			Map<String, Integer> identifierToMaxTime)
			throws ConfigurationException, OpenemsError.OpenemsNamedException {
		this.id = id;
		this.cpm = cpm;
		OpenemsError.OpenemsNamedException[] ex = { null };
		ConfigurationException[] exConfig = { null };
		identifierToTimerIdStringMap.forEach((identifier, timer) -> {
			if (ex[0] == null && exConfig[0] == null) {
				try {
					this.identifierToTimerMap.put(identifier, this.getValidTimer(timer));
				} catch (OpenemsError.OpenemsNamedException e) {
					ex[0] = e;
				} catch (ConfigurationException e) {
					exConfig[0] = e;
				}
			}
		});
		if (ex[0] != null) {
			throw ex[0];
		}
		if (exConfig[0] != null) {
			throw exConfig[0];
		}
		this.identifierToTimerMap.forEach((identifier, timer) -> {
			timer.addIdentifierToTimer(this.id, id, identifierToMaxTime.get(identifier));
		});

	}

	public TimerHandlerImpl(String id, ComponentManager cpm)
			throws OpenemsError.OpenemsNamedException, ConfigurationException {
		this(id, cpm, new HashMap<>(), new HashMap<>());
	}

	/**
	 * This method let's you add an identifier with it's configuration to a Timer.
	 * After that you can ask the TimerHandler if a time is up or reset a
	 * identifier->Timer.
	 *
	 * @param identifier 1 of n identifier a component can have.
	 * @param timer      the Id of the {@link Timer} previously configured
	 * @param maxTime    the max Allowed Time. (InitTime + MaxTime in Seconds)
	 * @throws OpenemsError.OpenemsNamedException if timer cannot be found
	 * @throws ConfigurationException             if Id not an instance of Timer
	 */
	@Override
	public void addOneIdentifier(String identifier, String timer, int maxTime)
			throws OpenemsError.OpenemsNamedException, ConfigurationException {
		var timerToAdd = this.getValidTimer(timer);
		this.identifierToTimerMap.put(identifier, timerToAdd);
		timerToAdd.addIdentifierToTimer(this.id, identifier, maxTime);
	}

	/**
	 * Private Method to validate the Timer. Checks if the ID is correct and if the
	 * component is an instance of a Timer.
	 *
	 * @param timer the TimerId
	 * @return the Timer if id is correct.
	 * @throws ConfigurationException             if Id not an instance of Timer
	 * @throws OpenemsError.OpenemsNamedException if id could not be found.
	 */
	private Timer getValidTimer(String timer) throws ConfigurationException, OpenemsError.OpenemsNamedException {
		var component = this.cpm.getComponent(timer);
		if (component instanceof Timer) {
			return (Timer) component;
		}
		throw new ConfigurationException("GetValidTimer: " + this.id, "TimerID not an instance of Timer " + timer);
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
