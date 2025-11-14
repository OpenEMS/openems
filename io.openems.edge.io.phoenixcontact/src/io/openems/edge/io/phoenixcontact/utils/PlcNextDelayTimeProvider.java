package io.openems.edge.io.phoenixcontact.utils;

import java.time.Duration;

import io.openems.common.types.HttpStatus;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.time.DelayTimeProvider;
import io.openems.edge.bridge.http.time.DelayTimeProviderChain;

public class PlcNextDelayTimeProvider implements DelayTimeProvider {

	private final int delayInSeconds;
	
	
	public PlcNextDelayTimeProvider(int delayInSeconds) {
		this.delayInSeconds = delayInSeconds;
	}

	@Override
	public Delay onFirstRunDelay() {
		return DelayTimeProviderChain.fixedDelay(Duration.ofSeconds(delayInSeconds)).getDelay();
	}

	@Override
	public Delay onErrorRunDelay(HttpError error) {
		return DelayTimeProviderChain.fixedDelay(Duration.ofSeconds(delayInSeconds)).getDelay();
	}

	@Override
	public Delay onSuccessRunDelay(HttpResponse<String> result) {
		if (result.status() != HttpStatus.OK) {
			return DelayTimeProviderChain.fixedDelay(Duration.ofSeconds(delayInSeconds)).getDelay();
		}
		return DelayTimeProviderChain.runNeverAgain().getDelay();
	}

}
