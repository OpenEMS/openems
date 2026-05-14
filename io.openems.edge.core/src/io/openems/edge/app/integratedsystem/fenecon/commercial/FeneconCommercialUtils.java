package io.openems.edge.app.integratedsystem.fenecon.commercial;

import io.openems.edge.core.appmanager.AppManagerUtil;

public class FeneconCommercialUtils {

	protected static boolean isOldGridMeterAppUsedByCommercialApp(//
			final AppManagerUtil appManagerUtil, //
			final String commercialAppId //
	) {
		final var oldGridMeterAppId = "App.Meter.Kdk";

		for (final var kdkApp : appManagerUtil.getInstantiatedAppsOfApp(oldGridMeterAppId)) {
			final var parentApps = appManagerUtil.getAppsWithDependencyTo(kdkApp);
			final var doesCommercialExists = parentApps.stream().anyMatch(app -> app.appId.equals(commercialAppId));
			if (doesCommercialExists) {
				return true;
			}
		}
		return false;
	}
}
