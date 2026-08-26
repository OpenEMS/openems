package io.openems.common.jsonrpc.serialization;

import java.util.Objects;

public final class NumberPathActual {

	public static final class NumberPathActualNonNull implements NumberPath {

		private final Number element;

		public NumberPathActualNonNull(Number element) {
			super();
			this.element = Objects.requireNonNull(element);
		}

		@Override
		public Number get() {
			return this.element;
		}

	}

	public static final class NumberPathActualNullable implements NumberPathNullable {

		private final Number element;

		public NumberPathActualNullable(Number element) {
			super();
			this.element = element;
		}

		@Override
		public boolean isPresent() {
			return this.element != null;
		}

		@Override
		public Number getOrNull() {
			return this.element;
		}

	}

	private NumberPathActual() {
	}

}
