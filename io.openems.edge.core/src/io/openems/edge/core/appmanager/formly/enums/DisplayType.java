package io.openems.edge.core.appmanager.formly.enums;

public enum DisplayType {
	STRING("string"), //
	BOOLEAN("boolean"), //
	NUMBER("number"), //
	OPTION_GROUP("optionGroup"), //
	;

	private final String typeName;

	private DisplayType(String typeName) {
		this.typeName = typeName;
	}

	public String getTypeName() {
		return this.typeName;
	}

}