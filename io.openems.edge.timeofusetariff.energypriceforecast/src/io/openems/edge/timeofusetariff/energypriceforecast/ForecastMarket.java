package io.openems.edge.timeofusetariff.energypriceforecast;

import io.openems.common.types.BiddingZone;

final class ForecastMarket {
	private ForecastMarket() {
	}

	static String forBiddingZone(BiddingZone zone) {
		return switch (zone) {
		case GERMANY -> "DE";
		case AUSTRIA -> "AT";
		case BELGIUM -> "BE";
		case NETHERLANDS -> "NL";
		case CZECHIA -> "CZ";
		case SWEDEN_SE1 -> "SE1";
		case SWEDEN_SE2 -> "SE2";
		case SWEDEN_SE3 -> "SE3";
		case SWEDEN_SE4 -> "SE4";
		case GREECE -> "GR";
		case LITHUANIA -> null;
		};
	}
}
