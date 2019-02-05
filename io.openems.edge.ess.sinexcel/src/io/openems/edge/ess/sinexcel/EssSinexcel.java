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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.Sinexcel", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE) //
//
public class EssSinexcel extends AbstractOpenemsModbusComponent
		implements SymmetricEss, ManagedSymmetricEss, EventHandler, OpenemsComponent {
//
//	private final Logger log = LoggerFactory.getLogger(EssSinexcel.class);
//
	public static final int DEFAULT_UNIT_ID = 1; 	// Konstanten
	public int maxApparentPower;
	private InverterState InverterState;
	private Battery battery;
	public LocalDateTime timeForSystemInitialization = null;

	private static final int DISABLED_ANTI_ISLANDING = 0;
	private static final int ENABLED_ANTI_ISLANDING = 1;
	private static final int START = 1;
	private static final int STOP = 1;
	private static final int SLOW_CHARGE_VOLTAGE = 4370; // Slow and Float Charge Voltage must be the same for the Lithium Ionbattery.
	private static final int FLOAT_CHARGE_VOLTAGE = 4370;
	private static final int MINIMAL_POWER = 1;
	private int counterOn = 0;
	private int counterOff = 0;
		
	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private Power power;

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.enabled(), DEFAULT_UNIT_ID, this.cm, "Modbus",
				config.modbus_id());
		this.InverterState = config.InverterState();
		if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "battery", config.battery_id())) {
			return;
		}

		softStart();
		doChannelMapping();
		resetDcAcEnergy();
		inverterOn();
		doHandlingSlowFloatVoltage();
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setBattery(Battery battery) {
		this.battery = battery;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public EssSinexcel() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));

	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus); // Bridge Modbus
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		SUNSPEC_DID_0103(new Doc()), //
		SET_INTERN_DC_RELAY(new Doc().unit(Unit.NONE)), 
		SETDATA_MOD_ON_CMD(new Doc().unit(Unit.ON_OFF)),
		SETDATA_MOD_OFF_CMD(new Doc().unit(Unit.ON_OFF)), 
		SETDATA_GRID_ON_CMD(new Doc().unit(Unit.ON_OFF)),
		SETDATA_GRID_OFF_CMD(new Doc().unit(Unit.ON_OFF)), 
		SET_ANTI_ISLANDING(new Doc().unit(Unit.ON_OFF)),
		SET_CHARGE_DISCHARGE_ACTIVE(new Doc().unit(Unit.KILOWATT)), //
		SET_CHARGE_DISCHARGE_REACTIVE(new Doc().unit(Unit.KILO_VOLT_AMPERE_REACTIVE)), //
		SET_CHARGE_CURRENT(new Doc().unit(Unit.AMPERE)), 
		SET_DISCHARGE_CURRENT(new Doc().unit(Unit.AMPERE)),
		SET_SLOW_CHARGE_VOLTAGE(new Doc().unit(Unit.VOLT)),
		SET_FLOAT_CHARGE_VOLTAGE(new Doc().unit(Unit.VOLT)),
		SET_UPPER_VOLTAGE(new Doc().unit(Unit.VOLT)), 
		SET_LOWER_VOLTAGE(new Doc().unit(Unit.VOLT)),
		SET_ANALOG_CHARGE_Energy(new Doc().unit(Unit.KILOWATT_HOURS)),
		SET_ANALOG_DISCHARGE_Energy(new Doc().unit(Unit.KILOWATT_HOURS)),
		SET_ANALOG_DC_CHARGE_Energy(new Doc().unit(Unit.KILOWATT_HOURS)),
		SET_ANALOG_DC_DISCHARGE_Energy(new Doc().unit(Unit.KILOWATT_HOURS)),
		BAT_MIN_CELL_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)),
		BAT_VOLTAGE(new Doc().unit(Unit.VOLT)),
		BAT_TEMP(new Doc().unit(Unit.DEGREE_CELSIUS)),
		BAT_SOC(new Doc().unit(Unit.PERCENT)),
		BAT_SOH(new Doc().unit(Unit.PERCENT)),
		DEBUG_DIS_MIN_V(new Doc().unit(Unit.VOLT)), @SuppressWarnings("unchecked")
		DIS_MIN_V(new Doc().unit(Unit.VOLT).onInit(channel -> { //
			// on each setNextWrite to the channel -> store the value in the DEBUG-channel
			((WriteChannel<Integer>) channel).onSetNextWrite(value -> {
				channel.getComponent().channel(ChannelId.DEBUG_DIS_MIN_V).setNextValue(value);
			});
		})),
		DEBUG_CHA_MAX_V(new Doc().unit(Unit.VOLT)), @SuppressWarnings("unchecked")
		CHA_MAX_V(new Doc().unit(Unit.VOLT).onInit(channel -> { //
			// on each setNextWrite to the channel -> store the value in the DEBUG-channel
			((WriteChannel<Integer>) channel).onSetNextWrite(value -> {
				channel.getComponent().channel(ChannelId.DEBUG_CHA_MAX_V).setNextValue(value);
			});
		})),

		DEBUG_DIS_MAX_A(new Doc().unit(Unit.AMPERE)), @SuppressWarnings("unchecked")
		DIS_MAX_A(new Doc().unit(Unit.AMPERE).onInit(channel -> { //
			// on each setNextWrite to the channel -> store the value in the DEBUG-channel
			((WriteChannel<Integer>) channel).onSetNextWrite(value -> {
				channel.getComponent().channel(ChannelId.DEBUG_DIS_MAX_A).setNextValue(value);
			});
		})),

		DEBUG_CHA_MAX_A(new Doc().unit(Unit.AMPERE)), @SuppressWarnings("unchecked")
		CHA_MAX_A(new Doc().unit(Unit.AMPERE).onInit(channel -> { //
			// on each setNextWrite to the channel -> store the value in the DEBUG-channel
			((WriteChannel<Integer>) channel).onSetNextWrite(value -> {
				channel.getComponent().channel(ChannelId.DEBUG_CHA_MAX_A).setNextValue(value);
			});
		})),

		DEBUG_EN_LIMIT(new Doc()), @SuppressWarnings("unchecked")
		EN_LIMIT(new Doc().text("new battery limits are activated when EnLimit is 1") //
				.onInit(channel -> { //
					// on each setNextWrite to the channel -> store the value in the DEBUG-channel
					((WriteChannel<Integer>) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_EN_LIMIT).setNextValue(value);
					});
				})),

		ANTI_ISLANDING(new Doc().unit(Unit.ON_OFF)),
		MOD_ON_CMD(new Doc().unit(Unit.ON_OFF)), //
		MOD_OFF_CMD(new Doc().unit(Unit.ON_OFF)), //
		GRID_ON_CMD(new Doc().unit(Unit.ON_OFF)), //
		GRID_OFF_CMD(new Doc().unit(Unit.ON_OFF)), //
		FREQUENCY(new Doc().unit(Unit.HERTZ)), //
		TEMPERATURE(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		SERIAL(new Doc().unit(Unit.NONE)), 
		MODEL(new Doc().unit(Unit.NONE)), 
		MANUFACTURER(new Doc().unit(Unit.NONE)),
		MODEL_2(new Doc().unit(Unit.NONE)), 
		VERSION(new Doc().unit(Unit.NONE)),
		SERIAL_NUMBER(new Doc().unit(Unit.NONE)),
		ANALOG_CHARGE_ENERGY(new Doc().unit(Unit.KILOWATT_HOURS)),
		ANALOG_DISCHARGE_ENERGY(new Doc().unit(Unit.KILOWATT_HOURS)),
		TARGET_OFFGRID_VOLTAGE(new Doc().unit(Unit.NONE)), 
		TARGET_OFFGRID_FREQUENCY(new Doc().unit(Unit.HERTZ)),
		ANALOG_DC_CHARGE_ENERGY(new Doc().unit(Unit.KILO_VOLT_AMPERE)),
		ANALOG_DC_DISCHARGE_ENERGY(new Doc().unit(Unit.KILO_VOLT_AMPERE)),
		AC_APPARENT_POWER(new Doc().unit(Unit.VOLT_AMPERE)), //
		AC_REACTIVE_POWER(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		AC_POWER(new Doc().unit(Unit.WATT)), //
		INVOUTVOLT_L1(new Doc().unit(Unit.VOLT)), 
		INVOUTVOLT_L2(new Doc().unit(Unit.VOLT)),
		INVOUTVOLT_L3(new Doc().unit(Unit.VOLT)), 
		INVOUTCURRENT_L1(new Doc().unit(Unit.AMPERE)), //
		INVOUTCURRENT_L2(new Doc().unit(Unit.AMPERE)), //
		INVOUTCURRENT_L3(new Doc().unit(Unit.AMPERE)), //
		DC_POWER(new Doc().unit(Unit.WATT)), //
		DC_CURRENT(new Doc().unit(Unit.AMPERE)), //
		DC_VOLTAGE(new Doc().unit(Unit.VOLT)), //
		SINEXCEL_STATE(new Doc().options(CurrentState.values())), //
		TARGET_ACTIVE_POWER(new Doc().unit(Unit.KILOWATT)), //
		TARGET_REACTIVE_POWER(new Doc().unit(Unit.KILOWATT)), //
		MAX_CHARGE_CURRENT(new Doc().unit(Unit.AMPERE)), 
		MAX_DISCHARGE_CURRENT(new Doc().unit(Unit.AMPERE)),
		LOWER_VOLTAGE_LIMIT(new Doc().unit(Unit.VOLT)), 
		UPPER_VOLTAGE_LIMIT(new Doc().unit(Unit.VOLT)),
		
		SINEXCEL_STATE_1(new Doc().level(Level.INFO).text("OFF")), //
		SINEXCEL_STATE_2(new Doc().level(Level.INFO).text("Sleeping")), //
		SINEXCEL_STATE_3(new Doc().level(Level.INFO).text("Starting")), //
		SINEXCEL_STATE_4(new Doc().level(Level.INFO).text("MPPT")), //
		SINEXCEL_STATE_5(new Doc().level(Level.INFO).text("Throttled")), //
		SINEXCEL_STATE_6(new Doc().level(Level.INFO).text("Shutting down")), //
		SINEXCEL_STATE_7(new Doc().level(Level.INFO).text("Fault")), //
		SINEXCEL_STATE_8(new Doc().level(Level.INFO).text("Standby")), //
		SINEXCEL_STATE_9(new Doc().level(Level.INFO).text("Started")), //

//-----------------------------------EVENT Bitfield 32-----------------------------------
		STATE_0(new Doc().level(Level.FAULT).text("Ground fault")), //
		STATE_1(new Doc().level(Level.WARNING).text("DC over Voltage")), //
		STATE_2(new Doc().level(Level.WARNING).text("AC disconnect open")), //
		STATE_3(new Doc().level(Level.WARNING).text("DC disconnect open")), //
		STATE_4(new Doc().level(Level.WARNING).text("Grid shutdown")), //
		STATE_5(new Doc().level(Level.WARNING).text("Cabinet open")), //
		STATE_6(new Doc().level(Level.WARNING).text("Manual shutdown")), //
		STATE_7(new Doc().level(Level.WARNING).text("Over temperature")), //
		STATE_8(new Doc().level(Level.WARNING).text("AC Frequency above limit")), //
		STATE_9(new Doc().level(Level.WARNING).text("AC Frequnecy under limit")), //
		STATE_10(new Doc().level(Level.WARNING).text("AC Voltage above limit")), //
		STATE_11(new Doc().level(Level.WARNING).text("AC Voltage under limit")), //
		STATE_12(new Doc().level(Level.WARNING).text("Blown String fuse on input")), //
		STATE_13(new Doc().level(Level.WARNING).text("Under temperature")), //
		STATE_14(new Doc().level(Level.WARNING).text("Generic Memory or Communication error (internal)")), //
		STATE_15(new Doc().level(Level.FAULT).text("Hardware test failure")), //
//---------------------------------------------------FAULT LIST------------------------------------------------------------
		STATE_16(new Doc().level(Level.FAULT).text("Fault Status")), //
		STATE_17(new Doc().level(Level.WARNING).text("Alert Status")), //
		STATE_18(new Doc().level(Level.INFO).text("On/Off Status")), //
		STATE_19(new Doc().level(Level.INFO).text("On Grid")), //
		STATE_20(new Doc().level(Level.INFO).text("Off Grid")), //
		STATE_21(new Doc().level(Level.WARNING).text("AC OVP")), //
		STATE_22(new Doc().level(Level.WARNING).text("AC UVP")), //
		STATE_23(new Doc().level(Level.WARNING).text("AC OFP")), //
		STATE_24(new Doc().level(Level.WARNING).text("AC UFP")), //
		STATE_25(new Doc().level(Level.WARNING).text("Grid Voltage Unbalance")), //
		STATE_26(new Doc().level(Level.WARNING).text("Grid Phase reserve")), //
		STATE_27(new Doc().level(Level.INFO).text("Islanding")), //
		STATE_28(new Doc().level(Level.WARNING).text("On/ Off Grid Switching Error")), //
		STATE_29(new Doc().level(Level.WARNING).text("Output Grounding Error")), //
		STATE_30(new Doc().level(Level.WARNING).text("Output Current Abnormal")), //
		STATE_31(new Doc().level(Level.WARNING).text("Grid Phase Lock Fails")), //
		STATE_32(new Doc().level(Level.WARNING).text("Internal Air Over-Temp")), //
		STATE_33(new Doc().level(Level.WARNING).text("Zeitüberschreitung der Netzverbindung")), //
		STATE_34(new Doc().level(Level.INFO).text("EPO")), //
		STATE_35(new Doc().level(Level.FAULT).text("HMI Parameters Fault")), //
		STATE_36(new Doc().level(Level.WARNING).text("DSP Version Error")), //
		STATE_37(new Doc().level(Level.WARNING).text("CPLD Version Error")), //
		STATE_38(new Doc().level(Level.WARNING).text("Hardware Version Error")), //
		STATE_39(new Doc().level(Level.WARNING).text("Communication Error")), //
		STATE_40(new Doc().level(Level.WARNING).text("AUX Power Error")), //
		STATE_41(new Doc().level(Level.FAULT).text("Fan Failure")), //
		STATE_42(new Doc().level(Level.WARNING).text("BUS Over Voltage")), //
		STATE_43(new Doc().level(Level.WARNING).text("BUS Low Voltage")), //
		STATE_44(new Doc().level(Level.WARNING).text("BUS Voltage Unbalanced")), //
		STATE_45(new Doc().level(Level.WARNING).text("AC Soft Start Failure")), //
		STATE_46(new Doc().level(Level.WARNING).text("Reserved")), //
		STATE_47(new Doc().level(Level.WARNING).text("Output Voltage Abnormal")), //
		STATE_48(new Doc().level(Level.WARNING).text("Output Current Unbalanced")), //
		STATE_49(new Doc().level(Level.WARNING).text("Over Temperature of Heat Sink")), //
		STATE_50(new Doc().level(Level.WARNING).text("Output Overload")), //
		STATE_51(new Doc().level(Level.WARNING).text("Reserved")), //
		STATE_52(new Doc().level(Level.WARNING).text("AC Breaker Short-Circuit")), //
		STATE_53(new Doc().level(Level.WARNING).text("Inverter Start Failure")), //
		STATE_54(new Doc().level(Level.WARNING).text("AC Breaker is open")), //
		STATE_55(new Doc().level(Level.WARNING).text("EE Reading Error 1")), //
		STATE_56(new Doc().level(Level.WARNING).text("EE Reading Error 2")), //
		STATE_57(new Doc().level(Level.FAULT).text("SPD Failure  ")), //
		STATE_58(new Doc().level(Level.WARNING).text("Inverter over load")), //
		STATE_59(new Doc().level(Level.INFO).text("DC Charging")), //
		STATE_60(new Doc().level(Level.INFO).text("DC Discharging")), //
		STATE_61(new Doc().level(Level.INFO).text("Battery fully charged")), //
		STATE_62(new Doc().level(Level.INFO).text("Battery empty")), //
		STATE_63(new Doc().level(Level.FAULT).text("Fault Status")), //
		STATE_64(new Doc().level(Level.WARNING).text("Alert Status")), //
		STATE_65(new Doc().level(Level.WARNING).text("DC input OVP")), //
		STATE_66(new Doc().level(Level.WARNING).text("DC input UVP")), //
		STATE_67(new Doc().level(Level.WARNING).text("DC Groundig Error")), //
		STATE_68(new Doc().level(Level.WARNING).text("BMS alerts")), //
		STATE_69(new Doc().level(Level.FAULT).text("DC Soft-Start failure")), //
		STATE_70(new Doc().level(Level.WARNING).text("DC relay short-circuit")), //
		STATE_71(new Doc().level(Level.WARNING).text("DC realy short open")), //
		STATE_72(new Doc().level(Level.WARNING).text("Battery power over load")), //
		STATE_73(new Doc().level(Level.FAULT).text("BUS start fails")), //
		STATE_74(new Doc().level(Level.WARNING).text("DC OCP"));//

//----------------------------------------------------------------------------------------------------------------------		
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;

		}
	}

	private void doChannelMapping() {
		
		this.channel(ChannelId.STATE_19).onChange(value -> {
			this.getGridMode().setNextValue(GridMode.UNDEFINED);
			@SuppressWarnings("unchecked")
			Optional<Boolean> v1 = (Optional<Boolean>) value.asOptional();
			if(!v1.isPresent()) {
				return;
			}
			if(v1.get()) {
				this.getGridMode().setNextValue(GridMode.ON_GRID);
			}
			else if(!v1.get()) {
				this.getGridMode().setNextValue(GridMode.OFF_GRID);
			}
		});
		
		this.battery.getSoc().onChange(value -> {
			this.getSoc().setNextValue(value.get());
			this.channel(ChannelId.BAT_SOC).setNextValue(value.get());
			this.channel(SymmetricEss.ChannelId.SOC).setNextValue(value.get());
		});

		this.battery.getVoltage().onChange(value -> {
			this.channel(ChannelId.BAT_VOLTAGE).setNextValue(value.get());
		});
	}

	private void setBatteryRanges() {
		maxApparentPower = 30000;
		if (battery == null) {
			return;
		}
		int disMaxA = battery.getDischargeMaxCurrent().value().orElse(0);
		int chaMaxA = battery.getChargeMaxCurrent().value().orElse(0);
		int disMinV = battery.getDischargeMinVoltage().value().orElse(0);
		int chaMaxV = battery.getChargeMaxVoltage().value().orElse(0);

		IntegerWriteChannel SET_DIS_MIN_V = this.channel(ChannelId.DIS_MIN_V);
		IntegerWriteChannel SET_CHA_MAX_V = this.channel(ChannelId.CHA_MAX_V);
		IntegerWriteChannel SET_DIS_MAX_A = this.channel(ChannelId.DIS_MAX_A);
		IntegerWriteChannel SET_CHA_MAX_A = this.channel(ChannelId.CHA_MAX_A);
		try {
			SET_CHA_MAX_A.setNextWriteValue(chaMaxA * 10);
			SET_DIS_MAX_A.setNextWriteValue(disMaxA * 10);
			SET_DIS_MIN_V.setNextWriteValue(disMinV * 10);
			SET_CHA_MAX_V.setNextWriteValue(chaMaxV * 10);
		} catch (OpenemsException e) {
			log.error("Error during setBatteryRanges, " + e.getMessage());
		}
	}

	public void inverterOn() {

		IntegerWriteChannel SETDATA_ModOnCmd = this.channel(ChannelId.SETDATA_MOD_ON_CMD);
		try {
			SETDATA_ModOnCmd.setNextWriteValue(START); // Here: START = 1
		} catch (OpenemsException e) {
			log.error("problem occurred while trying to start inverter" + e.getMessage());
		}
	}

	public void inverterOff() {
		
		IntegerWriteChannel SETDATA_ModOffCmd = this.channel(ChannelId.SETDATA_MOD_OFF_CMD);
		try {
			SETDATA_ModOffCmd.setNextWriteValue(STOP); // Here: STOP = 1
		} catch (OpenemsException e) {
			log.error("problem occurred while trying to stop system" + e.getMessage());
		}
	}

	public void resetDcAcEnergy() {
		IntegerWriteChannel CHARGE_ENERGY = this.channel(ChannelId.SET_ANALOG_CHARGE_Energy);
		IntegerWriteChannel DISCHARGE_ENERGY = this.channel(ChannelId.SET_ANALOG_DISCHARGE_Energy);
		IntegerWriteChannel CHARGE_DC_ENERGY = this.channel(ChannelId.SET_ANALOG_DC_CHARGE_Energy);
		IntegerWriteChannel DISCHARGE_DC_ENERGY = this.channel(ChannelId.SET_ANALOG_DC_DISCHARGE_Energy);
		try {
			CHARGE_DC_ENERGY.setNextWriteValue(0);
			DISCHARGE_DC_ENERGY.setNextWriteValue(0);
			CHARGE_ENERGY.setNextWriteValue(0);
			DISCHARGE_ENERGY.setNextWriteValue(0);
		} catch (OpenemsException e) {
			log.error("problem occurred while trying to reset the AC DC energy" + e.getMessage());
		}
	}

	public void softStart() {
		IntegerWriteChannel SET_DC_RELAY = this.channel(ChannelId.SET_INTERN_DC_RELAY);
		try {
			SET_DC_RELAY.setNextWriteValue(1);
		} catch (OpenemsException e) {
			log.error("problem occured while trying to set the intern DC relay");
		}
	}

	/**
	 * At first the PCS needs a stop command, then is required to remove the AC
	 * connection, after that the Grid OFF command.
	 */

	public void islandOn() {
		IntegerWriteChannel SET_ANTI_ISLANDING = this.channel(ChannelId.SET_ANTI_ISLANDING);
		IntegerWriteChannel SETDATA_GridOffCmd = this.channel(ChannelId.SETDATA_GRID_OFF_CMD);
		try {

			SET_ANTI_ISLANDING.setNextWriteValue(DISABLED_ANTI_ISLANDING);
			SETDATA_GridOffCmd.setNextWriteValue(STOP);
		} catch (OpenemsException e) {
			log.error("problem occurred while trying to activate" + e.getMessage());
		}
	}

	/**
	 * At first the PCS needs a stop command, then is required to plug in the AC
	 * connection, after that the Grid ON command.
	 */
	public void islandingOff() {
		IntegerWriteChannel SET_ANTI_ISLANDING = this.channel(ChannelId.SET_ANTI_ISLANDING);
		IntegerWriteChannel SETDATA_GridOnCmd = this.channel(ChannelId.SETDATA_GRID_ON_CMD);
		try {
			SET_ANTI_ISLANDING.setNextWriteValue(ENABLED_ANTI_ISLANDING);
			SETDATA_GridOnCmd.setNextWriteValue(START);
		} catch (OpenemsException e) {
			log.error("problem occurred while trying to deactivate islanding" + e.getMessage());
		}
	}

	public void doHandlingSlowFloatVoltage() {
		IntegerWriteChannel SET_SLOW_CHARGE_VOLTAGE = this.channel(ChannelId.SET_SLOW_CHARGE_VOLTAGE);
		IntegerWriteChannel SET_FLOAT_CHARGE_VOLTAGE = this.channel(ChannelId.SET_FLOAT_CHARGE_VOLTAGE);

		try {
			SET_SLOW_CHARGE_VOLTAGE.setNextWriteValue(SLOW_CHARGE_VOLTAGE);
			SET_FLOAT_CHARGE_VOLTAGE.setNextWriteValue(FLOAT_CHARGE_VOLTAGE);

		} catch (OpenemsException e) {
			log.error("problem occurred while trying to write the voltage limits" + e.getMessage());
		}
	}

//------------------------------------------------------GET VALUE--------------------------------------------------
	public boolean faultIslanding() {
		StateChannel i = this.channel(ChannelId.STATE_4);
		Optional<Boolean> islanding = i.getNextValue().asOptional();
		return islanding.isPresent() && islanding.get();
	}
	
	public boolean stateOnOff() {
		StateChannel v = this.channel(ChannelId.STATE_18);
		Optional<Boolean> stateOff = v.getNextValue().asOptional(); 
		return stateOff.isPresent() && stateOff.get();
	}

	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
//------------------------------------------------------------WRITE-----------------------------------------------------------
				 
				new FC6WriteRegisterTask(0x028A,
						m(EssSinexcel.ChannelId.SETDATA_MOD_ON_CMD, new UnsignedWordElement(0x028A))),
				new FC6WriteRegisterTask(0x028B,
						m(EssSinexcel.ChannelId.SETDATA_MOD_OFF_CMD, new UnsignedWordElement(0x028B))),

				new FC6WriteRegisterTask(0x0290,
						m(EssSinexcel.ChannelId.SET_INTERN_DC_RELAY, new UnsignedWordElement(0x0290))),

				new FC6WriteRegisterTask(0x028D,
						m(EssSinexcel.ChannelId.SETDATA_GRID_ON_CMD, new UnsignedWordElement(0x028D))), // Start SETDATA_GridOnCmd
				new FC6WriteRegisterTask(0x028E,
						m(EssSinexcel.ChannelId.SETDATA_GRID_OFF_CMD, new UnsignedWordElement(0x028E))), // Stop SETDATA_GridOffCmd

				new FC6WriteRegisterTask(0x0087,
						m(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS, new SignedWordElement(0x0087))),
				new FC6WriteRegisterTask(0x0088,
						m(ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_EQUALS, new SignedWordElement(0x0088))), 

				new FC6WriteRegisterTask(0x032B,
						m(EssSinexcel.ChannelId.CHA_MAX_A, new UnsignedWordElement(0x032B))), //
				new FC6WriteRegisterTask(0x032C,
						m(EssSinexcel.ChannelId.DIS_MAX_A, new UnsignedWordElement(0x032C))), //

				new FC6WriteRegisterTask(0x0329,
						m(EssSinexcel.ChannelId.SET_SLOW_CHARGE_VOLTAGE, new UnsignedWordElement(0x0329))), 
				new FC6WriteRegisterTask(0x0328,
						m(EssSinexcel.ChannelId.SET_FLOAT_CHARGE_VOLTAGE, new UnsignedWordElement(0x0328))),

				new FC6WriteRegisterTask(0x032E, 
						m(EssSinexcel.ChannelId.CHA_MAX_V, new UnsignedWordElement(0x032E))), 
				new FC6WriteRegisterTask(0x032D, 
						m(EssSinexcel.ChannelId.DIS_MIN_V, new UnsignedWordElement(0x032D))), 

				new FC6WriteRegisterTask(0x007E,
						m(EssSinexcel.ChannelId.SET_ANALOG_CHARGE_Energy, new UnsignedWordElement(0x007E))),
				new FC6WriteRegisterTask(0x007F,
						m(EssSinexcel.ChannelId.SET_ANALOG_CHARGE_Energy, new UnsignedWordElement(0x007F))),
				
				new FC6WriteRegisterTask(0x0080,
						m(EssSinexcel.ChannelId.SET_ANALOG_DISCHARGE_Energy, new UnsignedWordElement(0x0080))), 
				new FC6WriteRegisterTask(0x0081,
						m(EssSinexcel.ChannelId.SET_ANALOG_DISCHARGE_Energy, new UnsignedWordElement(0x0081))), 
				
				
				new FC6WriteRegisterTask(0x0090,
						m(EssSinexcel.ChannelId.SET_ANALOG_DC_CHARGE_Energy, new UnsignedWordElement(0x0090))),
				new FC6WriteRegisterTask(0x0091,
						m(EssSinexcel.ChannelId.SET_ANALOG_DC_CHARGE_Energy, new UnsignedWordElement(0x0091))),
				
				new FC6WriteRegisterTask(0x0092,
						m(EssSinexcel.ChannelId.SET_ANALOG_DC_DISCHARGE_Energy, new UnsignedWordElement(0x0092))),
				new FC6WriteRegisterTask(0x0093,
						m(EssSinexcel.ChannelId.SET_ANALOG_DC_DISCHARGE_Energy, new UnsignedWordElement(0x0093))),

//----------------------------------------------------------READ------------------------------------------------------
				new FC3ReadRegistersTask(0x0065, Priority.HIGH, //
						m(EssSinexcel.ChannelId.INVOUTVOLT_L1, new UnsignedWordElement(0x0065),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), 
						m(EssSinexcel.ChannelId.INVOUTVOLT_L2, new UnsignedWordElement(0x0066),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), 
						m(EssSinexcel.ChannelId.INVOUTVOLT_L3, new UnsignedWordElement(0x0067),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), 
						m(EssSinexcel.ChannelId.INVOUTCURRENT_L1, new UnsignedWordElement(0x0068),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), 
						m(EssSinexcel.ChannelId.INVOUTCURRENT_L2, new UnsignedWordElement(0x0069),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(EssSinexcel.ChannelId.INVOUTCURRENT_L3, new UnsignedWordElement(0x006A),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), 

				new FC3ReadRegistersTask(0x007E, Priority.HIGH,
						m(EssSinexcel.ChannelId.ANALOG_CHARGE_ENERGY, new UnsignedDoublewordElement(0x007E)), // 1
						m(EssSinexcel.ChannelId.ANALOG_DISCHARGE_ENERGY, new UnsignedDoublewordElement(0x0080)), // 1
						new DummyRegisterElement(0x0082, 0x0083),
						m(EssSinexcel.ChannelId.TEMPERATURE, new SignedWordElement(0x0084)), 
						new DummyRegisterElement(0x0085, 0x008C), 
						m(EssSinexcel.ChannelId.DC_POWER, new SignedWordElement(0x008D),
								ElementToChannelConverter.SCALE_FACTOR_1), 
						new DummyRegisterElement(0x008E, 0x008F),
						m(EssSinexcel.ChannelId.ANALOG_DC_CHARGE_ENERGY, new UnsignedDoublewordElement(0x0090)), // 1
						m(EssSinexcel.ChannelId.ANALOG_DC_DISCHARGE_ENERGY, new UnsignedDoublewordElement(0x0092))), // 1

				new FC3ReadRegistersTask(0x0248, Priority.HIGH,
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedWordElement(0x0248),
								ElementToChannelConverter.SCALE_FACTOR_1), 
						new DummyRegisterElement(0x0249),
						m(EssSinexcel.ChannelId.FREQUENCY, new SignedWordElement(0x024A),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), 
						new DummyRegisterElement(0x024B, 0x0254), 
						m(EssSinexcel.ChannelId.DC_CURRENT, new SignedWordElement(0x0255), 
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						new DummyRegisterElement(0x0256),
						m(EssSinexcel.ChannelId.DC_VOLTAGE, new UnsignedWordElement(0x0257),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), 
																					
				new FC3ReadRegistersTask(0x0260, Priority.HIGH,
						m(EssSinexcel.ChannelId.SINEXCEL_STATE, new UnsignedWordElement(0x0260))),

				new FC3ReadRegistersTask(0x0001, Priority.ONCE,
						m(EssSinexcel.ChannelId.MODEL, new StringWordElement(0x0001, 16)),
						m(EssSinexcel.ChannelId.SERIAL, new StringWordElement(0x0011, 8))),
				
				new FC3ReadRegistersTask(0x0220, Priority.ONCE,
//						m(EssSinexcel.ChannelId.Manufacturer, new StringWordElement(0x01F8, 16)), // String // Line109
//						m(EssSinexcel.ChannelId.Model_2, new StringWordElement(0x0208, 16)), // String (32Char) // line110
						m(EssSinexcel.ChannelId.VERSION, new StringWordElement(0x0220, 8))), // String (16Char) //
//						m(EssSinexcel.ChannelId.Serial_Number, new StringWordElement(0x0228, 16))), // String (32Char)// Line113

				new FC3ReadRegistersTask(0x032D, Priority.LOW,
						m(EssSinexcel.ChannelId.LOWER_VOLTAGE_LIMIT, new UnsignedWordElement(0x032D), // uint 16 //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(EssSinexcel.ChannelId.UPPER_VOLTAGE_LIMIT, new UnsignedWordElement(0x032E), // uint16 //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),

//				new FC3ReadRegistersTask(0x0316, Priority.LOW,
//						m(EssSinexcel.ChannelId.ANTI_ISLANDING, new UnsignedWordElement(0x0316))),

//----------------------------------------------------------------------------------------------------------

				new FC3ReadRegistersTask(0x024E, Priority.HIGH, //
						m(SymmetricEss.ChannelId.REACTIVE_POWER, new SignedWordElement(0x024E))), // int16 // Line136 // Magnification = 0

//-----------------------------------------EVENT Bitfield 32------------------------------------------------------------		
				new FC3ReadRegistersTask(0x0262, Priority.LOW, //
						bm(new UnsignedWordElement(0x0262)) //
								.m(EssSinexcel.ChannelId.STATE_0, 0) //
								.m(EssSinexcel.ChannelId.STATE_1, 1) //
								.m(EssSinexcel.ChannelId.STATE_2, 2) //
								.m(EssSinexcel.ChannelId.STATE_3, 3) //
								.m(EssSinexcel.ChannelId.STATE_4, 4) // Grid shutdown
								.m(EssSinexcel.ChannelId.STATE_5, 5) //
								.m(EssSinexcel.ChannelId.STATE_6, 6) //
								.m(EssSinexcel.ChannelId.STATE_7, 7) //
								.m(EssSinexcel.ChannelId.STATE_8, 8) //
								.m(EssSinexcel.ChannelId.STATE_9, 9) //
								.m(EssSinexcel.ChannelId.STATE_10, 10) //
								.m(EssSinexcel.ChannelId.STATE_11, 11) //
								.m(EssSinexcel.ChannelId.STATE_12, 12) //
								.m(EssSinexcel.ChannelId.STATE_13, 13) //
								.m(EssSinexcel.ChannelId.STATE_14, 14) //
								.m(EssSinexcel.ChannelId.STATE_15, 15) //
								.build()), //
//---------------------------------------------STATES---------------------------------------------------------
				new FC3ReadRegistersTask(0x0260, Priority.LOW, //
						bm(new UnsignedWordElement(0x0260)) //

								.m(EssSinexcel.ChannelId.SINEXCEL_STATE_1, 1) //
								.m(EssSinexcel.ChannelId.SINEXCEL_STATE_2, 2) //
								.m(EssSinexcel.ChannelId.SINEXCEL_STATE_3, 3) //
								.m(EssSinexcel.ChannelId.SINEXCEL_STATE_4, 4) //
								.m(EssSinexcel.ChannelId.SINEXCEL_STATE_5, 5) //
								.m(EssSinexcel.ChannelId.SINEXCEL_STATE_6, 6) //
								.m(EssSinexcel.ChannelId.SINEXCEL_STATE_7, 7) //
								.m(EssSinexcel.ChannelId.SINEXCEL_STATE_8, 8) //
								.m(EssSinexcel.ChannelId.SINEXCEL_STATE_9, 9) //
								.build()), //
//---------------------------------------------------FAULT LIST--------------------------------------------------------------------
				new FC3ReadRegistersTask(0x0020, Priority.LOW, //
						bm(new UnsignedWordElement(0x0020)) //
								.m(EssSinexcel.ChannelId.STATE_16, 0) //
								.m(EssSinexcel.ChannelId.STATE_17, 1) //
								.m(EssSinexcel.ChannelId.STATE_18, 2) //
								.m(EssSinexcel.ChannelId.STATE_19, 3) //
								.m(EssSinexcel.ChannelId.STATE_20, 4) //
								.build()), //

				new FC3ReadRegistersTask(0x0024, Priority.LOW, //
						bm(new UnsignedWordElement(0x0024)) //
								.m(EssSinexcel.ChannelId.STATE_21, 0) //
								.m(EssSinexcel.ChannelId.STATE_22, 1) //
								.m(EssSinexcel.ChannelId.STATE_23, 2) //
								.m(EssSinexcel.ChannelId.STATE_24, 3) //
								.m(EssSinexcel.ChannelId.STATE_25, 4) //
								.m(EssSinexcel.ChannelId.STATE_26, 5) //
								.m(EssSinexcel.ChannelId.STATE_27, 6) //
								.m(EssSinexcel.ChannelId.STATE_28, 7) //
								.m(EssSinexcel.ChannelId.STATE_29, 8) //
								.m(EssSinexcel.ChannelId.STATE_30, 9) //
								.m(EssSinexcel.ChannelId.STATE_31, 10) //
								.m(EssSinexcel.ChannelId.STATE_32, 11) //
								.m(EssSinexcel.ChannelId.STATE_33, 12) //
								.build()), //

				new FC3ReadRegistersTask(0x0025, Priority.LOW, //
						bm(new UnsignedWordElement(0x0025)) //
								.m(EssSinexcel.ChannelId.STATE_34, 0) //
								.m(EssSinexcel.ChannelId.STATE_35, 1) //
								.m(EssSinexcel.ChannelId.STATE_36, 2) //
								.m(EssSinexcel.ChannelId.STATE_37, 3) //
								.m(EssSinexcel.ChannelId.STATE_38, 4) //
								.m(EssSinexcel.ChannelId.STATE_39, 5) //
								.m(EssSinexcel.ChannelId.STATE_40, 6) //
								.m(EssSinexcel.ChannelId.STATE_41, 7) //
								.m(EssSinexcel.ChannelId.STATE_42, 8) //
								.m(EssSinexcel.ChannelId.STATE_43, 9) //
								.m(EssSinexcel.ChannelId.STATE_44, 10) //
								.m(EssSinexcel.ChannelId.STATE_45, 11) //
								.m(EssSinexcel.ChannelId.STATE_47, 13) //
								.m(EssSinexcel.ChannelId.STATE_48, 14) //
								.m(EssSinexcel.ChannelId.STATE_49, 15) //
								.build()), //

				new FC3ReadRegistersTask(0x0026, Priority.LOW, //
						bm(new UnsignedWordElement(0x0026)) //
								.m(EssSinexcel.ChannelId.STATE_50, 0) //
								.m(EssSinexcel.ChannelId.STATE_52, 2) //
								.m(EssSinexcel.ChannelId.STATE_53, 3) //
								.m(EssSinexcel.ChannelId.STATE_54, 4) //
								.build()), //

				new FC3ReadRegistersTask(0x0027, Priority.LOW, //
						bm(new UnsignedWordElement(0x0027)) //
								.m(EssSinexcel.ChannelId.STATE_55, 0) //
								.m(EssSinexcel.ChannelId.STATE_56, 1) //
								.m(EssSinexcel.ChannelId.STATE_57, 2) //
								.m(EssSinexcel.ChannelId.STATE_58, 3) //
								.build()), //

				new FC3ReadRegistersTask(0x0028, Priority.LOW, //
						bm(new UnsignedWordElement(0x0028)) //
								.m(EssSinexcel.ChannelId.STATE_59, 0) //
								.m(EssSinexcel.ChannelId.STATE_60, 1) //
								.m(EssSinexcel.ChannelId.STATE_61, 2) //
								.m(EssSinexcel.ChannelId.STATE_62, 3) //
								.m(EssSinexcel.ChannelId.STATE_63, 4) //
								.m(EssSinexcel.ChannelId.STATE_64, 5) //
								.build()), //

				new FC3ReadRegistersTask(0x002B, Priority.LOW, //
						bm(new UnsignedWordElement(0x002B)) //
								.m(EssSinexcel.ChannelId.STATE_65, 0) //
								.m(EssSinexcel.ChannelId.STATE_66, 1) //
								.m(EssSinexcel.ChannelId.STATE_67, 2) //
								.m(EssSinexcel.ChannelId.STATE_68, 3) //
								.build()), //

				new FC3ReadRegistersTask(0x002C, Priority.LOW, //
						bm(new UnsignedWordElement(0x002C)) //
								.m(EssSinexcel.ChannelId.STATE_69, 0) //
								.m(EssSinexcel.ChannelId.STATE_70, 1) //
								.m(EssSinexcel.ChannelId.STATE_71, 2) //
								.m(EssSinexcel.ChannelId.STATE_72, 3) //
								.m(EssSinexcel.ChannelId.STATE_73, 4) //
								.build()), //

				new FC3ReadRegistersTask(0x002F, Priority.LOW, //
						bm(new UnsignedWordElement(0x002F)) //
								.m(EssSinexcel.ChannelId.STATE_74, 0) //
								.build()) //

		);

	}

//------------------------------------------------------------------------------------------------------------------------

	@Override
	public String debugLog() {
		return "\nState: \t\t" + this.channel(ChannelId.SINEXCEL_STATE).value().asOptionString() //
				+ "\nDC Current: \t" + this.channel(ChannelId.DC_CURRENT).value().asStringWithoutUnit() + " A"
				+ "\nDC Voltage: \t" + this.channel(ChannelId.BAT_VOLTAGE).value().asStringWithoutUnit() + " V"
				+ "\nDC Power: \t" + this.channel(ChannelId.DC_POWER).value().asStringWithoutUnit() + " W"
				+ "\nAC Power: \t" + this.channel(SymmetricEss.ChannelId.ACTIVE_POWER).value().asString() + "\n"
		;
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
	public void applyPower(int activePower, int reactivePower) {
		
		switch (this.InverterState) {
		case ON:
			
			IntegerWriteChannel SET_ACTIVE_POWER = this.channel(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS);
			IntegerWriteChannel SET_REACTIVE_POWER = this.channel(ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_EQUALS);

			int reactiveValue = (int) ((reactivePower / 100));
			try {
				SET_REACTIVE_POWER.setNextWriteValue(reactiveValue);

			} catch (OpenemsException e) {
				log.error("EssSinexcel.applyPower(): Problem occurred while trying so set reactive power" + e.getMessage());
			}

			int activeValue = (int) ((activePower / 100));
			try {
				SET_ACTIVE_POWER.setNextWriteValue(activeValue);

			} catch (OpenemsException e) {
				log.error("EssSinexcel.applyPower(): Problem occurred while trying so set active power" + e.getMessage());
			}
			
			if (activePower == 0 && reactivePower == 0 && stateOnOff() == true) {
				counterOff++;
				if(counterOff == 47) {
					inverterOff();
					counterOff = 0;
				}
				
			}
			else if((activePower != 0 || reactivePower != 0) && stateOnOff() == false) {
				counterOn++;
				if(counterOn == 47) {
					inverterOn();
					counterOn = 0;
				}
			}
			break;
			
		case OFF:
			if(stateOnOff() == true) {
				inverterOff();
			}
			else {
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
			setBatteryRanges();
			
			
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
	public Power getPower() { // Siehe KACO
		return this.power;
	}

	@Override // Leistungsstufen des Wechselrichters
	public int getPowerPrecision() {
		return (int) (MINIMAL_POWER);
	}

	public IntegerWriteChannel getDischargeMinVoltageChannel() {
		return this.channel(ChannelId.DIS_MIN_V);
	}

	public IntegerWriteChannel getDischargeMaxAmpereChannel() {
		return this.channel(ChannelId.DIS_MAX_A);
	}

	public IntegerWriteChannel getChargeMaxVoltageChannel() {
		return this.channel(ChannelId.CHA_MAX_V);
	}

	public IntegerWriteChannel getChargeMaxAmpereChannel() {
		return this.channel(ChannelId.CHA_MAX_A);
	}

	public IntegerWriteChannel getEnLimitChannel() {
		return this.channel(ChannelId.EN_LIMIT);
	}

	public IntegerWriteChannel getBatterySocChannel() {
		return this.channel(ChannelId.BAT_SOC);
	}

	public IntegerWriteChannel getBatterySohChannel() {
		return this.channel(ChannelId.BAT_SOH);
	}

	public IntegerWriteChannel getBatteryTempChannel() {
		return this.channel(ChannelId.BAT_TEMP);
	}

	public IntegerWriteChannel getMinimalCellVoltage() {
		return this.channel(ChannelId.BAT_MIN_CELL_VOLTAGE);
	}

	public IntegerWriteChannel getVoltage() {
		return this.channel(ChannelId.BAT_VOLTAGE);
	}
	
}
