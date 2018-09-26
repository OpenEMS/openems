package io.openems.edge.ess.core.power;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import com.google.common.collect.Streams;

import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.LinearCoefficient;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class Data {

	/**
	 * Holds all managed Ess objects
	 */
	private final Set<ManagedSymmetricEss> esss = new HashSet<>();

	/**
	 * Holds all inverters, Sorted by weight
	 */
	private final List<Inverter> inverters = new ArrayList<>();

	private final List<Constraint> constraints = new CopyOnWriteArrayList<>();
	private final Coefficients coefficients = new Coefficients();

	private boolean symmetricMode = PowerComponent.DEFAULT_SYMMETRIC_MODE;

	public synchronized void addEss(ManagedSymmetricEss ess) {
		this.esss.add(ess);
		// MetaEss has no explicit Inverters
		if (!(ess instanceof MetaEss)) {
			// create inverters and add them to list
			for (Inverter inverter : Inverter.of(ess, this.symmetricMode)) {
				this.inverters.add(inverter);
			}
			// Initially sort Inverters
			Data.invertersUpdateWeights(this.inverters);
			Data.invertersSortByWeights(this.inverters);
		}
		this.coefficients.initialize(this.esss);
	}

	public synchronized void removeEss(ManagedSymmetricEss ess) {
		// remove from Ess set
		this.esss.remove(ess);
		// remove from Inverters list
		Iterator<Inverter> iter = this.inverters.iterator();
		while (iter.hasNext()) {
			Inverter inverter = iter.next();
			if (Objects.equals(ess, inverter.getEss())) {
				iter.remove();
			}
		}
		this.coefficients.initialize(this.esss);
	}

	public void setSymmetricMode(boolean symmetricMode) {
		if (this.symmetricMode != symmetricMode) {
			this.symmetricMode = symmetricMode;
			this.initializeCycle(); // because SymmetricEssConstraints need to be renewed
		}
	}

	public List<Inverter> getInverters() {
		return Collections.unmodifiableList(this.inverters);
	}

	public Set<ManagedSymmetricEss> getEsss() {
		return Collections.unmodifiableSet(esss);
	}

	public synchronized void initializeCycle() {
		this.constraints.clear();
		// Update sorting of Inverters
		Data.invertersUpdateWeights(this.inverters);
		Data.invertersAdjustSortingByWeights(this.inverters);
	}

	public void addConstraint(Constraint constraint) {
		this.constraints.add(constraint);
	}

	public void removeConstraint(Constraint constraint) {
		this.constraints.remove(constraint);
	}

	public void addSimpleConstraint(String description, ManagedSymmetricEss ess, Phase phase, Pwr pwr, Relationship relationship,
			double value) {
		this.constraints.add(this.createSimpleConstraint(description, ess, phase, pwr, relationship, value));
	}

	public Coefficients getCoefficients() {
		return coefficients;
	}

	public Coefficient getCoefficient(ManagedSymmetricEss ess, Phase phase, Pwr pwr) {
		return this.coefficients.of(ess, phase, pwr);
	}

	/**
	 * Gets Constraints for all Inverters.
	 * 
	 * @return
	 */
	public List<Constraint> getConstraintsForAllInverters() {
		return this.getConstraintsWithoutDisabledInverters(Collections.emptyList());
	}

	/**
	 * Gets Constraints with the 'enabledInverters' only.
	 * 
	 * @param enabledInverters
	 * @return
	 */
	public List<Constraint> getConstraintsForInverters(Collection<Inverter> enabledInverters) {
		List<Inverter> disabledInverters = new ArrayList<>(this.inverters);
		disabledInverters.removeAll(enabledInverters);
		return getConstraintsWithoutDisabledInverters(disabledInverters);
	}

	/**
	 * Gets Constraints without the 'disabledInverters'.
	 * 
	 * @param disabledInverters
	 * @return
	 */
	public List<Constraint> getConstraintsWithoutDisabledInverters(Collection<Inverter> disabledInverters) {
		return Streams.concat(//
				this.createDisableConstraintsForInactiveInverters(disabledInverters).stream(),
				this.createGenericEssConstraints().stream(), //
				this.createStaticEssConstraints().stream(), //
				this.createClusterConstraints().stream(), //
				this.createSumOfPhasesConstraints(disabledInverters).stream(), //
				this.createSymmetricEssConstraints().stream(), //
				this.constraints.stream()).collect(Collectors.toList());
	}

	/**
	 * Creates for each disabled inverter an EQUALS ZERO constraint
	 */
	public List<Constraint> createDisableConstraintsForInactiveInverters(Collection<Inverter> inverters) {
		List<Constraint> result = new ArrayList<>();
		for (Inverter inverter : inverters) {
			ManagedSymmetricEss ess = inverter.getEss();
			Phase phase = inverter.getPhase();
			for (Pwr pwr : Pwr.values()) {
				result.add(this.createSimpleConstraint(ess.id() + ": Disable " + pwr.getSymbol() + phase.getSymbol(),
						ess, phase, pwr, Relationship.EQUALS, 0));
			}
		}
		return result;
	}

	/**
	 * Creates for each Ess constraints for AllowedCharge, AllowedDischarge and
	 * MaxApparentPower
	 */
	public List<Constraint> createGenericEssConstraints() {
		List<Constraint> result = new ArrayList<>();
		for (ManagedSymmetricEss ess : this.esss) {
			if(ess instanceof MetaEss) {
				// ignore
				continue;
			}
			
			Optional<Integer> allowedCharge = ess.getAllowedCharge().value().asOptional();
			if (allowedCharge.isPresent()) {
				result.add(this.createSimpleConstraint(ess.id() + ": Allowed Charge", ess, Phase.ALL, Pwr.ACTIVE,
						Relationship.GREATER_OR_EQUALS, allowedCharge.get()));
			}
			Optional<Integer> allowedDischarge = ess.getAllowedDischarge().value().asOptional();
			if (allowedDischarge.isPresent()) {
				result.add(this.createSimpleConstraint(ess.id() + ": Allowed Discharge", ess, Phase.ALL, Pwr.ACTIVE,
						Relationship.LESS_OR_EQUALS, allowedDischarge.get()));
			}
			Optional<Integer> maxApparentPower = ess.getMaxApparentPower().value().asOptional();
			if (maxApparentPower.isPresent()) {
				if (ess instanceof ManagedAsymmetricEss && !this.symmetricMode) {
					double maxApparentPowerPerPhase = maxApparentPower.get() / 3d;
					for (Phase phase : Phase.values()) {
						if (phase == Phase.ALL) {
							continue; // do not add Max Apparent Power Constraint for ALL phases
						}
						result.add(this.createSimpleConstraint(ess.id() + phase.getSymbol() + ": Max Apparent Power",
								ess, phase, Pwr.ACTIVE, Relationship.GREATER_OR_EQUALS, maxApparentPowerPerPhase * -1));
						result.add(this.createSimpleConstraint(ess.id() + phase.getSymbol() + ": Max Apparent Power",
								ess, phase, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS, maxApparentPowerPerPhase));
						result.add(this.createSimpleConstraint(ess.id() + phase.getSymbol() + ": Max Apparent Power",
								ess, phase, Pwr.REACTIVE, Relationship.EQUALS, 0));
					}
				} else {
					result.add(this.createSimpleConstraint(ess.id() + ": Max Apparent Power", ess, Phase.ALL,
							Pwr.ACTIVE, Relationship.GREATER_OR_EQUALS, maxApparentPower.get() * -1));
					result.add(this.createSimpleConstraint(ess.id() + ": Max Apparent Power", ess, Phase.ALL,
							Pwr.ACTIVE, Relationship.LESS_OR_EQUALS, maxApparentPower.get()));
					result.add(this.createSimpleConstraint(ess.id() + ": Max Apparent Power", ess, Phase.ALL,
							Pwr.REACTIVE, Relationship.EQUALS, 0));
					// TODO add circular constraint for ReactivePower
				}
			}
		}
		return result;
	}

	/**
	 * Asks each Ess if it has any static Constraints and adds them
	 * 
	 * @return
	 */
	public List<Constraint> createStaticEssConstraints() {
		List<Constraint> result = new ArrayList<>();
		for (ManagedSymmetricEss ess : this.esss) {
			for (Constraint c : ess.getStaticConstraints()) {
				result.add(c);
			}
		}
		return result;
	}

	/**
	 * Creates Constraints for Cluster, e.g. ClusterL1 = ess1_L1 + ess2_L1 + ...
	 * 
	 * @return
	 */
	public List<Constraint> createClusterConstraints() {
		List<Constraint> result = new ArrayList<>();
		for (ManagedSymmetricEss ess : this.esss) {
			if (ess instanceof MetaEss) {
				MetaEss e = (MetaEss) ess;
				for (Phase phase : Phase.values()) {
					for (Pwr pwr : Pwr.values()) {
						// creates a constraint of the form
						// 1*sumL1 - 1*ess1_L1 - 1*ess2_L1 = 0
						List<LinearCoefficient> cos = new ArrayList<>();
						cos.add(new LinearCoefficient(this.coefficients.of(ess, phase, pwr), 1));
						for (ManagedSymmetricEss subEss : e.getEsss()) {
							if(!subEss.isEnabled()) {
								// ignore disabled Sub-ESS
								continue;
							}
							cos.add(new LinearCoefficient(this.coefficients.of(subEss, phase, pwr), -1));
						}
						Constraint c = new Constraint(ess.id() + ": Sum of " + pwr.getSymbol() + phase.getSymbol(), cos,
								Relationship.EQUALS, 0);
						result.add(c);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Creates Constraints for Three-Phased Ess: P = L1 + L2 + L3
	 * 
	 * @return
	 */
	public List<Constraint> createSumOfPhasesConstraints(Collection<Inverter> disabledInverters) {
		List<Constraint> result = new ArrayList<>();
		for (ManagedSymmetricEss ess : this.esss) {
			// deactivate disabled Inverters if they are part of this Ess
			boolean addL1 = true;
			boolean addL2 = true;
			boolean addL3 = true;
			for (Inverter inverter : disabledInverters) {
				if (Objects.equals(ess, inverter.getEss())) {
					switch (inverter.getPhase()) {
					case ALL:
						break;
					case L1:
						addL1 = false;
						break;
					case L2:
						addL2 = false;
						break;
					case L3:
						addL3 = false;
						break;
					}
				}
			}
			if (addL1 || addL2 || addL3) {
				for (Pwr pwr : Pwr.values()) {
					// creates two constraint of the form
					// 1*P - 1*L1 - 1*L2 - 1*L3 = 0
					// 1*Q - 1*L1 - 1*L2 - 1*L3 = 0
					List<LinearCoefficient> cos = new ArrayList<>();
					cos.add(new LinearCoefficient(this.coefficients.of(ess, Phase.ALL, pwr), 1));
					if (addL1) {
						cos.add(new LinearCoefficient(this.coefficients.of(ess, Phase.L1, pwr), -1));
					}
					if (addL2) {
						cos.add(new LinearCoefficient(this.coefficients.of(ess, Phase.L2, pwr), -1));
					}
					if (addL3) {
						cos.add(new LinearCoefficient(this.coefficients.of(ess, Phase.L3, pwr), -1));
					}
					result.add(new Constraint(ess.id() + ": " + pwr.getSymbol() + "=L1+L2+L3",
							cos.toArray(new LinearCoefficient[cos.size()]), Relationship.EQUALS, 0));
				}
			}
		}
		return result;
	}

	/**
	 * Creates Constraints for SymmetricEss, e.g. L1 = L2 = L3
	 * 
	 * @return
	 */
	public List<Constraint> createSymmetricEssConstraints() {
		List<Constraint> result = new ArrayList<>();
		for (Inverter inverter : this.inverters) {
			if (inverter != null && inverter.getPhase() == Phase.ALL) {
				ManagedSymmetricEss ess = inverter.getEss();
				for (Pwr pwr : Pwr.values()) {
					// creates two constraint of the form
					// 1*L1 - 1*L2 = 0
					// 1*L1 - 1*L3 = 0
					result.add(new Constraint(ess.id() + ": Symmetric L1/L2", new LinearCoefficient[] { //
							new LinearCoefficient(this.coefficients.of(ess, Phase.L1, pwr), 1), //
							new LinearCoefficient(this.coefficients.of(ess, Phase.L2, pwr), -1) //
					}, Relationship.EQUALS, 0));
					result.add(new Constraint(ess.id() + ": Symmetric L1/L3", new LinearCoefficient[] { //
							new LinearCoefficient(this.coefficients.of(ess, Phase.L1, pwr), 1), //
							new LinearCoefficient(this.coefficients.of(ess, Phase.L3, pwr), -1) //
					}, Relationship.EQUALS, 0));
				}
			}
		}
		return result;
	}

	/**
	 * Creates Constraints for Sum of P > 0
	 * 
	 * @return
	 */
	public List<Constraint> createPGreaterThan0Constraints() {
		List<Constraint> result = new ArrayList<>();
		List<LinearCoefficient> cos = new ArrayList<>();
		for (Inverter inverter : this.inverters) {
			ManagedSymmetricEss ess = inverter.getEss();
			for (Phase phase : Phase.values()) {
				cos.add(new LinearCoefficient(this.coefficients.of(ess, phase, Pwr.ACTIVE), 1));

			}
		}
		Constraint c = new Constraint("Sum of P > 0", cos, Relationship.GREATER_OR_EQUALS, 0);
		result.add(c);
		return result;
	}

	/**
	 * Creates a simple Constraint with only one Coefficient.
	 * 
	 * @param ess
	 * @param phase
	 * @param pwr
	 * @param relationship
	 * @param value
	 * @return
	 */
	public Constraint createSimpleConstraint(String description, ManagedSymmetricEss ess, Phase phase, Pwr pwr,
			Relationship relationship, double value) {
		return new Constraint(description, //
				new LinearCoefficient[] { //
						new LinearCoefficient(this.coefficients.of(ess, phase, pwr), 1) //
				}, relationship, //
				value);
	}

	/**
	 * Sets the weight of each Inverter according to the SoC of its ESS
	 */
	public static void invertersUpdateWeights(List<Inverter> inverters) {
		for (Inverter inverter : inverters) {
			inverter.weight = inverter.getEss().getSoc().value().orElse(50);
		}
	}

	/**
	 * Sorts the list of Inverters by their weights
	 */
	public static void invertersSortByWeights(List<Inverter> inverters) {
		Collections.sort(inverters, (e1, e2) -> {
			// first: sort by weight
			int weightCompare = Integer.compare(e2.weight, e1.weight);
			if (weightCompare != 0) {
				return weightCompare;
			}
			// second: sort by name
			return e1.toString().compareTo(e2.toString());
		});
	}

	/**
	 * Adjust the sorting of Inverters by weights.
	 * 
	 * This is different to 'invertersSortByWeights()' in that it tries to avoid
	 * resorting the entire list all the time. Instead it only adjusts the list
	 * slightly.
	 * 
	 * @param wrapper2
	 */
	public static void invertersAdjustSortingByWeights(List<Inverter> inverters) {
		for (int i = 0; i < inverters.size() - 1; i++) {
			for (int j = i; j < inverters.size() - 1; j++) {
				int weight1 = inverters.get(j).weight;
				int weight2 = inverters.get(j + 1).weight;
				if (weight1 * SORT_FACTOR < weight2) {
					Collections.swap(inverters, j, j + 1);
				}
			}
		}
	}

	private final static float SORT_FACTOR = 1.3f;
}
