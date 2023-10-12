package io.openems.edge.predictor.lstm;

import java.time.ZonedDateTime;

import org.junit.Test;
//CHECKSTYLE:OFF
public class SimpleTest {

	@Test
	public void test() {
		ZonedDateTime nowDate = ZonedDateTime.of(2023, 6, 11, 2, 50, 0, 0, ZonedDateTime.now().getZone());
		System.out.println(getMinute(nowDate));
		System.out.println(getMinutex(nowDate));

		nowDate = ZonedDateTime.of(2023, 6, 11, 2, 20, 0, 0, ZonedDateTime.now().getZone());
		System.out.println(getMinute(nowDate));
		System.out.println(getMinutex(nowDate));

		nowDate = ZonedDateTime.of(2023, 6, 11, 2, 40, 0, 0, ZonedDateTime.now().getZone());
		System.out.println(getMinute(nowDate));
		System.out.println(getMinutex(nowDate));

		nowDate = ZonedDateTime.of(2023, 6, 11, 2, 59, 0, 0, ZonedDateTime.now().getZone());
		System.out.println(getMinute(nowDate));
		System.out.println(getMinutex(nowDate));
	}

	public static Integer getMinute(ZonedDateTime fromDate) {

		int nowMinute = fromDate.getMinute();
		if (nowMinute >= 0 && nowMinute < 15) {
			return 0;
		} else if (nowMinute >= 15 && nowMinute < 30) {
			return 15;
		} else if (nowMinute >= 30 && nowMinute < 45) {
			return 30;
		}
		return 45;
	}

	public static int getMinutex(ZonedDateTime fromDate) {
		int nowMinute = fromDate.getMinute();

		System.out.println(nowMinute / 15);
		System.out.println((nowMinute / 15) * 15);
		return (nowMinute / 15) * 15;
	}
	//CHECKSTYLE:ON
}
