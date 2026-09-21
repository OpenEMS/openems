package io.openems.edge.common.type;

public final class Phase {

	private Phase() {
	}

	public enum SingleOrAllPhase {
		ALL(""), //
		L1("L1"), //
		L2("L2"), //
		L3("L3");

		public final String symbol;

		private SingleOrAllPhase(String symbol) {
			this.symbol = symbol;
		}

		/**
		 * Gets the {@link SinglePhase} for this {@link SingleOrAllPhase}.
		 * 
		 * @return {@link SinglePhase}; or null for {@link SingleOrAllPhase#ALL}
		 */
		public final SinglePhase getSinglePhase() {
			return switch (this) {
			case ALL -> null;
			case L1 -> SinglePhase.L1;
			case L2 -> SinglePhase.L2;
			case L3 -> SinglePhase.L3;
			};
		}
	}

	public enum SinglePhase {
		L1(SingleOrAllPhase.L1), //
		L2(SingleOrAllPhase.L2), //
		L3(SingleOrAllPhase.L3);

		public final String symbol;
		public final SingleOrAllPhase toSingleOrAllPhase;

		private SinglePhase(SingleOrAllPhase toSingleOrAllPhase) {
			this.toSingleOrAllPhase = toSingleOrAllPhase;
			this.symbol = toSingleOrAllPhase.symbol;
		}
	}

	public enum SingleOrThreePhase {
		SINGLE_PHASE(1), //
		THREE_PHASE(3);

		public final int count;

		private SingleOrThreePhase(int count) {
			this.count = count;
		}
	}
}
