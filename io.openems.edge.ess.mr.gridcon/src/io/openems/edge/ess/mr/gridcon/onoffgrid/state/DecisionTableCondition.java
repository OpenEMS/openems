package io.openems.edge.ess.mr.gridcon.onoffgrid.state;

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
//		UNSET
		;

		private boolean value;

		private NAProtection_1_On(boolean value) {
			this.value = value;
		}

		public boolean getValue() {
			return value;
		}
	}

	enum NAProtection_2_On {

		TRUE(true), FALSE(false),;

		private boolean value;

		private NAProtection_2_On(boolean value) {
			this.value = value;
		}

		public boolean getValue() {
			return value;
		}
	}

	enum GridconCommunicationFailed {

		TRUE(true), FALSE(false),;

		private boolean value;

		private GridconCommunicationFailed(boolean value) {
			this.value = value;
		}

		public boolean getValue() {
			return value;
		}
	}

	enum MeterCommunicationFailed {

		TRUE(true), FALSE(false),;

		private boolean value;

		private MeterCommunicationFailed(boolean value) {
			this.value = value;
		}

		public boolean getValue() {
			return value;
		}
	}

	enum VoltageInRange {

		TRUE(true), FALSE(false),;

		private boolean value;

		private VoltageInRange(boolean value) {
			this.value = value;
		}

		public boolean getValue() {
			return value;
		}
	}

	enum SyncBridgeOn {

		TRUE(true), FALSE(false),;

		private boolean value;

		private SyncBridgeOn(boolean value) {
			this.value = value;
		}

		public boolean getValue() {
			return value;
		}
	}

}
