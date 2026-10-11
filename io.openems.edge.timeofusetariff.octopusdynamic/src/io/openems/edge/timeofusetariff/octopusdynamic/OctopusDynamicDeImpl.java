package io.openems.edge.timeofusetariff.octopusdynamic;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
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
 * Provides account-specific dynamic electricity tariffs from Octopus Energy
 * Germany.
 *
 * <p>
 * The component retrieves the gross quarter-hourly rates supplied by the
 * provider and converts them from ct/kWh to EUR/MWh as required by OpenEMS.
 * Authentication credentials are supplied through the component configuration.
 *
 * <p>
 * Previously validated forecasts remain available during temporary API
 * failures. The {@link #getPrices()} method excludes elapsed time intervals.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "TimeOfUseTariff.OctopusDynamicDE", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class OctopusDynamicDeImpl extends AbstractOpenemsComponent
		implements OpenemsComponent, TimeOfUseTariff {

	private final Logger log = LoggerFactory.getLogger(OctopusDynamicDeImpl.class);
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

	private volatile long refreshMinutes = 30;

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
		this.refreshMinutes = Math.max(15, config.refreshMinutes());
		this.executor.execute(this::refreshAndSchedule);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.executor.shutdownNow();
	}

	@Override
	public TimeOfUsePrices getPrices() {
		return TimeOfUsePrices.from(Instant.now(), this.prices.get());
	}

	void refreshAndSchedule() {
		boolean successful = false;
		try {
			successful = this.refresh(this.accountNumber, this.email, this.password);
		} finally {
			if (!this.executor.isShutdown()) {
				this.scheduleNextRefresh(nextRefreshDelayMinutes(successful, this.refreshMinutes));
			}
		}
	}

	void scheduleNextRefresh(long delayMinutes) {
		this.executor.schedule(this::refreshAndSchedule, delayMinutes, TimeUnit.MINUTES);
	}

	static long nextRefreshDelayMinutes(boolean successful, long refreshMinutes) {
		return successful ? Math.max(15, refreshMinutes) : 5;
	}

	boolean refresh(String accountNumber, String email, String password) {
		String stage = "configuration";
		try {
			if (email == null || password == null || accountNumber == null
					|| email.isBlank() || password.isBlank() || accountNumber.isBlank()) {
				throw new IOException("CONFIGURATION_INCOMPLETE");
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
			account.addProperty("accountNumber", accountNumber);
			stage = "forecast";
			var forecast = this.post(PRICE_QUERY, account, token);
			stage = "price-validation";
			this.prices.set(parsePrices(forecast));
			return true;
		} catch (Exception ex) {
			String reason = categorizeFailure(stage, ex);
			this.log.warn("Octopus Dynamic DE refresh failed at stage [{}], category [{}]: {}. "
					+ "Previously valid prices retained; retry scheduled in 5 minutes.",
					stage, reason, describeFailure(reason));
			return false;
		}
	}


	/**
	 * Classifies provider failures without disclosing account credentials or
	 * response contents in diagnostic messages.
	 *
	 * @param stage the operation that failed
	 * @param exception the underlying exception
	 * @return a non-sensitive diagnostic category
	 */
	static String categorizeFailure(String stage, Exception exception) {
		if (exception instanceof SocketTimeoutException) {
			return "NETWORK_TIMEOUT";
		}
		if (exception instanceof UnknownHostException) {
			return "NETWORK_UNREACHABLE";
		}
		if (!(exception instanceof IOException)) {
			return "UNEXPECTED_RESPONSE";
		}
		var message = exception.getMessage();
		if (message == null) {
			return "REQUEST_FAILED";
		}
		return switch (message) {
		case "CONFIGURATION_INCOMPLETE" -> "CONFIGURATION_INCOMPLETE";
		case "ACCOUNT_NOT_FOUND" -> "ACCOUNT_NOT_FOUND";
		case "AUTH_TOKEN_MISSING", "AUTH_TOKEN_EMPTY" -> "AUTHENTICATION_REJECTED";
		case "HTTP_STATUS_401", "HTTP_STATUS_403" ->
			"authentication".equals(stage) ? "AUTHENTICATION_REJECTED" : "ACCOUNT_ACCESS_DENIED";
		case "GRAPHQL_ERRORS" ->
			"authentication".equals(stage) ? "AUTHENTICATION_REJECTED" : "PROVIDER_REJECTED_REQUEST";
		case "HTTP_STATUS_429" -> "API_RATE_LIMITED";
		case "HTTP_STATUS_500", "HTTP_STATUS_502", "HTTP_STATUS_503", "HTTP_STATUS_504" -> "PROVIDER_UNAVAILABLE";
		case "RESPONSE_NOT_JSON", "GRAPHQL_DATA_MISSING", "Unexpected Octopus price schema" ->
			"INVALID_API_RESPONSE";
		case "Unaligned quarter-hour price", "Ambiguous price", "Invalid price",
			"Conflicting price records", "Empty price list", "Missing quarter-hour price" ->
			"INVALID_PRICE_FORECAST";
		default -> "REQUEST_FAILED";
		};
	}

	/**
	 * Human-readable diagnostics. Never include API payloads, exception messages,
	 * passwords, tokens, account identifiers or email addresses.
	 */
	static String describeFailure(String category) {
		return switch (category) {
		case "CONFIGURATION_INCOMPLETE" -> "Please configure the account number, email and password";
		case "AUTHENTICATION_REJECTED" -> "Authentication failed; please check the email address and password";
		case "ACCOUNT_NOT_FOUND", "ACCOUNT_ACCESS_DENIED", "PROVIDER_REJECTED_REQUEST" ->
			"Account price request rejected; check the account number and account access permissions";
		case "NETWORK_TIMEOUT" -> "Connection to Octopus Energy timed out";
		case "NETWORK_UNREACHABLE" -> "Octopus Energy API host could not be reached";
		case "API_RATE_LIMITED" -> "Octopus Energy API rate limit reached";
		case "PROVIDER_UNAVAILABLE" -> "Octopus Energy API is temporarily unavailable";
		case "INVALID_PRICE_FORECAST" -> "Octopus Energy returned incomplete or inconsistent quarter-hour prices";
		case "INVALID_API_RESPONSE" -> "Octopus Energy returned an unexpected API response";
		default -> "Octopus Energy price refresh failed";
		};
	}

	JsonObject post(String query, JsonObject variables, String token) throws IOException {
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
	 * Parses the account-specific price forecast into a continuous sequence of
	 * quarter-hourly rates.
	 *
	 * <p>
	 * Prices are converted from gross ct/kWh to EUR/MWh. Invalid durations,
	 * missing intervals, and conflicting rates cause the entire forecast to be
	 * rejected.
	 *
	 * @param data the GraphQL response data
	 * @return the validated time-of-use price forecast
	 * @throws IOException if the response contains missing, ambiguous, or
	 *                     invalid price data
	 */
	static TimeOfUsePrices parsePrices(JsonObject data) throws IOException {
		if (!data.has("account") || data.get("account").isJsonNull()) {
			throw new IOException("ACCOUNT_NOT_FOUND");
		}
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
