package io.openems.edge.batteryinverter.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.type.TypeUtils;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "BatteryInverter.cluster", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
	EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class BatteryInverterClusterImpl extends AbstractOpenemsComponent implements BatteryInverterCluster, SymmetricBatteryInverter, ManagedSymmetricBatteryInverter, 
											OpenemsComponent, StartStoppable, EventHandler {

	private final Logger log = LoggerFactory.getLogger(BatteryInverterCluster.class);
	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<>(StartStop.UNDEFINED);
	private final ChannelManager channelManager = new ChannelManager(this);
	private final List<SymmetricBatteryInverter> inverters = new CopyOnWriteArrayList<>();

	@Reference
	private ConfigurationAdmin cm;

	@Reference 
	protected ComponentManager componentManager;

	private Config config;


	@Reference(//
			policy = ReferencePolicy.DYNAMIC,//
			policyOption = ReferencePolicyOption.GREEDY,//
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(!(service.factoryPid=BatteryInverter.Cluster)))")

	protected synchronized void addBatteryInverter(SymmetricBatteryInverter inverter) {
		this.inverters.add(inverter);
		this.channelManager.deactivate();
		this.channelManager.activate(this.inverters);
	}

	protected synchronized void removeBatteryInverter(SymmetricBatteryInverter inverter) {
		this.inverters.remove(inverter);
		this.channelManager.deactivate();
		this.channelManager.activate(this.inverters);
	}

	public BatteryInverterClusterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricBatteryInverter.ChannelId.values(), //
				ManagedSymmetricBatteryInverter.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				BatteryInverterCluster.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());

		// TODO: Update filter references for inverters
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "BatteryInverter", config.batteryInverterIds())) {
			return;
		}

		this.channelManager.activate(this.inverters);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.channelManager.deactivate();
		super.deactivate();
	}

	@Override
	public String debugLog() {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < this.inverters.size(); i++) {
			sb.append(inverters.get(i).debugLog());

			if (i < inverters.size() - 1) {
                sb.append("|");
            }
		}

		return sb.toString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricBatteryInverter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricBatteryInverter.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(BatteryInverterClusterImpl.class, accessMode, 300).build());
	}

	@Override
	public void run(Battery battery, int setActivePower, int setReactivePower) throws OpenemsNamedException {
		// Basic strategy is to divide the active and reactive power amongst the inverter in proportion to the maxApparentPower available for each one

		// Filter and cast to make a list of inverters which are instances of ManagedSymmetricBatteryInverter and isManaged==true
		List<ManagedSymmetricBatteryInverter> managedInverters = this.inverters.stream()
				.filter(ManagedSymmetricBatteryInverter.class::isInstance)
				.map(ManagedSymmetricBatteryInverter.class::cast)
				.filter(ManagedSymmetricBatteryInverter::isManaged)
				.collect(Collectors.toList());

		Integer totalMaxApparentPower = inverters.stream()
				.mapToInt(inverter -> inverter.getMaxApparentPower().get())
				.sum();


		Integer totalActivePowerSoFar = 0;
		Integer totalReactivePowerSoFar = 0;

		for (int i = 0; i < managedInverters.size(); i++) {

			ManagedSymmetricBatteryInverter inverter = managedInverters.get(i);
			Integer inverterMaxPower = inverter.getMaxApparentPower().get();
			
			Boolean isLast = (i == (managedInverters.size() -1 )); // Is this the last inverter in the list
			if (!isLast) {

				Double weight = (double) inverter.getMaxApparentPower().get() / totalMaxApparentPower;
				
				

				Integer activePower = Math.min((int) (setActivePower * weight), inverterMaxPower); // This truncates the decimal power (rounds down)
				Integer reactivePower = Math.min((int) (setReactivePower * weight), inverterMaxPower); // This truncates the decimal power (rounds down)

				inverter.run(battery, activePower, reactivePower);

				totalActivePowerSoFar += activePower;
				totalReactivePowerSoFar += reactivePower;
			} else { // If this is the last inverter, assign all remaining power to it.

				Integer activePower = Math.min(setActivePower - totalActivePowerSoFar, inverterMaxPower);
				Integer reactivePower = Math.min(setReactivePower - totalReactivePowerSoFar, inverterMaxPower);

				inverter.run(battery, activePower, reactivePower);

			}
		}	
	}

	@Override
	public int getPowerPrecision() {
		Integer result = null;
		for (SymmetricBatteryInverter inverter : this.inverters) {
			if (inverter instanceof ManagedSymmetricBatteryInverter) {
				result = TypeUtils.min(result, ((ManagedSymmetricBatteryInverter) inverter).getPowerPrecision());
			}
		}
		return TypeUtils.orElse(result, 1);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.handleStartStop();
			break;
		}
	}

	/**
	 * Handles the Start/Stop target from {@link Config} or set via
	 * {@link #setStartStop(StartStop)}.
	 */
	private void handleStartStop() {
		StartStop target = StartStop.UNDEFINED;

		switch (this.config.startStop()) {
		case AUTO: {
			target = this.startStopTarget.get();
			break;
		}
		case START: {
			target = StartStop.START;
			break;
		}
		case STOP: {
			target = StartStop.STOP;
			break;
		}
		}

		if (target == StartStop.UNDEFINED) {
			this.logInfo(this.log, "Start-Stop-Target is Undefined");
			return;
		}

		for (SymmetricBatteryInverter inverter : this.inverters) {
			if (inverter instanceof StartStoppable) {
				try {
					((StartStoppable) inverter).setStartStop(target);
				} catch (OpenemsNamedException e) {
					this.logError(this.log, e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		this.startStopTarget.set(value);
	}


}