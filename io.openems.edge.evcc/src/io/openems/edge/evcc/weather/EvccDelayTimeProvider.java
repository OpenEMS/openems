package io.openems.edge.evcc.weather;

import java.time.Clock;
import java.time.Duration;

import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.time.DelayTimeProvider;

public class EvccDelayTimeProvider implements DelayTimeProvider {

    @SuppressWarnings("unused")
	private final Clock clock;

    public EvccDelayTimeProvider(Clock clock) {
    	super();
        this.clock = clock;
    }

    @Override
    public Delay onFirstRunDelay() {
    	return Delay.immediate();
    }

    @Override
    public Delay onErrorRunDelay(HttpError error) {
        return Delay.of(Duration.ofMinutes(1));
    }

    @Override
    public Delay onSuccessRunDelay(io.openems.edge.bridge.http.api.HttpResponse<String> result) {
        return Delay.of(Duration.ofMinutes(15));
    }
}

