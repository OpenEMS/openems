package io.openems.edge.solaredge.hybrid.ess;


import java.util.ArrayList;
import java.util.List;
//import java.util.ArrayList;
//import java.util.List;
import java.util.Map;
//import java.util.Objects;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import io.openems.edge.common.event.EdgeEventConstants;
import org.osgi.service.metatype.annotations.Designate;
import com.google.common.collect.ImmutableMap;
import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.sunspec.AbstractSunSpecEss;

import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.solaredge.enums.ControlMode;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.solaredge.enums.AcChargePolicy;
import io.openems.edge.solaredge.enums.ChargeDischargeMode;

import io.openems.edge.solaredge.charger.SolaredgeDcCharger;



@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "SolarEdge.Hybrid.ESS", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE) //

@EventTopics({ //
	EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
	EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
})

public class SolarEdgeHybridEssImpl extends AbstractSunSpecEss
		implements SolarEdgeHybridEss, ManagedSymmetricEss, SymmetricEss, HybridEss, ModbusComponent,
		OpenemsComponent, EventHandler, ModbusSlave, TimedataProvider {
		//implements SunSpecEss, SymmetricEss, HybridEss,  ModbusComponent, OpenemsComponent, ModbusSlave, EventHandler {


	private static final int READ_FROM_MODBUS_BLOCK = 1;
	private final List<SolaredgeDcCharger> chargers = new ArrayList<>();
	
	// Hardware-Limits
	protected static final int HW_MAX_APPARENT_POWER = 5200;
	protected static final int HW_ALLOWED_CHARGE_POWER = -5000;
	protected static final int HW_ALLOWED_DISCHARGE_POWER = 5000;
	
	int CycleCounter =60;
	
	//this._setAllowedChargePower(HW_ALLOWED_CHARGE_POWER);
	//this._setAllowedChargePower(HW_ALLOWED_DISCHARGE_POWER);
	
	private Config config;
	
	@Reference
	private Power power;
	
	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW) //
			.put(DefaultSunSpecModel.S_103, Priority.LOW) //
			//.put(DefaultSunSpecModel.S_120, Priority.LOW) //
			//.put(DefaultSunSpecModel.S_802, Priority.LOW) //
			.build();
	@Reference
	protected ComponentManager componentManager;

	
	@Reference
	protected ConfigurationAdmin cm;
	
	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;	
	

	public SolarEdgeHybridEssImpl() throws OpenemsException {
		super(//
				ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				HybridEss.ChannelId.values(), //
				//EssDcCharger.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				//SymmetricEss.ChannelId.values(), //
				//SunSpecEss.ChannelId.values(), //
				//SolaredgeDcCharger.ChannelId.values(), //
				SolarEdgeHybridEss.ChannelId.values()
		);

		addStaticModbusTasks(this.getModbusProtocol());
		
	}



	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(),  this.cm,
				"Modbus", config.modbus_id(), READ_FROM_MODBUS_BLOCK)) {
			return;
		}
		this.config = config;
	}
	
	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
	
	
	@Override
	public void addCharger(SolaredgeDcCharger charger) {
		this.chargers.add(charger);
	}

	@Override
	public void removeCharger(SolaredgeDcCharger charger) {
		this.chargers.remove(charger);
	}
	
	@Override
	public String getModbusBridgeId() {
		return this.config.modbus_id();
	}
	
	@Override
	public void applyPower(int activePowerWanted, int reactivePowerWanted) throws OpenemsNamedException {
		CycleCounter++;
		
		// Using separate channel for the demanded charge/discharge power
		this._setChargePowerWanted(activePowerWanted);
		
		// Read-only mode -> switch to max. self consumption automatic
		if (this.config.readOnlyMode() ) {
			if ( CycleCounter >= 0) {
				CycleCounter=0;
				// Switch to automatic mode
				this._setControlMode(ControlMode.SE_CTRL_MODE_MAX_SELF_CONSUMPTION);
				setLimits();				
			}
			return;
		}
		else {
			if (CycleCounter >= 0) { // Set values every Cycle
				CycleCounter=0;
				

				int maxDischargePeakPower 	= getMaxDischargePeakPower().get();
				int maxChargePeakPower 		= getMaxChargePeakPower().get() -1;
		
				if (isControlModeRemote() == false || isStorageChargePolicyAlways() == false)
				{
					this._setControlMode(ControlMode.SE_CTRL_MODE_REMOTE); // Now the device can be remote controlled	
					this._setAcChargePolicy(AcChargePolicy.SE_CHARGE_DISCHARGE_MODE_ALWAYS);
					
					//The next 2 are fallback values which should become active after the 60 seonds timeout
					this._setChargeDischargeDefaultMode(ChargeDischargeMode.SE_CHARGE_POLICY_MAX_SELF_CONSUMPTION);
					this._setRemoteControlTimeout(60);
					 
				}
				// We assume to be in RC-Mode
				
				if (activePowerWanted <= 0) { // Negative Values are for charging
					/*
					 * Battery charging decreases when Soc > 90%. We have to set the wanted value to the max. power which the
					 * battery is willing to handle 
					 */
					
					if (activePowerWanted < maxChargePeakPower) activePowerWanted = maxChargePeakPower;
					
					if (activePowerWanted > 500) activePowerWanted -= 200; //Decrease Charging as a buffer

					this._setRemoteControlCommandMode(ChargeDischargeMode.SE_CHARGE_POLICY_PV_AC); // Mode for charging);
					this._setMaxChargePower(activePowerWanted * -1);// Values for register must be positive
					
				}
				else {
					/*
					 * The same for discharging. The battery can be discharged with high rates at the beginning
					 */
					if (activePowerWanted > maxDischargePeakPower) activePowerWanted = maxDischargePeakPower;
					
					this._setRemoteControlCommandMode(ChargeDischargeMode.SE_CHARGE_POLICY_MAX_EXPORT); // Mode for Discharging);
					this._setMaxDischargePower(activePowerWanted);				
				}
			}
	
		}
		
	}


	
	private void setLimits()  {
	
		_setAllowedChargePower(this.config.ChargePowerLimit() *-1);
		_setAllowedDischargePower(this.config.DischargePowerLimit());
		_setMaxApparentPower(HW_MAX_APPARENT_POWER);
	}

	
	
	
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}
	
	private boolean isControlModeRemote() {
		
		EnumReadChannel controlModeChannel = this.channel(SolarEdgeHybridEss.ChannelId.CONTROL_MODE);
		ControlMode controlMode = controlModeChannel.value().asEnum();
		
		return controlMode == ControlMode.SE_CTRL_MODE_REMOTE;

	}
	
	private boolean isStorageChargePolicyAlways() {
		EnumReadChannel acChargePolicyChannel = this.channel(SolarEdgeHybridEss.ChannelId.AC_CHARGE_POLICY);
		AcChargePolicy acChargePolicy = acChargePolicyChannel.value().asEnum();
		
		return acChargePolicy == AcChargePolicy.SE_CHARGE_DISCHARGE_MODE_ALWAYS;
	}
	
	
	/**
	 * Adds static modbus tasks.
	 * 
	 * @param protocol the {@link ModbusProtocol}
	 * @throws OpenemsException on error
	 */
	private void addStaticModbusTasks(ModbusProtocol protocol) throws OpenemsException {
		protocol.addTask(//
		new FC3ReadRegistersTask(0xE004, Priority.LOW, //
				m(SolarEdgeHybridEss.ChannelId.CONTROL_MODE, 
						new UnsignedWordElement(0xE004)),
				m(SolarEdgeHybridEss.ChannelId.AC_CHARGE_POLICY, 
						new UnsignedWordElement(0xE005)),					
				m(SolarEdgeHybridEss.ChannelId.MAX_CHARGE_LIMIT, 
						new FloatDoublewordElement(0xE006).wordOrder(WordOrder.LSWMSW)),					
				m(SolarEdgeHybridEss.ChannelId.STORAGE_BACKUP_LIMIT, 
						new FloatDoublewordElement(0xE008).wordOrder(WordOrder.LSWMSW)),
				m(SolarEdgeHybridEss.ChannelId.CHARGE_DISCHARGE_DEFAULT_MODE, 
						new UnsignedWordElement(0xE00A)),
				m(SolarEdgeHybridEss.ChannelId.REMOTE_CONTROL_TIMEOUT, 
						new UnsignedDoublewordElement(0xE00B).wordOrder(WordOrder.LSWMSW)),
				m(SolarEdgeHybridEss.ChannelId.REMOTE_CONTROL_COMMAND_MODE, 
						new UnsignedWordElement(0xE00D)),
				m(SolarEdgeHybridEss.ChannelId.MAX_CHARGE_POWER, 
						new FloatDoublewordElement(0xE00E).wordOrder(WordOrder.LSWMSW)),
				m(SolarEdgeHybridEss.ChannelId.MAX_DISCHARGE_POWER, 
						new FloatDoublewordElement(0xE010).wordOrder(WordOrder.LSWMSW)),
				new DummyRegisterElement(0xE012, 0xE141), // Reserved
				m(HybridEss.ChannelId.DC_DISCHARGE_ENERGY, //
						new FloatDoublewordElement(0xE142).wordOrder(WordOrder.LSWMSW)),
				m(SolarEdgeHybridEss.ChannelId.MAX_CHARGE_CONTINUES_POWER, //
						new FloatDoublewordElement(0xE144).wordOrder(WordOrder.LSWMSW)),
				m(SolarEdgeHybridEss.ChannelId.MAX_DISCHARGE_CONTINUES_POWER, //
						new FloatDoublewordElement(0xE146).wordOrder(WordOrder.LSWMSW)),
				m(SolarEdgeHybridEss.ChannelId.MAX_CHARGE_PEAK_POWER, //
						new FloatDoublewordElement(0xE148).wordOrder(WordOrder.LSWMSW)),						
				m(SolarEdgeHybridEss.ChannelId.MAX_DISCHARGE_PEAK_POWER, //
						new FloatDoublewordElement(0xE14A).wordOrder(WordOrder.LSWMSW)),
						
				new DummyRegisterElement(0xE14C, 0xE16B), // Reserved
				m(SolarEdgeHybridEss.ChannelId.BATT_AVG_TEMPERATURE, //
						new FloatDoublewordElement(0xE16C).wordOrder(WordOrder.LSWMSW)),	
				m(SolarEdgeHybridEss.ChannelId.BATT_MAX_TEMPERATURE, //
						new FloatDoublewordElement(0xE16E).wordOrder(WordOrder.LSWMSW)),
				m(SolarEdgeHybridEss.ChannelId.BATT_ACTUAL_VOLTAGE, //
						new FloatDoublewordElement(0xE170).wordOrder(WordOrder.LSWMSW)),						
				m(SolarEdgeHybridEss.ChannelId.BATT_ACTUAL_CURRENT, //
						new FloatDoublewordElement(0xE172).wordOrder(WordOrder.LSWMSW)),							
				m(HybridEss.ChannelId.DC_DISCHARGE_POWER, //
						new FloatDoublewordElement(0xE174).wordOrder(WordOrder.LSWMSW),
						ElementToChannelConverter.INVERT), //
				//new DummyRegisterElement(0xE176, 0xE17D), 
				m(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, //
						new UnsignedQuadruplewordElement(0xE176).wordOrder(WordOrder.LSWMSW)),
				m(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY, //
						new UnsignedQuadruplewordElement(0xE17A).wordOrder(WordOrder.LSWMSW)),
				m(SymmetricEss.ChannelId.CAPACITY, //
						new FloatDoublewordElement(0xE17E).wordOrder(WordOrder.LSWMSW)),
				m(SolarEdgeHybridEss.ChannelId.AVAIL_ENERGY, //
						new FloatDoublewordElement(0xE180).wordOrder(WordOrder.LSWMSW)),
				m(SolarEdgeHybridEss.ChannelId.SOH, //
						new FloatDoublewordElement(0xE182).wordOrder(WordOrder.LSWMSW)),
				m(SymmetricEss.ChannelId.SOC, //
						new FloatDoublewordElement(0xE184).wordOrder(WordOrder.LSWMSW)),
				m(SolarEdgeHybridEss.ChannelId.BATTERY_STATUS, //
						new UnsignedDoublewordElement(0xE186).wordOrder(WordOrder.LSWMSW)))				
						
						
				);			
				
						
				
		
		protocol.addTask(//
		new FC16WriteRegistersTask(0xE004, 
				m(SolarEdgeHybridEss.ChannelId.CONTROL_MODE, new SignedWordElement(0xE004)),
				m(SolarEdgeHybridEss.ChannelId.AC_CHARGE_POLICY, new SignedWordElement(0xE005)), // Max. charge power. Negative values
				m(SolarEdgeHybridEss.ChannelId.MAX_CHARGE_LIMIT, new FloatDoublewordElement(0xE006).wordOrder(WordOrder.LSWMSW)),  // kWh or percent
				m(SolarEdgeHybridEss.ChannelId.STORAGE_BACKUP_LIMIT, new FloatDoublewordElement(0xE008).wordOrder(WordOrder.LSWMSW)),  // Percent of capacity 
				m(SolarEdgeHybridEss.ChannelId.CHARGE_DISCHARGE_DEFAULT_MODE, new UnsignedWordElement(0xE00A)), // Usually set to 1 (Charge PV excess only)
				m(SolarEdgeHybridEss.ChannelId.REMOTE_CONTROL_TIMEOUT, new UnsignedDoublewordElement(0xE00B).wordOrder(WordOrder.LSWMSW)),
				m(SolarEdgeHybridEss.ChannelId.REMOTE_CONTROL_COMMAND_MODE, new UnsignedWordElement(0xE00D)),
				m(SolarEdgeHybridEss.ChannelId.MAX_CHARGE_POWER, new FloatDoublewordElement(0xE00E).wordOrder(WordOrder.LSWMSW)),  // Max. charge power. Negative values
				m(SolarEdgeHybridEss.ChannelId.MAX_DISCHARGE_POWER, new FloatDoublewordElement(0xE010).wordOrder(WordOrder.LSWMSW)) // Max. discharge power. Positive values
				)); // Disabled, automatic, remote controlled, etc.	
		
		
		// If no managed System is implemented these registers don´t need to be set
		
		//m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, new FloatDoublewordElement(0xE00E).wordOrder(WordOrder.LSWMSW)),  // Max. charge power. Negative values
		//m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, new FloatDoublewordElement(0xE010).wordOrder(WordOrder.LSWMSW)) // Max. discharge power. Positive values
		
							
	}


	

	@Override
	protected void onSunSpecInitializationCompleted()  {
		// TODO Add mappings for registers from S1 and S103

		// Example:
		// this.mapFirstPointToChannel(//
		// SymmetricEss.ChannelId.ACTIVE_POWER, //
		// ElementToChannelConverter.DIRECT_1_TO_1, //
		// DefaultSunSpecModel.S103.W);
	
		// this.mapFirstPointToChannel(//
		// SymmetricEss.ChannelId.CONSUMPTION_POWER, //
		// ElementToChannelConverter.DIRECT_1_TO_1, //
		// DefaultSunSpecModel.S103.W);
		
		 //DefaultSunSpecModel.S103.W);
		
		this.mapFirstPointToChannel(//
				ManagedSymmetricPvInverter.ChannelId.MAX_APPARENT_POWER, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S120.W_RTG);
		
		// AC-Output from the Inverter. Could be the combination from PV + battery
		this.mapFirstPointToChannel(//
			SymmetricEss.ChannelId.ACTIVE_POWER, //
			ElementToChannelConverter.DIRECT_1_TO_1, //
			DefaultSunSpecModel.S103.W);		
		
		
		this.mapFirstPointToChannel(//
				SymmetricEss.ChannelId.REACTIVE_POWER, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S103.V_AR);		
		

		
		
		
		
		this.setLimits();

	}


	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|WantedPower:" + this.getChargePowerWanted().asString() //
				+ "|Allowed Charge Power/Peak:"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER).value().asStringWithoutUnit() + " / " + this.channel(SolarEdgeHybridEss.ChannelId.MAX_CHARGE_PEAK_POWER).value().asStringWithoutUnit() + ";"
				+ "|Allowed DisCharge Power/Peak:"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER).value().asStringWithoutUnit() + " / " + this.channel(SolarEdgeHybridEss.ChannelId.MAX_DISCHARGE_PEAK_POWER).value().asStringWithoutUnit() + ";"
				+ "|ControlMode " + this.channel(SolarEdgeHybridEss.ChannelId.CONTROL_MODE).value().asStringWithoutUnit() //
				+ "|ChargePolicy " + this.channel(SolarEdgeHybridEss.ChannelId.AC_CHARGE_POLICY).value().asStringWithoutUnit() //
				+ "|DefaultMode " + this.channel(SolarEdgeHybridEss.ChannelId.CHARGE_DISCHARGE_DEFAULT_MODE).value().asStringWithoutUnit() //
				+ "|RemoteControlMode " + this.channel(SolarEdgeHybridEss.ChannelId.REMOTE_CONTROL_COMMAND_MODE).value().asStringWithoutUnit() //
				+ "|ChargePower " + this.channel(SolarEdgeHybridEss.ChannelId.MAX_CHARGE_POWER).value().asStringWithoutUnit() //
				+ "|DischargePower " + this.channel(SolarEdgeHybridEss.ChannelId.MAX_DISCHARGE_POWER).value().asStringWithoutUnit() //
				+ "|CommandTimeout " + this.channel(SolarEdgeHybridEss.ChannelId.REMOTE_CONTROL_TIMEOUT).value().asStringWithoutUnit() //
				
				+ "|" + this.getGridModeChannel().value().asOptionString() //
				+ "|Feed-In:";
	}	

	
	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}
	
	@Override
	public void handleEvent(Event event) {
		//super.handleEvent(event);
		
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:

			break;	
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			this.setLimits();
			break;	
		}
	}	

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				HybridEss.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(SolarEdgeHybridEssImpl.class, accessMode, 100) //
						.build());
	}

	@Override
	public Integer getSurplusPower() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Power getPower() {
		return this.power;
	}


	@Override
	public int getPowerPrecision() {
		// 
		return 1;
	}

	@Override
	public boolean isManaged() {
		return true;
		
		// Just for Testing
		// return !this.config.readOnlyMode();
	}
	
}
