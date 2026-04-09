package io.openems.edge.goodwe.common.enums;

public enum ReactivePowerMode {
	UNSELECTED("Unselected"), //
	FIX_PF("Fix Pf"), //
	FIX_Q("Fix Q"), //
	QU_CURVE("Q(U) Curve"), //
	COS_PHI_P_CURVE("cosPhi(P) curve"), //
	QP_CURVE("Q(P) Curve"); //

	private final String name;

	private ReactivePowerMode(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}
}