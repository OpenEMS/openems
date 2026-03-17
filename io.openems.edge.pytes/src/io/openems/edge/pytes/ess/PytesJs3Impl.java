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
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
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
			Integer dcDischargePower = this.battery.getDcDischargePower().get();
			this._setDcDischargePower(dcDischargePower);
			
			this.logDebug(this.log, "DcDischargePower: " + dcDischargePower + "W");
			
			if (this.allowedChargeDischargeHandler != null) {
				this.allowedChargeDischargeHandler.accept(this.componentManager);
			}
			this.decodeFaultBits();
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
				
				
				new FC3ReadRegistersTask(44100, Priority.HIGH,
						m(PytesJs3.ChannelId.REMOTE_DISPATCH_SWITCH, new UnsignedWordElement(44100)),
						m(PytesJs3.ChannelId.REMOTE_DISPATCH_TIMEOUT, new UnsignedWordElement(44101)),
						m(PytesJs3.ChannelId.REMOTE_DISPATCH_SYSTEM_LIMIT_SWITCH, new UnsignedWordElement(44102)),
						m(PytesJs3.ChannelId.REMOTE_DISPATCH_SYSTEM_IMPORT_LIMIT, new UnsignedWordElement(44103)),
						m(PytesJs3.ChannelId.REMOTE_DISPATCH_SYSTEM_EXPORT_LIMIT, new UnsignedWordElement(44104)),
						m(PytesJs3.ChannelId.REMOTE_DISPATCH_REALTIME_CONTROL_SWITCH, new UnsignedWordElement(44105)),
						m(PytesJs3.ChannelId.REMOTE_DISPATCH_REALTIME_CONTROL_POWER, new SignedDoublewordElement(44106)),
						m(PytesJs3.ChannelId.REMOTE_DISPATCH_REALTIME_CONTROL_FUNCTION_SWITCH, new UnsignedWordElement(44108))),


				new FC4ReadInputRegistersTask(33067, Priority.HIGH, //

						m(SymmetricEss.ChannelId.MAX_APPARENT_POWER, new UnsignedWordElement(33067),
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

						m(PytesJs3.ChannelId.SETTING_FLAG_BIT, new UnsignedWordElement(33115)),

						// Fault Code (01..07)
						m(PytesJs3.ChannelId.FAULT_CODE_01, new UnsignedWordElement(33116)),

						m(PytesJs3.ChannelId.FAULT_CODE_02, new UnsignedWordElement(33117)),

						m(PytesJs3.ChannelId.FAULT_CODE_03, new UnsignedWordElement(33118)),

						m(PytesJs3.ChannelId.FAULT_CODE_04, new UnsignedWordElement(33119)),

						m(PytesJs3.ChannelId.FAULT_CODE_05, new UnsignedWordElement(33120)),
						
						m(PytesJs3.ChannelId.OPERATING_STATUS, new UnsignedWordElement(33121)),

						m(PytesJs3.ChannelId.OPERATING_MODE, new UnsignedWordElement(33122)),

						m(PytesJs3.ChannelId.WORKING_MODE_RUNNING_STATUS, new UnsignedWordElement(33123),
								ElementToChannelConverter.SCALE_FACTOR_2),						

						m(PytesJs3.ChannelId.FAULT_CODE_06, new UnsignedWordElement(33124)),

						m(PytesJs3.ChannelId.FAULT_CODE_07, new UnsignedWordElement(33125)),

						new DummyRegisterElement(33126, 33131),	

						m(PytesJs3.ChannelId.STORAGE_CONTROL_SWITCHING_VALUE, new UnsignedWordElement(33132))

				));

	}

	@Override
	public String debugLog() {
		if (config.debugMode()) {
			return "SoC:" + this.getSoc().asString() //
					+ "|L:" + this.getActivePower().asString()
					+ this.channel(SymmetricEss.ChannelId.REACTIVE_POWER).value().asString()
					+ "\nMaxApparentPower=" + this.channel(SymmetricEss.ChannelId.MAX_APPARENT_POWER).value().asString()
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
					+ "\nInverterCurrentStatus=" + this.channel(PytesJs3.ChannelId.INVERTER_CURRENT_STATUS).value().asString()
					+ "\nOperatingMode=" + this.channel(PytesJs3.ChannelId.OPERATING_MODE).value().asString()
					+ "\nFrequency=" + this.channel(PytesJs3.ChannelId.FREQUENCY).value().asString()
					
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
					
					+ "\nWorkingModeRunningStatus=" + this.channel(PytesJs3.ChannelId.WORKING_MODE_RUNNING_STATUS).value().asString()
					+ "\nFaultCode06=" + this.channel(PytesJs3.ChannelId.FAULT_CODE_06).value().asString()
					+ "\nFaultCode07=" + this.channel(PytesJs3.ChannelId.FAULT_CODE_07).value().asString()
					+ "\nStorageControlSwitchingValue=" + this.channel(PytesJs3.ChannelId.STORAGE_CONTROL_SWITCHING_VALUE).value().asString()
					
					// Appendix 4 decoded fault bits  REG1 (33116)
					+ "\nFaultReg1_NoGrid=" + this.channel(PytesJs3.ChannelId.FAULT_REG1_NO_GRID).value().asString()
					+ "\nFaultReg1_GridOvVoltage=" + this.channel(PytesJs3.ChannelId.FAULT_REG1_GRID_OVERVOLTAGE).value().asString()
					+ "\nFaultReg1_GridUnVoltage=" + this.channel(PytesJs3.ChannelId.FAULT_REG1_GRID_UNDERVOLTAGE).value().asString()
					+ "\nFaultReg1_GridOverFreq=" + this.channel(PytesJs3.ChannelId.FAULT_REG1_GRID_OVERFREQ).value().asString()
					+ "\nFaultReg1_GridUnderFreq=" + this.channel(PytesJs3.ChannelId.FAULT_REG1_GRID_UNDERFREQ).value().asString()
					+ "\nFaultReg1_UnbalancedGrid=" + this.channel(PytesJs3.ChannelId.FAULT_REG1_UNBALANCED_GRID).value().asString()
					+ "\nFaultReg1_FreqFluctuation=" + this.channel(PytesJs3.ChannelId.FAULT_REG1_GRID_FREQ_FLUCTUATION).value().asString()
					+ "\nFaultReg1_ReverseCurrent=" + this.channel(PytesJs3.ChannelId.FAULT_REG1_GRID_REVERSE_CURRENT).value().asString()
					+ "\nFaultReg1_CurrentTrackErr=" + this.channel(PytesJs3.ChannelId.FAULT_REG1_GRID_CURRENT_TRACKING_ERROR).value().asString()
					+ "\nFaultReg1_MeterComFail=" + this.channel(PytesJs3.ChannelId.FAULT_REG1_METER_COM_FAIL).value().asString()
					+ "\nFaultReg1_FailSafe=" + this.channel(PytesJs3.ChannelId.FAULT_REG1_FAILSAFE).value().asString()
					+ "\nFaultReg1_MeterSelFail=" + this.channel(PytesJs3.ChannelId.FAULT_REG1_METER_SELECT_FAIL).value().asString()
					+ "\nFaultReg1_EpmHardLimit=" + this.channel(PytesJs3.ChannelId.FAULT_REG1_EPM_HARD_LIMIT).value().asString()
					+ "\nFaultReg1_G100OvrLimit=" + this.channel(PytesJs3.ChannelId.FAULT_REG1_G100_CURRENT_OVER_LIMIT).value().asString()
					+ "\nFaultReg1_AbnGridPhase=" + this.channel(PytesJs3.ChannelId.FAULT_REG1_ABNORMAL_GRID_PHASE_POLARITY).value().asString()
					
					// REG2 (33117)  Backup / Hub faults
					+ "\nFaultReg2_BackupOvVolt=" + this.channel(PytesJs3.ChannelId.FAULT_REG2_BACKUP_OVERVOLTAGE).value().asString()
					+ "\nFaultReg2_BackupOverload=" + this.channel(PytesJs3.ChannelId.FAULT_REG2_BACKUP_OVERLOAD).value().asString()
					+ "\nFaultReg2_GridBackupOverload=" + this.channel(PytesJs3.ChannelId.FAULT_REG2_GRID_BACKUP_OVERLOAD).value().asString()
					+ "\nFaultReg2_OffgridBackupUnVolt=" + this.channel(PytesJs3.ChannelId.FAULT_REG2_OFFGRID_BACKUP_UNDERVOLTAGE).value().asString()
					+ "\nFaultReg2_HubPanelOvCurrent=" + this.channel(PytesJs3.ChannelId.FAULT_REG2_HUB_PANEL_OV_CURRENT).value().asString()
					
					// REG3 (33118)  Battery faults
					+ "\nFaultReg3_BattNotConnected=" + this.channel(PytesJs3.ChannelId.FAULT_REG3_BATTERY_NOT_CONNECTED).value().asString()
					+ "\nFaultReg3_BattOvVoltCheck=" + this.channel(PytesJs3.ChannelId.FAULT_REG3_BATTERY_OVERVOLTAGE_CHECK).value().asString()
					+ "\nFaultReg3_BattUnVoltCheck=" + this.channel(PytesJs3.ChannelId.FAULT_REG3_BATTERY_UNDERVOLTAGE_CHECK).value().asString()
					+ "\nFaultReg3_BattBmsAlarm=" + this.channel(PytesJs3.ChannelId.FAULT_REG3_BATTERY_BMS_ALARM).value().asString()
					+ "\nFaultReg3_InconsistBattSel=" + this.channel(PytesJs3.ChannelId.FAULT_REG3_INCONSISTENT_BATTERY_SELECTION).value().asString()
					+ "\nFaultReg3_LeadAcidTempLow=" + this.channel(PytesJs3.ChannelId.FAULT_REG3_LEAD_ACID_TEMP_TOO_LOW).value().asString()
					+ "\nFaultReg3_LeadAcidTempHigh=" + this.channel(PytesJs3.ChannelId.FAULT_REG3_LEAD_ACID_TEMP_TOO_HIGH).value().asString()
					+ "\nFaultReg3_2ndBattNotConn=" + this.channel(PytesJs3.ChannelId.FAULT_REG3_SECOND_BATTERY_NOT_CONNECTED).value().asString()
					+ "\nFaultReg3_2ndBattSwOvVolt=" + this.channel(PytesJs3.ChannelId.FAULT_REG3_SECOND_BATTERY_SW_OVERVOLTAGE).value().asString()
					+ "\nFaultReg3_2ndBattSwUnVolt=" + this.channel(PytesJs3.ChannelId.FAULT_REG3_SECOND_BATTERY_SW_UNDERVOLTAGE).value().asString()
					+ "\nFaultReg3_ParallelBattComAbn=" + this.channel(PytesJs3.ChannelId.FAULT_REG3_PARALLEL_BATTERY_COM_ABNORMAL).value().asString()
					+ "\nFaultReg3_LowBattOffgrid=" + this.channel(PytesJs3.ChannelId.FAULT_REG3_LOW_BATTERY_OFFGRID).value().asString()
					
					// REG4 (33119)  DC / IGBT / AFCI faults
					+ "\nFaultReg4_DcOvVolt=" + this.channel(PytesJs3.ChannelId.FAULT_REG4_DC_OVERVOLTAGE).value().asString()
					+ "\nFaultReg4_DcBusOvVolt=" + this.channel(PytesJs3.ChannelId.FAULT_REG4_DC_BUS_OVERVOLTAGE).value().asString()
					+ "\nFaultReg4_DcBusUnbalanced=" + this.channel(PytesJs3.ChannelId.FAULT_REG4_DC_BUS_UNBALANCED_VOLTAGE).value().asString()
					+ "\nFaultReg4_DcBusUnVolt=" + this.channel(PytesJs3.ChannelId.FAULT_REG4_DC_BUS_UNDERVOLTAGE).value().asString()
					+ "\nFaultReg4_DcBusUnbalanced2=" + this.channel(PytesJs3.ChannelId.FAULT_REG4_DC_BUS_UNBALANCED_VOLTAGE_2).value().asString()
					+ "\nFaultReg4_DcOvCurrentA=" + this.channel(PytesJs3.ChannelId.FAULT_REG4_DC_OVERCURRENT_A).value().asString()
					+ "\nFaultReg4_DcOvCurrentB=" + this.channel(PytesJs3.ChannelId.FAULT_REG4_DC_OVERCURRENT_B).value().asString()
					+ "\nFaultReg4_DcInputInterf=" + this.channel(PytesJs3.ChannelId.FAULT_REG4_DC_INPUT_INTERFERENCE).value().asString()
					+ "\nFaultReg4_GridOvCurrent=" + this.channel(PytesJs3.ChannelId.FAULT_REG4_GRID_OVERCURRENT).value().asString()
					+ "\nFaultReg4_IgbtOvCurrent=" + this.channel(PytesJs3.ChannelId.FAULT_REG4_IGBT_OVERCURRENT).value().asString()
					+ "\nFaultReg4_GridInterf02=" + this.channel(PytesJs3.ChannelId.FAULT_REG4_GRID_INTERFERENCE_02).value().asString()
					+ "\nFaultReg4_AfciSelfCheck=" + this.channel(PytesJs3.ChannelId.FAULT_REG4_AFCI_SELF_CHECK).value().asString()
					+ "\nFaultReg4_GridCurrSampFault=" + this.channel(PytesJs3.ChannelId.FAULT_REG4_GRID_CURRENT_SAMPLING_FAULT).value().asString()
					+ "\nFaultReg4_DspSelfCheckErr=" + this.channel(PytesJs3.ChannelId.FAULT_REG4_DSP_SELF_CHECK_ERROR).value().asString()
					+ "\nFaultReg4_BattDischargeOvCurr=" + this.channel(PytesJs3.ChannelId.FAULT_REG4_BATTERY_DISCHARGE_OVERCURRENT).value().asString()
					
					// REG5 (33120) Protection faults
					+ "\nFaultReg5_GridInterf=" + this.channel(PytesJs3.ChannelId.FAULT_REG5_GRID_INTERFERENCE).value().asString()
					+ "\nFaultReg5_OverDcComponents=" + this.channel(PytesJs3.ChannelId.FAULT_REG5_OVER_DC_COMPONENTS).value().asString()
					+ "\nFaultReg5_OverTemp=" + this.channel(PytesJs3.ChannelId.FAULT_REG5_OVER_TEMPERATURE).value().asString()
					+ "\nFaultReg5_RelayCheck=" + this.channel(PytesJs3.ChannelId.FAULT_REG5_RELAY_CHECK).value().asString()
					+ "\nFaultReg5_UnderTemp=" + this.channel(PytesJs3.ChannelId.FAULT_REG5_UNDER_TEMPERATURE).value().asString()
					+ "\nFaultReg5_PvInsulFault=" + this.channel(PytesJs3.ChannelId.FAULT_REG5_PV_INSULATION_FAULT).value().asString()
					+ "\nFaultReg5_12vUnVolt=" + this.channel(PytesJs3.ChannelId.FAULT_REG5_12V_UNDERVOLTAGE).value().asString()
					+ "\nFaultReg5_LeakCurrent=" + this.channel(PytesJs3.ChannelId.FAULT_REG5_LEAK_CURRENT).value().asString()
					+ "\nFaultReg5_LeakCurrSelfChk=" + this.channel(PytesJs3.ChannelId.FAULT_REG5_LEAK_CURRENT_SELF_CHECK).value().asString()
					+ "\nFaultReg5_DspInitial=" + this.channel(PytesJs3.ChannelId.FAULT_REG5_DSP_INITIAL).value().asString()
					+ "\nFaultReg5_DspB=" + this.channel(PytesJs3.ChannelId.FAULT_REG5_DSP_B).value().asString()
					+ "\nFaultReg5_BattOvVoltHw=" + this.channel(PytesJs3.ChannelId.FAULT_REG5_BATTERY_OVERVOLTAGE_HW).value().asString()
					+ "\nFaultReg5_LlcHwOvCurr=" + this.channel(PytesJs3.ChannelId.FAULT_REG5_LLC_HW_OVERCURRENT).value().asString()
					+ "\nFaultReg5_GridTransientOvCurr=" + this.channel(PytesJs3.ChannelId.FAULT_REG5_GRID_TRANSIENT_OVERCURRENT).value().asString()
					+ "\nFaultReg5_BattComFail=" + this.channel(PytesJs3.ChannelId.FAULT_REG5_BATTERY_COM_FAILURE).value().asString()
					+ "\nFaultReg5_DspComFail=" + this.channel(PytesJs3.ChannelId.FAULT_REG5_DSP_COM_FAIL).value().asString()
					
					// REG6 (33124)  Parallel / multi-unit faults
					+ "\nFaultReg6_SlaveLoseErr=" + this.channel(PytesJs3.ChannelId.FAULT_REG6_SLAVE_LOSE_ERR).value().asString()
					+ "\nFaultReg6_MasterLoseErr=" + this.channel(PytesJs3.ChannelId.FAULT_REG6_MASTER_LOSE_ERR).value().asString()
					+ "\nFaultReg6_SlavePrdErr=" + this.channel(PytesJs3.ChannelId.FAULT_REG6_SLAVE_PRD_ERR).value().asString()
					+ "\nFaultReg6_MasterPrdErr=" + this.channel(PytesJs3.ChannelId.FAULT_REG6_MASTER_PRD_ERR).value().asString()
					+ "\nFaultReg6_AddrConflict=" + this.channel(PytesJs3.ChannelId.FAULT_REG6_ADDR_CONFLICT).value().asString()
					+ "\nFaultReg6_HeartbeatLose=" + this.channel(PytesJs3.ChannelId.FAULT_REG6_HEARTBEAT_LOSE).value().asString()
					+ "\nFaultReg6_DcanErr=" + this.channel(PytesJs3.ChannelId.FAULT_REG6_DCAN_ERR).value().asString()
					+ "\nFaultReg6_MulMasterErr=" + this.channel(PytesJs3.ChannelId.FAULT_REG6_MUL_MASTER_ERR).value().asString()
					+ "\nFaultReg6_ModeConflict=" + this.channel(PytesJs3.ChannelId.FAULT_REG6_MODE_CONFLICT).value().asString()
					+ "\nFaultReg6_SPlugVoltErr=" + this.channel(PytesJs3.ChannelId.FAULT_REG6_S_PLUG_VOLT_ERR).value().asString()
					+ "\nFaultReg6_OthersFault=" + this.channel(PytesJs3.ChannelId.FAULT_REG6_OTHERS_FAULT).value().asString()
					+ "\nFaultReg6_CanBusLose=" + this.channel(PytesJs3.ChannelId.FAULT_REG6_CAN_BUS_LOSE).value().asString()
					+ "\nFaultReg6_ModelMismatch=" + this.channel(PytesJs3.ChannelId.FAULT_REG6_MODEL_MISMATCH).value().asString()
					+ "\nFaultReg6_3pCreateFail=" + this.channel(PytesJs3.ChannelId.FAULT_REG6_3P_CREATE_FAIL).value().asString()
					+ "\nFaultReg6_AcbkOpen=" + this.channel(PytesJs3.ChannelId.FAULT_REG6_ACBK_OPEN).value().asString()
					
					// REG7 (33125)  Hardware / startup faults
					+ "\nFaultReg7_ReveDc=" + this.channel(PytesJs3.ChannelId.FAULT_REG7_REVE_DC).value().asString()
					+ "\nFaultReg7_BattHwOvVolt02=" + this.channel(PytesJs3.ChannelId.FAULT_REG7_BATTERY_HW_OVERVOLTAGE_02).value().asString()
					+ "\nFaultReg7_BattHwOvCurr=" + this.channel(PytesJs3.ChannelId.FAULT_REG7_BATTERY_HW_OVERCURRENT).value().asString()
					+ "\nFaultReg7_BusMidpointHwOvCurr=" + this.channel(PytesJs3.ChannelId.FAULT_REG7_BUS_MIDPOINT_HW_OVERCURRENT).value().asString()
					+ "\nFaultReg7_BattStartupFail=" + this.channel(PytesJs3.ChannelId.FAULT_REG7_BATTERY_STARTUP_FAIL).value().asString()
					+ "\nFaultReg7_Dc3AvgOvCurr=" + this.channel(PytesJs3.ChannelId.FAULT_REG7_DC3_AVG_OVERCURRENT).value().asString()
					+ "\nFaultReg7_Dc4AvgOvCurr=" + this.channel(PytesJs3.ChannelId.FAULT_REG7_DC4_AVG_OVERCURRENT).value().asString()
					+ "\nFaultReg7_SoftrunTimeout=" + this.channel(PytesJs3.ChannelId.FAULT_REG7_SOFTRUN_TIMEOUT).value().asString()
					+ "\nFaultReg7_OffgridToGridTimeout=" + this.channel(PytesJs3.ChannelId.FAULT_REG7_OFFGRID_TO_GRID_TIMEOUT).value().asString()
					+ "\nFaultReg7_DrmNotConnect=" + this.channel(PytesJs3.ChannelId.FAULT_REG7_DRM_NOT_CONNECT).value().asString()
					
					// Appendix 5  Operating Status decoded bits (33121)
					+ "\nOperatStat_NormalOp=" + this.channel(PytesJs3.ChannelId.OPERATING_STAT_NORMAL_OPERATION).value().asString()
					+ "\nOperatStat_Initializing=" + this.channel(PytesJs3.ChannelId.OPERATING_STAT_INITIALIZING).value().asString()
					+ "\nOperatStat_ControlledOff=" + this.channel(PytesJs3.ChannelId.OPERATING_STAT_CONTROLLED_OFF).value().asString()
					+ "\nOperatStat_FaultOff=" + this.channel(PytesJs3.ChannelId.OPERATING_STAT_FAULT_OFF).value().asString()
					+ "\nOperatStat_Standby=" + this.channel(PytesJs3.ChannelId.OPERATING_STAT_STANDBY).value().asString()
					+ "\nOperatStat_LimitedTempFreq=" + this.channel(PytesJs3.ChannelId.OPERATING_STAT_LIMITED_TEMP_FREQ).value().asString()
					+ "\nOperatStat_LimitedExternal=" + this.channel(PytesJs3.ChannelId.OPERATING_STAT_LIMITED_EXTERNAL).value().asString()
					+ "\nOperatStat_BackupOverload=" + this.channel(PytesJs3.ChannelId.OPERATING_STAT_BACKUP_OVERLOAD).value().asString()
					+ "\nOperatStat_LoadFault=" + this.channel(PytesJs3.ChannelId.OPERATING_STAT_LOAD_FAULT).value().asString()
					+ "\nOperatStat_GridFault=" + this.channel(PytesJs3.ChannelId.OPERATING_STAT_GRID_FAULT).value().asString()
					+ "\nOperatStat_BatteryFault=" + this.channel(PytesJs3.ChannelId.OPERATING_STAT_BATTERY_FAULT).value().asString()
					+ "\nOperatStat_GridSurgeWarn=" + this.channel(PytesJs3.ChannelId.OPERATING_STAT_GRID_SURGE_WARN).value().asString()
					+ "\nOperatStat_FanFaultWarn=" + this.channel(PytesJs3.ChannelId.OPERATING_STAT_FAN_FAULT_WARN).value().asString()
					+ "\nOperatStat_ExternalFanFail=" + this.channel(PytesJs3.ChannelId.OPERATING_STAT_EXTERNAL_FAN_FAIL).value().asString()
					
					// Appendix 6 Storage Control decoded bits (33132)
					+ "\nStorageCtrl_SelfUse=" + this.channel(PytesJs3.ChannelId.STORAGE_CTRL_SELF_USE_MODE).value().asString()
					+ "\nStorageCtrl_TimeOfUse=" + this.channel(PytesJs3.ChannelId.STORAGE_CTRL_TIME_OF_USE_MODE).value().asString()
					+ "\nStorageCtrl_OffGrid=" + this.channel(PytesJs3.ChannelId.STORAGE_CTRL_OFFGRID_MODE).value().asString()
					+ "\nStorageCtrl_BattWakeup=" + this.channel(PytesJs3.ChannelId.STORAGE_CTRL_BATT_WAKEUP).value().asString()
					+ "\nStorageCtrl_ReserveBatt=" + this.channel(PytesJs3.ChannelId.STORAGE_CTRL_RESERVE_BATT_MODE).value().asString()
					+ "\nStorageCtrl_AllowGridChg=" + this.channel(PytesJs3.ChannelId.STORAGE_CTRL_ALLOW_GRID_CHARGE).value().asString()
					+ "\nStorageCtrl_FeedInPriority=" + this.channel(PytesJs3.ChannelId.STORAGE_CTRL_FEED_IN_PRIORITY).value().asString()
					+ "\nStorageCtrl_BattOvc=" + this.channel(PytesJs3.ChannelId.STORAGE_CTRL_BATT_OVC).value().asString()
					+ "\nStorageCtrl_ForceChargePeak=" + this.channel(PytesJs3.ChannelId.STORAGE_CTRL_FORCE_CHARGE_PEAKSHAVING).value().asString()
					+ "\nStorageCtrl_BattCurrCorrect=" + this.channel(PytesJs3.ChannelId.STORAGE_CTRL_BATT_CURRENT_CORRECTION).value().asString()
					+ "\nStorageCtrl_BattHealing=" + this.channel(PytesJs3.ChannelId.STORAGE_CTRL_BATT_HEALING_MODE).value().asString()
					+ "\nStorageCtrl_PeakShaving=" + this.channel(PytesJs3.ChannelId.STORAGE_CTRL_PEAK_SHAVING_MODE).value().asString()

					// Appendix 7  Setting Flag Bit decoded bits (33115)
					+ "\nSettingFlag_FlashTimeout=" + this.channel(PytesJs3.ChannelId.SETTING_FLAG_FLASH_TIMEOUT).value().asString()
					+ "\nSettingFlag_ClearEnergy=" + this.channel(PytesJs3.ChannelId.SETTING_FLAG_CLEAR_ENERGY).value().asString()
					+ "\nSettingFlag_ResetDatalogger=" + this.channel(PytesJs3.ChannelId.SETTING_FLAG_RESET_DATALOGGER).value().asString()
					+ "\nSettingFlag_FactoryRecover=" + this.channel(PytesJs3.ChannelId.SETTING_FLAG_FACTORY_RECOVER).value().asString()
					
					+ "\nOperatingModeDecoded=" + this.channel(PytesJs3.ChannelId.OPERATING_MODE_DECODE).value().asString()
					
					
					;			
			
		}
		else {
			return 	"|SoC:" + this.getSoc().asString() //
					+ "|L:" + this.getActivePower().asString() //
					+ "|DcDischarge:" + this.getDcDischargePower().asString() 
					+ "|Allowed:" + this.getAllowedChargePower().asString() + ";"
					+ this.getAllowedDischargePower().asString(); //
		}
	}
	
	
	/**
	 * Decodes the 7 Appendix-4 fault bitmask registers into individual boolean channels.
	 * Called every cycle from handleEvent().
	 */
	private void decodeFaultBits() {
		decodeBits(PytesJs3.ChannelId.FAULT_CODE_01, new PytesJs3.ChannelId[]{
			PytesJs3.ChannelId.FAULT_REG1_NO_GRID,
			PytesJs3.ChannelId.FAULT_REG1_GRID_OVERVOLTAGE,
			PytesJs3.ChannelId.FAULT_REG1_GRID_UNDERVOLTAGE,
			PytesJs3.ChannelId.FAULT_REG1_GRID_OVERFREQ,
			PytesJs3.ChannelId.FAULT_REG1_GRID_UNDERFREQ,
			PytesJs3.ChannelId.FAULT_REG1_UNBALANCED_GRID,
			PytesJs3.ChannelId.FAULT_REG1_GRID_FREQ_FLUCTUATION,
			PytesJs3.ChannelId.FAULT_REG1_GRID_REVERSE_CURRENT,
			PytesJs3.ChannelId.FAULT_REG1_GRID_CURRENT_TRACKING_ERROR,
			PytesJs3.ChannelId.FAULT_REG1_METER_COM_FAIL,
			PytesJs3.ChannelId.FAULT_REG1_FAILSAFE,
			PytesJs3.ChannelId.FAULT_REG1_METER_SELECT_FAIL,
			PytesJs3.ChannelId.FAULT_REG1_EPM_HARD_LIMIT,
			PytesJs3.ChannelId.FAULT_REG1_G100_CURRENT_OVER_LIMIT,
			PytesJs3.ChannelId.FAULT_REG1_RESERVED_14,
			PytesJs3.ChannelId.FAULT_REG1_ABNORMAL_GRID_PHASE_POLARITY
		});
		decodeBits(PytesJs3.ChannelId.FAULT_CODE_02, new PytesJs3.ChannelId[]{
			PytesJs3.ChannelId.FAULT_REG2_BACKUP_OVERVOLTAGE,
			PytesJs3.ChannelId.FAULT_REG2_BACKUP_OVERLOAD,
			PytesJs3.ChannelId.FAULT_REG2_GRID_BACKUP_OVERLOAD,
			PytesJs3.ChannelId.FAULT_REG2_OFFGRID_BACKUP_UNDERVOLTAGE,
			PytesJs3.ChannelId.FAULT_REG2_HUB_PANEL_OV_CURRENT,
			PytesJs3.ChannelId.FAULT_REG2_RESERVED_05,
			PytesJs3.ChannelId.FAULT_REG2_RESERVED_06,
			PytesJs3.ChannelId.FAULT_REG2_RESERVED_07,
			PytesJs3.ChannelId.FAULT_REG2_RESERVED_08,
			PytesJs3.ChannelId.FAULT_REG2_RESERVED_09,
			PytesJs3.ChannelId.FAULT_REG2_RESERVED_10,
			PytesJs3.ChannelId.FAULT_REG2_RESERVED_11,
			PytesJs3.ChannelId.FAULT_REG2_RESERVED_12,
			PytesJs3.ChannelId.FAULT_REG2_RESERVED_13,
			PytesJs3.ChannelId.FAULT_REG2_RESERVED_14,
			PytesJs3.ChannelId.FAULT_REG2_RESERVED_15
		});
		decodeBits(PytesJs3.ChannelId.FAULT_CODE_03, new PytesJs3.ChannelId[]{
			PytesJs3.ChannelId.FAULT_REG3_BATTERY_NOT_CONNECTED,
			PytesJs3.ChannelId.FAULT_REG3_BATTERY_OVERVOLTAGE_CHECK,
			PytesJs3.ChannelId.FAULT_REG3_BATTERY_UNDERVOLTAGE_CHECK,
			PytesJs3.ChannelId.FAULT_REG3_BATTERY_BMS_ALARM,
			PytesJs3.ChannelId.FAULT_REG3_INCONSISTENT_BATTERY_SELECTION,
			PytesJs3.ChannelId.FAULT_REG3_LEAD_ACID_TEMP_TOO_LOW,
			PytesJs3.ChannelId.FAULT_REG3_LEAD_ACID_TEMP_TOO_HIGH,
			PytesJs3.ChannelId.FAULT_REG3_SECOND_BATTERY_NOT_CONNECTED,
			PytesJs3.ChannelId.FAULT_REG3_SECOND_BATTERY_SW_OVERVOLTAGE,
			PytesJs3.ChannelId.FAULT_REG3_SECOND_BATTERY_SW_UNDERVOLTAGE,
			PytesJs3.ChannelId.FAULT_REG3_PARALLEL_BATTERY_COM_ABNORMAL,
			PytesJs3.ChannelId.FAULT_REG3_LOW_BATTERY_OFFGRID,
			PytesJs3.ChannelId.FAULT_REG3_RESERVED_12,
			PytesJs3.ChannelId.FAULT_REG3_RESERVED_13,
			PytesJs3.ChannelId.FAULT_REG3_RESERVED_14,
			PytesJs3.ChannelId.FAULT_REG3_RESERVED_15
		});
		decodeBits(PytesJs3.ChannelId.FAULT_CODE_04, new PytesJs3.ChannelId[]{
			PytesJs3.ChannelId.FAULT_REG4_DC_OVERVOLTAGE,
			PytesJs3.ChannelId.FAULT_REG4_DC_BUS_OVERVOLTAGE,
			PytesJs3.ChannelId.FAULT_REG4_DC_BUS_UNBALANCED_VOLTAGE,
			PytesJs3.ChannelId.FAULT_REG4_DC_BUS_UNDERVOLTAGE,
			PytesJs3.ChannelId.FAULT_REG4_DC_BUS_UNBALANCED_VOLTAGE_2,
			PytesJs3.ChannelId.FAULT_REG4_DC_OVERCURRENT_A,
			PytesJs3.ChannelId.FAULT_REG4_DC_OVERCURRENT_B,
			PytesJs3.ChannelId.FAULT_REG4_DC_INPUT_INTERFERENCE,
			PytesJs3.ChannelId.FAULT_REG4_GRID_OVERCURRENT,
			PytesJs3.ChannelId.FAULT_REG4_IGBT_OVERCURRENT,
			PytesJs3.ChannelId.FAULT_REG4_GRID_INTERFERENCE_02,
			PytesJs3.ChannelId.FAULT_REG4_AFCI_SELF_CHECK,
			PytesJs3.ChannelId.FAULT_REG4_ARC_FAULT_RESERVED,
			PytesJs3.ChannelId.FAULT_REG4_GRID_CURRENT_SAMPLING_FAULT,
			PytesJs3.ChannelId.FAULT_REG4_DSP_SELF_CHECK_ERROR,
			PytesJs3.ChannelId.FAULT_REG4_BATTERY_DISCHARGE_OVERCURRENT
		});
		decodeBits(PytesJs3.ChannelId.FAULT_CODE_05, new PytesJs3.ChannelId[]{
			PytesJs3.ChannelId.FAULT_REG5_GRID_INTERFERENCE,
			PytesJs3.ChannelId.FAULT_REG5_OVER_DC_COMPONENTS,
			PytesJs3.ChannelId.FAULT_REG5_OVER_TEMPERATURE,
			PytesJs3.ChannelId.FAULT_REG5_RELAY_CHECK,
			PytesJs3.ChannelId.FAULT_REG5_UNDER_TEMPERATURE,
			PytesJs3.ChannelId.FAULT_REG5_PV_INSULATION_FAULT,
			PytesJs3.ChannelId.FAULT_REG5_12V_UNDERVOLTAGE,
			PytesJs3.ChannelId.FAULT_REG5_LEAK_CURRENT,
			PytesJs3.ChannelId.FAULT_REG5_LEAK_CURRENT_SELF_CHECK,
			PytesJs3.ChannelId.FAULT_REG5_DSP_INITIAL,
			PytesJs3.ChannelId.FAULT_REG5_DSP_B,
			PytesJs3.ChannelId.FAULT_REG5_BATTERY_OVERVOLTAGE_HW,
			PytesJs3.ChannelId.FAULT_REG5_LLC_HW_OVERCURRENT,
			PytesJs3.ChannelId.FAULT_REG5_GRID_TRANSIENT_OVERCURRENT,
			PytesJs3.ChannelId.FAULT_REG5_BATTERY_COM_FAILURE,
			PytesJs3.ChannelId.FAULT_REG5_DSP_COM_FAIL
		});
		decodeBits(PytesJs3.ChannelId.FAULT_CODE_06, new PytesJs3.ChannelId[]{
			PytesJs3.ChannelId.FAULT_REG6_SLAVE_LOSE_ERR,
			PytesJs3.ChannelId.FAULT_REG6_MASTER_LOSE_ERR,
			PytesJs3.ChannelId.FAULT_REG6_SLAVE_PRD_ERR,
			PytesJs3.ChannelId.FAULT_REG6_MASTER_PRD_ERR,
			PytesJs3.ChannelId.FAULT_REG6_ADDR_CONFLICT,
			PytesJs3.ChannelId.FAULT_REG6_HEARTBEAT_LOSE,
			PytesJs3.ChannelId.FAULT_REG6_DCAN_ERR,
			PytesJs3.ChannelId.FAULT_REG6_MUL_MASTER_ERR,
			PytesJs3.ChannelId.FAULT_REG6_MODE_CONFLICT,
			PytesJs3.ChannelId.FAULT_REG6_S_PLUG_VOLT_ERR,
			PytesJs3.ChannelId.FAULT_REG6_OTHERS_FAULT,
			PytesJs3.ChannelId.FAULT_REG6_CAN_BUS_LOSE,
			PytesJs3.ChannelId.FAULT_REG6_MODEL_MISMATCH,
			PytesJs3.ChannelId.FAULT_REG6_3P_CREATE_FAIL,
			PytesJs3.ChannelId.FAULT_REG6_ACBK_OPEN,
			PytesJs3.ChannelId.FAULT_REG6_RESERVED_15
		});
		decodeBits(PytesJs3.ChannelId.FAULT_CODE_07, new PytesJs3.ChannelId[]{
			PytesJs3.ChannelId.FAULT_REG7_REVE_DC,
			PytesJs3.ChannelId.FAULT_REG7_BATTERY_HW_OVERVOLTAGE_02,
			PytesJs3.ChannelId.FAULT_REG7_BATTERY_HW_OVERCURRENT,
			PytesJs3.ChannelId.FAULT_REG7_BUS_MIDPOINT_HW_OVERCURRENT,
			PytesJs3.ChannelId.FAULT_REG7_BATTERY_STARTUP_FAIL,
			PytesJs3.ChannelId.FAULT_REG7_DC3_AVG_OVERCURRENT,
			PytesJs3.ChannelId.FAULT_REG7_DC4_AVG_OVERCURRENT,
			PytesJs3.ChannelId.FAULT_REG7_SOFTRUN_TIMEOUT,
			PytesJs3.ChannelId.FAULT_REG7_OFFGRID_TO_GRID_TIMEOUT,
			PytesJs3.ChannelId.FAULT_REG7_DRM_NOT_CONNECT,
			PytesJs3.ChannelId.FAULT_REG7_RESERVED_10,
			PytesJs3.ChannelId.FAULT_REG7_RESERVED_11,
			PytesJs3.ChannelId.FAULT_REG7_RESERVED_12,
			PytesJs3.ChannelId.FAULT_REG7_RESERVED_13,
			PytesJs3.ChannelId.FAULT_REG7_RESERVED_14,
			PytesJs3.ChannelId.FAULT_REG7_RESERVED_15
		});
		
		// Appendix 5 � Operating Status register 33121
		decodeBits(PytesJs3.ChannelId.OPERATING_STATUS, new PytesJs3.ChannelId[]{
		    PytesJs3.ChannelId.OPERATING_STAT_NORMAL_OPERATION,
		    PytesJs3.ChannelId.OPERATING_STAT_INITIALIZING,
		    PytesJs3.ChannelId.OPERATING_STAT_CONTROLLED_OFF,
		    PytesJs3.ChannelId.OPERATING_STAT_FAULT_OFF,
		    PytesJs3.ChannelId.OPERATING_STAT_STANDBY,
		    PytesJs3.ChannelId.OPERATING_STAT_LIMITED_TEMP_FREQ,
		    PytesJs3.ChannelId.OPERATING_STAT_LIMITED_EXTERNAL,
		    PytesJs3.ChannelId.OPERATING_STAT_BACKUP_OVERLOAD,
		    PytesJs3.ChannelId.OPERATING_STAT_LOAD_FAULT,
		    PytesJs3.ChannelId.OPERATING_STAT_GRID_FAULT,
		    PytesJs3.ChannelId.OPERATING_STAT_BATTERY_FAULT,
		    PytesJs3.ChannelId.OPERATING_STAT_RESERVED_11,
		    PytesJs3.ChannelId.OPERATING_STAT_GRID_SURGE_WARN,
		    PytesJs3.ChannelId.OPERATING_STAT_FAN_FAULT_WARN,
		    PytesJs3.ChannelId.OPERATING_STAT_EXTERNAL_FAN_FAIL,
		    PytesJs3.ChannelId.OPERATING_STAT_RESERVED_15
		});
		
		// Appendix 6 � Storage Control Switching register 33132
		decodeBits(PytesJs3.ChannelId.STORAGE_CONTROL_SWITCHING_VALUE, new PytesJs3.ChannelId[]{
		    PytesJs3.ChannelId.STORAGE_CTRL_SELF_USE_MODE,
		    PytesJs3.ChannelId.STORAGE_CTRL_TIME_OF_USE_MODE,
		    PytesJs3.ChannelId.STORAGE_CTRL_OFFGRID_MODE,
		    PytesJs3.ChannelId.STORAGE_CTRL_BATT_WAKEUP,
		    PytesJs3.ChannelId.STORAGE_CTRL_RESERVE_BATT_MODE,
		    PytesJs3.ChannelId.STORAGE_CTRL_ALLOW_GRID_CHARGE,
		    PytesJs3.ChannelId.STORAGE_CTRL_FEED_IN_PRIORITY,
		    PytesJs3.ChannelId.STORAGE_CTRL_BATT_OVC,
		    PytesJs3.ChannelId.STORAGE_CTRL_FORCE_CHARGE_PEAKSHAVING,
		    PytesJs3.ChannelId.STORAGE_CTRL_BATT_CURRENT_CORRECTION,
		    PytesJs3.ChannelId.STORAGE_CTRL_BATT_HEALING_MODE,
		    PytesJs3.ChannelId.STORAGE_CTRL_PEAK_SHAVING_MODE,
		    PytesJs3.ChannelId.STORAGE_CTRL_RESERVED_12,
		    PytesJs3.ChannelId.STORAGE_CTRL_RESERVED_13,
		    PytesJs3.ChannelId.STORAGE_CTRL_RESERVED_14,
		    PytesJs3.ChannelId.STORAGE_CTRL_RESERVED_15
		});

		// Appendix 7 � Setting Flag Bit register 33115
		decodeBits(PytesJs3.ChannelId.SETTING_FLAG_BIT, new PytesJs3.ChannelId[]{
		    PytesJs3.ChannelId.SETTING_FLAG_FLASH_TIMEOUT,
		    PytesJs3.ChannelId.SETTING_FLAG_CLEAR_ENERGY,
		    PytesJs3.ChannelId.SETTING_FLAG_RESERVED_02,
		    PytesJs3.ChannelId.SETTING_FLAG_RESERVED_03,
		    PytesJs3.ChannelId.SETTING_FLAG_RESERVED_04,
		    PytesJs3.ChannelId.SETTING_FLAG_RESERVED_05,
		    PytesJs3.ChannelId.SETTING_FLAG_RESERVED_06,
		    PytesJs3.ChannelId.SETTING_FLAG_RESERVED_07,
		    PytesJs3.ChannelId.SETTING_FLAG_RESET_DATALOGGER,
		    PytesJs3.ChannelId.SETTING_FLAG_FACTORY_RECOVER,
		    PytesJs3.ChannelId.SETTING_FLAG_RESERVED_10,
		    PytesJs3.ChannelId.SETTING_FLAG_RESERVED_11,
		    PytesJs3.ChannelId.SETTING_FLAG_RESERVED_12,
		    PytesJs3.ChannelId.SETTING_FLAG_RESERVED_13,
		    PytesJs3.ChannelId.SETTING_FLAG_RESERVED_14,
		    PytesJs3.ChannelId.SETTING_FLAG_RESERVED_15
		});
		
		// Appendix 8 — Operating Mode register 33122 (only one bit valid at a time)
		var rawMode = this.channel(PytesJs3.ChannelId.OPERATING_MODE).value();
		if (rawMode.isDefined()) {
		    int raw = (Integer) rawMode.get();
		    // Find which bit is set (bit position = enum value)
		    int bitPos = -1;
		    for (int i = 0; i <= 8; i++) {
		        if ((raw & (1 << i)) != 0) {
		            bitPos = i;
		            break;
		        }
		    }
		    this.channel(PytesJs3.ChannelId.OPERATING_MODE_DECODE).setNextValue(bitPos);
		};
	}

	/**
	 * Reads a raw integer channel and sets 16 boolean channels, one per bit.
	 * BIT0 ? bits[0], BIT1 ? bits[1], ... BIT15 ? bits[15]
	 */
	private void decodeBits(PytesJs3.ChannelId rawChannel, PytesJs3.ChannelId[] bits) {
		var rawValue = this.channel(rawChannel).value();
		if (!rawValue.isDefined()) {
			return; // no data yet from Modbus, leave channels undefined
		}
		int raw = (Integer) rawValue.get();
		for (int i = 0; i < bits.length; i++) {
			this.channel(bits[i]).setNextValue((raw & (1 << i)) != 0);
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
		
		if (this.battery == null) {
			this.applyPowerHandler = null;

			return;
		}
		
		logDebug(this.log, "ApplyPower: ActivePowerTarget = " + targetActivePower);

		if (this.applyPowerHandler != null) {
			this.applyPowerHandler.apply(targetActivePower, reactivePower, this.config.maxApparentPower());
		}

		
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
	
	
	// for use in handler-classes
	public void debugLog(String message) {
	    this.logDebug(this.log, message);
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
