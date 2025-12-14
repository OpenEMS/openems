package io.openems.edge.battery.bmw.statemachine;

import java.time.Clock;
import java.util.Map;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.bmw.BatteryBmwImpl;
import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleService;
import io.openems.edge.bridge.modbus.api.BridgeModbusTcp;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<BatteryBmwImpl> {

	private static final String SPLIT_REGEX = "(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)";

	protected final Clock clock;
	protected final BridgeHttp httpBridge;
	protected final HttpBridgeCycleService cycleService;

	public Context(BatteryBmwImpl parent, Clock clock, BridgeHttp httpBridge, HttpBridgeCycleService cycleService) {
		super(parent);
		this.clock = clock;
		this.httpBridge = httpBridge;
		this.cycleService = cycleService;
	}

	protected void setComponent(Context context, String uri) throws OpenemsNamedException {
		final var battery = context.getParent();
		final var http = context.httpBridge;
		var map = Map.of(//
				"Authorization", "Bearer " + battery.getToken(), //
				"Content-Type", "application/json");
		final var id = battery.id().split(SPLIT_REGEX);
		final var url = battery.getUrl((BridgeModbusTcp) battery.getBridgeModbus(), uri, id[1]);
		var postData = "{data: {data: \"1\"}}";
		final var endPoint = new Endpoint(url, HttpMethod.POST, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
				BridgeHttp.DEFAULT_READ_TIMEOUT, postData, map);
		http.request(endPoint);
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
		final var endPoint = new Endpoint(url, HttpMethod.GET, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
				BridgeHttp.DEFAULT_READ_TIMEOUT, outdata, map);
		return endPoint;
	}
}