package io.openems.edge.predictor.weather.forecast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenMeteoForecast {
    private static final Logger logger = LoggerFactory.getLogger(OpenMeteoForecast.class);
    private String apiUrl;
    private JsonObject json;

    public OpenMeteoForecast() {
        //  constructor.
    }

    /**
     * Fetch weather forecast data for the given coordinates.
     *
     * @param latitude  Latitude of the location.
     * @param longitude Longitude of the location.
     * @throws Exception If fetching the data fails.
     */
    public void fetchData(String latitude, String longitude) throws Exception {
        this.apiUrl = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude
                + "&longitude=" + longitude
                + "&minutely_15=shortwave_radiation&forecast_days=3";

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(apiUrl).openStream()))) {
            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed: HTTP error code: " + conn.getResponseCode());
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            this.json = JsonParser.parseString(response.toString()).getAsJsonObject();
        } catch (Exception e) {
            logger.error("Error in fetching weather data: ", e);
            throw e;
        }
    }

    /**
     * Get the shortwave radiation data from the fetched JSON.
     *
     * @return Optional list of shortwave radiation values.
     * 
     * in the future one can add other factors such as temparature, cloud cover , snow cover etc , each parameters can be fetched individually and used for 
     * production power calculation 
     */
    public Optional<List<Double>> getShortWaveRadiation() {
        return Optional.ofNullable(json)
                .map(j -> j.getAsJsonObject("minutely_15"))
                .map(m -> m.getAsJsonArray("shortwave_radiation"))
                .map(arr -> IntStream.range(0, arr.size())
                        .mapToObj(arr::get)
                        .map(element -> element.getAsDouble())
                        .collect(Collectors.toList()));
    }
}
