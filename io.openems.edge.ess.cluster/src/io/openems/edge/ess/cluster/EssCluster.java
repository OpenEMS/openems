package io.openems.edge.ess.cluster;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.Relationship;
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
import io.openems.edge.ess.api.Ess;
import io.openems.edge.ess.power.api.ConstraintType;
import io.openems.edge.ess.power.api.LinearConstraintWrapper;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.symmetric.api.ManagedSymmetricEss;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.Cluster", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
)
public class EssCluster extends AbstractOpenemsComponent implements ManagedSymmetricEss, Ess, OpenemsComponent {

	private Power power = new Power(); // initialize empty power

	private final AverageInteger<Ess> soc;
	private final SumInteger<Ess> activePower;
	private final SumInteger<Ess> maxActivePower;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	private List<Ess> esss = new CopyOnWriteArrayList<>();

	public EssCluster() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
		/*
		 * Define Channel Mergers
		 */
		this.soc = new AverageInteger<Ess>(this, Ess.ChannelId.SOC, Ess.ChannelId.SOC);
		this.activePower = new SumInteger<Ess>(this, Ess.ChannelId.ACTIVE_POWER, Ess.ChannelId.ACTIVE_POWER);
		this.maxActivePower = new SumInteger<Ess>(this, Ess.ChannelId.MAX_ACTIVE_POWER, Ess.ChannelId.MAX_ACTIVE_POWER);
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
		int noOfManagedEss = 0;
		for (Ess ess : this.esss) {
			if (ess instanceof ManagedSymmetricEss) {
				noOfManagedEss++;
				// Disable individual Power objects
				((ManagedSymmetricEss) ess).getPower().setDisabled();
			}
		}
		ManagedSymmetricEss[] managedEsss = new ManagedSymmetricEss[noOfManagedEss];
		int i = 0;
		for (Ess ess : this.esss) {
			if (ess instanceof ManagedSymmetricEss) {
				managedEsss[i++] = (ManagedSymmetricEss) ess;
			}
		}
		this.power = new Power(managedEsss);

		/*
		 * ManagedSymmetricEss: Add Constraints to keep Ess1 L1 == Ess2 L1 == Ess3 L1
		 * and Ess1 L2 == Ess2 L2 == Ess3 L2,...
		 */
		// FIXME: should distribute power smarter.
		// of Ess!
		for (Phase phase : Phase.values()) {
			for (i = 1; i < managedEsss.length; i++) {
				double[] coefficients = new double[this.power.getNoOfCoefficients()];
				int firstIndex = (i - 1) * 6 + phase.getOffset();
				int secondIndex = i * 6 + phase.getOffset();
				coefficients[firstIndex] = 1;
				coefficients[secondIndex] = -1;
				this.power.addConstraint(ConstraintType.STATIC, new LinearConstraintWrapper(
						new LinearConstraint(coefficients, Relationship.EQ, 0), "All Ess equal"));
			}
		}

		/*
		 * Initialize Channel-Mergers
		 */
		for (Ess ess : this.esss) {
			this.soc.addComponent(ess);
			this.activePower.addComponent(ess);
			this.maxActivePower.addComponent(ess);
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		/*
		 * Clear Channel-Mergers
		 */
		for (Ess ess : this.esss) {
			this.soc.removeComponent(ess);
			this.activePower.removeComponent(ess);
			this.maxActivePower.removeComponent(ess);
		}
	}

	@Override
	public boolean addToSum() {
		return false;
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
		// ignore: this will never be called. 'Power' calls the callbacks of the child
		// Ess devices.
	}

	@Override
	public int getPowerPrecision() {
		// TODO kleinstes gemeinsames Vielfaches von PowerPrecision
		return 0;
	}

}
