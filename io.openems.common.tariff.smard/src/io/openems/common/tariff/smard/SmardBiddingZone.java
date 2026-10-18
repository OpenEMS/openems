package io.openems.common.tariff.smard;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.openems.common.types.BiddingZone;

public enum SmardBiddingZone {

	GERMANY(BiddingZone.GERMANY, "4169"),

	AUSTRIA(BiddingZone.AUSTRIA, "4170"),

	BELGIUM(BiddingZone.BELGIUM, "4996"),

	NETHERLANDS(BiddingZone.NETHERLANDS, "256"),

	CZECHIA(BiddingZone.CZECHIA, "261"),

	;

	private static final Map<BiddingZone, SmardBiddingZone> MAPPING;

	static {
		MAPPING = Arrays.stream(SmardBiddingZone.values())
				.collect(Collectors.toUnmodifiableMap(SmardBiddingZone::getBiddingZone, Function.identity()));
	}

	/**
	 * Returns smard bidding zone by bidding zone or null if SMARD does not support
	 * the given bidding zone.
	 * 
	 * @param biddingZone Bidding zone to search
	 * @return SMARD Bidding Zone or null
	 */
	public static SmardBiddingZone getByBiddingZone(BiddingZone biddingZone) {
		return MAPPING.get(biddingZone);
	}

	private final BiddingZone biddingZone;
	private final String filter;

	private SmardBiddingZone(BiddingZone biddingZone, String filter) {
		this.biddingZone = biddingZone;
		this.filter = filter;
	}

	public BiddingZone getBiddingZone() {
		return this.biddingZone;
	}

	public String getSmardFilter() {
		return this.filter;
	}

}
