package io.openems.edge.battery.bmw.statemachine;

import static io.openems.common.bridge.http.api.BridgeHttp.DEFAULT_CONNECT_TIMEOUT;
import static io.openems.common.bridge.http.api.BridgeHttp.DEFAULT_READ_TIMEOUT;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.battery.bmw.BatteryBmwImpl;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleService;
import io.openems.edge.bridge.modbus.api.BridgeModbusTcp;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<BatteryBmwImpl> {

	private static final String SPLIT_REGEX = "(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)";

	protected final Clock clock;
	protected final BridgeHttp httpBridge;
	protected final HttpBridgeCycleService cycleService;

	private final Logger log = LoggerFactory.getLogger(Context.class);

	public Context(BatteryBmwImpl parent, Clock clock, BridgeHttp httpBridge, HttpBridgeCycleService cycleService) {
		super(parent);
		this.clock = clock;
		this.httpBridge = httpBridge;
		this.cycleService = cycleService;
	}

	protected void setComponent(Context context, String uri, String value) throws OpenemsNamedException {
		final var battery = context.getParent();
		final var http = context.httpBridge;

		final var id = battery.id().split(SPLIT_REGEX);
		final var url = battery.getUrl((BridgeModbusTcp) battery.getBridgeModbus(), uri, id[1]);

		final var postData = JsonUtils.buildJsonObject() //
				.add("data", JsonUtils.buildJsonObject() //
						.addProperty("data", value).build())
				.build();

		final var endPoint = BridgeHttp.create(url) //
				.setBodyJson(postData)//
				.build();

		try {
			CompletableFuture<HttpResponse<String>> futureResponse = http.request(endPoint);

			futureResponse.thenAccept(response -> {
				this.log.debug("Request succeeded for URI: {}", uri);
			}).exceptionally(throwable -> {
				this.log.error("Request failed for URI: {} - Error: {}", uri, throwable.getMessage());
				return null;
			});

		} catch (Exception e) {
			this.log.error("POST request failed for URI: {} - Error: {}", uri, e.getMessage());
			throw e;
		}
	}

	/**
	 * Gets the endpoint.
	 * 
	 * @param uri     the uri state or release
	 * @param outdata write data
	 * @return {@link Endpoint}
	 */
	public Endpoint getEndpoint(String uri, String outdata) {
		final var battery = this.getParent();
		var map = Map.of(//
				"Authorization", "Bearer " + battery.getToken(), //
				"Content-Type", "application/json");
		final var id = battery.id().split(SPLIT_REGEX);
		final var url = battery.getUrl((BridgeModbusTcp) battery.getBridgeModbus(), uri, id[1]);
		final var endPoint = new Endpoint(url, HttpMethod.GET, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT, outdata,
				map);
		return endPoint;
	}
}