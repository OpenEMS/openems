package io.openems.edge.ess.sinexcel;

import java.time.LocalDateTime;
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

import io.openems.common.channel.AccessMode;
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
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
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
		property = { EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE }) //
public class EssSinexcel extends AbstractOpenemsModbusComponent
		implements SymmetricEss, ManagedSymmetricEss, EventHandler, OpenemsComponent, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(EssSinexcel.class);

	public static final int MAX_APPARENT_POWER = 30_000;
	public static final int DEFAULT_UNIT_ID = 1;

	public int maxApparentPower;
	private InverterState inverterState;
	private Battery battery;
	public LocalDateTime timeForSystemInitialization = null;

	protected int SLOW_CHARGE_VOLTAGE = 4370; // for new batteries - 3940
	protected int FLOAT_CHARGE_VOLTAGE = 4370; // for new batteries - 3940

	private int a = 0;
	private int counterOn = 0;
	private int counterOff = 0;

	// State-Machines
	private final StateMachine stateMachine;

	@Reference
	protected ComponentManager componentManager;

	protected Config config;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private Power power;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setBattery(Battery battery) {
		this.battery = battery;
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled(), DEFAULT_UNIT_ID, this.cm, "Modbus",
				config.modbus_id());
		this.config = config;
		this.inverterState = config.InverterState();

		// initialize the connection to the battery
		this.initializeBattery(config.battery_id());

		this.SLOW_CHARGE_VOLTAGE = config.toppingCharge();
		this.FLOAT_CHARGE_VOLTAGE = config.toppingCharge();

		// this.getNoOfCells();
		this.resetDcAcEnergy();
		this.inverterOn();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public EssSinexcel() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				SinexcelChannelId.values() //
		);
		this._setMaxApparentPower(EssSinexcel.MAX_APPARENT_POWER);
		this.stateMachine = new StateMachine(this);
	}

//	private void getNoOfCells() throws OpenemsNamedException {
//		Battery bms = this.componentManager.getComponent(this.config.battery_id());
//		this.numberOfSlaves = (int) bms.getComponentContext().getProperties().get("numberOfSlaves");
//	}

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

		this.battery.getSocChannel().onChange((oldValue, newValue) -> {
			this._setSoc(newValue.get());
			this.channel(SinexcelChannelId.BAT_SOC).setNextValue(newValue.get());
		});

		this.battery.getVoltageChannel().onChange((oldValue, newValue) -> {
			this.channel(SinexcelChannelId.BAT_VOLTAGE).setNextValue(newValue.get());
		});

		this.battery.getMinCellVoltageChannel().onChange((oldValue, newValue) -> {
			this._setMinCellVoltage(newValue.get());
		});
	}

	/**
	 * Sets the Battery Ranges. Executed on TOPIC_CYCLE_AFTER_PROCESS_IMAGE.
	 * 
	 * @throws OpenemsNamedException
	 */
	private void setBatteryRanges() throws OpenemsNamedException {
		if (battery == null) {
			return;
		}

		int disMaxA = battery.getDischargeMaxCurrent().orElse(0);
		int chaMaxA = battery.getChargeMaxCurrent().orElse(0);
		int disMinV = battery.getDischargeMinVoltage().orElse(0);
		int chaMaxV = battery.getChargeMaxVoltage().orElse(0);

		// Sinexcel range for Max charge/discharge current is 0A to 90A,
		if (chaMaxA > 90) {
			{
				IntegerWriteChannel setChaMaxA = this.channel(SinexcelChannelId.CHARGE_MAX_A);
				setChaMaxA.setNextWriteValue(900);
			}
		} else {
			{
				IntegerWriteChannel setChaMaxA = this.channel(SinexcelChannelId.CHARGE_MAX_A);
				setChaMaxA.setNextWriteValue(chaMaxA * 10);
			}
		}

		if (disMaxA > 90) {
			{
				IntegerWriteChannel setDisMaxA = this.channel(SinexcelChannelId.DISCHARGE_MAX_A);
				setDisMaxA.setNextWriteValue(900);
			}
		} else {
			{
				IntegerWriteChannel setDisMaxA = this.channel(SinexcelChannelId.DISCHARGE_MAX_A);
				setDisMaxA.setNextWriteValue(disMaxA * 10);
			}
		}
		{
			IntegerWriteChannel setDisMinV = this.channel(SinexcelChannelId.DISCHARGE_MIN_V);
			setDisMinV.setNextWriteValue(disMinV * 10);
		}
		{
			IntegerWriteChannel setChaMaxV = this.channel(SinexcelChannelId.CHARGE_MAX_V);
			setChaMaxV.setNextWriteValue(chaMaxV * 10);
		}
		final double EFFICIENCY_FACTOR = 0.9;
		this._setAllowedChargePower((int) (chaMaxA * chaMaxV * -1 * EFFICIENCY_FACTOR));
		this._setAllowedDischargePower((int) (disMaxA * disMinV * EFFICIENCY_FACTOR));
	}

	/**
	 * Starts the inverter.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	public void inverterOn() throws OpenemsNamedException {
		EnumWriteChannel setdataModOnCmd = this.channel(SinexcelChannelId.MOD_ON_CMD);
		setdataModOnCmd.setNextWriteValue(FalseTrue.TRUE); // true = START
	}

	/**
	 * Stops the inverter.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	public void inverterOff() throws OpenemsNamedException {
		EnumWriteChannel setdataModOffCmd = this.channel(SinexcelChannelId.MOD_OFF_CMD);
		setdataModOffCmd.setNextWriteValue(FalseTrue.TRUE); // true = STOP
	}

	/**
	 * Resets DC/AC energy values to zero.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	public void resetDcAcEnergy() throws OpenemsNamedException {
		IntegerWriteChannel chargeEnergy = this.channel(SinexcelChannelId.SET_ANALOG_CHARGE_ENERGY);
		chargeEnergy.setNextWriteValue(0);
		IntegerWriteChannel dischargeEnergy = this.channel(SinexcelChannelId.SET_ANALOG_DISCHARGE_ENERGY);
		dischargeEnergy.setNextWriteValue(0);
		IntegerWriteChannel chargeDcEnergy = this.channel(SinexcelChannelId.SET_ANALOG_DC_CHARGE_ENERGY);
		chargeDcEnergy.setNextWriteValue(0);
		IntegerWriteChannel dischargeDcEnergy = this.channel(SinexcelChannelId.SET_ANALOG_DC_DISCHARGE_ENERGY);
		dischargeDcEnergy.setNextWriteValue(0);
	}

	/**
	 * Executes a Soft-Start. Sets the internal DC relay. Once this register is set
	 * to 1, the PCS will start the soft-start procedure, otherwise, the PCS will do
	 * nothing on the DC input Every time the PCS is powered off, this register will
	 * be cleared to 0. In some particular cases, the BMS wants to re-softstart, the
	 * EMS should actively clear this register to 0, after BMS soft-started, set it
	 * to 1 again.
	 *
	 * @throws OpenemsNamedException on error
	 */
	public void softStart(boolean switchOn) throws OpenemsNamedException {
		this.logInfo(this.log, "[In boolean soft start method]");
		IntegerWriteChannel setDcRelay = this.channel(SinexcelChannelId.SET_INTERN_DC_RELAY);
		if (switchOn) {
			setDcRelay.setNextWriteValue(1);
		} else {
			setDcRelay.setNextWriteValue(0);
		}
	}

	/**
	 * At first the PCS needs a stop command, then is required to remove the AC
	 * connection, after that the Grid OFF command.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	public void islandOn() throws OpenemsNamedException {
		IntegerWriteChannel setAntiIslanding = this.channel(SinexcelChannelId.SET_ANTI_ISLANDING);
		setAntiIslanding.setNextWriteValue(0); // Disabled
		IntegerWriteChannel setdataGridOffCmd = this.channel(SinexcelChannelId.OFF_GRID_CMD);
		setdataGridOffCmd.setNextWriteValue(1); // Stop
	}

	/**
	 * At first the PCS needs a stop command, then is required to plug in the AC
	 * connection, after that the Grid ON command.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	public void islandingOff() throws OpenemsNamedException {
		IntegerWriteChannel setAntiIslanding = this.channel(SinexcelChannelId.SET_ANTI_ISLANDING);
		setAntiIslanding.setNextWriteValue(1); // Enabled
		IntegerWriteChannel setdataGridOnCmd = this.channel(SinexcelChannelId.OFF_GRID_CMD);
		setdataGridOnCmd.setNextWriteValue(1); // Start
	}

	public void doHandlingSlowFloatVoltage() throws OpenemsNamedException {
		// System.out.println("Upper voltage : " +
		// this.channel(SinexcelChannelId.UPPER_VOLTAGE_LIMIT).value().asStringWithoutUnit());
		IntegerWriteChannel setSlowChargeVoltage = this.channel(SinexcelChannelId.SET_SLOW_CHARGE_VOLTAGE);
		setSlowChargeVoltage.setNextWriteValue(this.SLOW_CHARGE_VOLTAGE);
		IntegerWriteChannel setFloatChargeVoltage = this.channel(SinexcelChannelId.SET_FLOAT_CHARGE_VOLTAGE);
		setFloatChargeVoltage.setNextWriteValue(this.FLOAT_CHARGE_VOLTAGE);
	}

	public boolean faultIslanding() {
		StateChannel i = this.channel(SinexcelChannelId.STATE_4);
		Optional<Boolean> islanding = i.getNextValue().asOptional();
		return islanding.isPresent() && islanding.get();
	}

	public boolean stateOnOff() {
		StateChannel v = this.channel(SinexcelChannelId.STATE_18);
		Optional<Boolean> stateOff = v.getNextValue().asOptional();
		return stateOff.isPresent() && stateOff.get();
	}

//	/**
//	 * Is Grid Shutdown?.
//	 * 
//	 * @return true if grid is shut down
//	 */
//	public boolean faultIslanding() {
//		StateChannel channel = this.channel(SinexcelChannelId.STATE_4);
//		Optional<Boolean> islanding = channel.getNextValue().asOptional();
//		return islanding.isPresent() && islanding.get();
//	}
//
//	/**
//	 * Is inverter state ON?.
//	 * 
//	 * @return true if inverter is in ON-State
//	 */
//	public boolean isStateOn() {
//		StateChannel channel = this.channel(SinexcelChannelId.STATE_18);
//		Optional<Boolean> stateOff = channel.getNextValue().asOptional();
//		return stateOff.isPresent() && stateOff.get();
//	}

	// SF: was commented before
//	public boolean stateOn() {
//		StateChannel v = this.channel(SinexcelChannelId.Sinexcel_STATE_9);
//		Optional<Boolean> stateOff = v.getNextValue().asOptional(); 
//		return stateOff.isPresent() && stateOff.get();
//	}

//------------------------------------------------------------------------------------------------------------------	

	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //

				new FC6WriteRegisterTask(0x028A, //
						m(SinexcelChannelId.MOD_ON_CMD, new UnsignedWordElement(0x028A))),
				new FC6WriteRegisterTask(0x028B, //
						m(SinexcelChannelId.MOD_OFF_CMD, new UnsignedWordElement(0x028B))),
				new FC6WriteRegisterTask(0x028C, //
						m(SinexcelChannelId.CLEAR_FAILURE_CMD, new UnsignedWordElement(0x028C))),
				new FC6WriteRegisterTask(0x028D, //
						m(SinexcelChannelId.ON_GRID_CMD, new UnsignedWordElement(0x028D))),
				new FC6WriteRegisterTask(0x028E, //
						m(SinexcelChannelId.OFF_GRID_CMD, new UnsignedWordElement(0x028E))),

				new FC6WriteRegisterTask(0x0290, // FIXME: not documented!
						m(SinexcelChannelId.SET_INTERN_DC_RELAY, new UnsignedWordElement(0x0290))),

				new FC6WriteRegisterTask(0x0087, //
						m(SinexcelChannelId.SET_ACTIVE_POWER, new SignedWordElement(0x0087))), // in 100 W
				new FC6WriteRegisterTask(0x0088,
						m(SinexcelChannelId.SET_REACTIVE_POWER, new SignedWordElement(0x0088))), // in 100 var
// --
				new FC6WriteRegisterTask(0x032B, //
						m(SinexcelChannelId.CHARGE_MAX_A, new UnsignedWordElement(0x032B))), //
				new FC6WriteRegisterTask(0x032C, //
						m(SinexcelChannelId.DISCHARGE_MAX_A, new UnsignedWordElement(0x032C))), //

				new FC6WriteRegisterTask(0x0329,
						m(SinexcelChannelId.SET_SLOW_CHARGE_VOLTAGE, new UnsignedWordElement(0x0329))),
				new FC6WriteRegisterTask(0x0328,
						m(SinexcelChannelId.SET_FLOAT_CHARGE_VOLTAGE, new UnsignedWordElement(0x0328))),

				new FC6WriteRegisterTask(0x032E, m(SinexcelChannelId.CHARGE_MAX_V, new UnsignedWordElement(0x032E))),
				new FC6WriteRegisterTask(0x032D, m(SinexcelChannelId.DISCHARGE_MIN_V, new UnsignedWordElement(0x032D))),

				new FC16WriteRegistersTask(0x007E,
						m(SinexcelChannelId.SET_ANALOG_CHARGE_ENERGY, new UnsignedDoublewordElement(0x007E))),
				// new FC6WriteRegisterTask(0x007F,
				// m(SinexcelChannelId.SET_ANALOG_CHARGE_ENERGY, new
				// UnsignedWordElement(0x007F))),

				new FC16WriteRegistersTask(0x0080,
						m(SinexcelChannelId.SET_ANALOG_DISCHARGE_ENERGY, new UnsignedDoublewordElement(0x0080))),
				// new FC6WriteRegisterTask(0x0081,
				// m(SinexcelChannelId.SET_ANALOG_DISCHARGE_ENERGY, new
				// UnsignedWordElement(0x0081))),

				new FC16WriteRegistersTask(0x0090,
						m(SinexcelChannelId.SET_ANALOG_DC_CHARGE_ENERGY, new UnsignedDoublewordElement(0x0090))),
				// new FC6WriteRegisterTask(0x0091,
				// m(SinexcelChannelId.SET_ANALOG_DC_CHARGE_ENERGY, new
				// UnsignedWordElement(0x0091))),

				new FC16WriteRegistersTask(0x0092,
						m(SinexcelChannelId.SET_ANALOG_DC_DISCHARGE_ENERGY, new UnsignedDoublewordElement(0x0092))),
				// new FC6WriteRegisterTask(0x0093,
				// m(SinexcelChannelId.SET_ANALOG_DC_DISCHARGE_ENERGY, new
				// UnsignedWordElement(0x0093))),

//----------------------------------------------------------READ------------------------------------------------------
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

				new FC3ReadRegistersTask(0x007E, Priority.HIGH,
						m(SinexcelChannelId.ANALOG_CHARGE_ENERGY, new UnsignedDoublewordElement(0x007E)), // 1
						m(SinexcelChannelId.ANALOG_DISCHARGE_ENERGY, new UnsignedDoublewordElement(0x0080)), // 1
						new DummyRegisterElement(0x0082, 0x0083),
						m(SinexcelChannelId.TEMPERATURE, new SignedWordElement(0x0084)),
						new DummyRegisterElement(0x0085, 0x008C), //
						m(SinexcelChannelId.DC_POWER, new SignedWordElement(0x008D),
								ElementToChannelConverter.SCALE_FACTOR_1),
						new DummyRegisterElement(0x008E, 0x008F), //
						m(SinexcelChannelId.ANALOG_DC_CHARGE_ENERGY, new UnsignedDoublewordElement(0x0090)), // 1
						m(SinexcelChannelId.ANALOG_DC_DISCHARGE_ENERGY, new UnsignedDoublewordElement(0x0092))), // 1

				new FC3ReadRegistersTask(0x0248, Priority.HIGH, //
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedWordElement(0x0248), //
								ElementToChannelConverter.SCALE_FACTOR_1),
						new DummyRegisterElement(0x0249),
						m(SinexcelChannelId.FREQUENCY, new SignedWordElement(0x024A),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						new DummyRegisterElement(0x024B, 0x0254), //
						m(SinexcelChannelId.DC_CURRENT, new SignedWordElement(0x0255),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						new DummyRegisterElement(0x0256), //
						m(SinexcelChannelId.DC_VOLTAGE, new UnsignedWordElement(0x0257),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),

				new FC3ReadRegistersTask(0x024E, Priority.HIGH, //
						m(SymmetricEss.ChannelId.REACTIVE_POWER, new SignedWordElement(0x024E))), //

				new FC3ReadRegistersTask(0x0260, Priority.HIGH, //
						m(SinexcelChannelId.SINEXCEL_STATE, new UnsignedWordElement(0x0260))), //

				new FC3ReadRegistersTask(0x0001, Priority.ONCE, //
						m(SinexcelChannelId.MODEL, new StringWordElement(0x0001, 16)), //
						m(SinexcelChannelId.SERIAL, new StringWordElement(0x0011, 8))), //

//				new FC3ReadRegistersTask(0x0074, Priority.LOW,
//						m(EssSinexcel.SinexcelChannelId.Analog_ApparentPower_L1, new SignedWordElement(0x0074),	// L1 // kVA // 100
//								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), 
//						m(EssSinexcel.SinexcelChannelId.Analog_ApparentPower_L2, new SignedWordElement(0x0075), // L2 // kVA // 100
//								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
//						m(EssSinexcel.SinexcelChannelId.Analog_ApparentPower_L3, new SignedWordElement(0x0076), // L3 // kVA // 100
//								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)),

				new FC3ReadRegistersTask(0x0220, Priority.ONCE,
//						m(EssSinexcel.SinexcelChannelId.Manufacturer, new StringWordElement(0x01F8, 16)), // String // Line109
//						m(EssSinexcel.SinexcelChannelId.Model_2, new StringWordElement(0x0208, 16)), // String (32Char) // line110
						m(SinexcelChannelId.VERSION, new StringWordElement(0x0220, 8))), // String (16Char) //
//						m(EssSinexcel.SinexcelChannelId.Serial_Number, new StringWordElement(0x0228, 16))), // String (32Char)// Line113

				new FC3ReadRegistersTask(0x032D, Priority.LOW,
						m(SinexcelChannelId.LOWER_VOLTAGE_LIMIT, new UnsignedWordElement(0x032D), // uint 16 //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(SinexcelChannelId.UPPER_VOLTAGE_LIMIT, new UnsignedWordElement(0x032E), // uint16 //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),

//				new FC3ReadRegistersTask(0x006B, Priority.LOW,
//						m(EssSinexcel.SinexcelChannelId.Analog_GridCurrent_Freq, new UnsignedWordElement(0x006B),
//								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), // 10
//				new FC3ReadRegistersTask(0x006E, Priority.LOW,
//						m(EssSinexcel.SinexcelChannelId.Analog_ActivePower_Rms_Value_L1, new SignedWordElement(0x006E),		// L1 // kW //100
//								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)), 
//				new FC3ReadRegistersTask(0x006F, Priority.LOW,
//						m(EssSinexcel.SinexcelChannelId.Analog_ActivePower_Rms_Value_L2, new SignedWordElement(0x006F),		// L2 // kW // 100
//								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)), 
//				new FC3ReadRegistersTask(0x0070, Priority.LOW,
//						m(EssSinexcel.SinexcelChannelId.Analog_ActivePower_Rms_Value_L3, new SignedWordElement(0x0070),		// L3 // kW // 100
//								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)), 
//				new FC3ReadRegistersTask(0x0071, Priority.LOW,
//						m(EssSinexcel.SinexcelChannelId.Analog_ReactivePower_Rms_Value_L1, new SignedWordElement(0x0071), 	// L1 // kVAr // 100
//								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)),
//				new FC3ReadRegistersTask(0x0072, Priority.LOW,
//						m(EssSinexcel.SinexcelChannelId.Analog_ReactivePower_Rms_Value_L2, new SignedWordElement(0x0072),	// L2 // kVAr // 100
//								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)), 
//				new FC3ReadRegistersTask(0x0073, Priority.LOW,
//						m(EssSinexcel.SinexcelChannelId.Analog_ReactivePower_Rms_Value_L3, new SignedWordElement(0x0073),	 // L3 // kVAr // 100
//								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)),
//				new FC3ReadRegistersTask(0x0077, Priority.LOW,
//						m(EssSinexcel.SinexcelChannelId.Analog_PF_RMS_Value_L1, new SignedWordElement(0x0077),	// 100
//								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)), 
//				new FC3ReadRegistersTask(0x0078, Priority.LOW,
//						m(EssSinexcel.SinexcelChannelId.Analog_PF_RMS_Value_L2, new SignedWordElement(0x0078),	// 100
//								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)), 
//				new FC3ReadRegistersTask(0x0079, Priority.LOW,
//						m(EssSinexcel.SinexcelChannelId.Analog_PF_RMS_Value_L3, new SignedWordElement(0x0079),	// 100
//								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)), 
//				new FC3ReadRegistersTask(0x007A, Priority.LOW,
//						m(EssSinexcel.SinexcelChannelId.Analog_ActivePower_3Phase, new SignedWordElement(0x007A))), // 1
//				new FC3ReadRegistersTask(0x007B, Priority.LOW,
//						m(EssSinexcel.SinexcelChannelId.Analog_ReactivePower_3Phase, new SignedWordElement(0x007B))), // 1
//				new FC3ReadRegistersTask(0x007C, Priority.LOW,
//						m(EssSinexcel.SinexcelChannelId.Analog_ApparentPower_3Phase, new UnsignedWordElement(0x007C))), // 1
//				new FC3ReadRegistersTask(0x007D, Priority.LOW,
//						m(EssSinexcel.SinexcelChannelId.Analog_PowerFactor_3Phase, new SignedWordElement(0x007D))), // 1
//				new FC3ReadRegistersTask(0x0082, Priority.LOW,
//						m(EssSinexcel.SinexcelChannelId.Analog_REACTIVE_Energy, new UnsignedDoublewordElement(0x0082))), // 1
//				new FC3ReadRegistersTask(0x0082, Priority.LOW,
//						m(EssSinexcel.SinexcelChannelId.Analog_Reactive_Energy_2, new UnsignedDoublewordElement(0x0082))), // 1//Line61
//				new FC3ReadRegistersTask(0x0089, Priority.HIGH,
//						m(EssSinexcel.SinexcelChannelId.Target_OffGrid_Voltage, new UnsignedWordElement(0x0089))), // Range: -0,1 ... 0,1 (to rated Voltage)// 100
//				new FC3ReadRegistersTask(0x008A, Priority.HIGH,
//						m(EssSinexcel.SinexcelChannelId.Target_OffGrid_Frequency, new SignedWordElement(0x008A))), // Range: -2... 2Hz//100

//----------------------------------------------------------START and STOP--------------------------------------------------------------------				
//				new FC3ReadRegistersTask(0x023A, Priority.LOW, //
//						m(EssSinexcel.SinexcelChannelId.SUNSPEC_DID_0103, new UnsignedWordElement(0x023A))), //
//
//				new FC3ReadRegistersTask(0x028D, Priority.LOW,
//						m(EssSinexcel.SinexcelChannelId.GRID_ON_CMD, new UnsignedWordElement(0x028D))),
//				new FC3ReadRegistersTask(0x028E, Priority.LOW,
//						m(EssSinexcel.SinexcelChannelId.GRID_OFF_CMD, new UnsignedWordElement(0x028E))),
//				new FC3ReadRegistersTask(0x0316, Priority.LOW,
//						m(EssSinexcel.SinexcelChannelId.ANTI_ISLANDING, new UnsignedWordElement(0x0316))),

//----------------------------------------------------------------------------------------------------------

//				new FC3ReadRegistersTask(0x024C, Priority.LOW, //
//						m(EssSinexcel.SinexcelChannelId.AC_Apparent_Power, new SignedWordElement(0x024C))), // int16 // Line134// Magnification = 0

				// Magnification = 0

//-----------------------------------------EVENT Bitfield 32------------------------------------------------------------		
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
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|Allowed:"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER).value().asStringWithoutUnit() + ";"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER).value().asString() //
				+ "|" + this.getGridModeChannel().value().asOptionString();
	}

	@Override
	public Constraint[] getStaticConstraints() throws OpenemsNamedException {
		if (battery.getStartStop() != StartStop.START) {
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
			IntegerWriteChannel setActivePower = this.channel(SinexcelChannelId.SET_ACTIVE_POWER);
			setActivePower.setNextWriteValue(activePower / 100);

			IntegerWriteChannel setReactivePower = this.channel(SinexcelChannelId.SET_REACTIVE_POWER);
			setReactivePower.setNextWriteValue(reactivePower / 100);

			if (this.stateOnOff() == false) {
				a = 1;
			}

			if (this.stateOnOff() == true) {
				a = 0;
			}

			if (activePower == 0 && reactivePower == 0 && a == 0) {
				this.counterOff++;
				if (this.counterOff == 48) {
					this.inverterOff();
					this.counterOff = 0;
				}

			} else if ((activePower != 0 || reactivePower != 0) && a == 1) {
				this.counterOn++;
				if (this.counterOn == 48) {
					this.inverterOn();
					this.counterOn = 0;
				}
			}
			break;

		case OFF:
			if (this.stateOnOff() == true) {
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
//		boolean island = faultIslanding();
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			try {
				this.setBatteryRanges();
				this.doHandlingSlowFloatVoltage();
				this.stateMachine.run();
			} catch (OpenemsNamedException e) {
				this.logError(this.log, "EventHandler failed: " + e.getMessage());
			}

//			if(island = true) {
//				islandingOn();
//			}
//			else if(island = false) {
//				islandingOff();
//			}

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
		return this.channel(SinexcelChannelId.DISCHARGE_MIN_V);
	}

	public IntegerWriteChannel getDischargeMaxAmpereChannel() {
		return this.channel(SinexcelChannelId.DISCHARGE_MAX_A);
	}

	public IntegerWriteChannel getChargeMaxVoltageChannel() {
		return this.channel(SinexcelChannelId.CHARGE_MAX_V);
	}

	public IntegerWriteChannel getChargeMaxAmpereChannel() {
		return this.channel(SinexcelChannelId.CHARGE_MAX_A);
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

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode) //
		);
	}
}
