package io.openems.edge.energy.dummy.forecast;

import io.openems.edge.energy.api.simulatable.Forecast;

public class DummyForecast {
	/**
	 * Builds a typical {@link Forecast} for Autumn season with 24 periods.
	 * 
	 * <p>
	 * Prices: Tibber at 28.12.2022; €Ct*100
	 * 
	 * @return {@link Forecast}
	 */
	public static Forecast autumn24() {
		return new Forecast(
				// Production
				new Integer[] { 2, 2, 2, 1, 1, 2, 2, 22, 827, 3693, 4488, 5963, 9366, 9653, 8181, 6413, 5624, 2609, 368,
						8, 1, 1, 1, 1 },
				// Consumption
				new Integer[] { 757, 280, 322, 316, 281, 231, 5068, 4268, 2316, 347, 870, 1819, 225, 204, 7035, 5781,
						1341, 380, 2306, 927, 309, 2426, 265, 1439 },
				//
				new Float[] { 18F, 17F, 14F, 13F, 13F, 13F, 15F, 17F, 18F, 20F, 21F, 20F, 19F, 19F, 21F, 21F, 23F, 24F,
						23F, 21F, 19F, 18F, 17F, 15F },
				//
				new Float[] { 11F, 11F, 11F, 11F, 11F, 11F, 11F, 11F, 11F, 11F, 11F, 11F, 11F, 11F, 11F, 11F, 11F, 11F,
						11F, 11F, 11F, 11F, 11F, 11F } //
		);
	}

}