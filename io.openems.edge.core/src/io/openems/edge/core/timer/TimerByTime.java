package io.openems.edge.core.timer;

import java.time.Clock;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.timer.Timer;

public class TimerByTime extends AbstractTimer implements Timer {

    private final Clock clock;
    private final int waitTimeSeconds;
    private long expirationTimeMillis;

    public TimerByTime(TimerManager tm, //
	    Clock clock, Channel<Integer> channel, //
	    int waitTimeSeconds, //
	    int startDelayInSecs) {
	// startDelayInSecs must not be handled in AbstractTimerImpl
	super(tm, channel, 0);
	this.waitTimeSeconds = waitTimeSeconds;
	this.clock = clock;
	this.expirationTimeMillis = this.clock.millis() + (this.waitTimeSeconds + startDelayInSecs) * 1000L;
    }

    @Override
    public boolean check() {
	long diffTime = this.expirationTimeMillis - this.clock.millis();

	diffTime = Math.max(diffTime, 0);
	this.updateChannel(((int) diffTime / 1000));

	return this.expirationTimeMillis <= this.clock.millis();
    }

    @Override
    public void reset() {
	this.expirationTimeMillis = this.clock.millis() + this.waitTimeSeconds * 1000L;
	this.updateChannel(this.waitTimeSeconds);
    }

}
