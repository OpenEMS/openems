package io.openems.edge.controller.predictiontester;

import java.util.Arrays;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.predictor.api.manager.PredictorManager;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.io.openems.edge.controller.predictiontester", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class PredictionTesterImpl extends AbstractOpenemsComponent
		implements PredictionTester, Controller, OpenemsComponent {

	private Config config = null;

	public PredictionTesterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				PredictionTester.ChannelId.values() //
		);
	}

	@Reference
	private PredictorManager predictorManager;

	private static final ChannelAddress SUM_PRODUCTION = new ChannelAddress("_sum", "ProductionActivePower");
	// private static final ChannelAddress SUM_CONSUMPTION = new
	// ChannelAddress("_sum", "ConsumptionActivePower");

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		Integer[] predictedProduction = predictorManager.get24HoursPrediction(SUM_PRODUCTION).getValues();
		System.out.println(Arrays.toString(predictedProduction));
	}
}
