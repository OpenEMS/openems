package io.openems.edge.controller.ess.timeofusetariff.discharge;

import java.time.ZonedDateTime;
import java.util.TreeMap;

import io.openems.edge.controller.ess.timeofusetariff.discharge.tariff.TimeOfUseTariff;

public class DummyTimeOfUseTariffProvider implements TimeOfUseTariff {

	private final ZonedDateTime now;

	public DummyTimeOfUseTariffProvider(ZonedDateTime now) {
		this.now = now;
	}

	@Override
	public TreeMap<ZonedDateTime, Float> getPrices() {

		TreeMap<ZonedDateTime, Float> quarterlyPrices = new TreeMap<>();

		Float[] prices = { 158.95f, 160.98f, 171.95f, 174.96f, //
				161.93f, 152f, 120.01f, 111.03f, //
				105.04f, 105f, 74.23f, 73.28f, //
				67.97f, 72.53f, 89.66f, 150.01f, //
				173.54f, 178.4f, 158.91f, 140.01f, //
				149.99f, 157.43f, 130.9f, 120.14f };

		for (int i = 0; i < 24; i++) {
			quarterlyPrices.put(now, prices[i]);
			quarterlyPrices.put(now.plusMinutes(15), prices[i]);
			quarterlyPrices.put(now.plusMinutes(30), prices[i]);
			quarterlyPrices.put(now.plusMinutes(45), prices[i]);
		}

		return quarterlyPrices;
	}

}
