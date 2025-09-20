package io.openems.edge.core.appmanager.formly.enums;

public enum InputType {
	TEXT("text"), //
	PASSWORD("password"), //
	NUMBER("number"), //
	;

	private String formlyTypeName;

	private InputType(String type) {
		this.formlyTypeName = type;
	}

	public String getFormlyTypeName() {
		return this.formlyTypeName;
	}
}