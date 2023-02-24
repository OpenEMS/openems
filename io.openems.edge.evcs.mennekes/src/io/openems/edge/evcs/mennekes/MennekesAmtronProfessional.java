package io.openems.edge.evcs.mennekes;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Mennekes Amtron Professional charging protocol interface.
 * 
 * <p>
 * Defines the interface for Mennekes Amtron Professional (eichrechtskonform)
 */
public interface MennekesAmtronProfessional extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Apply charge power limit.
		 * 
		 * <p>
		 * WriteChannel for the modbus register to apply the charge power given by the
		 * applyChargePowerLimit method
		 */
		APPLY_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH)),

		FIRMWARE_VERSION(Doc.of(OpenemsType.STRING).unit(Unit.NONE)
				.text("Saves the Firmware Version as Long, hast to converted to hex to interpret")),
		RAW_FIRMWARE_VERSION(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)
				.text("Saves the Firmware Version as Long, hast to converted to hex to interpret").onInit(channel -> {
					channel.onUpdate(newValue -> {
						StringReadChannel firmwareVersionChannel = channel.getComponent()
								.channel(MennekesAmtronProfessional.ChannelId.FIRMWARE_VERSION);
						var rawFirmwareVersionValue = newValue.asOptional();
						if (rawFirmwareVersionValue.isPresent()) {
							int rawFirmwareVersion = (int) rawFirmwareVersionValue.get();
							String hex = Integer.toHexString(rawFirmwareVersion);
							int l = hex.length();
							byte[] data = new byte[l / 2];
							for (int i = 0; i < l; i += 2) {
								data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
										+ Character.digit(hex.charAt(i + 1), 16));
							}
							String firmwareVersion = new String(data);
							firmwareVersionChannel.setNextValue(firmwareVersion);
						}
					});
				})),
		OCPP_CP_STATUS(Doc.of(MennekesOcppState.values()).initialValue(MennekesOcppState.UNDEFINED)),

		/**
		 * ERROR CODES 1.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: Integer
		 * <li>Unit: None
		 * </ul>
		 */

		ERROR_CODES_1(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)),

		/**
		 * ERROR CODES 2.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: Integer
		 * <li>Unit: None
		 * </ul>
		 */

		ERROR_CODES_2(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)),

		/**
		 * ERROR CODES 3.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: Integer
		 * <li>Unit: None
		 * </ul>
		 */
		ERROR_CODES_3(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)),

		/**
		 * ERROR CODES 4.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: Integer
		 * <li>Unit: None
		 * </ul>
		 */
		ERROR_CODES_4(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)),

		/**
		 * VEHICLE STATE.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: VehicleState
		 * <li>Unit: None
		 * </ul>
		 */
		VEHICLE_STATE(Doc.of(VehicleState.values()).initialValue(VehicleState.UNDEFINED)),

		/**
		 * CP AVAILABILITY.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: Boolean
		 * <li>Unit: None
		 * </ul>
		 */
		CP_AVAILABILITY(Doc.of(OpenemsType.BOOLEAN).unit(Unit.NONE)),

		/**
		 * CP AVAILABILITY.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: Boolean
		 * <li>Unit: None
		 * </ul>
		 */
		SAFE_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
		// OPERATOR_CURRENT_LIMIT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)), // Not
		// yet available
		// PLUG_LOCK_STATUS(Doc.of(OpenemsType.BOOLEAN).unit(Unit.NONE)), // Not yet
		// available

		/**
		 * Active Power L1.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		ACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Active Power L2.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		ACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Active Power L3.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		ACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Current L1.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * </ul>
		 */
		CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Current L2.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * </ul>
		 */
		CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Current L3.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * </ul>
		 */
		CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),

		// Charge Process Information
		// REQUIRED_ENERGY_EV(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

		/**
		 * MAX CURRENT EV.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: Integer
		 * <li>Unit: Ampere
		 * </ul>
		 */
		MAX_CURRENT_EV(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)),

		/**
		 * MIN CURRENT LIMIT.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: Integer
		 * <li>Unit: Ampere
		 * </ul>
		 */
		MIN_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)),

		/**
		 * CHARGE DURATION.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Type: Integer
		 * <li>Unit: SECONDS
		 * </ul>
		 */
		CHARGE_DURATION(Doc.of(OpenemsType.INTEGER).unit(Unit.SECONDS)),

		// Energy Management Control

		/**
		 * EMS CURRENT LIMIT.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: AMPERE
		 * </ul>
		 */
		EMS_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),

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
	 * Gets the Channel for {@link ChannelId#APPLY_CURRENT_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getApplyCurrentLimitChannel() {
		return this.channel(ChannelId.APPLY_CURRENT_LIMIT);
	}

	/**
	 * Sets the charge current limit of the EVCS in [A] on
	 * {@link ChannelId#APPLY_CURRENT_LIMIT} Channel.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setApplyCurrentLimit(Integer value) throws OpenemsNamedException {
		this.getApplyCurrentLimitChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#FIRMWARE_VERSION}.
	 * 
	 * @return returns the channel
	 *
	 */
	public default StringReadChannel getFirmwareVersionChannel() {
		return this.channel(ChannelId.FIRMWARE_VERSION);
	}

	/**
	 * Internal method to get the 'nextValue' of {@link ChannelId#FIRMWARE_VERSION}
	 * Channel.
	 *
	 * @return value of firmware version value
	 */
	public default Value<String> getFirmwareVersionValue() {
		return this.getFirmwareVersionChannel().getNextValue();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#FIRMWARE_VERSION}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setFirmwareVersion(String value) {
		this.getFirmwareVersionChannel().setNextValue(value);
	}

	/**
	 * Gets the Firmware Version as an Integer. See
	 * {@link ChannelId#RAW_FIRMWARE_VERSION}.
	 *
	 * @return the Channel {@link Value}
	 */

	public default Value<Integer> getRawFirmwareVersionValue() {
		return this.getRawFirmwareVersionChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#RAW_FIRMWARE_VERSION}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getRawFirmwareVersionChannel() {
		return this.channel(ChannelId.RAW_FIRMWARE_VERSION);
	}

	/**
	 * Gets the Channel for {@link ChannelId#OCPP_CP_STATUS}.
	 *
	 * @return the Channel
	 */
	public default Channel<MennekesOcppState> getOcppCpStatusChannel() {
		return this.channel(ChannelId.OCPP_CP_STATUS);
	}

	/**
	 * Gets the OCPP Status of the EVCS charging station. See
	 * {@link ChannelId#OCPP_CP_STATUS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default MennekesOcppState getOcppCpStatus() {
		return this.getOcppCpStatusChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ERROR_CODES_1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getErrorCode1Channel() {
		return this.channel(ChannelId.ERROR_CODES_1);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ERROR_CODES_2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getErrorCode2Channel() {
		return this.channel(ChannelId.ERROR_CODES_2);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ERROR_CODES_3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getErrorCode3Channel() {
		return this.channel(ChannelId.ERROR_CODES_3);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ERROR_CODES_4}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getErrorCode4Channel() {
		return this.channel(ChannelId.ERROR_CODES_4);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CP_AVAILABILITY}.
	 *
	 * @return the Channel
	 */

	public default BooleanReadChannel getCpAvailabilityChannel() {
		return this.channel(ChannelId.CP_AVAILABILITY);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_CURRENT_EV}.
	 *
	 * @return the Channel
	 */

	public default IntegerReadChannel getMaxCurrentEvChannel() {
		return this.channel(ChannelId.MAX_CURRENT_EV);
	}

	/**
	 * Gets the Channel for {@link ChannelId#VEHICLE_STATE}.
	 *
	 * @return the Channel
	 */

	public default Channel<VehicleState> getVehicleStateChannel() {
		return this.channel(ChannelId.VEHICLE_STATE);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_POWER_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getActivePowerL1Channel() {
		return this.channel(ChannelId.ACTIVE_POWER_L1);
	}

	/**
	 * Gets the Active Power on L1 in [W]. See {@link ChannelId#ACTIVE_POWER_L1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getActivePowerL1() {
		return this.getActivePowerL1Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_POWER_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getActivePowerL2Channel() {
		return this.channel(ChannelId.ACTIVE_POWER_L2);
	}

	/**
	 * Gets the Active Power on L2 in [W]. See {@link ChannelId#ACTIVE_POWER_L2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getActivePowerL2() {
		return this.getActivePowerL2Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_POWER_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getActivePowerL3Channel() {
		return this.channel(ChannelId.ACTIVE_POWER_L3);
	}

	/**
	 * Gets the Active Power on L3 in [W]. See {@link ChannelId#ACTIVE_POWER_L3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getActivePowerL3() {
		return this.getActivePowerL3Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CURRENT_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentL1Channel() {
		return this.channel(ChannelId.CURRENT_L1);
	}

	/**
	 * Gets the Current on L1 in [mA]. See {@link ChannelId#CURRENT_L1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentL1() {
		return this.getCurrentL1Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CURRENT_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentL2Channel() {
		return this.channel(ChannelId.CURRENT_L2);
	}

	/**
	 * Gets the Current on L2 in [mA]. See {@link ChannelId#CURRENT_L2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentL2() {
		return this.getCurrentL2Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CURRENT_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentL3Channel() {
		return this.channel(ChannelId.CURRENT_L3);
	}

	/**
	 * Gets the Current on L3 in [mA]. See {@link ChannelId#CURRENT_L3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentL3() {
		return this.getCurrentL3Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#MIN_CURRENT_LIMIT}.
	 *
	 * @return the Channel
	 */

	public default IntegerReadChannel getMinCurrentLimitChannel() {
		return this.channel(ChannelId.MIN_CURRENT_LIMIT);
	}

	/**
	 * Gets the Minimum current limit in [A] of the AC charger. See
	 * {@link ChannelId#MIN_CURRENT_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */

	public default Value<Integer> getMinCurrentLimit() {
		return this.getMinCurrentLimitChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#EMS_CURRENT_LIMIT}
	 * Channel. Sets the value in Ampere as an Integer-Value.
	 *
	 * @return returns the EMS current limit
	 */

	public default Value<Integer> getEmsCurrentLimit() {
		return this.getEmsCurrentLimitChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#EMS_CURRENT_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEmsCurrentLimitChannel() {
		return this.channel(ChannelId.EMS_CURRENT_LIMIT);
	}
}
