package market.square.api;

import java.util.HashMap;
import java.util.Map;

public class LoadCurve {
	
	Map<Long, Double> chart = new HashMap<>();
	
	public void setValue(double value, long from, long duration) {
		for (long l = 0; l < duration; l++) {
			chart.put(new Long(l), new Double(value));
		}
	}
	
	public double getValue(long at) {
		return chart.get(new Long(at)).doubleValue();
	}
	
	public double getAvg(long from, long duration) {
		chart.stream().filter(map::containsKey).collect(Collectors.toList(Double.class));
	}

}
