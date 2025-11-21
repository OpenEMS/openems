package io.openems.edge.evse.chargepoint.abl;

import static io.openems.common.channel.AccessMode.WRITE_ONLY;
import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.PersistencePriority.VERY_LOW;
import static io.openems.common.channel.Unit.AMPERE;
import static io.openems.common.channel.Unit.MILLIAMPERE;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
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
		 * <li>Persistence: HIGH
		 * </ul>
		 */
		CHARGING_STATE(Doc.of(ChargingState.values()) //
				.persistencePriority(HIGH) //
				.text("Current charging state (A1, B1, B2, C2, etc.)")),

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
				.unit(MILLIAMPERE) //
				.text("Debug mirror of SET_CHARGING_CURRENT for monitoring")),

		/**
		 * Set the charging current limit.
		 *
		 * <ul>
		 * <li>Interface: EvseChargePointAbl
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * <li>Range: 6000...32000 mA (6-32A)
		 * </ul>
		 */
		SET_CHARGING_CURRENT(Doc.of(INTEGER) //
				.unit(MILLIAMPERE) //
				.accessMode(WRITE_ONLY) //
				.onChannelSetNextWriteMirrorToDebugChannel(DEBUG_SET_CHARGING_CURRENT) //
				.text("Set charging current limit in milliampere (6000-32000 mA)")),

		/**
		 * Indicates if electric vehicle is connected (UCP voltage &lt;= 10V).
		 *
		 * <ul>
		 * <li>Interface: EvseChargePointAbl
		 * <li>Type: Boolean
		 * <li>Persistence: HIGH
		 * </ul>
		 */
		EV_CONNECTED(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(HIGH) //
				.text("Electric vehicle plugged in (true = UCP ≤ 10V)")),

		/**
		 * Current of phase 1 in Ampere (resolution 1A).
		 *
		 * <ul>
		 * <li>Interface: EvseChargePointAbl
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>Range: 0...80A (0x64 = phase current meter not available)
		 * <li>Persistence: VERY_LOW
		 * </ul>
		 */
		PHASE_CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(AMPERE) //
				.persistencePriority(VERY_LOW) //
				.text("Phase 1 current in Ampere (1A resolution, null if meter unavailable)")),

		/**
		 * Current of phase 2 in Ampere (resolution 1A).
		 *
		 * <ul>
		 * <li>Interface: EvseChargePointAbl
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>Range: 0...80A (0x64 = phase current meter not available)
		 * <li>Persistence: VERY_LOW
		 * </ul>
		 */
		PHASE_CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(AMPERE) //
				.persistencePriority(VERY_LOW) //
				.text("Phase 2 current in Ampere (1A resolution, null if meter unavailable)")),

		/**
		 * Current of phase 3 in Ampere (resolution 1A).
		 *
		 * <ul>
		 * <li>Interface: EvseChargePointAbl
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>Range: 0...80A (0x64 = phase current meter not available)
		 * <li>Persistence: VERY_LOW
		 * </ul>
		 */
		PHASE_CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(AMPERE) //
				.persistencePriority(VERY_LOW) //
				.text("Phase 3 current in Ampere (1A resolution, null if meter unavailable)")),

		/**
		 * Device ID of the ABL charging station.
		 *
		 * <ul>
		 * <li>Interface: EvseChargePointAbl
		 * <li>Type: Integer
		 * <li>Range: 0x01...0x10 (1-16)
		 * <li>Persistence: VERY_LOW
		 * </ul>
		 */
		DEVICE_ID(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(VERY_LOW) //
				.text("ABL device ID (1-16)")),

		/**
		 * Firmware version major.minor.
		 *
		 * <ul>
		 * <li>Interface: EvseChargePointAbl
		 * <li>Type: String
		 * <li>Persistence: VERY_LOW
		 * </ul>
		 */
		FIRMWARE_VERSION(Doc.of(OpenemsType.STRING) //
				.persistencePriority(VERY_LOW) //
				.text("Firmware version (e.g., \"1.2\")")),

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
	public default BooleanReadChannel getEvConnectedChannel() {
		return this.channel(ChannelId.EV_CONNECTED);
	}

	/**
	 * Gets the read value for Channel {@link ChannelId#EV_CONNECTED}.
	 *
	 * @return the Channel read value
	 */
	public default Value<Boolean> getEvConnected() {
		return this.getEvConnectedChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#PHASE_CURRENT_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getPhaseCurrentL1Channel() {
		return this.channel(ChannelId.PHASE_CURRENT_L1);
	}

	/**
	 * Gets the read value for Channel {@link ChannelId#PHASE_CURRENT_L1}.
	 *
	 * @return the Channel read value in Ampere
	 */
	public default Value<Integer> getPhaseCurrentL1() {
		return this.getPhaseCurrentL1Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#PHASE_CURRENT_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getPhaseCurrentL2Channel() {
		return this.channel(ChannelId.PHASE_CURRENT_L2);
	}

	/**
	 * Gets the read value for Channel {@link ChannelId#PHASE_CURRENT_L2}.
	 *
	 * @return the Channel read value in Ampere
	 */
	public default Value<Integer> getPhaseCurrentL2() {
		return this.getPhaseCurrentL2Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#PHASE_CURRENT_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getPhaseCurrentL3Channel() {
		return this.channel(ChannelId.PHASE_CURRENT_L3);
	}

	/**
	 * Gets the read value for Channel {@link ChannelId#PHASE_CURRENT_L3}.
	 *
	 * @return the Channel read value in Ampere
	 */
	public default Value<Integer> getPhaseCurrentL3() {
		return this.getPhaseCurrentL3Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#DEVICE_ID}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDeviceIdChannel() {
		return this.channel(ChannelId.DEVICE_ID);
	}

	/**
	 * Gets the read value for Channel {@link ChannelId#DEVICE_ID}.
	 *
	 * @return the Channel read value (1-16)
	 */
	public default Value<Integer> getDeviceId() {
		return this.getDeviceIdChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#FIRMWARE_VERSION}.
	 *
	 * @return the Channel
	 */
	public default StringReadChannel getFirmwareVersionChannel() {
		return this.channel(ChannelId.FIRMWARE_VERSION);
	}

	/**
	 * Gets the read value for Channel {@link ChannelId#FIRMWARE_VERSION}.
	 *
	 * @return the Channel read value (e.g., "1.2")
	 */
	public default Value<String> getFirmwareVersion() {
		return this.getFirmwareVersionChannel().value();
	}
}
