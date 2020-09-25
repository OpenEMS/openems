package io.openems.edge.ess.mr.gridcon;

import java.util.Optional;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.bydcommercial.BatteryBoxC130;
import io.openems.edge.battery.soltaro.single.versiona.SingleRack;
import io.openems.edge.battery.soltaro.single.versionb.SingleRackVersionB;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.startstop.StartStop;

public class Helper {

	public static boolean isUndefined(Battery battery) {
		for (Channel<?> c : battery.channels()) {
			if (isBatteryApiChannel(c)) {
				if (c instanceof AbstractReadChannel<?,?> && !(c instanceof WriteChannel<?>) ) {
					if (!c.value().isDefined()) {
						System.out.println("Channel " + c + " is not defined!");
						return true;
					}
				}
			}
		}
		return false;
	}
	
public static boolean isBatteryApiChannel(Channel<?> c) {
		for (io.openems.edge.common.channel.ChannelId id : Battery.ChannelId.values()) {
			if (id.equals(c.channelId())) {
				return true;
			}
		}
		return false;
	}
	
public static boolean isRunning(Battery battery) {
	return battery.getStartStop() == StartStop.START;
}

public static boolean isStopped(Battery battery) {
	return battery.getStartStop() == StartStop.STOP;
}




public boolean isError(Battery battery) {
	return isAlarmLevel2Error(battery);
}

public static boolean isAlarmLevel2Error(Battery battery) {	
	if (battery instanceof SingleRack) {		
		return (readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versiona.SingleRack.ChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_HIGH)
				|| readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versiona.SingleRack.ChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH)
				|| readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versiona.SingleRack.ChannelId.ALARM_LEVEL_2_CHA_CURRENT_HIGH)
				|| readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versiona.SingleRack.ChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_LOW)
				|| readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versiona.SingleRack.ChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW)
				|| readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versiona.SingleRack.ChannelId.ALARM_LEVEL_2_DISCHA_CURRENT_HIGH)
				|| readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versiona.SingleRack.ChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH)
				|| readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versiona.SingleRack.ChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_LOW)
				|| readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versiona.SingleRack.ChannelId.ALARM_LEVEL_2_INSULATION_LOW)
				|| readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versiona.SingleRack.ChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH)
				|| readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versiona.SingleRack.ChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW));	
	} else if (battery instanceof SingleRackVersionB) {
		return //
				readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versionb.SingleRackVersionB.ChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_LOW)
				||readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versionb.SingleRackVersionB.ChannelId.ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH)
				||readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versionb.SingleRackVersionB.ChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_DIFFERENCE_HIGH)
				||readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versionb.SingleRackVersionB.ChannelId.ALARM_LEVEL_2_INSULATION_LOW)
				||readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versionb.SingleRackVersionB.ChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_DIFFERENCE_HIGH)
				||readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versionb.SingleRackVersionB.ChannelId.ALARM_LEVEL_2_POLES_TEMPERATURE_DIFFERENCE_HIGH)
				||readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versionb.SingleRackVersionB.ChannelId.ALARM_LEVEL_2_TEMPERATURE_DIFFERENCE_HIGH)
				||readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versionb.SingleRackVersionB.ChannelId.ALARM_LEVEL_2_SOC_LOW)
				||readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versionb.SingleRackVersionB.ChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_LOW)
				||readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versionb.SingleRackVersionB.ChannelId.ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH)
				||readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versionb.SingleRackVersionB.ChannelId.ALARM_LEVEL_2_DISCHA_CURRENT_HIGH)
				||readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versionb.SingleRackVersionB.ChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW)
				||readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versionb.SingleRackVersionB.ChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_LOW)
				||readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versionb.SingleRackVersionB.ChannelId.ALARM_LEVEL_2_CHA_CURRENT_HIGH)
				||readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versionb.SingleRackVersionB.ChannelId.ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH)
				||readValueFromBooleanChannel(battery, io.openems.edge.battery.soltaro.single.versionb.SingleRackVersionB.ChannelId.ALARM_LEVEL_2_CELL_VOLTAGE_HIGH);
	} else if (battery instanceof BatteryBoxC130) {
		return false; //TODO
	}
	return false;
	
}

private static boolean readValueFromBooleanChannel(Battery battery, ChannelId channelId) {
	StateChannel r = battery.channel(channelId);
	Optional<Boolean> bOpt = r.value().asOptional();
	return bOpt.isPresent() && bOpt.get();
}

public static void startBattery(Battery battery) {
	if (battery != null && !Helper.isRunning(battery)) {
		try {
			battery.start();
		} catch (OpenemsNamedException e) {
		System.out.println("Was not able to start battery " + battery.id() + "!\n" + e.getMessage());
		}
	}
	
}

public static void stopBattery(Battery battery) {
	if (battery != null && Helper.isStopped(battery)) {
		try {
			battery.stop();
		} catch (OpenemsNamedException e) {
		System.out.println("Was not able to stop battery " + battery.id() + "!\n" + e.getMessage());
		}
	}
	
}

}
