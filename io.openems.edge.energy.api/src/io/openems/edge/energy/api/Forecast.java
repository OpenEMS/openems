//package io.openems.edge.energy.api;
//
//import static io.openems.edge.energy.api.ExecutionPlan.NO_OF_PERIODS;
//
//import java.util.stream.IntStream;
//import java.util.stream.Stream;
//
///**
// * Holds fixed forecasted values for production, consumption and grid
// * cost/revenue per {@link Period}.
// */
//public class Forecast {
//
//	public static class Period {
//
//		public final Integer production; // [Wh]
//		public final Integer consumption; // [Wh]
//		public final Float buyFromGridCost; // [Ct/kWh]
//		public final Float sellToGridRevenue; // [Ct/kWh]
//
//		public Period(Integer production, Integer consumption, Float buyFromGridCost, Float sellToGridRevenue) {
//			this.production = production;
//			this.consumption = consumption;
//			this.buyFromGridCost = buyFromGridCost;
//			this.sellToGridRevenue = sellToGridRevenue;
//		}
//
//		/**
//		 * Get the Grid Cost for given energy.
//		 * 
//		 * @param energy the energy in [Wh]
//		 * @return the cost (positive) or revenue (negative)
//		 */
//		public Float getGridCost(Integer energy) {
//			if (energy == null) {
//				return null;
//
//			} else if (energy >= 0) {
//				// Buy-From-Grid
//				return energy * this.buyFromGridCost;
//
//			} else {
//				// Sell-To-Grid
//				return energy * this.sellToGridRevenue;
//			}
//		}
//	}
//
//	private final Period[] periods;
//
//	public Forecast(Integer[] productions, Integer[] consumptions, Float[] buyFromGridCosts,
//			Float[] sellToGridRevenue) {
//		this.periods = IntStream.range(0, NO_OF_PERIODS) //
//				.mapToObj(p -> new Period(Utils.orElse(productions, p, null), //
//						Utils.orElse(consumptions, p, null), //
//						Utils.orElse(buyFromGridCosts, p, null), //
//						Utils.orElse(sellToGridRevenue, p, null)) //
//				) //
//				.toArray(Period[]::new);
//	}
//
//	/**
//	 * Get a {@link Stream} of {@link Period}s.
//	 * 
//	 * @return stream
//	 */
//	public Stream<Period> periods() {
//		return Stream.of(this.periods);
//	}
//
//	/**
//	 * Get a {@link Period} by its index.
//	 * 
//	 * @param p the index
//	 * @return the {@link Period}
//	 * @throws IllegalArgumentException on invalid index
//	 */
//	public Period getPeriod(int p) throws IllegalArgumentException {
//		if (p > -1 && p < NO_OF_PERIODS) {
//			return this.periods[p];
//		}
//		throw new IllegalArgumentException("Period index [" + p + "] must be in interval [0," + NO_OF_PERIODS + "]");
//	}
//}
