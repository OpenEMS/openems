package awattar;

import java.time.LocalDateTime;
import java.util.Map;

public class main {

	public static void main(String[] args) {
		App.main(args);
		LocalDateTime now = LocalDateTime.now();
		System.out.println(App.getCheapestHours(now, App.endTimeStamp(), 3000L, 2L));
		for (Map.Entry<LocalDateTime, Long> entry : App.result.entrySet()) {
			System.out.println("Key: " + entry.getKey() + ". Value: " + entry.getValue());
		}
			
		}
	}
