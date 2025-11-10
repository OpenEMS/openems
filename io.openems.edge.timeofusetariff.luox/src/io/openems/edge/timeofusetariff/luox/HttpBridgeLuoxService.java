package io.openems.edge.timeofusetariff.luox;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpExecutor;
import io.openems.common.bridge.http.api.EndpointFetcher;
import io.openems.common.bridge.http.api.HttpBridgeService;
import io.openems.common.bridge.http.api.HttpBridgeServiceDefinition;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.api.UrlBuilder;
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProviderChain;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.types.HttpStatus;
import io.openems.edge.common.component.ComponentManager;

public class HttpBridgeLuoxService implements HttpBridgeService {

	@Component(service = HttpBridgeLuoxServiceDefinition.class)
	public static class HttpBridgeLuoxServiceDefinition implements HttpBridgeServiceDefinition<HttpBridgeLuoxService> {

		@Reference
		private ComponentManager componentManager;

		@Override
		public HttpBridgeLuoxService create(BridgeHttp bridgeHttp, BridgeHttpExecutor executor,
				EndpointFetcher endpointFetcher) {
			return new HttpBridgeLuoxService(bridgeHttp, this.componentManager);
		}
	}

	public static final String PROD_URL = "https://api.partner.lumenaza.de";

	private final Logger log = LoggerFactory.getLogger(HttpBridgeLuoxService.class);

	private final ComponentManager componentManager;
	private final BridgeHttp bridgeHttp;
	private final HttpBridgeTimeService timeService;

	public HttpBridgeLuoxService(BridgeHttp bridgeHttp, ComponentManager componentManager) {
		this.bridgeHttp = bridgeHttp;
		this.componentManager = componentManager;
		this.timeService = this.bridgeHttp.createService(HttpBridgeTimeServiceDefinition.INSTANCE);
	}

	private String getUrl() {
		return PROD_URL;
	}

	/**
	 * Queries prices for a contract.
	 *
	 * @param accessToken            the OAuth access token
	 * @param onAuthenticationFailed callback if authentication failed
	 * @param onServerError          callback if a servererror occurred
	 * @return the contract details
	 */
	public CompletableFuture<LuoxApi.ContractDetailsResponse> queryContractDetails(//
			final String accessToken, //
			final Runnable onAuthenticationFailed, //
			final Runnable onServerError //
	) {
		final var future = new CompletableFuture<LuoxApi.ContractDetailsResponse>();
		this.timeService.subscribeJsonTime(
				DelayTimeProvider.abortOnSuccess(DelayTimeProvider.Delay.of(Duration.ofMinutes(15))),
				BridgeHttp.create(UrlBuilder.parse(this.getUrl()) //
						.withPath("/customers/contract_details") //
						.toEncodedString()) //
						.setHeader("Authorization", "Bearer " + accessToken) //
						.build(),
				httpResponse -> {
					final var response = LuoxApi.ContractDetailsResponse.serializer().deserialize(httpResponse.data());
					future.complete(response);
				}, httpError -> {
					this.handleFailedRequest(httpError, onAuthenticationFailed, onServerError);
				});
		return future;
	}

	/**
	 * Subscribes to price updates for a contract. Prices are fetched every 24
	 * hours.
	 * 
	 * @param accessToken            the OAuth access token
	 * @param contractId             the contract id
	 * @param onAuthenticationFailed callback if authentication failed
	 * @param onServerError          callback if a servererror occurred
	 * @param onUpdatePrices         callback with the updated prices
	 */
	public void subscribeToPrices(//
			final String accessToken, //
			final String contractId, //
			final Runnable onAuthenticationFailed, //
			final Runnable onServerError, //
			final Consumer<LuoxApi.PricesResponse> onUpdatePrices //
	) {
		this.timeService.subscribeJsonTime(new FetchPricesDelayTimeProvider(this.componentManager.getClock()),
				this.createPricesEndpoint(accessToken, contractId), httpResponse -> {
					final var response = LuoxApi.PricesResponse.serializer().deserialize(httpResponse.data());
					this.log.info("Fetched prices: {}", response);

					// Parse the response for the prices
					onUpdatePrices.accept(response);
				}, //
				httpError -> {
					this.handleFailedRequest(httpError, onAuthenticationFailed, onServerError);
				});
	}

	private void handleFailedRequest(//
			Throwable throwable, //
			Runnable onAuthenticationFailed, //
			Runnable onServerError //
	) {
		this.log.error("Request failed", throwable);

		if (throwable instanceof CompletionException completionException) {
			throwable = completionException.getCause();
		}

		switch (throwable) {
		case HttpError.ResponseError responseError -> {
			if (responseError.status.code() == HttpStatus.UNAUTHORIZED.code()) {
				onAuthenticationFailed.run();
			}
			if (responseError.status.isServerError()) {
				onServerError.run();
			}
		}
		default -> {

		}
		}
	}

	private BridgeHttp.Endpoint createPricesEndpoint(String accessToken, String contractId) {
		return BridgeHttp //
				.create(UrlBuilder.parse(this.getUrl()) //
						.withPath("/contracts/" + contractId + "/prices") //
						.toEncodedString()) //
				.setHeader("Authorization", "Bearer " + accessToken) //
				.build();

	}

	@Override
	public void close() throws Exception {

	}

	private record FetchPricesDelayTimeProvider(Clock clock) implements DelayTimeProvider {

		@Override
		public Delay onFirstRunDelay() {
			return Delay.immediate();
		}

		@Override
		public Delay onSuccessRunDelay(HttpResponse<String> result) {
			return DelayTimeProviderChain.fixedAtEveryFull(this.clock, DurationUnit.ofHours(24)) //
					.plusFixedAmount(Duration.ofHours(16)) //
					.plusRandomDelay(60, ChronoUnit.SECONDS) //
					.getDelay();
		}

		@Override
		public Delay onErrorRunDelay(HttpError error) {
			return switch (error) {
			case HttpError.ResponseError responseError -> {
				if (responseError.status.code() == HttpStatus.UNAUTHORIZED.code()) {
					yield Delay.infinite();
				}
				yield Delay.of(Duration.ofMinutes(10));
			}
			case HttpError.UnknownError unknownError -> {
				yield Delay.of(Duration.ofMinutes(10));
			}
			};
		}

	}
}
