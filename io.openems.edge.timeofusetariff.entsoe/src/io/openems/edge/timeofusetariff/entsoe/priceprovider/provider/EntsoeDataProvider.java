package io.openems.edge.timeofusetariff.entsoe.priceprovider.provider;

import io.openems.common.function.Disposable;
import io.openems.common.types.EntsoeBiddingZone;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.timeofusetariff.entsoe.priceprovider.MarketPriceUpdateEvent;

import java.time.Clock;
import java.util.function.Consumer;

public abstract class EntsoeDataProvider implements Disposable {
	protected final Clock clock;
	protected final EntsoeBiddingZone biddingZone;
	private final Consumer<MarketPriceUpdateEvent> onUpdate;

	protected EntsoeDataProvider(Clock clock, EntsoeBiddingZone biddingZone, Consumer<MarketPriceUpdateEvent> onUpdate) {
		this.clock = clock;
		this.biddingZone = biddingZone;
		this.onUpdate = onUpdate != null ? onUpdate : FunctionUtils::doNothing;
	}

	protected void triggerUpdate(MarketPriceUpdateEvent event) {
		this.onUpdate.accept(event);
	}

	/**
	 * Activates the data provider.
	 */
	public abstract void activate();

	/**
	 * Deactivates the data provider.
	 */
	public abstract void deactivate();
}
