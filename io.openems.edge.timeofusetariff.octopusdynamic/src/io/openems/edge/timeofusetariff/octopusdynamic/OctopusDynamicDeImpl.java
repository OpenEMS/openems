package io.openems.edge.timeofusetariff.octopusdynamic;

import java.io.IOException;
import java.time.Instant;
import java.util.TreeMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Octopus Energy Germany dynamic tariff provider.
 *
 * <p>
 * All prices are provider-gross final prices (cent/kWh -> EUR/MWh).
 * Existing supplier/network charges MUST NOT be added again.
 *
 * <p>
 * Credentials are provided via the OpenEMS component configuration.
 * Do NOT log GraphQL responses, tokens or credentials.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "TimeOfUseTariff.OctopusDynamicDE", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class OctopusDynamicDeImpl extends AbstractOpenemsComponent
        implements OpenemsComponent, TimeOfUseTariff {

    private static final Logger LOG = LoggerFactory.getLogger(OctopusDynamicDeImpl.class);
    private static final String API_URL = "https://api.oeg-kraken.energy/v1/graphql/";
    private static final String TOKEN_MUTATION = """
        mutation Authentication($email: String!, $password: String!) {
            obtainKrakenToken(input: {email: $email, password: $password}) { token }
        }
        """;
    private static final String PRICE_QUERY = """
        query Prices($accountNumber: String!) {
          account(accountNumber: $accountNumber) {
            properties { electricityMalos { agreements {
              unitRateForecast { validFrom validTo unitRateInformation {
                ... on TimeOfUseProductUnitRateInformation {
                  rates { netUnitRateCentsPerKwh latestGrossUnitRateCentsPerKwh }
                }
              } }
            } } }
          }
        }
        """;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .callTimeout(20, TimeUnit.SECONDS).build();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        var t = new Thread(r, "octopus-dynamic-de");
        t.setDaemon(true);
        return t;
    });
    private final AtomicReference<TimeOfUsePrices> prices =
            new AtomicReference<>(TimeOfUsePrices.EMPTY_PRICES);

    private volatile String accountNumber;
    private volatile String email;
    private volatile String password;

    public OctopusDynamicDeImpl() {
        super(OpenemsComponent.ChannelId.values());
    }

    @Activate
    private void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        if (!config.enabled()) {
            return;
        }
        this.accountNumber = config.accountNumber();
        this.email = config.email();
        this.password = config.password();
        long refresh = Math.max(15, config.refreshMinutes());
        this.executor.scheduleWithFixedDelay(this::refresh, 0, refresh, TimeUnit.MINUTES);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
        this.executor.shutdownNow();
    }

    @Override
    public TimeOfUsePrices getPrices() {
        // Only publish from the current quarter onward; stale periods are removed.
        return TimeOfUsePrices.from(Instant.now(), this.prices.get());
    }

    private void refresh() {
        String stage = "configuration";
        try {
            var email = this.email;
            var password = this.password;
            if (email == null || password == null || this.accountNumber == null
                    || email.isBlank() || password.isBlank() || this.accountNumber.isBlank()) {
                throw new IOException("Octopus credentials/account not configured");
            }
            var variables = new JsonObject();
            variables.addProperty("email", email);
            variables.addProperty("password", password);
            stage = "authentication";
            var auth = this.post(TOKEN_MUTATION, variables, null);
            var authResult = auth.getAsJsonObject("obtainKrakenToken");
            if (authResult == null || !authResult.has("token") || authResult.get("token").isJsonNull()) {
                throw new IOException("AUTH_TOKEN_MISSING");
            }
            var token = authResult.get("token").getAsString();
            if (token.isBlank()) {
                throw new IOException("AUTH_TOKEN_EMPTY");
            }
            var account = new JsonObject();
            account.addProperty("accountNumber", this.accountNumber);
            stage = "forecast";
            var forecast = this.post(PRICE_QUERY, account, token);
            stage = "price-validation";
            this.prices.set(parsePrices(forecast));
        } catch (Exception ex) {
            // Do not expose HTTP response bodies or messages that may contain credentials.
            String reason = ex instanceof IOException ? ex.getMessage() : "UNEXPECTED_RESPONSE_SHAPE";
            LOG.warn("Octopus Dynamic DE refresh failed at stage [{}], category [{}]; clearing stale prices",
                    stage, reason);
            this.prices.set(TimeOfUsePrices.EMPTY_PRICES);
        }
    }

    private JsonObject post(String query, JsonObject variables, String token) throws IOException {
        var json = new JsonObject();
        json.addProperty("query", query);
        json.add("variables", variables);
        var builder = new Request.Builder().url(API_URL)
                .post(RequestBody.create(json.toString(),
                        MediaType.parse("application/json; charset=utf-8")));
        if (token != null) {
            builder.header("Authorization", token);
        }
        try (var response = this.client.newCall(builder.build()).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("HTTP_STATUS_" + response.code());
            }
            JsonObject payload;
            try {
                payload = JsonParser.parseString(response.body().string()).getAsJsonObject();
            } catch (RuntimeException ex) {
                throw new IOException("RESPONSE_NOT_JSON");
            }
            if (payload.has("errors") && payload.getAsJsonArray("errors").size() > 0) {
                throw new IOException("GRAPHQL_ERRORS");
            }
            if (!payload.has("data") || payload.get("data").isJsonNull()) {
                throw new IOException("GRAPHQL_DATA_MISSING");
            }
            return payload.getAsJsonObject("data");
        }
    }

    private static JsonArray array(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return new JsonArray();
        }
        return obj.getAsJsonArray(key);
    }

    /**
     * Input: gross cent/kWh. Output: currency/MWh (1 cent/kWh = 10 EUR/MWh).
     * Refuse conflicting locations/rates and missing quarter-hour slots.
     *
     * @param data Octopus GraphQL data object
     * @return validated quarterly prices for OpenEMS
     * @throws IOException on missing, ambiguous, or invalid prices
     */
    static TimeOfUsePrices parsePrices(JsonObject data) throws IOException {
        var values = new TreeMap<Instant, Double>();
        try {
            for (var property : array(data.getAsJsonObject("account"), "properties")) {
                for (var malo : array(property.getAsJsonObject(), "electricityMalos")) {
                    for (var agreement : array(malo.getAsJsonObject(), "agreements")) {
                        for (var slot : array(agreement.getAsJsonObject(), "unitRateForecast")) {
                            var x = slot.getAsJsonObject();
                            var start = Instant.parse(x.get("validFrom").getAsString());
                            var end = Instant.parse(x.get("validTo").getAsString());
                            if (end.getEpochSecond() - start.getEpochSecond() != 900
                                    || start.getEpochSecond() % 900 != 0) {
                                throw new IOException("Unaligned quarter-hour price");
                            }
                            var rates = array(x.getAsJsonObject("unitRateInformation"), "rates");
                            if (rates.size() != 1) {
                                throw new IOException("Ambiguous price");
                            }
                            double gross = rates.get(0).getAsJsonObject()
                                    .get("latestGrossUnitRateCentsPerKwh").getAsDouble();
                            if (!Double.isFinite(gross)) {
                                throw new IOException("Invalid price");
                            }
                            // Octopus gross cent/kWh -> OpenEMS currency/MWh
                            var previous = values.putIfAbsent(start, gross * 10.0);
                            if (previous != null && Double.compare(previous, gross * 10.0) != 0) {
                                throw new IOException("Conflicting price records");
                            }
                        }
                    }
                }
            }
        } catch (RuntimeException ex) {
            throw new IOException("Unexpected Octopus price schema");
        }
        if (values.isEmpty()) {
            throw new IOException("Empty price list");
        }
        Instant expected = null;
        for (Map.Entry<Instant, Double> row : values.entrySet()) {
            if (expected != null && !row.getKey().equals(expected)) {
                throw new IOException("Missing quarter-hour price");
            }
            expected = row.getKey().plusSeconds(900);
        }
        return TimeOfUsePrices.from(ImmutableSortedMap.copyOf(values));
    }
}
