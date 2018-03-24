package io.openems.edge.application;

import java.util.Optional;

public class Utils {

	// Source: https://stackoverflow.com/a/4202114/4137113
	protected static int getGreatestCommonDivisor(int a, int b) {
		while (b > 0) {
			int temp = b;
			b = a % b; // % is remainder
			a = temp;
		}
		return a;
	}

	protected static Optional<Integer> getGreatestCommonDivisor(int[] input) {
		if (input.length == 0) {
			return Optional.empty();
		}
		int result = input[0];
		for (int i = 1; i < input.length; i++) {
			result = getGreatestCommonDivisor(result, input[i]);
		}
		return Optional.of(result);
	}

	private static int getLeastCommonMultiple(int a, int b) {
		return a * (b / getGreatestCommonDivisor(a, b));
	}

	protected static Optional<Integer> getLeastCommonMultiple(int[] input) {
		if (input.length == 0) {
			return Optional.empty();
		}
		int result = input[0];
		for (int i = 1; i < input.length; i++) {
			result = getLeastCommonMultiple(result, input[i]);
		}
		return Optional.of(result);
	}
}
