package io.openems.edge.core.appmanager.flag;

import com.google.gson.JsonElement;

import io.openems.common.utils.JsonUtils;

public final class Flags {

	public static final Flag SHOW_AFTER_KEY_REDEEM = new FlagRecord("showAfterKeyRedeem");

	public static final Flag ALWAYS_INSTALLED = new FlagRecord("alwaysInstalled");

	public static final Flag FREE_FROM_DEPENDENCY = new FlagRecord("freeFromDependency");

	private Flags() {
		super();
	}

	private static record FlagRecord(String name) implements Flag {

	}

	public static record SwitchFlag(String handlerId, String canSwitchMethod, String switchMethod) implements Flag {

		public static final String NAME = "canSwitchVersion"; // flag name

		@Override
		public String name() {
			return NAME;
		}

		@Override
		public JsonElement toJson() {
			return JsonUtils.buildJsonObject() //
					.addProperty("name", this.name()) //
					.addProperty("handlerId", this.handlerId()) //
					.addProperty("canSwitchMethod", this.canSwitchMethod()) //
					.addProperty("switchMethod", this.switchMethod()) //
					.build();
		}
	}
}
