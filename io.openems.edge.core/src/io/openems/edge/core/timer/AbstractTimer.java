package io.openems.edge.core.timer;

import java.util.Optional;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.timer.Timer;

public abstract class AbstractTimer implements Timer {

    protected final Timer delayTimer;
    protected final Channel<Integer> channel;

    protected AbstractTimer(TimerManager tm, //
	    Channel<Integer> channel, //
	    final int startDelayInSecs) {

	this.channel = channel;
	if (startDelayInSecs > 0) {
	    this.delayTimer = tm.getTimerByTime(null, 0, startDelayInSecs);
	} else {
	    this.delayTimer = null;
	}
    }

    protected boolean delayStart() {
	return this.delayTimer != null && this.delayTimer.check() == false;
    }

    protected void updateChannel(int value) {
	Optional.ofNullable(this.channel).ifPresent(channel -> channel.setNextValue(value));
    }
}
