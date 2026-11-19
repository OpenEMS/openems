package io.openems.edge.timeofusetariff.octopus.dynamic.gridbuy;

import static io.openems.common.utils.DateUtils.parseZonedDateTimeOrError;
import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.common.utils.JsonUtils.getAsDouble;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.getAsOptionalJsonArray;
import static io.openems.common.utils.JsonUtils.getAsOptionalJsonObject;
import static io.openems.common.utils.JsonUtils.getAsOptionalString;
import static io.openems.common.utils.JsonUtils.getAsString;
import static io.openems.common.utils.JsonUtils.parseToJsonObject;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProviderChain;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.HttpStatus;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

final class Utils {

	private static final String TOKEN_MUTATION = """
			mutation Authentication($email: String!, $password: String!) {
				obtainKrakenToken(input: {email: $email, password: $password}) { token }
			}""";

	private static final String PRICE_QUERY = """
			query Prices($accountNumber: String!) {
				account(accountNumber: $accountNumber) {
					properties { electricityMalos { agreements {
						unitRateForecast { validFrom validTo unitRateInformation {
							... on TimeOfUseProductUnitRateInformation { rates { latestGrossUnitRateCentsPerKwh } }
						} }
					} } }
				}
			}""";

	private static final int CONNECT_TIMEOUT_MS = 10_000;
	private static final int READ_TIMEOUT_MS = 20_000;
	private static final long QUARTER_SECONDS = 900;
	/** Kraken error code for "too many requests". */
	private static final String RATE_LIMIT_ERROR_CODE = "KT-CT-1199";
	/** Kraken error codes for an expired or invalid token. */
	private static final Set<String> AUTHORIZATION_ERROR_CODES = Set.of("KT-CT-1124");
	/** Values of extensions.errorType/errorClass for authentication errors. */
	private static final Set<String> AUTHORIZATION_ERROR_TYPES = Set.of("AUTHORIZATION", "AUTHENTICATION");

	/** Kraken API prices are in ct/kWh; multiply by 10 to get EUR/MWh. */
	private static final double CENTS_PER_KWH_TO_CURRENCY_PER_MWH = 10.0;

	static final Duration RATE_LIMIT_DELAY = Duration.ofHours(1);
	static final Duration ERROR_RETRY_DELAY = Duration.ofMinutes(5);
	static final int MIN_REFRESH_MINUTES = 15;

	private Utils() {
	}

	/**
	 * Result of a price update, used to decide on the next request.
	 */
	enum UpdateResult {
		/** Prices were updated. */
		SUCCESS,
		/** HTTP 429; retry after a long delay. */
		RATE_LIMITED,
		/** Credentials rejected; do not retry until the configuration changes. */
		AUTHENTICATION_FAILED,
		/** Any other error; retry after a short delay. */
		FAILED;
	}

	/**
	 * A failed request to the Kraken API with the resulting {@link UpdateResult}.
	 */
	static final class KrakenApiException extends Exception {

		private static final long serialVersionUID = 1L;

		private final UpdateResult result;

		KrakenApiException(UpdateResult result, String message) {
			super(message);
			this.result = result;
		}

		UpdateResult getResult() {
			return this.result;
		}
	}

	/**
	 * Delay time provider for the Octopus price endpoint.
	 */
	static class OctopusDelayTimeProvider implements DelayTimeProvider<UpdateResult> {

		private final Duration refreshInterval;

		OctopusDelayTimeProvider(int refreshMinutes) {
			this.refreshInterval = Duration.ofMinutes(Math.max(MIN_REFRESH_MINUTES, refreshMinutes));
		}

		@Override
		public Delay onFirstRunDelay() {
			return Delay.immediate();
		}

		@Override
		public Delay onErrorRunDelay(HttpError error) {
			return this.getDelay(classify(error));
		}

		@Override
		public Delay onSuccessRunDelay(UpdateResult result) {
			return this.getDelay(result == null ? UpdateResult.FAILED : result);
		}

		Delay getDelay(UpdateResult result) {
			return switch (result) {
			case SUCCESS -> DelayTimeProviderChain.fixedDelay(this.refreshInterval) //
					.plusRandomDelay(60, SECONDS) //
					.getDelay();
			case RATE_LIMITED -> DelayTimeProviderChain.fixedDelay(RATE_LIMIT_DELAY) //
					.plusRandomDelay(60, SECONDS) //
					.getDelay();
			case AUTHENTICATION_FAILED -> Delay.infinite();
			case FAILED -> DelayTimeProviderChain.fixedDelay(ERROR_RETRY_DELAY) //
					.plusRandomDelay(30, SECONDS) //
					.getDelay();
			};
		}
	}

	/**
	 * Classifies an error of a request chain.
	 *
	 * <p>
	 * Walks the cause chain, because errors of the token request arrive wrapped
	 * in completion exceptions.
	 *
	 * @param error the {@link Throwable}
	 * @return the {@link UpdateResult}
	 */
	static UpdateResult classify(Throwable error) {
		for (var t = error; t != null; t = t.getCause()) {
			if (t instanceof KrakenApiException e) {
				return e.getResult();
			}
			if (t instanceof HttpError.ResponseError re) {
				final var code = re.status.code();
				if (code == HttpStatus.TOO_MANY_REQUESTS.code()) {
					return UpdateResult.RATE_LIMITED;
				}
				if (code == HttpStatus.UNAUTHORIZED.code() || code == HttpStatus.FORBIDDEN.code()) {
					return UpdateResult.AUTHENTICATION_FAILED;
				}
				return UpdateResult.FAILED;
			}
		}
		return UpdateResult.FAILED;
	}

	/**
	 * Gets the HTTP status code of the first response error in the cause chain.
	 *
	 * @param error the {@link Throwable}
	 * @return the status code or -1 if there is none
	 */
	static int getHttpStatusCode(Throwable error) {
		for (var t = error; t != null; t = t.getCause()) {
			if (t instanceof HttpError.ResponseError re) {
				return re.status.code();
			}
		}
		return -1;
	}

	/**
	 * Creates the endpoint to obtain a Kraken API token.
	 *
	 * @param country  the {@link Country}
	 * @param email    the account email
	 * @param password the account password
	 * @return the {@link BridgeHttp.Endpoint}
	 */
	static BridgeHttp.Endpoint createTokenEndpoint(Country country, String email, String password) {
		return createGraphQlEndpoint(country, TOKEN_MUTATION, JsonUtils.buildJsonObject() //
				.addProperty("email", email) //
				.addProperty("password", password) //
				.build());
	}

	/**
	 * Creates the endpoint to query the unit rate forecast of an account.
	 *
	 * @param country       the {@link Country}
	 * @param accountNumber the account number
	 * @return the {@link BridgeHttp.Endpoint}
	 */
	static BridgeHttp.Endpoint createPriceEndpoint(Country country, String accountNumber) {
		return createGraphQlEndpoint(country, PRICE_QUERY, JsonUtils.buildJsonObject() //
				.addProperty("accountNumber", accountNumber) //
				.build());
	}

	private static BridgeHttp.Endpoint createGraphQlEndpoint(Country country, String query, JsonObject variables) {
		return BridgeHttp.create(country.getApiUrl()) //
				.setConnectTimeout(CONNECT_TIMEOUT_MS) //
				.setReadTimeout(READ_TIMEOUT_MS) //
				.setBodyJson(JsonUtils.buildJsonObject() //
						.addProperty("query", query) //
						.add("variables", variables) //
						.build()) //
				.build();
	}

	/**
	 * Classifies the GraphQL errors of a response.
	 *
	 * <p>
	 * "Too many requests" (KT-CT-1199) is a rate limit. Errors with
	 * {@code extensions.errorType} or {@code errorClass} AUTHORIZATION or
	 * AUTHENTICATION, or an expired-token code, are authentication errors. Any
	 * other error is a generic failure.
	 *
	 * @param json the response body
	 * @return the {@link UpdateResult}; empty if there are no errors
	 */
	static Optional<UpdateResult> classifyGraphQlErrors(JsonObject json) {
		final var errors = getAsOptionalJsonArray(json, "errors").orElseGet(JsonArray::new);
		if (errors.isEmpty()) {
			return Optional.empty();
		}
		final var extensions = errors.asList().stream() //
				.map(e -> getAsOptionalJsonObject(e, "extensions").orElseGet(JsonObject::new)) //
				.toList();
		if (extensions.stream().anyMatch(x -> RATE_LIMIT_ERROR_CODE.equals(string(x, "errorCode")))) {
			return Optional.of(UpdateResult.RATE_LIMITED);
		}
		if (extensions.stream().anyMatch(x -> AUTHORIZATION_ERROR_CODES.contains(string(x, "errorCode")) //
				|| AUTHORIZATION_ERROR_TYPES.contains(string(x, "errorType")) //
				|| AUTHORIZATION_ERROR_TYPES.contains(string(x, "errorClass")))) {
			return Optional.of(UpdateResult.AUTHENTICATION_FAILED);
		}
		return Optional.of(UpdateResult.FAILED);
	}

	/**
	 * Whether a GraphQL response reports an authentication error, e.g. an expired
	 * token.
	 *
	 * @param jsonData the response body
	 * @return true if the response contains an authentication error
	 */
	static boolean hasGraphQlAuthorizationError(String jsonData) {
		if (jsonData == null) {
			return false;
		}
		try {
			return classifyGraphQlErrors(parseToJsonObject(jsonData)) //
					.map(r -> r == UpdateResult.AUTHENTICATION_FAILED) //
					.orElse(false);
		} catch (OpenemsNamedException e) {
			return false;
		}
	}

	/**
	 * Parses the token from the obtainKrakenToken mutation response.
	 *
	 * <p>
	 * GraphQL errors of the login are treated as rejected credentials (except
	 * rate limits), so that wrong credentials are not retried repeatedly.
	 *
	 * @param jsonData the response body
	 * @return the token
	 * @throws KrakenApiException    if the login failed
	 * @throws OpenemsNamedException on parse error
	 */
	static String parseToken(String jsonData) throws KrakenApiException, OpenemsNamedException {
		final var json = parseToJsonObject(jsonData);
		final var errorResult = classifyGraphQlErrors(json);
		if (errorResult.isPresent()) {
			final var result = errorResult.get() == UpdateResult.RATE_LIMITED //
					? UpdateResult.RATE_LIMITED //
					: UpdateResult.AUTHENTICATION_FAILED;
			throw new KrakenApiException(result, "Login failed with GraphQL errors");
		}
		return getAsOptionalJsonObject(json, "data") //
				.flatMap(data -> getAsOptionalJsonObject(data, "obtainKrakenToken")) //
				.flatMap(result -> getAsOptionalString(result, "token")) //
				.filter(t -> !t.isBlank()) //
				.orElseThrow(() -> new KrakenApiException(UpdateResult.FAILED, "No token in login response"));
	}

	/**
	 * Parses the account unit rate forecast to {@link TimeOfUsePrices}.
	 *
	 * <p>
	 * The account may contain several agreements, e.g. around a contract change
	 * or for different supply points. Empty forecasts are skipped. The agreement
	 * that is active at {@code now} is preferred (if several are active, the most
	 * recently started one); other agreements only fill the time after it.
	 * Prices before {@code now} are dropped. The result must start at the current
	 * quarter and is cut at the first gap, so it is always continuous.
	 * Individual invalid slots and quarters with conflicting prices within one
	 * agreement are skipped instead of rejecting the whole forecast.
	 *
	 * <p>
	 * Prices are converted from gross ct/kWh to currency/MWh.
	 *
	 * @param jsonData the response body
	 * @param now      the current time
	 * @return the {@link TimeOfUsePrices}
	 * @throws KrakenApiException    on GraphQL errors
	 * @throws OpenemsNamedException if the response is invalid or contains no
	 *                               price for the current quarter
	 */
	static TimeOfUsePrices parsePrices(String jsonData, Instant now) throws KrakenApiException, OpenemsNamedException {
		final var json = parseToJsonObject(jsonData);
		final var errorResult = classifyGraphQlErrors(json);
		if (errorResult.isPresent()) {
			throw new KrakenApiException(errorResult.get(), "GraphQL errors in price response");
		}
		final var account = getAsJsonObject(getAsJsonObject(json, "data"), "account");
		final var currentQuarter = roundDownToQuarter(now);

		final var agreements = new ArrayList<NavigableMap<Instant, Double>>();
		for (var property : array(account, "properties")) {
			for (var malo : array(property, "electricityMalos")) {
				for (var agreement : array(malo, "agreements")) {
					final var slots = parseForecast(array(agreement, "unitRateForecast"));
					if (!slots.isEmpty()) {
						agreements.add(slots);
					}
				}
			}
		}

		final var merged = mergeAgreements(agreements, currentQuarter);
		final var result = ImmutableSortedMap.<Instant, Double>naturalOrder();
		// A forecast that starts in the future must not be presented as current price
		var expected = currentQuarter;
		for (var entry : merged.tailMap(currentQuarter, true).entrySet()) {
			if (!entry.getKey().equals(expected)) {
				break; // gap: keep the continuous part only
			}
			result.put(entry.getKey(), entry.getValue());
			expected = entry.getKey().plusSeconds(QUARTER_SECONDS);
		}

		final var prices = TimeOfUsePrices.from(result.build());
		if (prices.isEmpty()) {
			throw new OpenemsException("No price for the current quarter in Octopus price response");
		}
		return prices;
	}

	/**
	 * Merges the agreements, preferring the one active at the current quarter.
	 *
	 * @param agreements     the parsed agreements
	 * @param currentQuarter the current quarter
	 * @return the merged prices
	 */
	private static NavigableMap<Instant, Double> mergeAgreements(List<NavigableMap<Instant, Double>> agreements,
			Instant currentQuarter) {
		// Active agreements first (the most recently started one wins), then the
		// others by start time
		agreements.sort((a, b) -> {
			final var aActive = a.containsKey(currentQuarter);
			final var bActive = b.containsKey(currentQuarter);
			if (aActive != bActive) {
				return aActive ? -1 : 1;
			}
			return aActive //
					? b.firstKey().compareTo(a.firstKey()) //
					: a.firstKey().compareTo(b.firstKey());
		});
		final var merged = new TreeMap<Instant, Double>();
		for (var agreement : agreements) {
			if (merged.isEmpty()) {
				merged.putAll(agreement);
				continue;
			}
			// Later agreements only continue after the preferred data
			agreement.tailMap(merged.lastKey(), false).forEach(merged::putIfAbsent);
		}
		return merged;
	}

	/**
	 * Parses one unitRateForecast into quarter-hourly prices.
	 *
	 * @param forecast the unitRateForecast array
	 * @return the prices per quarter; invalid or conflicting slots are skipped
	 */
	private static NavigableMap<Instant, Double> parseForecast(JsonArray forecast) {
		final var result = new TreeMap<Instant, Double>();
		final var conflicts = new HashSet<Instant>();
		for (var slot : forecast) {
			try {
				final var start = parseZonedDateTimeOrError(getAsString(slot, "validFrom")).toInstant();
				final var end = parseZonedDateTimeOrError(getAsString(slot, "validTo")).toInstant();
				final var seconds = end.getEpochSecond() - start.getEpochSecond();
				if (seconds <= 0 || seconds % QUARTER_SECONDS != 0 || start.getEpochSecond() % QUARTER_SECONDS != 0) {
					continue;
				}
				final var rates = array(getAsJsonObject(slot, "unitRateInformation"), "rates");
				if (rates.size() != 1) {
					continue; // missing or ambiguous rate
				}
				final var price = getAsDouble(rates.get(0), "latestGrossUnitRateCentsPerKwh")
						* CENTS_PER_KWH_TO_CURRENCY_PER_MWH;
				if (!Double.isFinite(price)) {
					continue;
				}
				for (var t = start; t.isBefore(end); t = t.plusSeconds(QUARTER_SECONDS)) {
					final var previous = result.putIfAbsent(t, price);
					if (previous != null && Double.compare(previous, price) != 0) {
						conflicts.add(t);
					}
				}
			} catch (OpenemsNamedException e) {
				// skip invalid slot
			}
		}
		conflicts.forEach(result::remove);
		return result;
	}

	private static String string(JsonObject json, String member) {
		return getAsOptionalString(json, member).orElse("");
	}

	private static JsonArray array(JsonElement element, String member) {
		return getAsOptionalJsonArray(element, member).orElseGet(JsonArray::new);
	}
}
