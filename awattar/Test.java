package io.openems.impl.controller.symmetric.awattar;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

public class Test {

	class HourlyData {
		int price;

		public HourlyData(int price) {
			this.price = price;
		}
	}

	TreeMap<LocalDateTime, HourlyData> data = new TreeMap<>();

	public static void main(String[] args) {
		Test t = new Test();
		t.run();
	}

	private void run() {
		LocalDateTime t1 = LocalDateTime.of(0, 1, 1, 1, 1);
		LocalDateTime t2 = LocalDateTime.of(0, 1, 1, 1, 2);
		LocalDateTime t3 = LocalDateTime.of(0, 1, 1, 1, 3);
		LocalDateTime t4 = LocalDateTime.of(0, 1, 1, 1, 4);

		data.put(t1, new HourlyData(10));
		data.put(t2, new HourlyData(5));
		data.put(t3, new HourlyData(20));
		data.put(t4, new HourlyData(15));

		data.forEach((time, data) -> {
			System.out.println(time + ", " + data.price);
		});

		System.out.println("--");

		Collection<HourlyData> col = data.subMap(t1, t3).values();
		List<HourlyData> l = new ArrayList<>(col);

		l.forEach(data -> {
			System.out.println(data.price);
		});

		Collections.sort(l, (d1, d2) -> {
			return Integer.compare(d1.price, d2.price);
		});

		System.out.println("--");

		l.forEach(data -> {
			System.out.println(data.price);
		});
	}

	private List<Integer> getCheapestHours(int start, int end) {
		List<Integer> result = new ArrayList<>();
		if (true) {
			return result;
		}
		result.addAll(getCheapestHours(start, end));
		return result;
	}

}
