package io.openems.edge.core.services;

import static io.openems.common.utils.FunctionUtils.doNothing;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.jsonrpc.syncable.SyncableEntry;
import io.openems.common.jsonrpc.syncable.SyncableKey;
import io.openems.common.jsonrpc.syncable.types.SyncableExchangeRateData;
import io.openems.common.services.currency.AbstractCurrencyExchange;
import io.openems.common.services.currency.CurrencyExchange;
import io.openems.common.services.currency.CurrencyExchangeApi;
import io.openems.common.services.currency.ExchangeRate;
import io.openems.edge.common.services.syncable.SyncableRepository;
import io.openems.edge.common.services.syncable.SyncableSubscription;
import io.openems.edge.controller.api.backend.api.DataFetchType;
import io.openems.edge.controller.api.backend.services.BackendConnectionStateListener;

@Component(service = CurrencyExchange.class)
public class EdgeCurrencyExchange extends AbstractCurrencyExchange {
	@Reference
	protected BackendConnectionStateListener backendConnectionStateListener;

	@Reference
	protected SyncableRepository syncableRepository;

	private final Logger log = LoggerFactory.getLogger(EdgeCurrencyExchange.class);
	private final Consumer<DataFetchType> onFetchTypeChangeFunc = this::onFetchTypeChange;
	private final Consumer<SyncableEntry<SyncableExchangeRateData>> onBackendReceiveFunc = this::onBackendReceive;
	private final Consumer<List<ExchangeRate>> onLocallyReceiveFunc = this::onLocallyReceive;

	private DataFetchType currentDataFetchType = DataFetchType.WAITING;
	private SyncableSubscription<SyncableExchangeRateData> backendSubscription;
	private ServiceReference<CurrencyExchangeApi> apiReference;
	protected CurrencyExchangeApi api;
	private BundleContext bundleContext;

	@Activate
	protected void activate(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
		this.backendConnectionStateListener.getDataFetchType().subscribe(this.onFetchTypeChangeFunc);
	}

	@Deactivate
	protected void deactivate() {
		this.backendConnectionStateListener.getDataFetchType().unsubscribe(this.onFetchTypeChangeFunc);
		this.destroyBackend();
		this.destroyLocally();
	}

	protected void onBackendReceive(SyncableEntry<SyncableExchangeRateData> entry) {
		if (entry == null) {
			return;
		}

		this.setExchangeRates(Arrays.asList(entry.value().exchangeRates()));
	}

	protected void onLocallyReceive(List<ExchangeRate> rates) {
		this.setExchangeRates(rates);
	}

	protected synchronized void onFetchTypeChange(DataFetchType newFetchType) {
		switch (newFetchType) {
		case WAITING -> doNothing();
		case FETCH_LOCALLY -> {
			switch (this.currentDataFetchType) {
			case WAITING -> this.initLocally();
			case FETCH_LOCALLY -> doNothing();
			case FETCH_VIA_BACKEND -> this.switchFromBackendToLocally();
			}
			this.currentDataFetchType = newFetchType;
		}
		case FETCH_VIA_BACKEND -> {
			switch (this.currentDataFetchType) {
			case WAITING -> this.initBackend();
			case FETCH_LOCALLY -> this.switchFromLocallyToBackend();
			case FETCH_VIA_BACKEND -> doNothing();
			}
			this.currentDataFetchType = newFetchType;
		}
		}
	}

	protected void switchFromLocallyToBackend() {
		this.destroyLocally();
		this.initBackend();
	}

	protected void switchFromBackendToLocally() {
		this.destroyBackend();
		this.initLocally();
	}

	protected void initBackend() {
		this.log.info("Initialize currency exchange via backend.");

		this.backendSubscription = this.syncableRepository.subscribe(new SyncableKey.CurrencyExchangeRates());
		this.backendSubscription.getEntry().subscribe(this.onBackendReceiveFunc);
	}

	protected void destroyBackend() {
		if (this.backendSubscription != null) {
			this.backendSubscription.getEntry().unsubscribe(this.onBackendReceiveFunc);
			this.backendSubscription.dispose();
			this.backendSubscription = null;
		}
	}

	protected void initLocally() {
		this.log.info("Initialize local currency exchange.");

		this.loadApiReference();
		this.api.getExchangeRates().subscribe(this.onLocallyReceiveFunc);
	}

	protected void loadApiReference() {
		this.apiReference = this.bundleContext.getServiceReference(CurrencyExchangeApi.class);
		if (this.apiReference == null) {
			throw new IllegalStateException("Can't find CurrencyExchangeApi service");
		}

		this.api = this.bundleContext.getService(this.apiReference);
	}

	protected void destroyLocally() {
		if (this.api != null) {
			this.api.getExchangeRates().unsubscribe(this.onLocallyReceiveFunc);
			this.api = null;
		}

		if (this.apiReference != null) {
			this.bundleContext.ungetService(this.apiReference);
			this.apiReference = null;
		}
	}

}
