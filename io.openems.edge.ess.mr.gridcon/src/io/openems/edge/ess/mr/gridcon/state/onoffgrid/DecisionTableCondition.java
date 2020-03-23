package io.openems.edge.ess.mr.gridcon.state.onoffgrid;

public interface DecisionTableCondition {

	public static double LOWER_VOLTAGE = 207.0;
	public static double UPPER_VOLTAGE = 253.0;

	NAProtection_1_On isNaProtection1On() throws Exception;
	NAProtection_2_On isNaProtection2On() throws Exception;
	GridconCommunicationFailed isGridconCommunicationFailed() throws Exception;
	MeterCommunicationFailed isMeterCommunicationFailed() throws Exception;
	VoltageInRange isVoltageInRange() throws Exception;
	SyncBridgeOn isSyncBridgeOn() throws Exception;

	enum NAProtection_1_On {

		TRUE(true),
		FALSE(false),
		UNSET(null)
		;

		private Boolean value;

		private NAProtection_1_On(Boolean value) {
			this.value = value;
		}

		public Boolean getValue() {
			return value;
		}
	}

	enum NAProtection_2_On {

		TRUE(true), FALSE(false), UNSET(null);

		private Boolean value;

		private NAProtection_2_On(Boolean value) {
			this.value = value;
		}

		public Boolean getValue() {
			return value;
		}
	}

	enum GridconCommunicationFailed {

		TRUE(true), FALSE(false), UNSET(null);

		private Boolean value;

		private GridconCommunicationFailed(Boolean value) {
			this.value = value;
		}

		public Boolean getValue() {
			return value;
		}
	}

	enum MeterCommunicationFailed {

		TRUE(true), FALSE(false), UNSET(null);

		private Boolean value;

		private MeterCommunicationFailed(Boolean value) {
			this.value = value;
		}

		public Boolean getValue() {
			return value;
		}
	}

	enum VoltageInRange {

		TRUE(true), FALSE(false), UNSET(null);

		private Boolean value;

		private VoltageInRange(Boolean value) {
			this.value = value;
		}

		public Boolean getValue() {
			return value;
		}
	}

	enum SyncBridgeOn {

		TRUE(true), FALSE(false), UNSET(null);

		private Boolean value;

		private SyncBridgeOn(Boolean value) {
			this.value = value;
		}

		public Boolean getValue() {
			return value;
		}
	}

}
