package io.openems.edge.sma.ess.enums;

import io.openems.common.types.OptionsEnum;

/**
 * Device types of the SMA Sunny Island as reported in Modbus register 30053
 * ("Device type").
 *
 * <p>
 * The generation -11 does not provide register 30231 ("Maximum active
 * power device"). For these types a fixed maximum apparent power of one device
 * is defined. All other types provide the register and have no fixed value
 * (null).
 *
 * <p>
 * The values are per device. For a three-phase Master/Slave cluster the caller
 * has to multiply them by the number of devices.
 */
public enum SunnyIslandDeviceType implements OptionsEnum {

	UNDEFINED(-1, "Undefined", null), //

	// Generation -11: register 30231 is not available -> fixed value
	SI_3_0M_11(9278, "Sunny Island 3.0M-11", 2_300), //
	SI_4_4M_11(9279, "Sunny Island 4.4M-11", 3_300), //
	SI_6_0H_11(9223, "Sunny Island 6.0H-11", 4_600), //
	SI_8_0H_11(9224, "Sunny Island 8.0H-11", 6_000), //

	// Generation -12: register 30231 is available
	SI_3_0M_12(9331, "Sunny Island 3.0M-12", null), //
	SI_4_4M_12(9332, "Sunny Island 4.4M (SI 4.4M-12)", null), //
	SI_6_0H_12(9333, "Sunny Island 6.0H (SI 6.0H-12)", null), //
	SI_8_0H_12(9334, "Sunny Island 8.0H (SI 8.0H-12)", null), //

	// Generation -13: register 30231 is available
	SI_4_4M_13(9474, "Sunny Island 4.4M-13 (SI4.4M-13)", null), //
	SI_6_0H_13(9475, "Sunny Island 6.0H-13 (SI6.0H-13)", null), //
	SI_8_0H_13(9476, "Sunny Island 8.0H-13 (SI8.0H-13)", null); //

	private final int value;
	private final String name;
	private final Integer maxApparentPower;

	private SunnyIslandDeviceType(int value, String name, Integer maxApparentPower) {
		this.value = value;
		this.name = name;
		this.maxApparentPower = maxApparentPower;
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

	/**
	 * Gets the fixed maximum apparent power of one device in [VA].
	 *
	 * @return the maximum apparent power; null if there is no fixed value
	 */
	public Integer getMaxApparentPower() {
		return this.maxApparentPower;
	}
}
