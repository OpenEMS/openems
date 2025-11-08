package io.openems.edge.battery.bmw;

import java.time.Duration;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProviderChain;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.types.HttpStatus;

@Component(scope = ServiceScope.SINGLETON, service = BmwToken.class)
public class BmwToken {
	private static final int FETCH_TOKEN_DELAY = 30;

	private String token;
	private BridgeHttp http;
	private HttpBridgeTimeService timeService;

	@Activate
	public BmwToken(@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED) BridgeHttp http) {
		this.http = http;
		this.timeService = http.createService(HttpBridgeTimeServiceDefinition.INSTANCE);
	}

	/**
	 * Fetch the token.
	 * 
	 * @param endpoint the endpoint
	 */
	public synchronized void fetchToken(Endpoint endpoint) {
		// TODO check if the token is expires
		this.timeService.subscribeJsonTime(new BmwTokenDelayProvider(), endpoint, (a, b) -> {
			if (a == null) {
				return;
			}
			var jsonObject = a.data().getAsJsonObject();
			this.token = jsonObject.getAsJsonPrimitive("jwtToken").getAsString();
		});
	}

	public String getToken() {
		return this.token;
	}

	private static final class BmwTokenDelayProvider implements DelayTimeProvider {

		@Override
		public Delay onFirstRunDelay() {
			return DelayTimeProviderChain.fixedDelay(Duration.ofSeconds(FETCH_TOKEN_DELAY)).getDelay();
		}

		@Override
		public Delay onErrorRunDelay(HttpError error) {
			return DelayTimeProviderChain.fixedDelay(Duration.ofSeconds(FETCH_TOKEN_DELAY)).getDelay();
		}

		@Override
		public Delay onSuccessRunDelay(HttpResponse<String> result) {
			if (result.status() != HttpStatus.OK) {
				return DelayTimeProviderChain.fixedDelay(Duration.ofSeconds(FETCH_TOKEN_DELAY)).getDelay();
			}
			return DelayTimeProviderChain.runNeverAgain().getDelay();
		}
	}
}