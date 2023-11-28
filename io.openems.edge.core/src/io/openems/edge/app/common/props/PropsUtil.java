package io.openems.edge.app.common.props;

import io.openems.edge.app.integratedsystem.FeneconHome;
import io.openems.edge.core.appmanager.AppManagerUtil;

public final class PropsUtil {

	private PropsUtil() {
	}

	/**
	 * Checks if a {@link FeneconHome} is installed.
	 * 
	 * @param util the {@link AppManagerUtil} to get the installed instances
	 * @return true if a {@link FeneconHome} is installed otherwise false
	 */
	public static boolean isHomeInstalled(AppManagerUtil util) {
		return !util.getInstantiatedAppsOf(//
				"App.FENECON.Home", //
				"App.FENECON.Home.20", //
				"App.FENECON.Home.30" //
		).isEmpty();
	}

}
