package io.openems.impl.device.simulator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by maxo2 on 30.08.2017.
 */
import com.google.gson.JsonObject;

public class CSVLoadGenerator implements LoadGenerator {

	private String filepath = "";
	private String columnKey = "";
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	private List<String> values = new ArrayList<>(0);
	private int count = 0;
	private int columnPart = 0;
	private String separator = "";

	public CSVLoadGenerator(JsonObject config) {
		super();
		/**
		 * Get config details.
		 */
		this.filepath = config.get("filepath").getAsString();
		this.columnKey = config.get("columnKey").getAsString();

		/**
		 * Try to read the specified file and extract important information according the file's structure.
		 */
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(this.filepath));
			values = br.lines().collect(Collectors.toList());
			String[] str = values.get(0).split("=");
			separator = str[str.length - 1];
			str = values.get(1).split(separator);
			for (int i = 0; i < str.length; i++) {
				if (str[i].equals(columnKey)) {
					columnPart = i;
				}
			}
			values.remove(1);
			values.remove(0);
			count = -1;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					/* ignore */
				}
			}
		}
	}

	public CSVLoadGenerator() {}

	@Override
	public long getLoad() {
		long value = 0;
		/**
		 * Get the next line, pick the defined column and parse to long.
		 */
		try {
			count++;
			String[] str = new String[0];
			try {
				str = values.get(count).split(separator);
			} catch (IndexOutOfBoundsException e) {
				/**
				 * Restart file after its end was reached.
				 */
				log.error(e.getMessage() + " --> index reset ");
				count = 0;
				str = values.get(count).split(separator);
			}
			value = (long) Double.parseDouble(str[columnPart]);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return value;
	}

}
