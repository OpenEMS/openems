package io.openems.edge.ess.cluster;

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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.ess.api.Ess;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.symmetric.api.ManagedSymmetricEss;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.Cluster", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
)
public class EssCluster extends AbstractOpenemsComponent
		implements ManagedSymmetricEss, Ess, OpenemsComponent, EventHandler {

	private Power power = new Power(); // initialize empty power
	
	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.AT_LEAST_ONE)
	private final List<Ess> esss = new CopyOnWriteArrayList<>();

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		// update filter for 'esss' component
		if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "esss", config.ess_ids())) {
			return;
		}

		for (Ess ess : esss) {
			System.out.println(ess.id());
		}
		
		// Add ManagedSymmetricEss to Power object
		int noOfManagedEss = 0;
		for(Ess ess : this.esss) {
			if(ess instanceof ManagedSymmetricEss) {
				noOfManagedEss++;
			}
		}
		ManagedSymmetricEss[] managedEsss = new ManagedSymmetricEss[noOfManagedEss];
		int i =0;
		for(Ess ess : this.esss) {
			if(ess instanceof ManagedSymmetricEss) {
				managedEsss[i++] = (ManagedSymmetricEss) ess;
			}
		}
		this.power = new Power(managedEsss);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getPowerPrecision() {
		// TODO Auto-generated method stub
		return 0;
	}

}
