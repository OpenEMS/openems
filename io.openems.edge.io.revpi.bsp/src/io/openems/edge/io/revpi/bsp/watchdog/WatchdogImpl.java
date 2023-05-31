package io.openems.edge.io.revpi.bsp.watchdog;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.revpi.bsp.core.Core;

@Designate(ocd = Config.class, factory = false)
@Component(//
	name = "io.revpi.bsp.watchdog", //
	immediate = true, //
	configurationPolicy = ConfigurationPolicy.REQUIRE //
) //
@EventTopics({ //
	EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
}) //
public class WatchdogImpl extends AbstractOpenemsComponent implements Watchdog, OpenemsComponent, EventHandler {

    private final Logger log = LoggerFactory.getLogger(WatchdogImpl.class);

    @Reference
    private Core core;

    @Reference
    private ComponentManager componentManager;

    public WatchdogImpl() {
	super(OpenemsComponent.ChannelId.values(), //
		Watchdog.ChannelId.values() //
	);
    }

    @Activate
    void activate(ComponentContext context, Config config) {
	super.activate(context, config.id(), config.alias(), config.enabled());
	this.logInfo(this.log, "activated");
    }

    @Deactivate
    protected void deactivate() {
	super.deactivate();
	this.logInfo(this.log, "deactivated");
    }

    @Override
    public void handleEvent(Event event) {
	if (!this.isEnabled()) {
	    return;
	}
	switch (event.getTopic()) {
	case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
	    this.toggleWatchdogValue();
	    break;
	}
    }

    private void toggleWatchdogValue() {
	try {
	    Channel<Boolean> channel = this.channel(Watchdog.ChannelId.WATCHDOG);
	    var value = channel.value().orElse(false);
	    channel.setNextValue(!value);
	    this.core.toggleWatchdog();
	} catch (Exception e) {
	    this.logError(this.log, "Unable to retrigger watchdog " + e.getMessage());
	}
    }

}
