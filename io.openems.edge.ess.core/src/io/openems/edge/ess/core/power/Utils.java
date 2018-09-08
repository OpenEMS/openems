package io.openems.edge.ess.core.power;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.openems.common.utils.IntUtils;
import io.openems.common.utils.IntUtils.Round;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.ConstraintType;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class Utils {

	/**
	 * TODO migrate this to new solver. The pError could be postive or negative according to this logic. 
	 * 
	 * Round values to accuracy of inverter; following this logic:
	 *
	 * On Discharge (Power > 0)
	 *
	 * <ul>
	 * <li>if SoC > 50 %: round up (more discharge)
	 * <li>if SoC < 50 %: round down (less discharge)
	 * </ul>
	 *
	 * On Charge (Power < 0)
	 *
	 * <ul>
	 * <li>if SoC > 50 %: round down (less charge)
	 * <li>if SoC < 50 %: round up (more discharge)
	 * </ul>
	 */
	public static int roundToInverterPrecision(ManagedSymmetricEss ess, double value) {
		Round round = Round.DOWN;
		int precision = ess.getPowerPrecision();
		int soc = ess.getSoc().value().orElse(0);

		if (value > 0 && soc > 50 || value < 0 && soc < 50) {
			round = Round.UP;
		}

		return IntUtils.roundToPrecision((float) value, round, precision);
	}

	/**
	 * Create a Simple Constraint
	 * 
	 * @param ess
	 * @param type
	 * @param phase
	 * @param pwr
	 * @param relationship
	 * @param value
	 * @return
	 */
	public static Constraint createSimpleConstraint(ManagedSymmetricEss ess, ConstraintType type, Phase phase, Pwr pwr,
			Relationship relationship, int value) {
		return new Constraint( //
				type, new Coefficient[] { //
						new Coefficient(ess, phase, pwr, 1) }, //
				relationship, //
				value);
	}
}
