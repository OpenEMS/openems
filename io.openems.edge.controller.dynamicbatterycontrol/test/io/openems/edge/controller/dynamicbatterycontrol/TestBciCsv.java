package io.openems.edge.controller.dynamicbatterycontrol;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class TestBciCsv {

//	private TreeMap<DateTime, Float> bciList = new TreeMap<ZonedDateTime, Float>();

	public static void main(String[] args) {

		System.out.println("Started");
		List<BciListFromCsv> bcilistFromCsv = readListFromCsv(
				"C:\\Users\\sagar.venu\\git\\DynamicBatteryControl\\io.openems.edge.controller.dynamicbatterycontrol\\test\\io\\openems\\edge\\controller\\dynamicbatterycontrol\\BciCsv.csv");

		System.out.println("Finished");
		for (BciListFromCsv b : bcilistFromCsv) {

//			this.bciList.put(b.getTimeStamp(), b.getDate());
			System.out.println("b " + b.getBci() + "  " + b.getDate());
		}

//		return this.bciList;
	}

	private static List<BciListFromCsv> readListFromCsv(String fileName) {
		List<BciListFromCsv> bcilistFromCsv = new ArrayList<>();

		Path pathToFile = Paths.get(fileName);
		System.out.println(pathToFile.toAbsolutePath());

		try {
			BufferedReader br = new BufferedReader(
					new InputStreamReader(new FileInputStream(pathToFile.toAbsolutePath().toString()), "utf-8"));

			br.readLine();

			// read the first line from the text file
			String line = null;

			// loop until all lines are read
			while ((line = br.readLine()) != null) {

				String[] attributes = line.split(",");

				BciListFromCsv bci = createBci(attributes);
				bcilistFromCsv.add(bci);

				line = br.readLine();

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return bcilistFromCsv;
	}

	private static BciListFromCsv createBci(String[] metadata) {

		long timeStamp = Long.parseLong(metadata[0].trim());

		LocalDate date = Instant.ofEpochMilli(timeStamp).atZone(ZoneId.systemDefault()).toLocalDate();

		float bci = Float.parseFloat(metadata[2]);

		return new BciListFromCsv(date, bci);
	}

}

class BciListFromCsv {
	private LocalDate date;
	private float bci;

	public float getBci() {
		return bci;
	}

	public BciListFromCsv(LocalDate date, float bci) {
		this.date = date;
		this.bci = bci;
	}

	public LocalDate getDate() {
		return date;
	}

}
