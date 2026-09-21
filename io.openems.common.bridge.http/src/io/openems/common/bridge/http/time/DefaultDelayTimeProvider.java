package io.openems.common.bridge.http.time;

import java.util.function.Function;
import java.util.function.Supplier;

import io.openems.common.bridge.http.api.HttpError;

public class DefaultDelayTimeProvider<T> implements DelayTimeProvider<T> {

	private final Supplier<DelayTimeProvider.Delay> onFirstRunDelay;
	private final Function<HttpError, DelayTimeProvider.Delay> onErrorDelay;
	private final Function<T, Delay> onSuccessDelay;

	public DefaultDelayTimeProvider(//
			Supplier<Delay> onFirstRunDelay, //
			Function<HttpError, Delay> onErrorDelay, //
			Function<T, Delay> onSuccessDelay //
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
	public Delay onSuccessRunDelay(T result) {
		return this.onSuccessDelay.apply(result);
	}

}