package io.openems.edge.ess.core.power;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.math3.optim.linear.Relationship;

import com.google.common.collect.Streams;

import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.coefficient.Coefficient;
import io.openems.edge.ess.power.api.coefficient.Coefficients;
import io.openems.edge.ess.power.api.coefficient.LinearCoefficient;

public class Data {

	private final List<ManagedSymmetricEss> esss = new CopyOnWriteArrayList<>();
	private final List<Constraint> constraints = new CopyOnWriteArrayList<>();
	private final Coefficients coefficients = new Coefficients();

	private boolean symmetricMode = PowerComponent.DEFAULT_SYMMETRIC_MODE;

	public synchronized void addEss(ManagedSymmetricEss ess) {
		this.esss.add(ess);
		this.coefficients.initialize(esss);
	}

	public synchronized void removeEss(ManagedSymmetricEss ess) {
		this.esss.remove(ess);
		this.coefficients.initialize(esss);
	}

	public void setSymmetricMode(boolean symmetricMode) {
		if (this.symmetricMode != symmetricMode) {
			this.symmetricMode = symmetricMode;
			this.initializeCycle(); // because SymmetricEssConstraints need to be renewed
		}
	}

	public synchronized void initializeCycle() {
		this.constraints.clear();
		Streams.concat(//
				this.createGenericEssConstraints().stream(), //
				this.createStaticEssConstraints().stream(), //
				this.createClusterConstraints().stream(), //
				this.createSumOfPhasesConstraints().stream(), //
				this.createSymmetricEssConstraints().stream() //
		).forEach(c -> {
			this.constraints.add(c);
		});
	}

	public void addConstraint(Constraint constraint) {
		this.constraints.add(constraint);
	}

	public void removeConstraint(Constraint constraint) {
		this.constraints.remove(constraint);
	}

	public void addSimpleConstraint(ManagedSymmetricEss ess, Phase phase, Pwr pwr, Relationship relationship,
			double value) {
		this.constraints.add(this.createSimpleConstraint("", ess, phase, pwr, relationship, value));
	}

	public Coefficients getCoefficients() {
		return coefficients;
	}

	public Coefficient getCoefficient(ManagedSymmetricEss ess, Phase phase, Pwr pwr) {
		return this.coefficients.of(ess, phase, pwr);
	}

	public List<Constraint> getConstraints() {
		return Collections.unmodifiableList(this.constraints);
	}

	/**
	 * Creates for each Ess constraints for AllowedCharge, AllowedDischarge and
	 * MaxApparentPower
	 */
	public List<Constraint> createGenericEssConstraints() {
		List<Constraint> result = new ArrayList<>();
		for (ManagedSymmetricEss ess : this.esss) {
			Optional<Integer> allowedCharge = ess.getAllowedCharge().value().asOptional();
			if (allowedCharge.isPresent()) {
				result.add(this.createSimpleConstraint(ess.id() + ": Allowed Charge", ess, Phase.ALL, Pwr.ACTIVE,
						Relationship.GEQ, allowedCharge.get()));
			}
			Optional<Integer> allowedDischarge = ess.getAllowedDischarge().value().asOptional();
			if (allowedDischarge.isPresent()) {
				result.add(this.createSimpleConstraint(ess.id() + ": Allowed Discharge", ess, Phase.ALL, Pwr.ACTIVE,
						Relationship.LEQ, allowedDischarge.get()));
			}
			Optional<Integer> maxApparentPower = ess.getMaxApparentPower().value().asOptional();
			if (maxApparentPower.isPresent()) {
				result.add(this.createSimpleConstraint(ess.id() + ": Max Apparent Power", ess, Phase.ALL, Pwr.ACTIVE,
						Relationship.GEQ, maxApparentPower.get() * -1));
				result.add(this.createSimpleConstraint(ess.id() + ": Max Apparent Power", ess, Phase.ALL, Pwr.ACTIVE,
						Relationship.LEQ, maxApparentPower.get()));
				result.add(this.createSimpleConstraint(ess.id() + ": Max Apparent Power", ess, Phase.ALL, Pwr.REACTIVE,
						Relationship.EQ, 0));
				// TODO add circular constraint for ReactivePower
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
							cos.add(new LinearCoefficient(this.coefficients.of(subEss, phase, pwr), -1));
						}
						Constraint c = new Constraint(ess.id() + ": Sum of " + pwr.getSymbol() + phase.getSymbol(), cos,
								Relationship.EQ, 0);
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
	public List<Constraint> createSumOfPhasesConstraints() {
		List<Constraint> result = new ArrayList<>();
		for (ManagedSymmetricEss ess : this.esss) {
			for (Pwr pwr : Pwr.values()) {
				// creates two constraint of the form
				// 1*P - 1*L1 - 1*L2 - 1*L3 = 0
				// 1*Q - 1*L1 - 1*L2 - 1*L3 = 0
				result.add(new Constraint(ess.id() + ": " + pwr.getSymbol() + "=L1+L2+L3", new LinearCoefficient[] { //
						new LinearCoefficient(this.coefficients.of(ess, Phase.ALL, pwr), 1), //
						new LinearCoefficient(this.coefficients.of(ess, Phase.L1, pwr), -1), //
						new LinearCoefficient(this.coefficients.of(ess, Phase.L2, pwr), -1), //
						new LinearCoefficient(this.coefficients.of(ess, Phase.L3, pwr), -1), //
				}, Relationship.EQ, 0));
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
		for (ManagedSymmetricEss ess : this.esss) {
			if (ess instanceof ManagedAsymmetricEss && this.symmetricMode == false) {
				continue;
			}
			for (Pwr pwr : Pwr.values()) {
				// creates two constraint of the form
				// 1*L1 - 1*L2 = 0
				// 1*L1 - 1*L3 = 0
				result.add(new Constraint(ess.id() + ": Symmetric L1/L2", new LinearCoefficient[] { //
						new LinearCoefficient(this.coefficients.of(ess, Phase.L1, pwr), 1), //
						new LinearCoefficient(this.coefficients.of(ess, Phase.L2, pwr), -1) //
				}, Relationship.EQ, 0));
				result.add(new Constraint(ess.id() + ": Symmetric L1/L3", new LinearCoefficient[] { //
						new LinearCoefficient(this.coefficients.of(ess, Phase.L1, pwr), 1), //
						new LinearCoefficient(this.coefficients.of(ess, Phase.L3, pwr), -1) //
				}, Relationship.EQ, 0));
			}
		}
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
}
