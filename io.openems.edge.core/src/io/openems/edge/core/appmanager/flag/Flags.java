package io.openems.edge.core.appmanager.flag;

public final class Flags {

	public static final Flag SHOW_AFTER_KEY_REDEEM = new FlagRecord("showAfterKeyRedeem");

	private Flags() {
		super();
	}

	private static record FlagRecord(String name) implements Flag {
		
	}

}
