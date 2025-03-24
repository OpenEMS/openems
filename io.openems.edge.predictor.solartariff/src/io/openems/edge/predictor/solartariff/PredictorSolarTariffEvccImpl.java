package io.openems.edge.predictor.solartariff;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
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
public class PredictorSolarTariffEvccImpl extends AbstractPredictor
		implements
			Predictor,
			OpenemsComponent {

	private final Logger log = LoggerFactory
			.getLogger(PredictorSolarTariffEvccImpl.class);

	private boolean executed = false;
	private LocalDateTime prevHour = LocalDateTime.now();
	private final TreeMap<LocalDateTime, Integer> hourlySolarData = new TreeMap<>();
	Prediction cachedPrediction;

	@Reference
	private ComponentManager componentManager;

	private Config config;

	private PredictorSolarTariffEvccAPI solarForecastAPI; // Service to fetch
															// weather data

	public PredictorSolarTariffEvccImpl() throws OpenemsNamedException {
		super(OpenemsComponent.ChannelId.values(),
				Controller.ChannelId.values(),
				PredictorSolarTariffEvcc.ChannelId.values());
	}

	@Activate
	private void activate(ComponentContext context, Config config)
			throws Exception {
		this.config = config;
		super.activate(context, this.config.id(), this.config.alias(),
				this.config.enabled(), this.config.channelAddresses(),
				this.config.logVerbosity());

		// Fetch latest weather forecast data every 15 minutes
		this.solarForecastAPI = new PredictorSolarTariffEvccAPI(config.url()); // initialize
																				// here
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ClockProvider getClockProvider() {
		return this.componentManager;
	}

	protected Prediction createNewPrediction(ChannelAddress channelAddress) {
		try {
			System.out.println("SolarForecast.createNewPrediction is called.");
			LocalDateTime currentHour = LocalDateTime
					.now(this.componentManager.getClock()).withNano(0)
					.withMinute(0).withSecond(0);
			ZoneId zoneId = ZoneId.of("UTC");

			JsonArray js = null;

			if (!executed) {
				js = this.solarForecastAPI.getSolarForecast();
				this.prevHour = currentHour;
				this.executed = true;
			} else if (currentHour.isAfter(this.prevHour)) {
				js = this.solarForecastAPI.getSolarForecast();
				this.prevHour = currentHour;
			} else {
				// TODO correct?
				if (cachedPrediction != null) {
					log.debug("SolarForecast.createNewPrediction cached");
					return cachedPrediction;
				}
			}

			log.debug("SolarForecast.createNewPrediction refreshing");

			if (js != null) {
				hourlySolarData.clear();
				for (int i = 0; i < js.size(); i++) {
					JsonElement timeElement = js.get(i).getAsJsonObject()
							.get("start");
					OffsetDateTime utcTime = OffsetDateTime
							.parse(timeElement.getAsString()); // UTC Zeit
																// parsen
					LocalDateTime localTime = utcTime.toLocalDateTime(); // Zeit
																			// in
																			// LocalDateTime
																			// konvertieren

					JsonElement price = js.get(i).getAsJsonObject()
							.get("price");

					log.debug("SolarForecast prediction: " + localTime + " "
							+ price.getAsInt() + " Wh");
					hourlySolarData.put(localTime, price.getAsInt());
				}
				this.channel(PredictorSolarTariffEvcc.ChannelId.PREDICT_ENABLED)
						.setNextValue(true);

				this.channel(PredictorSolarTariffEvcc.ChannelId.PREDICT)
						.setNextValue(hourlySolarData.firstEntry().getValue());

			} else {
				log.warn("Failed to fetch solar forecast data.");
				this.channel(PredictorSolarTariffEvcc.ChannelId.PREDICT_ENABLED)
						.setNextValue(false);
				log.debug("SolarForecast.createNewPrediction failed.");
				return Prediction.EMPTY_PREDICTION;
			}

			log.debug("SolarForecast.createNewPrediction transforming");

			// Create an array to store the forecast values for the next 192
			// intervals (48 hours in 15-minute steps)
			var values = new Integer[192];

			int i = 0;
			for (Entry<LocalDateTime, Integer> entry : hourlySolarData
					.entrySet()) {
				// System.out.println("loop processing...");
				if (!entry.getKey().atZone(zoneId)
						.isBefore(currentHour.atZone(zoneId))
						&& i < values.length) {
					// convert hourly values in 15min steps (summarized later,
					// therefore divided by 4 //TODO correct?
					// limited for next 24h from current hour
					values[i++] = entry.getValue();
					values[i++] = entry.getValue();
					values[i++] = entry.getValue();
					values[i++] = entry.getValue();
				}
			}
			log.debug("loop completed");

			this.channel(PredictorSolarTariffEvcc.ChannelId.PREDICT)
					.setNextValue(hourlySolarData.firstEntry().getValue());

			Prediction prediction = Prediction.from(currentHour.atZone(zoneId),
					values);

			cachedPrediction = prediction;

			// Return the prediction starting from the calculated time
			return prediction;

		} catch (Exception e) {
			log.error("Error creating prediction: ", e);
			return Prediction.EMPTY_PREDICTION;
		}
	}

	@Override
	public String debugLog() {
		return "Prediction: "
				+ this.channel(PredictorSolarTariffEvcc.ChannelId.PREDICT)
						.value().toString();
	}
}