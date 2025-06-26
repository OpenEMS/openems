package io.openems.edge.bridge.http.time;

import java.util.function.Function;
import java.util.function.Supplier;

import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;

public class DefaultDelayTimeProvider implements DelayTimeProvider {

	private final Supplier<Delay> onFirstRunDelay;
	private final Function<HttpError, Delay> onErrorDelay;
	private final Function<HttpResponse<String>, Delay> onSuccessDelay;

	public DefaultDelayTimeProvider(//
			Supplier<Delay> onFirstRunDelay, //
			Function<HttpError, Delay> onErrorDelay, //
			Function<HttpResponse<String>, Delay> onSuccessDelay //
	) {
		this.onFirstRunDelay = onFirstRunDelay;
		this.onErrorDelay = onErrorDelay;
		this.onSuccessDelay = onSuccessDelay;
	}

	@Override
	public Delay onFirstRunDelay() {
		return this.onFirstRunDelay.get();
	}

	@Override
	public Delay onErrorRunDelay(HttpError error) {
		return this.onErrorDelay.apply(error);
	}

	@Override
	public Delay onSuccessRunDelay(HttpResponse<String> result) {
		return this.onSuccessDelay.apply(result);
	}

}