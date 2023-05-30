package io.openems.edge.core.componentmanager.jsonrpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import com.google.common.base.CaseFormat;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.response.Base64PayloadResponse;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.internal.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Represents a JSON-RPC Response for 'channelExportXlsxRequest'.
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
public class ChannelExportXlsxResponse extends Base64PayloadResponse {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss Z");

	private static final int COL_CHANNEL_ID = 0;
	private static final int COL_VALUE = 1;
	private static final int COL_UNIT = 2;
	private static final int COL_DESCRIPTION = 3;
	private static final int COL_SOURCE = 4;
	private static final int COL_ACCESS = 5;

	public ChannelExportXlsxResponse(UUID id, OpenemsComponent component) throws OpenemsException {
		super(id, generatePayload(component));
	}

	protected static byte[] generatePayload(OpenemsComponent component) throws OpenemsException {
		Worksheet ws = null;
		try {
			try (var os = new ByteArrayOutputStream()) {
				Workbook wb = null;
				try {
					wb = new Workbook(os, "OpenEMS Modbus-Register Export", "1.0");
					ws = wb.newWorksheet("Modbus-Registers");

					// Add headers
					var row = addSheetHeader(wb, ws, component);

					ws.setAutoFilter(row, COL_CHANNEL_ID, COL_ACCESS);

					// Create Sheet
					List<Channel<?>> channels = component.channels().stream() //
							.sorted((c1, c2) -> c1.channelId().name().compareTo(c2.channelId().name())) //
							.toList(); //
					for (Channel<?> channel : channels) {
						/*
						 * create descriptive text
						 */
						var description = "";
						if (channel instanceof EnumReadChannel) {
							try {
								description += channel.value().asOptionString();
							} catch (IllegalArgumentException e) {
								description += "UNKNOWN OPTION VALUE [" + channel.value().asString() + "]";
								description += "ERROR: " + e.getMessage();
							}

						} else if (channel instanceof StateChannel
								&& ((StateChannel) channel).value().orElse(false) == true) {
							if (!description.isEmpty()) {
								description += "; ";
							}
							description += ((StateChannel) channel).channelDoc().getText();

						} else if (channel instanceof StateCollectorChannel
								&& ((StateCollectorChannel) channel).value().orElse(0) != 0) {
							if (!description.isEmpty()) {
								description += "; ";
							}
							description += ((StateCollectorChannel) channel).listStates();
						}

						ws.value(row, COL_CHANNEL_ID, channel.channelId().id());

						switch (channel.channelDoc().getAccessMode()) {
						case WRITE_ONLY:
							break;
						case READ_ONLY:
						case READ_WRITE:
							ws.value(row, COL_VALUE, channel.value().asStringWithoutUnit());
							break;
						}

						ws.value(row, COL_UNIT, channel.channelDoc().getUnit().getSymbol());
						ws.value(row, COL_DESCRIPTION, description);
						ws.value(row, COL_ACCESS, channel.channelDoc().getAccessMode().getAbbreviation());

						// Source
						final var readSource = channel.getMetaInfo();
						if (readSource != null) {
							ws.value(row, COL_SOURCE, readSource.toString());
						}

						row++;
					}
				} finally {
					if (wb != null) {
						wb.finish();
					}
				}
				os.flush();
				return os.toByteArray();
			}
		} catch (IOException e) {
			throw new OpenemsException("Unable to generate Xlsx payload: " + e.getMessage());
		}
	}

	private static int addSheetHeader(Workbook wb, Worksheet ws, OpenemsComponent component) {
		var row = 0;
		addHeader(wb, ws, row++, "Export created on", ZonedDateTime.now().format(DATE_TIME_FORMATTER));
		addHeader(wb, ws, row++, "Version", OpenemsConstants.VERSION.toString());
		addHeader(wb, ws, row++, "", "");

		addHeader(wb, ws, row++, "Component-ID", component.id());
		addHeader(wb, ws, row++, "Service-ID", component.servicePid());
		addHeader(wb, ws, row++, "Implementation", reducePackageName(component.getClass()));

		var inheritances = getInheritanceViaReflection(component.getClass(), null).asMap();
		for (Entry<Inheritance, Collection<String>> entry : inheritances.entrySet()) {
			var inheritance = entry.getKey();
			var names = entry.getValue();
			var first = true;
			for (String name : names) {
				if (first) {
					addHeader(wb, ws, row++, CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, inheritance.name()),
							name);
				} else {
					addHeader(wb, ws, row++, "", name);
				}
				first = false;
			}
		}

		row++;

		addTableHeader(wb, ws, row, COL_CHANNEL_ID, "Channel", 20);
		addTableHeader(wb, ws, row, COL_VALUE, "Value", 35);
		addTableHeader(wb, ws, row, COL_UNIT, "Unit", 20);
		addTableHeader(wb, ws, row, COL_DESCRIPTION, "Description", 25);
		addTableHeader(wb, ws, row, COL_SOURCE, "Read Source", 20);
		addTableHeader(wb, ws, row, COL_ACCESS, "Access", 10);

		return ++row;
	}

	private static void addHeader(Workbook wb, Worksheet ws, int row, String title, String value) {
		ws.value(row, 0, title);
		ws.value(row, 1, value);
	}

	private static void addTableHeader(Workbook wb, Worksheet ws, int row, int col, String title, int width) {
		ws.width(col, width);
		ws.value(row, col, title);
		ws.style(row, col).bold().set();
	}

	private enum Inheritance {
		EXTEND, IMPLEMENT;
	}

	private static Multimap<Inheritance, String> getInheritanceViaReflection(Class<?> clazz,
			Multimap<Inheritance, String> map) {
		if (map == null) {
			map = HashMultimap.create();
		}
		Class<?> superClazz = clazz.getSuperclass();
		if (superClazz != null && !superClazz.equals(Object.class)) {
			map.put(Inheritance.EXTEND, reducePackageName(superClazz));
			getInheritanceViaReflection(superClazz, map);
		}
		for (Class<?> iface : clazz.getInterfaces()) {
			map.put(Inheritance.IMPLEMENT, reducePackageName(iface));
			getInheritanceViaReflection(iface, map);
		}
		return map;
	}

	private static String reducePackageName(Class<?> clazz) {
		return reducePackageName(clazz.getName());
	}

	private static String reducePackageName(String name) {
		if (name.startsWith("io.openems.edge.")) {
			return name.substring(16);
		}
		return name;
	}

}
