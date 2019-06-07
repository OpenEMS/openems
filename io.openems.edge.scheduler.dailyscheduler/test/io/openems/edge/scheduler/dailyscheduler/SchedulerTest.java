package io.openems.edge.scheduler.dailyscheduler;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;

public class SchedulerTest {

	private final TreeMap<LocalTime, List<String>> contollersList = new TreeMap<>();

	@Test
	public void test() {

		/*
		 * String S = "[{\r\n" + "	\"time\": \"00:00\",\r\n" +
		 * "	\"controller\": [\"ctrlCharge0\"]\r\n" + "}, {\r\n" +
		 * "	\"time\": \"16:45\",\r\n" +
		 * "	\"controller\": [\"ctrlCharge0\", \"ctrlCharge1\"]\r\n" + "}]";
		 */

		String S1 = "[{\r\n" + "		\"Time\": \"11:06\",\r\n" + "		\"Controllers\": []\r\n" + "	},\r\n"
				+ "	{\r\n" + "		\"Time\": \"11:07\",\r\n"
				+ "		\"Controllers\": [\"ctrlBalancingCosPhi0\", \"ctrlBalancingCosPhi1\"]\r\n" + "	},\r\n" + "\r\n"
				+ "	{\r\n" + "		\"Time\": \"11:08\",\r\n" + "		\"Controllers\": []\r\n" + "	},\r\n" + "\r\n"
				+ "	{\r\n" + "		\"Time\": \"11:09\",\r\n" + "		\"Controllers\": [\"ctrlBalancingCosPhi0\"]\r\n"
				+ "	},\r\n" + "\r\n" + "	{\r\n" + "		\"Time\": \"21:00\",\r\n" + "		\"Controllers\": []\r\n"
				+ "	}\r\n" + "]";

		try {
			JsonArray controllerTime = JsonUtils.getAsJsonArray(JsonUtils.parse(S1));
			
			/*for (int i = 0; i < controllerTime.size(); i++) {
		    JsonObject json = controllerTime.get(i).getAsJsonObject();
		    Iterator<String> keys = json.keySet().iterator();

		    while (keys.hasNext()) {
		        String key = keys.next();
		        System.out.println("Key :" + key + "  Value :" + json.get(key));
		    }

		}*/

			for (JsonElement element : controllerTime) {

				LocalTime Time = LocalTime.parse(JsonUtils.getAsString(element, "Time"));
				JsonArray controllers = JsonUtils.getAsJsonArray(element, "Controllers");
				List<String> controllersList = new ArrayList<>();
				for (JsonElement id : controllers) {

					controllersList.add(JsonUtils.getAsString(id).replaceAll("\"", ""));
				}

				this.contollersList.put(Time, controllersList);

			}
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}

		for (Map.Entry<LocalTime, List<String>> entry : this.contollersList.entrySet()) {

			if (!entry.getValue().isEmpty()) {
				//System.out.println(entry.getValue());
			}

		}

		LocalTime currentTime = LocalTime.now();

		if (!(this.contollersList.isEmpty())) {
			if (!this.contollersList.lowerEntry(currentTime).getValue().isEmpty()) {
				System.out.println(this.contollersList.lowerEntry(currentTime).getValue());
			}
		}

	}

}
