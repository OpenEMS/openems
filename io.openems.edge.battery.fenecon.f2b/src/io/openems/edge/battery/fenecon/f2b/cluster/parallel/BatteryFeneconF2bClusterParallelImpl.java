package io.openems.edge.battery.fenecon.f2b.cluster.parallel;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.stream.Collectors;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.f2b.BatteryFeneconF2b;
import io.openems.edge.battery.fenecon.f2b.DeviceSpecificOnSetNextValueHandler;
import io.openems.edge.battery.fenecon.f2b.cluster.common.AbstractBatteryFeneconF2bCluster;
import io.openems.edge.battery.fenecon.f2b.cluster.common.BatteryFeneconF2bCluster;
import io.openems.edge.battery.fenecon.f2b.cluster.common.ChannelManager;
import io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine.Context;
import io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine.StateMachine;
import io.openems.edge.battery.fenecon.f2b.cluster.common.statemachine.StateMachine.State;
import io.openems.edge.battery.protection.BatteryProtection;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Battery.Fenecon.F2B.Cluster.Parallel", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class BatteryFeneconF2bClusterParallelImpl extends AbstractBatteryFeneconF2bCluster
		implements BatteryFeneconF2bClusterParallel, BatteryFeneconF2bCluster, BatteryFeneconF2b, Battery,
		OpenemsComponent, EventHandler, ModbusSlave, StartStoppable {

	private final Logger log = LoggerFactory.getLogger(BatteryFeneconF2bClusterParallelImpl.class);
	private final ParallelChannelManager channelManager = new ParallelChannelManager(this);
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);
	private static final int ALLOWED_MAX_VOLTAGE_DIFFERENCE = 4;
	private boolean hvContactorUnlocked = false;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	protected ComponentManager componentManager;

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(!(service.factoryPid=Battery.Fenecon.F2B.Cluster.Parallel)))")
	protected synchronized void addBattery(BatteryFeneconF2b battery) {
		super.addBattery(battery);
	}

	protected synchronized void removeBattery(BatteryFeneconF2b battery) {
		super.removeBattery(battery);
	}

	protected ChannelManager getChannelManager() {
		return this.channelManager;
	}

	public BatteryFeneconF2bClusterParallelImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				BatteryFeneconF2b.ChannelId.values(), //
				BatteryProtection.ChannelId.values(), //
				BatteryFeneconF2bCluster.ChannelId.values(), //
				BatteryFeneconF2bClusterParallel.ChannelId.values(), //
				StartStoppable.ChannelId.values() //
		);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), this.cm, config.startStop());

		// update filter for 'Battery'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Battery", config.battery_ids())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.channelManager.deactivate();
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
	}

	@Override
	public void startBatteries() throws OpenemsException {
		super.startBatteries();
		final var minVoltage = this.getMinInternalVoltage();
		final var maxVoltage = this.getMaxInternalVoltage();
		// In case of not defined values long time waiting, will lead the battery to the
		// max start time attempt failed
		if (minVoltage.isEmpty() || maxVoltage.isEmpty() || minVoltage.getAsInt() == 0 || maxVoltage.getAsInt() == 0) {
			return;
		}
		// Stop batteries
		this.stopNotStartableBatteries();
		// Start Batteries
		this.startStartableBatteries();
	}

	@Override
	public String debugLog() {
		return Battery.generateDebugLog(this, this.stateMachine);
	}

	@Override
	protected ComponentManager getComponentManager() {
		return this.componentManager;
	}

	@Override
	protected void handleStateMachine() {
		// Store the current State
		var state = this.stateMachine.getCurrentState();
		this._setStateMachine(state);

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		// Prepare Context
		try {
			var context = new Context(this, this.componentManager.getClock(), this.batteries);

			// Call the StateMachine
			this.stateMachine.run(context);

			this._setRunFailed(false);
		} catch (OpenemsNamedException e) {
			this._setRunFailed(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	@Override
	public void setStartStop(StartStop value) {
		this.startStopTarget.set(value);
	}

	@Override
	public void setHvContactorUnlocked(boolean value) {
		this.hvContactorUnlocked = value;
	}

	@Override
	public boolean isHvContactorUnlocked() {
		return this.hvContactorUnlocked;
	}

	@Override
	public BatteryFeneconF2bCluster getBatteryFeneconF2bCluster() {
		return this;
	}

	@Override
	public DeviceSpecificOnSetNextValueHandler<? extends BatteryFeneconF2b> getDeviceSpecificOnSetNextValueHandler() {
		return null;
	}

	@Override
	public Map<Value<Integer>, BatteryFeneconF2b> getInternalVoltageBatteryMap() {
		return super.batteries.stream() //
				.filter(t -> !t.hasFaults())//
				.collect(Collectors.toMap(BatteryFeneconF2b::getInternalVoltage, //
						Function.identity()));
	}

	@Override
	public OptionalInt getMinInternalVoltage() {
		return this.getInternalVoltageBatteryMap().entrySet().stream()//
				.map(Map.Entry::getKey)//
				.filter(Value::isDefined) //
				.mapToInt(Value::get) //
				.min();
	}

	@Override
	public OptionalInt getMaxInternalVoltage() {
		return this.getInternalVoltageBatteryMap().entrySet().stream()//
				.map(Map.Entry::getKey)//
				.filter(Value::isDefined) //
				.mapToInt(Value::get) //
				.max();
	}

	@Override
	public List<BatteryFeneconF2b> getStartableBatteries() {
		var batteryInternalVoltageList = this.getInternalVoltageBatteryMap().entrySet().stream()//
				.filter(map -> map.getKey().isDefined())//
				.toList();
		var batteryList = batteryInternalVoltageList.stream()//
				.map(Map.Entry::getValue)//
				.toList();
		// batteryList.isEmpty() check need it because of 'Undefined' state, it should
		// be checked whether all batteries already started
		if (batteryList.size() <= 1) {
			return batteryList;
		}

		final var minVoltage = this.getMinInternalVoltage();
		final var maxVoltage = this.getMaxInternalVoltage();
		final var voltageDifference = maxVoltage.getAsInt() - minVoltage.getAsInt();
		if (voltageDifference <= ALLOWED_MAX_VOLTAGE_DIFFERENCE) {
			this._setVoltageDifferenceHigh(false);
			return this.batteries;
		}
		this._setVoltageDifferenceHigh(true);
		return batteryInternalVoltageList.stream()
				.filter(map -> map.getKey().get() < this.getMaxInternalVoltage().getAsInt())//
				.map(Map.Entry::getValue)//
				.toList();
	}

	@Override
	public List<BatteryFeneconF2b> getNotStartableBatteries() {
		final var minVoltage = this.getMinInternalVoltage();
		final var maxVoltage = this.getMaxInternalVoltage();
		final var voltageDifference = maxVoltage.getAsInt() - minVoltage.getAsInt();
		if (voltageDifference <= ALLOWED_MAX_VOLTAGE_DIFFERENCE) {
			return emptyList();
		}
		return this.getInternalVoltageBatteryMap().entrySet().stream()//
				.filter(map -> map.getKey().isDefined())//
				.filter(map -> map.getKey().get() >= this.getMaxInternalVoltage().getAsInt())//
				.map(Map.Entry::getValue)//
				.toList();
	}

	@Override
	public void stopNotStartableBatteries() {
		var notStartableBatteries = this.getNotStartableBatteries();
		if (notStartableBatteries.isEmpty()) {
			return;
		}
		notStartableBatteries.forEach(notStartableBattery -> {
			try {
				notStartableBattery.setHvContactorUnlocked(false);
				notStartableBattery.stop();
			} catch (OpenemsNamedException e) {
				this.logError(this.log, "Battery: " + notStartableBattery.id() + " can not stop " + e.getMessage());
			}
		});
	}

	@Override
	public void startStartableBatteries() {
		var startableBatteries = this.getStartableBatteries();
		if (startableBatteries.isEmpty()) {
			return;
		}
		startableBatteries.forEach(startableBattery -> {
			try {
				this.setHvContactorUnlocked(true);
				startableBattery.start();
			} catch (OpenemsNamedException e) {
				this.logError(this.log, "Battery: " + startableBattery.id() + " can not start " + e.getMessage());
			}
		});
	}

	@Override
	public boolean areStartableBatteriesStarted() {
		if (this.getStartableBatteries().isEmpty()) {
			return false;
		}
		return this.getStartableBatteries().stream()//
				.allMatch(Battery::isStarted);
	}

	@Override
	public synchronized boolean areAllBatteriesStarted() {
		return this.areStartableBatteriesStarted();
	}

	@Override
	public synchronized boolean areAllBatteriesStopped() {
		return this.batteries.stream().anyMatch(Battery::isStopped);
	}

	@Override
	public List<BatteryFeneconF2b> getNotStartedBatteries() {
		if (this.batteries.isEmpty()) {
			return emptyList();
		}
		if (this.batteries.stream()//
				.map(BatteryFeneconF2b::getInternalVoltage)//
				.anyMatch(Value::isDefined)) {
			return this.getStartableBatteries().stream()//
					.filter(t -> !t.isStarted())//
					.toList();
		}
		return this.batteries;
	}
}
