package io.openems.edge.timeofusetariff.entsoe.priceprovider;

import java.util.Objects;

import io.openems.common.types.EntsoeBiddingZone;

public record EntsoeConfiguration(EntsoeBiddingZone biddingZone, String entsoeApiKey) {
	@Override
	public int hashCode() {
		return Objects.hash(this.biddingZone, this.entsoeApiKey);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof EntsoeConfiguration other)) {
			return false;
		}

		return this.biddingZone == other.biddingZone && Objects.equals(this.entsoeApiKey, other.entsoeApiKey);
	}
}
