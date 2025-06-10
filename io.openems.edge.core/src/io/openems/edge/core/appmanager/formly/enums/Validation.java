package io.openems.edge.core.appmanager.formly.enums;

import io.openems.edge.core.host.NetworkConfiguration;

public enum Validation {
	// TODO translation
	IP(NetworkConfiguration.PATTERN_INET4ADDRESS, "Input is not a valid IP Address!"), //
	;

	private String pattern;
	private String errorMsg;

	private Validation(String pattern, String errorMsg) {
		this.pattern = pattern;
		this.errorMsg = errorMsg;
	}

	public String getErrorMsg() {
		return this.errorMsg;
	}

	public String getPattern() {
		return this.pattern;
	}

}