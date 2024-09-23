package io.openems.edge.greenconsumptionadvisor.api;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.osgi.service.cm.ConfigurationAdmin;
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

import io.openems.common.utils.JsonUtils;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Green.Consumption.Advice.API", // This name has to be kept for compatibility reasons
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class GreenConsumptionAdvisorImpl extends AbstractOpenemsComponent
		implements Controller, OpenemsComponent, GreenConsumptionAdvisor {

	@Reference
	private ConfigurationAdmin cm;

	private final Logger log = LoggerFactory.getLogger(GreenConsumptionAdvisorImpl.class);

	private static final String URL_API_RECOMMENDATION = "https://api.corrently.io/v2.0/gsi/advisor?zip=";
	private String urlString;
	private LocalDateTime timeNextActualization;
	private GridEmissionInformation currentInformation;
	private JsonArray dataBuffer;

	public GreenConsumptionAdvisorImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				GreenConsumptionAdvisor.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		// TODO validate zip code
		this.urlString = URL_API_RECOMMENDATION + config.zip_code();
		this.timeNextActualization = LocalDateTime.now();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {

		if (this.timeNextActualization.isBefore(LocalDateTime.now())) {
			this.updateDataBuffer();
			this.timeNextActualization = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(1);
		}
		this.evaluateDataBuffer();
		this.writeRecommendationToChannel();
	}

	private void evaluateDataBuffer() {
		if (this.dataBuffer == null) {
			return;
		}
		if (this.currentInformation == null) {
			try {
				this.currentInformation = this.getGridEmissionInformationFromJson(this.dataBuffer.get(0));
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
			}
		}
		// check if newer information available, if yes actualize and delete first
		// element of buffer
		LocalDateTime lastFullHour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
		int i;
		try {
			for (i = 0; i < this.dataBuffer.size() - 1; i++) {
				GridEmissionInformation bufferElement = this.getGridEmissionInformationFromJson(this.dataBuffer.get(i));
				if (bufferElement.getTimestamp().equals(lastFullHour)) {
					this.currentInformation = bufferElement;
					break;
				}
			}
			// Check if no bufferElement was found, then reset all
			LocalDateTime timeLastElement = this.getGridEmissionInformationFromJson(this.dataBuffer.get(i))
					.getTimestamp();
			if (this.dataBuffer.size() - 1 == i && this.currentInformation.getTimestamp().isAfter(timeLastElement)) {
				this.dataBuffer = null;
				this.currentInformation = null;
			}
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
	}

	private GridEmissionInformation getGridEmissionInformationFromJson(JsonElement jsonElement)
			throws OpenemsNamedException {
		long timestamp = JsonUtils.getAsLong(jsonElement, "time");
		Instant instant = Instant.ofEpochMilli(timestamp);
		// Convert the Instant to LocalDateTime in the system's default time zone
		LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
		int co2 = JsonUtils.getAsInt(jsonElement, "co2");
		String advice = JsonUtils.getAsString(jsonElement, "advice");
		ConsumptionAdvice consAdvice = ConsumptionAdvice.UNDEFINED;
		switch (advice) {
		case "green":
			consAdvice = ConsumptionAdvice.GREEN;
			break;
		case "yellow":
			consAdvice = ConsumptionAdvice.YELLOW;
			break;
		case "red":
			consAdvice = ConsumptionAdvice.RED;
			break;
		}
		return new GridEmissionInformation(dateTime, consAdvice, co2);
	}

	private void updateDataBuffer() {
		JsonArray predictionData;
		try {
			URL url = new URL(this.urlString);
			// Open connection
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");

			// Check if the response code is 200 (HTTP_OK)
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("HTTP error code : " + conn.getResponseCode());
			}

			// Read the response from the input stream
			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			String output;
			StringBuilder response = new StringBuilder();
			while ((output = br.readLine()) != null) {
				response.append(output);
			}

			// Close the connection
			conn.disconnect();

			// Parse the JSON response
			JsonObject responseJson = JsonUtils.parseToJsonObject(response.toString());
			predictionData = responseJson.getAsJsonArray("data");

		} catch (Exception e) {
			e.printStackTrace();
			this.log.error(
					"Could not get latest data of CO2 emissions per kWh from dedicated API. Check correct zip-Code.");
			predictionData = this.dataBuffer;
		}
		this.dataBuffer = predictionData;

	}

	private void writeRecommendationToChannel() {
		if (this.currentInformation == null) {
			this._setConsumptionAdvice(ConsumptionAdvice.UNDEFINED);
		} else {
			this._setConsumptionAdvice(this.currentInformation.getConsumptionAdvice());
			this._setCo2EmissionsPerKilowatthour(this.currentInformation.getCo2Emissions());
		}
	}

}
