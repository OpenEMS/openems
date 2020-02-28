package io.openems.edge.evcs.ocpp.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.MeasuringEvcs;
import io.openems.edge.evcs.api.Status;

public abstract class AbstractOcppEvcsComponent extends AbstractOpenemsComponent
		implements Evcs, MeasuringEvcs, EventHandler {

	protected final Set<OcppProfileType> profileTypes;

	private final WriteHandler writeHandler = new WriteHandler(this);

	protected AbstractOcppEvcsComponent(OcppProfileType[] profileTypes,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);

		this.profileTypes = new HashSet<OcppProfileType>(Arrays.asList(profileTypes));
	}

	@Override
	protected void activate(ComponentContext context, String id, String alias, boolean enabled) {
		super.activate(context, id, alias, enabled);

		this.channel(ChannelId.OCPP_ID).setNextValue(getConfiguredOcppId());
		this.channel(ChannelId.CONNECTOR_ID).setNextValue(getConfiguredConnectorId());
		this.channel(Evcs.ChannelId.MAXIMUM_HARDWARE_POWER).setNextValue(getConfiguredMaximumHardwarePower());
		this.channel(Evcs.ChannelId.MINIMUM_HARDWARE_POWER).setNextValue(getConfiguredMinimumHardwarePower());
		this.getEnergySession().setNextValue(0);
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			if (this.getChargingSessionId().getNextValue().orElse("").isEmpty()) {
				this.getChargingstationCommunicationFailed().setNextValue(true);
				break;
			} else {
				this.getChargingstationCommunicationFailed().setNextValue(false);
			}
			if (this.status().getNextValue().asEnum().equals(Status.CHARGING_FINISHED)) {
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

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Session Id.
		 * 
		 * <p>
		 * Id is set if there is a new Session between - the EVCS implemented by this
		 * Component and the Server. If this value is empty, no communication was
		 * established
		 * 
		 * <ul>
		 * <li>Readable
		 * <li>Type: String
		 * </ul>
		 */
		CHARGING_SESSION_ID(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)
				.text("Identifies a current Session set by the server")), //

		/**
		 * Ocpp id.
		 * 
		 * <p>
		 * Id that is defined in every EVCS which implements OCPP
		 * 
		 * <ul>
		 * <li>Readable
		 * <li>Type: String
		 * </ul>
		 */
		OCPP_ID(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY).text("OCPP Id of the Charging Station")), //

		/**
		 * Ocpp connector id.
		 * 
		 * <p>
		 * Id that is defined for every connector on an EVCS that implements OCPP.
		 * Defines which plug is used (Like two plugs/connectors in ABL).
		 * 
		 * <ul>
		 * <li>Readable
		 * <li>Type: Integer
		 * </ul>
		 */
		CONNECTOR_ID(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).text("Connector id of the charger")); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public abstract Set<OcppInformations> getSupportedMeasurements();

	public abstract String getConfiguredOcppId();

	public abstract Integer getConfiguredConnectorId();

	public abstract Integer getConfiguredMaximumHardwarePower();

	public abstract Integer getConfiguredMinimumHardwarePower();

	public abstract OcppServer getConfiguredOcppServer();

	/**
	 * Reset all measured channel values.
	 */
	private void resetMeasuredChannelValues() {
		for (MeasuringEvcs.ChannelId c : MeasuringEvcs.ChannelId.values()) {
			Channel<?> channel = this.channel(c);
			channel.setNextValue(null);
		}
		this.getChargePower().setNextValue(0);
	}

	/**
	 * Session Id.
	 * 
	 * <p>
	 * Id is set if there is a new Session between - the EVCS implemented by this
	 * Component and the Server. If this value is empty, no communication was
	 * established.
	 * 
	 * @return StringReadChannel
	 */
	public StringReadChannel getChargingSessionId() {
		return this.channel(ChannelId.CHARGING_SESSION_ID);
	}

	/**
	 * Ocpp id.
	 * 
	 * <p>
	 * Id that is defined in every EVCS which implements OCPP.
	 * 
	 * @return StringReadChannel
	 */
	public StringReadChannel getOcppId() {
		return this.channel(ChannelId.OCPP_ID);
	}

	/**
	 * Ocpp connector id.
	 * 
	 * <p>
	 * Id that is defined for every connector on an EVCS that implements OCPP.
	 * Defines which plug is used (Like two plugs/connectors in ABL).
	 * 
	 * @return StringReadChannel
	 */
	public IntegerReadChannel getConnectorId() {
		return this.channel(ChannelId.CONNECTOR_ID);
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
					+ this.status().value().asEnum().getName();
		}
		return "Power:" + this.getChargePower().value().orElse(0) + "|" + this.status().value().asEnum().getName();
	}
}
