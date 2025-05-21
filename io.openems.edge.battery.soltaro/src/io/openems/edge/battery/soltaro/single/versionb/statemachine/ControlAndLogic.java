package io.openems.edge.battery.soltaro.single.versionb.statemachine;

import java.util.Optional;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.soltaro.single.versionb.BatterySoltaroSingleRackVersionB;
import io.openems.edge.battery.soltaro.single.versionb.enums.ContactorControl;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class ControlAndLogic {

	private static final Integer SYSTEM_RESET = 0x1;
	private static final Integer SLEEP = 0x1;
	protected static final int RETRY_COMMAND_SECONDS = 30;
	protected static final int RETRY_COMMAND_MAX_ATTEMPTS = 10;

	protected static void stopSystem(BatterySoltaroSingleRackVersionB singleRackVersionB) throws OpenemsNamedException {
		// To avoid hardware damages do not send stop command if system has already
		// stopped
		if (singleRackVersionB.getContactorControl() != ContactorControl.CUT_OFF) {
			singleRackVersionB.setContactorControl(ContactorControl.CUT_OFF);
		}
	}

	protected static void startSystem(BatterySoltaroSingleRackVersionB singleRackVersionB)
			throws OpenemsNamedException {
		// To avoid hardware damages do not send start command if system has already
		// started
		if (singleRackVersionB.getContactorControl() != ContactorControl.ON_GRID
				&& singleRackVersionB.getContactorControl() != ContactorControl.CONNECTION_INITIATING) {
			singleRackVersionB.setContactorControl(ContactorControl.CONNECTION_INITIATING);
		}
	}

	protected static void resetSystem(BatterySoltaroSingleRackVersionB singleRackVersionB)
			throws OpenemsNamedException {
		singleRackVersionB.setSystemReset(SYSTEM_RESET);
	}

	protected static void sleepSystem(BatterySoltaroSingleRackVersionB singleRackVersionB)
			throws OpenemsNamedException {
		singleRackVersionB.setSleep(SLEEP);
	}

	protected static boolean isSystemRunning(BatterySoltaroSingleRackVersionB singleRackVersionB) {
		return singleRackVersionB.getContactorControl() == ContactorControl.ON_GRID;
	}

	protected static boolean isSystemStopped(BatterySoltaroSingleRackVersionB singleRackVersionB) {
		return singleRackVersionB.getContactorControl() == ContactorControl.CUT_OFF;
	}

	protected static boolean hasError(BatterySoltaroSingleRackVersionB singleRackVersionB,
			Optional<Integer> numberOfModules) {
		return isAlarmLevel2Error(singleRackVersionB) || isSlaveCommunicationError(singleRackVersionB, numberOfModules);
	}

	private static boolean isAlarmLevel2Error(BatterySoltaroSingleRackVersionB singleRackVersionB) {
		return readValueFromBooleanChannel(singleRackVersionB,
				BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_HIGH)
				|| readValueFromBooleanChannel(singleRackVersionB,
						BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH)
				|| readValueFromBooleanChannel(singleRackVersionB,
						BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_CHA_CURRENT_HIGH)
				|| readValueFromBooleanChannel(singleRackVersionB,
						BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_LOW)
				|| readValueFromBooleanChannel(singleRackVersionB,
						BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW)
				|| readValueFromBooleanChannel(singleRackVersionB,
						BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_DISCHA_CURRENT_HIGH)
				|| readValueFromBooleanChannel(singleRackVersionB,
						BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH)
				|| readValueFromBooleanChannel(singleRackVersionB,
						BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_LOW)
				|| readValueFromBooleanChannel(singleRackVersionB,
						BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_SOC_LOW)
				|| readValueFromBooleanChannel(singleRackVersionB,
						BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_TEMPERATURE_DIFFERENCE_HIGH)
				|| readValueFromBooleanChannel(singleRackVersionB,
						BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_POLES_TEMPERATURE_DIFFERENCE_HIGH)
				|| readValueFromBooleanChannel(singleRackVersionB,
						BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_DIFFERENCE_HIGH)
				|| readValueFromBooleanChannel(singleRackVersionB,
						BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_INSULATION_LOW)
				|| readValueFromBooleanChannel(singleRackVersionB,
						BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_DIFFERENCE_HIGH)
				|| readValueFromBooleanChannel(singleRackVersionB,
						BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH)
				|| readValueFromBooleanChannel(singleRackVersionB,
						BatterySoltaroSingleRackVersionB.ChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW);
	}

	private static boolean isSlaveCommunicationError(BatterySoltaroSingleRackVersionB singleRackVersionB,
			Optional<Integer> numberOfModules) {
		var b = false;
		switch (numberOfModules.orElse(0)) {
		case 20:
			b = b || readValueFromBooleanChannel(singleRackVersionB,
					BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_20_COMMUNICATION_ERROR);
			// fall-through
		case 19:
			b = b || readValueFromBooleanChannel(singleRackVersionB,
					BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_19_COMMUNICATION_ERROR);
			// fall-through
		case 18:
			b = b || readValueFromBooleanChannel(singleRackVersionB,
					BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_18_COMMUNICATION_ERROR);
			// fall-through
		case 17:
			b = b || readValueFromBooleanChannel(singleRackVersionB,
					BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_17_COMMUNICATION_ERROR);
			// fall-through
		case 16:
			b = b || readValueFromBooleanChannel(singleRackVersionB,
					BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_16_COMMUNICATION_ERROR);
			// fall-through
		case 15:
			b = b || readValueFromBooleanChannel(singleRackVersionB,
					BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_15_COMMUNICATION_ERROR);
			// fall-through
		case 14:
			b = b || readValueFromBooleanChannel(singleRackVersionB,
					BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_14_COMMUNICATION_ERROR);
			// fall-through
		case 13:
			b = b || readValueFromBooleanChannel(singleRackVersionB,
					BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_13_COMMUNICATION_ERROR);
			// fall-through
		case 12:
			b = b || readValueFromBooleanChannel(singleRackVersionB,
					BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_12_COMMUNICATION_ERROR);
			// fall-through
		case 11:
			b = b || readValueFromBooleanChannel(singleRackVersionB,
					BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_11_COMMUNICATION_ERROR);
			// fall-through
		case 10:
			b = b || readValueFromBooleanChannel(singleRackVersionB,
					BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_10_COMMUNICATION_ERROR);
			// fall-through
		case 9:
			b = b || readValueFromBooleanChannel(singleRackVersionB,
					BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_9_COMMUNICATION_ERROR);
			// fall-through
		case 8:
			b = b || readValueFromBooleanChannel(singleRackVersionB,
					BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_8_COMMUNICATION_ERROR);
			// fall-through
		case 7:
			b = b || readValueFromBooleanChannel(singleRackVersionB,
					BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_7_COMMUNICATION_ERROR);
			// fall-through
		case 6:
			b = b || readValueFromBooleanChannel(singleRackVersionB,
					BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_6_COMMUNICATION_ERROR);
			// fall-through
		case 5:
			b = b || readValueFromBooleanChannel(singleRackVersionB,
					BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_5_COMMUNICATION_ERROR);
			// fall-through
		case 4:
			b = b || readValueFromBooleanChannel(singleRackVersionB,
					BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_4_COMMUNICATION_ERROR);
			// fall-through
		case 3:
			b = b || readValueFromBooleanChannel(singleRackVersionB,
					BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_3_COMMUNICATION_ERROR);
			// fall-through
		case 2:
			b = b || readValueFromBooleanChannel(singleRackVersionB,
					BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_2_COMMUNICATION_ERROR);
			// fall-through
		case 1:
			b = b || readValueFromBooleanChannel(singleRackVersionB,
					BatterySoltaroSingleRackVersionB.ChannelId.SLAVE_1_COMMUNICATION_ERROR);
		}

		return b;
	}

	private static boolean readValueFromBooleanChannel(OpenemsComponent component,
			BatterySoltaroSingleRackVersionB.ChannelId singleRackChannelId) {
		StateChannel r = component.channel(singleRackChannelId);
		var bOpt = r.value().asOptional();
		return bOpt.isPresent() && bOpt.get();
	}

	/**
	 * Sets the watchdog.
	 *
	 * @param singleRackVersionB the battery
	 * @param timeSeconds        the time in seconds
	 */
	public static void setWatchdog(BatterySoltaroSingleRackVersionB singleRackVersionB, int timeSeconds) {
		try {
			singleRackVersionB.setWatchdog(timeSeconds);
		} catch (OpenemsNamedException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Sets the soc low alarm.
	 *
	 * @param singleRackVersionB the battery
	 * @param soCLowAlarm        the value to set
	 */
	public static void setSoCLowAlarm(BatterySoltaroSingleRackVersionB singleRackVersionB, int soCLowAlarm) {
		try {
			singleRackVersionB.setSocLowProtection(soCLowAlarm);
			singleRackVersionB.setSocLowProtectionRecover(soCLowAlarm);
		} catch (OpenemsNamedException e) {
			System.out.println(e.getMessage());
		}
	}

}
