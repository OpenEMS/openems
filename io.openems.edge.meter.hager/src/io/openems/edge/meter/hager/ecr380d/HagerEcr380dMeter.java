package io.openems.edge.meter.hager.ecr380d;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
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
		VENDOR_NAME(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.VERY_LOW) //
				.text("Vendor Name")),
		PRODUCT_CODE(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.VERY_LOW) //
				.text("Product Code")),
		SW_VERSION(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(PersistencePriority.VERY_LOW) //
				.text("Software version")),
		VENDOR_URL(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.VERY_LOW)
				.text("Product Name")),
		PRODUCT_NAME(Doc.of(OpenemsType.STRING)//
				.persistencePriority(PersistencePriority.VERY_LOW) //
				.text("Product Name")),
		MODEL_NAME(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.VERY_LOW) //
				.text("Model Name")),
		APPLICATION_NAME(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.VERY_LOW) //
				.text("User application name")),
		HW_VERSION(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.VERY_LOW) //
				.text("Hardware version")),
		PRODUCTION_CODE_SERIAL_NUMBER(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.VERY_LOW) //
				.text("Production code serial number")),
		PRODUCTION_SITE_CODE(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.VERY_LOW) //
				.text("Production site code")),
		PRODUCTION_DAY_OF_YEAR(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(PersistencePriority.VERY_LOW) //
				.text("Production day of the year")),
		PRODUCTION_YEAR(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(PersistencePriority.VERY_LOW) //
				.text("Production year")),
		
		// 6. Instantaneous Measures (Start 0xB000)
		// @see ElectricityMeter.ChannelId.VOLTAGE_L1
		// V_L1_NEUTRAL(Doc.of(OpenemsType.INTEGER) //
		// 	 .unit(Unit.VOLT) //
		//   .persistencePriority(PersistencePriority.HIGH) //
		//   .text("Power L1 to N")), 
		// @see ElectricityMeter.ChannelId.VOLTAGE_L2
		// V_L2_NEUTRAL(Doc.of(OpenemsType.INTEGER) //
		//   .unit(Unit.VOLT) //
		//   .persistencePriority(PersistencePriority.HIGH) //
		//   .text("Power L2 to N")), 
		// @see ElectricityMeter.ChannelId.VOLTAGE_L3
		// V_L3_NEUTRAL(Doc.of(OpenemsType.INTEGER) //
		//   .unit(Unit.VOLT) //
		//   .persistencePriority(PersistencePriority.HIGH) //
		//   .text("Power L3 to N")), 
		V_L1_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Power L1 to L2")), 
		V_L2_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Power L2 to L3")), 
		V_L3_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Power L3 to L1")), 
		// FREQUENCY(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.HERTZ).persistencePriority(PersistencePriority.HIGH).text("Frequency"), new UnsignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0006), ElementToChannelScaleFactorConverter.DIVIDE(100.0d)), 
		/* 0x0007+8 not defined */
		// @see ElectricityMeter.ChannelId.CURRENT_L1
		// I_L1(Doc.of(OpenemsType.INTEGER) //
		//    .unit(Unit.MILLIAMPERE) //
		//    .persistencePriority(PersistencePriority.HIGH) //
		//    .text("Current L1")), 
		// @see ElectricityMeter.ChannelId.CURRENT_L2
		// I_L2(Doc.of(OpenemsType.INTEGER) //
		//    .unit(Unit.MILLIAMPERE) //
		//    .persistencePriority(PersistencePriority.HIGH) //
		//    .text("Current L2")), 
		// @see ElectricityMeter.ChannelId.CURRENT_L3
		// I_L3(Doc.of(OpenemsType.INTEGER) //
		//    .unit(Unit.MILLIAMPERE) //
		//    .persistencePriority(PersistencePriority.HIGH) //
		//    .text("Current L3")), 
		I_NEUTRAL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Current neutral")), 

		// @see ElectricityMeter.ChannelId.ACTIVE_POWER
		// P_SUM(Doc.of(OpenemsType.INTEGER)
		//    .unit(Unit.WATT) //
		//    .persistencePriority(PersistencePriority.HIGH)
		//    .text("Power Sum(L1,L2,L3)")), // kW/100, S32
		// @see ElectricityMeter.ChannelId.REACTIVE_POWER
		// Q_SUM(Doc.of(OpenemsType.INTEGER) //
		//    .unit(Unit.VOLT_AMPERE_REACTIVE) //
		//    .persistencePriority(PersistencePriority.HIGH) //
		//    .text("Power reactive Sum(L1,L2,L3)")), // kvar/100, S32
		S_SUM(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Power apparent Sum(L1,L2,L3)")), // kVA/100, U32
		PF_SUM_IEC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Power factor IEC Sum(L1,L2,L3)")), // promille, S16, -1000..+1000
		PF_SUM_IEEE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Power factor IEEE Sum(L1,L2,L3)")), // promille, S16, -1000..+1000

		// @see ElectricityMeter.ChannelId.ACTIVE_POWER_L1
		// P_L1(Doc.of(OpenemsType.INTEGER)
		//    .unit(Unit.WATT) //
		//    .persistencePriority(PersistencePriority.HIGH)
		//     .text("Power L1")), // kW/100, S32
		// @see ElectricityMeter.ChannelId.ACTIVE_POWER_L2
		// P_L2(Doc.of(OpenemsType.INTEGER) //
		//    .unit(Unit.WATT) //
		//    .persistencePriority(PersistencePriority.HIGH) //
		//    .text("Power L2")), // kW/100, S32
		// @see ElectricityMeter.ChannelId.ACTIVE_POWER_L3
		// P_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
		//    .persistencePriority(PersistencePriority.HIGH) //
		//    .text("Power L3")), // kW/100, S32

		// @see ElectricityMeter.ChannelId.REACTIVE_POWER_L1
		// Q_L1(Doc.of(OpenemsType.INTEGER) //
		//    .unit(Unit.VOLT_AMPERE_REACTIVE) //
		//    .persistencePriority(PersistencePriority.HIGH) //
		//    .text("Power reactive L1")), // kvar/100, S32
		// @see ElectricityMeter.ChannelId.REACTIVE_POWER_L2
		// Q_L2(Doc.of(OpenemsType.INTEGER) //
		//    .unit(Unit.VOLT_AMPERE_REACTIVE) //
		//    .persistencePriority(PersistencePriority.HIGH) //
		//    .text("Power reactive L2")), // kvar/100, S32
		// @see ElectricityMeter.ChannelId.REACTIVE_POWER_L3
		// Q_L3(Doc.of(OpenemsType.INTEGER) //
		//    .unit(Unit.VOLT_AMPERE_REACTIVE) //
		//    .persistencePriority(PersistencePriority.HIGH) //
		//    .text("Power reactive L3")), // kvar/100, S32

		S_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Power apparent L1")), // kVA/100, U32
		S_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Power apparent L2")), // kVA/100, U32
		S_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Power apparent L3")), // kVA/100, U32

		PF_L1_IEC(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Power factor IEC L1")), // promille, S16, -1000..+1000
		PF_L2_IEC(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Power factor IEC L2")), // promille, S16, -1000..+1000
		PF_L3_IEC(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Power factor IEC L3")), // promille, S16, -1000..+1000

		PF_L1_IEEE(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Power factor IEEE L1")), // promille, S16, -1000..+1000
		PF_L2_IEEE(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Power factor IEEE L2")), // promille, S16, -1000..+1000
		PF_L3_IEEE(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Power factor IEEE L3")), // promille, S16, -1000..+1000
		
		// 7. Energies (kWh, ΣT) – Start 0xB060
		// @see ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY
		// EA_PLUS_SUM(Doc.of(OpenemsType.LONG) //
		//    .unit(Unit.WATT_HOURS) //
		//    .persistencePriority(PersistencePriority.HIGH) //
		//    .text("Imported Energy")), // Ea+ (ΣT) kWh, U32
		ER_PLUS_SUM(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Imported reactive Energy")), // Ea+ (ΣT) kWh, U32
		// @see ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY
		// EA_MINUS_SUM(Doc.of(OpenemsType.LONG) //
		//    .unit(Unit.WATT_HOURS) //
		//    .persistencePriority(PersistencePriority.HIGH) //
		//    .text("Exported Energy")), // Ea- (ΣT) kWh, U32
		ER_MINUS_SUM(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Exported reactive Energy")), // Ea- (ΣT) kWh, U32
		EA_PLUS_DETAILED_SUM(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Imported Energy")), // Ea+ (ΣT) kWh, U32
		EA_MINUS_DETAILED_SUM(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Exported Energy")), // Ea- (ΣT) kWh, U32

		// 12. Energies per Phase (kWh)
		// @see ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1
		// EA_PLUS_L1(Doc.of(OpenemsType.LONG) //
		//    .unit(Unit.WATT_HOURS) //
		//    .persistencePriority(PersistencePriority.HIGH) //
		//    .text("Imported Energy L1")),
		// @see ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2
		// EA_PLUS_L2(Doc.of(OpenemsType.LONG) //
		//    .unit(Unit.WATT_HOURS) //
		//    .persistencePriority(PersistencePriority.HIGH) //
		//    .text("Imported Energy L2")),
		// @see ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3
		// EA_PLUS_L3(Doc.of(OpenemsType.LONG) //
		//    .unit(Unit.WATT_HOURS) //
		//    .persistencePriority(PersistencePriority.HIGH) //
		//    .text("Imported Energy L3")),
		// @see ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1
		// EA_MINUS_L1(Doc.of(OpenemsType.LONG) //
		//    .unit(Unit.WATT_HOURS) //
		//    .persistencePriority(PersistencePriority.HIGH) //
		//    .text("Exported Energy L1")),
		// @see ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2
		// EA_MINUS_L2(Doc.of(OpenemsType.LONG) //
		//    .unit(Unit.WATT_HOURS) //
		//    .persistencePriority(PersistencePriority.HIGH) //
		//    .text("Exported Energy L2")),
		// @see ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3
		// EA_MINUS_L3(Doc.of(OpenemsType.LONG) //
		//    .unit(Unit.WATT_HOURS) //
		//    .persistencePriority(PersistencePriority.HIGH) //
		//    .text("Exported Energy L3")),

		ER_PLUS_L1(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Imported reactive Energy L1")),
		ER_PLUS_L2(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Imported reactive Energy L2")),
		ER_PLUS_L3(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Imported reactive Energy L3")),
		ER_MINUS_L1(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Exported reactive Energy L1")),
		ER_MINUS_L2(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Exported reactive Energy L2")),
		ER_MINUS_L3(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Exported reactive Energy L3")),

		;
		
		private final Doc doc;

		ChannelId(final Doc doc) {
			this.doc = doc;
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

	}
}