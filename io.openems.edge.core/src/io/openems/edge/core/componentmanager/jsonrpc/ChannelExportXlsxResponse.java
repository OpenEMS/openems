package io.openems.edge.core.componentmanager.jsonrpc;

import static io.openems.edge.common.channel.ChannelUtils.getChannelNature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
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
import io.openems.edge.common.channel.EnumDoc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.StateChannelDoc;
import io.openems.edge.common.channel.internal.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Represents a JSON-RPC Response for 'channelExportXlsxRequest'.
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
	private static final int COL_TYPE = 6;
	private static final int COL_NATURE = 7;

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
					for (var channel : channels) {
						var cr = ChannelRow.fromChannel(channel);
						ws.value(row, COL_CHANNEL_ID, cr.channelId);
						ws.value(row, COL_VALUE, cr.value);
						ws.value(row, COL_UNIT, cr.unit);
						ws.value(row, COL_DESCRIPTION, cr.description);
						ws.value(row, COL_SOURCE, cr.readSource);
						ws.value(row, COL_ACCESS, cr.access);
						ws.value(row, COL_TYPE, cr.type);
						ws.value(row, COL_NATURE, cr.nature);
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

	protected static record ChannelRow(String channelId, String value, String unit, String description,
			String readSource, String access, String type, String nature) {

		protected static ChannelRow fromChannel(Channel<?> channel) {
			final var doc = channel.channelDoc();
			final var accessMode = doc.getAccessMode();

			final var value = switch (accessMode) {
			case WRITE_ONLY -> "";
			case READ_ONLY, READ_WRITE -> //
				switch (channel) {
				case EnumReadChannel erc -> erc.value().get() + ":" + erc.value().asOptionString();
				default -> channel.value().asStringWithoutUnit();
				};
			};

			final var additionalDescription = switch (accessMode) {
			// WRITE_ONLY throws IllegalArgumentException in channel.value()
			case WRITE_ONLY -> null;
			case READ_ONLY, READ_WRITE -> //
				switch (channel) {
				case StateCollectorChannel scc when (scc.value().orElse(0) != 0) -> {
					yield scc.listStates();
				}
				default -> null;
				};
			};
			final var description = doc.getText() //
					+ (additionalDescription != null ? " | " + additionalDescription : "");

			final var channelId = channel.channelId().id();
			final var unit = doc.getUnit().symbol;
			final var access = accessMode.getAbbreviation();
			final var readSource = Optional.ofNullable(channel.getMetaInfo()) //
					.map(Object::toString) //
					.orElse("");

			final var type = switch (doc) {
			case StateChannelDoc sc -> "State";
			case EnumDoc e -> "Enum";
			default -> CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, doc.getType().toString());
			};

			final var nature = getChannelNature(channel);

			return new ChannelRow(channelId, value, unit, description, readSource, access, type, nature);
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
		addTableHeader(wb, ws, row, COL_TYPE, "Type", 10);
		addTableHeader(wb, ws, row, COL_NATURE, "Nature", 10);

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
