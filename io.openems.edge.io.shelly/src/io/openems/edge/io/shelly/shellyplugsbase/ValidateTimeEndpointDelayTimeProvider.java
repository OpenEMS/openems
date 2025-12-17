package io.openems.edge.io.shelly.shellyplugsbase;

import java.time.Duration;

import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.time.DelayTimeProvider;

public class ValidateTimeEndpointDelayTimeProvider implements DelayTimeProvider {

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
