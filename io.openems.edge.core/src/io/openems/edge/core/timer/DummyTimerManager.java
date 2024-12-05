package io.openems.edge.core.timer;

import io.openems.edge.common.timer.Timer;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

@EventTopics({ //
	EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE//
})
public class DummyTimerManager extends AbstractOpenemsComponent
	implements TimerManager, OpenemsComponent, EventHandler {

    private final ComponentManager cpm;
    public int coreCyclesCount = 0;

    public DummyTimerManager(ComponentManager cpm) {
	super(OpenemsComponent.ChannelId.values() //
	);
	this.cpm = cpm;
	this.channels().forEach(Channel::nextProcessImage);
	super.activate(null, "SingleComponentTimer", "", true);
    }

    @Override
    public void handleEvent(Event event) {
	this.coreCyclesCount++;
    }

    @Override
    public Timer getTimerByCount(Channel<Integer> channel, int countCheckCalls, int startDelayInSecs) {
	return new TimerByCount(this, channel, countCheckCalls, startDelayInSecs);
    }

    @Override
    public Timer getTimerByCoreCycles(Channel<Integer> channel, int count, int startDelayInSecs) {
	return new DummyTimerByCoreCycles(this, channel, count, startDelayInSecs);
    }

    @Override
    public Timer getTimerByTime(Channel<Integer> channel, int seconds, int startDelayInSecs) {
	return new TimerByTime(this, this.cpm.getClock(), channel, seconds, startDelayInSecs);
    }
}
