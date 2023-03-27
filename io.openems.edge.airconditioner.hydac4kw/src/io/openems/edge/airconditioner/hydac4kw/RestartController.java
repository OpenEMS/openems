package io.openems.edge.airconditioner.hydac4kw;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

import com.google.common.base.Optional;

import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstoppratelimited.StartFrequency;
import io.openems.edge.common.startstoppratelimited.StartStoppEvent;

public final class RestartController {
	private final int maxStartAmount;
	private final Duration duration;
	private final Runnable onStart;
	private final Runnable onStop;
	private EventLog<StartStoppEvent> eventHistory;
	
	public RestartController (StartFrequency freq, Runnable onStart, Runnable onStop) {
		this.maxStartAmount = freq.getTimes();
		this.duration = freq.getDuration();
		this.onStart = onStart;
		this.onStop = onStop;
		this.eventHistory = new EventLog<>(freq.getTimes() * 2);
	}
	
	
	public Optional<Integer> requestStart() {
		var remainingStarts = this.remainingStartsAvaliable();
		// Update change event log only if there is something new
		if(remainingStarts > 0 && !this.isRunning()) {
			this.eventHistory.push(StartStoppEvent.of(StartStop.START));
			this.onStart.run();
			return Optional.of(remainingStarts);
		} else {
			return Optional.absent();
		}
	}
	
	public boolean isRunning() {
		var lastState = this.eventHistory.first().map(t -> t.getState()).orElse(StartStop.STOP);
		return lastState == StartStop.START;
	}
	
	public int remainingStartsAvaliable() {
		Function<Instant, Boolean> filterLastPeriod = (Instant t) -> t.isAfter(Instant.now().minus(this.duration));
		int lastOnRequestsInDuration = (int)(this.eventHistory
			.asStream()
			.filter(t -> filterLastPeriod.apply(t.getTimestamp()))
			.filter(t -> t.getState() == StartStop.START)
			.count());
		return this.maxStartAmount - lastOnRequestsInDuration;
	}

	public void requestStop() {
		if(this.isRunning()) {
			this.eventHistory.push(StartStoppEvent.of(StartStop.STOP));
			this.onStop.run();
		}
	}
	
	@Override
	public String toString() {
		return String.format(
				"RestartController={isRunning: %s, remainingStarts: %s, events: %s}", // 
				this.isRunning(), this.remainingStartsAvaliable(), // 
				this.eventHistory.asStream().map(t -> t.getState()).toList() //
		);
	}
	
}
