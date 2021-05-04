package io.openems.controller.emsig.ojalgo;

import static io.openems.controller.emsig.ojalgo.Constants.ESS_MAX_ENERGY;
import static io.openems.controller.emsig.ojalgo.Constants.NO_OF_PERIODS;
import static java.math.BigDecimal.ONE;

import java.io.IOException;

public class OjalgoTest {

	public void run() throws IOException {
		int[] maxGridBuy = new int[NO_OF_PERIODS];
		int[] maxGridSell = new int[NO_OF_PERIODS];

		EnergyModel em = null;
		for (int i = 0; i < NO_OF_PERIODS; i++) {
			em = new EnergyModel();

			em.model.addExpression("End of 1st HLZ") //
					.set(em.periods[5].ess.energy, ONE) //
					.level(0);

			em.model.addExpression("Beginning of 2nd HLZ") //
					.set(em.periods[18].ess.energy, ONE) //
					.level(ESS_MAX_ENERGY * 60);

			em.model.addExpression("End of 2nd HLZ") //
					.set(em.periods[23].ess.energy, ONE) //
					.level(0);

			em.model.addExpression("Extreme Grid Power") //
					.set(em.periods[i].grid.power, ONE) //
					.weight(ONE);

			em.model.maximise();
			maxGridBuy[i] = em.periods[i].grid.buy.power.getValue().intValue();
			em.model.minimise();
			maxGridSell[i] = em.periods[i].grid.sell.power.getValue().intValue();
		}
		em.print();
	}
}
