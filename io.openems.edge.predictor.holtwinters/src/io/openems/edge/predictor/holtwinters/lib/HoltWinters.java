package io.openems.edge.predictor.holtwinters.lib;

/**
 * Copyright 2011 Nishant Chandra <nishant.chandra at gmail dot com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Given a time series, say a complete monthly data for 12 months, the
 * Holt-Winters smoothing and forecasting technique is built on the following
 * formulae (multiplicative version):
 *
 * St[i] = alpha * y[i] / It[i - period] + (1.0 - alpha) * (St[i - 1] + Bt[i -
 * 1]) Bt[i] = gamma * (St[i] - St[i - 1]) + (1 - gamma) * Bt[i - 1] It[i] =
 * beta * y[i] / St[i] + (1.0 - beta) * It[i - period] Ft[i + m] = (St[i] + (m *
 * Bt[i])) * It[i - period + m]
 *
 * Note: Many authors suggest calculating initial values of St, Bt and It in a
 * variety of ways, but some of them are incorrect e.g. determination of It
 * parameter using regression. This implementation uses the NIST recommended
 * methods.
 *
 * For more details, see: http://adorio-research.org/wordpress/?p=1230
 * http://www.itl.nist.gov/div898/handbook/pmc/section4/pmc435.htm
 *
 * @author Nishant Chandra
 *
 */
public class HoltWinters {

	/**
	 * This method is the entry point. It calculates the initial values and returns
	 * the forecast for the future m periods.
	 *
	 * @param y       - Time series data.
	 * @param alpha   - Exponential smoothing coefficients for level, trend,
	 *                seasonal components.
	 * @param beta    - Exponential smoothing coefficients for level, trend,
	 *                seasonal components.
	 * @param gamma   - Exponential smoothing coefficients for level, trend,
	 *                seasonal components.
	 * @param perdiod - A complete season's data consists of L periods. And we need
	 *                to estimate the trend factor from one period to the next. To
	 *                accomplish this, it is advisable to use two complete seasons;
	 *                that is, 2L periods.
	 * @param m       - Extrapolated future data points. - 4 quarterly, - 7 weekly,
	 *                - 12 monthly
	 *
	 * @param debug   - Print debug values. Useful for testing.
	 *
	 */
	public static double[] forecast(long[] y, double alpha, double beta, double gamma, int period, int m,
			boolean debug) {

		validateArguments(y, alpha, beta, gamma, period, m);

		int seasons = y.length / period;
		double a0 = calculateInitialLevel(y);
		double b0 = calculateInitialTrend(y, period);
		double[] initialSeasonalIndices = calculateSeasonalIndices(y, period, seasons);

		if (debug) {
			System.out.println(
					String.format("Total observations: %d, Seasons %d, Periods %d", y.length, seasons, period));
			System.out.println("Initial level value a0: " + a0);
			System.out.println("Initial trend value b0: " + b0);
			printArray("Seasonal Indices: ", initialSeasonalIndices);
		}

		double[] forecast = calculateHoltWinters(y, a0, b0, alpha, beta, gamma, initialSeasonalIndices, period, m,
				debug);

		if (debug) {
			printArray("Forecast", forecast);
		}

		return forecast;
	}

	public static double[] forecast(long[] y, double alpha, double beta, double gamma, int period, int m) {
		return forecast(y, alpha, beta, gamma, period, m, false);
	}

	/**
	 * Validate input.
	 *
	 * @param y
	 * @param alpha
	 * @param beta
	 * @param gamma
	 * @param m
	 */
	private static void validateArguments(long[] y, double alpha, double beta, double gamma, int period, int m) {
		if (y == null) {
			throw new IllegalArgumentException("Value of y should be not null");
		}

		if (m <= 0) {
			throw new IllegalArgumentException("Value of m must be greater than 0.");
		}

		if (m > period) {
			throw new IllegalArgumentException("Value of m must be <= period.");
		}

		if ((alpha < 0.0) || (alpha > 1.0)) {
			throw new IllegalArgumentException("Value of Alpha should satisfy 0.0 <= alpha <= 1.0");
		}

		if ((beta < 0.0) || (beta > 1.0)) {
			throw new IllegalArgumentException("Value of Beta should satisfy 0.0 <= beta <= 1.0");
		}

		if ((gamma < 0.0) || (gamma > 1.0)) {
			throw new IllegalArgumentException("Value of Gamma should satisfy 0.0 <= gamma <= 1.0");
		}
	}

	/**
	 * This method realizes the Holt-Winters equations.
	 *
	 * @param y
	 * @param a0
	 * @param b0
	 * @param alpha
	 * @param beta
	 * @param gamma
	 * @param initialSeasonalIndices
	 * @param period
	 * @param m
	 * @param debug
	 * @return - Forecast for m periods.
	 */
	private static double[] calculateHoltWinters(long[] y, double a0, double b0, double alpha, double beta,
			double gamma, double[] initialSeasonalIndices, int period, int m, boolean debug) {

		double[] St = new double[y.length];
		double[] Bt = new double[y.length];
		double[] It = new double[y.length];
		double[] Ft = new double[y.length + m];

		// Initialize base values
		St[1] = a0;
		Bt[1] = b0;

		for (int i = 0; i < period; i++) {
			It[i] = initialSeasonalIndices[i];
		}

		// Start calculations
		for (int i = 2; i < y.length; i++) {

			// Calculate overall smoothing
			if ((i - period) >= 0) {
				St[i] = alpha * y[i] / It[i - period] + (1.0 - alpha) * (St[i - 1] + Bt[i - 1]);
			} else {
				St[i] = alpha * y[i] + (1.0 - alpha) * (St[i - 1] + Bt[i - 1]);
			}

			// Calculate trend smoothing
			Bt[i] = gamma * (St[i] - St[i - 1]) + (1 - gamma) * Bt[i - 1];

			// Calculate seasonal smoothing
			if ((i - period) >= 0) {
				It[i] = beta * y[i] / St[i] + (1.0 - beta) * It[i - period];
			}

			// Calculate forecast
			if (((i + m) >= period)) {
				Ft[i + m] = (St[i] + (m * Bt[i])) * It[i - period + m];
			}

			if (debug) {
				System.out.println(String.format("i = %d, y = %d, S = %f, Bt = %f, It = %f, F = %f", i, y[i], St[i],
						Bt[i], It[i], Ft[i]));
			}
		}

		return Ft;
	}

	/**
	 * See: http://robjhyndman.com/researchtips/hw-initialization/ 1st period's
	 * average can be taken. But y[0] works better.
	 *
	 * @return - Initial Level value i.e. St[1]
	 */
	private static double calculateInitialLevel(long[] y) {
		return y[0];
	}

	/**
	 * See: http://www.itl.nist.gov/div898/handbook/pmc/section4/pmc435.htm
	 *
	 * @return - Initial trend - Bt[1]
	 */
	private static double calculateInitialTrend(long[] y, int period) {

		double sum = 0;

		for (int i = 0; i < period; i++) {
			sum += (y[period + i] - y[i]);
		}

		return sum / (period * period);
	}

	/**
	 * See: http://www.itl.nist.gov/div898/handbook/pmc/section4/pmc435.htm
	 *
	 * @return - Seasonal Indices.
	 */
	private static double[] calculateSeasonalIndices(long[] y, int period, int seasons) {

		double[] seasonalAverage = new double[seasons];
		double[] seasonalIndices = new double[period];

		double[] averagedObservations = new double[y.length];

		for (int i = 0; i < seasons; i++) {
			for (int j = 0; j < period; j++) {
				seasonalAverage[i] += y[(i * period) + j];
			}
			seasonalAverage[i] /= period;
		}

		for (int i = 0; i < seasons; i++) {
			for (int j = 0; j < period; j++) {
				averagedObservations[(i * period) + j] = y[(i * period) + j] / seasonalAverage[i];
			}
		}

		for (int i = 0; i < period; i++) {
			for (int j = 0; j < seasons; j++) {
				seasonalIndices[i] += averagedObservations[(j * period) + i];
			}
			seasonalIndices[i] /= seasons;
		}

		return seasonalIndices;
	}

	/**
	 * Utility method to print array values.
	 *
	 * @param description
	 * @param data
	 */
	private static void printArray(String description, double[] data) {
		System.out.println(description);
		for (int i = 0; i < data.length; i++) {
			System.out.println(data[i]);
		}
	}
}
