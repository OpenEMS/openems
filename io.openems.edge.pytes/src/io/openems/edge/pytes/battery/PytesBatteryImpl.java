package io.openems.edge.pytes.battery;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
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
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.pytes.ess.PytesJs3;


@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Pytes.Battery", //
		immediate = true, //
		configurationPolicy = REQUIRE //
) //

@EventTopics({ // ToDo: take the right events
	EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
	EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
})

public class PytesBatteryImpl extends AbstractOpenemsModbusComponent
		implements Battery, PytesBattery, ModbusComponent, OpenemsComponent, EventHandler,  ModbusSlave {

	public static final int DEFAULT_UNIT_ID = 225;
	public static final int BATTERY_VOLTAGE = 48;


	@Reference
	protected ConfigurationAdmin cm;

	private final Logger log = LoggerFactory.getLogger(PytesBatteryImpl.class);

	protected Config config;

	public PytesBatteryImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				PytesBattery.ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(
		    name = "ess",
		    policy = ReferencePolicy.STATIC,
		    policyOption = ReferencePolicyOption.GREEDY,
		    cardinality = ReferenceCardinality.MANDATORY
		)
		private volatile PytesJs3 ess;

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;

	    if (super.activate(context, config.id(), config.alias(), config.enabled(),
	            this.ess.getUnitId(), this.cm, "Modbus",
	            this.ess.getModbusBridgeId()) || OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(),
	            "ess", config.ess_id())) {
	        return;
	    }

	    this.ess.addBattery(this);

	    // Not available from the inverter/BMS, see Config#capacity()
	    this._setCapacity(config.capacity() > 0 ? config.capacity() : null);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		// unregister from the ESS, otherwise it keeps using this stale instance
		if (this.ess != null) {
			this.ess.removeBattery(this);
		}
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		// super.handleEvent(event);

		switch (event.getTopic()) {

		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			this.calculateAndSetBatteryPower();
			break;
		}
	}

	@Override
	public String debugLog() {
		return "SoC: " + this.getSoc()
		+ " DcPower: " + this.getDcDischargePower().asString()
		+ " DcPowerUnsigned: " + this.getDcDischargePowerUnsigned().asString()		
		;
	}

	/**
	 * Uses Info Log for further debug features.
	 */
	@Override
	protected void logDebug(Logger log, String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		// TODO implement battery start/stop if needed
		this._setStartStop(value);
	}

	private void calculateAndSetBatteryPower() {
		// Integer batteryCurrentWithoutDirection = this.getCurrentWithoutDirection().get(); // mA
		
		// Integer batteryVoltage = this.getBatteryVoltage().get(); //mV
		
		Integer batteryCurrentDirection = this.getBatteryCurrentDirection().get(); // 0 -> charge

		Integer batteryCurrentWithoutDirection = this.getBmsBatteryCurrent().get(); // mA (from BMS)
		Integer batteryVoltage = this.getBmsBatteryVoltage().get(); // mV (from BMS)
		
		
		if (batteryCurrentWithoutDirection == null || batteryVoltage == null || batteryCurrentDirection == null) {
			this.logDebug(this.log, "Battery power cannot be calculated yet, values missing");
			return;
		}

		int sign = batteryCurrentDirection == 0 ? -1 : 1;

		// mA * mV overflows int above ~40 A (40_500 * 53_000 > 2^31), which
		// produced wrong sign and magnitude at high charge currents.
		long powerMicroWatt = (long) batteryCurrentWithoutDirection * batteryVoltage * sign;
		int power = (int) Math.round(powerMicroWatt / 1_000_000.0);
		this._setDcDischargePower(power);
		this._setVoltage((int) Math.round(batteryVoltage / 1000.0)); // parent class wants V
		this._setCurrent((int) Math.round(batteryCurrentWithoutDirection * sign / 1000.0)); // parent class wants A

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
		return new ModbusProtocol(this,


				// ---------------------------------------------------------------
				// Inverter battery port + BMS values (reg 33133-33144)
				// Priority HIGH - read every cycle for power/current/voltage calc
				// ---------------------------------------------------------------
				new FC4ReadInputRegistersTask(33133, Priority.HIGH,

						// reg 33133 - Battery voltage at inverter port [mV]
						// Datasheet: 0.1 V -> SCALE_FACTOR_2 -> mV
						m(PytesBattery.ChannelId.BATTERY_VOLTAGE, new UnsignedWordElement(33133),
								ElementToChannelConverter.SCALE_FACTOR_2),
						
						// reg 33134 - Battery current magnitude [mA] (always positive, no sign)
						// Direction is in reg 33135. Datasheet: 0.1 A -> SCALE_FACTOR_2
						m(PytesBattery.ChannelId.CURRENT_WITHOUT_DIRECTION, new SignedWordElement(33134),
								ElementToChannelConverter.SCALE_FACTOR_2),
						
						// reg 33135 - Battery current direction
						// 0  = charging (power into battery), 1 = discharge (power out)
						m(PytesBattery.ChannelId.BATTERY_CURRENT_DIRECTION, new UnsignedWordElement(33135)),
						
						// reg 33136 - LLC bus voltage (internal DC bus between battery and inverter) [mV]
						// Datasheet: 0.1 V -> SCALE_FACTOR_2
						m(PytesBattery.ChannelId.LLC_BUS_VOLTAGE, new UnsignedWordElement(33136),
								ElementToChannelConverter.SCALE_FACTOR_2),
						
						new DummyRegisterElement(33137, 33138), // 		

						// reg 33139 - Batter state of Charge [%]
						// Datasheet: resolution 1 -> no converter needed
						m(Battery.ChannelId.SOC, new UnsignedWordElement(33139)),
						
						// reg 33140 - Batter state of Health [%]
						// Datasheet: resolution 1 -> no converter needed
						m(Battery.ChannelId.SOH, new UnsignedWordElement(33140)),
						
						// reg 33141 - BMS battery voltage [mV]
						// Datasheet: 0.01 V -> SCALE_FACTOR_1
						// Battery.ChannelId.VOLTAGE is set programmatically in V by
						// calculateAndSetBatteryPower() - this register is the BMS cross-check 
						m(PytesBattery.ChannelId.BMS_BATTERY_VOLTAGE, new UnsignedWordElement(33141),
								ElementToChannelConverter.SCALE_FACTOR_1),
						
						// reg 33142 – BMS battery current [mA], signed
						// No direction! Look at Register 33135
						// Datasheet: 0.1 A -> SCALE_FACTOR_2 -> mA
						m(PytesBattery.ChannelId.BMS_BATTERY_CURRENT, new SignedWordElement(33142),
								ElementToChannelConverter.SCALE_FACTOR_2),
						
						// reg 33143 – BMS maximum charge current limit [mA]
						// Datasheet: 0.1 A -> SCALE_FACTOR_2 -> mA
						m(PytesBattery.ChannelId.BMS_CHARGE_CURRENT_LIMIT, new UnsignedWordElement(33143),
								ElementToChannelConverter.SCALE_FACTOR_2),
						
						// reg 33144 – BMS maximum discharge current limit [mA]
						// Datasheet: 0.1 A -> SCALE_FACTOR_2 -> mA
						m(PytesBattery.ChannelId.BMS_DISCHARGE_CURRENT_LIMIT, new UnsignedWordElement(33144),
								ElementToChannelConverter.SCALE_FACTOR_2),

						// reg 33145-33148 - Fault status words. Read in the same frame:
						// four extra registers are cheaper than a second Modbus request.
						// reg 33145 – Battery Fault Status word 01 (Appendix 9)
						m(new BitsWordElement(33145, this)
							.bit(1, PytesBattery.ChannelId.BMS_FAULT01_OVERVOLTAGE_PRO)
							.bit(2, PytesBattery.ChannelId.BMS_FAULT01_UNDERVOLTAGE_PRO)
							.bit(3, PytesBattery.ChannelId.BMS_FAULT01_OVER_TEMPERATURE_PRO)
							.bit(4, PytesBattery.ChannelId.BMS_FAULT01_UNDER_TEMPERATURE_PRO)
							.bit(5, PytesBattery.ChannelId.BMS_FAULT01_OVER_TEMPERATURE_CHARGE_PRO)
							.bit(6, PytesBattery.ChannelId.BMS_FAULT01_UNDER_TEMPERATURE_CHARGE_PRO)
							.bit(7, PytesBattery.ChannelId.BMS_FAULT01_DISCHARGE_OVERCURRENT_PRO)),

					
						// reg 33146 – Battery Fault Status word 02 (Appendix 9)
						m(new BitsWordElement(33146, this)
							.bit(0, PytesBattery.ChannelId.BMS_FAULT02_CHARGE_OVERCURRENT_PRO)
							.bit(1, PytesBattery.ChannelId.BMS_FAULT02_SYSTEM_LOW_TEMPERATURE_1)
							.bit(2, PytesBattery.ChannelId.BMS_FAULT02_SYSTEM_LOW_TEMPERATURE_2)
							.bit(3, PytesBattery.ChannelId.BMS_FAULT02_BMS_INTERNAL_PRO)
							.bit(4, PytesBattery.ChannelId.BMS_FAULT02_UNBALANCED_MODULES)
							.bit(6, PytesBattery.ChannelId.BMS_FAULT02_FULL_CHARGE_REQUEST)
							.bit(7, PytesBattery.ChannelId.BMS_FAULT02_FORCE_CHARGE_REQUEST)),

						new DummyRegisterElement(33147, 33147), // Reserved
						// reg 33148 - Backup port load power [W], see BACKUP_LOAD_POWER
						m(PytesBattery.ChannelId.BACKUP_LOAD_POWER, new UnsignedWordElement(33148)),

						// reg 33149-33150 - Battery power, same frame
						// reg 33149–33150 – Battery power [W] (S32, two registers)
						// Datasheet: 1 W resolution → no converter needed
						// Positive = charging, negative = discharging
						// Stored in DC_DISCHARGE_POWER_UNSIGNED for cross-check.
						// The signed DC_DISCHARGE_POWER is set programmatically by
						// calculateAndSetBatteryPower() using voltage × current × direction.
						m(PytesBattery.ChannelId.DC_DISCHARGE_POWER_UNSIGNED, new SignedDoublewordElement(33149))
				)

		);

	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(PytesBattery.class, accessMode, 100) //
						//.channel(0, PytesBattery.ChannelId.CHARGE_CYCLES, ModbusType.UINT16) //
						//.channel(1, PytesBattery.ChannelId.DC_CHARGED_ENERGY, ModbusType.UINT16) //
						//.channel(2, PytesBattery.ChannelId.DC_DISCHARGED_ENERGY, ModbusType.UINT16) //

						.build());
	}
}
