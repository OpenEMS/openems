package io.openems.edge.airconditioner.hydac4kw;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;

import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstoppratelimited.StartFrequency;
import io.openems.edge.common.startstoppratelimited.StartStoppEvent;


public final class RestartController {
	private final int maxStartAmount;
	private final Duration duration;
	private final Runnable onStart;
	private final Runnable onStop;
	private EventLog<StartStoppEvent> eventHistory;
	
	/**
	 * Creates a restart controller with the desired parameters and callbacks.
	 * @param freq how often starts can be requested.
	 * @param onStart called when a successful start attempt is happened.
	 * @param onStop called when a successful stop attempt is happened.
	 * 
	 * Example usage:
	 * <code>
	 * var freq = StartFrequencyBuilder
	 * 		.withOccurence(5)
	 * 		.withDuration(Duration.ofMinutes(20))
	 * 		.build();
	 * var restartController(
	 * 		freq,
	 * 		() -> ...,
	 * 		() -> ...
	 * );
	 * </code>
	 */
	public RestartController (StartFrequency freq, Runnable onStart, Runnable onStop) {
		this.maxStartAmount = freq.getTimes();
		this.duration = freq.getDuration();
		this.onStart = onStart;
		this.onStop = onStop;
		this.eventHistory = new EventLog<>(freq.getTimes() * 4);
	}
	
	/**
	 * Requests a start on the device. This action is only executed if 
	 * 1. start counts in time range smaller than allowed.
	 * 2. device is not running already.
	 * @return Optional.empty() if the start request cannot be fulfilled, otherwise the number of remaining start attempts.
	 */
	public Optional<Integer> requestStart() {
		var remainingStarts = this.remainingStartsAvaliable();
		// Update change event log only if there is something new
		if(remainingStarts > 0 && !this.isRunning()) {
			this.eventHistory.push(StartStoppEvent.of(StartStop.START));
			this.onStart.run();
			return Optional.of(remainingStarts);
		} else {
			return Optional.empty();
		}
	}
	
	public boolean isRunning() {
		var lastState = this.eventHistory.first().map(t -> t.getState()).orElse(StartStop.STOP);
		return lastState == StartStop.START;
	}
	
	
	/**
	 * Queries the number of remaining starts based on the historic event log.
	 * @return number of remaining starts in the time interval
	 */
	public int remainingStartsAvaliable() {
		Function<Instant, Boolean> filterLastPeriod = (Instant t) -> t.isAfter(Instant.now().minus(this.duration));
		int lastOnRequestsInDuration = (int)(this.eventHistory
			.asStream()
			.filter(t -> filterLastPeriod.apply(t.getTimestamp()))
			.filter(t -> t.getState() == StartStop.START)
			.count());
		return this.maxStartAmount - lastOnRequestsInDuration;
	}

	/**
	 * Stops the device.
	 * Stop is allowed any time. The <code>onStop</code> runnable will be only 
	 * called if the device was running previously.
	 */
	public void requestStop() {
		if(this.isRunning()) {
			this.eventHistory.push(StartStoppEvent.of(StartStop.STOP));
			this.onStop.run();
		}
	}
	
	@Override
	public String toString() {
		return String.format(
				"RestartController={isRunning: %s, remainingStarts: %s,  events(%s): %s}", // 
				this.isRunning(), this.remainingStartsAvaliable(), //
				this.eventHistory.asStream().filter(t -> t.getTimestamp().isAfter(Instant.now().minus(this.duration))).count(),
				this.eventHistory.asStream().map(t -> t.getState()).toList() //
		);
	}
}
