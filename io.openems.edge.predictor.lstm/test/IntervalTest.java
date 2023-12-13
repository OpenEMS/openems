import static org.junit.Assert.fail;

import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.edge.predictor.lstm.common.HyperParameters;

public class IntervalTest {

	/**
	 * Calculates the minute value based on the provided current date and
	 * hyperparameters.
	 * This method divides the range of minutes (0-59) into groups based on the
	 * interval specified in the hyperparameters. It then determines the appropriate
	 * minute value for the given current date within the defined groups.
	 *
	 * @param nowDate         The current date and time represented by a
	 *                        ZonedDateTime.
	 * @param hyperParameters The hyperparameters containing the interval for minute
	 *                        grouping.
	 * @return The minute value calculated based on the grouping defined by the
	 *         hyperparameters.
	 */
	public static int getMinute(ZonedDateTime nowDate, HyperParameters hyperParameters) {
		int totalGroups = 60 / hyperParameters.getInterval();
		int startVal = 0;
		int endVal = 0;
		for (int i = 0; i < totalGroups; i++) {
			endVal = startVal + hyperParameters.getInterval();
			boolean check = startVal <= nowDate.getMinute() && nowDate.getMinute() < endVal;
			if (check == false) {

				startVal = endVal;

			} else {

				break;
			}
		}
  //		if (startVal == 60) {
  //			return 0;
  //		}
		return startVal;

	}

	@Test
	public void test() {

		HyperParameters hyperParameters = new HyperParameters();
		///IntervalTest obj = new IntervalTest();
		for (int i = 0; i < 60; i++) {
			ZonedDateTime nowDate = ZonedDateTime.of(2022, 12, 24, 23, 0, 0, 0, ZonedDateTime.now().getZone());

			int min = IntervalTest.getMinute(nowDate.plusMinutes(i), hyperParameters);

			System.out.println(" passed minute : " + nowDate.plusMinutes(i).getMinute() + ", Returned Minute: " + min);
		}
		fail("Not yet implemented");
	}

}
