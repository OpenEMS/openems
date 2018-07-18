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
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.Cluster", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
)
public class EssCluster extends AbstractOpenemsComponent
		implements ManagedAsymmetricEss, AsymmetricEss, ManagedSymmetricEss, SymmetricEss, MetaEss, OpenemsComponent {

	private final AverageInteger<SymmetricEss> soc;
	private final SumInteger<SymmetricEss> activePower;
	private final SumInteger<SymmetricEss> reactivePower;
	private final SumInteger<AsymmetricEss> activePowerL1;
	private final SumInteger<AsymmetricEss> reactivePowerL1;
	private final SumInteger<AsymmetricEss> activePowerL2;
	private final SumInteger<AsymmetricEss> reactivePowerL2;
	private final SumInteger<AsymmetricEss> activePowerL3;
	private final SumInteger<AsymmetricEss> reactivePowerL3;
	private final SumInteger<SymmetricEss> maxActivePower;
	private final SumInteger<SymmetricEss> activeChargeEnergy;
	private final SumInteger<SymmetricEss> activeDischargeEnergy;

	@Reference
	private Power power = null;

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
		this.reactivePower = new SumInteger<SymmetricEss>(this, SymmetricEss.ChannelId.REACTIVE_POWER,
				SymmetricEss.ChannelId.REACTIVE_POWER);
		this.activePowerL1 = new SumInteger<AsymmetricEss>(this, AsymmetricEss.ChannelId.ACTIVE_POWER_L1,
				AsymmetricEss.ChannelId.ACTIVE_POWER_L1);
		this.reactivePowerL1 = new SumInteger<AsymmetricEss>(this, AsymmetricEss.ChannelId.REACTIVE_POWER_L1,
				AsymmetricEss.ChannelId.REACTIVE_POWER_L1);
		this.activePowerL2 = new SumInteger<AsymmetricEss>(this, AsymmetricEss.ChannelId.ACTIVE_POWER_L2,
				AsymmetricEss.ChannelId.ACTIVE_POWER_L2);
		this.reactivePowerL2 = new SumInteger<AsymmetricEss>(this, AsymmetricEss.ChannelId.REACTIVE_POWER_L2,
				AsymmetricEss.ChannelId.REACTIVE_POWER_L2);
		this.activePowerL3 = new SumInteger<AsymmetricEss>(this, AsymmetricEss.ChannelId.ACTIVE_POWER_L3,
				AsymmetricEss.ChannelId.ACTIVE_POWER_L3);
		this.reactivePowerL3 = new SumInteger<AsymmetricEss>(this, AsymmetricEss.ChannelId.REACTIVE_POWER_L3,
				AsymmetricEss.ChannelId.REACTIVE_POWER_L3);
		this.maxActivePower = new SumInteger<SymmetricEss>(this, SymmetricEss.ChannelId.MAX_ACTIVE_POWER,
				SymmetricEss.ChannelId.MAX_ACTIVE_POWER);
		this.activeChargeEnergy = new SumInteger<SymmetricEss>(this, SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY,
				SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);
		this.activeDischargeEnergy = new SumInteger<SymmetricEss>(this, SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY,
				SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY);
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
			this.managedEsss[i] = managedSymmetricEsssList.get(i);
		}

		/*
		 * Initialize Channel-Mergers
		 */
		for (SymmetricEss ess : this.esss) {
			this.soc.addComponent(ess);
			this.activePower.addComponent(ess);
			this.reactivePower.addComponent(ess);
			this.maxActivePower.addComponent(ess);
			this.activeChargeEnergy.addComponent(ess);
			this.activeDischargeEnergy.addComponent(ess);
			if (ess instanceof AsymmetricEss) {
				AsymmetricEss e = (AsymmetricEss) ess;
				this.activePowerL1.addComponent(e);
				this.reactivePowerL1.addComponent(e);
				this.activePowerL2.addComponent(e);
				this.reactivePowerL2.addComponent(e);
				this.activePowerL3.addComponent(e);
				this.reactivePowerL3.addComponent(e);
			}
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
			this.reactivePower.removeComponent(ess);
			this.maxActivePower.removeComponent(ess);
			this.activeChargeEnergy.removeComponent(ess);
			this.activeDischargeEnergy.removeComponent(ess);
			if (ess instanceof AsymmetricEss) {
				AsymmetricEss e = (AsymmetricEss) ess;
				this.activePowerL1.removeComponent(e);
				this.reactivePowerL1.removeComponent(e);
				this.activePowerL2.removeComponent(e);
				this.reactivePowerL2.removeComponent(e);
				this.activePowerL3.removeComponent(e);
				this.reactivePowerL3.removeComponent(e);
			}
		}
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

	@Override
	public Power getPower() {
		return this.power;
	}
}
