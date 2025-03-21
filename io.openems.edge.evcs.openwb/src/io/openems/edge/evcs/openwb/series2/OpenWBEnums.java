package io.openems.edge.evcs.openwb.series2;

import io.openems.common.types.OptionsEnum;

public class OpenWBEnums {
public enum ChargingActiveState implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	NOT_CHARGING(0, "No cable attached"), //
	CHARGING(1, "Cable attached, no car attached"), //
;
	
	private final int value;
	private final String name;

	private ChargingActiveState(int value, String name) {
		this.value = value;
		this.name = name;
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
};

	public enum EvseState implements OptionsEnum {
	
		UNDEFINED(-1, "Undefined"), //
		STARTING(0, "Starting"), //
		RUNNING(1, "Running"), //
		ERROR(2, "Error"), //
		;
	
		private final int value;
		private final String name;
	
		private EvseState(int value, String name) {
			this.value = value;
			this.name = name;
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

	public enum PluggedState implements OptionsEnum {
	
		UNDEFINED(-1, "Undefined"), //
		NO_VEHICLE_ATTACHED(0, "No vehicle attached"), //
		VEHICLE_ATTACHED(1, "Vehicle attached"), //
		;
	
		private final int value;
		private final String name;
	
		private PluggedState(int value, String name) {
			this.value = value;
			this.name = name;
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

	public enum HardwareType implements OptionsEnum {
	
		UNDEFINED(-1, "Undefined"), //
		Series2(1, "OpenWB series 2"), //
		Pro(2, "OpenWB Pro") //
		;
	
		private final int value;
		private final String name;
	
		private HardwareType(int value, String name) {
			this.value = value;
			this.name = name;
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
}