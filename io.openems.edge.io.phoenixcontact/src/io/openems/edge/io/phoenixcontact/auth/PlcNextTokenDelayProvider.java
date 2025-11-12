package io.openems.edge.io.phoenixcontact.auth;

import java.time.Duration;

import io.openems.common.types.HttpStatus;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.time.DelayTimeProvider;
import io.openems.edge.bridge.http.time.DelayTimeProviderChain;

public class PlcNextTokenDelayProvider implements DelayTimeProvider {

	private static final int TOKEN_FETCH_DELAY = 30;

	@Override
	public Delay onFirstRunDelay() {
		return DelayTimeProviderChain.fixedDelay(Duration.ofSeconds(TOKEN_FETCH_DELAY)).getDelay();
	}

	@Override
	public Delay onErrorRunDelay(HttpError error) {
		return DelayTimeProviderChain.fixedDelay(Duration.ofSeconds(TOKEN_FETCH_DELAY)).getDelay();
	}

	@Override
	public Delay onSuccessRunDelay(HttpResponse<String> result) {
		if (result.status() != HttpStatus.OK) {
			return DelayTimeProviderChain.fixedDelay(Duration.ofSeconds(TOKEN_FETCH_DELAY)).getDelay();
		}
		return DelayTimeProviderChain.runNeverAgain().getDelay();
	}

}
