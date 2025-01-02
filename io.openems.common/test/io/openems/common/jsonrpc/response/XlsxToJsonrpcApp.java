package io.openems.common.jsonrpc.response;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class XlsxToJsonrpcApp {

	private static final String PATH = "";
	private static final String FILE_IN = "file.xlsx";
	private static final String FILE_OUT = "file.json";

	/**
	 * Tool to convert from XLSX to JSONRPC.
	 * 
	 * @param args arguments
	 * @throws IOException on error
	 */
	public static void main(String[] args) throws IOException {
		var timestamps = new JsonArray();
		var data = new JsonObject();

		try (//
				var is = new FileInputStream(Path.of(PATH, FILE_IN).toFile());
				var wb = new ReadableWorkbook(is) //
		) {
			var ws = wb.getFirstSheet();
			var rows = ws.read();
			var cols = rows.get(0).stream() //
					.skip(1) //
					.map(Cell::asString) //
					.toArray(String[]::new);
			for (var col : cols) {
				data.add(col, new JsonArray());
			}
			for (var i = 1; i < rows.size(); i++) {
				var row = rows.get(i);
				timestamps.add(row.getCell(0).asDate().toString() + ":00Z");
				for (var j = 0; j < cols.length; j++) {
					var cell = row.getCell(j + 1);
					if (cell == null) {
						continue;
					}
					data.get(cols[j]).getAsJsonArray()//
							.add(cell.asNumber());
				}
			}

			var result = new JsonObject();
			result.add("timestamps", timestamps);
			result.add("data", data);
			var json = new JsonObject();
			json.addProperty("jsonrpc", "2.0");
			json.addProperty("id", "");
			json.add("result", result);

			Files.writeString(Path.of(PATH, FILE_OUT),
					new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(json));
		}
	}

}
