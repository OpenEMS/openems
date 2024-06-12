package io.openems.edge.core.timer;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.timer.Timer;

@Designate(ocd = Config.class, factory = false)
@Component(//
	name = TimerManager.SINGLETON_SERVICE_PID, //
	immediate = true, //
	property = { //
		"enabled=true" //
	})
@EventTopics({ //
	EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE//
})
public class TimerManagerImpl extends AbstractOpenemsComponent implements TimerManager, EventHandler, OpenemsComponent {

    private int coreCyclesCount = 0;

    @Reference
    private ConfigurationAdmin cm;

    @Reference
    protected ComponentManager componentManager;

    public TimerManagerImpl() {
	super(//
		OpenemsComponent.ChannelId.values() //
	);
    }

    @Activate
    private void activate(ComponentContext context, Config config) {
	super.activate(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
	if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
	    return;
	}
    }

    @Override
    @Deactivate
    protected void deactivate() {
	super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
	this.coreCyclesCount++;
    }

    protected int getCoreCyclesCount() {
	return this.coreCyclesCount;
    }

    @Override
    public Timer getTimerByCount(Channel<Integer> channel, int countCheckCalls, int startDelayInSecs) {
	return new TimerByCount(this, channel, countCheckCalls, startDelayInSecs);
    }

    @Override
    public Timer getTimerByCoreCycles(Channel<Integer> channel, int count, int startDelayInSecs) {
	return new TimerByCoreCycles(this, channel, count, startDelayInSecs);
    }

    @Override
    public Timer getTimerByTime(Channel<Integer> channel, int seconds, int startDelayInSecs) {
	return new TimerByTime(this, this.componentManager.getClock(), channel, seconds, startDelayInSecs);
    }

}
