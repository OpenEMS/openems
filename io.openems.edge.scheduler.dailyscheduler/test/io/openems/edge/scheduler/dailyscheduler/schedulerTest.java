package io.openems.edge.scheduler.dailyscheduler;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class schedulerTest {

	// private final Logger log = LoggerFactory.getLogger(testtt.class);
	private final TreeMap<LocalTime, List<String>> contollersList = new TreeMap<>();

	@Test
	public void test() {

		String S = "[{\r\n" + "	\"time\": \"00:00\",\r\n" + "	\"controller\": [\"ctrlCharge0\"]\r\n" + "}, {\r\n"
				+ "	\"time\": \"16:45\",\r\n" + "	\"controller\": [\"ctrlCharge0\", \"ctrlCharge1\"]\r\n" + "}]";

		try {
			JsonArray controllerTime = JsonUtils.getAsJsonArray(JsonUtils.parse(S));

			for (JsonElement element : controllerTime) {

				LocalTime Time = LocalTime.parse(JsonUtils.getAsString(element, "time"));
				JsonArray controllers = JsonUtils.getAsJsonArray(element, "controller");
				List<String> controllersList= new ArrayList<>();
				for(JsonElement id : controllers) {
					//JsonUtils.getAsString(id);
					controllersList.add(JsonUtils.getAsString(id));
				}
				
				this.contollersList.put(Time, controllersList);

			}
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
		
		LocalTime currentTime = LocalTime.now();
		
		System.out.print(contollersList.lowerEntry(currentTime).getValue());
	}

}
