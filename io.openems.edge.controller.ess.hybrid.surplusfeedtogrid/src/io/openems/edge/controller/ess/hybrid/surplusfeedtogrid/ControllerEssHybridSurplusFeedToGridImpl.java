package io.openems.edge.controller.ess.hybrid.surplusfeedtogrid;

import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.ess.power.api.Pwr.ACTIVE;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.Hybrid.Surplus-Feed-To-Grid", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
public class ControllerEssHybridSurplusFeedToGridImpl extends AbstractOpenemsComponent
		implements ControllerEssHybridSurplusFeedToGrid, Controller, OpenemsComponent {

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	private HybridEss ess;

	public ControllerEssHybridSurplusFeedToGridImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssHybridSurplusFeedToGrid.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		// update filter for 'ess'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		var surplusPower = this.ess.getSurplusPower();

		// No surplus power by Ess? -> stop
		if (surplusPower == null) {
			this._setSurplusFeedToGridIsLimited(false);
			return;
		}

		var managedEss = (ManagedSymmetricEss) this.ess;

		// Get maximum possible surplus feed-in power
		var maxDischargePower = managedEss.getPower().getMaxPower(managedEss, ALL, ACTIVE);

		// Is surplus power limited by a higher priority Controller? -> set info state
		final int minDischargePower;
		if (maxDischargePower > surplusPower) {
			this._setSurplusFeedToGridIsLimited(false);
			minDischargePower = surplusPower;
		} else {
			this._setSurplusFeedToGridIsLimited(true);
			minDischargePower = maxDischargePower;
		}

		// Set surplus feed-in power set-point
		managedEss.setActivePowerGreaterOrEquals(minDischargePower);
	}
}
