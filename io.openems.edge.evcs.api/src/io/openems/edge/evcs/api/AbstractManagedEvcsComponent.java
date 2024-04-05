package io.openems.edge.evcs.api;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

/**
 * Abstract Managed EVCS Component.
 * 
 * <p>
 * Includes the logic for the write handler - that is sending the limits
 * depending on the 'send' logic of each implementation. The
 * SET_CHARGE_POWER_LIMIT or SET_CHARGE_POWER_LIMIT_WITH_FILTER Channel are
 * usually set by the evcs Controller.
 *
 * <p>
 * Please ensure to add the event topics at in the properties of the subclass:
 * 
 * <pre>
 * &#64;EventTopics({ //
 *   EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
 * })
 * </pre>
 * 
 * <p>
 * and also call "super.handleEvent(event)" in the subclass:
 * 
 * <pre>
 * &#64;Override
 * public void handleEvent(Event event) {
 * 	super.handleEvent(event);
 * }
 * </pre>
 */
public abstract class AbstractManagedEvcsComponent extends AbstractOpenemsComponent
		implements Evcs, ManagedEvcs, EventHandler {

	private final Logger log = LoggerFactory.getLogger(AbstractManagedEvcsComponent.class);

	private final WriteHandler writeHandler = new WriteHandler(this);
	private final ChargeStateHandler chargeStateHandler = new ChargeStateHandler(this);

	protected AbstractManagedEvcsComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	@Override
	protected void activate(ComponentContext context, String id, String alias, boolean enabled) {
		super.activate(context, id, alias, enabled);

		Evcs.addCalculatePowerLimitListeners(this);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			this.writeHandler.run();
			break;
		}
	}

	@Override
	public ChargeStateHandler getChargeStateHandler() {
		return this.chargeStateHandler;
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

	@Override
	protected void logDebug(Logger log, String message) {
		if (this.getConfiguredDebugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	public void logDebug(String message) {
		this.logDebug(this.log, message);
	}

	@Override
	public String debugLog() {
		return "Limit:" + this.getSetChargePowerLimit().orElse(null) + "|" + this.getStatus().getName();
	}
}
