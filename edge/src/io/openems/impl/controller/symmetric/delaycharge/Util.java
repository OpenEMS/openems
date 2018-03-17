package io.openems.impl.controller.symmetric.delaycharge;

import java.time.LocalDateTime;

public class Util {

	public static long currentSecondOfDay() {
		LocalDateTime now = LocalDateTime.now();
		return now.getHour() * 3600 + now.getMinute() * 60 + now.getSecond();
	}

}
