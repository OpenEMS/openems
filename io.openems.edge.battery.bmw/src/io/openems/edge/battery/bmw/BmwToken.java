package io.openems.edge.battery.bmw;

import java.time.Duration;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.types.HttpStatus;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.time.DelayTimeProvider;
import io.openems.edge.bridge.http.time.DelayTimeProviderChain;

@Component(scope = ServiceScope.SINGLETON, service = BmwToken.class)
public class BmwToken {
	private static final int FETCH_TOKEN_DELAY = 30;

	private String token;
	private BridgeHttp http;

	@Activate
	public BmwToken(@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED) BridgeHttp http) {
		this.http = http;
	}

	/**
	 * Fetch the token.
	 * 
	 * @param endpoint the endpoint
	 */
	public synchronized void fetchToken(Endpoint endpoint) {
		// TODO check if the token is expires
		this.http.subscribeJsonTime(new BmwTokenDelayProvider(), endpoint, (a, b) -> {
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