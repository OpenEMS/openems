package io.openems.edge.timeofusetariff.entsoe.priceprovider;

import java.time.Clock;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.time.periodic.PeriodicExecutorFactory;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.types.MarketPriceData;
import io.openems.common.utils.BehaviorSubject;
import io.openems.common.utils.StringUtils;
import io.openems.edge.timeofusetariff.entsoe.priceprovider.provider.EntsoeDataProvider;
import io.openems.edge.timeofusetariff.entsoe.priceprovider.provider.HttpEntsoeDataProvider;

public class EntsoeMarketPriceProviderImpl implements EntsoeMarketPriceProvider, MarketPriceProvider {
	private final Logger log = LoggerFactory.getLogger(EntsoeMarketPriceProviderImpl.class);

	private final EntsoeConfiguration config;
	private final BehaviorSubject<MarketPriceData> priceData = new BehaviorSubject<>(null);
	private final BehaviorSubject<MarketPriceUpdateEvent> updateState = new BehaviorSubject<>(null);

	private final OpenemsEdgeOem oem;
	private final Supplier<Clock> clockSupplier;
	private final BridgeHttpFactory httpBridgeFactory;

	private boolean activated = false;
	private EntsoeDataProvider dataProvider;
	private Object delayedDataProviderCheckObject;

	EntsoeMarketPriceProviderImpl(EntsoeConfiguration config, OpenemsEdgeOem oem, Supplier<Clock> clockSupplier,
			BridgeHttpFactory httpBridgeFactory, PeriodicExecutorFactory periodicExecutorFactory) {
		this.config = config;

		this.oem = oem;
		this.clockSupplier = clockSupplier;
		this.httpBridgeFactory = httpBridgeFactory;
	}

	synchronized void activate() {
		this.initializeDataProvider();
		this.checkForDataProviderDelayed();
		this.activated = true;
	}

	synchronized void deactivate() {
		this.activated = false;

		if (this.dataProvider != null) {
			this.dataProvider.deactivate();
			this.dataProvider.dispose();
			this.dataProvider = null;
		}
	}

	private void checkForDataProviderDelayed() {
		var checkObj = new Object();
		this.delayedDataProviderCheckObject = checkObj;

		CompletableFuture.delayedExecutor(1, TimeUnit.MINUTES).execute(() -> {
			if (this.delayedDataProviderCheckObject == checkObj) {
				this.delayedDataProviderCheckObject = null;
				if (this.dataProvider == null && this.activated) {
					this.log.error("Missing data provider. Is backend correctly loaded?");
				}
			}
		});
	}

	private void initializeDataProvider() {
		this.initializeHttpProvider();
	}

	private void initializeHttpProvider() {
		var apiKey = this.config.entsoeApiKey();
		if (StringUtils.isNullOrEmpty(apiKey)) {
			apiKey = this.oem.getEntsoeToken();
		}
		if (StringUtils.isNullOrEmpty(apiKey)) {
			throw new RuntimeException(
					"Can't fetch entso-e data. No security token is configured and no backend connection is available.");
		}

		this.log.info("Using http connection to fetch market price data from ENTSO-E API.");
		this.dataProvider = new HttpEntsoeDataProvider(this.httpBridgeFactory, apiKey, this.clockSupplier.get(),
				this.config.biddingZone(), this::onDataUpdate);
		this.dataProvider.activate();
	}

	private void onDataUpdate(MarketPriceUpdateEvent event) {
		switch (event) {
		case MarketPriceUpdateEvent.Successful(var data) -> this.setMarketPriceData(data);
		case MarketPriceUpdateEvent.FailedWithHttpError(var httpStatus, var httpBody) -> {
			this.log.warn("Unable to Update Entsoe Time-Of-Use Price (Status Code %d): %s".formatted(httpStatus.code(),
					httpBody));
		}
		case MarketPriceUpdateEvent.FailedWithException(var exception) -> {
			if (exception instanceof ExecutionException ex && ex.getCause() instanceof OpenemsNamedException oex) {
				this.log.error("Unable to Update Entsoe Time-Of-Use Price: " + oex.getMessage());
			} else {
				this.log.error("Unable to Update Entsoe Time-Of-Use Price", exception);
			}
		}
		}

		this.updateState.setValue(event);
	}

	private void setMarketPriceData(MarketPriceData marketPriceData) {
		this.log.info("Received new entsoe prices. Range: %s - %s".formatted(marketPriceData.getValues().getFirstTime(),
				marketPriceData.getValues().getEndTime()));

		this.priceData.setValue(marketPriceData);
	}

	@Override
	public EntsoeConfiguration getConfig() {
		return this.config;
	}

	@Override
	public void triggerPriceUpdate() {
		if (this.dataProvider != null) {
			this.log.info("Triggering price update ...");
			this.dataProvider.deactivate();
			this.dataProvider.activate();
		}
	}

	@Override
	public BehaviorSubject<MarketPriceData> getMarketPrices() {
		return this.priceData;
	}

	@Override
	public BehaviorSubject<MarketPriceUpdateEvent> getUpdateState() {
		return this.updateState;
	}
}
