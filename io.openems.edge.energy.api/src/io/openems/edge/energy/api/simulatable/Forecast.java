package io.openems.edge.energy.api.simulatable;

import static io.openems.edge.energy.api.simulatable.ExecutionPlan.NO_OF_PERIODS;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.openems.edge.energy.api.Utils;

/**
 * Holds fixed forecasted values for production, consumption and grid
 * cost/revenue per {@link Period}.
 */
public class Forecast {

	public static class Period {

		public final Integer production; // [Wh]
		public final Integer consumption; // [Wh]
		public final Float buyFromGridCost; // [Ct/kWh]
		public final Float sellToGridRevenue; // [Ct/kWh]

		public Period(Integer production, Integer consumption, Float buyFromGridCost, Float sellToGridRevenue) {
			this.production = production;
			this.consumption = consumption;
			this.buyFromGridCost = buyFromGridCost;
			this.sellToGridRevenue = sellToGridRevenue;
		}

		public Float getGridCost(Integer energy) {
			if (energy == null) {
				return null;

			} else if (energy >= 0) {
				// Buy-From-Grid
				return energy * this.buyFromGridCost;

			} else {
				// Sell-To-Grid
				return energy * this.sellToGridRevenue;
			}
		}
	}

	private final Period[] periods;

	public Forecast(Integer[] productions, Integer[] consumptions, Float[] buyFromGridCosts,
			Float[] sellToGridRevenue) {
		this.periods = IntStream.range(0, NO_OF_PERIODS) //
				.mapToObj(p -> new Period(Utils.orElse(productions, p, null), //
						Utils.orElse(consumptions, p, null), //
						Utils.orElse(buyFromGridCosts, p, null), //
						Utils.orElse(sellToGridRevenue, p, null)) //
				) //
				.toArray(Period[]::new);
	}

	public Stream<Period> periods() {
		return Stream.of(this.periods);
	}

	public Period getPeriod(int p) throws IllegalArgumentException {
		if (p < NO_OF_PERIODS) {
			return this.periods[p];
		}
		throw new IllegalArgumentException("Period index [" + p + "] must be smaller than [" + NO_OF_PERIODS + "]");
	}
}
