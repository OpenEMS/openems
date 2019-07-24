package io.openems.edge.bridge.modbus.sunspec;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;

public class SunSpecModelUtils {

	/**
	 * Holds one "Point" or "Register" within a SunSpec "Model" or "Block".
	 */
	public static interface Point {

		/**
		 * Gets the Point-ID.
		 * 
		 * <p>
		 * This method referrs to {@link Enum#name()}.
		 * 
		 * @return the ID.
		 */
		public String name();

		/**
		 * The internal PointImpl object for easier handling in Enums.
		 * 
		 * @return the internal PointImpl
		 */
		public PointImpl get();

		/**
		 * Returns true if the value represents a 'defined' value in SunSpec.
		 * 
		 * @param type  the PointType
		 * @param value the value
		 * @return true for defined values
		 */
		public default boolean isDefined(Object value) {
			return PointType.isDefined(this.get().type, value);
		}

		/**
		 * Gets the {@link ChannelId} for this Point.
		 * 
		 * @return the ChannelId.
		 */
		public default SunSChannelId<?> getChannelId() {
			return this.get().channelId;
		}
	}

	/**
	 * The internal PointImpl object for easier handling in Enums.
	 */
	public static class PointImpl {
		public final String label;
		public final String description;
		public final String notes;
		public final PointType type;
		public final boolean mandatory;
		public final AccessMode accessMode;
		public final SunSChannelId<?> channelId;

		public PointImpl(String channelId, String label, String description, String notes, PointType type,
				boolean mandatory, AccessMode accessMode) {
			this.label = label;
			this.description = description;
			this.notes = notes;
			this.type = type;
			this.mandatory = mandatory;
			this.accessMode = accessMode;
			this.channelId = new SunSChannelId<>(channelId, //
					Doc.of(this.getMatchingOpenemsType()) //
							.accessMode(accessMode));
		}

		/**
		 * Generates a Modbus Element for the given point + startAddress.
		 * 
		 * @param startAddress the startAddress of the Point
		 * @return a new Modbus Element
		 */
		public final AbstractModbusElement<?> generateModbusElement(Integer startAddress) {
			switch (this.type) {
			case UINT16:
			case ACC16:
			case ENUM16:
			case BITFIELD16:
				return new UnsignedWordElement(startAddress);
			case INT16:
			case SUNSSF:
			case COUNT:
				return new SignedWordElement(startAddress);
			case UINT32:
			case ACC32:
			case ENUM32:
			case BITFIELD32:
			case IPADDR:
				return new UnsignedDoublewordElement(startAddress);
			case INT32:
				return new SignedDoublewordElement(startAddress);
			case UINT64:
			case ACC64:
				return new UnsignedQuadruplewordElement(startAddress);
			case INT64:
				return new SignedQuadruplewordElement(startAddress);
			case FLOAT32:
				return new FloatDoublewordElement(startAddress);
			case PAD:
				return new DummyRegisterElement(startAddress);
			case FLOAT64:
				break;
			case EUI48:
				break;
			case IPV6ADDR:
				// TODO this would be UINT128
				break;
			case STRING2:
				return new StringWordElement(startAddress, 2);
			case STRING4:
				return new StringWordElement(startAddress, 4);
			case STRING5:
				return new StringWordElement(startAddress, 5);
			case STRING6:
				return new StringWordElement(startAddress, 6);
			case STRING7:
				return new StringWordElement(startAddress, 7);
			case STRING8:
				return new StringWordElement(startAddress, 8);
			case STRING12:
				return new StringWordElement(startAddress, 12);
			case STRING16:
				return new StringWordElement(startAddress, 16);
			case STRING20:
				return new StringWordElement(startAddress, 20);
			case STRING25:
				return new StringWordElement(startAddress, 25);
			}
			throw new IllegalArgumentException(
					"Point [" + this.label + "]: Type [" + this.type + "] is not supported!");
		};

		/**
		 * Gets the {@link OpenemsType} that matches this SunSpec-Type.
		 * 
		 * @return the {@link OpenemsType}
		 */
		public final OpenemsType getMatchingOpenemsType() {
			switch (this.type) {
			case UINT16:
			case ACC16:
			case ENUM16:
			case BITFIELD16:
			case INT16:
			case SUNSSF:
			case COUNT:
			case UINT32:
			case ACC32:
			case ENUM32:
			case BITFIELD32:
			case IPADDR:
			case INT32:
			case PAD: // ignore
			case EUI48:
				return OpenemsType.INTEGER;
			case UINT64:
			case ACC64:
			case INT64:
			case IPV6ADDR:
				return OpenemsType.LONG;
			case FLOAT32:
			case FLOAT64:
				return OpenemsType.DOUBLE;
			case STRING2:
			case STRING4:
			case STRING5:
			case STRING6:
			case STRING7:
			case STRING8:
			case STRING12:
			case STRING16:
			case STRING20:
			case STRING25:
				return OpenemsType.STRING;
			}
			throw new IllegalArgumentException("Unable to get matching OpenemsType for " + this.type);
		}
	}

	public static enum PointType {
		INT16(1), UINT16(1), COUNT(1), ACC16(1), INT32(2), UINT32(2), FLOAT32(2), ACC32(2), INT64(4), UINT64(4),
		FLOAT64(4), ACC64(4), ENUM16(1), ENUM32(2), BITFIELD16(1), BITFIELD32(2), SUNSSF(1), STRING2(2), STRING4(4),
		STRING5(5), STRING6(6), STRING7(7), STRING8(8), STRING12(12), STRING16(16), STRING20(20), STRING25(25), PAD(1),
		IPADDR(1), IPV6ADDR(16), EUI48(6);

		public final int length;

		private PointType(int length) {
			this.length = length;
		}

		/**
		 * Returns true if the value represents a 'defined' value in SunSpec.
		 * 
		 * @param type  the PointType
		 * @param value the value
		 * @return true for defined values
		 */
		public static boolean isDefined(PointType type, Object value) {
			if (value == null) {
				return false;
			}
			switch (type) {
			case INT16:
			case SUNSSF:
				return !value.equals(-32768);
			case UINT16:
			case ENUM16:
			case BITFIELD16:
			case COUNT:
				return !value.equals(0xFFFF);
			case ACC16:
			case ACC32:
			case IPADDR:
			case ACC64:
			case IPV6ADDR:
				return !value.equals(0);
			case INT32:
				return !value.equals(0x80000000); // TODO correct?
			case UINT32:
			case ENUM32:
			case BITFIELD32:
				return !value.equals(4294967295l);
			case INT64:
				return !value.equals(0x8000000000000000l); // TODO correct?
			case UINT64:
				return !value.equals(0xFFFFFFFFFFFFFFFFl); // TODO correct?
			case FLOAT32:
				return !value.equals(Float.NaN);
			case FLOAT64:
				return false; // TODO not implemented
			case PAD:
				// This value is never needed
				return false;
			case STRING12:
			case STRING16:
			case STRING2:
			case STRING20:
			case STRING25:
			case STRING4:
			case STRING5:
			case STRING6:
			case STRING7:
			case STRING8:
				return !"".equals(value);
			case EUI48:
				return false; // TODO not implemented
			}
			return false;
		}
	}

	protected static enum PointCategory {
		NONE, MEASUREMENT, METERED, STATUS, EVENT, SETTING, CONTROL;
	}

	public static class SunSChannelId<T> implements io.openems.edge.common.channel.ChannelId {

		private final String name;
		private final OpenemsTypeDoc<T> doc;

		public SunSChannelId(String name, OpenemsTypeDoc<T> doc) {
			this.name = name;
			this.doc = doc;
		}

		@Override
		public String name() {
			return this.name;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}
