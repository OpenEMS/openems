package io.openems.edge.core.timer;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.timer.Timer;

public class TimerByCount extends AbstractTimer implements Timer {

    private final int maxCount;
    private int count;

    public TimerByCount(TimerManager tm, //
	    Channel<Integer> channel, //
	    int maxCheckCalls, //
	    int startDelayInSecs) {
	super(tm, channel, startDelayInSecs);

	this.maxCount = maxCheckCalls;
	this.count = this.maxCount;
    }

    @Override
    public boolean check() {
	if (this.delayStart()) {
	    return false;
	}
	this.updateChannel(this.count);

	return this.count-- <= 0;
    }

    @Override
    public void reset() {
	this.count = this.maxCount;
    }

}
