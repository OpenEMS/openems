//package io.openems.edge.controller.ess.fastfrequencyreserve.enums;
//
//import io.openems.common.types.OptionsEnum;
//
//public enum State implements OptionsEnum {
//
//	UNDEFINED(-1, "Undefined"),
//
//	PRE_ACTIVATIOM_STATE(0, "Time before the Activation"),
//
//	ACTIVATION_TIME(1, "Detected the freq dip, Start discharging"),
//
//	SUPPORT_DURATION(3, "Hold discharging"),
//
//	DEACTIVATION_TIME(4, "Set 0[W] power"),
//	
//	BUFFERED_SUPPORT(5, "Buffer support duration"),
//
//	BUFFERED_TIME_BEFORE_RECOVERY(6, "Buffer time before recovery"),
//
//	RECOVERY_TIME(7, "Recovery time")
//
//	;
//
//	private final int value;
//	private final String name;
//
//	private State(int value, String name) {
//		this.value = value;
//		this.name = name;
//	}
//
//	@Override
//	public int getValue() {
//		return this.value;
//	}
//
//	@Override
//	public String getName() {
//		return this.name;
//	}
//
//	@Override
//	public OptionsEnum getUndefined() {
//		return UNDEFINED;
//	}
//
//}
