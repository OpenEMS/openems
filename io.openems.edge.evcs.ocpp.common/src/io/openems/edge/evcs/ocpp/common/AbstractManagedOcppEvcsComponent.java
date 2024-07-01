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
import org.slf4j.LoggerFactory;

import eu.chargetime.ocpp.NotConnectedException;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.model.Request;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evcs.api.AbstractManagedEvcsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.MeasuringEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.timedata.api.TimedataProvider;

/**
 * Abstract Managed Ocpp EVCS Component.
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
 * <pre>{@code
 * &#64;EventTopics({ //
 * 	EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
 * 	EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
 * })}
 * </pre>
 * 
 * <pre>
 * and also call "super.handleEvent(event)" in the subclass:
 * {@code}
 *  &#64;Override
 *	public void handleEvent(Event event) {
 *		super.handleEvent(event);
 *	}
 * </pre>
 */
public abstract class AbstractManagedOcppEvcsComponent extends AbstractManagedEvcsComponent
		implements Evcs, ManagedEvcs, MeasuringEvcs, EventHandler, TimedataProvider {

	private final Logger log = LoggerFactory.getLogger(AbstractManagedOcppEvcsComponent.class);

	private ChargingProperty lastChargingProperty = null;

	protected final Set<OcppProfileType> profileTypes;

	protected OcppServer ocppServer = null;

	protected UUID sessionId = null;

	private final ChargeSessionStamp sessionStart = new ChargeSessionStamp();

	private final ChargeSessionStamp sessionEnd = new ChargeSessionStamp();

	protected AbstractManagedOcppEvcsComponent(OcppProfileType[] profileTypes,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);

		this.profileTypes = new HashSet<>(Arrays.asList(profileTypes));
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

		this.setInitialSettings();
	}

	@Override
	protected void modified(ComponentContext context, String id, String alias, boolean enabled) {
		super.modified(context, id, alias, enabled);

		this.setInitialSettings();
	}

	private void setInitialSettings() {
		// Normally the limits are set automatically when the phase channel is set, but
		// not every OCPP charger provides the information about the number of phases.
		int fixedMaximum = this.getFixedMaximumHardwarePower().orElse(DEFAULT_MAXIMUM_HARDWARE_POWER);
		int fixedMinimum = this.getFixedMinimumHardwarePower().orElse(DEFAULT_MINIMUM_HARDWARE_POWER);

		this.getMaximumHardwarePowerChannel().setNextValue(fixedMaximum);
		this.getMinimumHardwarePowerChannel().setNextValue(fixedMinimum);
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.setInitialTotalEnergyFromTimedata();
			break;

		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			if (this.sessionId == null) {
				this.lostSession();
				return;
			}

			this.checkCurrentState();
			break;
		}
	}

	/**
	 * Initialize total energy value from from Timedata service if it is not already
	 * set.
	 */
	private void setInitialTotalEnergyFromTimedata() {
		var totalEnergy = this.getActiveConsumptionEnergy().orElse(null);

		// Total energy already set
		if (totalEnergy != null) {
			return;
		}

		var timedata = this.getTimedata();
		var componentId = this.id();
		if (timedata == null || componentId == null) {
			return;
		} else {
			timedata.getLatestValue(new ChannelAddress(componentId, Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY.id()))
					.thenAccept(totalEnergyOpt -> {
						if (this.getActiveConsumptionEnergy().isDefined()) {
							// Value has been read from device in the meantime
							return;
						}

						if (totalEnergyOpt.isPresent()) {
							try {
								this._setActiveConsumptionEnergy(
										TypeUtils.getAsType(OpenemsType.LONG, totalEnergyOpt.get()));
							} catch (IllegalArgumentException e) {
								this._setActiveConsumptionEnergy(TypeUtils.getAsType(OpenemsType.LONG, 0L));
							}
						} else {
							this._setActiveConsumptionEnergy(TypeUtils.getAsType(OpenemsType.LONG, 0L));
						}
					});
		}
	}

	@Override
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * New session started.
	 * 
	 * <p>
	 * This is called in the OcppServer if this EVCS matches the appeared EVCS in
	 * the server.
	 * 
	 * @param server    the {@link OcppServer}
	 * @param sessionId the referring session {@link UUID}
	 */
	public void newSession(OcppServer server, UUID sessionId) {
		this.ocppServer = server;
		this.sessionId = sessionId;
		this._setStatus(Status.NOT_READY_FOR_CHARGING);
		this._setChargingstationCommunicationFailed(false);
	}

	/**
	 * Lost the current Session.
	 * 
	 * <p>
	 * This is called in the OcppServer if this EVCS disappeared from the server
	 * connections.
	 */
	public void lostSession() {
		this.ocppServer = null;
		this.sessionId = null;
		this._setStatus(Status.UNDEFINED);
		this._setChargingstationCommunicationFailed(true);
		this.resetMeasuredChannelValues();
	}

	/**
	 * Get the supported measurements.
	 * 
	 * @return supported measurements as a Set of {@link OcppInformations}
	 */
	public abstract Set<OcppInformations> getSupportedMeasurements();

	/**
	 * Get the configured OCPP ID.
	 * 
	 * @return OCPP ID
	 */
	public abstract String getConfiguredOcppId();

	/**
	 * Get configured Connector-ID.
	 * 
	 * <p>
	 * Every OCPP-EVCS Component requires the corresponding Connector-ID of the
	 * charger. This is especially important for an EVCS with two or more
	 * connectors.
	 * 
	 * @return configured Connector-ID
	 */
	public abstract Integer getConfiguredConnectorId();

	/**
	 * Returns session energy.
	 * 
	 * <p>
	 * Is the EVCS supporting SessionEnergy or only a meter with the total energy.
	 * 
	 * @return boolean
	 */
	public abstract boolean returnsSessionEnergy();

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
	}

	/**
	 * Reset the measured channel values and the charge power.
	 */
	private void resetMeasuredChannelValues() {
		for (MeasuringEvcs.ChannelId c : MeasuringEvcs.ChannelId.values()) {
			Channel<?> channel = this.channel(c);
			channel.setNextValue(null);
		}
		this._setChargePower(0);
	}

	/**
	 * Check the current state and resets the measured values.
	 */
	private void checkCurrentState() {
		var state = this.getStatus();
		switch (state) {
		case CHARGING:
		case READY_FOR_CHARGING:
			break;
		case CHARGING_FINISHED:
			this.resetMeasuredChannelValues();
			break;
		case CHARGING_REJECTED:
		case ENERGY_LIMIT_REACHED:
		case ERROR:
		case NOT_READY_FOR_CHARGING:
		case STARTING:
		case UNDEFINED:
			this._setChargePower(0);
			break;
		}
	}

	public ChargingProperty getLastChargingProperty() {
		return this.lastChargingProperty;
	}

	public void setLastChargingProperty(ChargingProperty chargingProperty) {
		this.lastChargingProperty = chargingProperty;
	}

	public ChargeSessionStamp getSessionStart() {
		return this.sessionStart;
	}

	public ChargeSessionStamp getSessionEnd() {
		return this.sessionEnd;
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
			return "Limit:" + ((ManagedEvcs) this).getSetChargePowerLimit().orElse(null) + "|"
					+ this.getStatus().getName();
		}
		return "Power:" + this.getChargePower().orElse(0) + "|" + this.getStatus().getName();
	}

	@Override
	public boolean applyDisplayText(String text) throws OpenemsException {

		Request request = this.getStandardRequests().setDisplayText(text);

		// Feature not supported
		if (request == null) {
			return false;
		}

		try {
			this.ocppServer.send(this.sessionId, request).whenComplete((confirmation, throwable) -> {
				if (throwable != null) {
					throwable.printStackTrace();
					return;
				}

				this.logInfo(this.log, confirmation.toString());
			});

			return true;

		} catch (OccurenceConstraintException e) {
			this.logWarn(this.log, "The request is not a valid OCPP request.");
		} catch (UnsupportedFeatureException e) {
			this.logWarn(this.log, "This feature is not implemented by the charging station.");
		} catch (NotConnectedException e) {
			this.logWarn(this.log, "The server is not connected.");
		}
		return false;
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws OpenemsException {
		return this.setPower(power);
	}

	@Override
	public boolean pauseChargeProcess() throws OpenemsException {
		return this.setPower(0);
	}

	/**
	 * Sets the current or power depending on the setChargePowerLimit
	 * StandardRequest implementation of the charger.
	 * 
	 * <p>
	 * Depending on the charger implementation it will send different requests with
	 * different units.
	 * 
	 * @param power Power that should be send
	 * @return Returns true if the power was set correctly.
	 */
	private boolean setPower(int power) {

		Request request = this.getStandardRequests().setChargePowerLimit(power);

		try {
			this.ocppServer.send(this.sessionId, request).whenComplete((confirmation, throwable) -> {
				if (throwable != null) {
					throwable.printStackTrace();
					return;
				}

				this.logInfo(this.log, confirmation.toString());
			});

			return true;

		} catch (OccurenceConstraintException e) {
			this.logWarn(this.log, "The request is not a valid OCPP request.");
		} catch (UnsupportedFeatureException e) {
			this.logWarn(this.log, "This feature is not implemented by the charging station.");
		} catch (NotConnectedException e) {
			this.logWarn(this.log, "The server is not connected.");
		}
		return false;
	}
}
