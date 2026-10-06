package io.openems.edge.bridge.eebus.test;

public sealed interface DummyLimitation {
	record DummyNoLimitation() implements DummyLimitation {
	}

	record DummyAbsoluteLimitation(int limit) implements DummyLimitation {
	}

	record DummyPercentageLimitation(double percentage) implements DummyLimitation {
	}
}
