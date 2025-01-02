package io.openems.edge.energy.api.simulation;

import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;

public enum Coefficient {
	/* _sum/Production; positive */
	PROD,
	/* _sum/Consumption; positive */
	CONS,
	/* _sum/EssActivePower; charge negative; discharge positive */
	ESS,
	/* _sum/EssActivePower; sell-to-grid negative, buy-from-grid positive */
	GRID,
	/* Production -> Consumption, positive */
	PROD_TO_CONS,
	/* Production -> Grid, positive */
	PROD_TO_GRID,
	/* Production -> ESS, positive */
	PROD_TO_ESS,
	/* Grid -> Consumption, positive */
	GRID_TO_CONS,
	/* ESS -> Consumption, positive */
	ESS_TO_CONS,
	/* Grid -> ESS, discharge-to-grid negative, charge-from-grid positive */
	GRID_TO_ESS;

	/**
	 * Gets the {@link Coefficient#name()} in CamelCase.
	 * 
	 * @return name
	 */
	public String toCamelCase() {
		return UPPER_UNDERSCORE.to(UPPER_CAMEL, this.name());
	}
}