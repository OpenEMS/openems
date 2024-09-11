package io.openems.edge.controller.ess.timeofusetariff.v1;

import java.util.List;

import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve;
import io.openems.edge.controller.ess.limittotaldischarge.ControllerEssLimitTotalDischarge;
import io.openems.edge.controller.ess.timeofusetariff.ControlMode;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public record ContextV1(List<ControllerEssEmergencyCapacityReserve> ctrlEmergencyCapacityReserves,
		List<ControllerEssLimitTotalDischarge> ctrlLimitTotalDischarges, ManagedSymmetricEss ess,
		ControlMode controlMode, int maxChargePowerFromGrid, boolean limitChargePowerFor14aEnWG) {
}