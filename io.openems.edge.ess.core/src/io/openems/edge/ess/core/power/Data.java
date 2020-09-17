package io.openems.edge.ess.core.power;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Streams;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSinglePhaseEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.core.power.data.ApparentPowerConstraintUtil;
import io.openems.edge.ess.core.power.data.ConstraintUtil;
import io.openems.edge.ess.core.power.data.WeightsUtil;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.DummyInverter;
import io.openems.edge.ess.power.api.EssType;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.LinearCoefficient;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class Data {

	private final Logger log = LoggerFactory.getLogger(Data.class);

	private final PowerComponent parent;

	/**
	 * Holds all Inverters, Sorted by weight.
	 */
	private final List<Inverter> inverters = new ArrayList<>();

	/**
	 * Holds all Ess.
	 */
	private final List<ManagedSymmetricEss> esss = new ArrayList<>();

	private final List<Constraint> constraints = new CopyOnWriteArrayList<>();
	private final Coefficients coefficients = new Coefficients();

	private boolean symmetricMode = PowerComponent.DEFAULT_SYMMETRIC_MODE;

	public Data(PowerComponent parent) {
		this.parent = parent;
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
			EssType essType = EssType.getEssType(ess);
			for (Inverter inverter : Inverter.of(this.symmetricMode, ess, essType)) {
				this.inverters.add(inverter);
			}
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

	public synchronized void initializeCycle() {
		// Remove Constraints of last Cycle
		this.constraints.clear();
		// Update sorting of Inverters
		WeightsUtil.updateWeightsFromSoc(this.inverters, this.esss);
		WeightsUtil.adjustSortingByWeights(this.inverters);
	}

	protected List<ManagedSymmetricEss> getEsss() {
		return esss;
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

	public Coefficients getCoefficients() {
		return this.coefficients;
	}

	public Coefficient getCoefficient(String essId, Phase phase, Pwr pwr) throws OpenemsException {
		return this.coefficients.of(essId, phase, pwr);
	}

	/**
	 * Gets Constraints for all Inverters.
	 * 
	 * @return List of Constraints
	 * @throws OpenemsException
	 */
	public List<Constraint> getConstraintsForAllInverters() throws OpenemsException {
		return this.getConstraintsWithoutDisabledInverters(Collections.emptyList());
	}

	/**
	 * Gets Constraints with the 'enabledInverters' only.
	 * 
	 * @param enabledInverters Collection of enabled inverters
	 * @return List of Constraints
	 * @throws OpenemsException
	 */
	public List<Constraint> getConstraintsForInverters(Collection<Inverter> enabledInverters) throws OpenemsException {
		List<Inverter> disabledInverters = new ArrayList<>(this.inverters);
		disabledInverters.removeAll(enabledInverters);
		return getConstraintsWithoutDisabledInverters(disabledInverters);
	}

	/**
	 * Gets Constraints without the 'disabledInverters'.
	 * 
	 * @param disabledInverters Collection of disabled inverters
	 * @return List of Constraints
	 * @throws OpenemsException
	 */
	public List<Constraint> getConstraintsWithoutDisabledInverters(Collection<Inverter> disabledInverters)
			throws OpenemsException {
		return Streams.concat(//
				this.createDisableConstraintsForInactiveInverters(disabledInverters).stream(),
				this.createGenericEssConstraints().stream(), //
				this.createStaticEssConstraints().stream(), //
				this.createClusterConstraints().stream(), //
				this.createSumOfPhasesConstraints().stream(), //
				this.createSymmetricEssConstraints().stream(), //
				this.createSinglePhaseEssConstraints().stream(), //
				this.constraints.stream()).collect(Collectors.toList());
	}

	/**
	 * Creates for each disabled inverter an EQUALS ZERO constraint.
	 * 
	 * @param inverters Collection of inverters
	 * @return List of Constraints
	 * @throws OpenemsException
	 */
	public List<Constraint> createDisableConstraintsForInactiveInverters(Collection<Inverter> inverters)
			throws OpenemsException {
		List<Constraint> result = new ArrayList<>();
		for (Inverter inv : inverters) {
			String essId = inv.getEssId();
			Phase phase = inv.getPhase();
			for (Pwr pwr : Pwr.values()) {
				result.add(ConstraintUtil.createSimpleConstraint(this.coefficients, //
						essId + ": Disable " + pwr.getSymbol() + phase.getSymbol(), //
						essId, phase, pwr, Relationship.EQUALS, 0));
			}
		}
		return result;
	}

	/**
	 * Creates for each Ess constraints for AllowedCharge, AllowedDischarge and
	 * MaxApparentPower.
	 * 
	 * @return List of Constraints
	 * @throws OpenemsException
	 */
	public List<Constraint> createGenericEssConstraints() throws OpenemsException {
		List<Constraint> result = new ArrayList<>();
		for (ManagedSymmetricEss ess : this.esss) {

			if (ess instanceof MetaEss) {
				// ignore
				continue;
			}

			// Allowed Charge Power
			result.add(ConstraintUtil.createSimpleConstraint(this.coefficients, ess.id() + ": Allowed Charge", //
					ess.id(), Phase.ALL, Pwr.ACTIVE, Relationship.GREATER_OR_EQUALS, //
					ess.getAllowedChargePower().orElse(0)));

			// Allowed Charge Power
			result.add(ConstraintUtil.createSimpleConstraint(this.coefficients, ess.id() + ": Allowed Discharge", //
					ess.id(), Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS, //
					ess.getAllowedDischargePower().orElse(0)));

			// Max Apparent Power
			int maxApparentPower = ess.getMaxApparentPower().orElse(0);
			if (ess instanceof ManagedAsymmetricEss && !this.symmetricMode && !(ess instanceof ManagedSinglePhaseEss)) {
				double maxApparentPowerPerPhase = maxApparentPower / 3d;
				for (Phase phase : Phase.values()) {
					if (phase == Phase.ALL) {
						continue; // do not add Max Apparent Power Constraint for ALL phases
					}
					result.addAll(ApparentPowerConstraintUtil.generateConstraints(this.coefficients, ess.id(), phase,
							maxApparentPowerPerPhase));
				}
			} else {
				result.addAll(ApparentPowerConstraintUtil.generateConstraints(this.coefficients, ess.id(), Phase.ALL,
						maxApparentPower));
			}
		}
		return result;
	}

	/**
	 * Asks each Ess if it has any static Constraints and adds them.
	 * 
	 * @return @return List of Constraints
	 */
	public List<Constraint> createStaticEssConstraints() {
		List<Constraint> result = new ArrayList<>();
		boolean isFailed = false;
		for (ManagedSymmetricEss ess : this.esss) {
			try {
				for (Constraint c : ess.getStaticConstraints()) {
					result.add(c);
				}
			} catch (OpenemsNamedException e) {
				this.log.error("Setting static contraints for Ess [" + ess.id() + "] failed: " + e.getMessage());
				isFailed = true;
			}
		}
		this.parent.channel(PowerComponent.ChannelId.STATIC_CONSTRAINTS_FAILED).setNextValue(isFailed);
		return result;
	}

	/**
	 * Creates Constraints for Cluster, e.g. ClusterL1 = ess1_L1 + ess2_L1 + ...
	 * 
	 * @return List of Constraints
	 * @throws OpenemsException
	 */
	public List<Constraint> createClusterConstraints() throws OpenemsException {
		List<Constraint> result = new ArrayList<>();
		for (ManagedSymmetricEss ess : this.esss) {
			if (ess instanceof MetaEss) {
				MetaEss e = (MetaEss) ess;
				if (this.symmetricMode) {
					// Symmetric Mode
					for (Pwr pwr : Pwr.values()) {
						result.add(this.createClusterConstraint(e, Phase.ALL, pwr));
					}
				} else {
					// Asymmetric Mode
					for (Phase phase : Phase.values()) {
						for (Pwr pwr : Pwr.values()) {
							result.add(this.createClusterConstraint(e, phase, pwr));
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Creates a constraint of the form: 1*sumL1 - 1*ess1_L1 - 1*ess2_L1 = 0
	 * 
	 * @param e     the {@link MetaEss} Cluster
	 * @param phase the {@link Phase}
	 * @param pwr   the {@link Pwr}
	 * @return the {@link Constraint}
	 * @throws OpenemsException on error
	 */
	private Constraint createClusterConstraint(MetaEss e, Phase phase, Pwr pwr) throws OpenemsException {
		List<LinearCoefficient> cos = new ArrayList<>();
		cos.add(new LinearCoefficient(this.coefficients.of(e.id(), phase, pwr), 1));
		for (String subEssId : e.getEssIds()) {
			cos.add(new LinearCoefficient(this.coefficients.of(subEssId, phase, pwr), -1));
		}
		return new Constraint(e.id() + ": Sum of " + pwr.getSymbol() + phase.getSymbol(), cos, Relationship.EQUALS, 0);
	}

	/**
	 * Creates Constraints for Three-Phased Ess: P = L1 + L2 + L3.
	 * 
	 * <p>
	 * If symmetricMode is activated, an empty list is returned.
	 * 
	 * @return List of Constraints
	 * @throws OpenemsException on error
	 */
	public List<Constraint> createSumOfPhasesConstraints() throws OpenemsException {
		List<Constraint> result = new ArrayList<>();
		if (this.symmetricMode) {
			// Symmetric Mode
		} else {
			// Asymmetric Mode
			for (ManagedSymmetricEss ess : this.esss) {
				for (Pwr pwr : Pwr.values()) {
					// creates two constraint of the form
					// 1*P - 1*L1 - 1*L2 - 1*L3 = 0
					// 1*Q - 1*L1 - 1*L2 - 1*L3 = 0
					result.add(new Constraint(ess.id() + ": " + pwr.getSymbol() + "=L1+L2+L3",
							new LinearCoefficient[] {
									new LinearCoefficient(this.coefficients.of(ess.id(), Phase.ALL, pwr), 1),
									new LinearCoefficient(this.coefficients.of(ess.id(), Phase.L1, pwr), -1),
									new LinearCoefficient(this.coefficients.of(ess.id(), Phase.L2, pwr), -1),
									new LinearCoefficient(this.coefficients.of(ess.id(), Phase.L3, pwr), -1) //
							}, Relationship.EQUALS, 0));
				}
			}
		}
		return result;
	}

	/**
	 * Creates Constraints for SymmetricEss, e.g. L1 = L2 = L3.
	 * 
	 * @return List of Constraints
	 * @throws OpenemsException
	 */
	public List<Constraint> createSymmetricEssConstraints() throws OpenemsException {
		List<Constraint> result = new ArrayList<>();
		for (ManagedSymmetricEss ess : this.esss) {
			EssType essType = EssType.getEssType(ess);
			if (!this.symmetricMode && essType == EssType.SYMMETRIC) {
				/*
				 * Symmetric-Mode is deactivated and this is a Symmetric ESS: Add Symmetric
				 * Constraints
				 */
				for (Pwr pwr : Pwr.values()) {
					// creates two constraint of the form
					// 1*L1 - 1*L2 = 0
					// 1*L1 - 1*L3 = 0
					result.add(new Constraint(ess.id() + ": Symmetric L1/L2", new LinearCoefficient[] { //
							new LinearCoefficient(this.coefficients.of(ess.id(), Phase.L1, pwr), 1), //
							new LinearCoefficient(this.coefficients.of(ess.id(), Phase.L2, pwr), -1) //
					}, Relationship.EQUALS, 0));
					result.add(new Constraint(ess.id() + ": Symmetric L1/L3", new LinearCoefficient[] { //
							new LinearCoefficient(this.coefficients.of(ess.id(), Phase.L1, pwr), 1), //
							new LinearCoefficient(this.coefficients.of(ess.id(), Phase.L3, pwr), -1) //
					}, Relationship.EQUALS, 0));
				}
			}
		}
		return result;
	}

	/**
	 * For Single-Phase-ESS: Creates an EQUALS ZERO constraint for the not-connected
	 * phases.
	 * 
	 * <p>
	 * If symmetricMode is activated, an empty list is returned.
	 * 
	 * @return List of Constraints
	 * @throws OpenemsException
	 */
	public List<Constraint> createSinglePhaseEssConstraints() throws OpenemsException {
		List<Constraint> result = new ArrayList<>();
		if (this.symmetricMode) {
			// Symmetric Mode
		} else {
			// Asymmetric Mode
			for (Inverter inv : inverters) {
				if (inv instanceof DummyInverter) {
					for (Pwr pwr : Pwr.values()) {
						result.add(ConstraintUtil.createSimpleConstraint(this.coefficients, //
								inv.getEssId() + ": Dummy " + pwr.getSymbol() + inv.getPhase().getSymbol(),
								inv.getEssId(), inv.getPhase(), pwr, Relationship.EQUALS, 0));
					}
				}
			}
		}
		return result;
	}

	protected ManagedSymmetricEss getEss(String essId) {
		for (ManagedSymmetricEss ess : esss) {
			if (essId.equals(ess.id())) {
				return ess;
			}
		}
		return null;
	}

}
