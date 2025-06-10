package io.openems.edge.ess.power.api;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import io.openems.common.exceptions.OpenemsException;

public class Coefficients {

	private final List<Coefficient> coefficients = new CopyOnWriteArrayList<>();

	private boolean symmetricMode = false;
	private int noOfCoefficients = 0;

	/**
	 * Initialize the Coefficients for the linear equation system.
	 *
	 * @param symmetricMode if activated, Coefficients are only added for Sum of all
	 *                      Phases. Otherwise Coefficients for Sum and each Phase
	 *                      are added.
	 * @param essIds        Set of ESS-Ids
	 */
	public synchronized void initialize(boolean symmetricMode, Set<String> essIds) {
		this.coefficients.clear();
		this.symmetricMode = symmetricMode;
		var index = 0;
		for (String essId : essIds) {
			if (symmetricMode) {
				// Symmetric Mode
				for (Pwr pwr : Pwr.values()) {
					this.coefficients.add(new Coefficient(index++, essId, Phase.ALL, pwr));
				}
			} else {
				// Asymmetric Mode
				for (Phase phase : Phase.values()) {
					for (Pwr pwr : Pwr.values()) {
						this.coefficients.add(new Coefficient(index++, essId, phase, pwr));
					}
				}
			}
		}
		this.noOfCoefficients = index;
	}

	/**
	 * Gets the {@link Coefficient} for the given Ess-ID, {@link Phase} and
	 * {@link Pwr}.
	 * 
	 * @param essId the Ess-ID
	 * @param phase the {@link Phase}
	 * @param pwr   the {@link Pwr}
	 * @return the {@link Coefficient}
	 * @throws OpenemsException on error
	 */
	public Coefficient of(String essId, Phase phase, Pwr pwr) throws OpenemsException {
		if (this.symmetricMode && phase != Phase.ALL) {
			throw new OpenemsException("Symmetric-Mode is activated. Coefficients for [" + essId + "," + phase + ","
					+ pwr + "] is not available!");
		}
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
		return this.noOfCoefficients;
	}
}
