package io.openems.edge.ess.power.api;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import io.openems.edge.ess.api.ManagedSymmetricEss;

public class Coefficients {

	private final List<Coefficient> coefficients = new CopyOnWriteArrayList<>();

	private int noOfCoefficients = 0;

	public synchronized void initialize(Set<ManagedSymmetricEss> esss) {
		this.coefficients.clear();
		int index = 0;
		for (ManagedSymmetricEss ess : esss) {
			for (Phase phase : Phase.values()) {
				for (Pwr pwr : Pwr.values()) {
					this.coefficients.add(new Coefficient(index++, ess, phase, pwr));
				}
			}
		}
		this.noOfCoefficients = index;
	}

	public Coefficient of(ManagedSymmetricEss ess, Phase phase, Pwr pwr) {
		for (Coefficient c : this.coefficients) {
			if (Objects.equals(c.ess, ess) && c.phase == phase && c.pwr == pwr) {
				return c;
			}
		}
		throw new IllegalArgumentException("Coefficient for [" + ess.id() + "," + phase + "," + pwr
				+ "] was not found. Forgot to call initialize()?");
	}

	public List<Coefficient> getAll() {
		return Collections.unmodifiableList(this.coefficients);
	}

	public int getNoOfCoefficients() {
		return noOfCoefficients;
	}
}
