package io.openems.edge.ess.core.power;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.Streams;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.core.power.data.ConstraintUtil;
import io.openems.edge.ess.core.power.data.WeightsUtil;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.EssType;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class Data {

	/**
	 * Holds all Inverters, always roughly sorted by weight.
	 */
	private final List<Inverter> inverters = new ArrayList<>();

	/**
	 * Holds all Ess.
	 */
	private final List<ManagedSymmetricEss> esss = new ArrayList<>();

	private final List<Constraint> constraints = new CopyOnWriteArrayList<>();
	private final Coefficients coefficients = new Coefficients();

	private boolean symmetricMode = PowerComponent.DEFAULT_SYMMETRIC_MODE;
	private Consumer<Boolean> onStaticConstraintsFailed = null;

	/**
	 * Adds a callback for onStaticConstraintsFailed event.
	 *
	 * @param onStaticConstraintsFailed the Callback
	 */
	public void onStaticConstraintsFailed(Consumer<Boolean> onStaticConstraintsFailed) {
		this.onStaticConstraintsFailed = onStaticConstraintsFailed;
	}

	/**
	 * Adds a {@link ManagedSymmetricEss}. Called by {@link PowerComponentImpl}.
	 *
	 * @param ess the {@link ManagedSymmetricEss}
	 */
	protected synchronized void addEss(ManagedSymmetricEss ess) {
		this.esss.add(ess);
		this.updateInverters();
	}

	/**
	 * Removes a {@link ManagedSymmetricEss}. Called by {@link PowerComponentImpl}.
	 *
	 * @param ess the {@link ManagedSymmetricEss}
	 */
	protected synchronized void removeEss(ManagedSymmetricEss ess) {
		this.esss.remove(ess);
		this.updateInverters();
	}

	/**
	 * Activates Symmetric-Mode.
	 *
	 * @param symmetricMode Symmetric-Mode enabled?
	 */
	public synchronized void setSymmetricMode(boolean symmetricMode) {
		if (this.symmetricMode != symmetricMode) {
			this.symmetricMode = symmetricMode;
			this.updateInverters();
			this.initializeCycle(); // because SymmetricEssConstraints need to be renewed
		}
	}

	private synchronized void updateInverters() {
		this.inverters.clear();

		// Create inverters and add them to list
		for (ManagedSymmetricEss ess : this.esss) {
			var essType = EssType.getEssType(ess);
			Collections.addAll(this.inverters, Inverter.of(this.symmetricMode, ess, essType));
		}

		// Re-Initialize Coefficients
		Set<String> essIds = new HashSet<>();
		for (ManagedSymmetricEss ess : this.esss) {
			essIds.add(ess.id());
		}
		this.coefficients.initialize(this.symmetricMode, essIds);

		// Initially sort Inverters
		WeightsUtil.updateWeightsFromSoc(this.inverters, this.esss);
		WeightsUtil.sortByWeights(this.inverters);
	}

	protected synchronized void initializeCycle() {
		// Remove Constraints of last Cycle
		this.constraints.clear();
		// Update sorting of Inverters
		WeightsUtil.updateWeightsFromSoc(this.inverters, this.esss);
		WeightsUtil.adjustSortingByWeights(this.inverters);
	}

	protected List<ManagedSymmetricEss> getEsss() {
		return this.esss;
	}

	protected List<Inverter> getInverters() {
		return Collections.unmodifiableList(this.inverters);
	}

	protected void addConstraint(Constraint constraint) {
		this.constraints.add(constraint);
	}

	protected void removeConstraint(Constraint constraint) {
		this.constraints.remove(constraint);
	}

	/**
	 * Adds a simple Constraint with only one Coefficient.
	 *
	 * @param description  a description for the Constraint
	 * @param essId        the component Id of a {@link ManagedSymmetricEss}
	 * @param phase        the {@link Phase}
	 * @param pwr          the {@link Pwr}
	 * @param relationship the {@link Relationship}
	 * @param value        the value
	 * @throws OpenemsException on error
	 */
	public void addSimpleConstraint(String description, String essId, Phase phase, Pwr pwr, Relationship relationship,
			double value) throws OpenemsException {
		if (this.symmetricMode && phase != Phase.ALL) {
			// Symmetric Mode is activated; but asymmetric Constraints is added
			phase = Phase.ALL;
			value *= 3;
		}
		this.constraints.add(ConstraintUtil.createSimpleConstraint(this.coefficients, //
				description, essId, phase, pwr, relationship, value));
	}

	/**
	 * Get the Coefficients of the linear solver.
	 *
	 * @return the {@link Coefficients}
	 */
	public Coefficients getCoefficients() {
		return this.coefficients;
	}

	/**
	 * Get the Coefficient of the linear solver for the given parameters.
	 *
	 * @param essId the Component-ID of a {@link ManagedSymmetricEss}
	 * @param phase the {@link Phase}
	 * @param pwr   the {@link Pwr}
	 * @return the {@link Coefficients}
	 */
	public Coefficient getCoefficient(String essId, Phase phase, Pwr pwr) throws OpenemsException {
		return this.coefficients.of(essId, phase, pwr);
	}

	/**
	 * Gets Constraints for all Inverters.
	 *
	 * @return List of Constraints
	 * @throws OpenemsException on error
	 */
	public List<Constraint> getConstraintsForAllInverters() throws OpenemsException {
		return this.getConstraintsWithoutDisabledInverters(Collections.emptyList());
	}

	/**
	 * Gets Constraints with the 'enabledInverters' only.
	 *
	 * @param enabledInverters Collection of enabled {@link Inverter}s
	 * @return List of {@link Constraint}s
	 * @throws OpenemsException on error
	 */
	public List<Constraint> getConstraintsForInverters(Collection<Inverter> enabledInverters) throws OpenemsException {
		List<Inverter> disabledInverters = new ArrayList<>(this.inverters);
		disabledInverters.removeAll(enabledInverters);
		return this.getConstraintsWithoutDisabledInverters(disabledInverters);
	}

	/**
	 * Gets Constraints without the 'disabledInverters'.
	 *
	 * @param disabledInverters Collection of disabled inverters
	 * @return List of Constraints
	 * @throws OpenemsException on error
	 */
	public List<Constraint> getConstraintsWithoutDisabledInverters(Collection<Inverter> disabledInverters)
			throws OpenemsException {
		return Streams.concat(//
				ConstraintUtil.createDisableConstraintsForInactiveInverters(this.coefficients, disabledInverters)
						.stream(),
				ConstraintUtil.createGenericEssConstraints(this.coefficients, this.esss, this.symmetricMode).stream(), //
				ConstraintUtil.createStaticEssConstraints(this.esss, this.onStaticConstraintsFailed).stream(), //
				ConstraintUtil.createMetaEssConstraints(this.coefficients, this.esss, this.symmetricMode).stream(), //
				ConstraintUtil.createSumOfPhasesConstraints(this.coefficients, this.esss, this.symmetricMode).stream(), //
				ConstraintUtil.createSymmetricEssConstraints(this.coefficients, this.esss, this.symmetricMode).stream(), //
				ConstraintUtil.createSinglePhaseEssConstraints(this.coefficients, this.inverters, this.symmetricMode)
						.stream(), //
				this.constraints.stream()).collect(Collectors.toList());
	}

	protected ManagedSymmetricEss getEss(String essId) {
		for (ManagedSymmetricEss ess : this.esss) {
			if (essId.equals(ess.id())) {
				return ess;
			}
		}
		return null;
	}

}
