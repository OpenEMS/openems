package awattar;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class DateTest {
	
	
	
	public static void main(String[] args) {
		
		
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime later = now.plusMinutes(45);
		
		LocalDateTime another = later.plusDays(2);
		System.out.println(another);
		
		long seconds = ChronoUnit.SECONDS.between(now, another);
		System.out.println(seconds);
		
	}

}
