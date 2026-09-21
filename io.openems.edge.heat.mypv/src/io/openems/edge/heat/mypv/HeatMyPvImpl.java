package io.openems.edge.heat.mypv;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.channel.ChannelUtils.setWriteValueIfNotRead;
import static io.openems.edge.energy.api.handler.RescheduleMode.OPTIMIZE_CURRENT_PERIOD;
import static io.openems.edge.meter.api.ElectricityMeter.calculateAverageVoltageFromPhases;
import static io.openems.edge.meter.api.ElectricityMeter.calculateSumCurrentFromPhases;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.time.Clock;

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
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
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
import io.openems.edge.heat.mypv.statemachine.Context;
import io.openems.edge.heat.mypv.statemachine.StateMachine;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = HeatMyPvImpl.FACTORY_ID, //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=CONSUMPTION_METERED" //
		})
@GenerateTargetsFromReferences("Modbus")
public class HeatMyPvImpl extends AbstractOpenemsModbusComponent implements HeatMyPv, ModbusComponent, OpenemsComponent,
		Heat, ElectricityMeter, ManagedHeatElement, TimedataProvider, Controller, ComponentJsonApi, EnergySchedulable {

	public static final String FACTORY_ID = "Heat.MyPv";

	// gets the total energy consumption in kWh
	private final CalculateEnergyFromPower totalEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	// gets the energy consumption in kWh per phase
	private final CalculateEnergyFromPower phaseEnergyL1 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1);
	private final CalculateEnergyFromPower phaseEnergyL2 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2);
	private final CalculateEnergyFromPower phaseEnergyL3 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3);
	private final StateMachine stateMachine;

	private EshWithDifferentModes<Mode, EnergyScheduler.OptimizationContext, Void> energyScheduleHandler;

	private volatile Config config = null;
	private volatile JSCalendar.Tasks<HeatMyPvPayload> tasks = JSCalendar.Tasks.empty();

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private ConfigurationAdmin configurationAdmin;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	private Sum sum;

	@Override
	@Reference(//
			policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))" //
	)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public HeatMyPvImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Heat.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedHeatElement.ChannelId.values(), //
				Controller.ChannelId.values(), //
				HeatMyPv.ChannelId.values() //
		);
		this.stateMachine = new StateMachine(StateMachine.State.OFF);

		calculateSumCurrentFromPhases(this);
		calculateAverageVoltageFromPhases(this);
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
			this.energyScheduleHandler.triggerReschedule("HeatMyPvImpl::modified()", OPTIMIZE_CURRENT_PERIOD);
		}
	}

	private synchronized void applyConfig(Config config) {
		final Clock clock = this.componentManager != null ? this.componentManager.getClock() : Clock.systemUTC();
		this.config = config;
		this.tasks = JSCalendar.Tasks.fromStringOrEmpty(clock, config.jsCalendar(), HeatMyPvPayload.serializer());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		var protocol = new ModbusProtocol(this,
				new FC3ReadRegistersTask(1000, Priority.HIGH,
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedWordElement(1000)), //
						m(Heat.ChannelId.TEMPERATURE, new SignedWordElement(1001)), //
						new DummyRegisterElement(1002, 1060), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new SignedWordElement(1061)), //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new SignedWordElement(1062)), //
						new DummyRegisterElement(1063, 1066), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new SignedWordElement(1067)), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new SignedWordElement(1068)), //
						new DummyRegisterElement(1069, 1071), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new SignedWordElement(1072)), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new SignedWordElement(1073)), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new SignedWordElement(1074)), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new SignedWordElement(1075)), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new SignedWordElement(1076)), //
						m(Heat.ChannelId.STATUS, new SignedWordElement(1077)))); //

		if (!this.config.readOnly()) {
			protocol.addTask(new FC6WriteRegisterTask(1000, //
					m(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER, new SignedWordElement(1000))));
		}

		return protocol;
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.CONSUMPTION_METERED;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	public void setTargetActivePowerForHeatElement(Integer requestedActivePower) throws OpenemsNamedException {
		IntegerWriteChannel targetActivePowerChannel = this.channel(//
				ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER);
		setWriteValueIfNotRead(targetActivePowerChannel, this.clampTargetActivePower(requestedActivePower));
	}

	private int clampTargetActivePower(Integer requestedActivePower) {
		if (requestedActivePower == null) {
			return 0;
		}
		return Math.clamp(requestedActivePower, 0, this.config.maxHeatPower());
	}

	@Override
	public void run() throws OpenemsNamedException {
		final var currentMode = this.config.readOnly() //
				? this.config.mode() //
				: this.resolveCurrentMode();

		this.totalEnergy.update(this.getActivePower().orElse(0));
		this.phaseEnergyL1.update(this.getActivePowerL1().orElse(0));
		this.phaseEnergyL2.update(this.getActivePowerL2().orElse(0));
		this.phaseEnergyL3.update(this.getActivePowerL3().orElse(0));

		if (this.config.readOnly()) {
			// In read-only mode the scheduler is not applied.
			setValue(this, ManagedHeatElement.ChannelId.CONTROL_NOT_ALLOWED, true);
			setValue(this, HeatMyPv.ChannelId.FAST_HEAT_POWER_NOT_APPLIED, false);
		} else {
			setValue(this, ManagedHeatElement.ChannelId.CONTROL_NOT_ALLOWED, false);
			this.runStateMachine(currentMode);
		}

		setValue(this, HeatMyPv.ChannelId.STATE_MACHINE, this.stateMachine.getCurrentState());
		setValue(this, HeatMyPv.ChannelId.MODE, ChannelMode.fromMode(currentMode));
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

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		JSCalendarApi.buildJsonApiRoutes(builder, HeatMyPvPayload.serializer(), //
				() -> this.tasks, //
				() -> new UpdateJsCalendarRecord(this.configurationAdmin, this.componentManager, this.servicePid(),
						"jsCalendar"));
	}

	@Override
	public EnergyScheduleHandler getEnergyScheduleHandler() {
		return this.energyScheduleHandler;
	}
}
