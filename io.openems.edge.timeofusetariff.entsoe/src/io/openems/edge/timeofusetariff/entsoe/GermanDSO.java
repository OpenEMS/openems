package io.openems.edge.timeofusetariff.entsoe;

import static io.openems.edge.timeofusetariff.entsoe.AncillaryCosts.GridFee.Tariff.HIGH;
import static io.openems.edge.timeofusetariff.entsoe.AncillaryCosts.GridFee.Tariff.LOW;
import static io.openems.edge.timeofusetariff.entsoe.AncillaryCosts.GridFee.Tariff.STANDARD;

import io.openems.edge.timeofusetariff.entsoe.AncillaryCosts.GridFee;

//CHECKSTYLE:OFF
public enum GermanDSO {
	// CHECKSTYLE:ON

	BAYERNWERK(GridFee.create() //
			.addDateRange(dr -> dr //
					.setStart(2025, 4, 1) //
					.setEnd(2025, 9, 30) //
					.setStandardTariff(8.75) //
					.addTimeRange(tr -> tr //
							.setFullDay() //
							.setTariff(STANDARD))) //
			.addDateRange(dr -> dr //
					.setStart(2025, 10, 1) //
					.setEnd(2025, 12, 31) //
					.setLowTariff(0.88) //
					.setHighTariff(11.58) //
					.addTimeRange(tr -> tr //
							.setStart(0, 0) //
							.setEnd(5, 0) //
							.setTariff(LOW)) //
					.addTimeRange(tr -> tr //
							.setStart(17, 0) //
							.setEnd(21, 0) //
							.setTariff(HIGH))) //
	);

	public final GridFee gridFee;

	private GermanDSO(GridFee.Builder gridFeeBuilder) {
		this.gridFee = gridFeeBuilder.build();
	}
}