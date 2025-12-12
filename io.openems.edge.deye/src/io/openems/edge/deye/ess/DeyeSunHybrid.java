package io.openems.edge.deye.ess;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.deye.enums.BatteryChargeMode;
import io.openems.edge.deye.enums.BatteryOperateMode;
import io.openems.edge.deye.enums.EmsPowerMode;
import io.openems.edge.deye.enums.EnableDisable;
import io.openems.edge.deye.enums.EnergyManagementModel;
import io.openems.edge.deye.enums.GridStandard;
import io.openems.edge.deye.enums.InverterRunState;
import io.openems.edge.deye.enums.LimitControlFunction;
import io.openems.edge.deye.enums.RemoteLockState;
import io.openems.edge.deye.enums.WorkState;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.timedata.api.TimedataProvider;

public interface DeyeSunHybrid
		extends ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler, ModbusSlave, TimedataProvider {

	/**
	 * Gets the Modbus Unit-ID.
	 *
	 * @return the Unit-ID
	 */
	public Integer getUnitId();

	/**
	 * Gets the Modbus-Bridge Component-ID, i.e. "modbus0".
	 *
	 * @return the Component-ID
	 */
	public String getModbusBridgeId();

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		
		// Internal Channels
		MAX_AC_EXPORT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		MAX_AC_IMPORT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //		
		
	
		EMS_POWER_MODE(Doc.of(EmsPowerMode.values()) //
				.accessMode(AccessMode.READ_WRITE)), //			
		
		TARGET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)
				.persistencePriority(PersistencePriority.HIGH)), //

		TARGET_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)
				.persistencePriority(PersistencePriority.HIGH)), //		
		
		
		// EnumReadChannels
		SERIAL_NUMBER(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.HIGH) //
				.accessMode(AccessMode.READ_ONLY)),
		SURPLUS_FEED_IN_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //

		ACTIVE_POWER_REGULATION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE)), //			
	
		
		REACTIVE_POWER_REGULATION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE)), //			

		APPARENT_POWER_REGULATION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE)), //			
		
		
	    RATED_POWER(Doc.of(OpenemsType.LONG)
	            .unit(Unit.WATT)
	            .accessMode(AccessMode.READ_ONLY)),
		

		
		REMOTE_LOCK_STATE(Doc.of(RemoteLockState.values()) //
				.accessMode(AccessMode.READ_WRITE)), //			
		
		// Gen Port Use Channels
		// AC 1/28/2024
		SET_GRID_LOAD_OFF_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT) //
				.accessMode(AccessMode.WRITE_ONLY)), // ), //


		
		ENABLE_SWITCH_STATE(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_WRITE)), //	
		
		SWITCH_STATE(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_ONLY)), //	
		
		
	

		FACTORY_RESET_STATE(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_WRITE)), //	
		

		SELF_CHECKING_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_ONLY)), //			

		ISLAND_PROTECTION_ENABLE(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_ONLY)), //			
		
		SET_MPPT_NUMBER(Doc.of(EnableDisable.values()) // Enables or disables MPPT tracker
				.accessMode(AccessMode.WRITE_ONLY)), //
		MPPT_NUMBER(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_ONLY)), //			

		// GFDI (Ground Fault Detection Interrupter)
	    // Function: Monitors the DC side for ground faults—i.e. unintended current paths to earth. 
	    // If the inverter detects an imbalance between the positive and negative DC conductors, 
	    // it instantly disconnects both DC lines to protect the installation and anyone nearby.		
		GFDI_STATE(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_WRITE)), //			

		
		// RISO (Residual Insulation Monitoring)
	    // Function: Continuously measures the insulation resistance (Rₒ) between the PV array and earth. 
		// If the resistance falls below the threshold stored in Register 65, 
	    // the inverter triggers an insulation-fault alarm, allowing early detection of moisture ingress, cable damage, or insulation breakdown.		
		RISO_STATE(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_WRITE)), //			

		SET_GRID_STANDARD(Doc.of(GridStandard.values()) // what is this?
				.accessMode(AccessMode.WRITE_ONLY)), //	
		GRID_STANDARD(Doc.of(GridStandard.values()) // what is this?
				.accessMode(AccessMode.READ_ONLY)), //			

		
	    SELL_MODE_TIME_POINT_1(Doc.of(OpenemsType.INTEGER)
		        .accessMode(AccessMode.READ_WRITE)), // register 148
	    SELL_MODE_TIME_POINT_2(Doc.of(OpenemsType.INTEGER)
		        .accessMode(AccessMode.READ_WRITE)),
	    SELL_MODE_TIME_POINT_3(Doc.of(OpenemsType.INTEGER)
		        .accessMode(AccessMode.READ_WRITE)), // register 148
	    SELL_MODE_TIME_POINT_4(Doc.of(OpenemsType.INTEGER)
		        .accessMode(AccessMode.READ_WRITE)),
	    SELL_MODE_TIME_POINT_5(Doc.of(OpenemsType.INTEGER)
		        .accessMode(AccessMode.READ_WRITE)), // register 148
	    SELL_MODE_TIME_POINT_6(Doc.of(OpenemsType.INTEGER)
		        .accessMode(AccessMode.READ_WRITE)),


	    // Sell Mode Power Limits (W)
	    SELL_MODE_TIME_POINT_1_POWER(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.WATT)
	            .accessMode(AccessMode.READ_WRITE)),
	    SELL_MODE_TIME_POINT_2_POWER(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.WATT)
	            .accessMode(AccessMode.READ_WRITE)),
	    SELL_MODE_TIME_POINT_3_POWER(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.WATT)
	            .accessMode(AccessMode.READ_WRITE)),
	    SELL_MODE_TIME_POINT_4_POWER(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.WATT)
	            .accessMode(AccessMode.READ_WRITE)),
	    SELL_MODE_TIME_POINT_5_POWER(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.WATT)
	            .accessMode(AccessMode.READ_WRITE)),
	    SELL_MODE_TIME_POINT_6_POWER(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.WATT)
	            .accessMode(AccessMode.READ_WRITE)),
 	    

	    // Sell Mode Voltage Limits (V)
	    SELL_MODE_TIME_POINT_1_VOLTAGE(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.MILLIVOLT)
	            .accessMode(AccessMode.READ_WRITE)),
	    SELL_MODE_TIME_POINT_2_VOLTAGE(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.MILLIVOLT)
	            .accessMode(AccessMode.READ_WRITE)),
	    SELL_MODE_TIME_POINT_3_VOLTAGE(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.MILLIVOLT)
	            .accessMode(AccessMode.READ_WRITE)),
	    SELL_MODE_TIME_POINT_4_VOLTAGE(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.MILLIVOLT)
	            .accessMode(AccessMode.READ_WRITE)),
	    SELL_MODE_TIME_POINT_5_VOLTAGE(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.MILLIVOLT)
	            .accessMode(AccessMode.READ_WRITE)),
	    SELL_MODE_TIME_POINT_6_VOLTAGE(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.MILLIVOLT)
	            .accessMode(AccessMode.READ_WRITE)),

	    // Sell Mode Capacity Limits (Ah)
	    SELL_MODE_TIME_POINT_1_CAPACITY(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.AMPERE_HOURS)
	            .accessMode(AccessMode.READ_WRITE)), 
	    SELL_MODE_TIME_POINT_2_CAPACITY(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.AMPERE_HOURS)
	            .accessMode(AccessMode.READ_WRITE)),    
	    SELL_MODE_TIME_POINT_3_CAPACITY(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.AMPERE_HOURS)
	            .accessMode(AccessMode.READ_WRITE)),    	    
	    SELL_MODE_TIME_POINT_4_CAPACITY(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.AMPERE_HOURS)
	            .accessMode(AccessMode.READ_WRITE)),    
	    SELL_MODE_TIME_POINT_5_CAPACITY(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.AMPERE_HOURS)
	            .accessMode(AccessMode.READ_WRITE)),    
	    SELL_MODE_TIME_POINT_6_CAPACITY(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.AMPERE_HOURS)
	            .accessMode(AccessMode.READ_WRITE)),    	    

	    // Bit 0 -> Charge from grid enabled, Bit 1 -> Charge from generator 
	    CHARGE_MODE_TIME_POINT_1(Doc.of(OpenemsType.INTEGER)
	            .accessMode(AccessMode.READ_WRITE)),
	    CHARGE_MODE_TIME_POINT_2(Doc.of(OpenemsType.INTEGER)
	            .accessMode(AccessMode.READ_WRITE)),
	    CHARGE_MODE_TIME_POINT_3(Doc.of(OpenemsType.INTEGER)
	            .accessMode(AccessMode.READ_WRITE)),
	    CHARGE_MODE_TIME_POINT_4(Doc.of(OpenemsType.INTEGER)
	            .accessMode(AccessMode.READ_WRITE)),
	    CHARGE_MODE_TIME_POINT_5(Doc.of(OpenemsType.INTEGER)
	            .accessMode(AccessMode.READ_WRITE)),
	    CHARGE_MODE_TIME_POINT_6(Doc.of(OpenemsType.INTEGER)
	            .accessMode(AccessMode.READ_WRITE)),	
	    
	    // BMS Status Registers (read-only)
	    BMS_CHARGING_VOLTAGE(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.VOLT)
	            .accessMode(AccessMode.READ_WRITE)),
	    BMS_DISCHARGING_VOLTAGE(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.VOLT)
	            .accessMode(AccessMode.READ_WRITE)),	    
	    BMS_CHARGE_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.AMPERE)
	            .accessMode(AccessMode.READ_WRITE)),
	    BMS_DISCHARGE_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.AMPERE)
	            .accessMode(AccessMode.READ_WRITE)),
	    BMS_BATTERY_SOC(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.PERCENT)
	            .accessMode(AccessMode.READ_WRITE)),
	    BMS_BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.MILLIVOLT)
	            .accessMode(AccessMode.READ_WRITE)),
	    BMS_BATTERY_CURRENT(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.AMPERE)
	            .accessMode(AccessMode.READ_WRITE)),
	    BMS_BATTERY_CHARGE_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.AMPERE)
	            .accessMode(AccessMode.READ_WRITE)),
	    BMS_BATTERY_DISCHARGE_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.AMPERE)
	            .accessMode(AccessMode.READ_WRITE)),
	    BMS_BATTERY_ALARM(Doc.of(OpenemsType.INTEGER)
	            .accessMode(AccessMode.READ_WRITE)),
	    BMS_BATTERY_FAULT_LOCATION(Doc.of(OpenemsType.INTEGER)
	            .accessMode(AccessMode.READ_WRITE)),
	    BMS_BATTERY_SYMBOL_2(Doc.of(OpenemsType.INTEGER)
	            .accessMode(AccessMode.READ_WRITE)),
	    BMS_BATTERY_LITHIUM_TYPE(Doc.of(OpenemsType.INTEGER)
	            .accessMode(AccessMode.READ_WRITE)),
	    BMS_BATTERY_SOH(Doc.of(OpenemsType.INTEGER)
	            .accessMode(AccessMode.READ_WRITE)),		
		
		SET_BATTERY_CHARGE_MODE(Doc.of(BatteryChargeMode.values()) // lead or lithium optimized charge curve
				.accessMode(AccessMode.WRITE_ONLY)), //		
		BATTERY_CHARGE_MODE(Doc.of(BatteryChargeMode.values()) // 
				.accessMode(AccessMode.READ_ONLY)), //			
		
		SET_BATTERY_EQUALIZATION_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.WRITE_ONLY)), //	
		BATTERY_EQUALIZATION_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //				
				.accessMode(AccessMode.READ_ONLY)), //			

		SET_BATTERY_ABSORPTION_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.WRITE_ONLY)), //	
		BATTERY_ABSORPTION_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //			
		

		SET_BATTERY_FLOAT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.WRITE_ONLY)), //	
		BATTERY_FLOAT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_ONLY)), //			
		
		//SET_BATTERY_CAPACITY(Doc.of(OpenemsType.INTEGER) //
		//		.unit(Unit.AMPERE_HOURS) //
		//		.accessMode(AccessMode.WRITE_ONLY)), //	
		//BATTERY_CAPACITY(Doc.of(OpenemsType.INTEGER) //
		//		.unit(Unit.AMPERE_HOURS) //				
		//		.accessMode(AccessMode.READ_ONLY)), //			
		
		SET_BATTERY_EMPTY_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.WRITE_ONLY)), //	
		BATTERY_EMPTY_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_ONLY)), //
		
			
		SET_MAX_SOLAR_SELL_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //	
		MAX_SOLAR_SELL_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)), //		
		
		SET_BATTERY_ZERO_EXPORT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		BATTERY_ZERO_EXPORT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)), //		
		
		// Days between battery balancing cycles
		// Values between 0-90 days are vaild
		SET_BATTERY_EQUALIZATION_DAY_CYCLE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.WRITE_ONLY)), //	
		BATTERY_EQUALIZATION_DAY_CYCLE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY)), //			
		
		// time in balancing mode. Resolution 0.5h
		// Values 0-20 are valid (-> max. 10h) 
		SET_BATTERY_EQUALIZATION_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.WRITE_ONLY)), //		
		BATTERY_EQUALIZATION_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY)), //			
		
		
		
	
		
		// Battery voltages need to be adjusted based on temperature 
		// (for example, charging voltage must be higher in cold conditions). 
		// The TEMPCO register defines by how many millivolts per degree Celsius the voltage setpoints shift per cell.
		SET_BATTERY_TEMPERATURE_COMPENSATION_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.WRITE_ONLY)), //		
		BATTERY_TEMPERATURE_COMPENSATION_VALUE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY)), //			

	    // Battery operating parameters (read-only)
	    BATTERY_OPERATE_MODE(Doc.of(BatteryOperateMode.values())
	        .accessMode(AccessMode.READ_ONLY)), // register 111
	    
	    // Battery operating parameters (read-only)
	    SET_BATTERY_OPERATE_MODE(Doc.of(BatteryOperateMode.values())
	        .accessMode(AccessMode.WRITE_ONLY)), // register 111	    

	    LITHIUM_WAKE_UP_SIGN(Doc.of(OpenemsType.INTEGER)
	        .accessMode(AccessMode.READ_ONLY)), // register 112

	    
	    // BMS Metrics
	    BATTERY_TEMPERATURE(Doc.of(OpenemsType.INTEGER) // register 586
	            .unit(Unit.DEGREE_CELSIUS)
	            .accessMode(AccessMode.READ_ONLY)),
	    BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER) // register 587
	            .unit(Unit.VOLT)
	            .accessMode(AccessMode.READ_ONLY)),
	    BATTERY_SOC(Doc.of(OpenemsType.INTEGER) // register 588
	            .unit(Unit.PERCENT)
	            .accessMode(AccessMode.READ_ONLY)),
	    BATTERY_OUTPUT_POWER(Doc.of(OpenemsType.INTEGER) // register 590
	            .unit(Unit.WATT)
	            .accessMode(AccessMode.READ_ONLY)),
	    BATTERY_OUTPUT_CURRENT(Doc.of(OpenemsType.INTEGER) // register 591
	            .unit(Unit.AMPERE)
	            .accessMode(AccessMode.READ_ONLY)),
	    BATTERY_CORRECTED_AH(Doc.of(OpenemsType.INTEGER) // register 592
	            .unit(Unit.AMPERE_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),	   	    

	    // Generator / grid charging settings (read-only)
	    GENERATOR_MAX_OPERATING_TIME(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.SECONDS)
	        .accessMode(AccessMode.READ_ONLY)), // register 121

	    GENERATOR_COOLING_TIME(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.SECONDS)
	        .accessMode(AccessMode.READ_ONLY)), // register 122

	    GENERATOR_CHARGING_START_VOLTAGE(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.MILLIVOLT)
	        .accessMode(AccessMode.READ_ONLY)), // register 123

	    GENERATOR_CHARGING_START_CAPACITY(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.PERCENT)
	        .accessMode(AccessMode.READ_ONLY)), // register 124

	    GENERATOR_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.AMPERE)
	        .accessMode(AccessMode.READ_ONLY)), // register 125

	    GRID_CHARGING_START_VOLTAGE(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.MILLIVOLT)
	        .accessMode(AccessMode.READ_ONLY)), // register 126

	    GRID_CHARGING_START_CAPACITY(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.PERCENT)
	        .accessMode(AccessMode.READ_ONLY)), // register 127

	    GRID_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.AMPERE)
	        .accessMode(AccessMode.READ_ONLY)), // register 128
	    
	    SET_GRID_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER)
		        .unit(Unit.AMPERE)
		        .accessMode(AccessMode.WRITE_ONLY)), // register 128	    

	    GENERATOR_CHARGING_ENABLE(Doc.of(OpenemsType.BOOLEAN)
	        .accessMode(AccessMode.READ_ONLY)), // register 129
	    
	    SET_GENERATOR_CHARGING_ENABLE(Doc.of(OpenemsType.BOOLEAN)
		        .accessMode(AccessMode.WRITE_ONLY)), // register 129
	    
	    GRID_CHARGING_ENABLE(Doc.of(OpenemsType.BOOLEAN)
	        .accessMode(AccessMode.READ_ONLY)), // register 130
	    
	    SET_GRID_CHARGING_ENABLE(Doc.of(OpenemsType.BOOLEAN)
		        .accessMode(AccessMode.WRITE_ONLY)), // register 130	    

	    // Power management & sell‑mode settings (read-only)
	    AC_COUPLE_FREQUENCY_LIMIT(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.MILLIHERTZ)
	        .accessMode(AccessMode.READ_ONLY)), // register 131

	    FORCE_GENERATOR_AS_LOAD(Doc.of(OpenemsType.INTEGER)
	        .accessMode(AccessMode.READ_ONLY)), // register 132

	    GENERATOR_INPUT_AS_LOAD_ENABLE(Doc.of(OpenemsType.INTEGER)
	        .accessMode(AccessMode.READ_ONLY)), // register 133

	    SMARTLOAD_OFF_BATT_VOLTAGE(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.MILLIVOLT)
	        .accessMode(AccessMode.READ_ONLY)), // register 134

	    SMARTLOAD_OFF_BATT_CAPACITY(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.PERCENT)
	        .accessMode(AccessMode.READ_ONLY)), // register 135

	    SMARTLOAD_ON_BATT_VOLTAGE(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.MILLIVOLT)
	        .accessMode(AccessMode.READ_ONLY)), // register 136

	    SMARTLOAD_ON_BATT_CAPACITY(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.PERCENT)
	        .accessMode(AccessMode.READ_ONLY)), // register 137

	    OUTPUT_VOLTAGE_LEVEL(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.VOLT)
	        .accessMode(AccessMode.READ_ONLY)), // register 138

	    MIN_SOLAR_POWER_TO_START_GENERATOR(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.WATT)
	        .accessMode(AccessMode.READ_ONLY)), // register 139

	    GEN_GRID_SIGNAL_ON(Doc.of(OpenemsType.INTEGER)
	        .accessMode(AccessMode.READ_ONLY)), // register 140
	    
	    
	    ENERGY_MANAGEMENT_MODEL(Doc.of(EnergyManagementModel.values())
	        .accessMode(AccessMode.READ_WRITE)), // register 141
	    
   

	    // Daily Metrics
	    TODAY_ACTIVE_ENERGY(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),
	    TODAY_REACTIVE_ENERGY(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),
	    TODAY_GRID_CONNECTED(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.SECONDS)
	            .accessMode(AccessMode.READ_ONLY)),

	    // Battery Energy
	    TODAY_BATTERY_CHARGE(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),
	    TODAY_BATTERY_DISCHARGE(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),
	    TOTAL_BATTERY_CHARGE(Doc.of(OpenemsType.LONG)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),
	    TOTAL_BATTERY_DISCHARGE(Doc.of(OpenemsType.LONG)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),

	    // Grid Trading
	    TODAY_BUY_GRID(Doc.of(OpenemsType.INTEGER)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),
	    TODAY_SOLD_GRID(Doc.of(OpenemsType.FLOAT)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),
	    TOTAL_BUY_GRID(Doc.of(OpenemsType.LONG)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),
	    TOTAL_SELL_GRID(Doc.of(OpenemsType.LONG)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),
	    TOTAL_ACTIVE_ENERGY_GENERATION(Doc.of(OpenemsType.LONG)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),	   
	    TOTAL_REACTIVE_ENERGY_GENERATION(Doc.of(OpenemsType.LONG)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),	 	    

	    // Consumption
	    TODAY_TO_LOAD(Doc.of(OpenemsType.FLOAT)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),
	    TOTAL_TO_LOAD(Doc.of(OpenemsType.LONG)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),

	    // PV Generation
	    TODAY_FROM_PV(Doc.of(OpenemsType.FLOAT)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),
	    TODAY_FROM_PV_S1(Doc.of(OpenemsType.FLOAT)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),
	    TODAY_FROM_PV_S2(Doc.of(OpenemsType.FLOAT)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),
	    TODAY_FROM_PV_S3(Doc.of(OpenemsType.FLOAT)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),
	    TODAY_FROM_PV_S4(Doc.of(OpenemsType.FLOAT)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),
	    TOTAL_FROM_PV(Doc.of(OpenemsType.LONG)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),
	    
	    

	    // Generator
	    TODAY_FROM_GENERATOR(Doc.of(OpenemsType.FLOAT)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),
	    TOTAL_FROM_GENERATOR(Doc.of(OpenemsType.LONG)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),
	    TODAY_GENERATOR_WORKTIME(Doc.of(OpenemsType.FLOAT)
	            .unit(Unit.HOUR)
	            .accessMode(AccessMode.READ_ONLY)),

	    // Temperatures
	    DC_TRANSFORMER_TEMP(Doc.of(OpenemsType.FLOAT)
	            .unit(Unit.DEGREE_CELSIUS)
	            .accessMode(AccessMode.READ_ONLY)),
	    HEATSINK_TEMP(Doc.of(OpenemsType.FLOAT)
	            .unit(Unit.DEGREE_CELSIUS)
	            .accessMode(AccessMode.READ_ONLY)),

	    // Annual Consumption
	    LOAD_ANNUAL_CONSUMPTION(Doc.of(OpenemsType.LONG)
	            .unit(Unit.WATT_HOURS)
	            .accessMode(AccessMode.READ_ONLY)),	    
	    

	    
	    // AC Relay & Dry Contacts (bitfield)
	    AC_RELAY_INVERTER(Doc.of(OpenemsType.BOOLEAN)
	            .accessMode(AccessMode.READ_ONLY)),
	    AC_RELAY_GRID(Doc.of(OpenemsType.BOOLEAN)
	            .accessMode(AccessMode.READ_ONLY)),
	    AC_RELAY_GENERATOR(Doc.of(OpenemsType.BOOLEAN)
	            .accessMode(AccessMode.READ_ONLY)),
	    AC_RELAY_GRID_POWER(Doc.of(OpenemsType.BOOLEAN)
	            .accessMode(AccessMode.READ_ONLY)),
	    DRY_CONTACT_1(Doc.of(OpenemsType.BOOLEAN)
	            .accessMode(AccessMode.READ_ONLY)),
	    DRY_CONTACT_2(Doc.of(OpenemsType.BOOLEAN)
	            .accessMode(AccessMode.READ_ONLY)),	    
	    
	    
	    
	    // Mode 1 – ZeroExportToLoad
	    // In this mode, the inverter ensures zero export by dynamically 
	    // matching its AC output to the local load. Any PV energy in excess of the momentary household consumption 
	    // is simply curtailed rather than sent to the grid. Battery charging remains active—surplus PV (after covering the load) 
	    // goes into the battery—but no power is ever exported to the public grid.

	    // Mode 2 – ZeroExportToCT
	    // Here, an external current transformer (CT) measures the net grid flow. The inverter continuously adjusts 
	    // its output so that the CT reading stays at zero. As a result, no power flows into or out of the grid, 
	    // regardless of load or PV production. All PV surplus feeds the battery (if enabled), and all PV deficits 
	    // are topped up from the battery or grid (depending on other settings), but grid flow remains at zero by CT control.
	    

	    LIMIT_CONTROL_FUNCTION(Doc.of(LimitControlFunction.values())
	        .accessMode(AccessMode.READ_WRITE)), // register 142
	    
	    SET_LIMIT_CONTROL_FUNCTION(Doc.of(LimitControlFunction.values())
		        .accessMode(AccessMode.WRITE_ONLY)), // register 142	    

	    LIMIT_MAX_GRID_OUTPUT_POWER(Doc.of(OpenemsType.INTEGER)
	        .unit(Unit.WATT)
	        .accessMode(AccessMode.READ_ONLY)), // register 143

	    EXTERNAL_CURRENT_SENSOR_CLAMP_PHASE(Doc.of(OpenemsType.INTEGER)
	        .accessMode(AccessMode.READ_ONLY)), // register 144

	    SOLAR_SELL_MODE(Doc.of(EnableDisable.values())
	        .accessMode(AccessMode.READ_WRITE)), // register 145
  

	    TIME_OF_USE_SELLING_ENABLED(Doc.of(OpenemsType.BOOLEAN)
	        .accessMode(AccessMode.READ_ONLY)), // register 146
	    
	    TIME_OF_USE_MONDAY(Doc.of(OpenemsType.BOOLEAN)
	            .accessMode(AccessMode.READ_ONLY)), // register 146 Bit 1

	    TIME_OF_USE_TUESDAY(Doc.of(OpenemsType.BOOLEAN)
	            .accessMode(AccessMode.READ_ONLY)), // register 146 Bit 2

	    TIME_OF_USE_WEDNESDAY(Doc.of(OpenemsType.BOOLEAN)
	            .accessMode(AccessMode.READ_ONLY)), // register 146 Bit 3

	    TIME_OF_USE_THURSDAY(Doc.of(OpenemsType.BOOLEAN)
	            .accessMode(AccessMode.READ_ONLY)), // register 146 Bit 4

	    TIME_OF_USE_FRIDAY(Doc.of(OpenemsType.BOOLEAN)
	            .accessMode(AccessMode.READ_ONLY)), // register 146 Bit 5

	    TIME_OF_USE_SATURDAY(Doc.of(OpenemsType.BOOLEAN)
	            .accessMode(AccessMode.READ_ONLY)), // register 146 Bit 6

	    TIME_OF_USE_SUNDAY(Doc.of(OpenemsType.BOOLEAN)
	            .accessMode(AccessMode.READ_ONLY)), // register 146 Bit 7
  
	    
	    SET_TIME_OF_USE_SELLING_ENABLED(Doc.of(OpenemsType.BOOLEAN)
	            .accessMode(AccessMode.WRITE_ONLY)), // register 146 Bit 0 (main switch for Time-of-Use)

	    SET_TIME_OF_USE_MONDAY(Doc.of(OpenemsType.BOOLEAN)
	            .accessMode(AccessMode.WRITE_ONLY)), // register 146 Bit 1

	    SET_TIME_OF_USE_TUESDAY(Doc.of(OpenemsType.BOOLEAN)
	            .accessMode(AccessMode.WRITE_ONLY)), // register 146 Bit 2

	    SET_TIME_OF_USE_WEDNESDAY(Doc.of(OpenemsType.BOOLEAN)
	            .accessMode(AccessMode.WRITE_ONLY)), // register 146 Bit 3

	    SET_TIME_OF_USE_THURSDAY(Doc.of(OpenemsType.BOOLEAN)
	            .accessMode(AccessMode.WRITE_ONLY)), // register 146 Bit 4

	    SET_TIME_OF_USE_FRIDAY(Doc.of(OpenemsType.BOOLEAN)
	            .accessMode(AccessMode.WRITE_ONLY)), // register 146 Bit 5

	    SET_TIME_OF_USE_SATURDAY(Doc.of(OpenemsType.BOOLEAN)
	            .accessMode(AccessMode.WRITE_ONLY)), // register 146 Bit 6

	    SET_TIME_OF_USE_SUNDAY(Doc.of(OpenemsType.BOOLEAN)
	            .accessMode(AccessMode.WRITE_ONLY)), // register 146 Bit 7


	    GRID_PHASE_SEQUENCE(Doc.of(OpenemsType.INTEGER)
	        .accessMode(AccessMode.READ_ONLY)), // register 147


	    // Error / Warnings

	    FLASH_CHIP_ERROR(Doc.of(Level.WARNING) //
				.text("Flash chip Error")), //
	    TIME_ERROR(Doc.of(Level.WARNING) //
				.text("Time Error")), //
	    EEPROM_ERROR(Doc.of(Level.WARNING) //
				.text("EEPROM Error")), // 
	    ARC_COMMUNICATION_STATE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Arc pull communication sign")), //
	    CAN_COMMUNICATION_ERROR(Doc.of(Level.WARNING) //
				.text("Parallel CAN communication")), // 	 	    
	    LITHIUM_BATTERY_RS485(Doc.of(Level.WARNING) //
				.text("Lithium battery interface RS485")), // 	
	    LITHIUM_BATTERY_CAN(Doc.of(Level.INFO) //
				.text("Lithium battery interface CAN")), // 	
	    KEY1234_ERROR(Doc.of(Level.WARNING) //
				.text("Key 1234 Error")), //
	    LCD_INTERRUPT_STATE(Doc.of(OpenemsType.BOOLEAN) //
				.text("LCD interrupt status")), //	 
	    FAN_FAILURE(Doc.of(Level.WARNING) //
				.text("Fan Error")), //	 
	    GRID_PHASE_ERROR(Doc.of(Level.WARNING) //
				.text("Grid phase Error")), //	 

	    ERROR_MESSAGE_1(new IntegerDoc()
	            .text("Fault information word 1")
	            .onChannelUpdate((self, newValue) -> {
	                updateErrorState(self);
	            })),

	    ERROR_MESSAGE_2(new IntegerDoc()
	            .text("Fault information word 2")
	            .onChannelUpdate((self, newValue) -> {
	                updateErrorState(self);
	            })),

	    ERROR_MESSAGE_3(new IntegerDoc()
	            .text("Fault information word 3")
	            .onChannelUpdate((self, newValue) -> {
	                updateErrorState(self);
	            })),

	    ERROR_MESSAGE_4(new IntegerDoc()
	            .text("Fault information word 4")
	            .onChannelUpdate((self, newValue) -> {
	                updateErrorState(self);
	            })),


	    // F0(Doc.of(Level.FAULT).text("No Error")),  // for testing
	    F7(Doc.of(Level.FAULT).text("DC soft start error")),
	    F10(Doc.of(Level.FAULT).text("AUX power board error")),
	    F13(Doc.of(Level.FAULT).text("Working mode change")),
	    F18(Doc.of(Level.FAULT).text("Hardware AC overcurrent")),
	    F20(Doc.of(Level.FAULT).text("Hardware DC overcurrent")),
	    F22(Doc.of(Level.FAULT).text("Tz_EmergSStop_Fault. Emergency stop fault")),
	    F23(Doc.of(Level.FAULT).text("Instantaneous leakage current fault")),
	    F24(Doc.of(Level.FAULT).text("Phalanx insulation resistance fault")),
	    F26(Doc.of(Level.FAULT).text("Parallel CAN-Bus communication failure")),
	    F29(Doc.of(Level.FAULT).text("No AC grid")),
	    F35(Doc.of(Level.FAULT).text("Parallel system shutdown failure")),
	    F41(Doc.of(Level.FAULT).text("Parallel system stop")),
	    F42(Doc.of(Level.FAULT).text("AC line low voltage fault")),
	    F46(Doc.of(Level.FAULT).text("Backup battery failure")),
	    F47(Doc.of(Level.FAULT).text("AC overfrequency")),
	    F48(Doc.of(Level.FAULT).text("AC underfrequency")),
	    F49(Doc.of(Level.FAULT).text("Backup battery failure")),
	    F56(Doc.of(Level.FAULT).text("Bus voltage too low")),
	    F58(Doc.of(Level.FAULT).text("BMS communication failure")),
	    F63(Doc.of(Level.FAULT).text("Arc fault")),
	    F64(Doc.of(Level.FAULT).text("Heat sink temperature too high")),

	    
    	    
		
		// EnumWriteChannels
		WORK_STATE(Doc.of(WorkState.values()) //
				.accessMode(AccessMode.READ_WRITE)), //

		// IntegerWriteChannel
		SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		
		POWER_TO_GRID_TARGET(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE) //
				.accessMode(AccessMode.READ_WRITE)), //	
		


		SET_GEN_PEAK_SHAVING_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_GRID_PEAK_SHAVING_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		CT_RATIO(Doc.of(OpenemsType.INTEGER)), //
		
		INVERTER_RUN_STATE(Doc.of(InverterRunState.values()).accessMode(AccessMode.READ_ONLY)),	
		


		// LongReadChannel
		ORIGINAL_ACTIVE_CHARGE_ENERGY(Doc.of(OpenemsType.LONG)), //
		ORIGINAL_ACTIVE_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG)), //
		
		// Inverter Output includes external generator?
		GRID_OUTPUT_ACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)), 
		GRID_OUTPUT_ACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)), 
		GRID_OUTPUT_ACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)), 	
		GRID_OUTPUT_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)), 	
		GRID_OUTPUT_VOLTAGE_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_ONLY)), 	
		GRID_OUTPUT_VOLTAGE_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_ONLY)), 	
		GRID_OUTPUT_VOLTAGE_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_ONLY)), 	
		
		GRID_OUTPUT_CURRENT_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE) //
				.accessMode(AccessMode.READ_ONLY)), 	
		GRID_OUTPUT_CURRENT_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE) //
				.accessMode(AccessMode.READ_ONLY)), 			
		GRID_OUTPUT_CURRENT_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE) //
				.accessMode(AccessMode.READ_ONLY)), 	
		
		// Inverter Output without external generator??
		POWER_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)), 		
		POWER_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)),
		POWER_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)),		
		
		
		SET_REMOTE_MODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		
		SET_CONTROL_MODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //			
		
		SET_BATTERY_CONTROL_MODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //		
		
		SET_3P_CONTROL_MODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //	
		
		SET_BATTERY_POWER_PERCENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT)
				.accessMode(AccessMode.READ_WRITE)), //	
		
		SET_BATTERY_POWER_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT)
				.accessMode(AccessMode.READ_WRITE)), //	
		
		SET_AC_SETPOINT_3P_PERCENT(Doc.of(OpenemsType.INTEGER) // dezi-percent
				.accessMode(AccessMode.READ_WRITE)), //	
		SET_REMOTE_WATCHDOG_TIME(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //			

		// ToDo: Set right units and scaling
		SET_BATTERY_CONSTANT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //		
				
		SET_BATTERY_CONSTANT_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //		
						
		FUCKOFF_1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //		
		
		FUCKOFF_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //				
		

		
/*		
		// IntegerReadChannels
		ORIGINAL_ALLOWED_CHARGE_POWER(new IntegerDoc() //
				.onChannelUpdate((self, newValue) -> {
					// on each Update to the channel -> set the ALLOWED_CHARGE_POWER value with a
					// delta of max 500
					IntegerReadChannel currentValueChannel = self
							.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER);
					var originalValue = newValue.asOptional();
					var currentValue = currentValueChannel.value().asOptional();
					final int value;
					if (!originalValue.isPresent() && !currentValue.isPresent()) {
						value = 0;
					} else if (originalValue.isPresent() && !currentValue.isPresent()) {
						value = originalValue.get();
					} else if (!originalValue.isPresent() && currentValue.isPresent()) {
						value = currentValue.get();
					} else {
						value = Math.max(originalValue.get(), currentValue.get() - 500);
					}
					currentValueChannel.setNextValue(value);
				})), //

		ORIGINAL_ALLOWED_DISCHARGE_POWER(new IntegerDoc() //
				.onChannelUpdate((self, newValue) -> {
					// on each Update to the channel -> set the ALLOWED_DISCHARGE_POWER value with a
					// delta of max 500
					IntegerReadChannel currentValueChannel = self
							.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER);
					var originalValue = newValue.asOptional();
					var currentValue = currentValueChannel.value().asOptional();
					final int value;
					if (!originalValue.isPresent() && !currentValue.isPresent()) {
						value = 0;
					} else if (originalValue.isPresent() && !currentValue.isPresent()) {
						value = originalValue.get();
					} else if (!originalValue.isPresent() && currentValue.isPresent()) {
						value = currentValue.get();
					} else {
						value = Math.min(originalValue.get(), currentValue.get() + 500);
					}
					currentValueChannel.setNextValue(value);
				})), //

*/	
		APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)),  
		;//


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
	 * Gets the Channel for {@link ChannelId#GRID_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getLimitMaxPowerOutputChannel() {
		return this.channel(ChannelId.LIMIT_MAX_GRID_OUTPUT_POWER);
	}

	/**
	 * Gets the DC Discharge Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#GRID_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getLimitMaxPowerOutput() {
		return this.getLimitMaxPowerOutputChannel().value();
	}	
	
	
	//
	public default Channel<LimitControlFunction> getLimitControlFunctionChannel() {
		return this.channel(ChannelId.LIMIT_CONTROL_FUNCTION);
	}

	public default WriteChannel<LimitControlFunction> getSetLimitControlFunctionChannel() {
		return this.channel(ChannelId.LIMIT_CONTROL_FUNCTION);
	}	


	public default LimitControlFunction getLimitControlFunction() {
		return this.getLimitControlFunctionChannel().value().asEnum();
	}


	public default void setLimitControlFunction(LimitControlFunction value) throws OpenemsNamedException {
		this.getSetLimitControlFunctionChannel().setNextWriteValue(value);
		//setWriteValueIfNotRead
	}	
	
	//
	public default Channel<BatteryOperateMode> getBatteryOperateModeChannel() {
		return this.channel(ChannelId.BATTERY_OPERATE_MODE);
	}

	public default EnumWriteChannel getSetBatteryOperateModeChannel() {
		return this.channel(ChannelId.SET_BATTERY_OPERATE_MODE);
	}	


	public default Value<BatteryOperateMode> getBatteryOperateMode() {
		return this.getBatteryOperateModeChannel().value().asEnum();
	}


	public default void setBatteryOperateMode(BatteryOperateMode value) throws OpenemsNamedException {
		this.getSetBatteryOperateModeChannel().setNextWriteValue(value);
	}		
	
	
	//
	public default Channel<EnableDisable> getSolarSellModeChannel() {
		return this.channel(ChannelId.SOLAR_SELL_MODE);
	}

	public default WriteChannel<EnableDisable> setSolarSellModeChannel() {
		return this.channel(ChannelId.SOLAR_SELL_MODE);
	}	


	public default EnableDisable getSolarSellMode() {
		return this.getSolarSellModeChannel().value().asEnum();
	}


	public default void setSolarSellMode(EnableDisable value) throws OpenemsNamedException {
		this.setSolarSellModeChannel().setNextWriteValue(value);
	}		
	
	//
	public default Channel<EnergyManagementModel> getEnergyManagementModelChannel() {
		return this.channel(ChannelId.ENERGY_MANAGEMENT_MODEL);
	}

	public default WriteChannel<EnergyManagementModel> setEnergyManagementModelChannel() {
		return this.channel(ChannelId.ENERGY_MANAGEMENT_MODEL);
	}	


	public default EnergyManagementModel getEnergyManagementModel() {
		return this.getEnergyManagementModelChannel().value().asEnum();
	}


	public default void setEnergyManagementModel(EnergyManagementModel value) throws OpenemsNamedException {
		this.setEnergyManagementModelChannel().setNextWriteValue(value);
	}
	
	//
	public default Channel<EnableDisable> getMpptStatusChannel() {
		return this.channel(ChannelId.MPPT_NUMBER);
	}

	public default EnumWriteChannel getSetMpptChannel() {
		return this.channel(ChannelId.SET_MPPT_NUMBER);
	}	


	public default Value<Integer> getMpptStatus() {
		return this.getMpptStatusChannel().value().asEnum();
	}


	public default void setMpptMode(EnableDisable value) throws OpenemsNamedException {
		this.getSetMpptChannel().setNextWriteValue(value);
	}	

	//
	public default Channel<RemoteLockState> getRemoteLockChannel() {
		return this.channel(ChannelId.REMOTE_LOCK_STATE);
	}

	public default WriteChannel<RemoteLockState> getSetRemoteLockChannel() {
		return this.channel(ChannelId.REMOTE_LOCK_STATE);
	}	


	public default RemoteLockState getRemoteLockState() {
		return this.getRemoteLockChannel().value().asEnum();
	}


	public default void setRemoteLock(RemoteLockState value) throws OpenemsNamedException {
		this.getSetRemoteLockChannel().setNextWriteValue(value);
	}	
	
	
	
	//
	public default Channel<EnableDisable> getEnableSwitchChannel() {
		return this.channel(ChannelId.ENABLE_SWITCH_STATE);
	}

	public default WriteChannel<EnableDisable> getSetEnableSwitchChannel() {
		return this.channel(ChannelId.ENABLE_SWITCH_STATE);
	}	

	public default EnableDisable getEnableSwitchState() {
		return this.getEnableSwitchChannel().value().asEnum();
	}

	public default void setEnableSwitch(EnableDisable value) throws OpenemsNamedException {
		this.getSetEnableSwitchChannel().setNextWriteValue(value);
	}	
	
	
	
	//
	public default Channel<EnableDisable> getEnableGridChargeChannel() {
		return this.channel(ChannelId.GRID_CHARGING_ENABLE);
	}

	public default WriteChannel<EnableDisable> getSetEnableGridChargeChannel() {
		return this.channel(ChannelId.SET_GRID_CHARGING_ENABLE);
	}	


	public default EnableDisable getEnableGridChargeState() {
		return this.getEnableGridChargeChannel().value().asEnum();
	}

	public default void setEnableGridCharge(EnableDisable value) throws OpenemsNamedException {
		this.getSetEnableGridChargeChannel().setNextWriteValue(value);
	}	
	
	//
	public default IntegerReadChannel getGridChargeCurrentChannel() {
		return this.channel(ChannelId.GRID_CHARGE_CURRENT);
	}	
	
	public default IntegerWriteChannel getSetGridChargeCurrentChannel() {
		return this.channel(ChannelId.SET_GRID_CHARGE_CURRENT);
	}	
	
	public default Value<Integer> getGridChargeCurrent() {
		return this.getGridChargeCurrentChannel().value();
	}	
	
	public default void  setGridChargeCurrent(int value)  throws OpenemsNamedException  {
		this.getSetGridChargeCurrentChannel().setNextWriteValue(value);
	}	
	
	// Generator charging enabled
	// Getter for BooleanWriteChannels 
	public default BooleanWriteChannel getSetGeneratorCharingEnabledChannel() {
	    return this.channel(ChannelId.SET_GENERATOR_CHARGING_ENABLE);
	}
	
	// Getter für BooleanReadeChannels der Time-of-Use-Bits
	public default BooleanReadChannel getGeneratorCharingEnabledChannel() {
	    return this.channel(ChannelId.GENERATOR_CHARGING_ENABLE);
	}
	
	public default Boolean getGeneratorCharingEnabled()  {
		   return this.getGeneratorCharingEnabledChannel().value().get();
		}	

	public default void setGeneratorCharingEnabled(Boolean value) throws OpenemsNamedException {
	    this.getSetGeneratorCharingEnabledChannel().setNextWriteValue(value);
	}	

	// Grid charging enabled
	// Getter for BooleanWriteChannels 
	public default BooleanWriteChannel getSetGridCharingEnabledChannel() {
	    return this.channel(ChannelId.SET_GRID_CHARGING_ENABLE);
	}
	
	// Getter für BooleanReadeChannels der Time-of-Use-Bits
	public default BooleanReadChannel getGridCharingEnabledChannel() {
	    return this.channel(ChannelId.GRID_CHARGING_ENABLE);
	}
	
	public default Boolean getGridCharingEnabled()  {
		   return this.getGridCharingEnabledChannel().value().get();
		}	

	public default void setGridCharingEnabled(Boolean value) throws OpenemsNamedException {
	    this.getSetGridCharingEnabledChannel().setNextWriteValue(value);
	}		


	// Getter für BooleanWriteChannels der Time-of-Use-Bits
	public default BooleanWriteChannel getSetTimeOfUseSellingEnabledChannel() {
	    return this.channel(ChannelId.SET_TIME_OF_USE_SELLING_ENABLED);
	}

	public default BooleanWriteChannel getSetTimeOfUseMondayChannel() {
	    return this.channel(ChannelId.SET_TIME_OF_USE_MONDAY);
	}

	public default BooleanWriteChannel getSetTimeOfUseTuesdayChannel() {
	    return this.channel(ChannelId.SET_TIME_OF_USE_TUESDAY);
	}

	public default BooleanWriteChannel getSetTimeOfUseWednesdayChannel() {
	    return this.channel(ChannelId.SET_TIME_OF_USE_WEDNESDAY);
	}

	public default BooleanWriteChannel getSetTimeOfUseThursdayChannel() {
	    return this.channel(ChannelId.SET_TIME_OF_USE_THURSDAY);
	}

	public default BooleanWriteChannel getSetTimeOfUseFridayChannel() {
	    return this.channel(ChannelId.SET_TIME_OF_USE_FRIDAY);
	}

	public default BooleanWriteChannel getSetTimeOfUseSaturdayChannel() {
	    return this.channel(ChannelId.SET_TIME_OF_USE_SATURDAY);
	}

	public default BooleanWriteChannel getSetTimeOfUseSundayChannel() {
	    return this.channel(ChannelId.SET_TIME_OF_USE_SUNDAY);
	}
	
	// Getter für BooleanReadeChannels der Time-of-Use-Bits
	public default BooleanReadChannel getTimeOfUseSellingEnabledChannel() {
	    return this.channel(ChannelId.TIME_OF_USE_SELLING_ENABLED);
	}

	public default BooleanReadChannel getTimeOfUseMondayChannel() {
	    return this.channel(ChannelId.TIME_OF_USE_MONDAY);
	}

	public default BooleanReadChannel getTimeOfUseTuesdayChannel() {
	    return this.channel(ChannelId.TIME_OF_USE_TUESDAY);
	}

	public default BooleanReadChannel getTimeOfUseWednesdayChannel() {
	    return this.channel(ChannelId.TIME_OF_USE_WEDNESDAY);
	}

	public default BooleanReadChannel getTimeOfUseThursdayChannel() {
	    return this.channel(ChannelId.TIME_OF_USE_THURSDAY);
	}

	public default BooleanReadChannel getTimeOfUseFridayChannel() {
	    return this.channel(ChannelId.TIME_OF_USE_FRIDAY);
	}

	public default BooleanReadChannel getTimeOfUseSaturdayChannel() {
	    return this.channel(ChannelId.TIME_OF_USE_SATURDAY);
	}

	public default BooleanReadChannel getTimeOfUseSundayChannel() {
	    return this.channel(ChannelId.TIME_OF_USE_SUNDAY);
	}	




	// Getter für Time-of-Use-Bits
	public default Boolean getTimeOfUseSellingEnabled()  {
	   return this.getTimeOfUseSellingEnabledChannel().value().get();
	}

	public default Boolean getTimeOfUseMonday()  {
	   return this.getTimeOfUseMondayChannel().value().get();
	}

	public default Boolean getTimeOfUseTuesday() {
		return this.getTimeOfUseTuesdayChannel().value().get();
	}

	public default Boolean getTimeOfUseWednesday()  {
		return this.getTimeOfUseWednesdayChannel().value().get();
	}

	public default Boolean getTimeOfUseThursday()  {
		return this.getTimeOfUseThursdayChannel().value().get();
	}

	public default Boolean getTimeOfUseFriday()  {
		return this.getTimeOfUseFridayChannel().value().get();
	}

	public default Boolean getTimeOfUseSaturday()  {
		return this.getTimeOfUseSaturdayChannel().value().get();
	}

	public default Boolean getTimeOfUseSunday()  {
		return this.getTimeOfUseSundayChannel().value().get();
	}	
	
	// Setter für Time-of-Use-Bits
	public default void setTimeOfUseSellingEnabled(Boolean value) throws OpenemsNamedException {
	    this.getSetTimeOfUseSellingEnabledChannel().setNextWriteValue(value);
	}

	public default void setTimeOfUseMonday(Boolean value) throws OpenemsNamedException {
	    this.getSetTimeOfUseMondayChannel().setNextWriteValue(value);
	}

	public default void setTimeOfUseTuesday(Boolean value) throws OpenemsNamedException {
	    this.getSetTimeOfUseTuesdayChannel().setNextWriteValue(value);
	}

	public default void setTimeOfUseWednesday(Boolean value) throws OpenemsNamedException {
	    this.getSetTimeOfUseWednesdayChannel().setNextWriteValue(value);
	}

	public default void setTimeOfUseThursday(Boolean value) throws OpenemsNamedException {
	    this.getSetTimeOfUseThursdayChannel().setNextWriteValue(value);
	}

	public default void setTimeOfUseFriday(Boolean value) throws OpenemsNamedException {
	    this.getSetTimeOfUseFridayChannel().setNextWriteValue(value);
	}

	public default void setTimeOfUseSaturday(Boolean value) throws OpenemsNamedException {
	    this.getSetTimeOfUseSaturdayChannel().setNextWriteValue(value);
	}

	public default void setTimeOfUseSunday(Boolean value) throws OpenemsNamedException {
	    this.getSetTimeOfUseSundayChannel().setNextWriteValue(value);
	}
	
	
	
	/**
	 * Gets the Channel for {@link ChannelId#MAX_AC_EXPORT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxAcExportChannel() {
		return this.channel(ChannelId.MAX_AC_EXPORT);
	}

	/**
	 * Gets the Max AC-Export Power in [W]. Positive Values. See
	 * {@link ChannelId#MAX_AC_EXPORT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxAcExport() {
		return this.getMaxAcExportChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_AC_EXPORT}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxAcExport(Integer value) {
		this.getMaxAcExportChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_AC_IMPORT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxAcImportChannel() {
		return this.channel(ChannelId.MAX_AC_IMPORT);
	}

	/**
	 * Gets the Max AC-Import Power in [W]. Negative Values. See
	 * {@link ChannelId#MAX_AC_IMPORT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxAcImport() {
		return this.getMaxAcImportChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_AC_IMPORT}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxAcImport(Integer value) {
		this.getMaxAcImportChannel().setNextValue(value);
	}	
	
	// Setter for apparent power. This is where we actually control the ESS
	public default IntegerWriteChannel getSetPowerToGridTargetChannel() {
	    return this.channel(ChannelId.POWER_TO_GRID_TARGET);
	}
	
	public default void _setPowerToGridTarget(int value) throws OpenemsNamedException {
		this.getSetPowerToGridTargetChannel().setNextWriteValue(value);
	}	
	
	// Setter for apparent power. This is where we actually control the ESS
	public default IntegerReadChannel getPowerToGridTargetChannel() {
	    return this.channel(ChannelId.POWER_TO_GRID_TARGET);
	}
	
	public default Value<Integer> getPowerToGridTarget(){
		return this.getPowerToGridTargetChannel().value();
	}	
	
	//
	// Getter und Setter für SET_SELL_MODE_TIME_POINT_1_POWER
	//
	public default IntegerReadChannel getSellModeTimePoint1PowerChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_1_POWER);
	}

	public default IntegerWriteChannel getSetSellModeTimePoint1PowerChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_1_POWER);
	}

	public default Value<Integer> getSellModeTimePoint1Power() {
	    return this.getSellModeTimePoint1PowerChannel().value();
	}

	public default void setSellModeTimePoint1Power(int value) throws OpenemsNamedException {
	    this.getSetSellModeTimePoint1PowerChannel().setNextWriteValue(value);
	}

	//
	// Getter und Setter für SET_SELL_MODE_TIME_POINT_2_POWER
	//
	public default IntegerReadChannel getSellModeTimePoint2PowerChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_2_POWER);
	}

	public default IntegerWriteChannel getSetSellModeTimePoint2PowerChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_2_POWER);
	}

	public default Value<Integer> getSellModeTimePoint2Power() {
	    return this.getSellModeTimePoint2PowerChannel().value();
	}

	public default void setSellModeTimePoint2Power(int value) throws OpenemsNamedException {
	    this.getSetSellModeTimePoint2PowerChannel().setNextWriteValue(value);
	}

	//
	// Getter und Setter für SET_SELL_MODE_TIME_POINT_3_POWER
	//
	public default IntegerReadChannel getSellModeTimePoint3PowerChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_3_POWER);
	}

	public default IntegerWriteChannel getSetSellModeTimePoint3PowerChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_3_POWER);
	}

	public default Value<Integer> getSellModeTimePoint3Power() {
	    return this.getSellModeTimePoint3PowerChannel().value();
	}

	public default void setSellModeTimePoint3Power(int value) throws OpenemsNamedException {
	    this.getSetSellModeTimePoint3PowerChannel().setNextWriteValue(value);
	}

	//
	// Getter und Setter für SET_SELL_MODE_TIME_POINT_4_POWER
	//
	public default IntegerReadChannel getSellModeTimePoint4PowerChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_4_POWER);
	}

	public default IntegerWriteChannel getSetSellModeTimePoint4PowerChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_4_POWER);
	}

	public default Value<Integer> getSellModeTimePoint4Power() {
	    return this.getSellModeTimePoint4PowerChannel().value();
	}

	public default void setSellModeTimePoint4Power(int value) throws OpenemsNamedException {
	    this.getSetSellModeTimePoint4PowerChannel().setNextWriteValue(value);
	}

	//
	// Getter und Setter für SET_SELL_MODE_TIME_POINT_5_POWER
	//
	public default IntegerReadChannel getSellModeTimePoint5PowerChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_5_POWER);
	}

	public default IntegerWriteChannel getSetSellModeTimePoint5PowerChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_5_POWER);
	}

	public default Value<Integer> getSellModeTimePoint5Power() {
	    return this.getSellModeTimePoint5PowerChannel().value();
	}

	public default void setSellModeTimePoint5Power(int value) throws OpenemsNamedException {
	    this.getSetSellModeTimePoint5PowerChannel().setNextWriteValue(value);
	}

	//
	// Getter und Setter für SET_SELL_MODE_TIME_POINT_6_POWER
	//
	public default IntegerReadChannel getSellModeTimePoint6PowerChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_6_POWER);
	}

	public default IntegerWriteChannel getSetSellModeTimePoint6PowerChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_6_POWER);
	}

	public default Value<Integer> getSellModeTimePoint6Power() {
	    return this.getSellModeTimePoint6PowerChannel().value();
	}

	public default void setSellModeTimePoint6Power(int value) throws OpenemsNamedException {
	    this.getSetSellModeTimePoint6PowerChannel().setNextWriteValue(value);
	}
	
	//
	// Getter und Setter für SET_SELL_MODE_TIME_POINT_1 bis SET_SELL_MODE_TIME_POINT_6
	//

	// SET_SELL_MODE_TIME_POINT_1
	public default IntegerReadChannel getSellModeTimePoint1Channel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_1);
	}

	public default IntegerWriteChannel getSetSellModeTimePoint1Channel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_1);
	}

	public default Value<Integer> getSellModeTimePoint1() {
	    return this.getSellModeTimePoint1Channel().value();
	}

	public default void setSellModeTimePoint1(int value) throws OpenemsNamedException {
	    this.getSetSellModeTimePoint1Channel().setNextWriteValue(value);
	}

	// SET_SELL_MODE_TIME_POINT_2
	public default IntegerReadChannel getSellModeTimePoint2Channel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_2);
	}

	public default IntegerWriteChannel getSetSellModeTimePoint2Channel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_2);
	}

	public default Value<Integer> getSellModeTimePoint2() {
	    return this.getSellModeTimePoint2Channel().value();
	}

	public default void setSellModeTimePoint2(int value) throws OpenemsNamedException {
	    this.getSetSellModeTimePoint2Channel().setNextWriteValue(value);
	}

	// SET_SELL_MODE_TIME_POINT_3
	public default IntegerReadChannel getSellModeTimePoint3Channel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_3);
	}

	public default IntegerWriteChannel getSetSellModeTimePoint3Channel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_3);
	}

	public default Value<Integer> getSellModeTimePoint3() {
	    return this.getSellModeTimePoint3Channel().value();
	}

	public default void setSellModeTimePoint3(int value) throws OpenemsNamedException {
	    this.getSetSellModeTimePoint3Channel().setNextWriteValue(value);
	}

	// SET_SELL_MODE_TIME_POINT_4
	public default IntegerReadChannel getSellModeTimePoint4Channel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_4);
	}

	public default IntegerWriteChannel getSetSellModeTimePoint4Channel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_4);
	}

	public default Value<Integer> getSellModeTimePoint4() {
	    return this.getSellModeTimePoint4Channel().value();
	}

	public default void setSellModeTimePoint4(int value) throws OpenemsNamedException {
	    this.getSetSellModeTimePoint4Channel().setNextWriteValue(value);
	}

	// SET_SELL_MODE_TIME_POINT_5
	public default IntegerReadChannel getSellModeTimePoint5Channel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_5);
	}

	public default IntegerWriteChannel getSetSellModeTimePoint5Channel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_5);
	}

	public default Value<Integer> getSellModeTimePoint5() {
	    return this.getSellModeTimePoint5Channel().value();
	}

	public default void setSellModeTimePoint5(int value) throws OpenemsNamedException {
	    this.getSetSellModeTimePoint5Channel().setNextWriteValue(value);
	}

	// SET_SELL_MODE_TIME_POINT_6
	public default IntegerReadChannel getSellModeTimePoint6Channel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_6);
	}

	public default IntegerWriteChannel getSetSellModeTimePoint6Channel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_6);
	}

	public default Value<Integer> getSellModeTimePoint6() {
	    return this.getSellModeTimePoint6Channel().value();
	}

	public default void setSellModeTimePoint6(int value) throws OpenemsNamedException {
	    this.getSetSellModeTimePoint6Channel().setNextWriteValue(value);
	}
	
	
	//
	// Getter und Setter für SET_ACTIVE_POWER_REGULATION
	//
	public default IntegerReadChannel getActivePowerRegulationChannel() {
	    return this.channel(ChannelId.ACTIVE_POWER_REGULATION);
	}

	public default IntegerWriteChannel getSetActivePowerRegulationChannel() {
	    return this.channel(ChannelId.ACTIVE_POWER_REGULATION);
	}

	public default Value<Integer> getActivePowerRegulation() {
	    return this.getActivePowerRegulationChannel().value();
	}

	public default void setActivePowerRegulation(int value) throws OpenemsNamedException {
	    this.getSetActivePowerRegulationChannel().setNextWriteValue(value);
	}
	
	//
	// Getter und Setter für SZero Export Power
	//
	public default IntegerReadChannel getZeroExportPowerChannel() {
	    return this.channel(ChannelId.BATTERY_ZERO_EXPORT_POWER);
	}

	public default IntegerWriteChannel getSetZeroExportPowerChannel() {
	    return this.channel(ChannelId.SET_BATTERY_ZERO_EXPORT_POWER);
	}

	public default Value<Integer> getZeroExportPower() {
	    return this.getZeroExportPowerChannel().value();
	}

	public default void setZeroExportPower(int value) throws OpenemsNamedException {
	    this.getSetZeroExportPowerChannel().setNextWriteValue(value);
	}	
	
	
	
	// CHARGE_MODE_TIME_POINT_1
	public default IntegerReadChannel getChargeModeTimePoint1Channel() {
	    return this.channel(ChannelId.CHARGE_MODE_TIME_POINT_1);
	}
	public default IntegerWriteChannel getSetChargeModeTimePoint1Channel() {
	    return this.channel(ChannelId.CHARGE_MODE_TIME_POINT_1);
	}
	public default Value<Integer> getChargeModeTimePoint1() {
	    return this.getChargeModeTimePoint1Channel().value();
	}
	public default void setChargeModeTimePoint1(int value) throws OpenemsNamedException {
	    this.getSetChargeModeTimePoint1Channel().setNextWriteValue(value);
	}

	// CHARGE_MODE_TIME_POINT_2
	public default IntegerReadChannel getChargeModeTimePoint2Channel() {
	    return this.channel(ChannelId.CHARGE_MODE_TIME_POINT_2);
	}
	public default IntegerWriteChannel getSetChargeModeTimePoint2Channel() {
	    return this.channel(ChannelId.CHARGE_MODE_TIME_POINT_2);
	}
	public default Value<Integer> getChargeModeTimePoint2() {
	    return this.getChargeModeTimePoint2Channel().value();
	}
	public default void setChargeModeTimePoint2(int value) throws OpenemsNamedException {
	    this.getSetChargeModeTimePoint2Channel().setNextWriteValue(value);
	}

	// CHARGE_MODE_TIME_POINT_3
	public default IntegerReadChannel getChargeModeTimePoint3Channel() {
	    return this.channel(ChannelId.CHARGE_MODE_TIME_POINT_3);
	}
	public default IntegerWriteChannel getSetChargeModeTimePoint3Channel() {
	    return this.channel(ChannelId.CHARGE_MODE_TIME_POINT_3);
	}
	public default Value<Integer> getChargeModeTimePoint3() {
	    return this.getChargeModeTimePoint3Channel().value();
	}
	public default void setChargeModeTimePoint3(int value) throws OpenemsNamedException {
	    this.getSetChargeModeTimePoint3Channel().setNextWriteValue(value);
	}

	// CHARGE_MODE_TIME_POINT_4
	public default IntegerReadChannel getChargeModeTimePoint4Channel() {
	    return this.channel(ChannelId.CHARGE_MODE_TIME_POINT_4);
	}
	public default IntegerWriteChannel getSetChargeModeTimePoint4Channel() {
	    return this.channel(ChannelId.CHARGE_MODE_TIME_POINT_4);
	}
	public default Value<Integer> getChargeModeTimePoint4() {
	    return this.getChargeModeTimePoint4Channel().value();
	}
	public default void setChargeModeTimePoint4(int value) throws OpenemsNamedException {
	    this.getSetChargeModeTimePoint4Channel().setNextWriteValue(value);
	}

	// CHARGE_MODE_TIME_POINT_5
	public default IntegerReadChannel getChargeModeTimePoint5Channel() {
	    return this.channel(ChannelId.CHARGE_MODE_TIME_POINT_5);
	}
	public default IntegerWriteChannel getSetChargeModeTimePoint5Channel() {
	    return this.channel(ChannelId.CHARGE_MODE_TIME_POINT_5);
	}
	public default Value<Integer> getChargeModeTimePoint5() {
	    return this.getChargeModeTimePoint5Channel().value();
	}
	public default void setChargeModeTimePoint5(int value) throws OpenemsNamedException {
	    this.getSetChargeModeTimePoint5Channel().setNextWriteValue(value);
	}

	// CHARGE_MODE_TIME_POINT_6
	public default IntegerReadChannel getChargeModeTimePoint6Channel() {
	    return this.channel(ChannelId.CHARGE_MODE_TIME_POINT_6);
	}
	public default IntegerWriteChannel getSetChargeModeTimePoint6Channel() {
	    return this.channel(ChannelId.CHARGE_MODE_TIME_POINT_6);
	}
	public default Value<Integer> getChargeModeTimePoint6() {
	    return this.getChargeModeTimePoint6Channel().value();
	}
	public default void setChargeModeTimePoint6(int value) throws OpenemsNamedException {
	    this.getSetChargeModeTimePoint6Channel().setNextWriteValue(value);
	}
	
	
	// SELL_MODE_TIME_POINT_1_CAPACITY
	public default IntegerReadChannel getSellModeTimePoint1CapacityChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_1_CAPACITY);
	}
	public default IntegerWriteChannel getSetSellModeTimePoint1CapacityChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_1_CAPACITY);
	}
	public default Value<Integer> getSellModeTimePoint1Capacity() {
	    return this.getSellModeTimePoint1CapacityChannel().value();
	}
	public default void setSellModeTimePoint1Capacity(int value) throws OpenemsNamedException {
	    this.getSetSellModeTimePoint1CapacityChannel().setNextWriteValue(value);
	}

	// SELL_MODE_TIME_POINT_2_CAPACITY
	public default IntegerReadChannel getSellModeTimePoint2CapacityChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_2_CAPACITY);
	}
	public default IntegerWriteChannel getSetSellModeTimePoint2CapacityChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_2_CAPACITY);
	}
	public default Value<Integer> getSellModeTimePoint2Capacity() {
	    return this.getSellModeTimePoint2CapacityChannel().value();
	}
	public default void setSellModeTimePoint2Capacity(int value) throws OpenemsNamedException {
	    this.getSetSellModeTimePoint2CapacityChannel().setNextWriteValue(value);
	}

	// SELL_MODE_TIME_POINT_3_CAPACITY
	public default IntegerReadChannel getSellModeTimePoint3CapacityChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_3_CAPACITY);
	}
	public default IntegerWriteChannel getSetSellModeTimePoint3CapacityChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_3_CAPACITY);
	}
	public default Value<Integer> getSellModeTimePoint3Capacity() {
	    return this.getSellModeTimePoint3CapacityChannel().value();
	}
	public default void setSellModeTimePoint3Capacity(int value) throws OpenemsNamedException {
	    this.getSetSellModeTimePoint3CapacityChannel().setNextWriteValue(value);
	}

	// SELL_MODE_TIME_POINT_4_CAPACITY
	public default IntegerReadChannel getSellModeTimePoint4CapacityChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_4_CAPACITY);
	}
	public default IntegerWriteChannel getSetSellModeTimePoint4CapacityChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_4_CAPACITY);
	}
	public default Value<Integer> getSellModeTimePoint4Capacity() {
	    return this.getSellModeTimePoint4CapacityChannel().value();
	}
	public default void setSellModeTimePoint4Capacity(int value) throws OpenemsNamedException {
	    this.getSetSellModeTimePoint4CapacityChannel().setNextWriteValue(value);
	}

	// SELL_MODE_TIME_POINT_5_CAPACITY
	public default IntegerReadChannel getSellModeTimePoint5CapacityChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_5_CAPACITY);
	}
	public default IntegerWriteChannel getSetSellModeTimePoint5CapacityChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_5_CAPACITY);
	}
	public default Value<Integer> getSellModeTimePoint5Capacity() {
	    return this.getSellModeTimePoint5CapacityChannel().value();
	}
	public default void setSellModeTimePoint5Capacity(int value) throws OpenemsNamedException {
	    this.getSetSellModeTimePoint5CapacityChannel().setNextWriteValue(value);
	}

	// SELL_MODE_TIME_POINT_6_CAPACITY
	public default IntegerReadChannel getSellModeTimePoint6CapacityChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_6_CAPACITY);
	}
	public default IntegerWriteChannel getSetSellModeTimePoint6CapacityChannel() {
	    return this.channel(ChannelId.SELL_MODE_TIME_POINT_6_CAPACITY);
	}
	public default Value<Integer> getSellModeTimePoint6Capacity() {
	    return this.getSellModeTimePoint6CapacityChannel().value();
	}
	public default void setSellModeTimePoint6Capacity(int value) throws OpenemsNamedException {
	    this.getSetSellModeTimePoint6CapacityChannel().setNextWriteValue(value);
	}
	
	
	// Target Active Power
	public default IntegerReadChannel getTargetActivePowerChannel() {
	    return this.channel(ChannelId.TARGET_ACTIVE_POWER);
	}

	public default Value<Integer> getTargetActivePower() {
	    return this.getTargetActivePowerChannel().value();
	}
	public default void setTargetActivePower(int value) throws OpenemsNamedException {
	    this.getTargetActivePowerChannel().setNextValue(value);
	}	
	
	// Target Current
	public default IntegerReadChannel getTargetCurrentChannel() {
	    return this.channel(ChannelId.TARGET_CURRENT);
	}

	public default Value<Integer> getTargetCurrent() {
	    return this.getTargetCurrentChannel().value();
	}
	public default void setTargetCurrent(int value) throws OpenemsNamedException {
	    this.getTargetCurrentChannel().setNextValue(value);
	}	
	
	
	
	// This is actually the grid-side of the inverter an not necessarily the "real" grid output. Let´s say... AC-IN
	public default IntegerReadChannel getGridOutPowerChannel() {
	    return this.channel(ChannelId.GRID_OUTPUT_ACTIVE_POWER);
	}
	public default Value<Integer> getGridOutPower() {
	    return this.getGridOutPowerChannel().value();
	}	
	
	// internal work state
	public default Channel<WorkState> getWorkStateChannel() {
		return this.channel(ChannelId.WORK_STATE);
	}

	public default WorkState getWorkState() {
		return this.getWorkStateChannel().value().asEnum();
	}

	public default void setWorkState(WorkState value)  {
		this.getWorkStateChannel().setNextValue(value);
	}	
	
    // SET_REMOTE_MODE
    public default IntegerReadChannel getSetRemoteModeChannel() {
        return this.channel(ChannelId.SET_REMOTE_MODE);
    }
    public default IntegerWriteChannel getSetSetRemoteModeChannel() {
        return this.channel(ChannelId.SET_REMOTE_MODE);
    }
    public default Value<Integer> getSetRemoteMode() {
        return this.getSetRemoteModeChannel().value();
    }
    public default void setSetRemoteMode(int value) throws OpenemsNamedException {
        this.getSetSetRemoteModeChannel().setNextWriteValue(value);
    }

    // SET_CONTROL_MODE
    public default IntegerReadChannel getSetControlModeChannel() {
        return this.channel(ChannelId.SET_CONTROL_MODE);
    }
    public default IntegerWriteChannel getSetSetControlModeChannel() {
        return this.channel(ChannelId.SET_CONTROL_MODE);
    }
    public default Value<Integer> getSetControlMode() {
        return this.getSetControlModeChannel().value();
    }
    public default void setSetControlMode(int value) throws OpenemsNamedException {
        this.getSetSetControlModeChannel().setNextWriteValue(value);
    }

    // SET_BATTERY_CONTROL_MODE
    public default IntegerReadChannel getSetBatteryControlModeChannel() {
        return this.channel(ChannelId.SET_BATTERY_CONTROL_MODE);
    }
    public default IntegerWriteChannel getSetSetBatteryControlModeChannel() {
        return this.channel(ChannelId.SET_BATTERY_CONTROL_MODE);
    }
    public default Value<Integer> getSetBatteryControlMode() {
        return this.getSetBatteryControlModeChannel().value();
    }
    public default void setSetBatteryControlMode(int value) throws OpenemsNamedException {
        this.getSetSetBatteryControlModeChannel().setNextWriteValue(value);
    }

    // SET_3P_CONTROL_MODE
    public default IntegerReadChannel getSet3PControlModeChannel() {
        return this.channel(ChannelId.SET_3P_CONTROL_MODE);
    }
    public default IntegerWriteChannel getSetSet3PControlModeChannel() {
        return this.channel(ChannelId.SET_3P_CONTROL_MODE);
    }
    public default Value<Integer> getSet3PControlMode() {
        return this.getSet3PControlModeChannel().value();
    }
    public default void setSet3PControlMode(int value) throws OpenemsNamedException {
        this.getSetSet3PControlModeChannel().setNextWriteValue(value);
    }

    // SET_DC_POWER_PERCENT
    public default IntegerReadChannel getSetBatteryPowerPercentChannel() {
        return this.channel(ChannelId.SET_BATTERY_POWER_PERCENT);
    }
    public default IntegerWriteChannel getSetSetBatteryPowerPercentChannel() {
        return this.channel(ChannelId.SET_BATTERY_POWER_PERCENT);
    }
    public default Value<Integer> getSetBatteryPowerPercent() {
        return this.getSetBatteryPowerPercentChannel().value();
    }
    public default void setSetBatteryPowerPercent(int value) throws OpenemsNamedException {
        this.getSetSetBatteryPowerPercentChannel().setNextWriteValue(value);
    }

    // SET_BATTERY_POWER_SOC
    public default IntegerReadChannel getSetBatteryPowerSocChannel() {
        return this.channel(ChannelId.SET_BATTERY_POWER_SOC);
    }
    public default IntegerWriteChannel getSetSetBatteryPowerSocChannel() {
        return this.channel(ChannelId.SET_BATTERY_POWER_SOC);
    }
    public default Value<Integer> getSetBatteryPowerSoc() {
        return this.getSetBatteryPowerSocChannel().value();
    }
    public default void setSetBatteryPowerSoc(int value) throws OpenemsNamedException {
        this.getSetSetBatteryPowerSocChannel().setNextWriteValue(value);
    }

    // SET_AC_SETPOINT_3P
    public default IntegerReadChannel getSetAcSetpoint3pPercentChannel() {
        return this.channel(ChannelId.SET_AC_SETPOINT_3P_PERCENT);
    }
    public default IntegerWriteChannel getSetSetAcSetpoint3pPercentChannel() {
        return this.channel(ChannelId.SET_AC_SETPOINT_3P_PERCENT);
    }
    public default Value<Integer> getSetAcSetpoint3pPercent() {
        return this.getSetAcSetpoint3pPercentChannel().value();
    }
    public default void setSetAcSetpoint3pPercent(int value) throws OpenemsNamedException {
        this.getSetSetAcSetpoint3pPercentChannel().setNextWriteValue(value);
    }

    // SET_REMOTE_WATCHDOG_TIME
    public default IntegerReadChannel getSetRemoteWatchdogTimeChannel() {
        return this.channel(ChannelId.SET_REMOTE_WATCHDOG_TIME);
    }
    public default IntegerWriteChannel getSetSetRemoteWatchdogTimeChannel() {
        return this.channel(ChannelId.SET_REMOTE_WATCHDOG_TIME);
    }
    public default Value<Integer> getSetRemoteWatchdogTime() {
        return this.getSetRemoteWatchdogTimeChannel().value();
    }
    public default void setSetRemoteWatchdogTime(int value) throws OpenemsNamedException {
        this.getSetSetRemoteWatchdogTimeChannel().setNextWriteValue(value);
    }
	
    
    // SET_FUCKOFF_1
    public default IntegerReadChannel getFuckOff1Channel() {
        return this.channel(ChannelId.FUCKOFF_1);
    }
    public default IntegerWriteChannel getSetFuckOff1Channel() {
        return this.channel(ChannelId.FUCKOFF_1);
    }
    public default Value<Integer> getFuckOff1() {
        return this.getFuckOff1Channel().value();
    }
    public default void setFuckOff1(int value) throws OpenemsNamedException {
        this.getSetFuckOff1Channel().setNextWriteValue(value);
    }    
    
    // SET_FUCKOFF_2
    public default IntegerReadChannel getFuckOff2Channel() {
        return this.channel(ChannelId.FUCKOFF_2);
    }
    public default IntegerWriteChannel getSetFuckOff2Channel() {
        return this.channel(ChannelId.FUCKOFF_2);
    }
    public default Value<Integer> getFuckOff2() {
        return this.getFuckOff2Channel().value();
    }
    public default void setFuckOff2(int value) throws OpenemsNamedException {
        this.getSetFuckOff2Channel().setNextWriteValue(value);
    }    
    
    
	 // -----------------------------------------------------------------------------
	 // Battery Constant Voltage
	 // -----------------------------------------------------------------------------
	
	 public default IntegerReadChannel getBatteryConstantVoltageChannel() {
	     return this.channel(ChannelId.SET_BATTERY_CONSTANT_VOLTAGE);
	 }
	
	 public default IntegerWriteChannel getSetBatteryConstantVoltageChannel() {
	     return this.channel(ChannelId.SET_BATTERY_CONSTANT_VOLTAGE);
	 }
	
	 public default Value<Integer> getBatteryConstantVoltage() {
	     return this.getBatteryConstantVoltageChannel().value();
	 }
	
	 public default void setBatteryConstantVoltage(int value) throws OpenemsNamedException {
	     this.getSetBatteryConstantVoltageChannel().setNextWriteValue(value);
	 }
	
	 // -----------------------------------------------------------------------------
	 // Battery Constant Current
	 // -----------------------------------------------------------------------------
	
	 public default IntegerReadChannel getBatteryConstantCurrentChannel() {
	     return this.channel(ChannelId.SET_BATTERY_CONSTANT_CURRENT);
	 }
	
	 public default IntegerWriteChannel getSetBatteryConstantCurrentChannel() {
	     return this.channel(ChannelId.SET_BATTERY_CONSTANT_CURRENT);
	 }
	
	 public default Value<Integer> getBatteryConstantCurrent() {
	     return this.getBatteryConstantCurrentChannel().value();
	 }
	
	 public default void setBatteryConstantCurrent(int value) throws OpenemsNamedException {
	     this.getSetBatteryConstantCurrentChannel().setNextWriteValue(value);
	 }
	    

	// As Error messages 1-4 have to be treated as a 64 (Quadruple) element with LSW first
	// and OpenEMS does not offer this word order we have to do it manually.
	// Example Word 2 = 32 -> F22
	private static Set<Integer> readErrorCodes(OpenemsComponent self) {
	    Set<Integer> result = new HashSet<>();
	    addErrorBits(result, self.channel(ChannelId.ERROR_MESSAGE_1).value().asOptional(), 1);
	    addErrorBits(result, self.channel(ChannelId.ERROR_MESSAGE_2).value().asOptional(), 17);
	    addErrorBits(result, self.channel(ChannelId.ERROR_MESSAGE_3).value().asOptional(), 33);
	    addErrorBits(result, self.channel(ChannelId.ERROR_MESSAGE_4).value().asOptional(), 49);
	    	    
	    return result;
	}

	private static void addErrorBits(Set<Integer> errors, Optional<?> opt, int bitOffset) {
	    if (opt.isPresent() && opt.get() instanceof Integer) {
	        int value = (Integer) opt.get();
	        for (int i = 0; i < 16; i++) {
	            if ((value & (1 << i)) != 0) {
	                errors.add(bitOffset + i);
	            }
	        }
	    }
	}

	public static void updateErrorState(OpenemsComponent self) {
	    Set<Integer> activeErrors = readErrorCodes(self);

	    for (ChannelId channelId : ChannelId.values()) {
	        if (!channelId.name().matches("F\\d+")) {
	            continue; // Only Error Channels
	        }

	        int code = Integer.parseInt(channelId.name().substring(1)); // "F22" → 22
	        boolean isActive = activeErrors.contains(code);

	        self.channel(channelId).setNextValue(isActive); // true = Fehler aktiv
	    }
	}

	/**
	 * Check if F58 ("BMS communication failure") is active
	 * 
	 * @param self 
	 * @return true, if F58 is active
	 */
	public static boolean isBmsCommError(OpenemsComponent self) {
	    Set<Integer> activeErrors = readErrorCodes(self);
	    return activeErrors.contains(58);
	}

}
