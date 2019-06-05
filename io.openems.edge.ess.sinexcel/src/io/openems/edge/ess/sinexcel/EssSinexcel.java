package io.openems.edge.ess.sinexcel;

import java.util.Optional;

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
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Sinexcel", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE) //
public class EssSinexcel extends AbstractOpenemsModbusComponent
		implements SymmetricEss, ManagedSymmetricEss, EventHandler, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(EssSinexcel.class);

	public static final int DEFAULT_UNIT_ID = 1;
	public static final int MAX_APPARENT_POWER = 30_000;

	private static final int DISABLED_ANTI_ISLANDING = 0;
	private static final int ENABLED_ANTI_ISLANDING = 1;
	// Slow and Float Charge Voltage must be the same for the LiFePO4 battery.
	private static final int SLOW_CHARGE_VOLTAGE = 4370;
	private static final int FLOAT_CHARGE_VOLTAGE = 4370;

	private InverterState inverterState;
	private Battery battery;
	private int counterOn = 0;
	private int counterOff = 0;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private Power power;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setBattery(Battery battery) {
		this.battery = battery;
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus); // Bridge Modbus
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), DEFAULT_UNIT_ID, this.cm, "Modbus",
				config.modbus_id());

		this.inverterState = config.InverterState();

		// initialize the connection to the battery
		this.initializeBattery(config.battery_id());

		this.softStart();
		this.resetDcAcEnergy();
		this.inverterOn();
		this.doHandlingSlowFloatVoltage();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public EssSinexcel() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				SinexcelChannelId.values() //
		);
		this.channel(SymmetricEss.ChannelId.MAX_APPARENT_POWER).setNextValue(EssSinexcel.MAX_APPARENT_POWER);
	}

	/**
	 * Initializes the connection to the Battery.
	 * 
	 * @param servicePid this components' Service-PID
	 * @param batteryId  the Component-ID of the Battery component
	 */
	private void initializeBattery(String batteryId) {
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Battery", batteryId)) {
			return;
		}

		this.battery.getSoc().onChange(value -> {
			this.getSoc().setNextValue(value.get());
			this.channel(SinexcelChannelId.BAT_SOC).setNextValue(value.get());
			this.channel(SymmetricEss.ChannelId.SOC).setNextValue(value.get());
		});

		this.battery.getVoltage().onChange(value -> {
			this.channel(SinexcelChannelId.BAT_VOLTAGE).setNextValue(value.get());
		});
	}

	/**
	 * Sets the Battery Ranges. Executed on TOPIC_CYCLE_AFTER_PROCESS_IMAGE.
	 */
	private void setBatteryRanges() {
		if (battery == null) {
			return;
		}
		int disMaxA = battery.getDischargeMaxCurrent().value().orElse(0);
		int chaMaxA = battery.getChargeMaxCurrent().value().orElse(0);
		int disMinV = battery.getDischargeMinVoltage().value().orElse(0);
		int chaMaxV = battery.getChargeMaxVoltage().value().orElse(0);

		IntegerWriteChannel setDisMinV = this.channel(SinexcelChannelId.DIS_MIN_V);
		IntegerWriteChannel setChaMaxV = this.channel(SinexcelChannelId.CHA_MAX_V);
		IntegerWriteChannel setDisMaxV = this.channel(SinexcelChannelId.DIS_MAX_A);
		IntegerWriteChannel setChaMaxA = this.channel(SinexcelChannelId.CHA_MAX_A);
		try {
			setChaMaxA.setNextWriteValue(chaMaxA * 10);
			setDisMaxV.setNextWriteValue(disMaxA * 10);
			setDisMinV.setNextWriteValue(disMinV * 10);
			setChaMaxV.setNextWriteValue(chaMaxV * 10);

			this.channel(SinexcelChannelId.STATE_UNABLE_TO_SET_BATTERY_RANGES).setNextValue(false);
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "Unable to set battery ranges: " + e.getMessage());
			this.channel(SinexcelChannelId.STATE_UNABLE_TO_SET_BATTERY_RANGES).setNextValue(false);
		}
	}

	/**
	 * Starts the inverter.
	 */
	public void inverterOn() {
		IntegerWriteChannel setdataModOnCmd = this.channel(SinexcelChannelId.SETDATA_MOD_ON_CMD);
		try {
			setdataModOnCmd.setNextWriteValue(1); // Here: START = 1
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "problem occurred while trying to start inverter" + e.getMessage());
		}
	}

	/**
	 * Stops the inverter.
	 */
	public void inverterOff() {
		IntegerWriteChannel setdataModOffCmd = this.channel(SinexcelChannelId.SETDATA_MOD_OFF_CMD);
		try {
			setdataModOffCmd.setNextWriteValue(1); // Here: STOP = 1
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "problem occurred while trying to stop system" + e.getMessage());
		}
	}

	public void resetDcAcEnergy() {
		IntegerWriteChannel chargeEnergy = this.channel(SinexcelChannelId.SET_ANALOG_CHARGE_ENERGY);
		IntegerWriteChannel dischargeEnergy = this.channel(SinexcelChannelId.SET_ANALOG_DISCHARGE_ENERGY);
		IntegerWriteChannel chargeDcEnergy = this.channel(SinexcelChannelId.SET_ANALOG_DC_CHARGE_ENERGY);
		IntegerWriteChannel dischargeDcEnergy = this.channel(SinexcelChannelId.SET_ANALOG_DC_DISCHARGE_ENERGY);
		try {
			chargeDcEnergy.setNextWriteValue(0);
			dischargeDcEnergy.setNextWriteValue(0);
			chargeEnergy.setNextWriteValue(0);
			dischargeEnergy.setNextWriteValue(0);
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "problem occurred while trying to reset the AC DC energy" + e.getMessage());
		}
	}

	/**
	 * Executes a Soft-Start. Sets the internal DC relay.
	 */
	public void softStart() {
		IntegerWriteChannel setDcRelay = this.channel(SinexcelChannelId.SET_INTERN_DC_RELAY);
		try {
			setDcRelay.setNextWriteValue(1);
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "problem occured while trying to set the intern DC relay");
		}
	}

	/**
	 * At first the PCS needs a stop command, then is required to remove the AC
	 * connection, after that the Grid OFF command.
	 */
	public void islandOn() {
		IntegerWriteChannel setAntiIslanding = this.channel(SinexcelChannelId.SET_ANTI_ISLANDING);
		IntegerWriteChannel setdataGridOffCmd = this.channel(SinexcelChannelId.SETDATA_GRID_OFF_CMD);
		try {
			setAntiIslanding.setNextWriteValue(DISABLED_ANTI_ISLANDING);
			setdataGridOffCmd.setNextWriteValue(1); // Stop
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "problem occurred while trying to activate" + e.getMessage());
		}
	}

	/**
	 * At first the PCS needs a stop command, then is required to plug in the AC
	 * connection, after that the Grid ON command.
	 */
	public void islandingOff() {
		IntegerWriteChannel setAntiIslanding = this.channel(SinexcelChannelId.SET_ANTI_ISLANDING);
		IntegerWriteChannel setdataGridOnCmd = this.channel(SinexcelChannelId.SETDATA_GRID_ON_CMD);
		try {
			setAntiIslanding.setNextWriteValue(ENABLED_ANTI_ISLANDING);
			setdataGridOnCmd.setNextWriteValue(1); // Start
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "problem occurred while trying to deactivate islanding" + e.getMessage());
		}
	}

	public void doHandlingSlowFloatVoltage() {
		IntegerWriteChannel setSlowChargeVoltage = this.channel(SinexcelChannelId.SET_SLOW_CHARGE_VOLTAGE);
		IntegerWriteChannel setFloatChargeVoltage = this.channel(SinexcelChannelId.SET_FLOAT_CHARGE_VOLTAGE);
		try {
			setSlowChargeVoltage.setNextWriteValue(SLOW_CHARGE_VOLTAGE);
			setFloatChargeVoltage.setNextWriteValue(FLOAT_CHARGE_VOLTAGE);
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "problem occurred while trying to write the voltage limits" + e.getMessage());
		}
	}

	/**
	 * Is Grid Shutdown?.
	 * 
	 * @return true if grid is shut down
	 */
	public boolean faultIslanding() {
		StateChannel channel = this.channel(SinexcelChannelId.STATE_4);
		Optional<Boolean> islanding = channel.getNextValue().asOptional();
		return islanding.isPresent() && islanding.get();
	}

	/**
	 * Is inverter state ON?.
	 * 
	 * @return true if inverter is in ON-State
	 */
	public boolean isStateOn() {
		StateChannel channel = this.channel(SinexcelChannelId.STATE_18);
		Optional<Boolean> stateOff = channel.getNextValue().asOptional();
		return stateOff.isPresent() && stateOff.get();
	}

	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC6WriteRegisterTask(0x028A, //
						m(SinexcelChannelId.SETDATA_MOD_ON_CMD, new UnsignedWordElement(0x028A))),
				new FC6WriteRegisterTask(0x028B, //
						m(SinexcelChannelId.SETDATA_MOD_OFF_CMD, new UnsignedWordElement(0x028B))),
				new FC6WriteRegisterTask(0x0290, //
						m(SinexcelChannelId.SET_INTERN_DC_RELAY, new UnsignedWordElement(0x0290))),
				new FC6WriteRegisterTask(0x028D, //
						m(SinexcelChannelId.SETDATA_GRID_ON_CMD, new UnsignedWordElement(0x028D))), // Start
				new FC6WriteRegisterTask(0x028E, //
						m(SinexcelChannelId.SETDATA_GRID_OFF_CMD, new UnsignedWordElement(0x028E))), // Stop
				new FC6WriteRegisterTask(0x0087, //
						m(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, new SignedWordElement(0x0087))),
				new FC6WriteRegisterTask(0x0088, //
						m(ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_EQUALS, new SignedWordElement(0x0088))),
				new FC6WriteRegisterTask(0x032B, //
						m(SinexcelChannelId.CHA_MAX_A, new UnsignedWordElement(0x032B))), //
				new FC6WriteRegisterTask(0x032C, //
						m(SinexcelChannelId.DIS_MAX_A, new UnsignedWordElement(0x032C))), //
				new FC6WriteRegisterTask(0x0329, //
						m(SinexcelChannelId.SET_SLOW_CHARGE_VOLTAGE, new UnsignedWordElement(0x0329))),
				new FC6WriteRegisterTask(0x0328, //
						m(SinexcelChannelId.SET_FLOAT_CHARGE_VOLTAGE, new UnsignedWordElement(0x0328))),
				new FC6WriteRegisterTask(0x032E, //
						m(SinexcelChannelId.CHA_MAX_V, new UnsignedWordElement(0x032E))),
				new FC6WriteRegisterTask(0x032D, //
						m(SinexcelChannelId.DIS_MIN_V, new UnsignedWordElement(0x032D))),
				new FC6WriteRegisterTask(0x007E, //
						m(SinexcelChannelId.SET_ANALOG_CHARGE_ENERGY, new UnsignedWordElement(0x007E))),
				new FC6WriteRegisterTask(0x007F, //
						m(SinexcelChannelId.SET_ANALOG_CHARGE_ENERGY, new UnsignedWordElement(0x007F))),
				new FC6WriteRegisterTask(0x0080, //
						m(SinexcelChannelId.SET_ANALOG_DISCHARGE_ENERGY, new UnsignedWordElement(0x0080))),
				new FC6WriteRegisterTask(0x0081, //
						m(SinexcelChannelId.SET_ANALOG_DISCHARGE_ENERGY, new UnsignedWordElement(0x0081))),
				new FC6WriteRegisterTask(0x0090, //
						m(SinexcelChannelId.SET_ANALOG_DC_CHARGE_ENERGY, new UnsignedWordElement(0x0090))),
				new FC6WriteRegisterTask(0x0091, //
						m(SinexcelChannelId.SET_ANALOG_DC_CHARGE_ENERGY, new UnsignedWordElement(0x0091))),
				new FC6WriteRegisterTask(0x0092, //
						m(SinexcelChannelId.SET_ANALOG_DC_DISCHARGE_ENERGY, new UnsignedWordElement(0x0092))),
				new FC6WriteRegisterTask(0x0093, //
						m(SinexcelChannelId.SET_ANALOG_DC_DISCHARGE_ENERGY, new UnsignedWordElement(0x0093))),

				new FC3ReadRegistersTask(0x0065, Priority.HIGH, //
						m(SinexcelChannelId.INVOUTVOLT_L1, new UnsignedWordElement(0x0065),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(SinexcelChannelId.INVOUTVOLT_L2, new UnsignedWordElement(0x0066),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(SinexcelChannelId.INVOUTVOLT_L3, new UnsignedWordElement(0x0067),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(SinexcelChannelId.INVOUTCURRENT_L1, new UnsignedWordElement(0x0068),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(SinexcelChannelId.INVOUTCURRENT_L2, new UnsignedWordElement(0x0069),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(SinexcelChannelId.INVOUTCURRENT_L3, new UnsignedWordElement(0x006A),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),

				new FC3ReadRegistersTask(0x007E, Priority.HIGH, //
						m(SinexcelChannelId.ANALOG_CHARGE_ENERGY, new UnsignedDoublewordElement(0x007E)), //
						m(SinexcelChannelId.ANALOG_DISCHARGE_ENERGY, new UnsignedDoublewordElement(0x0080)), //
						new DummyRegisterElement(0x0082, 0x0083),
						m(SinexcelChannelId.TEMPERATURE, new SignedWordElement(0x0084)),
						new DummyRegisterElement(0x0085, 0x008C),
						m(SinexcelChannelId.DC_POWER, new SignedWordElement(0x008D),
								ElementToChannelConverter.SCALE_FACTOR_1),
						new DummyRegisterElement(0x008E, 0x008F),
						m(SinexcelChannelId.ANALOG_DC_CHARGE_ENERGY, new UnsignedDoublewordElement(0x0090)), //
						m(SinexcelChannelId.ANALOG_DC_DISCHARGE_ENERGY, new UnsignedDoublewordElement(0x0092))), //

				new FC3ReadRegistersTask(0x0248, Priority.HIGH, //
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedWordElement(0x0248),
								ElementToChannelConverter.SCALE_FACTOR_1),
						new DummyRegisterElement(0x0249),
						m(SinexcelChannelId.FREQUENCY, new SignedWordElement(0x024A),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						new DummyRegisterElement(0x024B, 0x0254), //
						m(SinexcelChannelId.DC_CURRENT, new SignedWordElement(0x0255),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						new DummyRegisterElement(0x0256),
						m(SinexcelChannelId.DC_VOLTAGE, new UnsignedWordElement(0x0257),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),

				new FC3ReadRegistersTask(0x0260, Priority.HIGH, //
						m(SinexcelChannelId.SINEXCEL_STATE, new UnsignedWordElement(0x0260))),

				new FC3ReadRegistersTask(0x0001, Priority.ONCE, //
						m(SinexcelChannelId.MODEL, new StringWordElement(0x0001, 16)),
						m(SinexcelChannelId.SERIAL, new StringWordElement(0x0011, 8))),

				new FC3ReadRegistersTask(0x0220, Priority.ONCE, //
						// m(EssSinexcel.ChannelId.Manufacturer, new StringWordElement(0x01F8, 16)), //
						// String // Line109
						// m(EssSinexcel.ChannelId.Model_2, new StringWordElement(0x0208, 16)),
						// String (32Char) // line110
						m(SinexcelChannelId.VERSION, new StringWordElement(0x0220, 8))), // String (16Char) //
				// m(EssSinexcel.ChannelId.Serial_Number, new StringWordElement(0x0228, 16))),
				// // String (32Char)// Line113

				new FC3ReadRegistersTask(0x032D, Priority.LOW, //
						m(SinexcelChannelId.LOWER_VOLTAGE_LIMIT, new UnsignedWordElement(0x032D),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(SinexcelChannelId.UPPER_VOLTAGE_LIMIT, new UnsignedWordElement(0x032E),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),

				// new FC3ReadRegistersTask(0x0316, Priority.LOW,
				// m(EssSinexcel.ChannelId.ANTI_ISLANDING, new UnsignedWordElement(0x0316))),

				new FC3ReadRegistersTask(0x024E, Priority.HIGH, //
						// Line136, Magnification = 0
						m(SymmetricEss.ChannelId.REACTIVE_POWER, new SignedWordElement(0x024E))),
				new FC3ReadRegistersTask(0x0262, Priority.LOW, //
						m(new BitsWordElement(0x0262, this) //
								.bit(0, SinexcelChannelId.STATE_0) //
								.bit(1, SinexcelChannelId.STATE_1) //
								.bit(2, SinexcelChannelId.STATE_2) //
								.bit(3, SinexcelChannelId.STATE_3) //
								.bit(4, SinexcelChannelId.STATE_4) //
								.bit(5, SinexcelChannelId.STATE_5) //
								.bit(6, SinexcelChannelId.STATE_6) //
								.bit(7, SinexcelChannelId.STATE_7) //
								.bit(8, SinexcelChannelId.STATE_8) //
								.bit(9, SinexcelChannelId.STATE_9) //
								.bit(10, SinexcelChannelId.STATE_10) //
								.bit(11, SinexcelChannelId.STATE_11) //
								.bit(12, SinexcelChannelId.STATE_12) //
								.bit(13, SinexcelChannelId.STATE_13) //
								.bit(14, SinexcelChannelId.STATE_14) //
								.bit(15, SinexcelChannelId.STATE_15))),

				new FC3ReadRegistersTask(0x0260, Priority.LOW, //
						m(new BitsWordElement(0x0260, this) //
								.bit(1, SinexcelChannelId.SINEXCEL_STATE_1) //
								.bit(2, SinexcelChannelId.SINEXCEL_STATE_2) //
								.bit(3, SinexcelChannelId.SINEXCEL_STATE_3) //
								.bit(4, SinexcelChannelId.SINEXCEL_STATE_4) //
								.bit(5, SinexcelChannelId.SINEXCEL_STATE_5) //
								.bit(6, SinexcelChannelId.SINEXCEL_STATE_6) //
								.bit(7, SinexcelChannelId.SINEXCEL_STATE_7) //
								.bit(8, SinexcelChannelId.SINEXCEL_STATE_8) //
								.bit(9, SinexcelChannelId.SINEXCEL_STATE_9))),

				new FC3ReadRegistersTask(0x0020, Priority.LOW, //
						m(new BitsWordElement(0x0020, this) //
								.bit(0, SinexcelChannelId.STATE_16) //
								.bit(1, SinexcelChannelId.STATE_17) //
								.bit(2, SinexcelChannelId.STATE_18) //
								.bit(3, SinexcelChannelId.STATE_19) //
								.bit(4, SinexcelChannelId.STATE_20))),

				new FC3ReadRegistersTask(0x0024, Priority.LOW, //
						m(new BitsWordElement(0x0024, this) //
								.bit(0, SinexcelChannelId.STATE_21) //
								.bit(1, SinexcelChannelId.STATE_22) //
								.bit(2, SinexcelChannelId.STATE_23) //
								.bit(3, SinexcelChannelId.STATE_24) //
								.bit(4, SinexcelChannelId.STATE_25) //
								.bit(5, SinexcelChannelId.STATE_26) //
								.bit(6, SinexcelChannelId.STATE_27) //
								.bit(7, SinexcelChannelId.STATE_28) //
								.bit(8, SinexcelChannelId.STATE_29) //
								.bit(9, SinexcelChannelId.STATE_30) //
								.bit(10, SinexcelChannelId.STATE_31) //
								.bit(11, SinexcelChannelId.STATE_32) //
								.bit(12, SinexcelChannelId.STATE_33))),

				new FC3ReadRegistersTask(0x0025, Priority.LOW, //
						m(new BitsWordElement(0x0025, this) //
								.bit(0, SinexcelChannelId.STATE_34) //
								.bit(1, SinexcelChannelId.STATE_35) //
								.bit(2, SinexcelChannelId.STATE_36) //
								.bit(3, SinexcelChannelId.STATE_37) //
								.bit(4, SinexcelChannelId.STATE_38) //
								.bit(5, SinexcelChannelId.STATE_39) //
								.bit(6, SinexcelChannelId.STATE_40) //
								.bit(7, SinexcelChannelId.STATE_41) //
								.bit(8, SinexcelChannelId.STATE_42) //
								.bit(9, SinexcelChannelId.STATE_43) //
								.bit(10, SinexcelChannelId.STATE_44) //
								.bit(11, SinexcelChannelId.STATE_45) //
								.bit(13, SinexcelChannelId.STATE_47) //
								.bit(14, SinexcelChannelId.STATE_48) //
								.bit(15, SinexcelChannelId.STATE_49))),

				new FC3ReadRegistersTask(0x0026, Priority.LOW, //
						m(new BitsWordElement(0x0026, this) //
								.bit(0, SinexcelChannelId.STATE_50) //
								.bit(2, SinexcelChannelId.STATE_52) //
								.bit(3, SinexcelChannelId.STATE_53) //
								.bit(4, SinexcelChannelId.STATE_54))),

				new FC3ReadRegistersTask(0x0027, Priority.LOW, //
						m(new BitsWordElement(0x0027, this) //
								.bit(0, SinexcelChannelId.STATE_55) //
								.bit(1, SinexcelChannelId.STATE_56) //
								.bit(2, SinexcelChannelId.STATE_57) //
								.bit(3, SinexcelChannelId.STATE_58))),

				new FC3ReadRegistersTask(0x0028, Priority.LOW, //
						m(new BitsWordElement(0x0028, this) //
								.bit(0, SinexcelChannelId.STATE_59) //
								.bit(1, SinexcelChannelId.STATE_60) //
								.bit(2, SinexcelChannelId.STATE_61) //
								.bit(3, SinexcelChannelId.STATE_62) //
								.bit(4, SinexcelChannelId.STATE_63) //
								.bit(5, SinexcelChannelId.STATE_64))),

				new FC3ReadRegistersTask(0x002B, Priority.LOW, //
						m(new BitsWordElement(0x002B, this) //
								.bit(0, SinexcelChannelId.STATE_65) //
								.bit(1, SinexcelChannelId.STATE_66) //
								.bit(2, SinexcelChannelId.STATE_67) //
								.bit(3, SinexcelChannelId.STATE_68))),

				new FC3ReadRegistersTask(0x002C, Priority.LOW, //
						m(new BitsWordElement(0x002C, this) //
								.bit(0, SinexcelChannelId.STATE_69) //
								.bit(1, SinexcelChannelId.STATE_70) //
								.bit(2, SinexcelChannelId.STATE_71) //
								.bit(3, SinexcelChannelId.STATE_72) //
								.bit(4, SinexcelChannelId.STATE_73))),

				new FC3ReadRegistersTask(0x002F, Priority.LOW, //
						m(new BitsWordElement(0x002F, this) //
								.bit(0, SinexcelChannelId.STATE_74))));
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value().asString() //
				+ "|L:" + this.getActivePower().value().asString() //
				+ "|Allowed:"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER).value().asStringWithoutUnit() + ";"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER).value().asString() //
				+ "|" + this.getGridMode().value().asOptionString();
	}

	@Override
	public Constraint[] getStaticConstraints() {
		if (!battery.getReadyForWorking().value().orElse(false)) {
			return new Constraint[] { //
					this.createPowerConstraint("Battery is not ready", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0), //
					this.createPowerConstraint("Battery is not ready", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) //
			};
		} else {
			return Power.NO_CONSTRAINTS;
		}
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		switch (this.inverterState) {
		case ON:
			/*
			 * Inverter State is ON
			 */
			IntegerWriteChannel setActivePower = this.channel(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS);
			IntegerWriteChannel setReactivePower = this
					.channel(ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_EQUALS);

			int reactiveValue = reactivePower / 100;
			setReactivePower.setNextWriteValue(reactiveValue);

			int activeValue = activePower / 100;
			setActivePower.setNextWriteValue(activeValue);

			// Energy saving mode
			if (activePower == 0 && reactivePower == 0 && this.isStateOn() == true) {
				this.counterOff++;
				if (this.counterOff == 47) {
					this.inverterOff();
					this.counterOff = 0;
				}

			} else if ((activePower != 0 || reactivePower != 0) && this.isStateOn() == false) {
				this.counterOn++;
				if (this.counterOn == 47) {
					this.inverterOn();
					this.counterOn = 0;
				}
			}
			break;

		case OFF:
			/*
			 * Inverter State is OFF
			 */
			if (this.isStateOn() == true) {
				this.inverterOff();
			} else {
				return;
			}
			break;
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.setBatteryRanges();
			break;
		}
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return 100;
	}

	public IntegerWriteChannel getDischargeMinVoltageChannel() {
		return this.channel(SinexcelChannelId.DIS_MIN_V);
	}

	public IntegerWriteChannel getDischargeMaxAmpereChannel() {
		return this.channel(SinexcelChannelId.DIS_MAX_A);
	}

	public IntegerWriteChannel getChargeMaxVoltageChannel() {
		return this.channel(SinexcelChannelId.CHA_MAX_V);
	}

	public IntegerWriteChannel getChargeMaxAmpereChannel() {
		return this.channel(SinexcelChannelId.CHA_MAX_A);
	}

	public IntegerWriteChannel getEnLimitChannel() {
		return this.channel(SinexcelChannelId.EN_LIMIT);
	}

	public IntegerWriteChannel getBatterySocChannel() {
		return this.channel(SinexcelChannelId.BAT_SOC);
	}

	public IntegerWriteChannel getBatterySohChannel() {
		return this.channel(SinexcelChannelId.BAT_SOH);
	}

	public IntegerWriteChannel getBatteryTempChannel() {
		return this.channel(SinexcelChannelId.BAT_TEMP);
	}

	public IntegerWriteChannel getMinimalCellVoltage() {
		return this.channel(SinexcelChannelId.BAT_MIN_CELL_VOLTAGE);
	}

	public IntegerWriteChannel getVoltage() {
		return this.channel(SinexcelChannelId.BAT_VOLTAGE);
	}

}
