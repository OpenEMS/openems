package io.openems.edge.timeofusetariff.entsoe.priceprovider;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ServiceScope;

import com.google.common.annotations.VisibleForTesting;

import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.time.periodic.PeriodicExecutorFactory;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.edge.common.component.ClockProvider;

@Component(service = EntsoeMarketPriceProviderPool.class, scope = ServiceScope.SINGLETON)
public class EntsoeMarketPriceProviderPoolImpl implements EntsoeMarketPriceProviderPool {
	@Reference
	private OpenemsEdgeOem oem;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL)
	private volatile ClockProvider clockProvider;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;

	@Reference
	private PeriodicExecutorFactory periodicExecutorFactory;

	protected final Map<EntsoeConfiguration, InitializedProvider> initializedProviders = new HashMap<>();

	public EntsoeMarketPriceProviderPoolImpl() {
	}

	@VisibleForTesting
	protected EntsoeMarketPriceProviderPoolImpl(OpenemsEdgeOem oem, ClockProvider clockProvider,
			BridgeHttpFactory httpBridgeFactory, PeriodicExecutorFactory periodicExecutorFactory) {
		this();
		this.oem = oem;
		this.clockProvider = clockProvider;
		this.httpBridgeFactory = httpBridgeFactory;
		this.periodicExecutorFactory = periodicExecutorFactory;
	}

	@Override
	public synchronized EntsoeMarketPriceProvider get(EntsoeConfiguration config) {
		var initializedProvider = this.initializedProviders.get(config);
		if (initializedProvider != null) {
			initializedProvider.usageCount++;
			return initializedProvider.provider;
		}

		var provider = this.createNewInstance(config);
		this.initializedProviders.put(config, new InitializedProvider(provider));
		return provider;
	}

	@Override
	public synchronized void unget(EntsoeMarketPriceProvider provider) {
		if (provider == null) {
			return;
		}

		var initializedProvider = this.initializedProviders.values().stream().filter(x -> x.provider == provider) //
				.findAny() //
				.orElse(null);
		if (initializedProvider == null) {
			return;
		}

		if (initializedProvider.usageCount <= 1) {
			initializedProvider.provider.deactivate();
			this.initializedProviders.remove(initializedProvider.provider.getConfig());
		} else {
			initializedProvider.usageCount--;
		}
	}

	private EntsoeMarketPriceProviderImpl createNewInstance(EntsoeConfiguration config) {
		var provider = new EntsoeMarketPriceProviderImpl(config, this.oem, this::getClock, this.httpBridgeFactory,
				this.periodicExecutorFactory);
		provider.activate();

		return provider;
	}

	private Clock getClock() {
		var clockProvider = this.clockProvider;
		if (clockProvider != null) {
			return clockProvider.getClock();
		}
		return Clock.systemDefaultZone();
	}

	@Deactivate
	protected void deactivate() {
		for (var initializedProvider : this.initializedProviders.values()) {
			initializedProvider.provider.deactivate();
		}
		this.initializedProviders.clear();
	}

	protected static class InitializedProvider {
		private final EntsoeMarketPriceProviderImpl provider;
		private int usageCount;

		public InitializedProvider(EntsoeMarketPriceProviderImpl provider) {
			this.provider = provider;
			this.usageCount = 1;
		}
	}
}
