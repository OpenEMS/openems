package io.openems.edge.pytes.ess;

import static io.openems.edge.common.cycle.Cycle.DEFAULT_CYCLE_TIME;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

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
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;

import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.generic.common.CycleProvider;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.pytes.battery.PytesBattery;
import io.openems.edge.pytes.dccharger.PytesDcCharger;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Pytes.Hybrid.ESS", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class PytesJs3Impl extends AbstractOpenemsModbusComponent implements PytesJs3, HybridEss, SymmetricEss,
		ManagedSymmetricEss, AsymmetricEss, ManagedAsymmetricEss, OpenemsComponent, ModbusComponent, EventHandler, CycleProvider {

	@Reference
	private ConfigurationAdmin cm;
	
	@Reference
	private ComponentManager componentManager;
	
	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}	
	
	
	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;	

	@Reference
	private Power power;
	
	@Reference
	private Cycle cycle;		
	
	private volatile ApplyPowerHandler applyPowerHandler = null;
	private volatile AllowedChargeDischargeHandler allowedChargeDischargeHandler = null;
		
	private final Logger log = LoggerFactory.getLogger(PytesJs3Impl.class);
	private Config config = null;

	private PytesDcCharger charger;
	private PytesBattery battery;
	
	private static final int MAX_APPARENT_POWER = 5000;		
	


	public PytesJs3Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				HybridEss.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				ManagedAsymmetricEss.ChannelId.values(), //
				PytesJs3.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			// TODO: fill channels
			this._setSoc(this.battery.getSoc().get());   // Integer value		
			this._setDcDischargePower(this.battery.getDcDischargePower().get());
			if (this.allowedChargeDischargeHandler != null) {
				this.allowedChargeDischargeHandler.accept(this.componentManager);
			}			
			break;
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				
/*
 *  not really reliable
				new FC16WriteRegistersTask(43128,
						m(PytesJs3.ChannelId.SET_REMOTE_CONTROL_AC_GRID_PORT_POWER, new UnsignedWordElement(43128)),
						new DummyRegisterElement(43129, 43131),
						m(PytesJs3.ChannelId.SET_REMOTE_CONTROL_MODE, new UnsignedWordElement(43132))),
*/
				
				new FC16WriteRegistersTask(44100,
						m(PytesJs3.ChannelId.SET_REMOTE_DISPATCH_SWITCH, new UnsignedWordElement(44100)),
						m(PytesJs3.ChannelId.SET_REMOTE_DISPATCH_TIMEOUT, new UnsignedWordElement(44101)),
						m(PytesJs3.ChannelId.SET_REMOTE_DISPATCH_SYSTEM_LIMIT_SWITCH, new UnsignedWordElement(44102)),
						m(PytesJs3.ChannelId.SET_REMOTE_DISPATCH_SYSTEM_IMPORT_LIMIT, new UnsignedWordElement(44103)),
						m(PytesJs3.ChannelId.SET_REMOTE_DISPATCH_SYSTEM_EXPORT_LIMIT, new UnsignedWordElement(44104)),
						m(PytesJs3.ChannelId.SET_REMOTE_DISPATCH_REALTIME_CONTROL_SWITCH, new UnsignedWordElement(44105)),
						m(PytesJs3.ChannelId.SET_REMOTE_DISPATCH_REALTIME_CONTROL_POWER, new SignedDoublewordElement(44106)),
						m(PytesJs3.ChannelId.SET_REMOTE_DISPATCH_REALTIME_CONTROL_FUNCTION_SWITCH, new UnsignedWordElement(44108))),

				new FC4ReadInputRegistersTask(33067, Priority.HIGH, //

						m(PytesJs3.ChannelId.INVERTED_RATED_APPARENT_POWER, new UnsignedWordElement(33067),
								ElementToChannelConverter.SCALE_FACTOR_1),

						m(PytesJs3.ChannelId.SAFETY_VERSION, new UnsignedWordElement(33068)),

						// Form complete HMI version with (33002)
						m(PytesJs3.ChannelId.HMI_SUB_VERSION, new UnsignedWordElement(33069)),

						m(PytesJs3.ChannelId.ALARM_CODE_DATA, new UnsignedWordElement(33070)),

						m(PytesJs3.ChannelId.DC_BUS_VOLTAGE, new UnsignedWordElement(33071),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(PytesJs3.ChannelId.DC_BUS_HALF_VOLTAGE, new UnsignedWordElement(33072),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(PytesJs3.ChannelId.VOLTAGE_L1, new UnsignedWordElement(33073),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(PytesJs3.ChannelId.VOLTAGE_L2, new UnsignedWordElement(33074),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(PytesJs3.ChannelId.VOLTAGE_L3, new UnsignedWordElement(33075),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(PytesJs3.ChannelId.CURRENT_L1, new UnsignedWordElement(33076),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(PytesJs3.ChannelId.CURRENT_L2, new UnsignedWordElement(33077),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(PytesJs3.ChannelId.CURRENT_L3, new UnsignedWordElement(33078),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(33079)),
						m(SymmetricEss.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(33081)),
						m(PytesJs3.ChannelId.APPARENT_POWER, new SignedDoublewordElement(33083)),
						new DummyRegisterElement(33085, 33090),
						m(PytesJs3.ChannelId.STANDARD_WORKING_MODE, new UnsignedWordElement(33091)), // Todo: Check what does it mean. Starts with WM 3???
						new DummyRegisterElement(33092, 33093),						
						m(PytesJs3.ChannelId.FREQUENCY, new UnsignedWordElement(33094)),
						m(PytesJs3.ChannelId.INVERTER_CURRENT_STATUS, new UnsignedWordElement(33095)), // see appendix 2
						m(PytesJs3.ChannelId.LEAD_ACID_BATTERY_TEMP, new SignedWordElement(33096),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(PytesJs3.ChannelId.FUNCTION_STATUS, new UnsignedWordElement(33097),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(PytesJs3.ChannelId.CURRENT_DRM_CODE_STATUS, new UnsignedWordElement(33098),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(PytesJs3.ChannelId.INVERTER_CABINET_TEMP, new SignedWordElement(33099),
								ElementToChannelConverter.SCALE_FACTOR_2),

						new DummyRegisterElement(33100, 33103),						
						
						m(PytesJs3.ChannelId.LIMITED_POWER_ACTUAL_VALUE, new UnsignedWordElement(33104),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(PytesJs3.ChannelId.PF_ADJUSTMENT_ACTUAL_VALUE, new SignedWordElement(33105),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(PytesJs3.ChannelId.LIMITED_REACTIVE_POWER, new SignedWordElement(33106),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(PytesJs3.ChannelId.INVERTER_MODULE_TEMP2, new SignedWordElement(33107),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(PytesJs3.ChannelId.VOLT_VAR_VREF_RT_VALUES, new UnsignedWordElement(33108),
								ElementToChannelConverter.SCALE_FACTOR_2),
						
						new DummyRegisterElement(33109, 33109),	
						
						m(PytesJs3.ChannelId.BMS_CHARGING_VOLTAGE_LIMIT, new UnsignedWordElement(33110),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(PytesJs3.ChannelId.BATTERY_BMS_STATUS, new UnsignedWordElement(33111),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(PytesJs3.ChannelId.INVERTER_INITIAL_SETTING_STATE, new UnsignedWordElement(33112),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(PytesJs3.ChannelId.BATCH_UPGRADE_BOWL, new UnsignedWordElement(33113),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(PytesJs3.ChannelId.FCAS_MODE_RUNNING_STATUS, new UnsignedWordElement(33114),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(PytesJs3.ChannelId.SETTING_FLAG_BIT, new UnsignedWordElement(33115),
								ElementToChannelConverter.SCALE_FACTOR_2),

						// Fault Code (01..07)
						m(PytesJs3.ChannelId.FAULT_CODE_01, new UnsignedWordElement(33116),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(PytesJs3.ChannelId.FAULT_CODE_02, new UnsignedWordElement(33117),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(PytesJs3.ChannelId.FAULT_CODE_03, new UnsignedWordElement(33118),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(PytesJs3.ChannelId.FAULT_CODE_04, new UnsignedWordElement(33119),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(PytesJs3.ChannelId.FAULT_CODE_05, new UnsignedWordElement(33120),
								ElementToChannelConverter.SCALE_FACTOR_2),
						
						m(PytesJs3.ChannelId.OPERATING_STATUS, new UnsignedWordElement(33121),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(PytesJs3.ChannelId.OPERATING_MODE, new UnsignedWordElement(33122),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(PytesJs3.ChannelId.WORKING_MODE_RUNNING_STATUS, new UnsignedWordElement(33123),
								ElementToChannelConverter.SCALE_FACTOR_2),						

						m(PytesJs3.ChannelId.FAULT_CODE_06, new UnsignedWordElement(33124),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(PytesJs3.ChannelId.FAULT_CODE_07, new UnsignedWordElement(33125),
								ElementToChannelConverter.SCALE_FACTOR_2),

						new DummyRegisterElement(33126, 33131),	

						m(PytesJs3.ChannelId.STORAGE_CONTROL_SWITCHING_VALUE, new UnsignedWordElement(33132),
								ElementToChannelConverter.SCALE_FACTOR_2)

				));

	}

	@Override
	public String debugLog() {
		if (config.debugMode()) {
			return "SoC:" + this.getSoc().asString() //
					+ "|L:" + this.getActivePower().asString()
					+ this.channel(SymmetricEss.ChannelId.REACTIVE_POWER).value().asString()
					+ "\nInvertedRatedApparentPower=" + this.channel(PytesJs3.ChannelId.INVERTED_RATED_APPARENT_POWER).value().asString()
					+ "\nSafetyVersion=" + this.channel(PytesJs3.ChannelId.SAFETY_VERSION).value().asString()
					+ "\nHmiSubVersion=" + this.channel(PytesJs3.ChannelId.HMI_SUB_VERSION).value().asString()
					+ "\nStandardWorkingMode=" + this.channel(PytesJs3.ChannelId.STANDARD_WORKING_MODE).value().asString()
					+ "\nAlarmCodeData=" + this.channel(PytesJs3.ChannelId.ALARM_CODE_DATA).value().asString()
					+ "\nDcBusVoltage=" + this.channel(PytesJs3.ChannelId.DC_BUS_VOLTAGE).value().asString()
					+ "\nDcBusHalfVoltage=" + this.channel(PytesJs3.ChannelId.DC_BUS_HALF_VOLTAGE).value().asString()
					+ "\nVoltageL1=" + this.channel(PytesJs3.ChannelId.VOLTAGE_L1).value().asString()
					+ "\nVoltageL2=" + this.channel(PytesJs3.ChannelId.VOLTAGE_L2).value().asString()
					+ "\nVoltageL3=" + this.channel(PytesJs3.ChannelId.VOLTAGE_L3).value().asString()
					+ "\nCurrentL1=" + this.channel(PytesJs3.ChannelId.CURRENT_L1).value().asString()
					+ "\nCurrentL2=" + this.channel(PytesJs3.ChannelId.CURRENT_L2).value().asString()
					+ "\nCurrentL3=" + this.channel(PytesJs3.ChannelId.CURRENT_L3).value().asString()
					+ "\nActivePower=" + this.channel(SymmetricEss.ChannelId.ACTIVE_POWER).value().asString()
					+ "\nReactivePower=" + this.channel(SymmetricEss.ChannelId.REACTIVE_POWER).value().asString()
					+ "\nApparentPower=" + this.channel(PytesJs3.ChannelId.APPARENT_POWER).value().asString()
					+ "\nFrequency=" + this.channel(PytesJs3.ChannelId.FREQUENCY).value().asString()
					+ "\nInverterCurrentStatus=" + this.channel(PytesJs3.ChannelId.INVERTER_CURRENT_STATUS).value().asString()
					+ "\nLeadAcidBatteryTemp=" + this.channel(PytesJs3.ChannelId.LEAD_ACID_BATTERY_TEMP).value().asString()
					+ "\nFunctionStatus=" + this.channel(PytesJs3.ChannelId.FUNCTION_STATUS).value().asString()
					+ "\nCurrentDrmCodeStatus=" + this.channel(PytesJs3.ChannelId.CURRENT_DRM_CODE_STATUS).value().asString()
					+ "\nInverterCabinetTemp=" + this.channel(PytesJs3.ChannelId.INVERTER_CABINET_TEMP).value().asString()
					+ "\nLimitedPowerActualValue=" + this.channel(PytesJs3.ChannelId.LIMITED_POWER_ACTUAL_VALUE).value().asString()
					+ "\nPfAdjustmentActualValue=" + this.channel(PytesJs3.ChannelId.PF_ADJUSTMENT_ACTUAL_VALUE).value().asString()
					+ "\nLimitedReactivePower=" + this.channel(PytesJs3.ChannelId.LIMITED_REACTIVE_POWER).value().asString()
					+ "\nInverterModuleTemp2=" + this.channel(PytesJs3.ChannelId.INVERTER_MODULE_TEMP2).value().asString()
					+ "\nVoltVarVrefRtValues=" + this.channel(PytesJs3.ChannelId.VOLT_VAR_VREF_RT_VALUES).value().asString()
					+ "\nBmsChargingVoltageLimit=" + this.channel(PytesJs3.ChannelId.BMS_CHARGING_VOLTAGE_LIMIT).value().asString()
					+ "\nBatteryBmsStatus=" + this.channel(PytesJs3.ChannelId.BATTERY_BMS_STATUS).value().asString()
					+ "\nInverterInitialSettingState=" + this.channel(PytesJs3.ChannelId.INVERTER_INITIAL_SETTING_STATE).value().asString()
					+ "\nBatchUpgradeBowl=" + this.channel(PytesJs3.ChannelId.BATCH_UPGRADE_BOWL).value().asString()
					+ "\nFcasModeRunningStatus=" + this.channel(PytesJs3.ChannelId.FCAS_MODE_RUNNING_STATUS).value().asString()
					+ "\nSettingFlagBit=" + this.channel(PytesJs3.ChannelId.SETTING_FLAG_BIT).value().asString()
					+ "\nFaultCode01=" + this.channel(PytesJs3.ChannelId.FAULT_CODE_01).value().asString()
					+ "\nFaultCode02=" + this.channel(PytesJs3.ChannelId.FAULT_CODE_02).value().asString()
					+ "\nFaultCode03=" + this.channel(PytesJs3.ChannelId.FAULT_CODE_03).value().asString()
					+ "\nFaultCode04=" + this.channel(PytesJs3.ChannelId.FAULT_CODE_04).value().asString()
					+ "\nFaultCode05=" + this.channel(PytesJs3.ChannelId.FAULT_CODE_05).value().asString()
					+ "\nOperatingStatus=" + this.channel(PytesJs3.ChannelId.OPERATING_STATUS).value().asString()
					+ "\nOperatingMode=" + this.channel(PytesJs3.ChannelId.OPERATING_MODE).value().asString()
					+ "\nWorkingModeRunningStatus=" + this.channel(PytesJs3.ChannelId.WORKING_MODE_RUNNING_STATUS).value().asString()
					+ "\nFaultCode06=" + this.channel(PytesJs3.ChannelId.FAULT_CODE_06).value().asString()
					+ "\nFaultCode07=" + this.channel(PytesJs3.ChannelId.FAULT_CODE_07).value().asString()
					+ "\nStorageControlSwitchingValue=" + this.channel(PytesJs3.ChannelId.STORAGE_CONTROL_SWITCHING_VALUE).value().asString()					
					
					
					;			
			
		}
		else {
			return "SoC:" + this.getSoc().asString() //
					+ "|L:" + this.getActivePower().asString();
		}
	}
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String getModbusBridgeId() {
		return this.config.modbus_id();
	}

	@Override
	public void addCharger(PytesDcCharger charger) {
		this.charger = charger;
		this.setPowerHandlers();
	}

	@Override
	public void removeCharger(PytesDcCharger charger) {
		if (this.charger == charger) {
			this.charger = null;
		}
		this.setPowerHandlers();
	}

	@Override
	public void addBattery(PytesBattery battery) {
		this.battery = battery;
		this.setPowerHandlers();
	}

	@Override
	public void removeBattery(PytesBattery battery) {
		if (this.battery == battery) {
			this.battery = null;
		}
		this.setPowerHandlers();
	}

	@Override
	public void retryModbusCommunication() {
		// TODO Auto-generated method stub

	}

	@Override
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) throws OpenemsNamedException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void applyPower(int targetActivePower, int reactivePower) throws OpenemsNamedException {

		this.logDebug(this.log,"\n\n applyPower called by {} with {} W");
		// AC 1/28/2024
		// IntegerWriteChannel setGridLoadOffPowerChannel =
		// this.channel(DeyeSunHybrid.ChannelId.SET_GRID_LOAD_OFF_POWER);
		// setGridLoadOffPowerChannel.setNextWriteValue(93);

		this._setMaxApparentPower(this.MAX_APPARENT_POWER);
		

		
		if (this.battery == null) {
			this.applyPowerHandler = null;

			return;
		}
		
		
		int setActivePowerValue = (int) Math.round(targetActivePower / 10.0);		
		
		this.setRemoteDispatchSwitch(1);
		this.setRemoteDispatchTimeout(5);
		this.setRemoteDispatchSystemLimitSwitch(0);
		this.setRemoteDispatchSystemImportLimit(150);
		this.setRemoteDispatchSystemExportLimit(150);
		this.setRemoteDispatchRealtimeControlSwitch(3); // Set grid connection point
		this.setRemoteDispatchRealtimeControlPower(setActivePowerValue);
		
		
		
		
		
		
		
		logDebug(this.log, "ApplyPower: ActivePowerTarget = " + targetActivePower);
/*
		if (this.applyPowerHandler != null) {
			this.applyPowerHandler.apply(activePower, reactivePower, this.config.maxApparentPower());
		}
*/
		
	}
	
	/**
	 * Uses Info Log for further debug features.
	 */
	@Override
	protected void logDebug(Logger log, String message) {
		if (this.config.debugMode()) {

			this.logInfo(log, message);

		}
	}
	
	public Logger getLogger() {
		return this.log;
	}
	
	
	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {

		return 10;
	}

	@Override
	public Integer getSurplusPower() {
		// TODO Auto-generated method stub
		return null;
	}

	private void setPowerHandlers() {
		  if (this.battery != null && this.charger != null) {
		    this.applyPowerHandler = new ApplyPowerHandler(this, this.battery, this.charger);
		    this.allowedChargeDischargeHandler = new AllowedChargeDischargeHandler(this, this.battery, this.charger);
		  } else {
		    this.applyPowerHandler = null;
		    this.allowedChargeDischargeHandler = null;
		  }
		}

	@Override
	public int getCycleTime() {
		return this.cycle != null ? this.cycle.getCycleTime() : DEFAULT_CYCLE_TIME;
	}	
	
	@Override
	public boolean isManaged() {
		return !this.config.readOnlyMode();
	}
}
