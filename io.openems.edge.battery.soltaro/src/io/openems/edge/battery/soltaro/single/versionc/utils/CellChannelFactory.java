package io.openems.edge.battery.soltaro.single.versionc.utils;

import io.openems.common.channel.Unit;
import io.openems.edge.battery.soltaro.ChannelIdImpl;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.IntegerDoc;

public class CellChannelFactory {

	/**
	 * Creates string number with leading zeros.
	 */
	private static final String NUMBER_FORMAT = "%03d";

	public static enum Type {
		TEMPERATURE(//
				"_TEMPERATURE", //
				Unit.DEZIDEGREE_CELSIUS, //
				Constants.TEMPERATURE_ADDRESS_OFFSET, //
				Constants.TEMPERATURE_SENSORS_PER_MODULE), //
		VOLTAGE(//
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
			this.offset = Constants.ADDRESS_OFFSET + offset;
			this.sensorsPerModule = sensorsPerModule;
		}

		public int getOffset() {
			return this.offset;
		}

		public int getSensorsPerModule() {
			return this.sensorsPerModule;
		}
	}

	public static ChannelId create(Type type, int index) {
		String key = "CLUSTER_1_BATTERY_" + String.format(NUMBER_FORMAT, index) + type.key;
		IntegerDoc doc = new IntegerDoc();
		doc.unit(type.unit);
		return new ChannelIdImpl(key, doc);
	}

}
