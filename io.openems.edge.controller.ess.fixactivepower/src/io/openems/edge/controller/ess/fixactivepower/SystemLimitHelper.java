package io.openems.edge.controller.ess.fixactivepower;

import static io.openems.edge.controller.ess.fixactivepower.ControllerEssFixActivePowerImpl.getAcPower;
import static io.openems.edge.controller.ess.fixactivepower.ControllerEssFixActivePowerImpl.getDcPower;

import com.google.common.annotations.VisibleForTesting;

import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.meta.types.GridLimit;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Pwr;

public final class SystemLimitHelper {

	private SystemLimitHelper() {
	}

	record LimitResult(ControllerEssFixActivePowerImpl.PowerTarget powerTarget, boolean limitedByEssHardware,
			boolean limitedByMetaLimit) {

		private static LimitResult noLimitWithWarning(boolean limitedByEssHardware, boolean limitedByMetaLimit) {
			return new LimitResult(null, limitedByEssHardware, limitedByMetaLimit);
		}

		private static LimitResult limitWithWarning(ControllerEssFixActivePowerImpl.PowerTarget powerTarget,
				boolean limitedByEssHardware, boolean limitedByMetaLimit) {
			return new LimitResult(powerTarget, limitedByEssHardware, limitedByMetaLimit);
		}

		private static LimitResult limitWithoutWarning(ControllerEssFixActivePowerImpl.PowerTarget powerTarget) {
			return new LimitResult(powerTarget, false, false);
		}
	}

	/**
	 * Calculates the active power target for the given mode and requested power,
	 * taking into account the system limits defined in the meta.
	 *
	 * <p>
	 * System limits are always considered.
	 * </p>
	 *
	 * @param componentId     component ID for logging purposes
	 * @param powerTarget     the requested power target
	 * @param ess             the ESS for which the target is calculated
	 * @param gridLimit       the {@link GridLimit} defined in {@link Meta}
	 * @param gridActivePower the current grid active power to
	 * @return limited Result
	 */
	@VisibleForTesting
	static LimitResult clampToSystemLimits(String componentId, ControllerEssFixActivePowerImpl.PowerTarget powerTarget,
			ManagedSymmetricEss ess, GridLimit.Immutable gridLimit, Integer gridActivePower) {
		return clampToSystemLimits(componentId, powerTarget, ess, gridLimit, gridActivePower, false, true);
	}

	/**
	 * Clamp the requested powerTarget, if required, while respecting the ESS
	 * hardware limits and the system limits configured in the meta.
	 *
	 * @param componentId                       component ID for logging purposes
	 * @param powerTarget                       the requested power target
	 * @param ess                               the ESS for which the target is
	 *                                          calculated
	 * @param gridLimit                         the {@link GridLimit} defined in
	 *                                          {@link Meta}
	 * @param gridActivePower                   the current grid active power
	 * @param ignoreSystemLimitsPermissionsOnce if true, system permissions will be
	 *                                          ignored once (used for service
	 *                                          purposes)
	 * @param considerSystemLimits              if true, system limits will be
	 *                                          considered
	 * @return limited Result
	 */
	static LimitResult clampToSystemLimits(String componentId, ControllerEssFixActivePowerImpl.PowerTarget powerTarget,
			ManagedSymmetricEss ess, GridLimit.Immutable gridLimit, Integer gridActivePower,
			boolean ignoreSystemLimitsPermissionsOnce, boolean considerSystemLimits) {

		if (targetHasNoDirectGridInteraction(ess, powerTarget) || !considerSystemLimits) {
			return LimitResult.limitWithoutWarning(powerTarget);
		}

		// Check Meta permissions
		if (!ignoreSystemLimitsPermissionsOnce && !systemLimitsPermissionsGranted(ess, powerTarget, gridLimit)) {
			return LimitResult.noLimitWithWarning(false, true);
		}

		// Fit into ess limits
		var requestedAcPower = getAcPower(ess, powerTarget.hybridEssMode(), powerTarget.power());
		var essLimitedAcPower = ess.getPower().fitValueIntoMinMaxPower(componentId, ess, powerTarget.phase(),
				powerTarget.pwr(), requestedAcPower);
		var isLimitedByEssHardware = requestedAcPower != essLimitedAcPower;

		// Fit into GridLimits of Meta
		var metaLimitedAcPower = limitAcPowerByMeta(essLimitedAcPower, gridLimit, gridActivePower,
				ess.getActivePower().orElse(0));
		var isLimitedByMetaLimit = metaLimitedAcPower != essLimitedAcPower;
		isLimitedByEssHardware = isLimitedByEssHardware && !isLimitedByMetaLimit;

		// Return AcPower limited by Ess Hardware and Meta Limits
		var acTarget = ControllerEssFixActivePowerImpl.PowerTarget.withAcPower(powerTarget, metaLimitedAcPower);
		return LimitResult.limitWithWarning(acTarget, isLimitedByEssHardware, isLimitedByMetaLimit);
	}

	private static boolean targetHasNoDirectGridInteraction(ManagedSymmetricEss ess,
			ControllerEssFixActivePowerImpl.PowerTarget powerTarget) {
		var dcPower = getDcPower(ess, powerTarget.hybridEssMode(), powerTarget.power());
		return dcPower == 0 || powerTarget.pwr().equals(Pwr.REACTIVE);
	}

	private static boolean systemLimitsPermissionsGranted(ManagedSymmetricEss ess,
			ControllerEssFixActivePowerImpl.PowerTarget powerTarget, GridLimit.Immutable gridLimit) {

		var dcPower = getDcPower(ess, powerTarget.hybridEssMode(), powerTarget.power());
		if (dcPower < 0) {
			return gridLimit.isEssChargeFromGridAllowed();
		}
		if (dcPower > 0) {
			return gridLimit.isEssDischargeToGridAllowed();
		}
		return true;
	}

	private static int limitAcPowerByMeta(int requestedAcPower, GridLimit.Immutable gridLimit, Integer gridActivePower,
			int essActivePower) {
		var acMinimum = calculateAcMinimum(gridLimit, gridActivePower, essActivePower);
		var acMaximum = calculateAcMaximum(gridLimit, gridActivePower, essActivePower);
		return Math.clamp(requestedAcPower, acMinimum, acMaximum);
	}

	/**
	 * Calculates the minimum active power based on the system limits and current
	 * grid and ESS power.
	 *
	 * @param systemLimits    the system limits defined in the meta
	 * @param gridActivePower the current grid active power
	 * @param essActivePower  the current ESS active power
	 * @return the calculated minimum active power
	 */
	static int calculateAcMinimum(GridLimit.Immutable systemLimits, Integer gridActivePower, int essActivePower) {
		final var gridLimit = systemLimits.gridBuyHardLimitWithBuffer();
		if (gridActivePower == null) {
			return (int) -gridLimit;
		}
		var realGridPower = gridActivePower + essActivePower;
		return realGridPower - (int) gridLimit;
	}

	/**
	 * Calculates the maximum active power based on the system limits and current
	 * grid and ESS power.
	 *
	 * @param gridLimit       the {@link GridLimit} defined in {@link Meta}
	 * @param gridActivePower the current grid active power
	 * @param essActivePower  the current ESS active power
	 * @return the calculated maximum active power
	 */
	static int calculateAcMaximum(GridLimit.Immutable gridLimit, Integer gridActivePower, int essActivePower) {

		final var gridLimitWithBuffer = gridLimit.gridSellHardLimitWithBuffer();
		if (gridActivePower == null) {
			return (int) gridLimitWithBuffer;
		}
		var realGridPower = gridActivePower + essActivePower;
		return realGridPower + (int) gridLimitWithBuffer;
	}
}
