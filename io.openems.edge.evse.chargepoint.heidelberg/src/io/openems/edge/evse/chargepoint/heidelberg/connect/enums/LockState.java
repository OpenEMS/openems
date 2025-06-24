
	package io.openems.edge.evse.chargepoint.heidelberg.connect.enums;

	import io.openems.common.types.OptionsEnum;

	public enum LockState implements OptionsEnum {
	    UNDEFINED(-1, "Undefined"),
	    LOCKED(0,    "Locked"),
		UNLOCKED(1, "Unlocked");

	    private final int value;
	    private final String name;

	    LockState(int value, String name) {
	        this.value = value;
	        this.name  = name;
	    }

	    @Override
	    public int getValue() {
	        return this.value;
	    }

	    @Override
	    public String getName() {
	        return this.name;
	    }

	    @Override
	    public OptionsEnum getUndefined() {
	        return UNDEFINED;
	    }
	}
