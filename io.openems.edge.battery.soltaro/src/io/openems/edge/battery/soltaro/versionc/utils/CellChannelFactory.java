package io.openems.edge.battery.soltaro.versionc.utils;

import io.openems.common.channel.Unit;
import io.openems.edge.battery.soltaro.cluster.enums.Rack;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.ChannelId.ChannelIdImpl;
import io.openems.edge.common.channel.IntegerDoc;

public class CellChannelFactory {

	/**
	 * Creates string number with leading zeros.
	 */
	private static final String NUMBER_FORMAT = "%03d";

	public static enum Type {
		TEMPERATURE_SINGLE(//
				"_TEMPERATURE", //
				Unit.DEZIDEGREE_CELSIUS, //
				Constants.TEMPERATURE_ADDRESS_OFFSET + Constants.SINGLE_RACK_ADDRESS_OFFSET, //
				Constants.TEMPERATURE_SENSORS_PER_MODULE), //
		VOLTAGE_SINGLE(//
				"_VOLTAGE", //
				Unit.MILLIVOLT, //
				Constants.VOLTAGE_ADDRESS_OFFSET + Constants.SINGLE_RACK_ADDRESS_OFFSET, //
				Constants.VOLTAGE_SENSORS_PER_MODULE), //

		TEMPERATURE_CLUSTER(//
				"_TEMPERATURE", //
				Unit.DEZIDEGREE_CELSIUS, //
				Constants.TEMPERATURE_ADDRESS_OFFSET, //
				Constants.TEMPERATURE_SENSORS_PER_MODULE), //
		VOLTAGE_CLUSTER(//
				"_VOLTAGE", //
				Unit.MILLIVOLT, //
				Constants.VOLTAGE_ADDRESS_OFFSET, //
				Constants.VOLTAGE_SENSORS_PER_MODULE); //

		private final String key;
		private final Unit unit;
		private final int offset;
		private final int sensorsPerModule;

		private Type(String key, Unit unit, int offset, int sensorsPerModule) {
			this.key = key;
			this.unit = unit;
			this.offset = +offset;
			this.sensorsPerModule = sensorsPerModule;
		}

		/**
		 * Gets the Offset.
		 *
		 * @return int
		 */
		public int getOffset() {
			return this.offset;
		}

		/**
		 * Gets the SensorsPerModule.
		 *
		 * @return int
		 */
		public int getSensorsPerModule() {
			return this.sensorsPerModule;
		}
	}

	/**
	 * Create ChannelId for a Single-Rack.
	 *
	 * @param type  the {@link Type}
	 * @param index the index
	 * @return the ChannelId
	 */
	public static ChannelId create(Type type, int index) {
		var key = "CLUSTER_1_BATTERY_" + String.format(NUMBER_FORMAT, index) + type.key;
		var doc = new IntegerDoc();
		doc.unit(type.unit);
		return new ChannelIdImpl(key, doc);
	}

	/**
	 * Create ChannelId for a Rack as part of a Cluster.
	 *
	 * @param rack  the {@link Rack}
	 * @param type  the {@link Type}
	 * @param index the index
	 * @return the ChannelId
	 */
	public static ChannelId create(Rack rack, Type type, int index) {
		var key = rack.getChannelIdPrefix() + String.format(NUMBER_FORMAT, index) + type.key;
		var doc = new IntegerDoc();
		doc.unit(type.unit);
		return new ChannelIdImpl(key, doc);
	}

}
