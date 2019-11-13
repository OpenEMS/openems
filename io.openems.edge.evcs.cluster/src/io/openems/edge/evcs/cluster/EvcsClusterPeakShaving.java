package io.openems.edge.evcs.cluster;

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
	private Map<String, Evcs> _evcss = new ConcurrentHashMap<>();

	private ConfigPeakShaving config;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected Sum sum;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	protected void addEvcs(Evcs evcs) {
		if (evcs == this) {
			return;
		}
		this.setClusteredState(evcs);
		this._evcss.put(evcs.id(), evcs);
		this.updateSortedEvcss();
	}

	protected void removeEvcs(Evcs evcs) {
		if (evcs == this) {
			return;
		}
		this.resetClusteredState(evcs);
		this._evcss.remove(evcs.id());
		this.updateSortedEvcss();
	}

	public EvcsClusterPeakShaving() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Evcs.ChannelId.values());
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
	 * Fills sortedEvcss using the order of evcs_ids property in the config
	 */
	private synchronized void updateSortedEvcss() {
		this.sortedEvcss.clear();
		for (String id : this.evcsIds) {
			Evcs evcs = this._evcss.get(id);
			if (evcs == null) {
				this.logWarn(this.log, "Required Evcs [" + id + "] is not available.");
			} else {
				this.sortedEvcss.add(evcs);
			}
		}
	}

	/**
	 * Sets the cluster channel to false and resets all depending channels
	 * 
	 * @param evcs
	 */
	private void resetClusteredState(Evcs evcs) {
		if (evcs instanceof ManagedEvcs) {
			((ManagedEvcs) evcs).isClustered().setNextValue(false);
			((ManagedEvcs) evcs).setChargePowerRequest().setNextValue(null);
		}
		evcs.getMaximumPower().setNextValue(null);
	}

	/**
	 * Sets the cluster channel to true
	 * 
	 * @param evcs
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
		try {
			SymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());
			int allowedChargePower = 0;
			int maxEssDischarge = 0;

			if (ess instanceof ManagedSymmetricEss) {
				ManagedSymmetricEss e = (ManagedSymmetricEss) ess;
				Power power = ((ManagedSymmetricEss) ess).getPower();
				maxEssDischarge = power.getMaxPower(e, Phase.ALL, Pwr.ACTIVE);
				maxEssDischarge = Math.abs(maxEssDischarge);
			} else {
				maxEssDischarge = ess.getMaxApparentPower().value().orElse(0);
			}
			int buyFromGrid = this.sum.getGridActivePower().value().orElse(0);
			long essDischargePower = this.sum.getEssActivePower().value().orElse(0);
			int essActivePowerDC = this.sum.getProductionDcActualPower().value().orElse(0);
			int evcsCharge = this.getChargePower().value().orElse(0);

			long maxAvailableStoragePower = maxEssDischarge - (essDischargePower - essActivePowerDC);

			allowedChargePower = (int) (evcsCharge + maxAvailableStoragePower + this.config.hardwarePowerLimit()
					- buyFromGrid);
			allowedChargePower = allowedChargePower > 0 ? allowedChargePower : 0;
			return allowedChargePower;

		} catch (OpenemsNamedException e) {
			e.printStackTrace();
			return 0;
		}
	}

	@Override
	public int getMinimumChargePowerGuarantee() {
		return 4500;
	}
}
