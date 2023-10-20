package io.openems.edge.bridge.modbus.sunspec;

import static io.openems.edge.bridge.modbus.api.element.AbstractModbusElement.FillElementsPriority.HIGH;

import java.util.Optional;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.FloatQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
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
		public final ModbusElement generateModbusElement(Integer startAddress) {
			return switch (this.type) {
			case UINT16, ACC16, ENUM16, BITFIELD16 -> new UnsignedWordElement(startAddress);
			case SUNSSF -> new SignedWordElement(startAddress)//
					.fillElementsPriority(HIGH);
			case INT16, COUNT -> new SignedWordElement(startAddress);
			case UINT32, ACC32, ENUM32, BITFIELD32, IPADDR -> new UnsignedDoublewordElement(startAddress);
			case INT32 -> new SignedDoublewordElement(startAddress);
			case UINT64, ACC64 -> new UnsignedQuadruplewordElement(startAddress);
			case INT64 -> new SignedQuadruplewordElement(startAddress);
			case FLOAT32 -> new FloatDoublewordElement(startAddress);
			case PAD -> new DummyRegisterElement(startAddress);
			case FLOAT64 -> new FloatQuadruplewordElement(startAddress);
			case EUI48 -> null;
			case IPV6ADDR // TODO this would be UINT128
				-> null;
			case STRING2 -> new StringWordElement(startAddress, 2);
			case STRING4 -> new StringWordElement(startAddress, 4);
			case STRING5 -> new StringWordElement(startAddress, 5);
			case STRING6 -> new StringWordElement(startAddress, 6);
			case STRING7 -> new StringWordElement(startAddress, 7);
			case STRING8 -> new StringWordElement(startAddress, 8);
			case STRING12 -> new StringWordElement(startAddress, 12);
			case STRING16 -> new StringWordElement(startAddress, 16);
			case STRING20 -> new StringWordElement(startAddress, 20);
			case STRING25 -> new StringWordElement(startAddress, 25);
			case STRING32 -> new StringWordElement(startAddress, 32);
			default -> throw new IllegalArgumentException(
					"Point [" + this.label + "]: Type [" + this.type + "] is not supported!");
			};
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
			return switch (this.type) {
			case UINT16, ACC16, ENUM16, BITFIELD16, INT16, SUNSSF, COUNT, INT32, PAD, // ignore
					EUI48, FLOAT32 // avoid floating point numbers; FLOAT32 might not fit in INTEGER
				-> OpenemsType.INTEGER;
			case ACC32, BITFIELD32, ENUM32, IPADDR, UINT32, UINT64, ACC64, INT64, IPV6ADDR, //
					FLOAT64 // avoid floating point numbers
				-> OpenemsType.LONG;
			case STRING2, STRING4, STRING5, STRING6, STRING7, STRING8, STRING12, STRING16, STRING20, STRING25,
					STRING32 ->
				OpenemsType.STRING;
			default -> throw new IllegalArgumentException("Unable to get matching OpenemsType for " + this.type);

			};
		}
	}

	public static enum PointType {
		INT16(1), UINT16(1), COUNT(1), ACC16(1), INT32(2), UINT32(2), FLOAT32(2), ACC32(2), INT64(4), UINT64(4),
		FLOAT64(4), ACC64(4), ENUM16(1), ENUM32(2), BITFIELD16(1), BITFIELD32(2), SUNSSF(1), STRING2(2), STRING4(4),
		STRING5(5), STRING6(6), STRING7(7), STRING8(8), STRING12(12), STRING16(16), STRING20(20), STRING25(25),
		STRING32(32),
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
			return switch (type) {
			case INT16, SUNSSF -> !value.equals(Short.MIN_VALUE /* -32768 */);
			case UINT16, ENUM16, BITFIELD16, COUNT -> !value.equals(65535);
			case ACC16, ACC32, IPADDR, ACC64, IPV6ADDR -> !value.equals(0);
			case INT32 -> !value.equals(0x80000000); // TODO correct?
			case UINT32, ENUM32, BITFIELD32 -> !value.equals(4294967295L);
			case INT64 -> !value.equals(0x8000000000000000L); // TODO correct?
			case UINT64 -> !value.equals(0xFFFFFFFFFFFFFFFFL); // TODO correct?
			case FLOAT32 -> !value.equals(Float.NaN);
			case FLOAT64 -> false; // TODO not implemented
			case PAD // This point is never needed/reserved
				-> false;
			case STRING12, STRING16, STRING2, STRING20, STRING25, STRING32, STRING4, STRING5, STRING6, STRING7,
					STRING8 ->
				!"".equals(value);
			case EUI48 -> false; // TODO not implemented
			default -> false;
			};
		}
	}

	public static enum PointCategory {
		NONE, MEASUREMENT, METERED, STATUS, EVENT, SETTING, CONTROL;
	}
}