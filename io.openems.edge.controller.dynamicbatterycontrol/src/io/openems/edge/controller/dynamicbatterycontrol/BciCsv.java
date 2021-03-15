package io.openems.edge.controller.dynamicbatterycontrol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class BciCsv {

	private TreeMap<LocalDate, Float> bciList = new TreeMap<LocalDate, Float>();

	public TreeMap<LocalDate, Float> dailyPrices(String url) {

		List<Bci> bcilistFromCsv = readListFromCsv("BciCsv.csv");

		for (Bci bci : bcilistFromCsv) {

			this.bciList.put(bci.getDate(), bci.getBci());
		}

		return this.bciList;
	}

	public TreeMap<LocalDate, Float> getBciList() {
		return bciList;
	}

	private List<Bci> readListFromCsv(String fileName) {
		List<Bci> bcilistFromCsv = new ArrayList<>();

//		Path pathToFile = Paths.get(fileName);

		try {
//			BufferedReader br = Files.newBufferedReader(pathToFile, StandardCharsets.US_ASCII);

			BufferedReader br = new BufferedReader(new InputStreamReader(BciCsv.class.getResourceAsStream(fileName)));

			// read the first line from the text file
			br.readLine();

			// read the first line from the text file
			String line = null;

			// loop until all lines are read
			while ((line = br.readLine()) != null) {

				String[] attributes = line.split(",");

				Bci bci = createBci(attributes);

				bcilistFromCsv.add(bci);

				line = br.readLine();

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return bcilistFromCsv;
	}

	private Bci createBci(String[] metadata) {

		long timeStamp = Long.parseLong(metadata[0]);

		LocalDate date = Instant.ofEpochMilli(timeStamp).atZone(ZoneId.systemDefault()).toLocalDate();

		float bci = Float.parseFloat(metadata[2]);

		// create and return book of this metadata return new Book(name, price, author);

		return new Bci(date, bci);
	}

}

class Bci {
	private LocalDate date;
	private float bci;

	public float getBci() {
		return bci;
	}

	public LocalDate getDate() {
		return date;
	}

	public Bci(LocalDate date, float bci) {
		this.date = date;
		this.bci = bci;
	}

}
