package io.openems.common.jsonrpc.serialization;

public final class BooleanPathActual {

	public static final class BooleanPathActualNonNull implements BooleanPath {

		private final boolean value;

		public BooleanPathActualNonNull(boolean value) {
			super();
			this.value = value;
		}

		@Override
		public boolean get() {
			return this.value;
		}

	}

	public static final class BooleanPathActualNullable implements BooleanPathNullable {

		private final Boolean value;

		public BooleanPathActualNullable(Boolean value) {
			super();
			this.value = value;
		}

		@Override
		public boolean isPresent() {
			return this.value != null;
		}

		@Override
		public Boolean getOrNull() {
			return this.value;
		}

	}

	private BooleanPathActual() {
	}

}
