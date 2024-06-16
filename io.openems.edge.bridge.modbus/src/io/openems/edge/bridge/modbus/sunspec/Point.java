package io.openems.edge.bridge.modbus.sunspec;

import static io.openems.common.channel.AccessMode.READ_ONLY;
import static io.openems.edge.bridge.modbus.api.element.AbstractModbusElement.FillElementsPriority.HIGH;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
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
import io.openems.edge.common.channel.internal.AbstractDoc;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;

/**
 * The internal PointImpl object for easier handling in Enums.
 */
public abstract sealed class Point {

	/**
	 * A Point that relates to a {@link ModbusElement}.
	 */
	protected static sealed interface ModbusElementPoint {

		/**
		 * Generates the {@link ModbusElement}.
		 * 
		 * @param startAddress the start-address
		 * @return the {@link ModbusElement}
		 */
		public ModbusElement generateModbusElement(int startAddress);
	}

	public static sealed interface Type {

		public static final short UNDEFINED_8 = Short.MIN_VALUE /* -32768 */;
		public static final int UNDEFINED_16 = 65535;
		public static final long UNDEFINED_32 = 4294967295L;

		/**
		 * Returns true if the value represents a 'defined' value in SunSpec.
		 *
		 * @param value the value; never null
		 * @return true for defined values
		 */
		public boolean isDefined(Object value);
	}

	public final String name;
	public final String description;
	public final Point.Type type;
	public final boolean mandatory;
	public final AccessMode accessMode;

	public Point(String name, String label, String description, Point.Type type, boolean mandatory,
			AccessMode accessMode) {
		this.name = name;
		this.description = description;
		this.type = type;
		this.mandatory = mandatory;
		this.accessMode = accessMode;
	}

	/**
	 * Returns true if the value represents a 'defined' value in SunSpec.
	 *
	 * @param value the value
	 * @return true for defined values
	 */
	public boolean isDefined(Object value) {
		if (this.type != null && value != null) {
			return this.type.isDefined(value);
		}
		return false;
	}

	/**
	 * Represents a Point with a ChannelId.
	 */
	public abstract static sealed class ChannelIdPoint extends Point
			permits Point.ValuePoint, Point.EnumPoint, Point.ScaleFactorPoint, Point.BitPoint {

		public final ChannelId channelId;

		private ChannelIdPoint(String name, String label, String description, Point.Type type, boolean mandatory,
				AccessMode accessMode, AbstractDoc<?> doc) {
			super(name, label, description, type, mandatory, accessMode);
			if (!label.isBlank() && !description.isBlank()) {
				doc.text(label + ". " + description);
			} else if (!label.isBlank()) {
				doc.text(label);
			} else {
				doc.text(description);
			}
			doc.accessMode(accessMode);
			this.channelId = new SunSChannelId<>(name, doc);
		}

	}

	/**
	 * Represents a Point with a discrete value.
	 */
	public static non-sealed class ValuePoint extends Point.ChannelIdPoint implements ModbusElementPoint {

		public static enum Type implements Point.Type {
			INT16(1), UINT16(1), COUNT(1), ACC16(1), INT32(2), UINT32(2), FLOAT32(2), ACC32(2), INT64(4), UINT64(4),
			FLOAT64(4), ACC64(4), STRING2(2), STRING4(4), STRING5(5), STRING6(6), STRING7(7), STRING8(8), STRING12(12),
			STRING16(16), STRING20(20), STRING25(25), STRING32(32),
			/* use PAD for reserved points */
			PAD(1), IPADDR(1), IPV6ADDR(16), EUI48(6);

			public final int length;

			private Type(int length) {
				this.length = length;
			}

			@Override
			public boolean isDefined(Object value) {
				return switch (this) {
				case INT16 -> !value.equals(UNDEFINED_8);
				case UINT16, COUNT -> !value.equals(UNDEFINED_16);
				case ACC16, ACC32, IPADDR, ACC64, IPV6ADDR -> !value.equals(0);
				case INT32 -> !value.equals(0x80000000); // TODO correct?
				case UINT32 -> !value.equals(UNDEFINED_32);
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
				};
			}
		}

		private final Type type;

		private ValuePoint(String name, String label, String description, ValuePoint.Type type, boolean mandatory,
				AccessMode accessMode, Unit unit, OpenemsTypeDoc<?> doc) {
			super(name, label, description, type, mandatory, accessMode, doc.unit(unit));
			this.type = type;
		}

		public ValuePoint(String name, String label, String description, ValuePoint.Type type, boolean mandatory,
				AccessMode accessMode, Unit unit) {
			this(name, label, description, type, mandatory, accessMode, unit, //
					Doc.of(//
							switch (type) {
							case UINT16, ACC16, INT16, COUNT, INT32, PAD, // ignore
									EUI48, FLOAT32 // avoid floating point numbers; FLOAT32 might not fit in INTEGER
								-> OpenemsType.INTEGER;
							case ACC32, IPADDR, UINT32, UINT64, ACC64, INT64, IPV6ADDR, //
									FLOAT64 // avoid floating point numbers
								-> OpenemsType.LONG;
							case STRING2, STRING4, STRING5, STRING6, STRING7, STRING8, STRING12, STRING16, STRING20,
									STRING25, STRING32 //
								-> OpenemsType.STRING;
							}));
		}

		@Override
		public ModbusElement generateModbusElement(int startAddress) {
			return switch (this.type) {
			case UINT16, ACC16 //
				-> new UnsignedWordElement(startAddress);
			case INT16, COUNT //
				-> new SignedWordElement(startAddress);
			case UINT32, ACC32, IPADDR //
				-> new UnsignedDoublewordElement(startAddress);
			case INT32 //
				-> new SignedDoublewordElement(startAddress);
			case UINT64, ACC64 //
				-> new UnsignedQuadruplewordElement(startAddress);
			case INT64 //
				-> new SignedQuadruplewordElement(startAddress);
			case FLOAT32 //
				-> new FloatDoublewordElement(startAddress);
			case PAD //
				-> new DummyRegisterElement(startAddress);
			case FLOAT64 //
				-> new FloatQuadruplewordElement(startAddress);
			case EUI48, IPV6ADDR // this would be UINT128
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
			};
		}
	}

	/**
	 * Represents a Point with a defined Scale-Factor.
	 */
	public static final class ScaledValuePoint extends Point.ValuePoint {
		public final String scaleFactor;

		public ScaledValuePoint(String name, String label, String description, ValuePoint.Type type, boolean mandatory,
				AccessMode accessMode, Unit unit, String scaleFactor) {
			super(name, label, description, type, mandatory, accessMode, unit, Doc.of(OpenemsType.FLOAT));
			this.scaleFactor = scaleFactor;
		}
	}

	/**
	 * Represents a Scale-Factor Point.
	 */
	public static final class ScaleFactorPoint extends Point.ChannelIdPoint implements ModbusElementPoint {

		public static enum Type implements Point.Type {
			SUNSSF(1);

			public final int length;

			private Type(int length) {
				this.length = length;
			}

			@Override
			public boolean isDefined(Object value) {
				return switch (this) {
				case SUNSSF -> !value.equals(UNDEFINED_8);
				};
			}
		}

		public ScaleFactorPoint(String name, String label, String description) {
			super(name, label, description, Type.SUNSSF, true, READ_ONLY, Doc.of(OpenemsType.INTEGER));
		}

		@Override
		public ModbusElement generateModbusElement(int startAddress) {
			return new SignedWordElement(startAddress) //
					.fillElementsPriority(HIGH);
		}
	}

	/**
	 * Represents a Point with an Enum value.
	 */
	public static final class EnumPoint extends Point.ChannelIdPoint implements ModbusElementPoint {

		public static enum Type implements Point.Type {
			ENUM16(1), ENUM32(2);

			public final int length;

			private Type(int length) {
				this.length = length;
			}

			@Override
			public boolean isDefined(Object value) {
				return switch (this) {
				case ENUM16 -> !value.equals(UNDEFINED_16);
				case ENUM32 -> !value.equals(UNDEFINED_32);
				};
			}
		}

		private final Type type;

		public EnumPoint(String name, String label, String description, EnumPoint.Type type, boolean mandatory,
				AccessMode accessMode, OptionsEnum[] options) {
			super(name, label, description, type, mandatory, accessMode, Doc.of(options));
			this.type = type;
		}

		@Override
		public ModbusElement generateModbusElement(int startAddress) {
			return switch (this.type) {
			case ENUM16 //
				-> new UnsignedWordElement(startAddress);
			case ENUM32 //
				-> new UnsignedDoublewordElement(startAddress);
			};
		}

	}

	/**
	 * Represents a Point with BitField values.
	 */
	public static final class BitFieldPoint extends Point {

		public interface SunSpecBitPoint extends SunSpecPoint {
			@Override
			public BitPoint get();
		}

		public static enum Type implements Point.Type {
			BITFIELD16(1), BITFIELD32(2);

			public final int length;

			private Type(int length) {
				this.length = length;
			}

			@Override
			public boolean isDefined(Object value) {
				return switch (this) {
				case BITFIELD16 -> !value.equals(UNDEFINED_16);
				case BITFIELD32 -> !value.equals(UNDEFINED_32);
				};
			}
		}

		public final SunSpecBitPoint[] points;
		public final BitFieldPoint.Type type;

		public BitFieldPoint(String name, String label, String description, BitFieldPoint.Type type, boolean mandatory,
				AccessMode accessMode, SunSpecBitPoint[] points) {
			super(name, label, description, type, mandatory, accessMode); //
			this.points = points;
			this.type = type;
		}

		/**
		 * Generates the {@link ModbusElement}s.
		 * 
		 * @param parent               the parent
		 *                             {@link AbstractOpenemsSunSpecComponent}
		 * @param addChannel           callback to add a Channel to parent (because the
		 *                             method is protected)
		 * @param startAddress         the start-address
		 * @param alternativeBitPoints alternative {@link SunSpecBitPoint}s
		 * @return the {@link ModbusElement}s
		 */
		public List<ModbusElement> generateModbusElements(AbstractOpenemsSunSpecComponent parent,
				Consumer<ChannelId> addChannel, int startAddress, SunSpecBitPoint[] alternativeBitPoints) {
			final SunSpecBitPoint[] points;
			if (alternativeBitPoints != null && alternativeBitPoints.length > 0) {
				points = alternativeBitPoints;
			} else {
				points = this.points;
			}

			return switch (this.type) {
			case BITFIELD16 -> {
				var bwe = new BitsWordElement(startAddress, parent);
				Arrays.stream(points).forEach(ssbp -> {
					var bit = ssbp.get().bit;
					var channelId = ssbp.get().channelId;
					addChannel.accept(channelId);
					bwe.bit(bit, channelId);
				});
				yield List.of(bwe);
			}
			case BITFIELD32 -> {
				var bwe0 = new BitsWordElement(startAddress, parent);
				var bwe1 = new BitsWordElement(startAddress + 1, parent);
				Arrays.stream(points).forEach(ssbp -> {
					var bit = ssbp.get().bit;
					var channelId = ssbp.get().channelId;
					addChannel.accept(channelId);
					if (bit > 15) {
						bwe1.bit(bit - 16, channelId);
					} else {
						bwe0.bit(bit, channelId);
					}
				});
				yield List.of(bwe0, bwe1);
			}
			};
		}
	}

	/**
	 * Represents one Bit within a BitFieldPoint.
	 */
	public static final class BitPoint extends Point.ChannelIdPoint {

		public final int bit;

		private BitPoint(int bit, String name, String label, AbstractDoc<?> doc) {
			super(name, label, "", null /* point type */, false, AccessMode.READ_ONLY, doc);
			this.bit = bit;
		}

		public BitPoint(int bit, String name, String label) {
			this(bit, name, label, Doc.of(OpenemsType.BOOLEAN));
		}

		public BitPoint(int bit, String name, String label, Level level) {
			this(bit, name, label, Doc.of(level));
		}
	}
}