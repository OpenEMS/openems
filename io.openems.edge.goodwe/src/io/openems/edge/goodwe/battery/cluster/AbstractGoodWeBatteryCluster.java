package io.openems.edge.goodwe.battery.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.home.BatteryFeneconHome;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.startstop.StartStoppable;

public abstract class AbstractGoodWeBatteryCluster extends AbstractOpenemsComponent
		implements GoodWeBatteryCluster, Battery, OpenemsComponent, EventHandler, StartStoppable {

	private final Logger log = LoggerFactory.getLogger(AbstractGoodWeBatteryCluster.class);

	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<>(StartStop.UNDEFINED);
	private final List<Battery> batteries = new ArrayList<>();

	private StartStopConfig startStopConfig;

	protected synchronized void addBatteries(List<? extends Battery> batteries) {
		this.batteries.addAll(batteries);
		this.getChannelManager().deactivate();
		this.getChannelManager().activate(this.batteries);
	}

	protected synchronized void removeBatteries(List<? extends Battery> batteries) {
		this.batteries.removeAll(batteries);
		this.getChannelManager().deactivate();
		this.getChannelManager().activate(this.batteries);
	}

	protected AbstractGoodWeBatteryCluster(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	@Override
	protected void activate(ComponentContext context, String id, String alias, boolean enabled) {
		throw new IllegalArgumentException("Use the other activate() method!");
	}

	protected void activate(ComponentContext context, String id, String alias, boolean enabled,
			StartStopConfig startStop) {
		super.activate(context, id, alias, enabled);
		this.startStopConfig = startStop;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> this.handleStartStop();
		}
	}

	/**
	 * Starts/Stops all ESS in the Cluster as required by Config or call to
	 * setStartStop().
	 */
	private void handleStartStop() {
		var target = this.getStartStopTarget();
		if (target == this.getStartStop()) {
			return;
		}

		this.batteries.stream() //
				.filter(StartStoppable.class::isInstance) //
				.map(StartStoppable.class::cast) //
				.forEach(battery -> {
					try {
						battery.setStartStop(target);
					} catch (OpenemsNamedException e) {
						this.logError(this.log, e.getMessage());
					}
				});
	}

	protected abstract ChannelManager getChannelManager();

	@Override
	public StartStop getStartStopTarget() {
		return switch (this.startStopConfig) {
		case AUTO -> this.startStopTarget.get();
		case START -> StartStop.START;
		case STOP -> StartStop.STOP;
		};
	}

	@Override
	public void setStartStop(StartStop value) {
		this.startStopTarget.set(value);
	}

	/**
	 * Get {@link BatteryFeneconHome} batteries.
	 * 
	 * @return batteries
	 */
	public List<Battery> getBatteries() {
		return this.batteries;
	}

}
