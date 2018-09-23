package io.openems.edge.ess.core.power;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import io.openems.edge.ess.api.ManagedAsymmetricEss;

public class ManagedAsymmetricEssDummy extends DummyComponent<ManagedAsymmetricEssDummy>
		implements ManagedAsymmetricEss {

	public ManagedAsymmetricEssDummy(String id) {
		super(id);
	}

	Optional<Integer> expectPL1 = Optional.empty();

	public void expectPL1(int value) {
		this.expectPL1 = Optional.of(value);
	}

	Optional<Integer> expectPL2 = Optional.empty();

	public void expectPL2(int value) {
		this.expectPL2 = Optional.of(value);
	}

	Optional<Integer> expectPL3 = Optional.empty();

	public void expectPL3(int value) {
		this.expectPL3 = Optional.of(value);
	}

	Optional<Integer> expectQL1 = Optional.empty();

	public void expectQL1(int value) {
		this.expectQL1 = Optional.of(value);
	}

	Optional<Integer> expectQL2 = Optional.empty();

	public void expectQL2(int value) {
		this.expectQL2 = Optional.of(value);
	}

	Optional<Integer> expectQL3 = Optional.empty();

	public void expectQL3(int value) {
		this.expectQL3 = Optional.of(value);
	}

	@Override
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) {
		if (this.expectPL1.isPresent()) {
			assertEquals((int) this.expectPL1.get(), activePowerL1);
			this.expectPL1 = Optional.empty();
		}
		if (this.expectPL2.isPresent()) {
			assertEquals((int) this.expectPL2.get(), activePowerL2);
			this.expectPL2 = Optional.empty();
		}
		if (this.expectPL3.isPresent()) {
			assertEquals((int) this.expectPL3.get(), activePowerL3);
			this.expectPL3 = Optional.empty();
		}
		if (this.expectQL1.isPresent()) {
			assertEquals((int) this.expectQL1.get(), reactivePowerL1);
			this.expectQL1 = Optional.empty();
		}
		if (this.expectQL2.isPresent()) {
			assertEquals((int) this.expectQL2.get(), reactivePowerL2);
			this.expectQL2 = Optional.empty();
		}
		if (this.expectQL3.isPresent()) {
			assertEquals((int) this.expectQL3.get(), reactivePowerL3);
			this.expectQL3 = Optional.empty();
		}
	}

	@Override
	protected ManagedAsymmetricEssDummy self() {
		return this;
	}
}
