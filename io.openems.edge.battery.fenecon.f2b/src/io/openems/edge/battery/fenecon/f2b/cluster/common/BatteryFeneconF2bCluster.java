package io.openems.edge.battery.fenecon.f2b.cluster.common;

import java.util.List;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.f2b.BatteryFeneconF2b;
import io.openems.edge.battery.fenecon.f2b.cluster.parallel.BatteryFeneconF2bClusterParallelImpl;
import io.openems.edge.battery.fenecon.f2b.cluster.serial.BatteryFeneconF2bClusterSerialImpl;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;

public interface BatteryFeneconF2bCluster extends BatteryFeneconF2b, Battery, OpenemsComponent, StartStoppable {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		MAX_START_ATTEMPTS_FAILED(Doc.of(Level.WARNING) //
				.text("The maximum number of start attempts failed")), //
		MAX_STOP_ATTEMPTS_FAILED(Doc.of(Level.WARNING) //
				.text("The maximum number of stop attempts failed")), //
		TIMEOUT_START_BATTERIES(Doc.of(Level.FAULT) //
				.text("The maximum start time is passed")), //
		TIMEOUT_STOP_BATTERIES(Doc.of(Level.FAULT) //
				.text("The maximum stop time is passed")), //
		TIMEOUT_WAIT_FOR_BATTERIES_STATUS(Doc.of(Level.FAULT) //
				.text("The maximum start time is passed; waiting for batteries status!")), //
		AT_LEAST_ONE_BATTERY_NOT_RUNNING(Doc.of(Level.FAULT) //
				.text("At least one battery is not running!")),
		AT_LEAST_ONE_BATTERY_NOT_STOPPED(Doc.of(Level.FAULT) //
				.text("At least one battery is not stopped!")), //
		AT_LEAST_ONE_BATTERY_IN_ERROR(Doc.of(Level.FAULT) //
				.text("At least one battery is in error!")), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_START_ATTEMPTS_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getMaxStartAttemptsFailedChannel() {
		return this.channel(ChannelId.MAX_START_ATTEMPTS_FAILED);
	}

	/**
	 * Gets the {@link StateChannel} for
	 * {@link ChannelId#MAX_START_ATTEMPTS_FAILED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStartAttemptsFailed() {
		return this.getMaxStartAttemptsFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_START_ATTEMPTS_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxStartAttemptsFailed(Boolean value) {
		this.getMaxStartAttemptsFailedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_STOP_ATTEMPTS_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getMaxStopAttemptsFailedChannel() {
		return this.channel(ChannelId.MAX_STOP_ATTEMPTS_FAILED);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#MAX_STOP_ATTEMPTS_FAILED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStopAttemptsFailed() {
		return this.getMaxStopAttemptsFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_STOP_ATTEMPTS_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxStopAttemptsFailed(Boolean value) {
		this.getMaxStopAttemptsFailedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TIMEOUT_START_BATTERIES}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getTimeoutStartBatteriesChannel() {
		return this.channel(ChannelId.TIMEOUT_START_BATTERIES);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#TIMEOUT_START_BATTERIES}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getTimeoutStartBatteries() {
		return this.getTimeoutStartBatteriesChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#TIMEOUT_START_BATTERIES} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setTimeoutStartBatteries(Boolean value) {
		this.getTimeoutStartBatteriesChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TIMEOUT_STOP_BATTERIES}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getTimeoutStopBatteriesChannel() {
		return this.channel(ChannelId.TIMEOUT_STOP_BATTERIES);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#TIMEOUT_STOP_BATTERIES}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getTimeoutStopBatteries() {
		return this.getTimeoutStopBatteriesChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#TIMEOUT_STOP_BATTERIES} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setTimeoutStopBatteries(Boolean value) {
		this.getTimeoutStopBatteriesChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TIMEOUT_WAIT_FOR_BATTERIES_STATUS}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getTimeoutWaitForBatteriesStatusChannel() {
		return this.channel(ChannelId.TIMEOUT_WAIT_FOR_BATTERIES_STATUS);
	}

	/**
	 * Gets the {@link StateChannel} for
	 * {@link ChannelId#TIMEOUT_WAIT_FOR_BATTERIES_STATUS}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getTimeoutWaitForBatteriesStatus() {
		return this.getTimeoutWaitForBatteriesStatusChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#TIMEOUT_WAIT_FOR_BATTERIES_STATUS} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setTimeoutWaitForBatteriesStatus(Boolean value) {
		this.getTimeoutWaitForBatteriesStatusChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#AT_LEAST_ONE_BATTERY_NOT_RUNNING}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getAtLeastOneBatteryNotRunningChannel() {
		return this.channel(ChannelId.AT_LEAST_ONE_BATTERY_NOT_RUNNING);
	}

	/**
	 * Gets the {@link StateChannel} for
	 * {@link ChannelId#AT_LEAST_ONE_BATTERY_NOT_RUNNING}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getAtLeastOneBatteryNotRunning() {
		return this.getAtLeastOneBatteryNotRunningChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#AT_LEAST_ONE_BATTERY_NOT_RUNNING} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setAtLeastOneBatteryNotRunning(boolean value) {
		this.getAtLeastOneBatteryNotRunningChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#AT_LEAST_ONE_BATTERY_NOT_STOPPED}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getAtLeastOneBatteryNotStoppedChannel() {
		return this.channel(ChannelId.AT_LEAST_ONE_BATTERY_NOT_STOPPED);
	}

	/**
	 * Gets the {@link StateChannel} for
	 * {@link ChannelId#AT_LEAST_ONE_BATTERY_NOT_STOPPED}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getAtLeastOneBatteryNotStopped() {
		return this.getAtLeastOneBatteryNotStoppedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#AT_LEAST_ONE_BATTERY_NOT_STOPPED} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setAtLeastOneBatteryNotStopped(boolean value) {
		this.getAtLeastOneBatteryNotStoppedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#AT_LEAST_ONE_BATTERY_IN_ERROR}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getAtLeastOneBatteryInErrorChannel() {
		return this.channel(ChannelId.AT_LEAST_ONE_BATTERY_IN_ERROR);
	}

	/**
	 * Gets the {@link StateChannel} for
	 * {@link ChannelId#AT_LEAST_ONE_BATTERY_IN_ERROR}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getAtLeastOneBatteryInError() {
		return this.getAtLeastOneBatteryInErrorChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#AT_LEAST_ONE_BATTERY_IN_ERROR} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setAtLeastOneBatteryInError(boolean value) {
		this.getAtLeastOneBatteryInErrorChannel().setNextValue(value);
	}

	/**
	 * Gets the target Start/Stop mode from config or StartStop-Channel.
	 * 
	 * @return {@link StartStop}
	 */
	public StartStop getStartStopTarget();

	/**
	 * True on all batteries are stopped, false on one of the battery not stopped.
	 * 
	 * @return boolean
	 */
	public boolean areAllBatteriesStopped();

	/**
	 * True on all batteries are started, false on one of the battery not started.
	 * 
	 * @return boolean
	 */
	public boolean areAllBatteriesStarted();

	/**
	 * Start batteries one of the other. If one battery started, tries to start the
	 * next one in the queue.
	 */
	public void startBatteries() throws OpenemsNamedException;

	/**
	 * Stop batteries one of the other. If one battery stopped, tries to stop the
	 * next one in the queue.
	 */
	public void stopBatteries();

	/**
	 * Gets a list of not started batteries.
	 * 
	 * @return Started batteries {@link List}.
	 */
	public List<BatteryFeneconF2b> getNotStartedBatteries();

	/**
	 * Gets a list of not stopped batteries.
	 * 
	 * @return Stopped batteries {@link List}.
	 */
	public List<BatteryFeneconF2b> getNotStoppedBatteries();

	/**
	 * Are there any faults reported by any battery components?
	 *
	 * <p>
	 * Evaluates all Batteries {@link StateChannel}s and returns true if any Channel
	 * with {@link Level#FAULT} is set.
	 *
	 * @return true if there is a Fault.
	 */
	public boolean hasBatteriesFault();

	/**
	 * Gets the main contactor target which set by
	 * {@link #setMainContactor(Boolean)} method.
	 * 
	 * @return main contactor target.
	 */
	public boolean isHvContactorUnlocked();

	/**
	 * Gets the {@link BatteryFeneconF2bCluster cluster } class. It can be
	 * {@link BatteryFeneconF2bClusterSerialImpl} or
	 * {@link BatteryFeneconF2bClusterParallelImpl}
	 * 
	 * @return itself
	 */
	public BatteryFeneconF2bCluster getBatteryFeneconF2bCluster();

}
