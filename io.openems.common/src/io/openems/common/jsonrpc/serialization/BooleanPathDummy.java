package io.openems.common.jsonrpc.serialization;

import com.google.gson.JsonElement;

public final class BooleanPathDummy {

	public static final class BooleanPathDummyNonNull implements BooleanPath, JsonPathDummy {

		@Override
		public boolean get() {
			return false;
		}

		@Override
		public JsonElement buildPath() {
			return JsonPrimitivePathDummy.buildPath("boolean", false);
		}

	}

	public static final class BooleanPathDummyNullable implements BooleanPathNullable, JsonPathDummy {

		@Override
		public boolean isPresent() {
			return false;
		}

		@Override
		public Boolean getOrNull() {
			return null;
		}

		@Override
		public JsonElement buildPath() {
			return JsonPrimitivePathDummy.buildPath("boolean", true);
		}

	}

	private BooleanPathDummy() {
	}

}
