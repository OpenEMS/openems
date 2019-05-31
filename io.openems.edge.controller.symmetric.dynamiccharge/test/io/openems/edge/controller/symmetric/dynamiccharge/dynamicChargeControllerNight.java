package io.openems.edge.controller.symmetric.dynamiccharge;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class dynamicChargeControllerNight {
	static float demand = 0;
	private static TreeMap<LocalDateTime, Float> hourlyConsumption = new TreeMap<LocalDateTime, Float>();
	private static TreeMap<LocalDateTime, Float> hourlyPrices = new TreeMap<LocalDateTime, Float>();
	private static TreeMap<LocalDateTime, Float> chargeSchedule = new TreeMap<LocalDateTime, Float>();
	private static TreeMap<Float, Float> currentCharge = new TreeMap<Float, Float>();
	private static float SoC;
	private static LocalDateTime hour;
	private static float price;
	private static LocalDateTime start_hour_date = null;
	private static LocalDateTime end_hour_date = null;
	private static float minPrice = Float.MAX_VALUE;
	private static LocalDateTime cheapTimeStamp = null;
	private static int capacity = 12000;
	private static float totalDemand;
	private static float currentDemand;
	static float dynamicChargeDemandPrice = 0;
	private static long chargebleConsumption;
	private static long demand_Till_Cheapest_Hour;
	private static long availableCapacity;
	private static long nettCapacity;
	private static long maxApparentPower;

	private final static Ess ess = new Ess();

	private static class Data {
		public long Hours;
		private float pv;
		private float consumption;
		public float prices;
		public float soc;

		public Data(String line) {
			String[] data = line.split(",");
			Hours = Long.parseLong(data[0]);
			pv = Float.parseFloat(data[1]);
			consumption = Float.parseFloat(data[2]);
			soc = Float.parseFloat(data[3]);
			prices = Float.parseFloat(data[4]);
		}

		@Override
		public String toString() {
			return "Data [Hours=" + Hours + ", pv=" + pv + ", consumption=" + consumption + ", prices=" + prices
					+ ", Soc =" + soc + "]";
		}

	}

	private enum State {
		UP, DOWN, DOWN_CAL, UP_CAL, CALCULATE_CHARGE;
	}

	private static State currentState = State.DOWN_CAL;

	public static void main(String args[]) throws FileNotFoundException, IOException {
		try {
			File file = new File(
					"C:\\\\\\\\Users\\\\\\\\sagar.venu\\\\\\\\git\\\\\\\\old-master\\\\\\\\awattar\\\\\\\\src\\\\\\\\main\\\\\\\\java\\\\\\\\masterThesisTest\\\\\\\\14th - 16th May - HourlyData.csv");
			List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
			Data[] data = new Data[lines.size()];
			for (int i = 1; i < lines.size(); i++) {
				data[i - 1] = new Data(lines.get(i));
				// System.out.println(data);
			}
			System.out.println("Hours and prices to schedule the battery:  ");
			for (Data data2 : data) {
				if (data2 != null && data2.Hours >= 1526403600 && data2.Hours < 1526454000) {
					price = data2.prices;
					hour = LocalDateTime.ofInstant(Instant.ofEpochSecond(data2.Hours), ZoneId.systemDefault());
					hourlyPrices.put(hour, price);
					System.out.println(hour + " " + price);
				}
			}
			System.out.println(" ======================================================= ");

			for (Data data1 : data) {
				if (data1 != null) {

					switch (currentState) {
					case DOWN_CAL:
						System.out.println("Entering Down Cal");
						if (data1.pv > data1.consumption) {
							currentState = State.UP;
						} else if (start_hour_date != null) {
							if (start_hour_date.isBefore(
									LocalDateTime.ofInstant(Instant.ofEpochSecond(data1.Hours), ZoneId.systemDefault()))
									&& LocalDateTime
											.ofInstant(Instant.ofEpochSecond(data1.Hours), ZoneId.systemDefault())
											.getHour() >= 8
									&& LocalDateTime
											.ofInstant(Instant.ofEpochSecond(data1.Hours), ZoneId.systemDefault())
											.getHour() <= 10) {
								currentState = State.UP;
							}
							demand = data1.consumption - data1.pv;
							totalDemand += demand;
							//System.out.println(demand);
							hourlyConsumption.put(
									LocalDateTime.ofInstant(Instant.ofEpochSecond(data1.Hours), ZoneId.systemDefault()),
									demand);
						} else if (start_hour_date == null
								&& LocalDateTime.ofInstant(Instant.ofEpochSecond(data1.Hours), ZoneId.systemDefault())
										.getHour() >= 18) {
							start_hour_date = LocalDateTime.ofInstant(Instant.ofEpochSecond(data1.Hours),
									ZoneId.systemDefault());
							demand += data1.consumption - data1.pv;
							// System.out.println(demand);
							hourlyConsumption.put(
									LocalDateTime.ofInstant(Instant.ofEpochSecond(data1.Hours), ZoneId.systemDefault()),
									demand);

						}
						break;

					case UP:
						// System.out.println("Entering UP");
						if (end_hour_date == null && start_hour_date != null) {
							end_hour_date = LocalDateTime.ofInstant(Instant.ofEpochSecond(data1.Hours),
									ZoneId.systemDefault());
							demand += data1.consumption - data1.pv;
							// System.out.println(demand);
							hourlyConsumption.put(
									LocalDateTime.ofInstant(Instant.ofEpochSecond(data1.Hours), ZoneId.systemDefault()),
									demand);
						}

						currentState = State.UP_CAL;

						break;

					case UP_CAL:
						// System.out.println("Entering UP_CAL");
						if (data1.pv < data1.consumption
								&& LocalDateTime.ofInstant(Instant.ofEpochSecond(data1.Hours), ZoneId.systemDefault())
										.getHour() >= 17) {
							if (start_hour_date == null) {
								start_hour_date = LocalDateTime.ofInstant(Instant.ofEpochSecond(data1.Hours),
										ZoneId.systemDefault());
								demand += data1.consumption - data1.pv;
								// System.out.println(demand);
								hourlyConsumption.put(LocalDateTime.ofInstant(Instant.ofEpochSecond(data1.Hours),
										ZoneId.systemDefault()), demand);
								currentState = State.DOWN;
							} else if (end_hour_date != null && LocalDateTime
									.ofInstant(Instant.ofEpochSecond(data1.Hours), ZoneId.systemDefault())
									.getHour() >= 17) { // first cycle is already done and
														// setting charge controller
								// System.out.println("calculating Charge hours ");
//								SoC = data1.soc;
//								System.out.println(SoC);
								currentDemand = data1.consumption - data1.pv;
								currentCharge.put(currentDemand, data1.prices);
								currentState = State.CALCULATE_CHARGE;
							}
						}
						break;

					case DOWN:
						// System.out.println("Entering DOWN");
						demand += data1.consumption - data1.pv;
						// System.out.println(demand);
						hourlyConsumption.put(
								LocalDateTime.ofInstant(Instant.ofEpochSecond(data1.Hours), ZoneId.systemDefault()),
								demand);
						currentState = State.DOWN_CAL;
						break;

					case CALCULATE_CHARGE:
						System.out.println("calculating CheapHours and Amount of Electricity ");
						System.out.println(" ======================================================= ");
						// System.out.println(data1.consumption);
						currentDemand = data1.consumption - data1.pv;
						currentCharge.put(currentDemand, data1.prices);
						totalDemand = hourlyConsumption.lastEntry().getValue();
						System.out.println("Total predicted consumption for whole night is " + totalDemand + "Watts");
						getCheapestHoursFirst(hourlyPrices.firstKey(), hourlyPrices.lastKey());
						break;

					}
				}
			}
		} catch (NullPointerException e) {
			System.out.println(e);
		}
	}

	private static TreeMap<LocalDateTime, Float> getCheapestHoursFirst(LocalDateTime start, LocalDateTime end) {

		// function to find the minimum priceHour
		cheapHour(start, end);
		System.out.println("Cheap Price: " + minPrice);

		demand_Till_Cheapest_Hour = calculateDemandTillThishour(hourlyConsumption.firstKey().plusDays(1),
				cheapTimeStamp);

		/*
		 * Calculates the amount of energy that needs to be charged during the cheapest
		 * price hours.
		 */

		// if the battery has sufficient energy!
		if (availableCapacity >= demand_Till_Cheapest_Hour) {
			getCheapestHoursSecond(cheapTimeStamp, hourlyConsumption.lastKey());
		}

		// if the battery doesn't has sufficient energy!
		/*
		 * During the cheap hour, Grid is used for both charging the battery and also to
		 * satisfy the current loads
		 * (hourlyConsumption.get(cheapTimeStamp.minusDays(1))).
		 */
		chargebleConsumption = (long) (totalDemand - demand_Till_Cheapest_Hour
				- hourlyConsumption.get(cheapTimeStamp.minusDays(1)));

		if (chargebleConsumption > 0) {
			if (chargebleConsumption > maxApparentPower) {
				chargebleConsumption = maxApparentPower;
				totalDemand -= chargebleConsumption;
			} else {
				totalDemand -= chargebleConsumption;
			}
			chargeSchedule.put(cheapTimeStamp, (float) chargebleConsumption);
			getCheapestHoursFirst(hourlyPrices.firstKey(), cheapTimeStamp);
		}
		return chargeSchedule;
	}

	private static TreeMap<LocalDateTime, Float> getCheapestHoursSecond(LocalDateTime start, LocalDateTime end) {

		availableCapacity -= demand_Till_Cheapest_Hour; // This will be the capacity during cheapest hour.
		chargebleConsumption = (long) (totalDemand - availableCapacity
				- hourlyConsumption.get(cheapTimeStamp.minusDays(1)));
		if (chargebleConsumption > 0) {
			if (chargebleConsumption > maxApparentPower) {
				if ((maxApparentPower + availableCapacity) > nettCapacity) {
					chargebleConsumption = nettCapacity - availableCapacity;
					totalDemand -= chargebleConsumption;
					chargeSchedule.put(cheapTimeStamp, (float) chargebleConsumption);
					availableCapacity += chargebleConsumption;
					cheapHour(cheapTimeStamp, hourlyConsumption.lastKey().plusDays(1));
					demand_Till_Cheapest_Hour = calculateDemandTillThishour(cheapTimeStamp,
							hourlyConsumption.lastKey().plusDays(1));
					getCheapestHoursSecond(cheapTimeStamp, hourlyConsumption.lastKey());
				}
				chargeSchedule.put(cheapTimeStamp, (float) maxApparentPower);
				chargebleConsumption -= maxApparentPower;
				totalDemand -= chargebleConsumption;
				availableCapacity += maxApparentPower;
				getCheapestHoursSecond(cheapTimeStamp, hourlyConsumption.lastKey());
				return chargeSchedule;
			}
			chargeSchedule.put(cheapTimeStamp, (float) chargebleConsumption);
			return chargeSchedule;
		}
		return chargeSchedule;

	}

	private static void cheapHour(LocalDateTime start, LocalDateTime end) {
		minPrice = Float.MAX_VALUE;

		// Calculates the cheapest price hour within certain Hours.
		for (Map.Entry<LocalDateTime, Float> entry : hourlyPrices.subMap(start, end).entrySet()) {
			if (entry.getValue() < minPrice) {
				cheapTimeStamp = entry.getKey();
				minPrice = entry.getValue();
			}
		}
	}

	private static long calculateDemandTillThishour(LocalDateTime start, LocalDateTime end) {
		long demand = 0;
		for (Entry<LocalDateTime, Float> entry : hourlyConsumption.entrySet()) {
			if ((entry.getKey().plusDays(1).isEqual(start) || entry.getKey().plusDays(1).isAfter(start))
					&& entry.getKey().plusDays(1).isBefore(end)) {
				demand += entry.getValue();
			}
		}
		return demand;
	}
}
