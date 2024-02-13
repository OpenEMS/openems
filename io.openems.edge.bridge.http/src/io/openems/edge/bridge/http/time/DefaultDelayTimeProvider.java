package io.openems.edge.bridge.http.time;

public class DefaultDelayTimeProvider implements DelayTimeProvider {

	private final DelayTimeProviderChain firstRunDelay;
	private final DelayTimeProviderChain onErrorDelay;
	private final DelayTimeProviderChain onSuccessDelay;

	public DefaultDelayTimeProvider(//
			DelayTimeProviderChain firstRunDelay, //
			DelayTimeProviderChain onErrorDelay, //
			DelayTimeProviderChain onSuccessDelay //
	) {
		this.firstRunDelay = firstRunDelay == null ? onSuccessDelay : firstRunDelay;
		this.onErrorDelay = onErrorDelay == null ? onSuccessDelay : onErrorDelay;
		this.onSuccessDelay = onSuccessDelay;
	}

	@Override
	public Delay nextRun(boolean firstRun, boolean lastRunSuccessful) {
		if (firstRun) {
			return this.firstRunDelay.getDelay();
		}

		if (!lastRunSuccessful) {
			return this.onErrorDelay.getDelay();
		}

		return this.onSuccessDelay.getDelay();
	}

}