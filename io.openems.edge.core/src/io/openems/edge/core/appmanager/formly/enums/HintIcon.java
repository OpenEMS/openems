package io.openems.edge.core.appmanager.formly.enums;

public enum HintIcon {
	INFO("Info"), //
	WARNING("Warning"), //
	ERROR("Error");

	private final String iconName;

	private HintIcon(String iconName) {
		this.iconName = iconName;
	}

	public String getIconName() {
		return this.iconName;
	}
}
