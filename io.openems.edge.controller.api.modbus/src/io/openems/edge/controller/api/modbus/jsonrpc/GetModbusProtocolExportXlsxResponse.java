package io.openems.edge.controller.api.modbus.jsonrpc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import io.openems.common.channel.Unit;
import io.openems.common.jsonrpc.response.Base64PayloadResponse;
import io.openems.edge.common.modbusslave.ModbusRecord;
import io.openems.edge.common.modbusslave.ModbusRecordFloat32;
import io.openems.edge.common.modbusslave.ModbusRecordFloat64;
import io.openems.edge.common.modbusslave.ModbusRecordString16;
import io.openems.edge.common.modbusslave.ModbusRecordUint16;
import io.openems.edge.common.modbusslave.ModbusType;

/**
 * Represents a JSON-RPC Response for 'getModbusProtocolExportXlsx'.
 * 
 * <p>
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "payload": Base64-String
 *   }
 * }
 * </pre>
 */
public class GetModbusProtocolExportXlsxResponse extends Base64PayloadResponse {

	public GetModbusProtocolExportXlsxResponse(UUID id, TreeMap<Integer, String> components,
			TreeMap<Integer, ModbusRecord> records) {
		super(id, generatePayload(components, records));
	}

	private static final int COL_ADDRESS = 0;
	private static final int COL_DESCRIPTION = 1;
	private static final int COL_TYPE = 2;
	private static final int COL_VALUE = 3;
	private static final int COL_UNIT = 4;
	private static final int COL_ACCESS = 5;

	private static byte[] generatePayload(TreeMap<Integer, String> components, TreeMap<Integer, ModbusRecord> records) {
		byte[] payload = new byte[0];
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			Workbook wb = new Workbook(os, "MyApplication", "1.0");
			Worksheet ws = wb.newWorksheet("Sheet 1");
			
			ws.value(0, 0, "This is a string in A1");
			ws.value(0, 2, 1234);
			ws.value(0, 3, 123456L);
			ws.value(0, 4, 1.234);
			wb.finish();

			os.flush();
			payload = os.toByteArray();
			os.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		try (Workbook workbook = new XSSFWorkbook()) {
//			Sheet sheet = workbook.createSheet("Modbus-Table");
//			addSheetHeader(workbook, sheet);
//
//			// define Styles
//			CellStyle recordStyleEven = workbook.createCellStyle();
//			recordStyleEven.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
//			recordStyleEven.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//			CellStyle recordStyleOdd = workbook.createCellStyle();
//			XSSFFont componentHeaderFont = ((XSSFWorkbook) workbook).createFont();
//			componentHeaderFont.setBold(true);
//			componentHeaderFont.setFontHeight(15);
//			componentHeaderFont.setColor(IndexedColors.DARK_BLUE.getIndex());
//			CellStyle componentHeaderStyle = workbook.createCellStyle();
//			componentHeaderStyle.setFont(componentHeaderFont);
//			componentHeaderStyle.setBorderBottom(BorderStyle.THICK);
//			componentHeaderStyle.setBottomBorderColor(IndexedColors.DARK_BLUE.getIndex());
//
//			// Create Sheet
//			int nextRow = 1;
//			for (Entry<Integer, ModbusRecord> entry : records.entrySet()) {
//				int address = entry.getKey();
//
//				String component = components.get(address);
//				if (address == 0 || component != null) {
//					if (address == 0) {
//						// Add the global header row
//						addComponentHeader(sheet, componentHeaderStyle, "Header", nextRow);
//					} else {
//						// Add Component-Header-Row
//						addComponentHeader(sheet, componentHeaderStyle, component, nextRow);
//					}
//					nextRow++;
//				}
//
//				// Add a Record-Row
//				ModbusRecord record = entry.getValue();
//				if (nextRow % 2 == 0) {
//					addRecord(sheet, recordStyleEven, address, record, nextRow);
//				} else {
//					addRecord(sheet, recordStyleOdd, address, record, nextRow);
//				}
//				nextRow++;
//			}
//
//			// Add a Sheet to describe undefined values
//			addUndefinedSheet(workbook);
//			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//
//			workbook.write(outputStream);
//			payload = outputStream.toByteArray();
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		return payload;
	}

//	private static void addSheetHeader(Workbook workbook, Sheet sheet) {
//		sheet.setColumnWidth(COL_ADDRESS, 4_000);
//		sheet.setColumnWidth(COL_DESCRIPTION, 10_000);
//		sheet.setColumnWidth(COL_TYPE, 3_000);
//		sheet.setColumnWidth(COL_VALUE, 10_000);
//		sheet.setColumnWidth(COL_UNIT, 4_000);
//		sheet.setColumnWidth(COL_ACCESS, 3_000);
//		Row header = sheet.createRow(0);
//
//		CellStyle style = workbook.createCellStyle();
//		XSSFFont font = ((XSSFWorkbook) workbook).createFont();
//		font.setBold(true);
//		style.setFont(font);
//
//		Cell cell = header.createCell(COL_ADDRESS);
//		cell.setCellValue("Address");
//		cell.setCellStyle(style);
//		cell = header.createCell(COL_DESCRIPTION);
//		cell.setCellValue("Description");
//		cell.setCellStyle(style);
//		cell = header.createCell(COL_TYPE);
//		cell.setCellValue("Type");
//		cell.setCellStyle(style);
//		cell = header.createCell(COL_VALUE);
//		cell.setCellValue("Value/Range");
//		cell.setCellStyle(style);
//		cell = header.createCell(COL_UNIT);
//		cell.setCellValue("Unit");
//		cell.setCellStyle(style);
//		cell = header.createCell(COL_ACCESS);
//		cell.setCellValue("Access");
//		cell.setCellStyle(style);
//
//		// Add Auto-Filter
//		sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, COL_ACCESS));
//	}
//
//	private static void addComponentHeader(Sheet sheet, CellStyle style, String title, int rowCount) {
//		Row row = sheet.createRow(rowCount);
//		Cell cell = row.createCell(0);
//		cell.setCellValue(title);
//		cell.setCellStyle(style);
//		for (int i = 1; i <= COL_ACCESS; i++) {
//			cell = row.createCell(i);
//			cell.setCellStyle(style);
//		}
//	}
//
//	private static void addRecord(Sheet sheet, CellStyle style, int address, ModbusRecord record, int rowCount) {
//		Row row = sheet.createRow(rowCount);
//		Cell cell = row.createCell(COL_ADDRESS);
//		cell.setCellValue(address);
//		cell.setCellStyle(style);
//		cell = row.createCell(COL_DESCRIPTION);
//		cell.setCellValue(record.getName());
//		cell.setCellStyle(style);
//		cell = row.createCell(COL_TYPE);
//		cell.setCellValue(record.getType().toString());
//		cell.setCellStyle(style);
//		cell = row.createCell(COL_VALUE);
//		cell.setCellValue(record.getValueDescription());
//		cell.setCellStyle(style);
//		cell = row.createCell(COL_UNIT);
//		Unit unit = record.getUnit();
//		if (unit != Unit.NONE) {
//			cell.setCellValue(record.getUnit().toString());
//		}
//		cell.setCellStyle(style);
//		cell = row.createCell(COL_ACCESS);
//		cell.setCellValue(record.getAccessMode().getAbbreviation());
//		cell.setCellStyle(style);
//	}
//
//	/**
//	 * Add Sheet to describe UNDEFINED values.
//	 * 
//	 * @param workbook the Workbook
//	 */
//	private static void addUndefinedSheet(Workbook workbook) {
//		Sheet sheet = workbook.createSheet("Undefined values");
//		Row row;
//		Cell cell;
//
//		row = sheet.createRow(0);
//		cell = row.createCell(0);
//		cell.setCellValue("In case a modbus value is 'undefined', the following value will be read:");
//
//		row = sheet.createRow(1);
//		cell = row.createCell(0);
//		cell.setCellValue("type");
//		cell = row.createCell(1);
//		cell.setCellValue("value");
//
//		// Add Auto-Filter
//		sheet.setAutoFilter(new CellRangeAddress(1, 1, 0, 1));
//
//		int nextRow = 2;
//		for (ModbusType modbusType : ModbusType.values()) {
//			byte[] value = new byte[0];
//			switch (modbusType) {
//			case FLOAT32:
//				value = ModbusRecordFloat32.UNDEFINED_VALUE;
//				break;
//			case FLOAT64:
//				value = ModbusRecordFloat64.UNDEFINED_VALUE;
//				break;
//			case STRING16:
//				value = ModbusRecordString16.UNDEFINED_VALUE;
//				break;
//			case UINT16:
//				value = ModbusRecordUint16.UNDEFINED_VALUE;
//				break;
//			}
//
//			row = sheet.createRow(nextRow++);
//			cell = row.createCell(0);
//			cell.setCellValue(modbusType.toString());
//			cell = row.createCell(1);
//			cell.setCellValue(byteArrayToString(value));
//		}
//	}
//
//	private static String byteArrayToString(byte[] value) {
//		if (value.length == 0) {
//			return "";
//		}
//		StringBuilder result = new StringBuilder("0x");
//		for (byte b : value) {
//			result.append(Integer.toHexString(b & 0xff));
//		}
//		return result.toString();
//	}

}
