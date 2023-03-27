package io.openems.edge.common.startstoppratelimited;

import java.time.Instant;

import io.openems.edge.common.startstop.StartStop;

public final class StartStoppEvent {
	
	final StartStop state;
	private final Instant timestamp;
	
	public StartStoppEvent(final StartStop state, final Instant timestamp) {
		this.state = state;
		this.timestamp = timestamp;
	}
	
	public StartStop getState () {
		return this.state;
	}
	
	public Instant getTimestamp() {
		return this.timestamp;
	}
	
	public static StartStoppEvent of(final StartStop state) {
		return new StartStoppEvent(state, Instant.now());
	}
	
	@Override
	public String toString() {
		return String.format("StartStoppEvent={state: %s, timestamp: %s}", this.state, this.timestamp);
	}
}