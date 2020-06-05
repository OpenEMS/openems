package io.openems.edge.evcs.cluster;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

@Designate(ocd = ConfigPeakShaving.class, factory = true)
@Component(//
		name = "Evcs.Cluster.PeakShaving", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		})
public class EvcsClusterPeakShaving extends AbstractEvcsCluster implements OpenemsComponent, Evcs, EventHandler {

	private final Logger log = LoggerFactory.getLogger(EvcsClusterPeakShaving.class);

	// Used EVCSs
	private String[] evcsIds = new String[0];
	private final List<Evcs> sortedEvcss = new ArrayList<>();
	private Map<String, Evcs> evcss = new ConcurrentHashMap<>();

	private ConfigPeakShaving config;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected Sum sum;

	@Reference
	private SymmetricEss ess;

	public EvcsClusterPeakShaving() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Evcs.ChannelId.values());
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	protected void addEvcs(Evcs evcs) {
		if (evcs == this) {
			return;
		}
		this.setClusteredState(evcs);
		this.evcss.put(evcs.id(), evcs);
		this.updateSortedEvcss();
	}

	protected void removeEvcs(Evcs evcs) {
		if (evcs == this) {
			return;
		}
		this.resetClusteredState(evcs);
		this.evcss.remove(evcs.id());
		this.updateSortedEvcss();
	}

	@Activate
	void activate(ComponentContext context, ConfigPeakShaving config) throws OpenemsNamedException {
		this.evcsIds = config.evcs_ids();
		updateSortedEvcss();
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.config = config;

		// update filter for 'evcss' component
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Evcs", config.evcs_ids())) {
			return;
		}
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		for (Evcs evcs : this.sortedEvcss) {
			this.resetClusteredState(evcs);
			evcs.getMaximumPower().setNextValue(null);
		}
		super.deactivate();
	}

	/**
	 * Fills sortedEvcss using the order of evcs_ids property in the configuration.
	 */
	private synchronized void updateSortedEvcss() {
		this.sortedEvcss.clear();
		for (String id : this.evcsIds) {
			Evcs evcs = this.evcss.get(id);
			if (evcs == null) {
				this.logWarn(this.log, "Required Evcs [" + id + "] is not available.");
			} else {
				this.sortedEvcss.add(evcs);
			}
		}
	}

	/**
	 * Sets the cluster channel to false and resets all depending channels.
	 * 
	 * @param evcs Electric Vehicle Charging Station
	 */
	private void resetClusteredState(Evcs evcs) {
		if (evcs instanceof ManagedEvcs) {
			((ManagedEvcs) evcs).isClustered().setNextValue(false);
			((ManagedEvcs) evcs).setChargePowerRequest().setNextValue(null);
		}
		evcs.getMaximumPower().setNextValue(null);
	}

	/**
	 * Sets the cluster channel to true.
	 * 
	 * @param evcs Electric Vehicle Charging Station
	 */
	private void setClusteredState(Evcs evcs) {
		if (evcs instanceof ManagedEvcs) {
			((ManagedEvcs) evcs).isClustered().setNextValue(true);
		}
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
	}

	@Override
	public List<Evcs> getSortedEvcss() {
		return this.sortedEvcss;
	}

	@Override
	public int getMaximumPowerToDistribute() {
		int allowedChargePower = 0;
		int maxEssDischarge = 0;

		if (this.ess instanceof ManagedSymmetricEss) {
			ManagedSymmetricEss e = (ManagedSymmetricEss) ess;
			Power power = ((ManagedSymmetricEss) ess).getPower();
			maxEssDischarge = Math.abs(power.getMaxPower(e, Phase.ALL, Pwr.ACTIVE));
		} else {
			maxEssDischarge = ess.getMaxApparentPower().value().orElse(0);
		}

		int gridPower = getGridPower();
		long essDischargePower = this.sum.getEssActivePower().value().orElse(0);

		// TODO: Calculate the available ESS charge power, depending on a specific ESS
		// component (e.g. If there is a ESS cluster)
		int essActivePowerDC = this.sum.getProductionDcActualPower().value().orElse(0);
		int evcsCharge = this.getChargePower().value().orElse(0);

		long maxAvailableStoragePower = maxEssDischarge - (essDischargePower - essActivePowerDC);

		allowedChargePower = (int) (evcsCharge + maxAvailableStoragePower + (this.config.hardwarePowerLimit() * 3)
				- gridPower);

		this.logInfoInDebugmode(log,
				"Calculation of the maximum charge Power: EVCS Charge [" + evcsCharge
						+ "]  +  Max. available storage power [" + maxAvailableStoragePower
						+ "]  +  ( Configured Hardware Limit * 3 [" + this.config.hardwarePowerLimit() * 3
						+ "]  -  Maxium of all three phases * 3 [" + gridPower + "]");

		allowedChargePower = allowedChargePower > 0 ? allowedChargePower : 0;
		return allowedChargePower;

	}

	private int getGridPower() {
		SymmetricMeter meter;
		try {
			meter = this.componentManager.getComponent(this.config.meter_id());

			int gridPower = meter.getActivePower().value().orElse(0);

			if (meter instanceof AsymmetricMeter) {
				AsymmetricMeter asymmetricMeter = (AsymmetricMeter) meter;

				int gridPowerL1 = asymmetricMeter.getActivePowerL1().value().orElse(0);
				int gridPowerL2 = asymmetricMeter.getActivePowerL2().value().orElse(0);
				int gridPowerL3 = asymmetricMeter.getActivePowerL3().value().orElse(0);

				int maxPowerOnPhase = Math.max(Math.max(gridPowerL1, gridPowerL2), gridPowerL3);
				gridPower = maxPowerOnPhase * 3;
			}

			return gridPower;

		} catch (OpenemsNamedException e) {
			this.logWarn(log, "The component " + this.config.meter_id()
					+ " is not configured. Maximum power for all EVCS will be calculated without grid power.");
			return 0;
		}
	}

	@Override
	public int getMinimumChargePowerGuarantee() {
		return 4500;
	}

	@Override
	public boolean debugMode() {
		return this.config.debugMode();
	}
}
