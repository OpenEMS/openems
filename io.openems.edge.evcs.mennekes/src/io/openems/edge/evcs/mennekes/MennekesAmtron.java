package io.openems.edge.evcs.mennekes;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Hypercharger EV charging protocol interface.
 * 
 * <p>
 * Defines the interface for Alpitronic Hypercharger
 */
public interface MennekesAmtron extends OpenemsComponent {

	public enum Connector {
		SLOT_0(100), //
		SLOT_1(200), //
		SLOT_2(300), //
		SLOT_3(400);

		public final int modbusOffset;

		private Connector(int modbusOffset) {
			this.modbusOffset = modbusOffset;
		}
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		FIRMWARE_VERSION(Doc.of(OpenemsType.STRING).unit(Unit.NONE)
				.text("Saves the Firmware Version as Long, hast to converted to hex to interpret")),
		RAW_FIRMWARE_VERSION(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)
				.text("Saves the Firmware Version as Long, hast to converted to hex to interpret")
				.onInit(channel ->{
						channel.onUpdate(newValue -> {
							StringReadChannel firmwareVersionChannel = channel.getComponent()
									.channel(MennekesAmtron.ChannelId.FIRMWARE_VERSION);
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
		OCPP_CP_STATUS(Doc.of(OcppStateMennekes.values()).initialValue(OcppStateMennekes.UNDEFINED)),

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
		 * <li>Type: Integer
		 * <li>Unit: None
		 * </ul>
		 */
		VEHICLE_STATE(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)),
		
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
		// OPERATOR_CURRENT_LIMIT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)), // Not yet available
		// PLUG_LOCK_STATUS(Doc.of(OpenemsType.BOOLEAN).unit(Unit.NONE)), // Not yet available
		
		TOTAL_ENERGY(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS)), // Total energy that has been charged over the lifespan of the charging point
			
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
		 * SCHEDULED DEPARTURE TIME.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Type: String
		 * * </ul>
		 */
		
		SCHEDULED_DEPARTURE_TIME(Doc.of(OpenemsType.STRING)),
		
		/**
		 * SCHEDULED DEPARTURE HOUR.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Type: Integer
		 * <li>Unit: HOUR
		 * </ul>
		 */
		SCHEDULED_DEPARTURE_HOUR(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),
		
		/**
		 * SCHEDULED DEPARTURE MINUTE.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Type: Integer
		 * <li>Unit: Minute
		 * </ul>
		 */
		SCHEDULED_DEPARTURE_MINUTE(Doc.of(OpenemsType.INTEGER).unit(Unit.MINUTE)),
		
		/**
		 * RAW SCHEDULED DEPARTURE TIME.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Type: Integer
		 * </ul>
		 */
		RAW_SCHEDULED_DEPARTURE_TIME(Doc.of(OpenemsType.INTEGER)),
		
		/**
		 * SCHEDULED DEPARTURE DATE.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Type: String
		 * </ul>
		 */
		SCHEDULED_DEPARTURE_DATE(Doc.of(OpenemsType.STRING)),
		
		/**
		 * SCHEDULED DEPARTURE YEAR.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Type: Integer
		 * <li>Unit: YEAR
		 * </ul>
		 */
//		SCHEDULED_DEPARTURE_YEAR(Doc.of(OpenemsType.INTEGER).unit(Unit.YEAR)),
//		
//		/**
//		 * SCHEDULED DEPARTURE MONTH.
//		 *
//		 * <ul>
//		 * <li>Interface: MennekesAmtron 
//		 * <li>Type: Integer
//		 * <li>Unit: MONTH
//		 * </ul>
//		 */
//		SCHEDULED_DEPARTURE_MONTH(Doc.of(OpenemsType.INTEGER).unit(Unit.MONTH)),
//		
//		/**
//		 * SCHEDULED DEPARTURE DAY.
//		 *
//		 * <ul>
//		 * <li>Interface: MennekesAmtron 
//		 * <li>Type: Integer
//		 * <li>Unit: DAY
//		 * </ul>
//		 */
//		SCHEDULED_DEPARTURE_DAY(Doc.of(OpenemsType.INTEGER).unit(Unit.DAY)),
//		
//		/**
//		 * RAW SCHEDULED DEPARTURE DATE.
//		 *
//		 * <ul>
//		 * <li>Interface: MennekesAmtron 
//		 * <li>Type: Integer
//		 * </ul>
//		 */
		RAW_SCHEDULED_DEPARTURE_DATE(Doc.of(OpenemsType.INTEGER)),
		
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
		
		/**
		 * CHARGING SESSION START TIME.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Type: String
		 * </ul>
		 */
		CHARGING_SESSION_START_TIME(Doc.of(OpenemsType.STRING)),
		
		/**
		 * RAW CHARGING SESSION START TIME.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Type: Integer
		 * </ul>
		 */
		RAW_CHARGING_SESSION_START_TIME(Doc.of(OpenemsType.INTEGER)),
		
		/**
		 * CHARGING STOP TIME .
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Type: String
		 * </ul>
		 */
		CHARGING_STOP_TIME(Doc.of(OpenemsType.STRING)),
		
		/**
		 * CHARGING STOP HOUR .
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Type: Integer
		 * <li>Unit: HOUR
		 * </ul>
		 */
		CHARGING_STOP_HOUR(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),
		
		/**CHARGING STOP MINUTE.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Type: Integer
		 * <li>Unit: MINUTE
		 * </ul>
		 */
		CHARGING_STOP_MINUTE(Doc.of(OpenemsType.INTEGER).unit(Unit.MINUTE)),
		
		/**
		 *RAW CHARGING STOP TIME.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Type: Integer 
		 * </ul>
		 */
		RAW_CHARGING_STOP_TIME(Doc.of(OpenemsType.INTEGER)),
		
		/**
		 *RAW CHARGING STOP TIME.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Type: Integer 
		 * </ul>
		 */
		
		// OCPP-Data
		
		/**
		 *OCPP ID TAG.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Type: String
		 * </ul>
		 */
		OCPP_ID_TAG(Doc.of(OpenemsType.STRING)),
	
		/**
		 *RAW ID TAG 1.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Type: Integer 
		 * </ul>
		 */
		RAW_ID_TAG_1(Doc.of(OpenemsType.INTEGER)),
		
		/**
		 *RAW ID TAG 2.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Type: Integer 
		 * </ul>
		 */
		RAW_ID_TAG_2(Doc.of(OpenemsType.INTEGER)),
		
		/**
		 *RAW ID TAG 3.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Type: Integer 
		 * </ul>
		 */
		RAW_ID_TAG_3(Doc.of(OpenemsType.INTEGER)),
		
		/**
		 *RAW ID TAG 4.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Type: Integer 
		 * </ul>
		 */
		RAW_ID_TAG_4(Doc.of(OpenemsType.INTEGER)),
		
		/**
		 *RAW ID TAG 5.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Type: Integer 
		 * </ul>
		 */
		RAW_ID_TAG_5(Doc.of(OpenemsType.INTEGER)),
		
		/**
		 *OCPP EVCCID TAG.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Type: String 
		 * </ul>
		 */
		OCPP_EVCCID_TAG(Doc.of(OpenemsType.STRING)),
		
		/**
		 *RAW EVCCID TAG 1.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Type: Integer 
		 * </ul>
		 */
		RAW_EVCCID_TAG_1(Doc.of(OpenemsType.INTEGER)),
		
		/**
		 *RAW EVCCID TAG 2.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Type: Integer 
		 * </ul>
		 */
		RAW_EVCCID_TAG_2(Doc.of(OpenemsType.INTEGER)),
		
		/**
		 *RAW EVCCID TAG 3.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Type: Integer 
		 * </ul>
		 */
		RAW_EVCCID_TAG_3(Doc.of(OpenemsType.INTEGER)),
		

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

		/**
		 * EMS CURRENT LIMIT WRITE.
		 *
		 * <ul>
		 * <li>Interface: MennekesAmtron 
		 * <li>Writable
		 * <li>Type: Integer
		 * <li>Unit: AMPERE
		 * </ul>
		 */
		EMS_CURRENT_LIMIT_WRITE(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.WRITE_ONLY))
		
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
	 * Gets the Channel for {@link ChannelId#OCPP_CP_STATUS}.
	 *
	 * @return the Channel
	 */
	public default Channel<OcppStateMennekes> getOcppCpStatusChannel() {
		return this.channel(ChannelId.OCPP_CP_STATUS);
	}

	/**
	 * Gets the OCPP Status of the EVCS charging station. See {@link ChannelId#OCPP_CP_STATUS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default OcppStateMennekes getOcppCpStatus() {
		return this.getOcppCpStatusChannel().value().asEnum();
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
	
	public default IntegerReadChannel getVehicleStateChannel() {
		return this.channel(ChannelId.VEHICLE_STATE);
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#TOTAL_ENERGY}.
	 *
	 * @return the Channel
	 */
	
	public default IntegerReadChannel getTotalEnergyChannel() {
		return this.channel(ChannelId.TOTAL_ENERGY);
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
	 * Gets the Minimum current limit in [A] of the AC charger. See {@link ChannelId#MIN_CURRENT_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	
	public default Value<Integer> getMinCurrentLimit() {
		return this.getMinCurrentLimitChannel().value();
	}

	
	/**
	 * Internal method to set the 'nextValue' on  
	 * {@link ChannelId#EMS_CURRENT_LIMIT} Channel.
	 * Sets the value in Ampere as an Integer-Value.
	 *
	 * @param value the next value
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
	
	/**
	 * Gets the Channel for {@link ChannelId#EMS_CURRENT_LIMIT_WRITE}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getEmsCurrentLimitWriteChannel() {
		return this.channel(ChannelId.EMS_CURRENT_LIMIT_WRITE);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#EMS_CURRENT_LIMIT_WRITE}
	 * Channel.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException 
	 */
	public default void _setEmsCurrentLimitWrite(Integer value) throws OpenemsNamedException {
		this.getEmsCurrentLimitWriteChannel().setNextWriteValue(value);
		this.getEmsCurrentLimitWriteChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#EMS_CURRENT_LIMIT_WRITE}
	 * Channel.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException 
	 */
	public default void _setEmsCurrentLimitWrite(int value) throws OpenemsNamedException {
		this.getEmsCurrentLimitWriteChannel().setNextWriteValue(value);
		this.getEmsCurrentLimitWriteChannel().setNextValue(value);
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#RAW_FIRMWARE_VERSION
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getRawFirmwareVersionChannel() {
		return this.channel(ChannelId.RAW_FIRMWARE_VERSION);
	}
	
	/**
	 * Gets the Firmware Version as an Integer. See {@link ChannelId#RAW_FIRMWARE_VERSION}.
	 *
	 * @return the Channel {@link Value}
	 */
	
	public default Value<Integer> getRawFirmwareVersion() {
		return this.getRawFirmwareVersionChannel().value();
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#RAW_CHARGING_SESSION_START_TIME
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getRawChargingSessionStartTimeChannel() {
		return this.channel(ChannelId.RAW_CHARGING_SESSION_START_TIME);
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#RAW_CHARGING_END_TIME
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getRawChargingEndTimeChannel() {
		return this.channel(ChannelId.RAW_CHARGING_STOP_TIME);
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#RAW_SCHEDULED_DEPARTURE_TIME
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getRawScheduledDepartureTimeChannel() {
		return this.channel(ChannelId.RAW_SCHEDULED_DEPARTURE_TIME);
	}
	
	/**
	 * Gets the Firmware Version as an Integer. See {@link ChannelId#RAW_SCHEDULED_DEPARTURE_TIME}.
	 *
	 * @return the Channel {@link Value}
	 */
	
	public default Value<Integer> getRawScheduledDepartureTime() {
		return this.getRawScheduledDepartureTimeChannel().value();
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#RAW_SCHEDULED_DEPARTURE_DATE
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getRawScheduledDepartureDateChannel() {
		return this.channel(ChannelId.RAW_SCHEDULED_DEPARTURE_DATE);
	}
	
	/**
	 * Gets the Firmware Version as an Integer. See {@link ChannelId#RAW_SCHEDULED_DEPARTURE_DATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	
	public default Value<Integer> getRawScheduledDepartureDate() {
		return this.getRawScheduledDepartureDateChannel().value();
	}
	

	/**
	 * Gets the Channel for {@link ChannelId#FIRMWARE_VERSION
	 *
	 * @return the Channel
	 */
	public default StringReadChannel getFirmwareVersionChannel() {
		return this.channel(ChannelId.FIRMWARE_VERSION);
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

	public default IntegerReadChannel getRawIdTag1Channel() {
		return this.channel(ChannelId.RAW_ID_TAG_1);
	}
	public default IntegerReadChannel getRawIdTag2Channel() {
		return this.channel(ChannelId.RAW_ID_TAG_2);
	}
	public default IntegerReadChannel getRawIdTag3Channel() {
		return this.channel(ChannelId.RAW_ID_TAG_3);
	}
	public default IntegerReadChannel getRawIdTag4Channel() {
		return this.channel(ChannelId.RAW_ID_TAG_4);
	}
	public default IntegerReadChannel getRawIdTag5Channel() {
		return this.channel(ChannelId.RAW_ID_TAG_5);
	}
	
	public default Value<Integer> getRawIdTag1() {
		return this.getRawIdTag1Channel().value();
	}
	public default Value<Integer> getRawIdTag2() {
		return this.getRawIdTag2Channel().value();
	}
	public default Value<Integer> getRawIdTag3() {
		return this.getRawIdTag3Channel().value();
	}
	public default Value<Integer> getRawIdTag4() {
		return this.getRawIdTag4Channel().value();
	}
	public default Value<Integer> getRawIdTag5() {
		return this.getRawIdTag5Channel().value();
	}
	
	public default StringReadChannel getOcppIdTagChannel() {
		return this.channel(ChannelId.OCPP_ID_TAG);
	}
	
	public default Value<String> getOcppIdTag() {
		return this.getOcppIdTagChannel().value();
	}
	
	public default void _setOcppIdTag(String id) {
		this.getOcppIdTagChannel().setNextValue(id);
	}

}