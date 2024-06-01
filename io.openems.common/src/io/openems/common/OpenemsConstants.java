package io.openems.common;

import java.util.Optional;

import org.osgi.framework.Constants;

import io.openems.common.types.SemanticVersion;

public class OpenemsConstants {

	/**
	 * The major version of OpenEMS.
	 *
	 * <p>
	 * This is the year of the release.
	 */
	public static final short VERSION_MAJOR = 2024;

	/**
	 * The minor version of OpenEMS.
	 *
	 * <p>
	 * This is the month of the release.
	 */
	public static final short VERSION_MINOR = 7;

	/**
	 * The patch version of OpenEMS.
	 *
	 * <p>
	 * This is always `0` for OpenEMS open source releases and reserved for private
	 * distributions.
	 */
	public static final short VERSION_PATCH = 0;

	/**
	 * The additional version string.
	 */
	public static final String VERSION_STRING = "SNAPSHOT";

	/**
	 * The complete version as a SemanticVersion.
	 *
	 * <p>
	 * Use toString()-method to get something like "2022.1.0-SNAPSHOT"
	 */
	public static final SemanticVersion VERSION = new SemanticVersion(//
			OpenemsConstants.VERSION_MAJOR, //
			OpenemsConstants.VERSION_MINOR, //
			OpenemsConstants.VERSION_PATCH, //
			OpenemsConstants.VERSION_STRING);

	/**
	 * The version development branch.
	 */
	public static final String VERSION_DEV_BRANCH = "";

	/**
	 * The version development commit hash.
	 */
	public static final String VERSION_DEV_COMMIT = "";

	/**
	 * The version development build time.
	 */
	public static final String VERSION_DEV_BUILD_TIME = "";

	/*
	 * Constants for Component properties
	 */
	public static final String PROPERTY_COMPONENT_ID = "id";
	public static final String PROPERTY_OSGI_COMPONENT_ID = "component.id";
	public static final String PROPERTY_OSGI_COMPONENT_NAME = "component.name";
	public static final String PROPERTY_PID = Constants.SERVICE_PID;
	public static final String PROPERTY_FACTORY_PID = "service.factoryPid";
	public static final String PROPERTY_LAST_CHANGE_BY = "_lastChangeBy";
	public static final String PROPERTY_LAST_CHANGE_AT = "_lastChangeAt";

	private static final String OPENEMS_DATA_DIR = "openems.data.dir";

	/**
	 * Gets the path of the OpenEMS Data Directory, configured by "openems.data.dir"
	 * command line parameter.
	 *
	 * @return the path of the OpenEMS Data Directory
	 */
	public static final String getOpenemsDataDir() {
		return Optional.ofNullable(System.getProperty(OpenemsConstants.OPENEMS_DATA_DIR)).orElse("");
	}

}
