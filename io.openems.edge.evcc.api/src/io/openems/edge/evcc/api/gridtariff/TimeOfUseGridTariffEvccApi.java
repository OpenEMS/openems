package io.openems.edge.evcc.api.gridtariff;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

/**
 * API-Klasse für zeitabhängige Stromtarife über EVCC.
 */
public class TimeOfUseGridTariffEvccApi {

    private static final Logger log = LoggerFactory.getLogger(TimeOfUseGridTariffEvccApi.class);
    private final HttpClient client;
    private final String apiUrl;
    private final PriceCache cache = new PriceCache(); // Caching-Mechanismus

    /**
     * Konstruktor für die API-Kommunikation.
     * 
     * @param apiUrl URL der EVCC-API
     */
    public TimeOfUseGridTariffEvccApi(String apiUrl) {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.apiUrl = apiUrl;
    }

    /**
     * Holt Preise entweder aus dem Cache oder führt eine API-Abfrage durch.
     * 
     * @return Zeitabhängige Preise.
     */
    public TimeOfUsePrices fetchPrices() {
        return this.cache.getPrices().orElseGet(() -> {
            TimeOfUsePrices prices = this.fetchPricesFromApi();
            this.cache.updatePrices(prices);
            return prices;
        });
    }

    /**
     * Führt eine API-Abfrage durch, wenn der Cache nicht gültig ist.
     * 
     * @return Zeitabhängige Preise aus der API.
     */
    private TimeOfUsePrices fetchPricesFromApi() {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(this.apiUrl)).GET().timeout(Duration.ofSeconds(5)).build();

        try {
            HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300 && response.body() != null) {
                String json = response.body();
                return this.parsePrices(json);
            } else {
                log.warn("Failed to fetch prices. HTTP status code: {}", response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error while fetching prices", e);
        }

        return TimeOfUsePrices.EMPTY_PRICES;
    }

    private TimeOfUsePrices parsePrices(String jsonData) {
        try {
            var jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
            var resultObject = jsonObject.getAsJsonObject("result");
            var ratesArray = resultObject.getAsJsonArray("rates");
            var result = ImmutableSortedMap.<ZonedDateTime, Double>naturalOrder();

            for (JsonElement rateElement : ratesArray) {
                if (rateElement.isJsonObject()) {
                    JsonObject rateObject = rateElement.getAsJsonObject();
                    String startString = rateObject.get("start").getAsString();
                    String endString = rateObject.get("end").getAsString();
                    double value = rateObject.has("price") ? rateObject.get("price").getAsDouble() * 1000 : rateObject.get("value").getAsDouble() * 1000;

                    ZonedDateTime startsAt = ZonedDateTime.parse(startString, DateTimeFormatter.ISO_DATE_TIME).withZoneSameInstant(ZonedDateTime.now().getZone());
                    ZonedDateTime endsAt = ZonedDateTime.parse(endString, DateTimeFormatter.ISO_DATE_TIME).withZoneSameInstant(ZonedDateTime.now().getZone());
                    long duration = Duration.between(startsAt, endsAt).toMinutes();

                    switch ((int) duration) {
                        case 60:
                            for (int i = 0; i < 4; i++) {
                                ZonedDateTime quarterStart = startsAt.plusMinutes(i * 15);
                                result.put(quarterStart, value);
                            }
                            break;
                        case 15:
                            result.put(startsAt, value);
                            break;
                        default:
                            throw new IllegalArgumentException("Unexpected duration for rate: " + duration + " minutes");
                    }
                } else {
                    log.error("Rate element is not a JsonObject: {}", rateElement);
                }
            }

            return TimeOfUsePrices.from(result.build());
        } catch (Exception e) {
            log.error("Failed to parse EVCC API data", e);
            return TimeOfUsePrices.EMPTY_PRICES;
        }
    }

    // Caching-Klasse mit optimierter Javadoc und `this.` für Instanzvariablen.
    private static class PriceCache {
        private TimeOfUsePrices cachedPrices = TimeOfUsePrices.EMPTY_PRICES;
        private ZonedDateTime lastFetchTime = ZonedDateTime.now().minusMinutes(15);
        private static final Duration CACHE_DURATION = Duration.ofMinutes(15); // Anpassbare Cache-Dauer

        /**
         * Holt gespeicherte Preise, falls der Cache gültig ist.
         * 
         * @return Optional mit gespeicherten Preisen.
         */
        public Optional<TimeOfUsePrices> getPrices() {
            if (ZonedDateTime.now().isBefore(this.lastFetchTime.plus(CACHE_DURATION))) {
                return Optional.of(this.cachedPrices);
            }
            return Optional.empty();
        }

        /**
         * Aktualisiert den Cache mit neuen Preisen.
         * 
         * @param newPrices Neue Preise für den Cache.
         */
        public void updatePrices(TimeOfUsePrices newPrices) {
            this.cachedPrices = newPrices;
            this.lastFetchTime = ZonedDateTime.now();
        }
    }
}
