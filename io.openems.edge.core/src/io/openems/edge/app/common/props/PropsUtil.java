package io.openems.edge.app.common.props;

import io.openems.edge.app.integratedsystem.FeneconHome10;
import io.openems.edge.app.integratedsystem.FeneconHome20;
import io.openems.edge.app.integratedsystem.FeneconHome30;
import io.openems.edge.core.appmanager.AppManagerUtil;

public final class PropsUtil {

	private PropsUtil() {
	}

	/**
	 * Checks if any type of home is installed.
	 * 
	 * @param util the {@link AppManagerUtil} to get the installed instances
	 * @return true if a {@link FeneconHome10} is installed otherwise false
	 */
	// TODO has to be manually updated when a new home app gets added, maybe add
	// subcategory for home
	public static boolean isHomeInstalled(AppManagerUtil util) {
		return !util.getInstantiatedAppsOf(//
				"App.FENECON.Home", //
				"App.FENECON.Home.20", //
				"App.FENECON.Home.30", //
				"App.FENECON.Home6", //
				"App.FENECON.Home10.Gen2", //
				"App.FENECON.Home15" //
		).isEmpty();
	}

	/**
	 * Checks if a {@link FeneconHome10} is installed.
	 * 
	 * @param util the {@link AppManagerUtil} to get the installed instances
	 * @return true if a {@link FeneconHome10} is installed otherwise false
	 */
	public static boolean isHome10Installed(AppManagerUtil util) {
		return !util.getInstantiatedAppsOf(//
				"App.FENECON.Home" //
		).isEmpty();
	}

	/**
	 * Checks if a {@link FeneconHome20} or {@link FeneconHome30} is installed.
	 * 
	 * @param util the {@link AppManagerUtil} to get the installed instances
	 * @return true if a {@link FeneconHome20} or {@link FeneconHome30} is installed
	 *         otherwise false
	 */
	public static boolean isHome20Or30Installed(AppManagerUtil util) {
		return !util.getInstantiatedAppsOf(//
				"App.FENECON.Home.20", //
				"App.FENECON.Home.30" //
		).isEmpty();
	}

}
