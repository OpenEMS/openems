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

import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.OptionsEnum;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
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
//	private CircleConstraint maxApparentPowerConstraint = null;

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
//		this.maxApparentPowerConstraint = new CircleConstraint(this, MAX_APPARENT_POWER);
		
		
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
		return "Current state: " + this.channel(ChannelId.CURRENT_STATE).value().asOptionString();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {

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

	// TODO Is this implemented according SunSpec?
	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		CURRENT_STATE(new Doc().options(CurrentState.values())) //
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
		// TODO
		return new ModbusProtocol(unitId, //
				new FC3ReadRegistersTask(1000, Priority.LOW,
						m(GridconPCS.ChannelId.CURRENT_STATE, new UnsignedWordElement(1000)
						)),
				new FC6WriteRegisterTask(1000,
						m(GridconPCS.ChannelId.CURRENT_STATE, new UnsignedWordElement(1000))
						));
	}
}
