package io.openems.edge.bridge.modbus.sunspec;

import java.util.Optional;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
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
import io.openems.edge.common.channel.EnumDoc;

/**
 * Holds one "Point" or "Register" within a SunSpec "Model" or "Block".
 */
public interface SunSpecPoint {

	/**
	 * Gets the Point-ID.
	 *
	 * <p>
	 * This method refers to {@link Enum#name()}.
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
		public final Unit unit;
		public final SunSChannelId<?> channelId;
		public final Optional<String> scaleFactor;
		public final OptionsEnum[] options;

		public PointImpl(String channelId, String label, String description, String notes, PointType type,
				boolean mandatory, AccessMode accessMode, Unit unit, String scaleFactor, OptionsEnum[] options) {
			this.label = label;
			this.description = description;
			this.notes = notes;
			this.type = type;
			this.mandatory = mandatory;
			this.accessMode = accessMode;
			this.unit = unit;
			this.scaleFactor = Optional.ofNullable(scaleFactor);
			if (options.length == 0) {
				this.channelId = new SunSChannelId<>(channelId, //
						Doc.of(this.getMatchingOpenemsType(this.scaleFactor.isPresent())) //
								.unit(unit) //
								.accessMode(accessMode));
			} else {
				this.channelId = new SunSChannelId<>(channelId, //
						new EnumDoc(options) //
								.accessMode(accessMode));
			}
			this.options = options;
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
		}

		/**
		 * Gets the {@link OpenemsType} that matches this SunSpec-Type.
		 *
		 * @param hasScaleFactor true if this Point has a ScaleFactor. If true, a
		 *                       floating point type is applied to avoid rounding
		 *                       errors.
		 * @return the {@link OpenemsType}
		 */
		public final OpenemsType getMatchingOpenemsType(boolean hasScaleFactor) {
			if (hasScaleFactor) {
				return OpenemsType.FLOAT;
			}

			// TODO: map to floating point OpenemsType when appropriate
			switch (this.type) {
			case UINT16:
			case ACC16:
			case ENUM16:
			case BITFIELD16:
			case INT16:
			case SUNSSF:
			case COUNT:
			case INT32:
			case PAD: // ignore
			case EUI48:
			case FLOAT32: // avoid floating point numbers; FLOAT32 might not fit in INTEGER
				return OpenemsType.INTEGER;
			case ACC32:
			case BITFIELD32:
			case ENUM32:
			case IPADDR:
			case UINT32:
			case UINT64:
			case ACC64:
			case INT64:
			case IPV6ADDR:
			case FLOAT64: // avoid floating point numbers
				return OpenemsType.LONG;
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
		STRING5(5), STRING6(6), STRING7(7), STRING8(8), STRING12(12), STRING16(16), STRING20(20), STRING25(25), STRING32(32),
		/* use PAD for reserved points */
		PAD(1), IPADDR(1), IPV6ADDR(16), EUI48(6);

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
				return !value.equals(Short.MIN_VALUE /* -32768 */);
			case UINT16:
			case ENUM16:
			case BITFIELD16:
			case COUNT:
				return !value.equals(65535);
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
				return !value.equals(4294967295L);
			case INT64:
				return !value.equals(0x8000000000000000L); // TODO correct?
			case UINT64:
				return !value.equals(0xFFFFFFFFFFFFFFFFL); // TODO correct?
			case FLOAT32:
				return !value.equals(Float.NaN);
			case FLOAT64:
				return false; // TODO not implemented
			case PAD:
				// This point is never needed/reserved
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

	public static enum PointCategory {
		NONE, MEASUREMENT, METERED, STATUS, EVENT, SETTING, CONTROL;
	}
}