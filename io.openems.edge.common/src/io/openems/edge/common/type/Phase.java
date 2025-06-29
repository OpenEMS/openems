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
}
