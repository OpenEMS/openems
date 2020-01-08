package io.openems.edge.evcs.cluster;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
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
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Designate(ocd = ConfigSelfConsumption.class, factory = true)
@Component(//
		name = "Evcs.Cluster.SelfConsumtion", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		})
public class EvcsClusterSelfConsumption extends AbstractEvcsCluster implements OpenemsComponent, Evcs {

	private final Logger log = LoggerFactory.getLogger(EvcsClusterSelfConsumption.class);

	// Used EVCSs
	private String[] evcsIds = new String[0];
	private final List<Evcs> sortedEvcss = new ArrayList<>();
	private Map<String, Evcs> evcss = new ConcurrentHashMap<>();

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected Sum sum;

	/**
	 * Constructor.
	 */
	public EvcsClusterSelfConsumption() {
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
	void activate(ComponentContext context, ConfigSelfConsumption configSelfConsumption) throws OpenemsNamedException {
		this.evcsIds = configSelfConsumption.evcs_ids();
		updateSortedEvcss();
		super.activate(context, configSelfConsumption.id(), configSelfConsumption.alias(),
				configSelfConsumption.enabled());

		// update filter for 'evcs' component
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Evcs",
				configSelfConsumption.evcs_ids())) {
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
		int excessPower = 0;

		int buyFromGrid = this.sum.getGridActivePower().value().orElse(0);
		int essDischarge = this.sum.getEssActivePower().value().orElse(0);
		int essActivePowerDC = this.sum.getProductionDcActualPower().value().orElse(0);
		int evcsCharge = this.getChargePower().getNextValue().orElse(0);

		excessPower = evcsCharge - buyFromGrid - (essDischarge - essActivePowerDC);

		return excessPower;
	}

	@Override
	public int getMinimumChargePowerGuarantee() {
		return 0;
	}
}
