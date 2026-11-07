package io.openems.common.bridge.http.thirdparty;

import static org.osgi.service.component.annotations.ServiceScope.PROTOTYPE;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.time.DefaultDelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProvider.Delay;
import io.openems.common.bridge.http.time.DelayTimeProviderChain;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.services.currency.CurrencyExchangeApi;
import io.openems.common.services.currency.ExchangeRate;
import io.openems.common.utils.BehaviorSubject;
import io.openems.common.xml.serialization.XmlParser;

@Component(scope = PROTOTYPE)
public class EcbCurrencyExchangeApi implements CurrencyExchangeApi {
	private static final Currency BASE_CURRENCY = Currency.getInstance("EUR");
	private static final String ECB_PRICES_FETCH_URL = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";

	private final Logger log = LoggerFactory.getLogger(EcbCurrencyExchangeApi.class);

	@Reference
	protected BridgeHttpFactory httpBridgeFactory;

	private BridgeHttp httpBridge;
	private HttpBridgeTimeService timeService;

	private final BehaviorSubject<List<ExchangeRate>> exchangeRates = new BehaviorSubject<>(new ArrayList<>());

	@Activate
	protected void activate() {
		this.httpBridge = this.httpBridgeFactory.get();
		this.timeService = this.httpBridge.createService(HttpBridgeTimeServiceDefinition.INSTANCE);

		final var delayProvider = new DefaultDelayTimeProvider<HttpResponse<String>>(//
				this::getInitialFetchDelay, //
				e -> Delay.of(Duration.ofMinutes(5)), //
				s -> Delay.of(Duration.ofHours(1)));
		this.timeService.subscribeTime(delayProvider, //
				this::createPriceFetchEndpoint, //
				this::handleEndpointResponse, //
				this::handleEndpointError);
	}

	@Deactivate
	protected void deactivate() {
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
		this.timeService = null;
	}

	@Override
	public BehaviorSubject<List<ExchangeRate>> getExchangeRates() {
		return this.exchangeRates;
	}

	protected DelayTimeProvider.Delay getInitialFetchDelay() {
		return DelayTimeProviderChain.immediate().plusRandomDelay(60, ChronoUnit.SECONDS).getDelay();
	}

	private BridgeHttp.Endpoint createPriceFetchEndpoint() {
		return BridgeHttp.create(ECB_PRICES_FETCH_URL).setMethod(HttpMethod.GET).build();
	}

	private void handleEndpointResponse(HttpResponse<String> response) throws OpenemsError.OpenemsNamedException {
		final var exchangeRates = this.extractRatesFromResponseData(response.data());
		this.exchangeRates.setValue(exchangeRates);
	}

	private List<ExchangeRate> extractRatesFromResponseData(String body) throws OpenemsError.OpenemsNamedException {
		final var xml = XmlParser.INSTANCE.parseXml(body);
		final var cube = xml.getChildObject("Cube");
		final var timeBasedCube = cube.getChildObjects("Cube").getFirst();
		final var currencyCubes = timeBasedCube.getChildObjects("Cube");

		var exchangeRates = new ArrayList<ExchangeRate>();
		for (var currencyCube : currencyCubes) {
			final var targetCurrency = Currency.getInstance(currencyCube.getAttribute("currency"));
			final var rate = Double.parseDouble(currencyCube.getAttribute("rate"));

			final var key = new ExchangeRate.ExchangeRateKey(BASE_CURRENCY, targetCurrency);
			exchangeRates.add(new ExchangeRate(key, rate));
		}

		return exchangeRates;
	}

	private void handleEndpointError(HttpError error) {
		if (error instanceof HttpError.ResponseError responseError) {
			this.log.error("Failed to fetch exchange rates from ECB. HTTP status code: {}, body: {}",
					responseError.status, responseError.body);
		} else {
			this.log.error("Failed to fetch exchange rates from ECB.", error);
		}
	}

}
