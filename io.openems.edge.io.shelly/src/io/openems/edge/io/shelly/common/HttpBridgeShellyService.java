package io.openems.edge.io.shelly.common;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.osgi.service.component.annotations.Component;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpExecutor;
import io.openems.common.bridge.http.api.EndpointFetcher;
import io.openems.common.bridge.http.api.HttpBridgeService;
import io.openems.common.bridge.http.api.HttpBridgeServiceDefinition;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.types.Result;
import io.openems.edge.io.shelly.common.gen2.Gen2RpcDeviceInfo;

public class HttpBridgeShellyService implements HttpBridgeService {

	@Component(service = HttpBridgeShellyServiceDefinition.class)
	public static class HttpBridgeShellyServiceDefinition
			implements HttpBridgeServiceDefinition<HttpBridgeShellyService> {

		@Override
		public HttpBridgeShellyService create(BridgeHttp bridgeHttp, BridgeHttpExecutor executor,
				EndpointFetcher endpointFetcher) {
			return new HttpBridgeShellyService(bridgeHttp);
		}
	}

	private final BridgeHttp httpBridge;
	private final HttpBridgeTimeService timeService;

	private String baseUrl;

	public HttpBridgeShellyService(BridgeHttp httpBridge) {
		this.httpBridge = httpBridge;
		this.timeService = httpBridge.createService(HttpBridgeTimeServiceDefinition.INSTANCE);
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	/**
	 * Subscribes to receive shelly gen 2 device information every 15 minutes.
	 *
	 * @param onResult Called with device info result
	 */
	public void subscribeToGen2DeviceInfo(Consumer<Result<Gen2RpcDeviceInfo>> onResult) {
		this.timeService.subscribeJsonTime(new ShellyDeviceInfoEndpointDelayTimeProvider(),
				BridgeHttp.create(this.baseUrl + "/rpc/Shelly.GetDeviceInfo").build(), httpResponse -> {
					final var deviceInfo = Gen2RpcDeviceInfo.serializer().deserialize(httpResponse.data());
					onResult.accept(Result.ok(deviceInfo));
				}, httpError -> {
					onResult.accept(Result.error(httpError));
				});
	}

	public CompletableFuture<HttpResponse<String>> setSwitchStatus(int index, boolean on) {
		final String url = this.baseUrl + "/rpc/Switch.Set?id=" + index + "&on=" + (on ? "true" : "false");
		return this.httpBridge.get(url);
	}

	@Override
	public void close() throws Exception {
		this.timeService.removeAllTimeEndpoints();
	}

	static class ShellyDeviceInfoEndpointDelayTimeProvider implements DelayTimeProvider {

		@Override
		public Delay onFirstRunDelay() {
			return Delay.immediate();
		}

		@Override
		public Delay onErrorRunDelay(HttpError error) {
			return Delay.of(Duration.ofSeconds(1));
		}

		@Override
		public Delay onSuccessRunDelay(HttpResponse<String> result) {
			return Delay.of(Duration.ofMinutes(15));
		}

	}

}
