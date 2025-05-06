package io.openems.edge.evcc.api.solartariff;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
import com.google.gson.JsonObject;

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
	private long duration = -1;
	private ZonedDateTime prevHour = LocalDateTime.now()
			.atZone(ZoneId.of("UTC"));
	private final TreeMap<ZonedDateTime, Integer> hourlySolarData = new TreeMap<>();

	@Reference
	private ComponentManager componentManager;

	private Config config;

	private PredictorSolarTariffEvccApi solarForecastApi; // fetch weather data

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
		this.solarForecastApi = new PredictorSolarTariffEvccApi(
				this.config.url()); // initialize
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
			this.log.info("SolarForecast.createNewPrediction is called.");
			LocalDateTime localCurrentHour = LocalDateTime
					.now(this.componentManager.getClock()).withNano(0)
					.withMinute(0).withSecond(0);
			ZoneId zoneId = ZoneId.of("UTC");
			ZonedDateTime currentHour = localCurrentHour.atZone(zoneId);

			JsonArray js = null;

			if (!this.executed) {
				js = this.solarForecastApi.getSolarForecast();
				this.prevHour = currentHour;
				this.executed = true;
			} else if (this.prevHour != null
					&& currentHour.isAfter(this.prevHour)) {
				this.duration = -1;
				js = this.solarForecastApi.getSolarForecast();
				this.prevHour = currentHour;
			} else {
				// TODO something to do here?
			}

			this.log.debug("SolarForecast.createNewPrediction calculation");

			if (js != null) {
				this.hourlySolarData.clear();

				for (int i = 0; i < js.size(); i++) {
					JsonObject data = js.get(i).getAsJsonObject();
					JsonElement startsAt = data.get("start");

					// Parse den String in ZonedDateTime
					ZonedDateTime zonedDateTime = ZonedDateTime
							.parse(startsAt.getAsString());

					// execute once to determine intervals
					if (this.duration < 0) {
						JsonElement endsAt = data.get("end");

						// Parse den String in ZonedDateTime
						ZonedDateTime zonedDateTimeEnd = ZonedDateTime
								.parse(endsAt.getAsString());

						this.duration = Duration
								.between(zonedDateTime, zonedDateTimeEnd)
								.toMinutes();
					}

					// Konvertiere nach UTC
					ZonedDateTime utcDateTime = zonedDateTime
							.withZoneSameInstant(ZoneId.of("UTC"));

					JsonObject jsonObject = js.get(i).getAsJsonObject();

					Integer power = jsonObject.has("value")
							? jsonObject.get("value").getAsInt()
							: (jsonObject.has("price")
									? jsonObject.get("price").getAsInt()
									: null);

					this.log.debug("SolarForecast prediction: " + utcDateTime
							+ " " + power + " Wh");

					this.hourlySolarData.put(utcDateTime, power);

				}
				this.channel(PredictorSolarTariffEvcc.ChannelId.PREDICT_ENABLED)
						.setNextValue(true);

				this.channel(PredictorSolarTariffEvcc.ChannelId.PREDICT)
						.setNextValue(
								this.hourlySolarData.firstEntry().getValue());

			}

			if (this.hourlySolarData != null
					&& !this.hourlySolarData.isEmpty()) {
				this.log.debug(
						"SolarForecast.createNewPrediction transforming");

				// Create an array to store the forecast values for the next 192
				// intervals (48 hours in 15-minute steps)
				var values = new Integer[192];

				int i = 0;
				for (Entry<ZonedDateTime, Integer> entry : this.hourlySolarData
						.entrySet()) {
					// System.out.println("loop processing...");
					this.log.debug(
							"loop processing[" + i + "]: " + entry.getKey());
					if (!entry.getKey().isBefore(currentHour)
							&& i < values.length) {
						switch ((int) this.duration) {
							case 60 :
								// convert hourly values in 15min steps
								values[i++] = entry.getValue();
								values[i++] = entry.getValue();
								values[i++] = entry.getValue();
								values[i++] = entry.getValue();
								break;
							case 15 :
								// no conversion
								values[i++] = entry.getValue();
								break;
							default :
								throw new IllegalArgumentException(
										"Unexpected duration for power: "
												+ this.duration + " minutes");
						}
					}
				}
				this.log.debug("loop completed: " + i + " iterations");

				this.channel(PredictorSolarTariffEvcc.ChannelId.PREDICT_ENABLED)
						.setNextValue(true);

				this.channel(PredictorSolarTariffEvcc.ChannelId.PREDICT)
						.setNextValue(values[0]);

				this.log.debug("current PV energy prediction: " + values[0]
						+ " at: " + currentHour);

				Prediction prediction = Prediction.from(currentHour, values);

				// Return the prediction starting from the calculated time
				return prediction;
			} else {
				this.log.warn("No prediction data");
				return Prediction.EMPTY_PREDICTION;
			}

		} catch (Exception e) {
			this.log.error("Error creating prediction: ", e.toString());
			return Prediction.EMPTY_PREDICTION;
		}
	}

	@Override
	public String debugLog() {
		return "Prediction: " //
				+ this.getPrediction(this.getChannelAddresses()[0]).getFirst() //
				+ " Wh";
		// this.channel(PredictorSolarTariffEvcc.ChannelId.PREDICT).value();
	}
}