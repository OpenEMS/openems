package io.openems.edge.deye.battery;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.deye.enums.BatteryOperateMode;
import io.openems.edge.deye.enums.BatteryRunState;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;
import io.openems.edge.battery.api.Battery;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Deye.Battery", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
})
public class DeyeSunBatteryImpl extends AbstractOpenemsModbusComponent implements DeyeSunBattery, Battery,
		ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave, StartStoppable {

	protected static final int NET_CAPACITY = 10000;
	private BatteryRunState runState = BatteryRunState.INITIALIZING;

	// private final AtomicReference<StartStop> startStopTarget = new
	// AtomicReference<>(StartStop.UNDEFINED);
	private final Logger log = LoggerFactory.getLogger(DeyeSunBatteryImpl.class);

	private final CalculateEnergyFromPower calculateDcChargeEnergy = new CalculateEnergyFromPower(this,
			DeyeSunBattery.ChannelId.DC_CHARGE_ENERGY);

	private final CalculateEnergyFromPower calculateDcDischargeEnergy = new CalculateEnergyFromPower(this,
			DeyeSunBattery.ChannelId.DC_DISCHARGE_ENERGY);

	private volatile String offlineReason = null; // Grund für OFFLINE, optional

	@Reference
	private ComponentManager componentManager;

	@Reference
	private Power power;

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	// @Reference(policy = ReferencePolicy.DYNAMIC, policyOption =
	// ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	// private volatile DeyeSunHybrid ess;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	private Config config;
	private LocalDateTime lastDefineWorkState = LocalDateTime.now().minusSeconds(30);

	public DeyeSunBatteryImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				StartStoppable.ChannelId.values(), Battery.ChannelId.values(), //
				DeyeSunBattery.ChannelId.values() //
		);
		// this._setCapacity(NET_CAPACITY);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbusId())) {
			return;
		}
		this.config = config;
		this._setRunState(runState);
		if (this.isEnabled() && this.getStartStop() == StartStop.UNDEFINED) {
			this._setStartStop(StartStop.START);
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbusId())) {
			return;
		}
		this.config = config;

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String getModbusBridgeId() {
		return this.config.modbusId();
	}

	@Override
	public int getConfiguredMaxChargeCurrent() {
		return this.config.maxChargeCurrent();
	}

	@Override
	public int getConfiguredMaxDischargeCurrent() {
		return this.config.maxDischargeCurrent();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				//new FC16WriteRegistersTask(108,
				//		m(DeyeSunBattery.ChannelId.CONFIGURABLE_CHARGE_CURRENT_LIMIT, new SignedWordElement(108)), // 0-185A
				//		m(DeyeSunBattery.ChannelId.CONIGURABLE_DISCHARGE_CURRENT_LIMIT, new SignedWordElement(109))), // 0-185A

				new FC3ReadRegistersTask(102, Priority.HIGH, // °C
						m(DeyeSunBattery.ChannelId.BATTERY_CAPACITY, new SignedWordElement(102)),
						new DummyRegisterElement(103, 107),
						m(DeyeSunBattery.ChannelId.CONFIGURABLE_CHARGE_CURRENT_LIMIT, new SignedWordElement(108)), //
						m(DeyeSunBattery.ChannelId.CONIGURABLE_DISCHARGE_CURRENT_LIMIT, new SignedWordElement(109)),

						new DummyRegisterElement(110),
						m(DeyeSunBattery.ChannelId.BATTERY_OPERATE_MODE, new UnsignedWordElement(111)),
						m(DeyeSunBattery.ChannelId.LITHIUM_WAKE_UP_SIGN, new UnsignedWordElement(112)),
						// m(DeyeSunBattery.ChannelId.BATTERY_INTERNAL_RESISTANCE,new
						// UnsignedWordElement(113)),
						m(Battery.ChannelId.INNER_RESISTANCE, new UnsignedWordElement(113)),
						m(DeyeSunBattery.ChannelId.BATTERY_CHARGING_EFFICIENCY, new UnsignedWordElement(114)),
						m(DeyeSunBattery.ChannelId.BATTERY_CAPACITY_SHUTDOWN, new UnsignedWordElement(115)), // default
																												// 1%
						m(DeyeSunBattery.ChannelId.BATTERY_CAPACITY_RESTART, new UnsignedWordElement(116)), // default
																											// 1%
						m(DeyeSunBattery.ChannelId.BATTERY_LOW_BATT_CAPACITY, new UnsignedWordElement(117)), // default
																												// 1%
						m(DeyeSunBattery.ChannelId.BATTERY_VOLTAGE_SHUTDOWN, new UnsignedWordElement(118),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeSunBattery.ChannelId.BATTERY_VOLTAGE_RESTART, new UnsignedWordElement(119),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeSunBattery.ChannelId.BATTERY_VOLTAGE_LOW_BATT, new UnsignedWordElement(120),
								ElementToChannelConverter.SCALE_FACTOR_1)),
				// new DummyRegisterElement(121, 209),

				new FC3ReadRegistersTask(210, Priority.LOW, // °C
						// BMS
						m(DeyeSunBattery.ChannelId.BMS_CHARGING_VOLTAGE, new UnsignedWordElement(210),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeSunBattery.ChannelId.BMS_DISCHARGING_VOLTAGE, new UnsignedWordElement(211),
								ElementToChannelConverter.SCALE_FACTOR_1),

						m(Battery.ChannelId.CHARGE_MAX_CURRENT, new UnsignedWordElement(212)), //[A] dynamically calculated
						m(Battery.ChannelId.DISCHARGE_MAX_CURRENT, new UnsignedWordElement(213)),

						m(DeyeSunBattery.ChannelId.BMS_BATTERY_SOC, new UnsignedWordElement(214)),
						m(DeyeSunBattery.ChannelId.BMS_BATTERY_VOLTAGE, new SignedWordElement(215),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeSunBattery.ChannelId.BMS_BATTERY_CURRENT, new SignedWordElement(216),
								ElementToChannelConverter.SCALE_FACTOR_3),
						new DummyRegisterElement(217),

						// Hardware Limits Offgrid mode. Not clear!
						m(DeyeSunBattery.ChannelId.OFF_GRID_BATTERY_CHARGE_CURRENT_LIMIT, new UnsignedWordElement(218)),  // dynamically calculated
						m(DeyeSunBattery.ChannelId.OFF_GRID_BATTERY_DISCHARGE_CURRENT_LIMIT, new UnsignedWordElement(219)),

						m(DeyeSunBattery.ChannelId.BMS_BATTERY_ALARM, new UnsignedWordElement(220),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(DeyeSunBattery.ChannelId.BMS_BATTERY_FAULT_LOCATION, new UnsignedWordElement(221)),
						m(DeyeSunBattery.ChannelId.BMS_BATTERY_SYMBOL_2, new UnsignedWordElement(222)),
						m(DeyeSunBattery.ChannelId.BMS_BATTERY_LITHIUM_TYPE, new UnsignedWordElement(223)),
						// m(DeyeSunBattery.ChannelId.BMS_BATTERY_SOH, new UnsignedWordElement(224))),
						m(Battery.ChannelId.SOH, new UnsignedWordElement(224))),
				new FC3ReadRegistersTask(514, Priority.LOW,
						// Battery Energy
						// Charge today (kWh)
						m(DeyeSunBattery.ChannelId.TODAY_BATTERY_CHARGE, new UnsignedWordElement(514),
								ElementToChannelConverter.SCALE_FACTOR_2),
						// Discharge today (kWh)
						m(DeyeSunBattery.ChannelId.TODAY_BATTERY_DISCHARGE, new UnsignedWordElement(515),
								ElementToChannelConverter.SCALE_FACTOR_2),
						// Total charge (kWh)
						m(DeyeSunBattery.ChannelId.TOTAL_BATTERY_CHARGE,
								new UnsignedDoublewordElement(516).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2),
						// Total discharge (kWh)
						m(DeyeSunBattery.ChannelId.TOTAL_BATTERY_DISCHARGE,
								new UnsignedDoublewordElement(518).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2),
						new DummyRegisterElement(520, 585),
						m(DeyeSunBattery.ChannelId.BATTERY_TEMPERATURE, new UnsignedWordElement(586),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(DeyeSunBattery.ChannelId.BATTERY_VOLTAGE, new UnsignedWordElement(587),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(Battery.ChannelId.SOC, new UnsignedWordElement(588)), new DummyRegisterElement(589),
						m(DeyeSunBattery.ChannelId.BATTERY_OUTPUT_POWER, new SignedWordElement(590)),
						m(Battery.ChannelId.CURRENT, new SignedWordElement(591),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(DeyeSunBattery.ChannelId.BATTERY_CORRECTED_AH, new SignedWordElement(592))));
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	public String collectDebugData() {
		// Collect channel values in one stream
		return Stream.of(DeyeSunBattery.ChannelId.values(), Battery.ChannelId.values()

		).flatMap(Arrays::stream).map(id -> {
			try {
				return id.name() + "=" + this.channel(id).value().asString();
			} catch (Exception e) {
				return id.name() + "=n/a";
			}
		}).collect(Collectors.joining("; \n"));
	}

	/**
	 * Uses Info Log for further debug features.
	 */
	@Override
	protected void logDebug(Logger log, String message) {
		if (this.config.debugMode()) {

			if (this.config.extendedDebugMode()) {
				this.logInfo(log,
						"\n ############################################## Battery Values Start #############################################");
				this.logInfo(log, this.collectDebugData());
				this.logInfo(log,
						"\n ############################################## Battery Values End #############################################");
			}
			this.logInfo(log, message);

		}
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getDcPower().asString() //
				+ "|Battery Power:" + this.channel(DeyeSunBattery.ChannelId.BATTERY_OUTPUT_POWER).value().asString()
				+ " Voltage: " + this.channel(DeyeSunBattery.ChannelId.BATTERY_VOLTAGE).value().asString()
				+ " Current: " + this.channel(Battery.ChannelId.CURRENT).value().asString() + ";";
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			// this.hasError();
			this.calculateEnergy();
			this.logDebug(this.log, "");
			break;
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			this.defineWorkState();
			break;
		}
	}

	private void defineWorkState() {

		if (this.getBatteryOperateMode().asEnum() == BatteryOperateMode.NO_BATTERY) {
			changeState(BatteryRunState.NO_BATTERY);
			return;
		}

		// high prio OFFLINE
		if (offlineReason != null) {
			changeState(BatteryRunState.OFFLINE);
			return;
		}
		// local errors
		if (hasError()) {
			changeState(BatteryRunState.ERROR);
			return;
		}
		if (hasWarning()) {
			changeState(BatteryRunState.WARNING);
			return;
		}
		//
		if (!isEnabled()) {
			changeState(BatteryRunState.OFFLINE);
			return;
		}

		//

		StartStop startStop = this.getStartStop(); // <-- Channel

		if (startStop == StartStop.UNDEFINED) {
			changeState(BatteryRunState.INITIALIZING);
			return;
		}

		changeState(BatteryRunState.NORMAL);

		this.logDebug(this.log, "Battery: " + this + " Running: " + this.isStarted() + " RunState: "
				+ this.getRunState().toString() + "" + "");

	}

	/**
	 * Changes the state if hysteresis time passed, to avoid too quick changes.
	 *
	 * @param nextState the target state
	 * @return whether the state was changed
	 */
	private boolean changeState(BatteryRunState nextState) {
		var now = LocalDateTime.now();

		// avoid early transistions
		if (!now.minusSeconds(20).isAfter(this.lastDefineWorkState)) {
			return false;
		}
		this.lastDefineWorkState = now;

		if (this.runState == nextState) {
			return false;
		}

		this.runState = nextState;
		this._setRunState(this.runState); // save to channel
		return true;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode), //
				DeyeSunBattery.getModbusSlaveNatureTable(accessMode) //
		);
	}

	@Override
	public boolean hasError() {
		// ToDo: Maybe not complete
		var alarm = this.getBatteryAlarm().orElse(null);
		return alarm != null && alarm != 0;

	}

	@Override
	public boolean hasWarning() {
		// ToDo
		var batteryVoltage = this.getBatteryVoltage().get(); // from battery nature
		var batteryLowVoltage = this.getBatteryVoltageLow().get();

		var batteryCurrent = this.getCurrent().get(); // from battery nature
		var bmsDischargeCurrentLimit = this.getConfigurableDischargeCurrentLimit().get();
		var bmsChargeCurrentLimit = this.getConfigurableChargeCurrentLimit().get();

		// var bmsDischargeCurrentLimit = this.getDischargeMaxCurrent().get();

		// var bmsChargeCurrentLimit = this.getChargeMaxCurrent().get();

		if (batteryVoltage == null || batteryLowVoltage == null || batteryCurrent == null
				|| bmsDischargeCurrentLimit == null || bmsChargeCurrentLimit == null) {
			this.logWarn(log, "Battery values are not complete (yet)");
			return true;
		}

		if (batteryVoltage < batteryLowVoltage) {
			this.logWarn(log, "Battery Voltage too low");
			return true;
		}

		if (batteryCurrent < 0 && Math.abs(batteryCurrent) > bmsDischargeCurrentLimit) {
			this.logWarn(log, "Battery discharge current too high");
			return true;
		}

		if (batteryCurrent > 0 && Math.abs(batteryCurrent) > bmsChargeCurrentLimit) {
			this.logWarn(log, "Battery charge current too high");
			return true;
		}

		// ...and so forth

		return false;

	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	public BatteryRunState getRunState() {
		return this.runState;
	}

	private void calculateEnergy() {

		/*
		 * Calculate DC Power and Energy
		 */
		var dcDischargePower = this.getDcPower().get();

		if (dcDischargePower == null) {
			// Not available
			this.calculateDcChargeEnergy.update(null);
			this.calculateDcDischargeEnergy.update(null);
		} else if (dcDischargePower > 0) {
			// Discharge
			this.calculateDcChargeEnergy.update(0);
			this.calculateDcDischargeEnergy.update(dcDischargePower);
		} else {
			// Charge
			this.calculateDcChargeEnergy.update(dcDischargePower * -1);
			this.calculateDcDischargeEnergy.update(0);
		}
	}

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		StartStop previous = this.getStartStop(); //
		if (previous == value)
			return;

		this._setStartStop(value); //

		if (value == StartStop.START) {
			changeState(BatteryRunState.INITIALIZING);
		} else {
			changeState(BatteryRunState.OFFLINE);
		}
	}

	@Override
	public void setOfflineByExternal(String reason) {
		offlineReason = reason;
		if (changeState(BatteryRunState.OFFLINE)) {
			this.logWarn(log, "Battery set to OFFLINE by external request. Reason: " + reason);
		}
	}

	@Override
	public void clearExternalOffline() {
		if (offlineReason != null) {
			this.logInfo(log, "External OFFLINE reason cleared: " + offlineReason);
			offlineReason = null;
		}
	}

}