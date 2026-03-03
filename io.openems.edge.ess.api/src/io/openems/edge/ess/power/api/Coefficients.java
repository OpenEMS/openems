package io.openems.edge.ess.power.api;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.type.Phase;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;

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
		// Build the new coefficients in a temporary list first, then swap atomically.
		// This avoids a window where the list is cleared but not yet rebuilt, which
		// would cause concurrent readers in of() to throw "Coefficient was not found".
		var newCoefficients = new java.util.ArrayList<Coefficient>();
		var index = 0;
		for (String essId : essIds) {
			if (symmetricMode) {
				// Symmetric Mode
				for (Pwr pwr : Pwr.values()) {
					newCoefficients.add(new Coefficient(index++, essId, SingleOrAllPhase.ALL, pwr));
				}
			} else {
				// Asymmetric Mode
				for (var phase : SingleOrAllPhase.values()) {
					for (Pwr pwr : Pwr.values()) {
						newCoefficients.add(new Coefficient(index++, essId, phase, pwr));
					}
				}
			}
		}
		this.symmetricMode = symmetricMode;
		this.noOfCoefficients = index;
		// Atomic swap: clear and addAll in immediate succession on CopyOnWriteArrayList.
		// Because of() is also synchronized on 'this', no reader can see the empty state.
		this.coefficients.clear();
		this.coefficients.addAll(newCoefficients);
	}

	/**
	 * Gets the {@link Coefficient} for the given Ess-ID, {@link Phase} and
	 * {@link Pwr}.
	 * 
	 * @param essId the Ess-ID
	 * @param phase the {@link SingleOrAllPhase}
	 * @param pwr   the {@link Pwr}
	 * @return the {@link Coefficient}
	 * @throws OpenemsException on error
	 */
	public synchronized Coefficient of(String essId, SingleOrAllPhase phase, Pwr pwr) throws OpenemsException {
		if (this.symmetricMode && phase != SingleOrAllPhase.ALL) {
			throw new OpenemsException("Symmetric-Mode is activated. Coefficients for [" + essId + "," + phase + ","
					+ pwr + "] is not available!");
		}
		for (var c : this.coefficients) {
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
