package io.openems.edge.timeofusetariff.entsoe.priceprovider;

import io.openems.common.types.HttpStatus;
import io.openems.common.types.MarketPriceData;

public sealed interface MarketPriceUpdateEvent {
	record FailedWithException(Exception exception) implements MarketPriceUpdateEvent {
	}

	record FailedWithHttpError(HttpStatus responseStatusCode, String responseBody) implements MarketPriceUpdateEvent {
	}

	record Successful(MarketPriceData data) implements MarketPriceUpdateEvent {
	}
}
