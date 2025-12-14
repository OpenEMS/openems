package io.openems.edge.ess.core.power.data;

import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.common.type.Phase.SingleOrAllPhase.L1;
import static io.openems.edge.common.type.Phase.SingleOrAllPhase.L2;
import static io.openems.edge.common.type.Phase.SingleOrAllPhase.L3;
import static io.openems.edge.ess.core.power.data.ApparentPowerConstraintUtil.generateConstraints;
import static io.openems.edge.ess.power.api.EssType.SYMMETRIC;
import static io.openems.edge.ess.power.api.EssType.getEssType;
import static io.openems.edge.ess.power.api.Pwr.ACTIVE;
import static io.openems.edge.ess.power.api.Relationship.GREATER_OR_EQUALS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSinglePhaseEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.core.power.EssPower.ChannelId;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.DummyInverter;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.LinearCoefficient;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public final class ConstraintUtil {

	private static final Logger LOG = LoggerFactory.getLogger(ConstraintUtil.class);

	private ConstraintUtil() {
	}

	/**
	 * Creates a simple Constraint with only one Coefficient.
	 *
	 * @param coefficients the {@link Coefficients}
	 * @param description  a description for the Constraint
	 * @param essId        the component Id of a {@link ManagedSymmetricEss}
	 * @param phase        the {@link SingleOrAllPhase}
	 * @param pwr          the {@link Pwr}
	 * @param relationship the {@link Relationship}
	 * @param value        the value
	 * @return the {@link Constraint}
	 * @throws OpenemsException on error
	 */
	public static Constraint createSimpleConstraint(Coefficients coefficients, String description, String essId,
			SingleOrAllPhase phase, Pwr pwr, Relationship relationship, double value) throws OpenemsException {
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
		for (var inv : inverters) {
			var essId = inv.getEssId();
			var phase = inv.getPhase();
			for (var pwr : Pwr.values()) {
				result.add(createSimpleConstraint(coefficients, //
						essId + ": Disable " + pwr.symbol + phase.symbol, //
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
		var result = new ArrayList<Constraint>();
		for (var ess : esss) {

			if (ess instanceof MetaEss) {
				// ignore
				continue;
			}

			// Allowed Charge Power
			result.add(createSimpleConstraint(coefficients, ess.id() + ": Allowed Charge", //
					ess.id(), ALL, ACTIVE, GREATER_OR_EQUALS, //
					ess.getAllowedChargePower().orElse(0)));

			// Allowed Charge Power
			result.add(createSimpleConstraint(coefficients, ess.id() + ": Allowed Discharge", //
					ess.id(), ALL, ACTIVE, Relationship.LESS_OR_EQUALS, //
					ess.getAllowedDischargePower().orElse(0)));

			// Max Apparent Power
			int maxApparentPower = ess.getMaxApparentPower().orElse(0);
			if (ess instanceof ManagedAsymmetricEss && !symmetricMode && !(ess instanceof ManagedSinglePhaseEss)) {
				var maxApparentPowerPerPhase = maxApparentPower / 3d;
				for (var phase : SingleOrAllPhase.values()) {
					if (phase == ALL) {
						continue; // do not add Max Apparent Power Constraint for ALL phases
					}
					result.addAll(generateConstraints(coefficients, ess.id(), phase, maxApparentPowerPerPhase));
				}
			} else {
				result.addAll(generateConstraints(coefficients, ess.id(), ALL, maxApparentPower));
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
		for (var ess : esss) {
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
			for (var ess : esss) {
				for (var pwr : Pwr.values()) {
					// creates two constraint of the form
					// 1*P - 1*L1 - 1*L2 - 1*L3 = 0
					// 1*Q - 1*L1 - 1*L2 - 1*L3 = 0
					result.add(new Constraint(ess.id() + ": " + pwr.symbol + "=L1+L2+L3",
							new LinearCoefficient[] { new LinearCoefficient(coefficients.of(ess.id(), ALL, pwr), 1),
									new LinearCoefficient(coefficients.of(ess.id(), L1, pwr), -1),
									new LinearCoefficient(coefficients.of(ess.id(), L2, pwr), -1),
									new LinearCoefficient(coefficients.of(ess.id(), L3, pwr), -1) //
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
		var result = new ArrayList<Constraint>();
		for (var ess : esss) {
			var essType = getEssType(ess);
			if (!symmetricMode && essType == SYMMETRIC) {
				/*
				 * Symmetric-Mode is deactivated and this is a Symmetric ESS: Add Symmetric
				 * Constraints
				 */
				for (var pwr : Pwr.values()) {
					// creates two constraint of the form
					// 1*L1 - 1*L2 = 0
					// 1*L1 - 1*L3 = 0
					result.add(new Constraint(ess.id() + ": Symmetric L1/L2", new LinearCoefficient[] { //
							new LinearCoefficient(coefficients.of(ess.id(), L1, pwr), 1), //
							new LinearCoefficient(coefficients.of(ess.id(), L2, pwr), -1) //
					}, Relationship.EQUALS, 0));
					result.add(new Constraint(ess.id() + ": Symmetric L1/L3", new LinearCoefficient[] { //
							new LinearCoefficient(coefficients.of(ess.id(), L1, pwr), 1), //
							new LinearCoefficient(coefficients.of(ess.id(), L3, pwr), -1) //
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
			for (var inv : inverters) {
				if (inv instanceof DummyInverter) {
					for (var pwr : Pwr.values()) {
						result.add(createSimpleConstraint(coefficients, //
								inv.getEssId() + ": Dummy " + pwr.symbol + inv.getPhase().symbol, inv.getEssId(),
								inv.getPhase(), pwr, Relationship.EQUALS, 0));
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
		for (var ess : esss) {
			if (ess instanceof MetaEss e) {
				if (symmetricMode) {
					// Symmetric Mode
					for (var pwr : Pwr.values()) {
						result.add(createOneClusterConstraint(coefficients, e, SingleOrAllPhase.ALL, pwr));
					}
				} else {
					// Asymmetric Mode
					for (var phase : SingleOrAllPhase.values()) {
						for (var pwr : Pwr.values()) {
							result.add(createOneClusterConstraint(coefficients, e, phase, pwr));
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
	 * @param phase        the {@link SingleOrAllPhase}
	 * @param pwr          the {@link Pwr}
	 * @return the {@link Constraint}
	 * @throws OpenemsException on error
	 */
	private static Constraint createOneClusterConstraint(Coefficients coefficients, MetaEss e, SingleOrAllPhase phase,
			Pwr pwr) throws OpenemsException {
		List<LinearCoefficient> cos = new ArrayList<>();
		cos.add(new LinearCoefficient(coefficients.of(e.id(), phase, pwr), 1));
		for (var subEssId : e.getEssIds()) {
			cos.add(new LinearCoefficient(coefficients.of(subEssId, phase, pwr), -1));
		}
		return new Constraint(e.id() + ": Sum of " + pwr.symbol + phase.symbol, cos, Relationship.EQUALS, 0);
	}
}
