package io.openems.edge.evcs.ocpp.common;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;

import eu.chargetime.ocpp.model.Request;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.MeasuringEvcs;
import io.openems.edge.evcs.api.Status;

public abstract class AbstractOcppEvcsComponent extends AbstractOpenemsComponent
		implements Evcs, MeasuringEvcs, EventHandler {

	private ChargingProperty lastChargingProperty = null;

	protected final Set<OcppProfileType> profileTypes;

	private final WriteHandler writeHandler = new WriteHandler(this);
	
	protected OcppServer ocppServer = null;
	
	protected UUID sessionId = null;

	protected AbstractOcppEvcsComponent(OcppProfileType[] profileTypes,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);

		this.profileTypes = new HashSet<OcppProfileType>(Arrays.asList(profileTypes));
	}
	
	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
	
	@Override
	protected void activate(ComponentContext context, String id, String alias, boolean enabled) {
		super.activate(context, id, alias, enabled);

		this.channel(Evcs.ChannelId.MAXIMUM_HARDWARE_POWER).setNextValue(getConfiguredMaximumHardwarePower());
		this.channel(Evcs.ChannelId.MINIMUM_HARDWARE_POWER).setNextValue(getConfiguredMinimumHardwarePower());
		this.getEnergySession().setNextValue(0);
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			if(this.sessionId == null) {
				return;
			}
			if (this.getStatus().getNextValue().asEnum().equals(Status.CHARGING_FINISHED)) {
				this.resetMeasuredChannelValues();
			}
			writeHandler.run();
			break;
		}
	}

	@Override
	protected void deactivate() {
		super.deactivate();
	}
	
	public void newSession(OcppServer server, UUID sessionId) {
		this.ocppServer = server;
		this.sessionId = sessionId;
		this.getStatus().setNextValue(Status.NOT_READY_FOR_CHARGING);
		this.getChargingstationCommunicationFailed().setNextValue(false);
	}
	
	public void lostSession() {
		this.ocppServer = null;
		this.sessionId = null;
		this.getStatus().setNextValue(Status.UNDEFINED);
		this.getChargingstationCommunicationFailed().setNextValue(true);
	}

	public abstract Set<OcppInformations> getSupportedMeasurements();

	public abstract String getConfiguredOcppId();

	public abstract Integer getConfiguredConnectorId();

	public abstract Integer getConfiguredMaximumHardwarePower();

	public abstract Integer getConfiguredMinimumHardwarePower();
	
	/**
	 * Required requests that should be sent after a connection was established.
	 * 
	 * @return List of requests
	 */
	public abstract List<Request> getRequiredRequestsAfterConnection();

	/**
	 * Required requests that should be sent permanently during a session.
	 * 
	 * @return List of requests
	 */
	public abstract List<Request> getRequiredRequestsDuringConnection();

	/**
	 * Default requests that every OCPP EVCS should have.
	 * 
	 * @return OcppRequests
	 */
	public abstract OcppStandardRequests getStandardRequests();
	
	public UUID getSessionId() {
		return this.sessionId;
	};

	private void resetMeasuredChannelValues() {
		for (MeasuringEvcs.ChannelId c : MeasuringEvcs.ChannelId.values()) {
			Channel<?> channel = this.channel(c);
			channel.setNextValue(null);
		}
		this.getChargePower().setNextValue(0);
	}

	public ChargingProperty getLastChargingProperty() {
		return this.lastChargingProperty;
	}

	public void setLastChargingProperty(ChargingProperty chargingProperty) {
		this.lastChargingProperty = chargingProperty;
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
	public String debugLog() {
		if (this instanceof ManagedEvcs) {
			return "Limit:" + ((ManagedEvcs) this).setChargePowerLimit().value().orElse(null) + "|"
					+ this.getStatus().value().asEnum().getName();
		}
		return "Power:" + this.getChargePower().value().orElse(0) + "|" + this.getStatus().value().asEnum().getName();
	}
}
