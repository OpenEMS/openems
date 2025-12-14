package io.openems.edge.timeofusetariff.api;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.edge.timeofusetariff.api.AncillaryCosts.GridFee.Tariff;
import io.openems.edge.timeofusetariff.api.AncillaryCosts.GridFee.TimeRange;

//CHECKSTYLE:OFF
public class GermanDSOTest {
	// CHECKSTYLE:ON

	@Test
	public void test() {
		for (var dso : GermanDSO.values()) {
			var gf = dso.gridFee;
			for (var dr : gf.dateRanges()) {
				// DateRange
				assertNotNull(dr.start());
				assertNotNull(dr.end());

				// TimeRanges
				assertFalse(dr.timeRanges().isEmpty());
				assertTariff(dr.timeRanges(), Tariff.LOW, dr.lowTariff());
				assertTariff(dr.timeRanges(), Tariff.STANDARD, dr.standardTariff());
				assertTariff(dr.timeRanges(), Tariff.HIGH, dr.highTariff());

				for (var i = 0; i < dr.timeRanges().size() - 1; i++) {
					var tr1 = dr.timeRanges().get(i);
					var tr2 = dr.timeRanges().get(i + 1);
					assertTrue("No Gaps:" + dso.name() + ":" + dr.start() + ":" + tr1.end() + "!=" + tr2.start(),
							tr1.end().equals(tr2.start()));
				}
			}

			// DateRanges
			for (var i = 0; i < gf.dateRanges().size() - 1; i++) {
				var dr1 = gf.dateRanges().get(i);
				var dr2 = gf.dateRanges().get(i + 1);
				assertTrue("No Gaps:" + dso.name() + ":" + dr1.end() + "!=" + dr2.start(),
						dr1.end().plusDays(1).equals(dr2.start()));
			}
		}
	}

	private static void assertTariff(ImmutableList<TimeRange> timeRanges, Tariff tariff, double value) {
		var anyMatch = timeRanges.stream().map(TimeRange::tariff).anyMatch(t -> t.equals(tariff));
		if (Double.isNaN(value) && anyMatch) {
			throw new IllegalArgumentException("Tariff [" + tariff.name() + "] value is missing");
		} else if (!Double.isNaN(value) && !anyMatch) {
			throw new IllegalArgumentException("Tariff [" + tariff.name() + "] value [" + value + "] is not used");
		}
	}
}
