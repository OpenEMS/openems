package io.openems.edge.ess.core.power;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import io.openems.edge.ess.api.ManagedSymmetricEss;

public class ManagedSymmetricEssDummy extends DummyComponent<ManagedSymmetricEssDummy> implements ManagedSymmetricEss {

	public ManagedSymmetricEssDummy(String id) {
		super(id);
	}

	Optional<Integer> expectP = Optional.empty();

	public ManagedSymmetricEssDummy expectP(int value) {
		this.expectP = Optional.of(value);
		return this;
	}

	Optional<Integer> expectQ = Optional.empty();

	public ManagedSymmetricEssDummy expectQ(int value) {
		this.expectQ = Optional.of(value);
		return this;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
		if (this.expectP.isPresent()) {
			assertEquals((int) this.expectP.get(), activePower);
			this.expectP = Optional.empty();
		}
		if (this.expectQ.isPresent()) {
			assertEquals((int) this.expectQ.get(), reactivePower);
			this.expectQ = Optional.empty();
		}
	}

	@Override
	protected ManagedSymmetricEssDummy self() {
		return this;
	}
}
