package io.openems.common.types;

public enum OpenemsType {
	BOOLEAN, SHORT, INTEGER, LONG, //
	/**
	 * ENUM is a special type of INTEGER
	 */
	@Deprecated
	// TODO use INTEGER by default for ENUM
	ENUM, //
	FLOAT, DOUBLE, //
	STRING
}
