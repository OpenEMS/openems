package io.openems.edge.braiinsos.api;

import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpHeader;
import io.openems.common.bridge.http.api.HttpMediaType;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.authentication.HttpBridgeAuthenticationService;
import io.openems.common.bridge.http.authentication.HttpBridgeAuthenticationServiceDefinition;
import io.openems.common.bridge.http.time.DefaultDelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProvider.Delay;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.bridge.http.time.HttpBridgeTimeService.TimeEndpoint;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.HttpStatus;

public class BraiinsApi {

	private static final Delay POLLING_DELAY = Delay.of(Duration.ofSeconds(5));

	private final Logger log = LoggerFactory.getLogger(BraiinsApi.class);

	private final BridgeHttpFactory httpBridgeFactory;
	private final BridgeHttp httpBridge;
	private final HttpBridgeAuthenticationService<HttpHeader> authenticationService;
	private final HttpBridgeTimeService timeService;

	private final String baseUrl;
	private final String username;
	private final String password;

	private final Consumer<MinerStats> minerStatsCallback;
	private TimeEndpoint pollingEndpoint;

	public BraiinsApi(BridgeHttpFactory httpBridgeFactory, String ip, String username, String password,
			Consumer<MinerStats> minerStatsCallback) {
		this.httpBridgeFactory = httpBridgeFactory;
		this.httpBridge = httpBridgeFactory.get();
		this.authenticationService = this.httpBridge.createService(HttpBridgeAuthenticationServiceDefinition
				.of(() -> this.getToken().thenApply(HttpHeader::authorization)));
		this.timeService = this.authenticationService.createService(HttpBridgeTimeServiceDefinition.INSTANCE);
		this.baseUrl = "http://" + ip + "/api/v1";
		this.username = username;
		this.password = password;
		this.minerStatsCallback = minerStatsCallback;
	}

	/**
	 * Activates the {@link BraiinsApi}.
	 */
	public void activate() {
		if (this.pollingEndpoint != null) {
			return;
		}

		this.pollingEndpoint = this.timeService.subscribeTime(
				new DefaultDelayTimeProvider(Delay::immediate, error -> POLLING_DELAY, response -> POLLING_DELAY),
				this.createEndpoint(HttpMethod.GET, "/miner/stats", null), //
				this::handleMinerStatsResponse, //
				this::handleMinerStatsError);
	}

	/**
	 * Deactivate the {@link BraiinsApi}.
	 */
	public void deactivate() {
		if (this.pollingEndpoint != null) {
			this.timeService.removeTimeEndpoint(this.pollingEndpoint);
			this.pollingEndpoint = null;
		}
		this.httpBridgeFactory.unget(this.httpBridge);
	}

	private Endpoint createEndpoint(HttpMethod method, String url, String body) {
		return BridgeHttp.create(this.baseUrl + url) //
				.setHeader(HttpHeader.contentType(HttpMediaType.Application.JSON)) //
				.onlyIf(body != null, b -> b //
						.setBody(body))
				.setMethod(method) //
				.build();
	}

	protected CompletableFuture<String> getToken() {
		return this.httpBridge//
				.requestJson(this.createEndpoint(HttpMethod.POST, "/auth/login", //
						buildJsonObject() //
								.addProperty("username", this.username) //
								.addProperty("password", this.password) //
								.build().toString())) //
				.thenApply(msg -> AuthLogin.serializer() //
						.deserialize(msg.data())//
						.token());
	}

	/**
	 * Resumes mining.
	 * 
	 * @return {@link CompletableFuture}
	 */
	public CompletableFuture<Void> callActionResume() {
		return this.callAction("/actions/resume");
	}

	/**
	 * Pauses mining.
	 * 
	 * @return {@link CompletableFuture}
	 */
	public CompletableFuture<Void> callActionPause() {
		return this.callAction("/actions/pause");
	}

	private CompletableFuture<Void> callAction(String url) {
		return this.authenticationService //
				.request(this.createEndpoint(HttpMethod.PUT, url, null)) //
				.thenApply(msg -> null);
	}

	private void handleMinerStatsResponse(HttpResponse<String> response) throws OpenemsNamedException {
		this.minerStatsCallback.accept(MinerStats.serializer().deserialize(response.data()));
	}

	private void handleMinerStatsError(HttpError error) {
		if (isStoppedMinerError(error)) {
			this.minerStatsCallback.accept(new MinerStats(0., 0, 0.));
			return;
		}

		this.log.warn("Unable to fetch miner stats", error);
		this.minerStatsCallback.accept(null);
	}

	private static boolean isStoppedMinerError(Throwable throwable) {
		var cause = throwable;
		while (cause instanceof CompletionException || cause instanceof HttpError.UnknownError) {
			cause = cause.getCause();
			if (cause == null) {
				return false;
			}
		}

		return cause instanceof HttpError.ResponseError re && re.status == HttpStatus.INTERNAL_SERVER_ERROR;
	}
}
