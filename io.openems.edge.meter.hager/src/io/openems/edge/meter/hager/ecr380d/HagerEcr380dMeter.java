package io.openems.edge.meter.hager.ecr380d;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ElementToChannelScaleFactorConverter;
import io.openems.edge.bridge.modbus.api.element.ModbusRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * Hager ECR380D Modbus-Register and scaling described at <a href=
 * "https://assets.hager.com/step-content/P/HA_46515737/Document/std.lang.all/Modbus_table_Summary_V1_14.pdf">Modbus
 * table Summary – rev. 1.14</a>. 
 *
 */
public interface HagerEcr380dMeter extends ElectricityMeter, OpenemsComponent {

	/**
	 * Chapter 5. Device Identification (start 0x1000)
	 */
	public static final int DEVICE_START_ADDRESS = 0x1000;
	/**
	 * Chapter 6. Instantaneous Measures (Start 0xB000)
	 */
	public static final int INSTANTANEOUS_MEASURES_START_ADDRESS = 0xB000;
	/**
	 * Chapter 7. Energies (kWh, ΣT) – Start 0xB060	
	 */
	public static final int ENERGY_START_ADDRESS = 0xB060;
	/**
	 * Chapter 12. Energies per Phase (kWh)
	 */
	public static final int ENERGY_PER_PHASE_START_ADDRESS = 0xB180;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// 5. Device Identification (start 0x1000)
		VENDOR_NAME(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.VERY_LOW).text("Vendor Name"), new StringWordElement(DEVICE_START_ADDRESS | 0x0000, 16), ElementToChannelConverter.DIRECT_1_TO_1),
		PRODUCT_CODE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.VERY_LOW).text("Product Code"), new StringWordElement(DEVICE_START_ADDRESS | 0x0010, 16), ElementToChannelConverter.DIRECT_1_TO_1),
		SW_VERSION(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.VERY_LOW).text("Software version"), new StringWordElement(DEVICE_START_ADDRESS | 0x0020, 2), ElementToChannelConverter.DIRECT_1_TO_1),
		VENDOR_URL(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.VERY_LOW).text("Product Name"), new StringWordElement(DEVICE_START_ADDRESS | 0x0022, 16), ElementToChannelConverter.DIRECT_1_TO_1),
		PRODUCT_NAME(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.VERY_LOW).text("Product Name"), new StringWordElement(DEVICE_START_ADDRESS | 0x0032, 16), ElementToChannelConverter.DIRECT_1_TO_1),
		MODEL_NAME(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.VERY_LOW).text("Model Name"), new StringWordElement(DEVICE_START_ADDRESS | 0x0042, 16), ElementToChannelConverter.DIRECT_1_TO_1),
		APPLICATION_NAME(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.VERY_LOW).text("User application name"), new StringWordElement(DEVICE_START_ADDRESS | 0x0052, 16), ElementToChannelConverter.DIRECT_1_TO_1),
		HW_VERSION(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.VERY_LOW).text("Hardware version"), new StringWordElement(DEVICE_START_ADDRESS | 0x0062, 2), ElementToChannelConverter.DIRECT_1_TO_1),
		PRODUCTION_CODE_SERIAL_NUMBER(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.VERY_LOW).text("Production code serial number"), new StringWordElement(DEVICE_START_ADDRESS | 0x0064, 16), ElementToChannelConverter.DIRECT_1_TO_1),
		PRODUCTION_SITE_CODE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.VERY_LOW).text("Production site code"), new StringWordElement(DEVICE_START_ADDRESS | 0x0074, 2), ElementToChannelConverter.DIRECT_1_TO_1),
		PRODUCTION_DAY_OF_YEAR(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.VERY_LOW).text("Production day of the year"), new UnsignedWordElement(DEVICE_START_ADDRESS | 0x0076), ElementToChannelConverter.DIRECT_1_TO_1),
		PRODUCTION_YEAR(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.VERY_LOW).text("Production year"), new UnsignedWordElement(DEVICE_START_ADDRESS | 0x0077), ElementToChannelConverter.DIRECT_1_TO_1),
		
		// 6. Instantaneous Measures (Start 0xB000)
		// V_L1_NEUTRAL(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.VOLT).persistencePriority(PersistencePriority.HIGH).text("Power L1 to N"),new UnsignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0000), ElementToChannelScaleFactorConverter.DIVIDE(100.0d)), 
		// V_L2_NEUTRAL(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.VOLT).persistencePriority(PersistencePriority.HIGH).text("Power L2 to N"), new UnsignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0001), ElementToChannelScaleFactorConverter.DIVIDE(100.0d)), 
		// V_L3_NEUTRAL(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.VOLT).persistencePriority(PersistencePriority.HIGH).text("Power L3 to N"), new UnsignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0002), ElementToChannelScaleFactorConverter.DIVIDE(100.0d)), 
		V_L1_L2(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.MILLIVOLT).persistencePriority(PersistencePriority.HIGH).text("Power L1 to L2"), new UnsignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0003), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), 
		V_L2_L3(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.MILLIVOLT).persistencePriority(PersistencePriority.HIGH).text("Power L2 to L3"), new UnsignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0004), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), 
		V_L3_L1(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.MILLIVOLT).persistencePriority(PersistencePriority.HIGH).text("Power L3 to L1"), new UnsignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0005), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), 
		// FREQUENCY(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.HERTZ).persistencePriority(PersistencePriority.HIGH).text("Frequency"), new UnsignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0006), ElementToChannelScaleFactorConverter.DIVIDE(100.0d)), 
		/* 0x0007+8 not defined */
		// I_L1(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.MILLIAMPERE).persistencePriority(PersistencePriority.HIGH).text("Current L1"), new UnsignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0009), ElementToChannelConverter.DIRECT_1_TO_1), 
		// I_L2(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.MILLIAMPERE).persistencePriority(PersistencePriority.HIGH).text("Current L2"), new UnsignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x000B), ElementToChannelConverter.DIRECT_1_TO_1), 
		// I_L3(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.MILLIAMPERE).persistencePriority(PersistencePriority.HIGH).text("Current L3"), new UnsignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x000D), ElementToChannelConverter.DIRECT_1_TO_1), 
		I_NEUTRAL(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.MILLIAMPERE).persistencePriority(PersistencePriority.HIGH).text("Current neutral"), new UnsignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x000F), ElementToChannelConverter.DIRECT_1_TO_1), 

		// P_SUM(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.WATT).persistencePriority(PersistencePriority.HIGH).text("Power Sum(L1,L2,L3)"), new SignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0011), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), // kW/100, S32
		// Q_SUM(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.VOLT_AMPERE_REACTIVE).persistencePriority(PersistencePriority.HIGH).text("Power reactive Sum(L1,L2,L3)"), new SignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0013), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), // kvar/100, S32
		S_SUM(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.VOLT_AMPERE).persistencePriority(PersistencePriority.HIGH).text("Power apparent Sum(L1,L2,L3)"), new UnsignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0015), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), // kVA/100, U32
		PF_SUM_IEC(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY).unit(Unit.PERCENT).persistencePriority(PersistencePriority.HIGH).text("Power factor IEC Sum(L1,L2,L3)"), new SignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0017), ElementToChannelScaleFactorConverter.DIVIDE(1000.0d)), // promille, S16, -1000..+1000
		PF_SUM_IEEE(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY).unit(Unit.PERCENT).persistencePriority(PersistencePriority.HIGH).text("Power factor IEEE Sum(L1,L2,L3)"), new SignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0018), ElementToChannelScaleFactorConverter.DIVIDE(1000.0d)), // promille, S16, -1000..+1000

		// P_L1(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.WATT).persistencePriority(PersistencePriority.HIGH).text("Power L1"), new SignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0019), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), // kW/100, S32
		// P_L2(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.WATT).persistencePriority(PersistencePriority.HIGH).text("Power L2"), new SignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x001B), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), // kW/100, S32
		// P_L3(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.WATT).persistencePriority(PersistencePriority.HIGH).text("Power L3"), new SignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x001D), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), // kW/100, S32

		// Q_L1(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.VOLT_AMPERE_REACTIVE).persistencePriority(PersistencePriority.HIGH).text("Power reactive L1"), new SignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x001F), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), // kvar/100, S32
		// Q_L2(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.VOLT_AMPERE_REACTIVE).persistencePriority(PersistencePriority.HIGH).text("Power reactive L2"), new SignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0021), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), // kvar/100, S32
		// Q_L3(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.VOLT_AMPERE_REACTIVE).persistencePriority(PersistencePriority.HIGH).text("Power reactive L3"), new SignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0023), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), // kvar/100, S32

		S_L1(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.VOLT_AMPERE).persistencePriority(PersistencePriority.HIGH).text("Power apparent L1"), new UnsignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0025), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), // kVA/100, U32
		S_L2(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.VOLT_AMPERE).persistencePriority(PersistencePriority.HIGH).text("Power apparent L2"), new UnsignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0027), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), // kVA/100, U32
		S_L3(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.VOLT_AMPERE).persistencePriority(PersistencePriority.HIGH).text("Power apparent L3"), new UnsignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0029), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), // kVA/100, U32

		PF_L1_IEC(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY).unit(Unit.PERCENT).persistencePriority(PersistencePriority.HIGH).text("Power factor IEC L1"), new SignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x002B), ElementToChannelScaleFactorConverter.DIVIDE(1000.0d)), // promille, S16, -1000..+1000
		PF_L2_IEC(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY).unit(Unit.PERCENT).persistencePriority(PersistencePriority.HIGH).text("Power factor IEC L2"), new SignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x002C), ElementToChannelScaleFactorConverter.DIVIDE(1000.0d)), // promille, S16, -1000..+1000
		PF_L3_IEC(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY).unit(Unit.PERCENT).persistencePriority(PersistencePriority.HIGH).text("Power factor IEC L3"), new SignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x002D), ElementToChannelScaleFactorConverter.DIVIDE(1000.0d)), // promille, S16, -1000..+1000

		PF_L1_IEEE(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY).unit(Unit.PERCENT).persistencePriority(PersistencePriority.HIGH).text("Power factor IEEE L1"), new SignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x002E), ElementToChannelScaleFactorConverter.DIVIDE(1000.0d)), // promille, S16, -1000..+1000
		PF_L2_IEEE(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY).unit(Unit.PERCENT).persistencePriority(PersistencePriority.HIGH).text("Power factor IEEE L2"), new SignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x002F), ElementToChannelScaleFactorConverter.DIVIDE(1000.0d)), // promille, S16, -1000..+1000
		PF_L3_IEEE(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY).unit(Unit.PERCENT).persistencePriority(PersistencePriority.HIGH).text("Power factor IEEE L3"), new SignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0030), ElementToChannelScaleFactorConverter.DIVIDE(1000.0d)), // promille, S16, -1000..+1000
		
		// 7. Energies (kWh, ΣT) – Start 0xB060
		// EA_PLUS_SUM(Doc.of(OpenemsType.LONG).unit(Unit.WATT_HOURS).persistencePriority(PersistencePriority.HIGH).text("Imported Energy"), new UnsignedDoublewordElement(ENERGY_START_ADDRESS | 0x0000), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)), // Ea+ (ΣT) kWh, U32
		ER_PLUS_SUM(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.HIGH).text("Imported reactive Energy"), new UnsignedDoublewordElement(ENERGY_START_ADDRESS | 0x0002), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)), // Ea+ (ΣT) kWh, U32
		// EA_MINUS_SUM(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY).unit(Unit.WATT_HOURS).persistencePriority(PersistencePriority.HIGH).text("Exported Energy"), new UnsignedDoublewordElement(ENERGY_START_ADDRESS | 0x0004), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)), // Ea- (ΣT) kWh, U32
		ER_MINUS_SUM(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.HIGH).text("Exported reactive Energy"), new UnsignedDoublewordElement(ENERGY_START_ADDRESS | 0x0006), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)), // Ea- (ΣT) kWh, U32
		EA_PLUS_DETAILED_SUM(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY).unit(Unit.WATT_HOURS).persistencePriority(PersistencePriority.HIGH).text("Imported Energy"), new UnsignedDoublewordElement(ENERGY_START_ADDRESS | 0x0008), ElementToChannelConverter.DIRECT_1_TO_1), // Ea+ (ΣT) kWh, U32
		EA_MINUS_DETAILED_SUM(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY).unit(Unit.WATT_HOURS).persistencePriority(PersistencePriority.HIGH).text("Exported Energy"), new UnsignedDoublewordElement(ENERGY_START_ADDRESS | 0x000A), ElementToChannelConverter.DIRECT_1_TO_1), // Ea- (ΣT) kWh, U32

		// 12. Energies per Phase (kWh)
		// EA_PLUS_L1(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY).unit(Unit.WATT_HOURS).persistencePriority(PersistencePriority.HIGH).text("Imported Energy L1"), new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x0000), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)),
		// EA_PLUS_L2(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY).unit(Unit.WATT_HOURS).persistencePriority(PersistencePriority.HIGH).text("Imported Energy L2"), new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x0002), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)),
		// EA_PLUS_L3(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY).unit(Unit.WATT_HOURS).persistencePriority(PersistencePriority.HIGH).text("Imported Energy L3"), new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x0004), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)),
		// EA_MINUS_L1(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY).unit(Unit.WATT_HOURS).persistencePriority(PersistencePriority.HIGH).text("Exported Energy L1"), new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x0006), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)),
		// EA_MINUS_L2(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY).unit(Unit.WATT_HOURS).persistencePriority(PersistencePriority.HIGH).text("Exported Energy L2"), new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x0008), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)),
		// EA_MINUS_L3(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY).unit(Unit.WATT_HOURS).persistencePriority(PersistencePriority.HIGH).text("Exported Energy L3"), new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x000A), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)),

		ER_PLUS_L1(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.HIGH).text("Imported reactive Energy L1"), new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x000C), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)),
		ER_PLUS_L2(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.HIGH).text("Imported reactive Energy L2"), new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x000E), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)),
		ER_PLUS_L3(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.HIGH).text("Imported reactive Energy L3"), new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x0010), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)),
		ER_MINUS_L1(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.HIGH).text("Exported reactive Energy L1"), new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x0012), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)),
		ER_MINUS_L2(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.HIGH).text("Exported reactive Energy L2"), new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x0014), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)),
		ER_MINUS_L3(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.HIGH).text("Exported reactive Energy L3"), new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x0016), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)),

		;
		
		private final Doc doc;
		private final ModbusRegisterElement<?, ?> address;
		private final ElementToChannelConverter converter;

		ChannelId(final Doc doc, final ModbusRegisterElement<?, ?> address, ElementToChannelConverter converter) {
			this.doc = doc;
			this.address = address;
			this.converter = converter;
		}

		/**
		 * Accessor method for the {@link Doc} instance of this channel.
		 * 
		 * @return the Doc instance
		 * @see ChannelId#doc()
		 */
		@Override
		public Doc doc() {
			return this.doc;
		}

		/**
		 * Accessor method for the modbus register number of this channel.
		 * 
		 * @return the modbus register number for this channel
		 */
		public ModbusRegisterElement<?, ?> address() {
			return this.address;
		}
		
		/**
		 * Accessor method for the modbus channel converter of this channel.
		 * 
		 * @return the converter for this channel
		 */
		public ElementToChannelConverter converter() {
			return this.converter;
		}
	}
}