package io.openems.edge.core.timer;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.timer.Timer;

public class TimerByCoreCycles extends AbstractTimer implements Timer {

    private final int maxCoreCycles;
    private final TimerManagerImpl tm;
    private int refCount;

    public TimerByCoreCycles(TimerManagerImpl tm, //
	    Channel<Integer> channel, //
	    int maxCoreCycles, //
	    int startDelayInSecs) {
	super(tm, channel, startDelayInSecs);
	this.tm = tm;
	this.maxCoreCycles = maxCoreCycles;
	this.refCount = this.tm.getCoreCyclesCount();
    }

    @Override
    public boolean check() {
	if (this.delayStart()) {
	    return false;
	}
	this.updateChannel(this.refCount);

	// Note: ignore possible integer overflow (needs ~68years uptime)
	return this.refCount + this.maxCoreCycles <= this.tm.getCoreCyclesCount();
    }

    @Override
    public void reset() {
	this.refCount = this.tm.getCoreCyclesCount();
    }

}
