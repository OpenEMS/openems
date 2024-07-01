package io.openems.edge.ess.core.power.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.optim.PointValuePair;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.IntUtils;
import io.openems.common.utils.IntUtils.Round;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.core.power.solver.PowerTuple;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.Pwr;

public class InverterPrecision {

	/**
	 * Rounds each solution value to the Inverter precision; following this logic.
	 *
	 * <p>
	 * On Discharge (Power &gt; 0)
	 *
	 * <ul>
	 * <li>if SoC &gt; 50 %: round up (more discharge)
	 * <li>if SoC &lt; 50 %: round down (less discharge)
	 * </ul>
	 *
	 * <p>
	 * On Charge (Power &lt; 0)
	 *
	 * <ul>
	 * <li>if SoC &gt; 50 %: round down (less charge)
	 * <li>if SoC &lt; 50 %: round up (more discharge)
	 * </ul>
	 *
	 * @param coefficients    the {@link Coefficients}
	 * @param allInverters    all {@link Inverter}s
	 * @param esss            all {@link ManagedSymmetricEss}s
	 * @param solution        a {@link PointValuePair} solution
	 * @param targetDirection the {@link TargetDirection}
	 * @return a map of inverters to PowerTuples
	 * @throws OpenemsException on error
	 */
	// TODO: round value of one inverter, apply constraint, repeat... to further
	// optimize this
	public static Map<Inverter, PowerTuple> apply(Coefficients coefficients, List<Inverter> allInverters,
			List<ManagedSymmetricEss> esss, PointValuePair solution, TargetDirection targetDirection)
			throws OpenemsException {
		Map<Inverter, PowerTuple> result = new HashMap<>();
		var point = solution.getPoint();
		for (Inverter inv : allInverters) {
			var round = Round.TOWARDS_ZERO;
			var essId = inv.getEssId();
			var ess = getEss(esss, essId);
			int soc = ess.getSoc().orElse(0);
			var precision = ess.getPowerPrecision();
			var powerTuple = new PowerTuple();
			for (Pwr pwr : Pwr.values()) {
				var c = coefficients.of(essId, inv.getPhase(), pwr);
				var value = point[c.getIndex()];
				if (value > 0 && soc > 50 || value < 0 && soc < 50) {
					round = Round.AWAY_FROM_ZERO;
				}
				var roundedValue = IntUtils.roundToPrecision((float) value, round, precision);
				if (roundedValue == -1 || roundedValue == 1) {
					roundedValue = 0; // avoid unnecessary power settings on rounding 0.xxx to 1
				}
				powerTuple.setValue(pwr, roundedValue);
			}
			result.put(inv, powerTuple);
		}
		return result;
	}

	private static ManagedSymmetricEss getEss(List<ManagedSymmetricEss> esss, String essId) {
		for (ManagedSymmetricEss ess : esss) {
			if (essId.equals(ess.id())) {
				return ess;
			}
		}
		return null;
	}

}
