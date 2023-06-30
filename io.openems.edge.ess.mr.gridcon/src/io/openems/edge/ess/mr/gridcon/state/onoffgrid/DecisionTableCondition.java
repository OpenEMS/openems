// CHECKSTYLE:OFF

package io.openems.edge.ess.mr.gridcon.state.onoffgrid;

public interface DecisionTableCondition {

	public static double LOWER_VOLTAGE = 207.0;
	public static double UPPER_VOLTAGE = 253.0;

	NaProtection1On isNaProtection1On() throws Exception;

	NaProtection2On isNaProtection2On() throws Exception;

	GridconCommunicationFailed isGridconCommunicationFailed() throws Exception;

	MeterCommunicationFailed isMeterCommunicationFailed() throws Exception;

	VoltageInRange isVoltageInRange() throws Exception;

	SyncBridgeOn isSyncBridgeOn() throws Exception;

	enum NaProtection1On {

		TRUE(true), FALSE(false), UNSET(null);

		private Boolean value;

		private NaProtection1On(Boolean value) {
			this.value = value;
		}

		public Boolean getValue() {
			return this.value;
		}
	}

	enum NaProtection2On {

		TRUE(true), FALSE(false), UNSET(null);

		private Boolean value;

		private NaProtection2On(Boolean value) {
			this.value = value;
		}

		public Boolean getValue() {
			return this.value;
		}
	}

	enum GridconCommunicationFailed {

		TRUE(true), FALSE(false), UNSET(null);

		private Boolean value;

		private GridconCommunicationFailed(Boolean value) {
			this.value = value;
		}

		public Boolean getValue() {
			return this.value;
		}
	}

	enum MeterCommunicationFailed {

		TRUE(true), FALSE(false), UNSET(null);

		private Boolean value;

		private MeterCommunicationFailed(Boolean value) {
			this.value = value;
		}

		public Boolean getValue() {
			return this.value;
		}
	}

	enum VoltageInRange {

		TRUE(true), FALSE(false), UNSET(null);

		private Boolean value;

		private VoltageInRange(Boolean value) {
			this.value = value;
		}

		public Boolean getValue() {
			return this.value;
		}
	}

	enum SyncBridgeOn {

		TRUE(true), FALSE(false), UNSET(null);

		private Boolean value;

		private SyncBridgeOn(Boolean value) {
			this.value = value;
		}

		public Boolean getValue() {
			return this.value;
		}
	}
}
// CHECKSTYLE:ON