package io.openems.edge.evcs.ocpp.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.MeasuringEvcs;
import io.openems.edge.evcs.api.Status;

public abstract class AbstractOcppEvcsComponent extends AbstractOpenemsComponent
		implements Evcs, MeasuringEvcs, EventHandler {

	private final Set<OcppProfileType> profileTypes;

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
		this.channel(Evcs.ChannelId.MINIMUM_POWER).setNextValue(getConfiguredMinimumHardwarePower());
		this.getEnergySession().setNextValue(0);
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:

			if (this.getChargingSessionId().value().orElse("").isEmpty()) {
				this.getChargingstationCommunicationFailed().setNextValue(true);
			} else {
				this.getChargingstationCommunicationFailed().setNextValue(false);
			}
			if (this.status().getNextValue().asEnum().equals(Status.CHARGING_FINISHED)) {
				this.resetChannelValues();
			}

			// TODO: Ask a WriteHandler to set Values for the profiles
			for (OcppProfileType ocppProfileType : profileTypes) {
				switch (ocppProfileType) {
				case CORE:
					break;
				case FIRMWARE_MANAGEMENT:
					break;
				case LOCAL_AUTH_LIST_MANAGEMENT:
					break;
				case REMOTE_TRIGGER:
					break;
				case RESERVATION:
					break;
				case SMART_CHARGING:
					break;
				}
			}
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
		 * Id is set if there is a new Session between - the EVCS implemented by this
		 * Component and the Server. If this value is empty, no communication was
		 * established
		 * 
		 * <ul>
		 * <li>Interface: OcppEvcs
		 * <li>Readable
		 * <li>Type: String
		 * </ul>
		 */
		CHARGING_SESSION_ID(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)
				.text("Identifies a current Session set by the server")), //

		/**
		 * Ocpp id.
		 * 
		 * Id that is defined in every EVCS which implements OCPP
		 * 
		 * <ul>
		 * <li>Interface: OcppEvcs
		 * <li>Readable
		 * <li>Type: String
		 * </ul>
		 */
		OCPP_ID(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY).text("OCPP Id of the Charging Station")), //

		/**
		 * Ocpp connector id.
		 * 
		 * Id that is defined for every connector on an EVCS that implements OCPP.
		 * Defines which plug is used (Like two plugs/connectors in ABL).
		 * 
		 * <ul>
		 * <li>Interface: OcppEvcs
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

	private void resetChannelValues() {
		for (MeasuringEvcs.ChannelId c : MeasuringEvcs.ChannelId.values()) {
			Channel<?> channel = this.channel(c);
			channel.setNextValue(null);
		}
		this.getChargePower().setNextValue(0);
	}

	/**
	 * Session Id.
	 * 
	 * Id is set if there is a new Session between - the EVCS implemented by this
	 * Component and the Server. If this value is empty, no communication was
	 * established.
	 */
	public StringReadChannel getChargingSessionId() {
		return this.channel(ChannelId.CHARGING_SESSION_ID);
	}

	/**
	 * Ocpp id.
	 * 
	 * Id that is defined in every EVCS which implements OCPP.
	 */
	public StringReadChannel getOcppId() {
		return this.channel(ChannelId.OCPP_ID);
	}

	/**
	 * Ocpp connector id.
	 * 
	 * Id that is defined for every connector on an EVCS that implements OCPP.
	 * Defines which plug is used (Like two plugs/connectors in ABL).
	 */
	public IntegerReadChannel getConnectorId() {
		return this.channel(ChannelId.CONNECTOR_ID);
	}
}
