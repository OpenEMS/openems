package io.openems.edge.controller.ess.predictivedelaycharge.dc;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.predictivedelaycharge.AbstractPredictiveDelayCharge;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.predictor.api.hourly.ConsumptionHourlyPredictor;
import io.openems.edge.predictor.api.hourly.ProductionHourlyPredictor;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.DcPredictiveDelayCharge", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class DcPredictiveDelayCharge extends AbstractPredictiveDelayCharge implements Controller, OpenemsComponent {

	private Config config;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ProductionHourlyPredictor productionHourlyPredictor;

	@Reference
	protected ConsumptionHourlyPredictor consumptionHourlyPredictor;

	public DcPredictiveDelayCharge() {
		super();
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.meter_id(),
				config.noOfBufferHours(), config.debugMode());
		this.config = config;
	}

//
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		// Get required variables
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());
		Integer calculatedPower = super.getCalculatedPower(ess, productionHourlyPredictor, consumptionHourlyPredictor,
				componentManager);

		// checking if power per second is calculated
		if (calculatedPower != null) {
			int productionPower = 0;
			for (String chargerId : this.config.charger_ids()) {
				EssDcCharger charger = this.componentManager.getComponent(chargerId);
				productionPower += charger.getActualPower().orElse(0);
			}
			calculatedPower = productionPower - calculatedPower;

			// avoiding buying power from grid to charge the battery.
			if (calculatedPower > 0) {
				// set result
				ess.addPowerConstraintAndValidate("DcPredictiveDelayCharge", Phase.ALL, Pwr.ACTIVE,
						Relationship.GREATER_OR_EQUALS, calculatedPower);
			}
		}
	}

	@Override
	protected ComponentManager getComponentManager() {
		return this.componentManager;
	}

}
