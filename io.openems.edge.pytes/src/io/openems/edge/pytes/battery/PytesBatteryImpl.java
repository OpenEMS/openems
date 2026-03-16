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
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.pytes.ess.PytesJs3;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import io.openems.edge.common.event.EdgeEventConstants;


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

	private int minSocPercentage;

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
	


	private int maxSocPercentage;

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;

	    if (super.activate(context, config.id(), config.alias(), config.enabled(),
	            this.ess.getUnitId(), this.cm, "Modbus",
	            this.ess.getModbusBridgeId())) {
	        return;
	    }	   
	    
	    if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(),
	            "ess", config.ess_id())) {
	        return;
	    }	    
	    
	    this.ess.addBattery(this);



		this.installListener();
	}

	@Override
	@Deactivate
	protected void deactivate() {
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
		+ " DcPower: " + this.getDcDischargePower().asString();
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

	// ToDo
	private void checkSocControllers() {

		this.maxSocPercentage = 100; // Default max SoC

		this.logDebug(this.log, "checkSocControllers: MinSoC set to " + this.minSocPercentage + ", MaxSoC set to "
				+ this.maxSocPercentage);
	}

	private void installListener() {
		// ToDo.
		
		//
		// this.getVoltageChannel().onUpdate(value -> {
		//	
		//	Integer voltage =  value.get();
		// }
		// );
	}

	private void calculateAndSetBatteryPower() {
		Integer batteryCurrentWithoutDirection = this.getCurrentWithoutDirection().get(); // mA
		Integer batteryVoltage = this.getBatteryVoltage().get();
		Integer batteryCurrentDirection = this.getBatteryCurrentDirection().get(); // 0 -> charge
		
		if (batteryCurrentWithoutDirection == null || batteryVoltage == null || batteryCurrentDirection == null ) {
			log.error("Battery power cannot be calculated due to missing values");
			return;
		}

		int sign = batteryCurrentDirection == 0 ? -1 : 1;

	    int power = (int) Math.round(batteryCurrentWithoutDirection * batteryVoltage * sign / 1000000);
	    this._setDcDischargePower(power);
	    this._setVoltage((int) Math.round(batteryVoltage /1000.0)); // parent class wants V
		this._setCurrent((int) Math.round((batteryCurrentWithoutDirection * sign)/1000.0)); // parent class wants A
		
	}
	
	@Override
	public void setMinSocPercentage(int minSocPercentage) {
		this.minSocPercentage = minSocPercentage;

	}
	

	@Override
	public int getConfiguredMaxChargeCurrent() {
		return this.config.max_current(); // ToDo: different values for charge/discharge useful?
	}

	@Override
	public int getConfiguredMaxDischargeCurrent() {
		return this.config.max_current();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this,


				// ---------------------------------------------------
				// Battery core values: SOC, SOH, Voltage, Current
				// ---------------------------------------------------
				new FC4ReadInputRegistersTask(33133, Priority.HIGH, //
						
						m(PytesBattery.ChannelId.BATTERY_VOLTAGE, new UnsignedWordElement(33133), // mV
								ElementToChannelConverter.SCALE_FACTOR_2),
								
						m(PytesBattery.ChannelId.CURRENT_WITHOUT_DIRECTION, new SignedWordElement(33134), // mA. direction depends on 33135
								ElementToChannelConverter.SCALE_FACTOR_2),
						
						m(PytesBattery.ChannelId.BATTERY_CURRENT_DIRECTION, new UnsignedWordElement(33135)),  // 0-> charge, 1-> discharge
								
						m(PytesBattery.ChannelId.LLC_BUS_VOLTAGE, new UnsignedWordElement(33136),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
								
						m(PytesBattery.ChannelId.BACKUP_AC_VOLTAGE, new UnsignedWordElement(33137),
								ElementToChannelConverter.SCALE_FACTOR_2),
								
						m(PytesBattery.ChannelId.BACKUP_AC_CURRENT, new UnsignedWordElement(33138),
								ElementToChannelConverter.SCALE_FACTOR_2),						
						
						// Battery SOC [%], resolution 1 (100=100%)
						m(Battery.ChannelId.SOC, new UnsignedWordElement(33139)),
						
						// Battery SOH [%], resolution 1 (100=100%)
						m(Battery.ChannelId.SOH, new UnsignedWordElement(33140)),
						
						// Battery Voltage [mV], resolution 0.01V
						m(Battery.ChannelId.VOLTAGE, new UnsignedWordElement(33141),
								ElementToChannelConverter.SCALE_FACTOR_1),
						
						// Battery Current [mA], resolution 0.1A
						m(PytesBattery.ChannelId.BMS_BATTERY_CURRENT, new SignedWordElement(33142),
								ElementToChannelConverter.SCALE_FACTOR_2),
						
						// BMS Charge Current Limit [mA], resolution 0.1A
						m(PytesBattery.ChannelId.BMS_CHARGE_CURRENT_LIMIT, new UnsignedWordElement(33143),
								ElementToChannelConverter.SCALE_FACTOR_2),
						
						// BMS Discharge Current Limit [mA], resolution 0.1A
						m(PytesBattery.ChannelId.BMS_DISCHARGE_CURRENT_LIMIT, new UnsignedWordElement(33144),
								ElementToChannelConverter.SCALE_FACTOR_2)
						
				),
						
				// -------------------------------------------------------------
				// Battery fault status words (Low Priority - diagnostic only)
				// -------------------------------------------------------------
				new FC4ReadInputRegistersTask(33145, Priority.LOW, //

						// Battery Fault Status 01 (Appendix 9)
						m(new BitsWordElement(33145, this)
							.bit(1, PytesBattery.ChannelId.BMS_FAULT01_OVERVOLTAGE_PRO)
							.bit(2, PytesBattery.ChannelId.BMS_FAULT01_UNDERVOLTAGE_PRO)
							.bit(3, PytesBattery.ChannelId.BMS_FAULT01_OVER_TEMPERATURE_PRO)
							.bit(4, PytesBattery.ChannelId.BMS_FAULT01_UNDER_TEMPERATURE_PRO)
							.bit(5, PytesBattery.ChannelId.BMS_FAULT01_OVER_TEMPERATURE_CHARGE_PRO)
							.bit(6, PytesBattery.ChannelId.BMS_FAULT01_UNDER_TEMPERATURE_CHARGE_PRO)
							.bit(7, PytesBattery.ChannelId.BMS_FAULT01_DISCHARGE_OVERCURRENT_PRO)),

					
						// Battery Fault Status 02 (Appendix 9)
						m(new BitsWordElement(33146, this)
							.bit(0, PytesBattery.ChannelId.BMS_FAULT02_CHARGE_OVERCURRENT_PRO)
							.bit(1, PytesBattery.ChannelId.BMS_FAULT02_SYSTEM_LOW_TEMPERATURE_1)
							.bit(2, PytesBattery.ChannelId.BMS_FAULT02_SYSTEM_LOW_TEMPERATURE_2)
							.bit(3, PytesBattery.ChannelId.BMS_FAULT02_BMS_INTERNAL_PRO)
							.bit(4, PytesBattery.ChannelId.BMS_FAULT02_UNBALANCED_MODULES)
							.bit(6, PytesBattery.ChannelId.BMS_FAULT02_FULL_CHARGE_REQUEST)
							.bit(7, PytesBattery.ChannelId.BMS_FAULT02_FORCE_CHARGE_REQUEST)),

						new DummyRegisterElement(33147, 33148) // Reserved
				),
						
				// ---------------------------------------------------------------
				// Battery Power (HIGH priority – used by ESS controllers)
				// ---------------------------------------------------------------
				new FC4ReadInputRegistersTask(33149, Priority.HIGH,
 
						// Battery Power [W], resolution: 1 W
						// Positive = charging, negative = discharging
						m(PytesBattery.ChannelId.DC_DISCHARGE_POWER, new SignedDoublewordElement(33149))
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
