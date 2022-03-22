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
	public static final short VERSION_MAJOR = 2022;

	/**
	 * The minor version of OpenEMS.
	 *
	 * <p>
	 * This is the month of the release.
	 */
	public final static short VERSION_MINOR = 4;

	/**
	 * The patch version of OpenEMS.
	 *
	 * <p>
	 * This is always `0` for OpenEMS open source releases and reserved for private
	 * distributions.
	 */
	public final static short VERSION_PATCH = 0;

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
	 * The manufacturer of the device that is running OpenEMS.
	 *
	 * <p>
	 * Note: this should be max. 32 ASCII characters long
	 */
	public static final String MANUFACTURER = OpenemsOEM.MANUFACTURER;

	/**
	 * The model identifier of the device.
	 *
	 * <p>
	 * Note: this should be max. 32 ASCII characters long
	 */
	public static final String MANUFACTURER_MODEL = "OpenEMS";

	/**
	 * The options of the device.
	 *
	 * <p>
	 * Note: this should be max. 32 ASCII characters long
	 */
	public static final String MANUFACTURER_OPTIONS = "";

	/**
	 * The version of the device.
	 *
	 * <p>
	 * Note: this should be max. 32 ASCII characters long
	 */
	public static final String MANUFACTURER_VERSION = "";

	/**
	 * The serial number of the device.
	 *
	 * <p>
	 * Note: this should be max. 32 ASCII characters long
	 */
	public static final String MANUFACTURER_SERIAL_NUMBER = "";

	/**
	 * The Energy-Management-System serial number of the device.
	 *
	 * <p>
	 * Note: this should be max. 32 ASCII characters long
	 */
	public static final String MANUFACTURER_EMS_SERIAL_NUMBER = "";

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
