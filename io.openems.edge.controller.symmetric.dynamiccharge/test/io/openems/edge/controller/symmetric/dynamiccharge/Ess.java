package io.openems.edge.controller.symmetric.dynamiccharge;

import java.time.LocalDateTime;
import java.util.Map.Entry;
import java.util.TreeMap;

public class Ess {

	public void setPower(Float power, LocalDateTime hour) {
		System.out.println("Amount Bought from Grid: " + power + " During: " + hour);
		System.out.println(power);

	}

	// calculates accuracy for Hourly night consumption.

	public void checkAccuracyHourly(TreeMap<LocalDateTime, Float> treeMap) {
		float previous = 0;
		float current = 0;
		float accuracy = 0;
		float averageAccuracy = 0;
		int count = 0;
		/*
		 * takes the treeMap which has hourly night consumption as input and compares
		 * the hours with previous day's same hour consumption and derives the accuracy.
		 * 
		 */

		for (int i = 0; i < 24; i++) {
			// System.out.println("Hour: " + i);
			for (Entry<LocalDateTime, Float> entry : treeMap.entrySet()) {
				if (entry.getKey().getHour() == i) {
					if (previous == 0) {
						previous = entry.getValue();
					} else {

						current = entry.getValue();
						if (previous > current) {
							accuracy += ((current / previous) * 100);

						} else {
							accuracy += ((previous / current) * 100);
						}
						count += 1;
						previous = current;

					}

				}

			}
			averageAccuracy = accuracy / count;
			System.out.println("average Accuracy for Hour: " + i + " is " + String.format("%.2f", averageAccuracy));
			// System.out.println(String.format("%.2f", averageAccuracy));
			accuracy = 0;
			previous = 0;
			count = 0;

		}
	}

	// calculates accuracy for total night consumption
	// consumption
	/*
	 * takes the treeMap which has whole night consumption as input and compares
	 * the consumption with previous day's consumption and derives the accuracy.
	 * 
	 */
	public void checkAccuracyWholeNight(TreeMap<LocalDateTime, Float> treeMap) {
		float previous = 0;
		float current = 0;
		float accuracy = 0;
		int i = 0;

		for (Entry<LocalDateTime, Float> entry : treeMap.entrySet()) {
			if (previous == 0) {
				previous = entry.getValue();
				i += 1;
			} else {
				current = entry.getValue();
				if (previous > current) {
					accuracy = (current / previous) * 100;
					i += 1;

				} else {
					accuracy = (previous / current) * 100;
					i += 1;
				}
				previous = current;
			}

			// System.out.println(entry.getKey().getDayOfMonth() + "/" +
			// entry.getKey().getMonthValue());
			if (i > 1) {
				System.out.println(String.format("accuracy for day " + i + " is: " + "%.2f", accuracy));

			}
		}

	}

}
