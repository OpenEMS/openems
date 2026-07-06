package io.openems.edge.heat.askoma;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.channel.ChannelUtils.setWriteValueIfNotRead;
import static io.openems.edge.energy.api.handler.RescheduleMode.OPTIMIZE_CURRENT_PERIOD;
import static io.openems.edge.meter.api.ElectricityMeter.calculatePhasesFromActivePower;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

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
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.ChannelUtils;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.JSCalendarApi;
import io.openems.edge.common.jsonapi.JSCalendarApi.UpdateJsCalendarRecord;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.heat.api.Heat;
import io.openems.edge.heat.api.ManagedHeatElement;
import io.openems.edge.heat.api.Status;
import io.openems.edge.heat.askoma.jsonrpc.jsonrpc.GetSchedule;
import io.openems.edge.heat.askoma.statemachine.Context;
import io.openems.edge.heat.askoma.statemachine.StateMachine;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = HeatAskomaImpl.FACTORY_ID, //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=CONSUMPTION_METERED" //
		})
@GenerateTargetsFromReferences("Modbus")
public class HeatAskomaImpl extends AbstractOpenemsModbusComponent
		implements HeatAskoma, ModbusComponent, OpenemsComponent, ElectricityMeter, Heat, ManagedHeatElement,
		TimedataProvider, Controller, ComponentJsonApi, EnergySchedulable {

	public static final String FACTORY_ID = "Heat.Askoma";

	private final Logger log = LoggerFactory.getLogger(HeatAskomaImpl.class);

	// gets the total energy consumption in kWh
	private final CalculateEnergyFromPower totalEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

	private final StateMachine stateMachine;
	private EshWithDifferentModes<Mode, EnergyScheduler.OptimizationContext, Void> energyScheduleHandler;

	private volatile Config config = null;
	private volatile JSCalendar.Tasks<HeatAskomaPayload> tasks = JSCalendar.Tasks.empty();

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private ConfigurationAdmin configurationAdmin;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private Sum sum;

	@Reference(//
			policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))")
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public HeatAskomaImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				HeatAskoma.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Heat.ChannelId.values(), //
				ManagedHeatElement.ChannelId.values(), //
				Controller.ChannelId.values() //
		);
		this.stateMachine = new StateMachine(StateMachine.State.OFF);

		calculatePhasesFromActivePower(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.applyConfig(config);
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
		this.energyScheduleHandler = EnergyScheduler.buildEnergyScheduleHandler(this, this.componentManager,
				() -> this.config == null ? null
						: new EnergyScheduler.Config(this.config.mode(), this.config.maxHeatPower(), this.tasks));
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		this.applyConfig(config);
		super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
		if (this.energyScheduleHandler != null) {
			this.energyScheduleHandler.triggerReschedule("HeatAskomaImpl::modified()", OPTIMIZE_CURRENT_PERIOD);
		}
	}

	private synchronized void applyConfig(Config config) {
		this.config = config;
		this.tasks = JSCalendar.Tasks.fromStringOrEmpty(this.componentManager.getClock(), config.jsCalendar(),
				HeatAskomaPayload.serializer());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		var protocol = new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(109, Priority.HIGH, //
						new BitsWordElement(109, this) //
								.bit(0, HeatAskoma.ChannelId.HEATER1_ACTIVE) //
								.bit(1, HeatAskoma.ChannelId.HEATER2_ACTIVE) //
								.bit(2, HeatAskoma.ChannelId.HEATER3_ACTIVE) //
								.bit(3, HeatAskoma.ChannelId.PUMP_ACTIVE) //
								.bit(4, HeatAskoma.ChannelId.RELAYBOARD_IS_CONNECTED) //
								.bit(5, HeatAskoma.ChannelId.HEATER_1_2_3_CURRENT_FLOW) //
								.bit(6, HeatAskoma.ChannelId.HEAT_PUMP_REQUEST_ACTIVE) //
								.bit(7, HeatAskoma.ChannelId.EMERGENCY_MODE_ACTIVE) //
								.bit(8, HeatAskoma.ChannelId.LEGIONELLA_PROTECTION_ACTIVE) //
								.bit(9, HeatAskoma.ChannelId.ANALOG_INPUT_ACTIVE) //
								.bit(10, HeatAskoma.ChannelId.LOAD_SETPOINT_ACTIVE) //
								.bit(11, HeatAskoma.ChannelId.LOAD_FEEDIN_ACTIVE) //
								.bit(12, HeatAskoma.ChannelId.AUTO_HEATER_OFF_ACTIVE) //
								.bit(13, HeatAskoma.ChannelId.PUMP_RELAY_FOLLOW_UP_ACTIVE) //
								.bit(14, HeatAskoma.ChannelId.TEMPERATURE_LIMIT_REACHED) //
								.bit(15, HeatAskoma.ChannelId.ANY_ERROR_OCCURRED), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedWordElement(110))),

				new FC3ReadRegistersTask(597, Priority.LOW, //
						m(HeatAskoma.ChannelId.TEMPERATURE_SETPOINT, new UnsignedWordElement(597), SCALE_FACTOR_1)),

				new FC4ReadInputRegistersTask(638, Priority.HIGH, //
						m(Heat.ChannelId.TEMPERATURE, new UnsignedWordElement(638), SCALE_FACTOR_1))); //

		if (!this.config.readOnly()) {
			// Askoma spec: MODBUS_CMD_LOAD_FEEDIN_VALUE, signed int16, -30000..30000 W
			// values are negativ therefore the TARGET_GRID_ACTIVE_POWER is used instead of
			// TARGET_ACTIVE_POWER
			protocol.addTask(new FC3ReadRegistersTask(202, Priority.HIGH, //
					m(ManagedHeatElement.ChannelId.TARGET_GRID_ACTIVE_POWER, new SignedWordElement(202))));

			protocol.addTask(new FC6WriteRegisterTask(202, //
					m(ManagedHeatElement.ChannelId.TARGET_GRID_ACTIVE_POWER, new SignedWordElement(202))));
		}

		return protocol;
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.CONSUMPTION_METERED;
	}

	@Override
	public void run() throws OpenemsNamedException {
		// In read-only mode the scheduler is not applied; show the configured default.
		final var currentMode = this.config.readOnly() //
				? this.config.mode() //
				: this.resolveCurrentMode();

		this.totalEnergy.update(this.getActivePower().get());
		if (this.config.readOnly()) {
			// Write control is not allowed in read-only mode
			setValue(this, ManagedHeatElement.ChannelId.CONTROL_NOT_ALLOWED, true);
			this.setFastHeatPowerNotApplied(false);
		} else {
			setValue(this, ManagedHeatElement.ChannelId.CONTROL_NOT_ALLOWED, false);
			this.runStateMachine(currentMode);
		}
		setValue(this, HeatAskoma.ChannelId.STATE_MACHINE, this.stateMachine.getCurrentState());
		this.updateStatusChannel();
		this.updateModeChannel(currentMode);

	}

	private void updateModeChannel(Mode mode) {
		setValue(this, HeatAskoma.ChannelId.MODE, mode);
	}

	private void runStateMachine(Mode mode) throws OpenemsNamedException {
		var context = new Context(this, this.config, this.componentManager.getClock(), this.sum);
		if (!StateMachine.matchesMode(this.stateMachine.getCurrentState(), mode)) {
			this.stateMachine.forceNextState(StateMachine.fromMode(mode));
			this.stateMachine.run(context);
		}
		this.stateMachine.run(context);
	}

	private Mode resolveCurrentMode() {
		var activeTask = this.tasks.getActiveOneTask();
		if (activeTask != null) {
			return activeTask.payload().mode();
		}
		return this.config.mode();
	}

	public void setTargetActivePowerForHeatElement(Integer requestedActivePower) {
		IntegerWriteChannel targetActivePowerChannel = this.channel(//
				ManagedHeatElement.ChannelId.TARGET_GRID_ACTIVE_POWER);

		try {
			setWriteValueIfNotRead(targetActivePowerChannel, requestedActivePower);
		} catch (OpenemsNamedException e) {
			this.logError(this.log,
					"Unable to set TARGET_GRID_ACTIVE_POWER to [" + requestedActivePower + "]: " + e.getMessage());
		}
	}

	/**
	 * Gets the currently requested TARGET_GRID_ACTIVE_POWER value.
	 *
	 * <p>
	 * Prefers a pending write-value of the current cycle and falls back to the last
	 * read channel value.
	 *
	 * @return the requested target active power in watts or null if undefined
	 */
	public Integer getRequestedTargetGridActivePower() {
		IntegerWriteChannel channel = this.channel(ManagedHeatElement.ChannelId.TARGET_GRID_ACTIVE_POWER);
		return channel.getNextWriteValue().orElse(channel.value().get());
	}

	/**
	 * Sets the FAST_HEAT_POWER_NOT_APPLIED channel value.
	 *
	 * @param value the next value
	 */
	public void setFastHeatPowerNotApplied(boolean value) {
		ChannelUtils.setValue(this, HeatAskoma.ChannelId.FAST_HEAT_POWER_NOT_APPLIED, value);
	}

	protected void updateStatusChannel() {
		ChannelUtils.setValue(this, Heat.ChannelId.STATUS, this.calculateStatus());
	}

	private Status calculateStatus() {
		if (this.getHeaterCurrentFlow().orElse(false)) {
			// heating
			return Status.EXCESS;
		}
		if (this.getTemperatureLimiteReached().orElse(false)) {
			return Status.TEMPERATURE_REACHED;
		}
		// no heating
		return Status.NO_CONTROL_SIGNAL;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		JSCalendarApi.buildJsonApiRoutes(builder, HeatAskomaPayload.serializer(), //
				() -> this.tasks, //
				() -> new UpdateJsCalendarRecord(this.configurationAdmin, this.componentManager, this.servicePid(),
						"jsCalendar"));
		builder.handleRequest(new GetSchedule(), //
				call -> GetSchedule.Response.create(call.getRequest(), this.energyScheduleHandler));
	}

	@Override
	public EnergyScheduleHandler getEnergyScheduleHandler() {
		return this.energyScheduleHandler;
	}
}
