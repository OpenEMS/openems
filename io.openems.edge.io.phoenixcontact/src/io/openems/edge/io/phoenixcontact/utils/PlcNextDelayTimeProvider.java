package io.openems.edge.io.phoenixcontact.utils;

import java.time.Duration;

import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProviderChain;
import io.openems.common.types.HttpStatus;

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
