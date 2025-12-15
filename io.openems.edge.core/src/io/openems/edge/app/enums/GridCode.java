package io.openems.edge.app.enums;

import io.openems.common.session.Language;

public enum GridCode implements TranslatableEnum {
	VDE_4105("VDE-AR-N 4105"), //
	VDE_4110("VDE-AR-N 4110"),//
	;

	private String name;

	private GridCode(String name) {
		this.name = name;
	}

	@Override
	public String getTranslation(Language language) {
		return this.name;
	}

}
