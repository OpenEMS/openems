package io.openems.edge.ess.power.api;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import io.openems.common.exceptions.OpenemsException;

public class Coefficients {

	private final List<Coefficient> coefficients = new CopyOnWriteArrayList<>();

	private int noOfCoefficients = 0;

	public synchronized void initialize(Set<String> essIds) {
		this.coefficients.clear();
		int index = 0;
		for (String essId : essIds) {
			for (Phase phase : Phase.values()) {
				for (Pwr pwr : Pwr.values()) {
					this.coefficients.add(new Coefficient(index++, essId, phase, pwr));
				}
			}
		}
		this.noOfCoefficients = index;
	}

	public Coefficient of(String essId, Phase phase, Pwr pwr) throws OpenemsException {
		for (Coefficient c : this.coefficients) {
			if (Objects.equals(c.essId, essId) && c.phase == phase && c.pwr == pwr) {
				return c;
			}
		}
		throw new OpenemsException("Coefficient for [" + essId + "," + phase + "," + pwr
				+ "] was not found. Ess-Power is not (yet) fully initialized.");
	}

	public List<Coefficient> getAll() {
		return Collections.unmodifiableList(this.coefficients);
	}

	public int getNoOfCoefficients() {
		return noOfCoefficients;
	}
}
