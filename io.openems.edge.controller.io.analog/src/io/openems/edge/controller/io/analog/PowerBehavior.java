package io.openems.edge.controller.io.analog;

import java.util.function.BiFunction;

public enum PowerBehavior {
	LINEAR("Linear", new CalculateLinearFactor(), new CalculateLinearPower()), //
	NON_LINEAR("Nonlinear", new CalculateNonLinearFactor(), new CalculateNonLinearPower());

	public final String name;
	public final BiFunction<Integer, Integer, Float> calculateFactorFromPower;
	public final BiFunction<Integer, Float, Integer> calculatePowerFromFactor;

	private PowerBehavior(String name, BiFunction<Integer, Integer, Float> calculateFactorFromPower,
			BiFunction<Integer, Float, Integer> calculatePowerFromFactor) {
		this.name = name;
		this.calculateFactorFromPower = calculateFactorFromPower;
		this.calculatePowerFromFactor = calculatePowerFromFactor;
	}

	private static class CalculateLinearFactor implements BiFunction<Integer, Integer, Float> {

		@Override
		public Float apply(Integer maximumPower, Integer power) {
			if (maximumPower == null || power == null) {
				return null;
			}

			return power / maximumPower.floatValue();
		}
	}

	private static class CalculateNonLinearFactor implements BiFunction<Integer, Integer, Float> {

		@Override
		public Float apply(Integer maximumPower, Integer power) {
			if (maximumPower == null || power == null) {
				return null;
			}

			var linearFactor = power / maximumPower.floatValue();
			return (float) (Math.acos(1 - (2 * linearFactor)) / Math.PI);
		}
	}

	private static class CalculateLinearPower implements BiFunction<Integer, Float, Integer> {

		@Override
		public Integer apply(Integer maximumPower, Float factor) {
			if (maximumPower == null || factor == null) {
				return null;
			}

			return Math.round(maximumPower * factor);
		}
	}

	private static class CalculateNonLinearPower implements BiFunction<Integer, Float, Integer> {

		@Override
		public Integer apply(Integer maximumPower, Float factor) {
			if (maximumPower == null || factor == null) {
				return null;
			}

			var linearFactor = ((1 - Math.cos(factor * Math.PI)) / 2);
			return (int) Math.round(maximumPower * linearFactor);
		}
	}
}
