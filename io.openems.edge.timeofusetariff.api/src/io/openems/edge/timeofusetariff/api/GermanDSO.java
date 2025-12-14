package io.openems.edge.timeofusetariff.api;

import static io.openems.edge.timeofusetariff.api.AncillaryCosts.GridFee.Tariff.HIGH;
import static io.openems.edge.timeofusetariff.api.AncillaryCosts.GridFee.Tariff.LOW;
import static io.openems.edge.timeofusetariff.api.AncillaryCosts.GridFee.Tariff.STANDARD;
import static java.time.LocalTime.MIDNIGHT;
import static java.time.LocalTime.MIN;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import io.openems.edge.timeofusetariff.api.AncillaryCosts.GridFee;
import io.openems.edge.timeofusetariff.api.AncillaryCosts.GridFee.Tariff;

//CHECKSTYLE:OFF
public enum GermanDSO {
	// CHECKSTYLE:ON

	BAYERNWERK(GridFee.create()//
			.addDateRange(dr -> dr// 
			// 2025 Q1/Q4
					.setStart(2025, 10, 1)//
					.setEnd(2025, 12, 31)//
					.setLowTariff(0.88)//
					.setStandardTariff(8.75)//
					.setHighTariff(11.58)//
					.addTimeRange(tr -> tr//
							.setStart(0, 0)//
							.setEnd(5, 0)//
							.setTariff(LOW))//
					.addTimeRange(tr -> tr//
							.setStart(5, 0)//
							.setEnd(17, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(17, 0)//
							.setEnd(21, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(21, 0)//
							.setEnd(0, 0)//
							.setTariff(STANDARD)))

			.addDateRange(dr -> dr// 
			// 2026 Q1
					.setStart(2026, 1, 1)//
					.setEnd(2026, 3, 31)//
					.setStandardTariff(5.62)//
					.addTimeRange(tr -> tr//
							.setFullDay()//
							.setTariff(STANDARD)))//
			.addDateRange(dr -> dr// 
			// 2026 Q2/Q3
					.setStart(2026, 4, 1)//
					.setEnd(2026, 9, 30)//
					.setLowTariff(0.56)//
					.setStandardTariff(5.62)//
					.setHighTariff(10.75)//
					.addTimeRange(tr -> tr//
							.setStart(0, 0)//
							.setEnd(10, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(10, 0)//
							.setEnd(15, 0)//
							.setTariff(LOW))//
					.addTimeRange(tr -> tr//
							.setStart(15, 0)//
							.setEnd(17, 0)//
							.setTariff(STANDARD))
					.addTimeRange(tr -> tr//
							.setStart(17, 0)//
							.setEnd(22, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(22, 0)//
							.setEnd(0, 0)//
							.setTariff(STANDARD)))//
			.addDateRange(dr -> dr// 
			// 2026 Q4
					.setStart(2026, 10, 1)//
					.setEnd(2026, 12, 31)//
					.setStandardTariff(5.62)//
					.addTimeRange(tr -> tr//
							.setFullDay()//
							.setTariff(STANDARD)))//
	),

	NETZE_BW(GridFee.create()//
			.addDateRange(dr -> dr//
			// 2025 (full)
					.setStart(2025, 4, 1)//
					.setEnd(2025, 12, 31)//
					.setLowTariff(4.63)//
					.setStandardTariff(11.58)//
					.setHighTariff(17.09)//
					.addTimeRange(tr -> tr//
							.setStart(0, 0)//
							.setEnd(10, 0)//
							.setTariff(STANDARD))
					.addTimeRange(tr -> tr//
							.setStart(10, 0)//
							.setEnd(14, 0)//
							.setTariff(LOW))//
					.addTimeRange(tr -> tr//
							.setStart(14, 0)//
							.setEnd(17, 0)//
							.setTariff(STANDARD))
					.addTimeRange(tr -> tr//
							.setStart(17, 0)//
							.setEnd(22, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(22, 0)//
							.setEnd(0, 0)//
							.setTariff(STANDARD)))

			.addDateRange(dr -> dr//
			// 2026 (full)
					.setStart(2026, 1, 1)//
					.setEnd(2026, 12, 31)//
					.setLowTariff(3.61)//
					.setStandardTariff(9.01)//
					.setHighTariff(13.16)//
					.addTimeRange(tr -> tr//
							.setStart(0, 0)//
							.setEnd(10, 0)//
							.setTariff(STANDARD))
					.addTimeRange(tr -> tr//
							.setStart(10, 0)//
							.setEnd(14, 0)//
							.setTariff(LOW))//
					.addTimeRange(tr -> tr//
							.setStart(14, 0)//
							.setEnd(17, 0)//
							.setTariff(STANDARD))
					.addTimeRange(tr -> tr//
							.setStart(17, 0)//
							.setEnd(22, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(22, 0)//
							.setEnd(0, 0)//
							.setTariff(STANDARD)))//
	),

	EWE_NETZ(GridFee.create()//
			.addDateRange(dr -> dr//
			// 2025 (full)
					.setStart(2025, 4, 1)//
					.setEnd(2025, 12, 31)//
					.setLowTariff(0.49)//
					.setStandardTariff(4.89)//
					.setHighTariff(8.59)//
					.addTimeRange(tr -> tr//
							.setStart(5, 0)//
							.setEnd(17, 30)//
							.setTariff(STANDARD))
					.addTimeRange(tr -> tr//
							.setStart(17, 30)//
							.setEnd(20, 30)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(20, 30)//
							.setEnd(23, 0)//
							.setTariff(STANDARD))
					.addTimeRange(tr -> tr//
							.setStart(23, 0)//
							.setEnd(5, 0)//
							.setTariff(LOW)))//

			.addDateRange(dr -> dr//
			// 2026 (full)
					.setStart(2025, 4, 1)//
					.setEnd(2025, 12, 31)//
					.setLowTariff(0.32)//
					.setStandardTariff(3.20)//
					.setHighTariff(5.62)//
					.addTimeRange(tr -> tr//
							.setStart(5, 0)//
							.setEnd(17, 30)//
							.setTariff(STANDARD))
					.addTimeRange(tr -> tr//
							.setStart(17, 30)//
							.setEnd(20, 30)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(20, 30)//
							.setEnd(23, 0)//
							.setTariff(STANDARD))
					.addTimeRange(tr -> tr//
							.setStart(23, 0)//
							.setEnd(5, 0)//
							.setTariff(LOW)))//
	),

	MIT_NETZ(GridFee.create()//
			.addDateRange(dr -> dr//
			// 2025 Q4
					.setStart(2025, 10, 1)//
					.setEnd(2025, 12, 31)//
					.setLowTariff(0.99)//
					.setStandardTariff(8.95)//
					.setHighTariff(17.90)//
					.addTimeRange(tr -> tr//
							.setStart(0, 0)//
							.setEnd(3, 0)//
							.setTariff(LOW))//
					.addTimeRange(tr -> tr//
							.setStart(3, 0)//
							.setEnd(8, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(8, 0)//
							.setEnd(12, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(12, 0)//
							.setEnd(17, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(17, 0)//
							.setEnd(19, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(19, 0)//
							.setEnd(0, 0)//
							.setTariff(LOW)))//

			.addDateRange(dr -> dr//
			// 2026 Q1
					.setStart(2026, 1, 1)//
					.setEnd(2026, 3, 31)//
					.setLowTariff(0.82)//
					.setStandardTariff(7.51)//
					.setHighTariff(15.02)//
					.addTimeRange(tr -> tr//
							.setStart(0, 0)//
							.setEnd(3, 0)//
							.setTariff(LOW))//
					.addTimeRange(tr -> tr//
							.setStart(3, 0)//
							.setEnd(8, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(8, 0)//
							.setEnd(12, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(12, 0)//
							.setEnd(17, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(17, 0)//
							.setEnd(19, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(19, 0)//
							.setEnd(0, 0)//
							.setTariff(LOW)))//
			.addDateRange(dr -> dr//
			// 2026 Q2/Q3
					.setStart(2026, 4, 1)//
					.setEnd(2026, 9, 30)//
					.setStandardTariff(7.51)//
					.addTimeRange(tr -> tr//
							.setFullDay()//
							.setTariff(LOW)))//
			.addDateRange(dr -> dr//
			// 2026 Q4
					.setStart(2026, 10, 1)//
					.setEnd(2026, 12, 31)//
					.setLowTariff(0.82)//
					.setStandardTariff(7.51)//
					.setHighTariff(15.02)//
					.addTimeRange(tr -> tr//
							.setStart(0, 0)//
							.setEnd(3, 0)//
							.setTariff(LOW))//
					.addTimeRange(tr -> tr//
							.setStart(3, 0)//
							.setEnd(8, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(8, 0)//
							.setEnd(12, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(12, 0)//
							.setEnd(17, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(17, 0)//
							.setEnd(19, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(19, 0)//
							.setEnd(0, 0)//
							.setTariff(LOW)))//
	),

	SH_NETZ(GridFee.create()//
			.addDateRange(dr -> dr//
			// 2025 Q4
					.setStart(2025, 10, 1)//
					.setEnd(2025, 12, 31)//
					.setLowTariff(1.05)//
					.setStandardTariff(10.41)//
					.setHighTariff(15.83)//
					.addTimeRange(tr -> tr//
							.setStart(0, 0)//
							.setEnd(4, 0)//
							.setTariff(LOW))//
					.addTimeRange(tr -> tr//
							.setStart(4, 0)//
							.setEnd(10, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(10, 0)//
							.setEnd(13, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(13, 0)//
							.setEnd(18, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(18, 0)//
							.setEnd(21, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(21, 0)//
							.setEnd(0, 0)//
							.setTariff(LOW)))//

			.addDateRange(dr -> dr//
			// 2026 Q1
					.setStart(2026, 1, 1)//
					.setEnd(2026, 3, 31)//
					.setLowTariff(0.76)//
					.setStandardTariff(7.62)//
					.setHighTariff(9.90)//
					.addTimeRange(tr -> tr//
							.setStart(0, 0)//
							.setEnd(5, 0)//
							.setTariff(LOW))//
					.addTimeRange(tr -> tr//
							.setStart(5, 0)//
							.setEnd(9, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(9, 0)//
							.setEnd(14, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(14, 0)//
							.setEnd(17, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(17, 0)//
							.setEnd(21, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(21, 0)//
							.setEnd(22, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(22, 0)//
							.setEnd(0, 0)//
							.setTariff(LOW)))//
			.addDateRange(dr -> dr//
			// 2026 Q2/Q3
					.setStart(2026, 1, 1)//
					.setEnd(2026, 3, 31)//
					.setStandardTariff(7.62)//
					.addTimeRange(tr -> tr//
							.setFullDay()//
							.setTariff(STANDARD)))//
			.addDateRange(dr -> dr//
			// 2026 Q4
					.setStart(2026, 1, 1)//
					.setEnd(2026, 3, 31)//
					.setLowTariff(0.76)//
					.setStandardTariff(7.62)//
					.setHighTariff(9.90)//
					.addTimeRange(tr -> tr//
							.setStart(0, 0)//
							.setEnd(5, 0)//
							.setTariff(LOW))//
					.addTimeRange(tr -> tr//
							.setStart(5, 0)//
							.setEnd(9, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(9, 0)//
							.setEnd(14, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(14, 0)//
							.setEnd(17, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(17, 0)//
							.setEnd(21, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(21, 0)//
							.setEnd(22, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(22, 0)//
							.setEnd(0, 0)//
							.setTariff(LOW)))//
	),

	WEST_NETZ(GridFee.create()//
			.addDateRange(dr -> dr//
			// 2025 (full)
					.setStart(2025, 4, 1)//
					.setEnd(2025, 12, 31)//
					.setLowTariff(1.19)//
					.setStandardTariff(11.88)//
					.setHighTariff(17.75)//
					.addTimeRange(tr -> tr//
							.setStart(0, 0)//
							.setEnd(6, 0)//
							.setTariff(LOW))
					.addTimeRange(tr -> tr//
							.setStart(6, 0)//
							.setEnd(15, 0)//
							.setTariff(STANDARD))
					.addTimeRange(tr -> tr//
							.setStart(15, 0)//
							.setEnd(20, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(20, 0)//
							.setEnd(0, 0)//
							.setTariff(STANDARD)))//

			.addDateRange(dr -> dr//
			// 2026 (full)
					.setStart(2026, 1, 1)//
					.setEnd(2026, 12, 31)//
					.setLowTariff(1.13)//
					.setStandardTariff(11.34)//
					.setHighTariff(18.62)//
					.addTimeRange(tr -> tr//
							.setStart(0, 0)//
							.setEnd(7, 0)//
							.setTariff(LOW))
					.addTimeRange(tr -> tr//
							.setStart(7, 0)//
							.setEnd(15, 0)//
							.setTariff(STANDARD))
					.addTimeRange(tr -> tr//
							.setStart(15, 0)//
							.setEnd(20, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(20, 0)//
							.setEnd(0, 0)//
							.setTariff(STANDARD)))//
	),

	E_DIS(GridFee.create()//
			.addDateRange(dr -> dr//
			// 2025 Q4
					.setStart(2025, 10, 1)//
					.setEnd(2025, 12, 31)//
					.setLowTariff(1.04)//
					.setStandardTariff(10.29)//
					.setHighTariff(16.35)//
					.addTimeRange(tr -> tr//
							.setStart(0, 0)//
							.setEnd(4, 45)//
							.setTariff(LOW))//
					.addTimeRange(tr -> tr//
							.setStart(5, 45)//
							.setEnd(11, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(11, 0)//
							.setEnd(12, 45)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(12, 45)//
							.setEnd(16, 45)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(16, 45)//
							.setEnd(20, 0)//
							.setTariff(HIGH))//
					.addTimeRange(tr -> tr//
							.setStart(20, 0)//
							.setEnd(23, 30)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(23, 30)//
							.setEnd(0, 0)//
							.setTariff(LOW)))//

			.addDateRange(dr -> dr//
			// 2026 Q1
					.setStart(2026, 1, 1)//
					.setEnd(2026, 3, 31)//
					.setLowTariff(0.65)//
					.setStandardTariff(6.50)//
					.setHighTariff(10.47)//
					.addTimeRange(tr -> tr//
							.setStart(0, 0)//
							.setEnd(5, 0)//
							.setTariff(LOW))//
					.addTimeRange(tr -> tr//
							.setStart(5, 0)//
							.setEnd(10, 15)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(10, 15)//
							.setEnd(12, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(12, 0)//
							.setEnd(16, 45)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(16, 45)//
							.setEnd(20, 15)//
							.setTariff(HIGH))//
					.addTimeRange(tr -> tr//
							.setStart(20, 15)//
							.setEnd(23, 30)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(23, 30)//
							.setEnd(0, 0)//
							.setTariff(LOW)))//
			.addDateRange(dr -> dr//
			// 2026 Q2/Q3
					.setStart(2026, 4, 1)//
					.setEnd(2026, 9, 30)//
					.setStandardTariff(6.50)//
					.addTimeRange(tr -> tr//
							.setFullDay()//
							.setTariff(STANDARD)))//
			.addDateRange(dr -> dr//
			// 2026 Q4
					.setStart(2026, 9, 1)//
					.setEnd(2026, 12, 31)//
					.setLowTariff(0.65)//
					.setStandardTariff(6.50)//
					.setHighTariff(10.47)//
					.addTimeRange(tr -> tr//
							.setStart(0, 0)//
							.setEnd(5, 0)//
							.setTariff(LOW))//
					.addTimeRange(tr -> tr//
							.setStart(5, 0)//
							.setEnd(10, 15)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(10, 15)//
							.setEnd(12, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(12, 0)//
							.setEnd(16, 45)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(16, 45)//
							.setEnd(20, 15)//
							.setTariff(HIGH))//
					.addTimeRange(tr -> tr//
							.setStart(20, 15)//
							.setEnd(23, 30)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(23, 30)//
							.setEnd(0, 0)//
							.setTariff(LOW)))//
	),

	AVACON(GridFee.create()//
			.addDateRange(dr -> dr//
			// 2025 Q4
					.setStart(2025, 10, 1)//
					.setEnd(2025, 12, 31)//
					.setLowTariff(1.08)//
					.setStandardTariff(10.79)//
					.setHighTariff(15.01)//
					.addTimeRange(tr -> tr//
							.setStart(0, 15)//
							.setEnd(5, 0)//
							.setTariff(LOW))//
					.addTimeRange(tr -> tr//
							.setStart(5, 0)//
							.setEnd(16, 30)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(16, 30)//
							.setEnd(21, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(21, 0)//
							.setEnd(23, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(23, 0)//
							.setEnd(0, 15)//
							.setTariff(LOW)))//

			.addDateRange(dr -> dr//
			// 2026 Q1
					.setStart(2026, 1, 1)//
					.setEnd(2026, 3, 31)//
					.setLowTariff(0.71)//
					.setStandardTariff(7.19)//
					.setHighTariff(10.01)//
					.addTimeRange(tr -> tr//
							.setStart(0, 15)//
							.setEnd(5, 0)//
							.setTariff(LOW))//
					.addTimeRange(tr -> tr//
							.setStart(5, 0)//
							.setEnd(16, 30)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(16, 30)//
							.setEnd(21, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(21, 0)//
							.setEnd(23, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(23, 0)//
							.setEnd(0, 15)//
							.setTariff(LOW)))//
			.addDateRange(dr -> dr//
			// 2026 Q2/Q3
					.setStart(2026, 4, 1)//
					.setEnd(2026, 9, 30)//
					.setStandardTariff(7.19)//
					.addTimeRange(tr -> tr//
							.setFullDay()//
							.setTariff(STANDARD)))//
			.addDateRange(dr -> dr//
			// 2026 Q4
					.setStart(2025, 10, 1)//
					.setEnd(2025, 12, 31)//
					.setLowTariff(0.71)//
					.setStandardTariff(7.19)//
					.setHighTariff(10.01)//
					.addTimeRange(tr -> tr//
							.setStart(0, 15)//
							.setEnd(5, 0)//
							.setTariff(LOW))//
					.addTimeRange(tr -> tr//
							.setStart(5, 0)//
							.setEnd(16, 30)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(16, 30)//
							.setEnd(21, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(21, 0)//
							.setEnd(23, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(23, 0)//
							.setEnd(0, 15)//
							.setTariff(LOW)))//
	),

	LEW(GridFee.create()//
			.addDateRange(dr -> dr//
			// 2025 (full)
					.setStart(2025, 4, 1)//
					.setEnd(2025, 12, 31)//
					.setLowTariff(0.70)//
					.setStandardTariff(6.99)//
					.setHighTariff(13.88)//
					.addTimeRange(tr -> tr//
							.setStart(0, 0)//
							.setEnd(10, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(10, 0)//
							.setEnd(15, 0)//
							.setTariff(LOW))//
					.addTimeRange(tr -> tr//
							.setStart(15, 0)//
							.setEnd(17, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(17, 0)//
							.setEnd(21, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(21, 0)//
							.setEnd(0, 0)//
							.setTariff(STANDARD)))//

			.addDateRange(dr -> dr//
			// 2026 (full)
					.setStart(2026, 1, 1)//
					.setEnd(2026, 12, 31)//
					.setLowTariff(0.49)//
					.setStandardTariff(4.87)//
					.setHighTariff(9.63)//
					.addTimeRange(tr -> tr//
							.setStart(0, 0)//
							.setEnd(10, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(10, 0)//
							.setEnd(15, 0)//
							.setTariff(LOW))//
					.addTimeRange(tr -> tr//
							.setStart(15, 0)//
							.setEnd(17, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(17, 0)//
							.setEnd(21, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(21, 0)//
							.setEnd(0, 0)//
							.setTariff(STANDARD)))//
	),

	TE_NETZE(GridFee.create()//
			.addDateRange(dr -> dr//
					.setStart(2025, 4, 1)//
					.setEnd(2025, 9, 30)//
					.setStandardTariff(8.81)//
					.addTimeRange(tr -> tr//
							.setFullDay()//
							.setTariff(STANDARD)))//
			.addDateRange(dr -> dr//
					.setStart(2025, 10, 1)//
					.setEnd(2025, 12, 31)//
					.setStandardTariff(8.81)//
					.setLowTariff(3.52)//
					.setHighTariff(11.86)//
					.addTimeRange(tr -> tr//
							.setStart(1, 30)//
							.setEnd(4, 30)//
							.setTariff(LOW))//
					.addTimeRange(tr -> tr//
							.setStart(4, 30)//
							.setEnd(11, 30)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(11, 30)//
							.setEnd(13, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(13, 0)//
							.setEnd(18, 30)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(18, 30)//
							.setEnd(20, 30)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(20, 30)//
							.setEnd(22, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(22, 0)//
							.setEnd(23, 0)//
							.setTariff(HIGH))
					.addTimeRange(tr -> tr//
							.setStart(23, 0)//
							.setEnd(1, 30)//
							.setTariff(STANDARD)))),

	NETZE_ODR(GridFee.create()//
			.addDateRange(dr -> dr//
					.setStart(2025, 1, 1)//
					.setEnd(2025, 3, 31)//
					.setStandardTariff(7.63)//
					.addTimeRange(tr -> tr//
							.setFullDay()//
							.setTariff(STANDARD)))//
			.addDateRange(dr -> dr//
					.setStart(2025, 4, 1)//
					.setEnd(2025, 9, 30)//
					.setLowTariff(3.05)//
					.setStandardTariff(7.63)//
					.setHighTariff(13.23)//
					.addTimeRange(tr -> tr//
							.setStart(0, 0)//
							.setEnd(5, 0)//
							.setTariff(HIGH))//
					.addTimeRange(tr -> tr//
							.setStart(5, 0)//
							.setEnd(11, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(11, 0)//
							.setEnd(17, 0)//
							.setTariff(LOW))//
					.addTimeRange(tr -> tr//
							.setStart(17, 0)//
							.setEnd(22, 0)//
							.setTariff(STANDARD))//
					.addTimeRange(tr -> tr//
							.setStart(22, 0)//
							.setEnd(0, 0)//
							.setTariff(HIGH)))//
			.addDateRange(dr -> dr//
					.setStart(2025, 10, 1)//
					.setEnd(2025, 12, 31)//
					.setStandardTariff(7.63)//
					.addTimeRange(tr -> tr//
							.setFullDay()//
							.setTariff(STANDARD)))//
	);

	private static final ZoneId GERMAN_ZONE_ID = ZoneId.of("Europe/Berlin");

	public final GridFee gridFee;

	private GermanDSO(GridFee.Builder gridFeeBuilder) {
		this.gridFee = gridFeeBuilder.build();
	}

	/**
	 * Gets the grid fee price for a specific date and time.
	 *
	 * @param dateTime the date and time for the lookup.
	 * @return the price in ct/kWh.
	 * @throws IllegalStateException if no matching date range is configured for the
	 *                               given dateTime.
	 */
	public double getPriceAt(ZonedDateTime dateTime) {
		final var germanDateTime = dateTime.withZoneSameInstant(GERMAN_ZONE_ID);

		// Find the date range that contains the given dateTime
		var matchingDateRangeOpt = this.gridFee.dateRanges().stream()
				.filter(dr -> !germanDateTime.toLocalDate().isBefore(dr.start()) //
						&& !germanDateTime.toLocalDate().isAfter(dr.end()))
				.findFirst();

		if (!matchingDateRangeOpt.isPresent()) {
			throw new IllegalStateException(
					"No matching date range found for " + germanDateTime + " in DSO " + this.name());
		}

		var dateRange = matchingDateRangeOpt.get();
		final var time = germanDateTime.toLocalTime();

		// Find the time range within that date range that contains the given time
		var matchingTimeRangeOpt = dateRange.timeRanges().stream().filter(tr -> {
			var start = tr.start();
			var end = tr.end();

			// Special case for full-day ranges defined by the builder as MIN to MIDNIGHT
			if (start.equals(MIN) && end.equals(MIDNIGHT)) {
				return true;
			}

			// Handle overnight ranges (e.g., 23:00 - 05:00)
			if (start.isAfter(end)) {
				return !time.isBefore(start) || time.isBefore(end);
			} else { // Handle same-day ranges (e.g., 08:00 - 17:00)
				return !time.isBefore(start) && time.isBefore(end);
			}
		}).findFirst();

		Tariff tariff;
		if (matchingTimeRangeOpt.isPresent()) {
			// A specific time range (LOW, STANDARD, HIGH) was found
			tariff = matchingTimeRangeOpt.get().tariff();
		} else {
			// If no specific time range matches, it's a gap, which is covered by the
			// standard tariff.
			tariff = STANDARD;
		}

		return switch (tariff) {
		case LOW -> dateRange.lowTariff();
		case HIGH -> dateRange.highTariff();
		case STANDARD -> dateRange.standardTariff();
		};
	}
}