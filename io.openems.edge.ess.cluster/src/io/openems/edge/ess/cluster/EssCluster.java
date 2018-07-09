package io.openems.edge.ess.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.merger.AverageInteger;
import io.openems.edge.common.channel.merger.SumInteger;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.Cluster", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
)
public class EssCluster extends AbstractOpenemsComponent
		implements ManagedAsymmetricEss, AsymmetricEss, ManagedSymmetricEss, MetaEss, OpenemsComponent {

	private Power power = new Power(); // initialize empty power

	private final AverageInteger<SymmetricEss> soc;
	private final SumInteger<SymmetricEss> activePower;
	private final SumInteger<SymmetricEss> maxActivePower;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	private List<SymmetricEss> esss = new CopyOnWriteArrayList<>();

	private ManagedSymmetricEss[] managedEsss = new ManagedSymmetricEss[0];

	public EssCluster() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
		/*
		 * Define Channel Mergers
		 */
		this.soc = new AverageInteger<SymmetricEss>(this, SymmetricEss.ChannelId.SOC, SymmetricEss.ChannelId.SOC);
		this.activePower = new SumInteger<SymmetricEss>(this, SymmetricEss.ChannelId.ACTIVE_POWER,
				SymmetricEss.ChannelId.ACTIVE_POWER);
		this.maxActivePower = new SumInteger<SymmetricEss>(this, SymmetricEss.ChannelId.MAX_ACTIVE_POWER,
				SymmetricEss.ChannelId.MAX_ACTIVE_POWER);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		// update filter for 'esss' component
		if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "esss", config.ess_ids())) {
			return;
		}

		/*
		 * Add all ManagedSymmetricEss devices to Power object
		 */
		List<ManagedSymmetricEss> managedSymmetricEsssList = new ArrayList<>();
		for (SymmetricEss ess : this.esss) {
			if (ess instanceof ManagedSymmetricEss) {
				managedSymmetricEsssList.add((ManagedSymmetricEss) ess);
			}
		}
		this.managedEsss = new ManagedSymmetricEss[managedSymmetricEsssList.size()];
		for (int i = 0; i < managedSymmetricEsssList.size(); i++) {
			managedEsss[i] = managedSymmetricEsssList.get(i);
		}
		this.power = new Power(managedEsss);

		/*
		 * Initialize Channel-Mergers
		 */
		for (SymmetricEss ess : this.esss) {
			this.soc.addComponent(ess);
			this.activePower.addComponent(ess);
			this.maxActivePower.addComponent(ess);
		}

		super.activate(context, config.service_pid(), config.id(), config.enabled());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		/*
		 * Clear Channel-Mergers
		 */
		for (SymmetricEss ess : this.esss) {
			this.soc.removeComponent(ess);
			this.activePower.removeComponent(ess);
			this.maxActivePower.removeComponent(ess);
		}
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
		throw new IllegalArgumentException("EssClusterImpl.applyPower() should never be called.");
	}

	@Override
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) {
		throw new IllegalArgumentException("EssClusterImpl.applyPower() should never be called.");
	}

	@Override
	public int getPowerPrecision() {
		// TODO kleinstes gemeinsames Vielfaches von PowerPrecision
		return 0;
	}

	@Override
	public synchronized ManagedSymmetricEss[] getEsss() {
		return this.managedEsss;
	}

}
