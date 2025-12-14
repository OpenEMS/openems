package io.openems.edge.evse.chargepoint.abl;

import static io.openems.common.channel.AccessMode.WRITE_ONLY;
import static io.openems.common.channel.Unit.MILLIAMPERE;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.chargepoint.abl.enums.ChargingState;

public interface EvseChargePointAbl extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Current charging state of the ABL EVCC.
		 *
		 * <ul>
		 * <li>Interface: EvseChargePointAbl
		 * <li>Type: Enum (ChargingState)
		 * </ul>
		 */
		CHARGING_STATE(Doc.of(ChargingState.values())),

		/**
		 * Debug channel for set charging current - mirrors the write value.
		 *
		 * <ul>
		 * <li>Interface: EvseChargePointAbl
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * </ul>
		 */
		DEBUG_SET_CHARGING_CURRENT(Doc.of(INTEGER) //
				.unit(MILLIAMPERE)),

		/**
		 * Set the charging current limit.
		 *
		 * <ul>
		 * <li>Interface: EvseChargePointAbl
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * <li>Range: 0x0050...0x03E8 (corresponds to 8%...100% duty cycle, 0 to stop)
		 * </ul>
		 */
		SET_CHARGING_CURRENT(Doc.of(INTEGER) //
				.unit(MILLIAMPERE) //
				.accessMode(WRITE_ONLY) //
				.onChannelSetNextWriteMirrorToDebugChannel(DEBUG_SET_CHARGING_CURRENT)),

		/**
		 * Indicates if UCP voltage is &lt;= 10V (EV connected).
		 *
		 * <ul>
		 * <li>Interface: EvseChargePointAbl
		 * <li>Type: Boolean
		 * </ul>
		 */
		EV_CONNECTED(Doc.of(OpenemsType.BOOLEAN)),

		/**
		 * Current of phase 1 in Ampere (resolution 1A).
		 *
		 * <ul>
		 * <li>Interface: EvseChargePointAbl
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>Range: 0...80A (0x64 = phase current meter not available)
		 * </ul>
		 */
		PHASE_CURRENT_L1(Doc.of(OpenemsType.INTEGER)),

		/**
		 * Current of phase 2 in Ampere (resolution 1A).
		 *
		 * <ul>
		 * <li>Interface: EvseChargePointAbl
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>Range: 0...80A (0x64 = phase current meter not available)
		 * </ul>
		 */
		PHASE_CURRENT_L2(Doc.of(OpenemsType.INTEGER)),

		/**
		 * Current of phase 3 in Ampere (resolution 1A).
		 *
		 * <ul>
		 * <li>Interface: EvseChargePointAbl
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>Range: 0...80A (0x64 = phase current meter not available)
		 * </ul>
		 */
		PHASE_CURRENT_L3(Doc.of(OpenemsType.INTEGER)),

		/**
		 * Device ID of the ABL charging station.
		 *
		 * <ul>
		 * <li>Interface: EvseChargePointAbl
		 * <li>Type: Integer
		 * <li>Range: 0x01...0x10
		 * </ul>
		 */
		DEVICE_ID(Doc.of(OpenemsType.INTEGER)),

		/**
		 * Firmware version major.minor.
		 *
		 * <ul>
		 * <li>Interface: EvseChargePointAbl
		 * <li>Type: String
		 * </ul>
		 */
		FIRMWARE_VERSION(Doc.of(OpenemsType.STRING)),

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

	/**
	 * Gets the Channel for {@link ChannelId#SET_CHARGING_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetChargingCurrentChannel() {
		return this.channel(ChannelId.SET_CHARGING_CURRENT);
	}

	/**
	 * Sets the write value of the {@link ChannelId#SET_CHARGING_CURRENT} Channel
	 * used to set the charge current limit of the Charge-Point in [mA].
	 *
	 * @param value the next value (in milliampere)
	 * @throws OpenemsNamedException on error
	 */
	public default void setChargingCurrent(Integer value) throws OpenemsNamedException {
		this.getSetChargingCurrentChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGING_STATE}.
	 *
	 * @return the Channel
	 */
	public default EnumReadChannel getChargingStateChannel() {
		return this.channel(ChannelId.CHARGING_STATE);
	}

	/**
	 * Gets the read value for Channel {@link ChannelId#CHARGING_STATE}.
	 *
	 * @return the Channel read value
	 */
	public default ChargingState getChargingState() {
		return this.getChargingStateChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#EV_CONNECTED}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEvConnectedChannel() {
		return this.channel(ChannelId.EV_CONNECTED);
	}
}
