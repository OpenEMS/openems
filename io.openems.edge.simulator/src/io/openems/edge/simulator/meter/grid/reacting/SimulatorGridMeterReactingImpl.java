package io.openems.edge.simulator.meter.grid.reacting;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

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

import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.GridMeter.Reacting", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=GRID" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class SimulatorGridMeterReactingImpl extends AbstractOpenemsComponent implements SimulatorGridMeterReacting,
		SymmetricMeter, AsymmetricMeter, OpenemsComponent, TimedataProvider, EventHandler {

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	private final CopyOnWriteArraySet<ManagedSymmetricEss> symmetricEsss = new CopyOnWriteArraySet<>();
	private final CopyOnWriteArraySet<SymmetricMeter> symmetricMeters = new CopyOnWriteArraySet<>();

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(enabled=true)")
	private void addEss(ManagedSymmetricEss ess) {
		this.symmetricEsss.add(ess);
		ess.getActivePowerChannel().onSetNextValue(this.updateChannelsCallback);
	}

	protected void removeEss(ManagedSymmetricEss ess) {
		ess.getActivePowerChannel().removeOnSetNextValueCallback(this.updateChannelsCallback);
		this.symmetricEsss.remove(ess);
	}

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(!(service.factoryPid=Simulator.GridMeter.Reacting)))")
	private void addMeter(SymmetricMeter meter) {
		this.symmetricMeters.add(meter);
		meter.getActivePowerChannel().onSetNextValue(this.updateChannelsCallback);
	}

	protected void removeMeter(SymmetricMeter meter) {
		meter.getActivePowerChannel().removeOnSetNextValueCallback(this.updateChannelsCallback);
		this.symmetricMeters.remove(meter);
	}

	public SimulatorGridMeterReactingImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				SimulatorGridMeterReacting.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateEnergy();
			break;
		}
	}

	private final Consumer<Value<Integer>> updateChannelsCallback = value -> {
		Integer sum = null;

		for (ManagedSymmetricEss ess : this.symmetricEsss) {
			if (ess instanceof MetaEss) {
				// ignore this Ess
				continue;
			}
			sum = subtract(sum, ess.getActivePowerChannel().getNextValue().get());
		}
		for (SymmetricMeter sm : this.symmetricMeters) {
			try {
				switch (sm.getMeterType()) {
				case CONSUMPTION_METERED:
				case GRID:
					// ignore
					break;
				case CONSUMPTION_NOT_METERED:
					sum = add(sum, sm.getActivePowerChannel().getNextValue().get());
					break;
				case PRODUCTION:
				case PRODUCTION_AND_CONSUMPTION:
					sum = subtract(sum, sm.getActivePowerChannel().getNextValue().get());
					break;
				}
			} catch (NullPointerException e) {
				// ignore
			}
		}

		this._setActivePower(sum);

		var simulatedActivePowerByThree = TypeUtils.divide(sum, 3);
		this._setActivePowerL1(simulatedActivePowerByThree);
		this._setActivePowerL2(simulatedActivePowerByThree);
		this._setActivePowerL3(simulatedActivePowerByThree);
	};

	private static Integer add(Integer sum, Integer activePower) {
		if (activePower == null && sum == null) {
			return null;
		}
		if (activePower == null) {
			return sum;
		} else if (sum == null) {
			return activePower;
		} else {
			return sum + activePower;
		}
	}

	private static Integer subtract(Integer sum, Integer activePower) {
		if (activePower == null && sum == null) {
			return null;
		}
		if (activePower == null) {
			return sum;
		} else if (sum == null) {
			return activePower * -1;
		} else {
			return sum - activePower;
		}
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.GRID;
	}

	@Override
	public String debugLog() {
		return this.getActivePower().asString();
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// Calculate Energy
		var activePower = this.getActivePower().get();
		if (activePower == null) {
			// Not available
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower > 0) {
			// Buy-From-Grid
			this.calculateProductionEnergy.update(activePower);
			this.calculateConsumptionEnergy.update(0);
		} else {
			// Sell-To-Grid
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(activePower * -1);
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
