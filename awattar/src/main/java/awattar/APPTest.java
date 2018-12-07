package awattar;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;



public class APPTest {
	private static float minPrice = Float.MAX_VALUE;
	private static long start_timestamp = 0;
	private static long end_timestamp = 0;
	private static int cheapestHour = 0;
	private static LocalDateTime startTimeStamp = null;
	// private static LocalDateTime endTimeStamp = null;
	private static long chargebleConsumption = 0;
	private static Map<LocalDateTime, Float> HourlyData = new TreeMap<>();
	private static LocalDateTime endTimeStamp = null;

	public static void main(String[] args) {
		try {

			JsonParser parser = new JsonParser();

			URL url = APPTest.class.getResource("Data.json");

			Object obj;
			obj = (parser).parse(new FileReader(url.getPath())).getAsJsonObject();
			JsonObject jsonObject = (JsonObject) obj;
			JsonArray data = (JsonArray) jsonObject.get("data");
			//List<Float> hourlyPrices = new ArrayList<Float>();
			
			

			for (JsonElement element : data) {
				JsonObject jsonelement = (JsonObject) element;

				float marketPrice = jsonelement.get("marketprice").getAsFloat();
				long start_Timestamp = jsonelement.get("start_timestamp").getAsLong();
				long end_Timestamp = jsonelement.get("start_timestamp").getAsLong();
				endTimeStamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(end_Timestamp), ZoneId.systemDefault());
				startTimeStamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(start_Timestamp), ZoneId.systemDefault());
				HourlyData.put(startTimeStamp, marketPrice);
				//hourlyPrices.add(((JsonObject) element).get("marketprice").getAsFloat());


//				 if (marketPrice < minPrice) {
//				 minPrice = marketPrice;
//				 start_timestamp = jsonelement.get("start_timestamp").getAsLong();
//				 end_timestamp = jsonelement.get("end_timestamp").getAsLong();
//				 }
			}

//			for (Map.Entry<LocalDateTime, Float> entry : HourlyData.entrySet()) {
//				System.out.println("Key: " + entry.getKey() + ". Value: " + entry.getValue());
//			}
//			System.out.println("Price: " + minPrice);
//			System.out.println("start_timestamp: " + start_timestamp + " end_timestamp: " + end_timestamp);
//			//startTimeStamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(start_timestamp), ZoneId.systemDefault());
//			// endTimeStamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(end_timestamp), ZoneId.systemDefault());
//			cheapestHour = startTimeStamp.getHour();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static TreeMap<LocalDateTime, Long> getCheapestHours(LocalDateTime start, LocalDateTime end,
			long availableConsumption, long consumptionPerSecond) {

		TreeMap<LocalDateTime, Long> result = new TreeMap<LocalDateTime, Long>();

		for (Map.Entry<LocalDateTime, Float> entry : HourlyData.entrySet()) {
			if (entry.getValue() < minPrice && entry.getKey().isBefore(end)) {
				end = entry.getKey();
				minPrice = entry.getValue();
			}

		}

		long seconds = ChronoUnit.SECONDS.between(start, end);
		long neededConsumption = (long) (consumptionPerSecond * seconds);
		long totalConsumption = 10000;
		chargebleConsumption = totalConsumption - neededConsumption - chargebleConsumption;
		if (availableConsumption >= neededConsumption) {

			result.put(end, chargebleConsumption);
			return result;
		}
		
		result.put(end, chargebleConsumption);
		getCheapestHours(start, end, availableConsumption, consumptionPerSecond);

		result.putAll(getCheapestHours(start, end, availableConsumption,
		consumptionPerSecond));
		return result;
	}

	public static int getCheapestHour() {
		return cheapestHour;
	}

	public static long startTimeStamp() {
		return start_timestamp;

	}

	public static LocalDateTime endTimeStamp() {
		return endTimeStamp;

	}
}