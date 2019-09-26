package io.openems.common;

import org.osgi.framework.Constants;

import io.openems.common.types.SemanticVersion;

public class OpenemsConstants {

	/**
	 * The major version of OpenEMS.
	 * 
	 * This is usually the year of the release
	 */
	public final static short VERSION_MAJOR = 2019;

	/**
	 * The minor version of OpenEMS.
	 * 
	 * This is usually the number of the sprint within the year
	 */
	public final static short VERSION_MINOR = 13;

	/**
	 * The patch version of OpenEMS.
	 * 
	 * This is the number of the bugfix release
	 */
	public final static short VERSION_PATCH = 0;

	/**
	 * The additional version string
	 */
	// public final static String VERSION_STRING = "";
	public final static String VERSION_STRING = "SNAPSHOT";

	/**
	 * The complete version as a SemanticVersion.
	 * 
	 * <p>
	 * Use toString()-method to get something like "2019.6.0-SNAPSHOT"
	 */
	public final static SemanticVersion VERSION = new SemanticVersion(VERSION_MAJOR, VERSION_MINOR, VERSION_PATCH,
			VERSION_STRING);
	/**
	 * The manufacturer of the device that is running OpenEMS
	 * 
	 * Note: this should be max. 32 ASCII characters long
	 */
	public final static String MANUFACTURER = "OpenEMS Association e.V.";

	/**
	 * The model identifier of the device
	 * 
	 * Note: this should be max. 32 ASCII characters long
	 */
	public final static String MANUFACTURER_MODEL = "";

	/**
	 * The options of the device
	 * 
	 * Note: this should be max. 32 ASCII characters long
	 */
	public final static String MANUFACTURER_OPTIONS = "";

	/**
	 * The version of the device
	 * 
	 * Note: this should be max. 32 ASCII characters long
	 */
	public final static String MANUFACTURER_VERSION = "";

	/**
	 * The serial number of the device
	 * 
	 * Note: this should be max. 32 ASCII characters long
	 */
	public final static String MANUFACTURER_SERIAL_NUMBER = "";

	/**
	 * The Energy-Management-System serial number of the device
	 * 
	 * Note: this should be max. 32 ASCII characters long
	 */
	public final static String MANUFACTURER_EMS_SERIAL_NUMBER = "";

	/*
	 * Static OpenEMS Component-IDs
	 */
	public final static String CYCLE_ID = "_cycle";
	public final static String COMPONENT_MANAGER_ID = "_componentManager";
	public final static String META_ID = "_meta";
	public final static String SUM_ID = "_sum";
	public final static String HOST_ID = "_host";

	public final static String POWER_DOC_TEXT = "Negative values for Consumption; positive for Production";

	/*
	 * Constants for Component properties
	 */
	public final static String PROPERTY_COMPONENT_ID = "id";
	public final static String PROPERTY_OSGI_COMPONENT_ID = "component.id";
	public final static String PROPERTY_OSGI_COMPONENT_NAME = "component.name";
	public final static String PROPERTY_PID = Constants.SERVICE_PID;
	public final static String PROPERTY_FACTORY_PID = "service.factoryPid";
	public final static String PROPERTY_LAST_CHANGE_BY = "_lastChangeBy";
	public final static String PROPERTY_LAST_CHANGE_AT = "_lastChangeAt";
}
