package io.openems.edge.predictor.solartariff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public class PredictorSolarTariffEvccAPI {

    private final String apiUrl;

    public PredictorSolarTariffEvccAPI(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public JsonArray getSolarForecast() throws OpenemsNamedException {
        JsonObject jsonResponse = sendGetRequest(apiUrl);
        if (jsonResponse != null && jsonResponse.has("result")) {
            return jsonResponse.getAsJsonObject("result").getAsJsonArray("rates");
        } else {
            throw new OpenemsException("Invalid or empty response from Solar Forecast API.");
        }
    }

    private JsonObject sendGetRequest(String URL) throws OpenemsNamedException {
        try {
            URL url = new URL(URL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            int status = con.getResponseCode();
            String body;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    content.append(line);
                }
                body = content.toString();
            }
            if (status < 300) {
                return JsonUtils.parseToJsonObject(body);
            } else {
                throw new OpenemsException("Error while reading from API. Response code: " + status + ". " + body);
            }
        } catch (IOException e) {
            throw new OpenemsException("Unable to connect to the Solar Forecast API.");
        }
    }
}
