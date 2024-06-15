package io.openems.edge.ess.core.power.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSinglePhaseEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.core.power.EssPower.ChannelId;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.DummyInverter;
import io.openems.edge.ess.power.api.EssType;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.LinearCoefficient;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class ConstraintUtil {

	private static final Logger LOG = LoggerFactory.getLogger(ConstraintUtil.class);

	private ConstraintUtil() {
	}

	/**
	 * Creates a simple Constraint with only one Coefficient.
	 *
	 * @param coefficients the {@link Coefficients}
	 * @param description  a description for the Constraint
	 * @param essId        the component Id of a {@link ManagedSymmetricEss}
	 * @param phase        the {@link Phase}
	 * @param pwr          the {@link Pwr}
	 * @param relationship the {@link Relationship}
	 * @param value        the value
	 * @return the {@link Constraint}
	 * @throws OpenemsException on error
	 */
	public static Constraint createSimpleConstraint(Coefficients coefficients, String description, String essId,
			Phase phase, Pwr pwr, Relationship relationship, double value) throws OpenemsException {
		return new Constraint(description, //
				new LinearCoefficient[] { //
						new LinearCoefficient(coefficients.of(essId, phase, pwr), 1) //
				}, relationship, //
				value);
	}

	/**
	 * Creates for each disabled inverter an EQUALS ZERO constraint.
	 *
	 * @param coefficients the {@link Coefficients}
	 * @param inverters    Collection of {@link Inverter}s
	 * @return List of {@link Constraint}s
	 * @throws OpenemsException on error
	 */
	public static List<Constraint> createDisableConstraintsForInactiveInverters(Coefficients coefficients,
			Collection<Inverter> inverters) throws OpenemsException {
		List<Constraint> result = new ArrayList<>();
		for (Inverter inv : inverters) {
			var essId = inv.getEssId();
			var phase = inv.getPhase();
			for (Pwr pwr : Pwr.values()) {
				result.add(ConstraintUtil.createSimpleConstraint(coefficients, //
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
	 * @param coefficients  the {@link Coefficients}
	 * @param esss          list of {@link ManagedSymmetricEss}s
	 * @param symmetricMode Symmetric-Mode enabled?
	 * @return List of {@link Constraint}s
	 * @throws OpenemsException on error
	 */
	public static List<Constraint> createGenericEssConstraints(Coefficients coefficients,
			List<ManagedSymmetricEss> esss, boolean symmetricMode) throws OpenemsException {
		List<Constraint> result = new ArrayList<>();
		for (ManagedSymmetricEss ess : esss) {

			if (ess instanceof MetaEss) {
				// ignore
				continue;
			}

			// Allowed Charge Power
			result.add(ConstraintUtil.createSimpleConstraint(coefficients, ess.id() + ": Allowed Charge", //
					ess.id(), Phase.ALL, Pwr.ACTIVE, Relationship.GREATER_OR_EQUALS, //
					ess.getAllowedChargePower().orElse(0)));

			// Allowed Charge Power
			result.add(ConstraintUtil.createSimpleConstraint(coefficients, ess.id() + ": Allowed Discharge", //
					ess.id(), Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS, //
					ess.getAllowedDischargePower().orElse(0)));

			// Max Apparent Power
			int maxApparentPower = ess.getMaxApparentPower().orElse(0);
			if (ess instanceof ManagedAsymmetricEss && !symmetricMode && !(ess instanceof ManagedSinglePhaseEss)) {
				var maxApparentPowerPerPhase = maxApparentPower / 3d;
				for (Phase phase : Phase.values()) {
					if (phase == Phase.ALL) {
						continue; // do not add Max Apparent Power Constraint for ALL phases
					}
					result.addAll(ApparentPowerConstraintUtil.generateConstraints(coefficients, ess.id(), phase,
							maxApparentPowerPerPhase));
				}
			} else {
				result.addAll(ApparentPowerConstraintUtil.generateConstraints(coefficients, ess.id(), Phase.ALL,
						maxApparentPower));
			}
		}
		return result;
	}

	/**
	 * Asks each Ess if it has any static Constraints and adds them.
	 *
	 * @param esss                      list of {@link ManagedSymmetricEss}s
	 * @param onStaticConstraintsFailed callback for
	 *                                  {@link ChannelId#STATIC_CONSTRAINTS_FAILED}
	 *                                  channel
	 * @return List of {@link Constraint}s
	 */
	public static List<Constraint> createStaticEssConstraints(List<ManagedSymmetricEss> esss,
			Consumer<Boolean> onStaticConstraintsFailed) {
		List<Constraint> result = new ArrayList<>();
		var isFailed = false;
		for (ManagedSymmetricEss ess : esss) {
			try {
				Collections.addAll(result, ess.getStaticConstraints());
			} catch (OpenemsNamedException e) {
				LOG.error("Setting static constraints for Ess [" + ess.id() + "] failed: " + e.getMessage());
				isFailed = true;
			}
		}
		if (onStaticConstraintsFailed != null) {
			onStaticConstraintsFailed.accept(isFailed);
		}
		return result;
	}

	/**
	 * Creates Constraints for Three-Phased Ess: P = L1 + L2 + L3.
	 *
	 * <p>
	 * If symmetricMode is activated, an empty list is returned.
	 *
	 * @param coefficients  the {@link Coefficients}
	 * @param esss          list of {@link ManagedSymmetricEss}s
	 * @param symmetricMode Symmetric-Mode enabled?
	 * @return List of Constraints
	 * @throws OpenemsException on error
	 */
	public static List<Constraint> createSumOfPhasesConstraints(Coefficients coefficients,
			List<ManagedSymmetricEss> esss, boolean symmetricMode) throws OpenemsException {
		List<Constraint> result = new ArrayList<>();
		if (symmetricMode) {
			// Symmetric Mode
		} else {
			// Asymmetric Mode
			for (ManagedSymmetricEss ess : esss) {
				for (Pwr pwr : Pwr.values()) {
					// creates two constraint of the form
					// 1*P - 1*L1 - 1*L2 - 1*L3 = 0
					// 1*Q - 1*L1 - 1*L2 - 1*L3 = 0
					result.add(new Constraint(ess.id() + ": " + pwr.getSymbol() + "=L1+L2+L3",
							new LinearCoefficient[] {
									new LinearCoefficient(coefficients.of(ess.id(), Phase.ALL, pwr), 1),
									new LinearCoefficient(coefficients.of(ess.id(), Phase.L1, pwr), -1),
									new LinearCoefficient(coefficients.of(ess.id(), Phase.L2, pwr), -1),
									new LinearCoefficient(coefficients.of(ess.id(), Phase.L3, pwr), -1) //
							}, Relationship.EQUALS, 0));
				}
			}
		}
		return result;
	}

	/**
	 * Creates Constraints for SymmetricEss, e.g. L1 = L2 = L3.
	 *
	 * @param coefficients  the {@link Coefficients}
	 * @param esss          a list of {@link ManagedSymmetricEss}
	 * @param symmetricMode Symmetric-Mode activated?
	 * @return List of {@link Constraint}s
	 * @throws OpenemsException on error
	 */
	public static List<Constraint> createSymmetricEssConstraints(Coefficients coefficients,
			List<ManagedSymmetricEss> esss, boolean symmetricMode) throws OpenemsException {
		List<Constraint> result = new ArrayList<>();
		for (ManagedSymmetricEss ess : esss) {
			var essType = EssType.getEssType(ess);
			if (!symmetricMode && essType == EssType.SYMMETRIC) {
				/*
				 * Symmetric-Mode is deactivated and this is a Symmetric ESS: Add Symmetric
				 * Constraints
				 */
				for (Pwr pwr : Pwr.values()) {
					// creates two constraint of the form
					// 1*L1 - 1*L2 = 0
					// 1*L1 - 1*L3 = 0
					result.add(new Constraint(ess.id() + ": Symmetric L1/L2", new LinearCoefficient[] { //
							new LinearCoefficient(coefficients.of(ess.id(), Phase.L1, pwr), 1), //
							new LinearCoefficient(coefficients.of(ess.id(), Phase.L2, pwr), -1) //
					}, Relationship.EQUALS, 0));
					result.add(new Constraint(ess.id() + ": Symmetric L1/L3", new LinearCoefficient[] { //
							new LinearCoefficient(coefficients.of(ess.id(), Phase.L1, pwr), 1), //
							new LinearCoefficient(coefficients.of(ess.id(), Phase.L3, pwr), -1) //
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
	 * @param coefficients  the {@link Coefficients}
	 * @param inverters     a list of {@link Inverter}s
	 * @param symmetricMode Symmetric-Mode activated?
	 * @return List of Constraints
	 * @throws OpenemsException on error
	 */
	public static List<Constraint> createSinglePhaseEssConstraints(Coefficients coefficients,
			Collection<Inverter> inverters, boolean symmetricMode) throws OpenemsException {
		List<Constraint> result = new ArrayList<>();
		if (symmetricMode) {
			// Symmetric Mode
		} else {
			// Asymmetric Mode
			for (Inverter inv : inverters) {
				if (inv instanceof DummyInverter) {
					for (Pwr pwr : Pwr.values()) {
						result.add(ConstraintUtil.createSimpleConstraint(coefficients, //
								inv.getEssId() + ": Dummy " + pwr.getSymbol() + inv.getPhase().getSymbol(),
								inv.getEssId(), inv.getPhase(), pwr, Relationship.EQUALS, 0));
					}
				}
			}
		}
		return result;
	}

	/**
	 * Creates Constraints for {@link MetaEss}, e.g. ClusterL1 = ess1_L1 + ess2_L1 +
	 * ...
	 *
	 * @param coefficients  the {@link Coefficients}
	 * @param esss          list of {@link ManagedSymmetricEss}s
	 * @param symmetricMode Symmetric-Mode enabled?
	 * @return List of {@link Constraint}s
	 * @throws OpenemsException on error
	 */
	public static List<Constraint> createMetaEssConstraints(Coefficients coefficients, List<ManagedSymmetricEss> esss,
			boolean symmetricMode) throws OpenemsException {
		List<Constraint> result = new ArrayList<>();
		for (ManagedSymmetricEss ess : esss) {
			if (ess instanceof MetaEss) {
				var e = (MetaEss) ess;
				if (symmetricMode) {
					// Symmetric Mode
					for (Pwr pwr : Pwr.values()) {
						result.add(ConstraintUtil.createOneClusterConstraint(coefficients, e, Phase.ALL, pwr));
					}
				} else {
					// Asymmetric Mode
					for (Phase phase : Phase.values()) {
						for (Pwr pwr : Pwr.values()) {
							result.add(ConstraintUtil.createOneClusterConstraint(coefficients, e, phase, pwr));
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Creates a constraint of the form: 1*sumL1 - 1*ess1_L1 - 1*ess2_L1 = 0.
	 *
	 * @param coefficients the {@link Coefficients}
	 * @param e            the {@link MetaEss} Cluster
	 * @param phase        the {@link Phase}
	 * @param pwr          the {@link Pwr}
	 * @return the {@link Constraint}
	 * @throws OpenemsException on error
	 */
	private static Constraint createOneClusterConstraint(Coefficients coefficients, MetaEss e, Phase phase, Pwr pwr)
			throws OpenemsException {
		List<LinearCoefficient> cos = new ArrayList<>();
		cos.add(new LinearCoefficient(coefficients.of(e.id(), phase, pwr), 1));
		for (String subEssId : e.getEssIds()) {
			cos.add(new LinearCoefficient(coefficients.of(subEssId, phase, pwr), -1));
		}
		return new Constraint(e.id() + ": Sum of " + pwr.getSymbol() + phase.getSymbol(), cos, Relationship.EQUALS, 0);
	}

}
