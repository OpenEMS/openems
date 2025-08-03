package io.openems.common.jsonrpc.serialization;

import com.google.gson.JsonElement;

public final class NumberPathDummy {

	public static final class NumberPathDummyNonNull implements NumberPath, JsonPathDummy {

		@Override
		public Number get() {
			return 0;
		}

		@Override
		public JsonElement buildPath() {
			return JsonPrimitivePathDummy.buildPath("number", false);
		}

	}

	public static final class NumberPathDummyNullable implements NumberPathNullable, JsonPathDummy {

		@Override
		public boolean isPresent() {
			return false;
		}

		@Override
		public Number getOrNull() {
			return null;
		}

		@Override
		public JsonElement buildPath() {
			return JsonPrimitivePathDummy.buildPath("number", true);
		}

	}

	private NumberPathDummy() {
	}

}
