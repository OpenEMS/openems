package io.openems.edge.temperature.jumo;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface JumoHydroTransS20 extends OpenemsComponent, ModbusComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		TEMPERATURE_UNIT(Doc.of(TemperatureUnit.values())), //
		HUMIDITY_UNIT(Doc.of(HumidityUnit.values())), //

		TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		RELATIVE_HUMIDITY(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PERCENT)), //
		CARBON_DIOXIDE(Doc.of(OpenemsType.FLOAT)//
				.unit(Unit.PARTS_PER_MILLION)), //
		GENERAL_ERROR(Doc.of(Level.FAULT)//
				.text("General error ")), //
		CONFIGURATION_FAULT(Doc.of(Level.FAULT)//
				.text("Configuration faulty, The checksum of the configuration data is faulty (CRC).")), //
		CALIBRATION_FAULT(Doc.of(Level.FAULT)//
				.text("Calibration faulty, The checksum of the calibration data is faulty (CRC).")), //
		DEVICE_NOT_CALIBRATED(Doc.of(Level.FAULT)//
				.text("Either, The calibration flag has not been set or there is no calibration data.")), //
		HUMIDITY_SENSOR_FAULTY(Doc.of(Level.FAULT)//
				.text("Either, The humidity and temperature sensor is faulty or the communication to the sensor is impaired.")), //
		CO2_SENSOR_FAULTY(Doc.of(Level.FAULT)//
				.text("CO2 sensor faulty or the communication to the sensor is impaired.")), //
		DEVICE_RESTART_REQUIRED(Doc.of(Level.FAULT)//
				.text("Device restart required")), //
		DEW_POINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		MIXING_RATION(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)), //
		ABSOLUTE_HUMIDITY(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.GRAMS_PER_CUBIC_METER)), //
		SPECIFIC_ENTHALPY(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOJOULES_PER_KILOGRAM)), //
		WET_BULB_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		FROST_POINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		PARTIAL_WATER_VAPOR_PRESSURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIBAR)), //
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

	/*
	 * Gets the Channel for {@link ChannelId#TEMPERATURE}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getTemperatureChannel() {
		return this.channel(ChannelId.TEMPERATURE);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#TEMPERATURE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperature() {
		return this.getTemperatureChannel().value();
	}

	/*
	 * Gets the Channel for {@link ChannelId#RELATIVE_HUMIDITY}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getRelativeHumidityChannel() {
		return this.channel(ChannelId.RELATIVE_HUMIDITY);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#RELATIVE_HUMIDITY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getRelativeHumidity() {
		return this.getRelativeHumidityChannel().value();
	}

}