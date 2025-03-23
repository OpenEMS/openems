package io.openems.edge.predictor.solartariff;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
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
import io.openems.edge.common.event.EdgeEventConstants;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Predictor.SolarTariffEvccImpl", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "="
						+ EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		} //
)
public class PredictorSolarTariffEvccImpl extends AbstractPredictorHours
		implements
			PredictorHours,
			OpenemsComponent,
			EventHandler {

	private final Logger log = LoggerFactory
			.getLogger(PredictorSolarTariffEvccImpl.class);

	private PredictorSolarTariffEvccAPI solarForecastAPI;
	private boolean executed = false;
	private LocalDateTime prevHour = LocalDateTime.now();
	private final TreeMap<LocalDateTime, Integer> hourlySolarData = new TreeMap<>();

	@Reference
	private ComponentManager componentManager;

	public PredictorSolarTariffEvccImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				PredictorSolarTariffEvcc.ChannelId.values() //
		);
	}

	@Activate
	protected void activate(ComponentContext context, Config config)
			throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled(),
				config.channelAddresses());
		this.solarForecastAPI = new PredictorSolarTariffEvccAPI(config.url());
		log.info("PredictorSolarTariffEvccImpl activated.");
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		log.info("PredictorSolarTariffEvccImpl deactivated.");
	}

	@Override
	protected PredictionHours createNewPrediction(
			ChannelAddress channelAddress) {
		Integer[] result = new Integer[24];
		int i = 0;
		for (Entry<LocalDateTime, Integer> entry : hourlySolarData.entrySet()) {
			if (i < result.length)
				result[i++] = entry.getValue();
		}
		return new PredictionHours(result);
	}

	@Override
	protected ClockProvider getClockProvider() {
		return this.componentManager;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
			case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE :
				try {
					this.calculatePrediction();
					this.channel(
							PredictorSolarTariffEvcc.ChannelId.UNABLE_TO_PREDICT)
							.setNextValue(false);
				} catch (OpenemsNamedException e) {
					log.error(e.getMessage());
					this.channel(
							PredictorSolarTariffEvcc.ChannelId.UNABLE_TO_PREDICT)
							.setNextValue(true);
				}
				break;
		}
	}

	private void calculatePrediction() throws OpenemsNamedException {
		LocalDateTime currentHour = LocalDateTime
				.now(this.componentManager.getClock()).withNano(0).withMinute(0)
				.withSecond(0);
		JsonArray js = null;

		if (!executed) {
			js = this.solarForecastAPI.getSolarForecast();
			this.prevHour = currentHour;
			this.executed = true;
		} else if (currentHour.isAfter(this.prevHour)) {
			js = this.solarForecastAPI.getSolarForecast();
			this.prevHour = currentHour;
		} else {
			return;
		}

		if (js != null) {
			hourlySolarData.clear();
			for (int i = 0; i < js.size(); i++) {
				JsonElement timeElement = js.get(i).getAsJsonObject()
						.get("start");
				OffsetDateTime utcTime = OffsetDateTime
						.parse(timeElement.getAsString()); // UTC Zeit parsen
				LocalDateTime localTime = utcTime.toLocalDateTime(); // Zeit in
																		// LocalDateTime
																		// konvertieren

				JsonElement price = js.get(i).getAsJsonObject().get("price");
				//log.debug(...);
				this.logDebug(log, "SolarForecast prediction: " + localTime + " " + price.getAsInt() + " Wh");
				hourlySolarData.put(localTime, price.getAsInt());
			}
			this.channel(PredictorSolarTariffEvcc.ChannelId.PREDICT_ENABLED)
					.setNextValue(true);
			
			this.channel(PredictorSolarTariffEvcc.ChannelId.PREDICT).setNextValue(hourlySolarData.firstEntry().getValue());
			
		} else {
			log.warn("Failed to fetch solar forecast data.");
			this.channel(PredictorSolarTariffEvcc.ChannelId.PREDICT_ENABLED)
					.setNextValue(false);
		}
	}

	@Override
	public String debugLog() {
		return "Prediction: "
				+ this.channel(PredictorSolarTariffEvcc.ChannelId.PREDICT)
						.value().toString();
	}
}
