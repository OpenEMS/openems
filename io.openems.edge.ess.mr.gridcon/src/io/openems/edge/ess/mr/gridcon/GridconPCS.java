package io.openems.edge.ess.mr.gridcon;

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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.FloatWriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.OptionsEnum;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

import io.openems.edge.ess.power.api.CircleConstraint;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.MR.Gridcon", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
) //
public class GridconPCS extends AbstractOpenemsModbusComponent
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(GridconPCS.class);

	protected static final int MAX_APPARENT_POWER = 100000;
	private CircleConstraint maxApparentPowerConstraint = null;

	@Reference
	private Power power;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private Battery battery;

	public GridconPCS() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		// update filter for 'battery'
		if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "battery", config.battery_id())) {
			return;
		}

		/*
		 * Initialize Power
		 */
		// Max Apparent
		// TODO adjust apparent power from modbus element
		this.maxApparentPowerConstraint = new CircleConstraint(this, MAX_APPARENT_POWER);
		
		
		super.activate(context, config.service_pid(), config.id(), config.enabled(), config.unit_id(), this.cm, "Modbus",
				config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}
	
	private void handleStateMachine() {
		// TODO
		// see Software manual chapter 5.1
		if (isOffline()) {
			startSystem();
		} else if (isError()) {
			doErrorHandling();
		}
	}

	private boolean isError() {
		// TODO
		return false;
	}

	private boolean isOffline() {
		// TODO
		return false;
	}

	private void startSystem() {
		// TODO
		log.info("Try to start system");
	}

	private void doErrorHandling() {
		// TODO		
	}
	
	@Override
	public String debugLog() {
		return "Current state: " + this.channel(ChannelId.SYSTEM_CURRENT_STATE).value().asOptionString();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
		FloatWriteChannel channelPRef = this.channel(ChannelId.PCS_COMMAND_CONTROL_PARAMETER_P_REF);
		FloatWriteChannel channelQRef = this.channel(ChannelId.PCS_COMMAND_CONTROL_PARAMETER_Q_REF);
		try {
			channelPRef.setNextWriteValue((float) activePower);
			channelQRef.setNextWriteValue((float) reactivePower);
		} catch (OpenemsException e) {
			log.error("problem occurred while trying to set active/reactive power" + e.getMessage());
		}		
	}

	@Override
	public int getPowerPrecision() {
		// TODO
		return 100;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			handleStateMachine();
			break;
		}
	}

	// TODO numbers are not correctly
	public enum CurrentState implements OptionsEnum {  // see Software manual chapter 5.1
		OFFLINE(1, "Offline"),
		INIT(2, "Init"),
		IDLE(3, "Idle"),
		PRECHARGE(4, "Precharge"),
		STOP_PRECHARGE(5, "Stop precharge"),
		ECO(6, "Eco"),
		PAUSE(7, "Pause"),
		RUN(8, "Run"),
		ERROR(99, "Error");

		int value;
		String option;

		private CurrentState(int value, String option) {
			this.value = value;
			this.option = option;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getOption() {
			return option;
		}
	}
	
	public enum Command implements OptionsEnum {  // see manual(Betriebsanleitung Feldbus Konfiguration (Anybus-Modul)) page 15
		PLAY(1, "Start active filter"),
		PAUSE(2, "Set outgoing current of ACF to zero"),
		ACKNOWLEDGE(4, "Achnowledge errors"),
		STOP(8, "Switch off");
		
		int value;
		String option;

		private Command(int value, String option) {
			this.value = value;
			this.option = option;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getOption() {
			return option;
		}
	}

	// TODO Is this implemented according SunSpec?
	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		SYSTEM_CURRENT_STATE(new Doc().options(CurrentState.values())), //
		SYSTEM_CURRENT_PARAMETER_SET(new Doc()), //
		SYSTEM_UTILIZATION(new Doc().unit(Unit.PERCENT)),
		SYSTEM_SERVICE_MODE(new Doc().unit(Unit.ON_OFF)),
		SYSTEM_REMOTE_MODE(new Doc().unit(Unit.ON_OFF)),
		SYSTEM_MEASUREMENTS_LIFEBIT(new Doc().unit(Unit.ON_OFF)),
		SYSTEM_CCU_LIFEBIT(new Doc().unit(Unit.ON_OFF)),
		SYSTEM_NUMBER_ERROR_WARNINGS(new Doc().unit(Unit.NONE)),
		SYSTEM_COMMAND(new Doc().options(Command.values())),
		SYSTEM_PARAMETER_SET(new Doc()),
		SYSTEM_FIELDBUS_DEVICE_LIFEBIT(new Doc().unit(Unit.ON_OFF)),
		SYSTEM_ERROR_CODE(new Doc().unit(Unit.NONE)),
		SYSTEM_ERROR_ACKNOWLEDGE(new Doc().unit(Unit.NONE)),
		
		ACF_VOLTAGE_RMS_L12(new Doc().unit(Unit.VOLT)),
		ACF_VOLTAGE_RMS_L23(new Doc().unit(Unit.VOLT)),
		ACF_VOLTAGE_RMS_L31(new Doc().unit(Unit.VOLT)),
		ACF_RELATIVE_THD_FACTOR(new Doc().unit(Unit.PERCENT)),
		ACF_FREQUENCY(new Doc().unit(Unit.HERTZ)),
		ACF_CURRENT_RMS_L1(new Doc().unit(Unit.AMPERE)),
		ACF_CURRENT_RMS_L2(new Doc().unit(Unit.AMPERE)),
		ACF_CURRENT_RMS_L3(new Doc().unit(Unit.AMPERE)),
		ACF_ABSOLUTE_THD_FACTOR(new Doc().unit(Unit.AMPERE)),
		ACF_DISTORSION_POWER(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)),
		
		PCS_CCU_STATE_IDLE(new Doc()), 
		PCS_CCU_STATE_PRE_CHARGE(new Doc()),
		PCS_CCU_STATE_STOP_PRE_CHARGE(new Doc()),
		PCS_CCU_STATE_READY(new Doc()),
		PCS_CCU_STATE_PAUSE(new Doc()),
		PCS_CCU_STATE_RUN(new Doc()),
		PCS_CCU_STATE_ERROR(new Doc()),
		PCS_CCU_STATE_VOLTAGE_RAMPING_UP(new Doc()),
		PCS_CCU_STATE_OVERLOAD(new Doc()),
		PCS_CCU_STATE_SHORT_CIRCUIT_DETECTED(new Doc()),
		PCS_CCU_STATE_DERATING_POWER(new Doc()),
		PCS_CCU_STATE_DERATING_HARMONICS(new Doc()),
		PCS_CCU_STATE_SIA_ACTIVE(new Doc()),
//		CCU_STATE(new Doc() //TODO so gehts nicht, es können mehrere Werte gesetzt sein 
//				.option(1, "IDLE") //
//				.option(2, "Pre-Charge") //
//				.option(4, "Stop Pre-Charge") //
//				.option(8, "READY") //
//				.option(16, "PAUSE") // ,
//				.option(32, "RUN") //
//				.option(64, "Error") //
//				.option(128, "Voltage ramping up") //
//				.option(256, "Overload") //
//				.option(512, "Short circuit detected") //
//				.option(1024, "Derating power") //
//				.option(2048, "Derating harmonics") //
//				.option(4096, "SIA active")), //
		CCU_ERROR_CODE(new Doc().unit(Unit.NONE)),
		CCU_VOLTAGE_U12(new Doc().unit(Unit.VOLT)),
		CCU_VOLTAGE_U23(new Doc().unit(Unit.VOLT)),
		CCU_VOLTAGE_U31(new Doc().unit(Unit.VOLT)),
		CCU_CURRENT_IL1(new Doc().unit(Unit.AMPERE)),
		CCU_CURRENT_IL2(new Doc().unit(Unit.AMPERE)),
		CCU_CURRENT_IL3(new Doc().unit(Unit.AMPERE)),
		CCU_POWER_P(new Doc().unit(Unit.WATT)),
		CCU_POWER_Q(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)),
		CCU_FREQUENCY(new Doc().unit(Unit.HERTZ)),
		PCS_COMMAND_CONTROL_WORD_PLAY(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_READY(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ACKNOWLEDGE(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_STOP(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_BLACKSTART_APPROVAL(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_SYNC_APPROVAL(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ACTIVATE_SHORT_CIRCUIT_HANDLING(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_MODE_SELECTION(new Doc().unit(Unit.ON_OFF)), //0=voltage control, 1=current control
		PCS_COMMAND_CONTROL_WORD_TRIGGER_SIA(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ACTIVATE_HARMONIC_COMPENSATION(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ID_1_SD_CARD_PARAMETER_SET(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ID_2_SD_CARD_PARAMETER_SET(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ID_3_SD_CARD_PARAMETER_SET(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ID_4_SD_CARD_PARAMETER_SET(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ENABLE_IPU_4(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ENABLE_IPU_3(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ENABLE_IPU_2(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_CONTROL_WORD_ENABLE_IPU_1(new Doc().unit(Unit.ON_OFF)),
		PCS_COMMAND_ERROR_CODE_FALLBACK(new Doc()),
		PCS_COMMAND_CONTROL_PARAMETER_U0(new Doc()),
		PCS_COMMAND_CONTROL_PARAMETER_F0(new Doc()),
		PCS_COMMAND_CONTROL_PARAMETER_Q_REF(new Doc()),
		PCS_COMMAND_CONTROL_PARAMETER_P_REF(new Doc()),
		PCS_COMMAND_TIME_SYNC_DATE(new Doc()),
		PCS_COMMAND_TIME_SYNC_TIME(new Doc())
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol(int unitId) {
		return new ModbusProtocol(unitId, //
				new FC3ReadRegistersTask(528, Priority.LOW, //
						m(GridconPCS.ChannelId.SYSTEM_CURRENT_STATE, new UnsignedWordElement(528)), //
						m(GridconPCS.ChannelId.SYSTEM_CURRENT_PARAMETER_SET, new UnsignedWordElement(529)), // TODO parameter set and utilization are separated in half bytes, how to handle?
						bm(new UnsignedWordElement(530)) //
						.m(GridconPCS.ChannelId.SYSTEM_SERVICE_MODE, 1) //
						.m(GridconPCS.ChannelId.SYSTEM_REMOTE_MODE, 2) //
						.m(GridconPCS.ChannelId.SYSTEM_MEASUREMENTS_LIFEBIT, 6) //
						.m(GridconPCS.ChannelId.SYSTEM_CCU_LIFEBIT, 7) //						
						.build(), //
						m(GridconPCS.ChannelId.SYSTEM_NUMBER_ERROR_WARNINGS, new UnsignedWordElement(531)) //
				)
				, new FC16WriteRegistersTask(560,
						m(GridconPCS.ChannelId.SYSTEM_COMMAND, new UnsignedWordElement(560)), //
						m(GridconPCS.ChannelId.SYSTEM_PARAMETER_SET, new UnsignedWordElement(561)), //
						bm(new UnsignedWordElement(562)) //
						.m(GridconPCS.ChannelId.SYSTEM_FIELDBUS_DEVICE_LIFEBIT, 7) //						
						.build() //
				)
				, new FC3ReadRegistersTask(592, Priority.LOW, //)
						m(GridconPCS.ChannelId.SYSTEM_ERROR_CODE, new UnsignedDoublewordElement(592)) //
				)
				, new FC16WriteRegistersTask(624,
						m(GridconPCS.ChannelId.SYSTEM_ERROR_ACKNOWLEDGE, new UnsignedDoublewordElement(624))
				)
				, new FC3ReadRegistersTask(1808, Priority.LOW, //)
						m(GridconPCS.ChannelId.ACF_VOLTAGE_RMS_L12, new FloatDoublewordElement(1808)), //
						m(GridconPCS.ChannelId.ACF_VOLTAGE_RMS_L23, new FloatDoublewordElement(1810)), //
						m(GridconPCS.ChannelId.ACF_VOLTAGE_RMS_L31, new FloatDoublewordElement(1812)), //
						m(GridconPCS.ChannelId.ACF_RELATIVE_THD_FACTOR, new FloatDoublewordElement(1814)), //
						m(GridconPCS.ChannelId.ACF_FREQUENCY, new FloatDoublewordElement(1816)) //
				)
				, new FC3ReadRegistersTask(1904, Priority.LOW, //)
						m(GridconPCS.ChannelId.ACF_CURRENT_RMS_L1, new FloatDoublewordElement(1904)), //
						m(GridconPCS.ChannelId.ACF_CURRENT_RMS_L2, new FloatDoublewordElement(1906)), //
						m(GridconPCS.ChannelId.ACF_CURRENT_RMS_L3, new FloatDoublewordElement(1908)), //
						m(GridconPCS.ChannelId.ACF_ABSOLUTE_THD_FACTOR, new FloatDoublewordElement(1910)) //
				)
				, new FC3ReadRegistersTask(2064, Priority.LOW, //)
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(2064)), //
						m(SymmetricEss.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(2066)), //
						m(GridconPCS.ChannelId.ACF_DISTORSION_POWER, new FloatDoublewordElement(2068)) //
				)	
				, new FC3ReadRegistersTask(32528, Priority.LOW, //)						
						bm(new UnsignedDoublewordElement(32528)) //
						.m(GridconPCS.ChannelId.PCS_CCU_STATE_IDLE, 0) //
						.m(GridconPCS.ChannelId.PCS_CCU_STATE_PRE_CHARGE, 1) //
						.m(GridconPCS.ChannelId.PCS_CCU_STATE_STOP_PRE_CHARGE, 2) //
						.m(GridconPCS.ChannelId.PCS_CCU_STATE_READY, 3) //
						.m(GridconPCS.ChannelId.PCS_CCU_STATE_PAUSE, 4) //
						.m(GridconPCS.ChannelId.PCS_CCU_STATE_RUN, 5) //
						.m(GridconPCS.ChannelId.PCS_CCU_STATE_ERROR, 6) //
						.m(GridconPCS.ChannelId.PCS_CCU_STATE_VOLTAGE_RAMPING_UP, 7) //
						.m(GridconPCS.ChannelId.PCS_CCU_STATE_OVERLOAD, 8) //
						.m(GridconPCS.ChannelId.PCS_CCU_STATE_SHORT_CIRCUIT_DETECTED, 9) //
						.m(GridconPCS.ChannelId.PCS_CCU_STATE_DERATING_POWER, 10) //
						.m(GridconPCS.ChannelId.PCS_CCU_STATE_DERATING_HARMONICS, 11) //
						.m(GridconPCS.ChannelId.PCS_CCU_STATE_SIA_ACTIVE, 12) //
						.build(), //
						m(GridconPCS.ChannelId.CCU_ERROR_CODE, new UnsignedDoublewordElement(32530)), //
						m(GridconPCS.ChannelId.CCU_VOLTAGE_U12, new FloatDoublewordElement(32532)), //
						m(GridconPCS.ChannelId.CCU_VOLTAGE_U23, new FloatDoublewordElement(32534)), //
						m(GridconPCS.ChannelId.CCU_VOLTAGE_U31, new FloatDoublewordElement(32536)), //
						m(GridconPCS.ChannelId.CCU_CURRENT_IL1, new FloatDoublewordElement(32538)), //
						m(GridconPCS.ChannelId.CCU_CURRENT_IL2, new FloatDoublewordElement(32540)), //
						m(GridconPCS.ChannelId.CCU_CURRENT_IL3, new FloatDoublewordElement(32542)), //
						m(GridconPCS.ChannelId.CCU_POWER_P, new FloatDoublewordElement(32544)), //
						m(GridconPCS.ChannelId.CCU_POWER_Q, new FloatDoublewordElement(32546)), //
						m(GridconPCS.ChannelId.CCU_FREQUENCY, new FloatDoublewordElement(32548)) //
				)	
				, new FC16WriteRegistersTask(32560,
						bm(new UnsignedDoublewordElement(32560)) //
						.m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_WORD_PLAY, 0) //
						.m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_WORD_READY, 1) //
						.m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_WORD_ACKNOWLEDGE, 2) //
						.m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_WORD_STOP, 3) //
						.m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_WORD_BLACKSTART_APPROVAL, 4) //
						.m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_WORD_SYNC_APPROVAL, 5) //
						.m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_WORD_ACTIVATE_SHORT_CIRCUIT_HANDLING, 6) //
						.m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_WORD_MODE_SELECTION, 7) //
						.m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_WORD_TRIGGER_SIA, 8) //
						.m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_WORD_ACTIVATE_HARMONIC_COMPENSATION, 9) //
						.m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_WORD_ID_1_SD_CARD_PARAMETER_SET, 10) //
						.m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_WORD_ID_2_SD_CARD_PARAMETER_SET, 11) //
						.m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_WORD_ID_3_SD_CARD_PARAMETER_SET, 12) //
						.m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_WORD_ID_4_SD_CARD_PARAMETER_SET, 13) //
						.m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_WORD_ENABLE_IPU_4, 28) //
						.m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_WORD_ENABLE_IPU_3, 29) //
						.m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_WORD_ENABLE_IPU_2, 30) //
						.m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_WORD_ENABLE_IPU_1, 31) //
						.build(), //
						m(GridconPCS.ChannelId.PCS_COMMAND_ERROR_CODE_FALLBACK, new UnsignedDoublewordElement(32562)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_PARAMETER_U0, new UnsignedDoublewordElement(32564)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_PARAMETER_F0, new UnsignedDoublewordElement(32566)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_PARAMETER_Q_REF, new UnsignedDoublewordElement(32568)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_CONTROL_PARAMETER_P_REF, new UnsignedDoublewordElement(32570)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_TIME_SYNC_DATE, new UnsignedDoublewordElement(32572)), //
						m(GridconPCS.ChannelId.PCS_COMMAND_TIME_SYNC_TIME, new UnsignedDoublewordElement(32574)) //
				)
			);
	}
}
