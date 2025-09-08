package io.openems.edge.evcc.solartariff;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.predictor.api.prediction.AbstractPredictor;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.prediction.Predictor;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Predictor.PV.SolarForecastEvccModel", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)

public class PredictorSolarTariffEvccImpl extends AbstractPredictor implements Predictor, OpenemsComponent {

	private final String[] channelAdresses = { "_sum/ProductionActivePower" };

	@Reference
	private ComponentManager componentManager;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;

	private PredictorSolarTariffEvccApi solarForecastApi;

	private BridgeHttp httpBridge;

	public PredictorSolarTariffEvccImpl() throws OpenemsNamedException {
		super(OpenemsComponent.ChannelId.values(), Controller.ChannelId.values(),
				PredictorSolarTariffEvcc.ChannelId.values());
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws Exception {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.logVerbosity(),
				this.channelAdresses);

		if (!config.enabled()) {
			return;
		}

		this.httpBridge = this.httpBridgeFactory.get();

		// Fetch latest solar energy forecast periodically
		this.solarForecastApi = new PredictorSolarTariffEvccApi(config.url(), this.httpBridge,
				this.componentManager.getClock());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.httpBridgeFactory.unget(this.httpBridge);
	}

	@Override
	protected ClockProvider getClockProvider() {
		return this.componentManager;
	}

	protected Prediction createNewPrediction(ChannelAddress channelAddress) {
		if (this.solarForecastApi != null) {
			Prediction prediction = this.solarForecastApi.getPrediction();
			if (prediction != Prediction.EMPTY_PREDICTION && prediction != null) {
				this.channel(PredictorSolarTariffEvcc.ChannelId.PREDICT_ENABLED).setNextValue(true);
				this.channel(PredictorSolarTariffEvcc.ChannelId.PREDICT)
						.setNextValue(this.solarForecastApi.getCurrentPrediction());
				return prediction;
			}
		}
		return Prediction.EMPTY_PREDICTION;
	}

	@Override
	public String debugLog() {
		if (this.solarForecastApi != null) {
			return "Prediction: " //
					+ this.solarForecastApi.getCurrentPrediction() + " Wh";
		}
		return "Prediction: unavailable";
	}
}
