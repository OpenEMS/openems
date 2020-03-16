package io.openems.edge.ess.mr.gridcon.state.onoffgrid;

public class DecisionTableHelper {

	public static boolean isStateStartSystem(DecisionTableCondition condition) {
		boolean ret = false;
		try {
			ret = 
				!condition.isNaProtection1On().getValue()  &&
				!condition.isNaProtection2On().getValue() &&
				condition.isGridconCommunicationFailed().getValue() &&
				condition.isMeterCommunicationFailed().getValue() &&
				condition.isSyncBridgeOn().getValue();
		} catch (Exception e) {
			ret = false;
		}
		return ret;
	}

	public static boolean isWaitingForDevices(DecisionTableCondition condition) {
		boolean ret = false;
		try {
			ret = 
				!condition.isNaProtection1On().getValue() &&
				!condition.isNaProtection2On().getValue() &&
				condition.isGridconCommunicationFailed().getValue() &&
				condition.isMeterCommunicationFailed().getValue() &&
				!condition.isSyncBridgeOn().getValue();
		} catch (Exception e) {
			ret = false;
		}
		return ret;
	}

	public static boolean isOnGridMode(DecisionTableCondition condition) {
		boolean ret = false;
		try {
			ret = 
				condition.isNaProtection1On().getValue() &&
				condition.isNaProtection2On().getValue() &&
				!condition.isGridconCommunicationFailed().getValue() &&
				!condition.isMeterCommunicationFailed().getValue()
				;
		} catch (Exception e) {
			ret = false;
		}
		return ret;
	}

	public static boolean isOffGridMode(DecisionTableCondition condition) {
		boolean ret = false;
		try {
			ret =
					!condition.isNaProtection1On().getValue() &&
					!condition.isNaProtection2On().getValue() &&
					!condition.isGridconCommunicationFailed().getValue() &&
					!condition.isMeterCommunicationFailed().getValue() &&
					!condition.isVoltageInRange().getValue()			
				;
		} catch (Exception e) {
			ret = false;
		}
		return ret;
	}

	public static boolean isOffGridGridBack(DecisionTableCondition condition) {
		boolean ret = false;
		try {
			ret = 
					!condition.isNaProtection1On().getValue() &&
					!condition.isNaProtection2On().getValue() &&
					!condition.isGridconCommunicationFailed().getValue() &&
					!condition.isMeterCommunicationFailed().getValue() &&
					condition.isVoltageInRange().getValue() &&
					!condition.isSyncBridgeOn().getValue()
				;
		} catch (Exception e) {
			ret = false;
		}
		return ret;
	}

	public static boolean isOffGridWaitForGridAvailable(DecisionTableCondition condition) {
		boolean ret = false;
		try {
			ret = 
					!condition.isNaProtection1On().getValue() &&
					!condition.isNaProtection2On().getValue() &&
					!condition.isGridconCommunicationFailed().getValue() &&
					!condition.isMeterCommunicationFailed().getValue() &&
					condition.isVoltageInRange().getValue() &&
					condition.isSyncBridgeOn().getValue()
				;
		} catch (Exception e) {
			ret = false;
		}
		return ret;
	}

	public static boolean isAdjustParameters(DecisionTableCondition condition) {
		boolean ret = false;
		try {
			ret = 
					condition.isNaProtection1On().getValue() &&
					!condition.isNaProtection2On().getValue() &&
					!condition.isGridconCommunicationFailed().getValue() &&
					!condition.isMeterCommunicationFailed().getValue() &&
					condition.isVoltageInRange().getValue() &&
					condition.isSyncBridgeOn().getValue()
				;
		} catch (Exception e) {
			ret = false;
		}
		return ret;
	}

	public static boolean isFinishGoingOnGrid(DecisionTableCondition condition) {
		boolean ret = false;
		try {
			ret = 
					condition.isNaProtection1On().getValue() &&
					condition.isNaProtection2On().getValue() &&
					!condition.isMeterCommunicationFailed().getValue() &&
					condition.isVoltageInRange().getValue() &&
					condition.isSyncBridgeOn().getValue()
				;
		} catch (Exception e) {
			ret = false;
		}
		return ret;
	}

	public static boolean isRestartGridconAfterSync(DecisionTableCondition condition) {
		boolean ret = false;
		try {
			ret = 
					condition.isNaProtection1On().getValue() &&
					condition.isNaProtection2On().getValue() &&
					condition.isGridconCommunicationFailed().getValue() &&
					!condition.isMeterCommunicationFailed().getValue() &&
					condition.isVoltageInRange().getValue() &&
					!condition.isSyncBridgeOn().getValue()
				;
		} catch (Exception e) {
			ret = false;
		}
		return ret;
	}

	public static boolean isUndefined(DecisionTableCondition condition) {
		return 
				!isStateStartSystem(condition) &&
				!isWaitingForDevices(condition) &&
				!isOnGridMode(condition) &&
				!isOffGridMode(condition) &&
				!isOffGridGridBack(condition) &&
				!isOffGridWaitForGridAvailable(condition) &&
				!isAdjustParameters(condition) &&
				!isFinishGoingOnGrid(condition) &&
				!isRestartGridconAfterSync(condition) &&
				!isError(condition);
				
	}

	public static boolean isError(DecisionTableCondition condition) {
		boolean ret = false;
		try {
			ret = 
					condition.isNaProtection1On().getValue() &&
					!condition.isNaProtection2On().getValue() &&
					condition.isGridconCommunicationFailed().getValue() &&
					!condition.isMeterCommunicationFailed().getValue() &&
					condition.isVoltageInRange().getValue() &&
					condition.isSyncBridgeOn().getValue()
				;
		} catch (Exception e) {
			ret = false;
		}
		return ret;
	}
	
	
}
