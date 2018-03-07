package io.openems.impl.device.simulator;

import java.util.concurrent.ThreadLocalRandom;

public class SimulatorTools {

	public static long addRandomLong(long value, long min, long max, int delta) {
		long random = getRandomLong(delta * -1, delta);
		value += random;
		if (value > max) {
			value = max;
		} else if (value < min) {
			value = min;
		}
		return value;
	}

	public static long getRandomLong(int min, int max) {
		return ThreadLocalRandom.current().nextLong(min, max + 1);
	}

	public static double addRandomDouble(double value, double min, double max, double delta) {
		double random = getRandomDouble(delta * -1, delta);
		value += random;
		if (value > max) {
			value = max;
		} else if (value < min) {
			value = min;
		}
		return value;
	}

	public static double getRandomDouble(double min, double max) {
		return ThreadLocalRandom.current().nextDouble(min, max);
	}

}
