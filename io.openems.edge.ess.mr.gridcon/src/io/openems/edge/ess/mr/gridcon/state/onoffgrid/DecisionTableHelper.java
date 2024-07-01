// CHECKSTYLE:OFF

package io.openems.edge.ess.mr.gridcon.state.onoffgrid;

public class DecisionTableHelper {

	public static boolean isStateStartSystem(DecisionTableCondition condition) {
		boolean ret = false;
		try {
			ret = condition.isNaProtection1On().getValue() == Boolean.FALSE
					&& condition.isNaProtection2On().getValue() == Boolean.FALSE
					&& condition.isGridconCommunicationFailed().getValue() == Boolean.TRUE
					&& condition.isMeterCommunicationFailed().getValue() == Boolean.TRUE
					&& condition.isSyncBridgeOn().getValue() == Boolean.TRUE;
		} catch (Exception e) {
			ret = false;
		}
		return ret;
	}

	public static boolean isWaitingForDevices(DecisionTableCondition condition) {
		boolean ret = false;
		try {
			ret = condition.isNaProtection1On().getValue() == Boolean.FALSE
					&& condition.isNaProtection2On().getValue() == Boolean.FALSE
					&& condition.isGridconCommunicationFailed().getValue() == Boolean.TRUE
					&& condition.isMeterCommunicationFailed().getValue() == Boolean.TRUE
					&& condition.isSyncBridgeOn().getValue() == Boolean.FALSE;
		} catch (Exception e) {
			ret = false;
		}
		return ret;
	}

	public static boolean isOnGridMode(DecisionTableCondition condition) {
		boolean ret = false;
		try {
			ret = condition.isNaProtection1On().getValue() == Boolean.TRUE
					&& condition.isNaProtection2On().getValue() == Boolean.TRUE;
		} catch (Exception e) {
			ret = false;
		}
		return ret;
	}

	public static boolean isOffGridMode(DecisionTableCondition condition) {
		boolean ret = false;
		try {
			ret = condition.isNaProtection1On().getValue() == Boolean.FALSE
					&& condition.isNaProtection2On().getValue() == Boolean.FALSE
					&& condition.isGridconCommunicationFailed().getValue() == Boolean.FALSE
					&& condition.isMeterCommunicationFailed().getValue() == Boolean.FALSE
					&& condition.isVoltageInRange().getValue() == Boolean.FALSE;
		} catch (Exception e) {
			ret = false;
		}
		return ret;
	}

	public static boolean isOffGridGridBack(DecisionTableCondition condition) {
		boolean ret = false;
		try {
			ret = condition.isNaProtection1On().getValue() == Boolean.FALSE
					&& condition.isNaProtection2On().getValue() == Boolean.FALSE
					&& condition.isGridconCommunicationFailed().getValue() == Boolean.FALSE
					&& condition.isMeterCommunicationFailed().getValue() == Boolean.FALSE
					&& condition.isVoltageInRange().getValue() == Boolean.TRUE
					&& condition.isSyncBridgeOn().getValue() == Boolean.FALSE;
		} catch (Exception e) {
			ret = false;
		}
		return ret;
	}

	public static boolean isOffGridWaitForGridAvailable(DecisionTableCondition condition) {
		boolean ret = false;
		try {
			ret = condition.isNaProtection1On().getValue() == Boolean.FALSE
					&& condition.isNaProtection2On().getValue() == Boolean.FALSE
					&& condition.isGridconCommunicationFailed().getValue() == Boolean.FALSE
					&& condition.isMeterCommunicationFailed().getValue() == Boolean.FALSE
					&& condition.isVoltageInRange().getValue() == Boolean.TRUE
					&& condition.isSyncBridgeOn().getValue() == Boolean.TRUE;
		} catch (Exception e) {
			ret = false;
		}
		return ret;
	}

	public static boolean isAdjustParameters(DecisionTableCondition condition) {
		boolean ret = false;
		try {
			ret = condition.isNaProtection1On().getValue() == Boolean.TRUE
					&& condition.isNaProtection2On().getValue() == Boolean.FALSE
					&& condition.isGridconCommunicationFailed().getValue() == Boolean.FALSE
					&& condition.isMeterCommunicationFailed().getValue() == Boolean.FALSE
					&& condition.isVoltageInRange().getValue() == Boolean.TRUE
					&& condition.isSyncBridgeOn().getValue() == Boolean.TRUE;
		} catch (Exception e) {
			ret = false;
		}
		return ret;
	}

	public static boolean isUndefined(DecisionTableCondition condition) {
		return !isStateStartSystem(condition) && !isWaitingForDevices(condition) && !isOnGridMode(condition)
				&& !isOffGridMode(condition) && !isOffGridGridBack(condition)
				&& !isOffGridWaitForGridAvailable(condition) && !isAdjustParameters(condition) && !isError(condition);

	}

	// TODO could there really be an error situation?
	public static boolean isError(DecisionTableCondition condition) {
		boolean ret = false;
		try {
			ret = condition.isNaProtection1On().getValue() == Boolean.TRUE
					&& condition.isNaProtection2On().getValue() == Boolean.FALSE
					&& condition.isGridconCommunicationFailed().getValue() == Boolean.TRUE
					&& condition.isMeterCommunicationFailed().getValue() == Boolean.FALSE
					&& condition.isVoltageInRange().getValue() == Boolean.TRUE
					&& condition.isSyncBridgeOn().getValue() == Boolean.TRUE;
		} catch (Exception e) {
			ret = false;
		}
		return ret;
	}

}
// CHECKSTYLE:ON
