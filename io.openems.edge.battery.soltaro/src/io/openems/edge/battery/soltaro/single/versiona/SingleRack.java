package io.openems.edge.battery.soltaro.single.versiona;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;

import java.time.LocalDateTime;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.protection.BatteryProtection;
import io.openems.edge.battery.soltaro.common.batteryprotection.BatteryProtectionDefinitionSoltaro3000Wh;
import io.openems.edge.battery.soltaro.common.enums.BatteryState;
import io.openems.edge.battery.soltaro.common.enums.ChargeIndication;
import io.openems.edge.battery.soltaro.common.enums.State;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Bms.Soltaro.SingleRack.VersionA", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class SingleRack extends AbstractOpenemsModbusComponent
		implements Battery, ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave, StartStoppable {

	// Default values for the battery ranges
	public static final int DISCHARGE_MIN_V = 696;
	public static final int CHARGE_MAX_V = 854;
	protected static final int SYSTEM_ON = 1;
	protected static final int SYSTEM_OFF = 0;

	private final Logger log = LoggerFactory.getLogger(SingleRack.class);

	private Config config;

	private String modbusBridgeId;
	private BatteryState batteryState;
	private State state = State.UNDEFINED;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager componentManager;

	// If an error has occurred, this indicates the time when next action could be
	// done
	private LocalDateTime errorDelayIsOver = null;
	private int unsuccessfulStarts = 0;
	private LocalDateTime startAttemptTime = null;

	// Indicates that system is stopping; during that time no commands should be
	// sent
	private boolean isStopping = false;

	private LocalDateTime pendingTimestamp;

	private BatteryProtection batteryProtection = null;

	public SingleRack() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				Battery.ChannelId.values(), //
				SingleRack.ChannelId.values(), //
				BatteryProtection.ChannelId.values() //
		);
		this._setChargeMaxVoltage(SingleRack.CHARGE_MAX_V);
		this._setDischargeMinVoltage(SingleRack.DISCHARGE_MIN_V);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.modbusBridgeId = config.modbus_id();
		this.batteryState = config.batteryState();

		// Initialize Battery-Protection
		this.batteryProtection = BatteryProtection.create(this) //
				.applyBatteryProtectionDefinition(new BatteryProtectionDefinitionSoltaro3000Wh(), this.componentManager) //
				.build();

		this._setCapacity(config.capacity() * 1000);
		this.initializeCallbacks();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void initializeCallbacks() {
		this.channel(ChannelId.BMS_CONTACTOR_CONTROL).onChange((oldValue, newValue) -> {
			ContactorControl cc = newValue.asEnum();

			switch (cc) {
			case CONNECTION_INITIATING:
				// TODO start stop is not implemented;
				this._setStartStop(StartStop.UNDEFINED);
				break;
			case CUT_OFF:
				// TODO start stop is not implemented;
				this._setStartStop(StartStop.STOP);
				this.isStopping = false;
				break;
			case ON_GRID:
				// TODO start stop is not implemented; mark as started if 'readyForWorking'
				this._setStartStop(StartStop.START);
				break;
			default:
				break;
			}
		});
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {

		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.batteryProtection.apply();
			break;

		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.handleBatteryState();
			break;
		}
	}

	private void handleBatteryState() {
		switch (this.batteryState) {
		case DEFAULT:
			this.handleStateMachine();
			break;
		case OFF:
			this.stopSystem();
			break;
		case ON:
			this.startSystem();
			break;
		}
	}

	private void handleStateMachine() {
		this.log.info("SingleRackVersionBImpl.handleStateMachine(): State: " + this.getStateMachineState());
		var readyForWorking = false;
		switch (this.getStateMachineState()) {
		case ERROR:
			this.stopSystem();
			this.errorDelayIsOver = LocalDateTime.now().plusSeconds(this.config.errorLevel2Delay());
			this.setStateMachineState(State.ERRORDELAY);
			break;

		case ERRORDELAY:
			if (LocalDateTime.now().isAfter(this.errorDelayIsOver)) {
				this.errorDelayIsOver = null;
				if (this.isError()) {
					this.setStateMachineState(State.ERROR);
				} else {
					this.setStateMachineState(State.OFF);
				}
			}
			break;
		case INIT:
			if (this.isSystemRunning()) {
				this.setStateMachineState(State.RUNNING);
				this.unsuccessfulStarts = 0;
				this.startAttemptTime = null;
			} else if (this.startAttemptTime.plusSeconds(this.config.maxStartTime()).isBefore(LocalDateTime.now())) {
				this.startAttemptTime = null;
				this.unsuccessfulStarts++;
				this.stopSystem();
				this.setStateMachineState(State.STOPPING);
				if (this.unsuccessfulStarts >= this.config.maxStartAppempts()) {
					this.errorDelayIsOver = LocalDateTime.now().plusSeconds(this.config.startUnsuccessfulDelay());
					this.setStateMachineState(State.ERRORDELAY);
					this.unsuccessfulStarts = 0;
				}
			}
			break;
		case OFF:
			this.log.debug("in case 'OFF'; try to start the system");
			this.startSystem();
			this.log.debug("set state to 'INIT'");
			this.setStateMachineState(State.INIT);
			this.startAttemptTime = LocalDateTime.now();
			break;
		case RUNNING:
			if (this.isError()) {
				this.setStateMachineState(State.ERROR);
			} else if (!this.isSystemRunning()) {
				this.setStateMachineState(State.UNDEFINED);
			} else {
				readyForWorking = true;
				this.setStateMachineState(State.RUNNING);
			}
			break;
		case STOPPING:
			if (this.isError()) {
				this.setStateMachineState(State.ERROR);
			} else if (this.isSystemStopped()) {
				this.setStateMachineState(State.OFF);
			}
			break;
		case UNDEFINED:
			if (this.isError()) {
				this.setStateMachineState(State.ERROR);
			} else if (this.isSystemStopped()) {
				this.setStateMachineState(State.OFF);
			} else if (this.isSystemRunning()) {
				this.setStateMachineState(State.RUNNING);
			} else if (this.isSystemStatePending()) {
				this.setStateMachineState(State.PENDING);
			}
			break;
		case PENDING:
			if (this.pendingTimestamp == null) {
				this.pendingTimestamp = LocalDateTime.now();
			}
			if (this.pendingTimestamp.plusSeconds(this.config.pendingTolerance()).isBefore(LocalDateTime.now())) {
				// System state could not be determined, stop and start it
				this.pendingTimestamp = null;
				this.stopSystem();
				this.setStateMachineState(State.OFF);
			} else if (this.isError()) {
				this.setStateMachineState(State.ERROR);
				this.pendingTimestamp = null;
			} else if (this.isSystemStopped()) {
				this.setStateMachineState(State.OFF);
				this.pendingTimestamp = null;
			} else if (this.isSystemRunning()) {
				this.setStateMachineState(State.RUNNING);
				this.pendingTimestamp = null;
			}
			break;
		case ERROR_HANDLING:
			// Cannot handle errors
			break;
		}

		// TODO start stop is not implemented; mark as started if 'readyForWorking'
		this._setStartStop(readyForWorking ? StartStop.START : StartStop.UNDEFINED);
	}

	private boolean isError() {
		return this.isAlarmLevel2Error();
	}

	private boolean isAlarmLevel2Error() {
		return this.readValueFromBooleanChannel(ChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_HIGH)
				|| this.readValueFromBooleanChannel(ChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH)
				|| this.readValueFromBooleanChannel(ChannelId.ALARM_LEVEL_2_CHA_CURRENT_HIGH)
				|| this.readValueFromBooleanChannel(ChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_LOW)
				|| this.readValueFromBooleanChannel(ChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW)
				|| this.readValueFromBooleanChannel(ChannelId.ALARM_LEVEL_2_DISCHA_CURRENT_HIGH)
				|| this.readValueFromBooleanChannel(ChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH)
				|| this.readValueFromBooleanChannel(ChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_LOW)
				|| this.readValueFromBooleanChannel(ChannelId.ALARM_LEVEL_2_INSULATION_LOW)
				|| this.readValueFromBooleanChannel(ChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH)
				|| this.readValueFromBooleanChannel(ChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW);
	}

	private boolean isSystemRunning() {
		EnumReadChannel contactorControlChannel = this.channel(ChannelId.BMS_CONTACTOR_CONTROL);
		ContactorControl cc = contactorControlChannel.value().asEnum();
		return cc == ContactorControl.ON_GRID;
	}

	private boolean isSystemStopped() {
		EnumReadChannel contactorControlChannel = this.channel(ChannelId.BMS_CONTACTOR_CONTROL);
		ContactorControl cc = contactorControlChannel.value().asEnum();
		return cc == ContactorControl.CUT_OFF;
	}

	/**
	 * Checks whether system has an undefined state.
	 *
	 * @return true when the system is pending
	 */
	private boolean isSystemStatePending() {
		return !this.isSystemRunning() && !this.isSystemStopped();
	}

	private boolean readValueFromBooleanChannel(ChannelId channelId) {
		StateChannel r = this.channel(channelId);
		var bOpt = r.value().asOptional();
		return bOpt.isPresent() && bOpt.get();
	}

	/**
	 * Returns the statemachine state.
	 *
	 * @return the statemachine state
	 */
	public State getStateMachineState() {
		return this.state;
	}

	/**
	 * Sets the state.
	 *
	 * @param state the State
	 */
	public void setStateMachineState(State state) {
		this.state = state;
		this.channel(ChannelId.STATE_MACHINE).setNextValue(this.state);
	}

	/**
	 * Returns the modbus bridge id.
	 *
	 * @return the modbus bridge id
	 */
	public String getModbusBridgeId() {
		return this.modbusBridgeId;
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc() //
				+ "|Discharge:" + this.getDischargeMinVoltage() + ";" + this.getDischargeMaxCurrent() //
				+ "|Charge:" + this.getChargeMaxVoltage() + ";" + this.getChargeMaxCurrent() + "|Running: "
				+ this.isSystemRunning() + "|U: " + this.getVoltage() + "|I: " + this.getCurrent();
	}

	private void startSystem() {
		if (this.isStopping) {
			return;
		}

		EnumWriteChannel contactorControlChannel = this.channel(ChannelId.BMS_CONTACTOR_CONTROL);

		var contactorControlOpt = contactorControlChannel.value().asOptional();
		// To avoid hardware damages do not send start command if system has already
		// started
		if (contactorControlOpt.isPresent() && contactorControlOpt.get() == ContactorControl.ON_GRID.getValue()) {
			return;
		}

		try {
			contactorControlChannel.setNextWriteValue(SYSTEM_ON);
		} catch (OpenemsNamedException e) {
			this.log.error("Error while trying to start system\n" + e.getMessage());
		}
	}

	private void stopSystem() {
		EnumWriteChannel contactorControlChannel = this.channel(ChannelId.BMS_CONTACTOR_CONTROL);

		var contactorControlOpt = contactorControlChannel.value().asOptional();
		// To avoid hardware damages do not send stop command if system has already
		// stopped
		if (contactorControlOpt.isPresent() && contactorControlOpt.get() == ContactorControl.CUT_OFF.getValue()) {
			return;
		}

		try {
			contactorControlChannel.setNextWriteValue(SYSTEM_OFF);
			this.isStopping = true;
		} catch (OpenemsNamedException e) {
			this.log.error("Error while trying to stop system\n" + e.getMessage());
		}
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// IntegerWriteChannels
		CELL_VOLTAGE_PROTECT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE)), //
		CELL_VOLTAGE_RECOVER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE)),

		// EnumReadChannels
		CLUSTER_RUN_STATE(Doc.of(ClusterRunState.values())), //

		// EnumWriteChannels
		BMS_CONTACTOR_CONTROL(Doc.of(ContactorControl.values()) //
				.accessMode(AccessMode.READ_WRITE)), //

		// IntegerReadChannels
		CHARGE_INDICATION(Doc.of(ChargeIndication.values())), //
		SYSTEM_OVER_VOLTAGE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		SYSTEM_UNDER_VOLTAGE_PROTECTION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_MAX_CELL_VOLTAGE_ID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		CLUSTER_1_MIN_CELL_VOLTAGE_ID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		CLUSTER_1_MAX_CELL_TEMPERATURE_ID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		CLUSTER_1_MIN_CELL_TEMPERATURE_ID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		SYSTEM_INSULATION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOOHM)), //
		CLUSTER_1_BATTERY_000_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_001_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_002_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_003_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_004_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_005_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_006_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_007_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_008_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_009_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_010_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_011_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_012_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_013_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_014_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_015_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_016_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_017_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_018_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_019_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_020_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_021_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_022_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_023_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_024_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_025_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_026_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_027_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_028_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_029_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_030_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_031_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_032_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_033_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_034_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_035_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_036_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_037_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_038_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_039_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_040_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_041_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_042_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_043_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_044_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_045_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_046_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_047_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_048_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_049_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_050_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_051_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_052_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_053_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_054_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_055_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_056_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_057_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_058_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_059_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_060_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_061_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_062_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_063_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_064_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_065_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_066_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_067_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_068_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_069_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_070_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_071_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_072_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_073_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_074_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_075_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_076_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_077_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_078_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_079_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_080_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_081_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_082_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_083_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_084_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_085_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_086_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_087_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_088_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_089_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_090_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_091_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_092_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_093_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_094_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_095_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_096_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_097_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_098_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_099_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_100_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_101_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_102_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_103_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_104_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_105_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_106_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_107_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_108_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_109_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_110_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_111_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_112_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_113_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_114_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_115_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_116_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_117_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_118_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_119_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_120_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_121_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_122_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_123_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_124_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_125_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_126_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_127_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_128_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_129_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_130_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_131_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_132_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_133_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_134_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_135_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_136_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_137_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_138_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_139_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_140_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_141_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_142_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_143_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_144_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_145_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_146_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_147_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_148_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_149_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_150_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_151_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_152_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_153_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_154_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_155_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_156_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_157_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_158_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_159_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_160_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_161_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_162_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_163_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_164_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_165_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_166_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_167_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_168_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_169_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_170_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_171_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_172_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_173_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_174_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_175_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_176_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_177_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_178_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_179_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_180_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_181_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_182_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_183_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_184_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_185_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_186_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_187_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_188_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_189_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_190_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_191_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_192_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_193_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_194_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_195_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_196_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_197_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_198_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_199_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_200_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_201_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_202_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_203_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_204_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_205_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_206_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_207_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_208_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_209_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_210_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_211_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_212_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_213_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_214_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_215_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_216_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_217_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_218_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_219_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_220_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_221_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_222_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_223_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_224_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_225_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_226_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_227_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_228_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_229_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_230_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_231_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_232_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_233_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_234_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_235_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_236_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_237_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_238_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CLUSTER_1_BATTERY_239_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //

		CLUSTER_1_BATTERY_00_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_01_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_02_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_03_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_04_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_05_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_06_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_07_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_08_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_09_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_10_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_11_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_12_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_13_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_14_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_15_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_16_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_17_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_18_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_19_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_20_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_21_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_22_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_23_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_24_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_25_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_26_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_27_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_28_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_29_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_30_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_31_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_32_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_33_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_34_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_35_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_36_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_37_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_38_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_39_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_40_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_41_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_42_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_43_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_44_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_45_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_46_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CLUSTER_1_BATTERY_47_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //

		// StateChannels
		ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW(Doc.of(Level.WARNING) //
				.text("Cluster 1 Cell Discharge Temperature Low Alarm Level 2")), //
		ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Cell Discharge Temperature High Alarm Level 2")), //
		ALARM_LEVEL_2_INSULATION_LOW(Doc.of(Level.WARNING) //
				.text("Cluster1Insulation Low Alarm Level 2")), //
		ALARM_LEVEL_2_CELL_CHA_TEMP_LOW(Doc.of(Level.WARNING) //
				.text("Cluster1 Cell Charge Temperature Low Alarm Level 2")), //
		ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster1 Cell Charge Temperature High Alarm Level 2")), //
		ALARM_LEVEL_2_DISCHA_CURRENT_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Discharge Current High Alarm Level 2")), //
		ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
				.text("Cluster 1 Total Voltage Low Alarm Level 2")), //
		ALARM_LEVEL_2_CELL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
				.text("Cluster 1 Cell Voltage Low Alarm Level 2")), //
		ALARM_LEVEL_2_CHA_CURRENT_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Charge Current High Alarm Level 2")), //
		ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Total Voltage High Alarm Level 2")), //
		ALARM_LEVEL_2_CELL_VOLTAGE_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Cell Voltage High Alarm Level 2")), //
		ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW(Doc.of(Level.WARNING) //
				.text("Cluster 1 Cell Discharge Temperature Low Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Cell Discharge Temperature High Alarm Level 1")), //
		ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster1 Total Voltage Diff High Alarm Level 1")), //
		ALARM_LEVEL_1_INSULATION_LOW(Doc.of(Level.WARNING) //
				.text("Cluster1 Insulation Low Alarm Level1")), //
		ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Cell Voltage Diff High Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster X Cell temperature Diff High Alarm Level 1")), //
		ALARM_LEVEL_1_SOC_LOW(Doc.of(Level.WARNING) //
				.text("Cluster 1 SOC Low Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_CHA_TEMP_LOW(Doc.of(Level.WARNING) //
				.text("Cluster 1 Cell Charge Temperature Low Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Cell Charge Temperature High Alarm Level 1")), //
		ALARM_LEVEL_1_DISCHA_CURRENT_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Discharge Current High Alarm Level 1")), //
		ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
				.text("Cluster 1 Total Voltage Low Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
				.text("Cluster 1 Cell Voltage Low Alarm Level 1")), //
		ALARM_LEVEL_1_CHA_CURRENT_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Charge Current High Alarm Level 1")), //
		ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Total Voltage High Alarm Level 1")), //
		ALARM_LEVEL_1_CELL_VOLTAGE_HIGH(Doc.of(Level.WARNING) //
				.text("Cluster 1 Cell Voltage High Alarm Level 1")), //
		FAILURE_INITIALIZATION(Doc.of(Level.FAULT) //
				.text("Initialization failure")), //
		FAILURE_EEPROM(Doc.of(Level.FAULT) //
				.text("EEPROM fault")), //
		FAILURE_INTRANET_COMMUNICATION(Doc.of(Level.FAULT) //
				.text("Intranet communication fault")), //
		FAILURE_TEMP_SAMPLING_LINE(Doc.of(Level.FAULT) //
				.text("Temperature sampling line fault")), //
		FAILURE_BALANCING_MODULE(Doc.of(Level.FAULT) //
				.text("Balancing module fault")), //
		FAILURE_TEMP_SENSOR(Doc.of(Level.FAULT) //
				.text("Temperature sensor fault")), //
		FAILURE_TEMP_SAMPLING(Doc.of(Level.FAULT) //
				.text("Temperature sampling fault")), //
		FAILURE_VOLTAGE_SAMPLING(Doc.of(Level.FAULT) //
				.text("Voltage sampling fault")), //
		FAILURE_LTC6803(Doc.of(Level.FAULT) //
				.text("LTC6803 fault")), //
		FAILURE_CONNECTOR_WIRE(Doc.of(Level.WARNING) //
				.text("connector wire fault")), //
		FAILURE_SAMPLING_WIRE(Doc.of(Level.FAULT) //
				.text("sampling wire fault")), //
		PRECHARGE_TAKING_TOO_LONG(Doc.of(Level.FAULT) //
				.text("precharge time was too long")), //
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				new FC6WriteRegisterTask(0x2010, //
						m(SingleRack.ChannelId.BMS_CONTACTOR_CONTROL, new UnsignedWordElement(0x2010)) //
				), //
				new FC3ReadRegistersTask(0x2010, Priority.HIGH, //
						m(SingleRack.ChannelId.BMS_CONTACTOR_CONTROL, new UnsignedWordElement(0x2010)) //
				), //
				new FC3ReadRegistersTask(0x2042, Priority.HIGH, //
						m(Battery.ChannelId.CHARGE_MAX_VOLTAGE, new UnsignedWordElement(0x2042), //
								SCALE_FACTOR_MINUS_1) //
				), //
				new FC3ReadRegistersTask(0x2046, Priority.HIGH, //
						m(SingleRack.ChannelId.CELL_VOLTAGE_PROTECT, new UnsignedWordElement(0x2046)), //
						m(SingleRack.ChannelId.CELL_VOLTAGE_RECOVER, new UnsignedWordElement(0x2047)), //
						m(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE, new UnsignedWordElement(0x2048), //
								SCALE_FACTOR_MINUS_1) //
				), //
				new FC6WriteRegisterTask(0x2046, //
						m(SingleRack.ChannelId.CELL_VOLTAGE_PROTECT, new UnsignedWordElement(0x2046)) //
				), //
				new FC6WriteRegisterTask(0x2047, //
						m(SingleRack.ChannelId.CELL_VOLTAGE_RECOVER, new UnsignedWordElement(0x2047)) //
				), //
				new FC3ReadRegistersTask(0x2100, Priority.HIGH, //
						m(Battery.ChannelId.VOLTAGE, new UnsignedWordElement(0x2100), //
								SCALE_FACTOR_MINUS_1), //
						m(Battery.ChannelId.CURRENT, new SignedWordElement(0x2101), //
								SCALE_FACTOR_MINUS_1), //
						m(SingleRack.ChannelId.CHARGE_INDICATION, new UnsignedWordElement(0x2102)), //
						m(Battery.ChannelId.SOC, new UnsignedWordElement(0x2103)), //
						m(Battery.ChannelId.SOH, new UnsignedWordElement(0x2104)), //
						m(SingleRack.ChannelId.CLUSTER_1_MAX_CELL_VOLTAGE_ID, new UnsignedWordElement(0x2105)), //
						m(Battery.ChannelId.MAX_CELL_VOLTAGE, new UnsignedWordElement(0x2106)), //
						m(SingleRack.ChannelId.CLUSTER_1_MIN_CELL_VOLTAGE_ID, new UnsignedWordElement(0x2107)), //
						m(Battery.ChannelId.MIN_CELL_VOLTAGE, new UnsignedWordElement(0x2108)), //
						m(SingleRack.ChannelId.CLUSTER_1_MAX_CELL_TEMPERATURE_ID, new UnsignedWordElement(0x2109)), //
						m(Battery.ChannelId.MAX_CELL_TEMPERATURE, new SignedWordElement(0x210A), SCALE_FACTOR_MINUS_1), //
						m(SingleRack.ChannelId.CLUSTER_1_MIN_CELL_TEMPERATURE_ID, new UnsignedWordElement(0x210B)), //
						m(Battery.ChannelId.MIN_CELL_TEMPERATURE, new SignedWordElement(0x210C), SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(0x210D, 0x2115), //
						m(SingleRack.ChannelId.SYSTEM_INSULATION, new UnsignedWordElement(0x2116)) //
				), //
				new FC3ReadRegistersTask(0x2160, Priority.HIGH, //
						m(BatteryProtection.ChannelId.BP_CHARGE_BMS, new UnsignedWordElement(0x2160),
								SCALE_FACTOR_MINUS_1), //
						m(BatteryProtection.ChannelId.BP_DISCHARGE_BMS, new UnsignedWordElement(0x2161),
								SCALE_FACTOR_MINUS_1) //
				), //
				new FC3ReadRegistersTask(0x2140, Priority.LOW, //
						m(new BitsWordElement(0x2140, this) //
								.bit(0, SingleRack.ChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_HIGH) //
								.bit(1, SingleRack.ChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH) //
								.bit(2, SingleRack.ChannelId.ALARM_LEVEL_2_CHA_CURRENT_HIGH) //
								.bit(3, SingleRack.ChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_LOW) //
								.bit(4, SingleRack.ChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW) //
								.bit(5, SingleRack.ChannelId.ALARM_LEVEL_2_DISCHA_CURRENT_HIGH) //
								.bit(6, SingleRack.ChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH) //
								.bit(7, SingleRack.ChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_LOW) //
								.bit(12, SingleRack.ChannelId.ALARM_LEVEL_2_INSULATION_LOW) //
								.bit(14, SingleRack.ChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH) //
								.bit(15, SingleRack.ChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW) //
						), //
						m(new BitsWordElement(0x2141, this) //
								.bit(0, SingleRack.ChannelId.ALARM_LEVEL_1_CELL_VOLTAGE_HIGH) //
								.bit(1, SingleRack.ChannelId.ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH) //
								.bit(2, SingleRack.ChannelId.ALARM_LEVEL_1_CHA_CURRENT_HIGH) //
								.bit(3, SingleRack.ChannelId.ALARM_LEVEL_1_CELL_VOLTAGE_LOW) //
								.bit(4, SingleRack.ChannelId.ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW) //
								.bit(5, SingleRack.ChannelId.ALARM_LEVEL_1_DISCHA_CURRENT_HIGH) //
								.bit(6, SingleRack.ChannelId.ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH) //
								.bit(7, SingleRack.ChannelId.ALARM_LEVEL_1_CELL_CHA_TEMP_LOW) //
								.bit(8, SingleRack.ChannelId.ALARM_LEVEL_1_SOC_LOW) //
								.bit(9, SingleRack.ChannelId.ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH) //
								.bit(11, SingleRack.ChannelId.ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH) //
								.bit(12, SingleRack.ChannelId.ALARM_LEVEL_1_INSULATION_LOW) //
								.bit(13, SingleRack.ChannelId.ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH) //
								.bit(14, SingleRack.ChannelId.ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH) //
								.bit(15, SingleRack.ChannelId.ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW) //
						), //
						m(SingleRack.ChannelId.CLUSTER_RUN_STATE, new UnsignedWordElement(0x2142)) //
				), //
				new FC3ReadRegistersTask(0x2185, Priority.LOW, //
						m(new BitsWordElement(0x2185, this) //
								.bit(0, SingleRack.ChannelId.FAILURE_SAMPLING_WIRE)//
								.bit(1, SingleRack.ChannelId.FAILURE_CONNECTOR_WIRE)//
								.bit(2, SingleRack.ChannelId.FAILURE_LTC6803)//
								.bit(3, SingleRack.ChannelId.FAILURE_VOLTAGE_SAMPLING)//
								.bit(4, SingleRack.ChannelId.FAILURE_TEMP_SAMPLING)//
								.bit(5, SingleRack.ChannelId.FAILURE_TEMP_SENSOR)//
								.bit(8, SingleRack.ChannelId.FAILURE_BALANCING_MODULE)//
								.bit(9, SingleRack.ChannelId.FAILURE_TEMP_SAMPLING_LINE)//
								.bit(10, SingleRack.ChannelId.FAILURE_INTRANET_COMMUNICATION)//
								.bit(11, SingleRack.ChannelId.FAILURE_EEPROM)//
								.bit(12, SingleRack.ChannelId.FAILURE_INITIALIZATION)//
						) //
				), //
				new FC3ReadRegistersTask(0x2800, Priority.LOW, //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_000_VOLTAGE, new UnsignedWordElement(0x2800)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_001_VOLTAGE, new UnsignedWordElement(0x2801)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_002_VOLTAGE, new UnsignedWordElement(0x2802)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_003_VOLTAGE, new UnsignedWordElement(0x2803)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_004_VOLTAGE, new UnsignedWordElement(0x2804)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_005_VOLTAGE, new UnsignedWordElement(0x2805)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_006_VOLTAGE, new UnsignedWordElement(0x2806)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_007_VOLTAGE, new UnsignedWordElement(0x2807)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_008_VOLTAGE, new UnsignedWordElement(0x2808)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_009_VOLTAGE, new UnsignedWordElement(0x2809)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_010_VOLTAGE, new UnsignedWordElement(0x280A)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_011_VOLTAGE, new UnsignedWordElement(0x280B)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_012_VOLTAGE, new UnsignedWordElement(0x280C)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_013_VOLTAGE, new UnsignedWordElement(0x280D)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_014_VOLTAGE, new UnsignedWordElement(0x280E)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_015_VOLTAGE, new UnsignedWordElement(0x280F)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_016_VOLTAGE, new UnsignedWordElement(0x2810)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_017_VOLTAGE, new UnsignedWordElement(0x2811)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_018_VOLTAGE, new UnsignedWordElement(0x2812)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_019_VOLTAGE, new UnsignedWordElement(0x2813)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_020_VOLTAGE, new UnsignedWordElement(0x2814)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_021_VOLTAGE, new UnsignedWordElement(0x2815)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_022_VOLTAGE, new UnsignedWordElement(0x2816)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_023_VOLTAGE, new UnsignedWordElement(0x2817)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_024_VOLTAGE, new UnsignedWordElement(0x2818)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_025_VOLTAGE, new UnsignedWordElement(0x2819)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_026_VOLTAGE, new UnsignedWordElement(0x281A)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_027_VOLTAGE, new UnsignedWordElement(0x281B)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_028_VOLTAGE, new UnsignedWordElement(0x281C)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_029_VOLTAGE, new UnsignedWordElement(0x281D)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_030_VOLTAGE, new UnsignedWordElement(0x281E)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_031_VOLTAGE, new UnsignedWordElement(0x281F)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_032_VOLTAGE, new UnsignedWordElement(0x2820)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_033_VOLTAGE, new UnsignedWordElement(0x2821)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_034_VOLTAGE, new UnsignedWordElement(0x2822)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_035_VOLTAGE, new UnsignedWordElement(0x2823)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_036_VOLTAGE, new UnsignedWordElement(0x2824)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_037_VOLTAGE, new UnsignedWordElement(0x2825)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_038_VOLTAGE, new UnsignedWordElement(0x2826)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_039_VOLTAGE, new UnsignedWordElement(0x2827)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_040_VOLTAGE, new UnsignedWordElement(0x2828)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_041_VOLTAGE, new UnsignedWordElement(0x2829)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_042_VOLTAGE, new UnsignedWordElement(0x282A)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_043_VOLTAGE, new UnsignedWordElement(0x282B)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_044_VOLTAGE, new UnsignedWordElement(0x282C)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_045_VOLTAGE, new UnsignedWordElement(0x282D)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_046_VOLTAGE, new UnsignedWordElement(0x282E)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_047_VOLTAGE, new UnsignedWordElement(0x282F)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_048_VOLTAGE, new UnsignedWordElement(0x2830)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_049_VOLTAGE, new UnsignedWordElement(0x2831)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_050_VOLTAGE, new UnsignedWordElement(0x2832)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_051_VOLTAGE, new UnsignedWordElement(0x2833)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_052_VOLTAGE, new UnsignedWordElement(0x2834)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_053_VOLTAGE, new UnsignedWordElement(0x2835)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_054_VOLTAGE, new UnsignedWordElement(0x2836)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_055_VOLTAGE, new UnsignedWordElement(0x2837)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_056_VOLTAGE, new UnsignedWordElement(0x2838)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_057_VOLTAGE, new UnsignedWordElement(0x2839)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_058_VOLTAGE, new UnsignedWordElement(0x283A)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_059_VOLTAGE, new UnsignedWordElement(0x283B)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_060_VOLTAGE, new UnsignedWordElement(0x283C)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_061_VOLTAGE, new UnsignedWordElement(0x283D)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_062_VOLTAGE, new UnsignedWordElement(0x283E)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_063_VOLTAGE, new UnsignedWordElement(0x283F)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_064_VOLTAGE, new UnsignedWordElement(0x2840)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_065_VOLTAGE, new UnsignedWordElement(0x2841)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_066_VOLTAGE, new UnsignedWordElement(0x2842)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_067_VOLTAGE, new UnsignedWordElement(0x2843)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_068_VOLTAGE, new UnsignedWordElement(0x2844)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_069_VOLTAGE, new UnsignedWordElement(0x2845)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_070_VOLTAGE, new UnsignedWordElement(0x2846)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_071_VOLTAGE, new UnsignedWordElement(0x2847)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_072_VOLTAGE, new UnsignedWordElement(0x2848)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_073_VOLTAGE, new UnsignedWordElement(0x2849)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_074_VOLTAGE, new UnsignedWordElement(0x284A)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_075_VOLTAGE, new UnsignedWordElement(0x284B)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_076_VOLTAGE, new UnsignedWordElement(0x284C)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_077_VOLTAGE, new UnsignedWordElement(0x284D)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_078_VOLTAGE, new UnsignedWordElement(0x284E)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_079_VOLTAGE, new UnsignedWordElement(0x284F)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_080_VOLTAGE, new UnsignedWordElement(0x2850)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_081_VOLTAGE, new UnsignedWordElement(0x2851)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_082_VOLTAGE, new UnsignedWordElement(0x2852)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_083_VOLTAGE, new UnsignedWordElement(0x2853)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_084_VOLTAGE, new UnsignedWordElement(0x2854)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_085_VOLTAGE, new UnsignedWordElement(0x2855)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_086_VOLTAGE, new UnsignedWordElement(0x2856)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_087_VOLTAGE, new UnsignedWordElement(0x2857)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_088_VOLTAGE, new UnsignedWordElement(0x2858)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_089_VOLTAGE, new UnsignedWordElement(0x2859)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_090_VOLTAGE, new UnsignedWordElement(0x285A)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_091_VOLTAGE, new UnsignedWordElement(0x285B)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_092_VOLTAGE, new UnsignedWordElement(0x285C)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_093_VOLTAGE, new UnsignedWordElement(0x285D)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_094_VOLTAGE, new UnsignedWordElement(0x285E)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_095_VOLTAGE, new UnsignedWordElement(0x285F)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_096_VOLTAGE, new UnsignedWordElement(0x2860)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_097_VOLTAGE, new UnsignedWordElement(0x2861)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_098_VOLTAGE, new UnsignedWordElement(0x2862)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_099_VOLTAGE, new UnsignedWordElement(0x2863)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_100_VOLTAGE, new UnsignedWordElement(0x2864)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_101_VOLTAGE, new UnsignedWordElement(0x2865)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_102_VOLTAGE, new UnsignedWordElement(0x2866)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_103_VOLTAGE, new UnsignedWordElement(0x2867)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_104_VOLTAGE, new UnsignedWordElement(0x2868)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_105_VOLTAGE, new UnsignedWordElement(0x2869)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_106_VOLTAGE, new UnsignedWordElement(0x286A)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_107_VOLTAGE, new UnsignedWordElement(0x286B)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_108_VOLTAGE, new UnsignedWordElement(0x286C)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_109_VOLTAGE, new UnsignedWordElement(0x286D)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_110_VOLTAGE, new UnsignedWordElement(0x286E)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_111_VOLTAGE, new UnsignedWordElement(0x286F)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_112_VOLTAGE, new UnsignedWordElement(0x2870)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_113_VOLTAGE, new UnsignedWordElement(0x2871)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_114_VOLTAGE, new UnsignedWordElement(0x2872)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_115_VOLTAGE, new UnsignedWordElement(0x2873)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_116_VOLTAGE, new UnsignedWordElement(0x2874)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_117_VOLTAGE, new UnsignedWordElement(0x2875)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_118_VOLTAGE, new UnsignedWordElement(0x2876)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_119_VOLTAGE, new UnsignedWordElement(0x2877)) //

				), //
				new FC3ReadRegistersTask(0x2878, Priority.LOW, //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_120_VOLTAGE, new UnsignedWordElement(0x2878)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_121_VOLTAGE, new UnsignedWordElement(0x2879)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_122_VOLTAGE, new UnsignedWordElement(0x287A)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_123_VOLTAGE, new UnsignedWordElement(0x287B)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_124_VOLTAGE, new UnsignedWordElement(0x287C)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_125_VOLTAGE, new UnsignedWordElement(0x287D)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_126_VOLTAGE, new UnsignedWordElement(0x287E)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_127_VOLTAGE, new UnsignedWordElement(0x287F)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_128_VOLTAGE, new UnsignedWordElement(0x2880)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_129_VOLTAGE, new UnsignedWordElement(0x2881)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_130_VOLTAGE, new UnsignedWordElement(0x2882)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_131_VOLTAGE, new UnsignedWordElement(0x2883)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_132_VOLTAGE, new UnsignedWordElement(0x2884)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_133_VOLTAGE, new UnsignedWordElement(0x2885)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_134_VOLTAGE, new UnsignedWordElement(0x2886)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_135_VOLTAGE, new UnsignedWordElement(0x2887)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_136_VOLTAGE, new UnsignedWordElement(0x2888)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_137_VOLTAGE, new UnsignedWordElement(0x2889)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_138_VOLTAGE, new UnsignedWordElement(0x288A)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_139_VOLTAGE, new UnsignedWordElement(0x288B)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_140_VOLTAGE, new UnsignedWordElement(0x288C)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_141_VOLTAGE, new UnsignedWordElement(0x288D)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_142_VOLTAGE, new UnsignedWordElement(0x288E)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_143_VOLTAGE, new UnsignedWordElement(0x288F)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_144_VOLTAGE, new UnsignedWordElement(0x2890)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_145_VOLTAGE, new UnsignedWordElement(0x2891)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_146_VOLTAGE, new UnsignedWordElement(0x2892)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_147_VOLTAGE, new UnsignedWordElement(0x2893)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_148_VOLTAGE, new UnsignedWordElement(0x2894)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_149_VOLTAGE, new UnsignedWordElement(0x2895)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_150_VOLTAGE, new UnsignedWordElement(0x2896)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_151_VOLTAGE, new UnsignedWordElement(0x2897)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_152_VOLTAGE, new UnsignedWordElement(0x2898)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_153_VOLTAGE, new UnsignedWordElement(0x2899)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_154_VOLTAGE, new UnsignedWordElement(0x289A)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_155_VOLTAGE, new UnsignedWordElement(0x289B)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_156_VOLTAGE, new UnsignedWordElement(0x289C)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_157_VOLTAGE, new UnsignedWordElement(0x289D)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_158_VOLTAGE, new UnsignedWordElement(0x289E)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_159_VOLTAGE, new UnsignedWordElement(0x289F)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_160_VOLTAGE, new UnsignedWordElement(0x28A0)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_161_VOLTAGE, new UnsignedWordElement(0x28A1)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_162_VOLTAGE, new UnsignedWordElement(0x28A2)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_163_VOLTAGE, new UnsignedWordElement(0x28A3)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_164_VOLTAGE, new UnsignedWordElement(0x28A4)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_165_VOLTAGE, new UnsignedWordElement(0x28A5)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_166_VOLTAGE, new UnsignedWordElement(0x28A6)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_167_VOLTAGE, new UnsignedWordElement(0x28A7)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_168_VOLTAGE, new UnsignedWordElement(0x28A8)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_169_VOLTAGE, new UnsignedWordElement(0x28A9)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_170_VOLTAGE, new UnsignedWordElement(0x28AA)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_171_VOLTAGE, new UnsignedWordElement(0x28AB)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_172_VOLTAGE, new UnsignedWordElement(0x28AC)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_173_VOLTAGE, new UnsignedWordElement(0x28AD)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_174_VOLTAGE, new UnsignedWordElement(0x28AE)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_175_VOLTAGE, new UnsignedWordElement(0x28AF)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_176_VOLTAGE, new UnsignedWordElement(0x28B0)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_177_VOLTAGE, new UnsignedWordElement(0x28B1)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_178_VOLTAGE, new UnsignedWordElement(0x28B2)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_179_VOLTAGE, new UnsignedWordElement(0x28B3)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_180_VOLTAGE, new UnsignedWordElement(0x28B4)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_181_VOLTAGE, new UnsignedWordElement(0x28B5)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_182_VOLTAGE, new UnsignedWordElement(0x28B6)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_183_VOLTAGE, new UnsignedWordElement(0x28B7)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_184_VOLTAGE, new UnsignedWordElement(0x28B8)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_185_VOLTAGE, new UnsignedWordElement(0x28B9)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_186_VOLTAGE, new UnsignedWordElement(0x28BA)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_187_VOLTAGE, new UnsignedWordElement(0x28BB)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_188_VOLTAGE, new UnsignedWordElement(0x28BC)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_189_VOLTAGE, new UnsignedWordElement(0x28BD)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_190_VOLTAGE, new UnsignedWordElement(0x28BE)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_191_VOLTAGE, new UnsignedWordElement(0x28BF)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_192_VOLTAGE, new UnsignedWordElement(0x28C0)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_193_VOLTAGE, new UnsignedWordElement(0x28C1)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_194_VOLTAGE, new UnsignedWordElement(0x28C2)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_195_VOLTAGE, new UnsignedWordElement(0x28C3)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_196_VOLTAGE, new UnsignedWordElement(0x28C4)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_197_VOLTAGE, new UnsignedWordElement(0x28C5)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_198_VOLTAGE, new UnsignedWordElement(0x28C6)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_199_VOLTAGE, new UnsignedWordElement(0x28C7)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_200_VOLTAGE, new UnsignedWordElement(0x28C8)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_201_VOLTAGE, new UnsignedWordElement(0x28C9)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_202_VOLTAGE, new UnsignedWordElement(0x28CA)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_203_VOLTAGE, new UnsignedWordElement(0x28CB)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_204_VOLTAGE, new UnsignedWordElement(0x28CC)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_205_VOLTAGE, new UnsignedWordElement(0x28CD)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_206_VOLTAGE, new UnsignedWordElement(0x28CE)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_207_VOLTAGE, new UnsignedWordElement(0x28CF)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_208_VOLTAGE, new UnsignedWordElement(0x28D0)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_209_VOLTAGE, new UnsignedWordElement(0x28D1)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_210_VOLTAGE, new UnsignedWordElement(0x28D2)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_211_VOLTAGE, new UnsignedWordElement(0x28D3)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_212_VOLTAGE, new UnsignedWordElement(0x28D4)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_213_VOLTAGE, new UnsignedWordElement(0x28D5)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_214_VOLTAGE, new UnsignedWordElement(0x28D6)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_215_VOLTAGE, new UnsignedWordElement(0x28D7)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_216_VOLTAGE, new UnsignedWordElement(0x28D8)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_217_VOLTAGE, new UnsignedWordElement(0x28D9)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_218_VOLTAGE, new UnsignedWordElement(0x28DA)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_219_VOLTAGE, new UnsignedWordElement(0x28DB)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_220_VOLTAGE, new UnsignedWordElement(0x28DC)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_221_VOLTAGE, new UnsignedWordElement(0x28DD)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_222_VOLTAGE, new UnsignedWordElement(0x28DE)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_223_VOLTAGE, new UnsignedWordElement(0x28DF)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_224_VOLTAGE, new UnsignedWordElement(0x28E0)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_225_VOLTAGE, new UnsignedWordElement(0x28E1)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_226_VOLTAGE, new UnsignedWordElement(0x28E2)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_227_VOLTAGE, new UnsignedWordElement(0x28E3)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_228_VOLTAGE, new UnsignedWordElement(0x28E4)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_229_VOLTAGE, new UnsignedWordElement(0x28E5)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_230_VOLTAGE, new UnsignedWordElement(0x28E6)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_231_VOLTAGE, new UnsignedWordElement(0x28E7)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_232_VOLTAGE, new UnsignedWordElement(0x28E8)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_233_VOLTAGE, new UnsignedWordElement(0x28E9)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_234_VOLTAGE, new UnsignedWordElement(0x28EA)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_235_VOLTAGE, new UnsignedWordElement(0x28EB)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_236_VOLTAGE, new UnsignedWordElement(0x28EC)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_237_VOLTAGE, new UnsignedWordElement(0x28ED)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_238_VOLTAGE, new UnsignedWordElement(0x28EE)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_239_VOLTAGE, new UnsignedWordElement(0x28EF)) //

				), //
				new FC3ReadRegistersTask(0x2C00, Priority.LOW, //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_00_TEMPERATURE, new SignedWordElement(0x2C00)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_01_TEMPERATURE, new SignedWordElement(0x2C01)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_02_TEMPERATURE, new SignedWordElement(0x2C02)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_03_TEMPERATURE, new SignedWordElement(0x2C03)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_04_TEMPERATURE, new SignedWordElement(0x2C04)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_05_TEMPERATURE, new SignedWordElement(0x2C05)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_06_TEMPERATURE, new SignedWordElement(0x2C06)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_07_TEMPERATURE, new SignedWordElement(0x2C07)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_08_TEMPERATURE, new SignedWordElement(0x2C08)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_09_TEMPERATURE, new SignedWordElement(0x2C09)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_10_TEMPERATURE, new SignedWordElement(0x2C0A)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_11_TEMPERATURE, new SignedWordElement(0x2C0B)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_12_TEMPERATURE, new SignedWordElement(0x2C0C)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_13_TEMPERATURE, new SignedWordElement(0x2C0D)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_14_TEMPERATURE, new SignedWordElement(0x2C0E)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_15_TEMPERATURE, new SignedWordElement(0x2C0F)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_16_TEMPERATURE, new SignedWordElement(0x2C10)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_17_TEMPERATURE, new SignedWordElement(0x2C11)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_18_TEMPERATURE, new SignedWordElement(0x2C12)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_19_TEMPERATURE, new SignedWordElement(0x2C13)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_20_TEMPERATURE, new SignedWordElement(0x2C14)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_21_TEMPERATURE, new SignedWordElement(0x2C15)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_22_TEMPERATURE, new SignedWordElement(0x2C16)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_23_TEMPERATURE, new SignedWordElement(0x2C17)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_24_TEMPERATURE, new SignedWordElement(0x2C18)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_25_TEMPERATURE, new SignedWordElement(0x2C19)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_26_TEMPERATURE, new SignedWordElement(0x2C1A)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_27_TEMPERATURE, new SignedWordElement(0x2C1B)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_28_TEMPERATURE, new SignedWordElement(0x2C1C)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_29_TEMPERATURE, new SignedWordElement(0x2C1D)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_30_TEMPERATURE, new SignedWordElement(0x2C1E)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_31_TEMPERATURE, new SignedWordElement(0x2C1F)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_32_TEMPERATURE, new SignedWordElement(0x2C20)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_33_TEMPERATURE, new SignedWordElement(0x2C21)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_34_TEMPERATURE, new SignedWordElement(0x2C22)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_35_TEMPERATURE, new SignedWordElement(0x2C23)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_36_TEMPERATURE, new SignedWordElement(0x2C24)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_37_TEMPERATURE, new SignedWordElement(0x2C25)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_38_TEMPERATURE, new SignedWordElement(0x2C26)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_39_TEMPERATURE, new SignedWordElement(0x2C27)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_40_TEMPERATURE, new SignedWordElement(0x2C28)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_41_TEMPERATURE, new SignedWordElement(0x2C29)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_42_TEMPERATURE, new SignedWordElement(0x2C2A)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_43_TEMPERATURE, new SignedWordElement(0x2C2B)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_44_TEMPERATURE, new SignedWordElement(0x2C2C)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_45_TEMPERATURE, new SignedWordElement(0x2C2D)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_46_TEMPERATURE, new SignedWordElement(0x2C2E)), //
						m(SingleRack.ChannelId.CLUSTER_1_BATTERY_47_TEMPERATURE, new SignedWordElement(0x2C2F)))); //
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode));
	}

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		// TODO start stop is not implemented
		throw new NotImplementedException("Start Stop is not implemented for Soltaro Version A");
	}
}
