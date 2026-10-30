package io.openems.edge.evcs.alpitronic;

import static io.openems.common.channel.Unit.AMPERE;
import static io.openems.common.channel.Unit.VOLT;
import static io.openems.common.channel.Unit.WATT;
import static io.openems.common.channel.Unit.WATT_HOURS;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.DOUBLE;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.LONG;
import static io.openems.common.types.OpenemsType.STRING;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.chargepoint.alpitronic.enums.AvailableState;

/**
 * Hypercharger EV charging protocol interface.
 * 
 * <p>
 * Defines the interface for Alpitronic Hypercharger
 */
public interface EvcsAlpitronic extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		// Raw connector status
		RAW_STATUS(Doc.of(AvailableState.values())), //
		CHARGING_VOLTAGE(Doc.of(DOUBLE).unit(VOLT)), //
		CHARGING_CURRENT(Doc.of(DOUBLE).unit(AMPERE)), //

		// Station-level channels (new in refactored version)
		UNIX_TIME(Doc.of(LONG).text("Unix timestamp")), //
		NUM_CONNECTORS(Doc.of(INTEGER).text("Number of connectors")), //
		STATION_STATE(Doc.of(INTEGER).text("Station state")), //
		TOTAL_STATION_POWER(Doc.of(INTEGER).unit(WATT).text("Total station power")), //
		SERIAL_NUMBER(Doc.of(STRING).text("Serial number")), //
		CHARGEPOINT_ID(Doc.of(STRING).text("Charge point ID")), //
		VID(Doc.of(STRING).text("Vendor ID")), //
		ID_TAG(Doc.of(STRING).text("ID tag")), //
		LOAD_MANAGEMENT_ENABLED(Doc.of(BOOLEAN).text("Load management enabled")), //

		// Software version channels
		SOFTWARE_VERSION_MAJOR(Doc.of(INTEGER).text("Software version major")), //
		SOFTWARE_VERSION_MINOR(Doc.of(INTEGER).text("Software version minor")), //
		SOFTWARE_VERSION_PATCH(Doc.of(INTEGER).text("Software version patch")), //

		// Version-aware channels
		TOTAL_CHARGED_ENERGY(Doc.of(LONG)//
				.unit(WATT_HOURS)//
				.text("Total charged energy counter (v2.3+)")), //
		MAX_CHARGING_POWER_AC(Doc.of(INTEGER)//
				.unit(WATT)//
				.text("Maximum AC charging power per connector (v2.4+)"));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGING_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default DoubleReadChannel getChargingCurrentChannel() {
		return this.channel(ChannelId.CHARGING_CURRENT);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGING_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default DoubleReadChannel getChargingVoltageChannel() {
		return this.channel(ChannelId.CHARGING_VOLTAGE);
	}
}
