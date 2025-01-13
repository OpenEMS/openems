package io.openems.common.jsonrpc.response;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;

import org.dhatim.fastexcel.Workbook;

import com.google.gson.JsonParser;

public class JsonrpcToXlsxApp {

	private static final String PATH = "";
	private static final String FILE_IN = "file.json";
	private static final String FILE_OUT = "file.xlsx";

	/**
	 * Tool to convert from JSONRPC to XLSX.
	 * 
	 * @param args arguments
	 * @throws IOException on error
	 */
	public static void main(String[] args) throws IOException {
		var json = JsonParser.parseString(//
				String.join("", //
						Files.readAllLines(Path.of(PATH, FILE_IN))))
				.getAsJsonObject();
		if (json.has("result")) {
			// handle full JSONRPC-Response and UI log output object
			json = json.getAsJsonObject("result");
		}
		var timestamps = json.getAsJsonArray("timestamps");
		var data = json.getAsJsonObject("data");

		try (//
				var os = new FileOutputStream(Path.of(PATH, FILE_OUT).toFile());
				var wb = new Workbook(os, "", null) //
		) {
			var ws = wb.newWorksheet(FILE_IN);
			var cols = data.keySet().toArray(String[]::new);

			// Header
			ws.value(0, 0, "Timestamp");
			for (var i = 0; i < cols.length; i++) {
				ws.value(0, i + 1, cols[i]);
			}

			// Data
			for (var i = 0; i < timestamps.size(); i++) {
				var timestamp = ZonedDateTime.parse(timestamps.get(i).getAsString());
				ws.value(i + 1, 0, timestamp);
				ws.style(i + 1, 0).format("yyyy-MM-dd H:mm:ss").set();
				for (var j = 0; j < cols.length; j++) {
					var d = data.get(cols[j]).getAsJsonArray().get(i);
					ws.value(i + 1, j + 1, d.isJsonNull() ? null : d.getAsNumber());
				}
			}
		}
	}

}
