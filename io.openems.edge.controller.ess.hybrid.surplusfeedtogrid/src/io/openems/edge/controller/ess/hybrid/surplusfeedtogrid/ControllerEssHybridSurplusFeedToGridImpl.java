package io.openems.edge.controller.ess.hybrid.surplusfeedtogrid;

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
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.Hybrid.Surplus-Feed-To-Grid", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEssHybridSurplusFeedToGridImpl extends AbstractOpenemsComponent
		implements ControllerEssHybridSurplusFeedToGrid, Controller, OpenemsComponent {

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
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
		var maxDischargePower = managedEss.getPower().getMaxPower(managedEss, Phase.ALL, Pwr.ACTIVE);

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
