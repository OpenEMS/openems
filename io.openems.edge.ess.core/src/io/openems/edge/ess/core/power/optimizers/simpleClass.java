package io.openems.edge.ess.core.power.optimizers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.optim.PointValuePair;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.core.power.data.ConstraintUtil;
import io.openems.edge.ess.core.power.data.TargetDirection;
import io.openems.edge.ess.core.power.solver.ConstraintSolver;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class simpleClass {
	
	public static PointValuePair apply(Coefficients coefficients, TargetDirection targetDirection, //
			List<Inverter> allInverters, List<Inverter> targetInverters, //
			List<Constraint> allConstraints, List<Inverter> invs, //
			List<ManagedSymmetricEss> esss //
	) throws OpenemsException {
		
		
		
		List<Constraint> constraints = new ArrayList<>(allConstraints);
		
		System.out.println(allInverters);
		
		for (Inverter in : allInverters) {
		
			constraints.add(ConstraintUtil.createSimpleConstraint(coefficients, //
					in.toString() + ": pooran", //
					in.getEssId(), in.getPhase(), Pwr.ACTIVE, Relationship.EQUALS, -25000));
		}
		
		
		ConstraintSolver.solve(coefficients, constraints);
		
		
		
		return  null;
	}

}
