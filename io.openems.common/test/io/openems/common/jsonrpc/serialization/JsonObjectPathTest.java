package io.openems.common.jsonrpc.serialization;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.stringSerializer;
import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonNull;

import io.openems.common.channel.Level;
import io.openems.common.jsonrpc.serialization.StringPathParser.StringParserString;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.SemanticVersion;
import io.openems.common.utils.JsonUtils;

public class JsonObjectPathTest {

	private JsonObjectPath path;

	private final ChannelAddress channelAddress = new ChannelAddress("component0", "Channel");
	private final SemanticVersion semanticVersion = SemanticVersion.fromString("2020.1.1");
	private final LocalDate localDate = LocalDate.of(2025, 1, 1);
	private final ZonedDateTime zonedDateTime = ZonedDateTime.of(LocalDateTime.of(2025, 1, 1, 1, 1), ZoneId.of("UTC"));
	private final UUID uuid = UUID.randomUUID();

	@Before
	public void before() {
		this.path = new JsonObjectPathActual.JsonObjectPathActualNonNull(JsonUtils.buildJsonObject() //
				// string values
				.addProperty("string", "string") //
				.addProperty("channelAddress", this.channelAddress.toString()) //
				.addProperty("semanticVersion", this.semanticVersion.toString()) //
				.addProperty("enum", Level.WARNING) //
				.addProperty("localDate", this.localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)) //
				.addProperty("zonedDateTime", this.zonedDateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)) //
				.addProperty("uuid", this.uuid.toString()) //
				// number values
				.addProperty("number", 10) //
				.addProperty("long", 10L) //
				.addProperty("int", 10) //
				.addProperty("double", 10.5D) //
				.addProperty("float", 10.5F) //
				// boolean values
				.addProperty("boolean", true) //
				// array values
				.add("array", JsonUtils.buildJsonArray() //
						.add("string") //
						.build())
				// object values
				.add("object", JsonUtils.buildJsonObject() //
						.addProperty("string", "string") //
						.build())
				// null
				.add("null", JsonNull.INSTANCE) //
				.build());
	}

	@Test
	public void testGetJsonElementPath() {
		assertNotNull(this.path.getJsonElementPath("string"));
		assertNotNull(this.path.getJsonElementPath("number"));
		assertNotNull(this.path.getJsonElementPath("boolean"));
		assertNotNull(this.path.getJsonElementPath("array"));
		assertNotNull(this.path.getJsonElementPath("object"));
		assertNotNull(this.path.getJsonElementPath("null"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonElementPath("notExisting");
		});
	}

	@Test
	public void testGetJsonElement() {
		assertNotNull(this.path.getJsonElement("string"));
		assertNotNull(this.path.getJsonElement("number"));
		assertNotNull(this.path.getJsonElement("boolean"));
		assertNotNull(this.path.getJsonElement("array"));
		assertNotNull(this.path.getJsonElement("object"));
		assertNotNull(this.path.getJsonElement("null"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonElement("notExisting");
		});
	}

	@Test
	public void testGetJsonPrimitivePath() {
		assertNotNull(this.path.getJsonPrimitivePath("string"));
		assertNotNull(this.path.getJsonPrimitivePath("number"));
		assertNotNull(this.path.getJsonPrimitivePath("boolean"));
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonPrimitivePath("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonPrimitivePath("object");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonPrimitivePath("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonPrimitivePath("notExisting");
		});
	}

	@Test
	public void testGetJsonPrimitive() {
		assertNotNull(this.path.getJsonPrimitive("string"));
		assertNotNull(this.path.getJsonPrimitive("number"));
		assertNotNull(this.path.getJsonPrimitive("boolean"));
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonPrimitive("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonPrimitive("object");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonPrimitive("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonPrimitive("notExisting");
		});
	}

	@Test
	public void testGetNullableJsonElementPath() {
		assertNotNull(this.path.getNullableJsonElementPath("string"));
		assertNotNull(this.path.getNullableJsonElementPath("number"));
		assertNotNull(this.path.getNullableJsonElementPath("boolean"));
		assertNotNull(this.path.getNullableJsonElementPath("array"));
		assertNotNull(this.path.getNullableJsonElementPath("object"));
		assertNotNull(this.path.getNullableJsonElementPath("null"));
		assertNotNull(this.path.getNullableJsonElementPath("notExisting"));
	}

	@Test
	public void testGetNullableJsonPrimitivePath() {
		assertNotNull(this.path.getNullableJsonPrimitivePath("string"));
		assertNotNull(this.path.getNullableJsonPrimitivePath("number"));
		assertNotNull(this.path.getNullableJsonPrimitivePath("boolean"));
		assertNotNull(this.path.getNullableJsonPrimitivePath("null"));
		assertNotNull(this.path.getNullableJsonPrimitivePath("notExisting"));
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableJsonPrimitivePath("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableJsonPrimitivePath("object");
		});
	}

	@Test
	public void testCollect() {
		final var result = this.path.collect(new StringPathParser.StringParserString(),
				toMap(t -> t.getKey().get(), t -> t.getValue().get()));

		assertEquals(this.path.get().size(), result.size());
	}

	@Test
	public void testCollectStringKeys() {
		final var result = this.path.collectStringKeys(toMap(Entry::getKey, t -> t.getValue().get()));

		assertEquals(this.path.get().size(), result.size());
	}

	@Test
	public void testGetStringPathString() {
		assertNotNull(this.path.getStringPath("string"));
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPath("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPath("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPath("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPath("notExisting");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableJsonPrimitivePath("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableJsonPrimitivePath("object");
		});
	}

	@Test
	public void testGetStringPathStringStringParserOfT() {
		assertNotNull(this.path.getStringPath("string", new StringPathParser.StringParserString()));
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPath("number", new StringPathParser.StringParserString());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPath("boolean", new StringPathParser.StringParserString());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPath("null", new StringPathParser.StringParserString());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPath("notExisting", new StringPathParser.StringParserString());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPath("array", new StringPathParser.StringParserString());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPath("object", new StringPathParser.StringParserString());
		});
	}

	@Test
	public void testGetStringParsed() {
		assertEquals("string", this.path.getStringParsed("string", new StringPathParser.StringParserString()));
		assertEquals(Level.WARNING,
				this.path.getStringParsed("enum", new StringPathParser.StringParserEnum<>(Level.class)));
		assertEquals(this.localDate,
				this.path.getStringParsed("localDate", new StringPathParser.StringParserLocalDate()));
		assertEquals(this.zonedDateTime,
				this.path.getStringParsed("zonedDateTime", new StringPathParser.StringParserZonedDateTime()));
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringParsed("number", new StringPathParser.StringParserString());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringParsed("boolean", new StringPathParser.StringParserString());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringParsed("null", new StringPathParser.StringParserString());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringParsed("notExisting", new StringPathParser.StringParserString());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringParsed("array", new StringPathParser.StringParserString());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringParsed("object", new StringPathParser.StringParserString());
		});
	}

	@Test
	public void testGetStringParsedOrNull() {
		assertEquals("string", this.path.getStringParsedOrNull("string", new StringPathParser.StringParserString()));
		assertEquals(Level.WARNING,
				this.path.getStringParsedOrNull("enum", new StringPathParser.StringParserEnum<>(Level.class)));
		assertEquals(this.localDate,
				this.path.getStringParsedOrNull("localDate", new StringPathParser.StringParserLocalDate()));
		assertEquals(this.zonedDateTime,
				this.path.getStringParsedOrNull("zonedDateTime", new StringPathParser.StringParserZonedDateTime()));

		assertNull(this.path.getStringParsedOrNull("null", new StringPathParser.StringParserString()));
		assertNull(this.path.getStringParsedOrNull("notExisting", new StringPathParser.StringParserString()));
		assertNull(this.path.getStringParsedOrNull("null", new StringPathParser.StringParserEnum<>(Level.class)));
		assertNull(
				this.path.getStringParsedOrNull("notExisting", new StringPathParser.StringParserEnum<>(Level.class)));
		assertNull(this.path.getStringParsedOrNull("null", new StringPathParser.StringParserLocalDate()));
		assertNull(this.path.getStringParsedOrNull("notExisting", new StringPathParser.StringParserLocalDate()));
		assertNull(this.path.getStringParsedOrNull("null", new StringPathParser.StringParserZonedDateTime()));
		assertNull(this.path.getStringParsedOrNull("notExisting", new StringPathParser.StringParserZonedDateTime()));

		assertThrows(RuntimeException.class, () -> {
			this.path.getStringParsedOrNull("number", new StringPathParser.StringParserString());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringParsedOrNull("boolean", new StringPathParser.StringParserString());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringParsedOrNull("array", new StringPathParser.StringParserString());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringParsedOrNull("object", new StringPathParser.StringParserString());
		});
	}

	@Test
	public void testGetOptionalStringParsed() {
		assertEquals(Optional.of("string"),
				this.path.getOptionalStringParsed("string", new StringPathParser.StringParserString()));
		assertEquals(Optional.of(Level.WARNING),
				this.path.getOptionalStringParsed("enum", new StringPathParser.StringParserEnum<>(Level.class)));
		assertEquals(Optional.of(LocalDate.of(2025, 1, 1)),
				this.path.getOptionalStringParsed("localDate", new StringPathParser.StringParserLocalDate()));
		assertEquals(Optional.of(ZonedDateTime.of(LocalDateTime.of(2025, 1, 1, 1, 1), ZoneId.of("UTC"))),
				this.path.getOptionalStringParsed("zonedDateTime", new StringPathParser.StringParserZonedDateTime()));

		assertEquals(Optional.empty(),
				this.path.getOptionalStringParsed("null", new StringPathParser.StringParserString()));
		assertEquals(Optional.empty(),
				this.path.getOptionalStringParsed("notExisting", new StringPathParser.StringParserString()));
		assertEquals(Optional.empty(),
				this.path.getOptionalStringParsed("null", new StringPathParser.StringParserEnum<>(Level.class)));
		assertEquals(Optional.empty(),
				this.path.getOptionalStringParsed("notExisting", new StringPathParser.StringParserEnum<>(Level.class)));
		assertEquals(Optional.empty(),
				this.path.getOptionalStringParsed("null", new StringPathParser.StringParserLocalDate()));
		assertEquals(Optional.empty(),
				this.path.getOptionalStringParsed("notExisting", new StringPathParser.StringParserLocalDate()));
		assertEquals(Optional.empty(),
				this.path.getOptionalStringParsed("null", new StringPathParser.StringParserZonedDateTime()));
		assertEquals(Optional.empty(),
				this.path.getOptionalStringParsed("notExisting", new StringPathParser.StringParserZonedDateTime()));

		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalStringParsed("number", new StringPathParser.StringParserString());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalStringParsed("boolean", new StringPathParser.StringParserString());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalStringParsed("array", new StringPathParser.StringParserString());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalStringParsed("object", new StringPathParser.StringParserString());
		});
	}

	@Test
	public void testGetNullableStringPathString() {
		assertNotNull(this.path.getNullableStringPathString("string"));
		assertNotNull(this.path.getNullableStringPathString("null"));
		assertNotNull(this.path.getNullableStringPathString("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathString("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathString("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathString("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathString("object");
		});
	}

	@Test
	public void testGetNullableStringPathChannelAddress() {
		assertNotNull(this.path.getNullableStringPathChannelAddress("string"));
		assertNotNull(this.path.getNullableStringPathChannelAddress("channelAddress"));
		assertNotNull(this.path.getNullableStringPathChannelAddress("null"));
		assertNotNull(this.path.getNullableStringPathChannelAddress("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathChannelAddress("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathChannelAddress("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathChannelAddress("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathChannelAddress("object");
		});
	}

	@Test
	public void testGetNullableStringPathEnum() {
		assertNotNull(this.path.getNullableStringPathEnum("string", Level.class));
		assertNotNull(this.path.getNullableStringPathEnum("enum", Level.class));
		assertNotNull(this.path.getNullableStringPathEnum("null", Level.class));
		assertNotNull(this.path.getNullableStringPathEnum("notExisting", Level.class));

		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathEnum("number", Level.class);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathEnum("boolean", Level.class);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathEnum("array", Level.class);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathEnum("object", Level.class);
		});
	}

	@Test
	public void testGetNullableStringPathLocalDateString() {
		assertNotNull(this.path.getNullableStringPathLocalDate("string"));
		assertNotNull(this.path.getNullableStringPathLocalDate("localDate"));
		assertNotNull(this.path.getNullableStringPathLocalDate("null"));
		assertNotNull(this.path.getNullableStringPathLocalDate("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathLocalDate("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathLocalDate("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathLocalDate("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathLocalDate("object");
		});
	}

	@Test
	public void testGetNullableStringPathLocalDateStringDateTimeFormatter() {
		assertNotNull(this.path.getNullableStringPathLocalDate("string", DateTimeFormatter.ISO_LOCAL_DATE));
		assertNotNull(this.path.getNullableStringPathLocalDate("localDate", DateTimeFormatter.ISO_LOCAL_DATE));
		assertNotNull(this.path.getNullableStringPathLocalDate("null", DateTimeFormatter.ISO_LOCAL_DATE));
		assertNotNull(this.path.getNullableStringPathLocalDate("notExisting", DateTimeFormatter.ISO_LOCAL_DATE));

		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathLocalDate("number", DateTimeFormatter.ISO_LOCAL_DATE);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathLocalDate("boolean", DateTimeFormatter.ISO_LOCAL_DATE);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathLocalDate("array", DateTimeFormatter.ISO_LOCAL_DATE);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathLocalDate("object", DateTimeFormatter.ISO_LOCAL_DATE);
		});
	}

	@Test
	public void testGetNullableStringPathSemanticVersion() {
		assertNotNull(this.path.getNullableStringPathSemanticVersion("string"));
		assertNotNull(this.path.getNullableStringPathSemanticVersion("localDate"));
		assertNotNull(this.path.getNullableStringPathSemanticVersion("null"));
		assertNotNull(this.path.getNullableStringPathSemanticVersion("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathSemanticVersion("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathSemanticVersion("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathSemanticVersion("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathSemanticVersion("object");
		});
	}

	@Test
	public void testGetNullableStringPath() {
		assertNotNull(this.path.getNullableStringPath("string", new StringParserString()));
		assertNotNull(this.path.getNullableStringPath("null", new StringParserString()));
		assertNotNull(this.path.getNullableStringPath("notExisting", new StringParserString()));

		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathSemanticVersion("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathSemanticVersion("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathSemanticVersion("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathSemanticVersion("object");
		});
	}

	@Test
	public void testGetNullableStringPathUuid() {
		assertNotNull(this.path.getNullableStringPathUuid("string"));
		assertNotNull(this.path.getNullableStringPathUuid("uuid"));
		assertNotNull(this.path.getNullableStringPathUuid("null"));
		assertNotNull(this.path.getNullableStringPathUuid("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathUuid("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathUuid("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathUuid("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathUuid("object");
		});
	}

	@Test
	public void testGetNullableStringPathZonedDateTimeString() {
		assertNotNull(this.path.getNullableStringPathZonedDateTime("string"));
		assertNotNull(this.path.getNullableStringPathZonedDateTime("zonedDateTime"));
		assertNotNull(this.path.getNullableStringPathZonedDateTime("null"));
		assertNotNull(this.path.getNullableStringPathZonedDateTime("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathZonedDateTime("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathZonedDateTime("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathZonedDateTime("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathZonedDateTime("object");
		});
	}

	@Test
	public void testGetNullableStringPathZonedDateTimeStringDateTimeFormatter() {
		assertNotNull(this.path.getNullableStringPathZonedDateTime("string", DateTimeFormatter.ISO_ZONED_DATE_TIME));
		assertNotNull(
				this.path.getNullableStringPathZonedDateTime("zonedDateTime", DateTimeFormatter.ISO_ZONED_DATE_TIME));
		assertNotNull(this.path.getNullableStringPathZonedDateTime("null", DateTimeFormatter.ISO_ZONED_DATE_TIME));
		assertNotNull(
				this.path.getNullableStringPathZonedDateTime("notExisting", DateTimeFormatter.ISO_ZONED_DATE_TIME));

		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathZonedDateTime("number", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathZonedDateTime("boolean", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathZonedDateTime("array", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableStringPathZonedDateTime("object", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
	}

	@Test
	public void testGetStringPathUuid() {
		assertNotNull(this.path.getStringPathUuid("string"));
		assertNotNull(this.path.getStringPathUuid("uuid"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathUuid("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathUuid("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathUuid("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathUuid("object");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathUuid("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathUuid("notExisting");
		});
	}

	@Test
	public void testGetStringPathSemanticVersion() {
		assertNotNull(this.path.getStringPathSemanticVersion("string"));
		assertNotNull(this.path.getStringPathSemanticVersion("semanticVersion"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathSemanticVersion("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathSemanticVersion("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathSemanticVersion("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathSemanticVersion("object");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathSemanticVersion("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathSemanticVersion("notExisting");
		});
	}

	@Test
	public void testGetStringPathEnum() {
		assertNotNull(this.path.getStringPathEnum("string", Level.class));
		assertNotNull(this.path.getStringPathEnum("semanticVersion", Level.class));

		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathEnum("number", Level.class);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathEnum("boolean", Level.class);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathEnum("array", Level.class);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathEnum("object", Level.class);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathEnum("null", Level.class);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathEnum("notExisting", Level.class);
		});
	}

	@Test
	public void testGetStringPathZonedDateTimeStringDateTimeFormatter() {
		assertNotNull(this.path.getStringPathZonedDateTime("string", DateTimeFormatter.ISO_ZONED_DATE_TIME));
		assertNotNull(this.path.getStringPathZonedDateTime("zonedDateTime", DateTimeFormatter.ISO_ZONED_DATE_TIME));

		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathZonedDateTime("number", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathZonedDateTime("boolean", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathZonedDateTime("array", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathZonedDateTime("object", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathZonedDateTime("null", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathZonedDateTime("notExisting", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
	}

	@Test
	public void testGetStringPathZonedDateTimeString() {
		assertNotNull(this.path.getStringPathZonedDateTime("string"));
		assertNotNull(this.path.getStringPathZonedDateTime("zonedDateTime"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathZonedDateTime("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathZonedDateTime("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathZonedDateTime("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathZonedDateTime("object");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathZonedDateTime("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathZonedDateTime("notExisting");
		});
	}

	@Test
	public void testGetStringPathLocalDateStringDateTimeFormatter() {
		assertNotNull(this.path.getStringPathLocalDate("string", DateTimeFormatter.ISO_LOCAL_DATE));
		assertNotNull(this.path.getStringPathLocalDate("localDate", DateTimeFormatter.ISO_LOCAL_DATE));

		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathLocalDate("number", DateTimeFormatter.ISO_LOCAL_DATE);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathLocalDate("boolean", DateTimeFormatter.ISO_LOCAL_DATE);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathLocalDate("array", DateTimeFormatter.ISO_LOCAL_DATE);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathLocalDate("object", DateTimeFormatter.ISO_LOCAL_DATE);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathLocalDate("null", DateTimeFormatter.ISO_LOCAL_DATE);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathLocalDate("notExisting", DateTimeFormatter.ISO_LOCAL_DATE);
		});
	}

	@Test
	public void testGetStringPathLocalDateString() {
		assertNotNull(this.path.getStringPathLocalDate("string"));
		assertNotNull(this.path.getStringPathLocalDate("localDate"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathLocalDate("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathLocalDate("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathLocalDate("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathLocalDate("object");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathLocalDate("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringPathLocalDate("notExisting");
		});
	}

	@Test
	public void testGetNumberPath() {
		assertNotNull(this.path.getNumberPath("number"));
		assertNotNull(this.path.getNumberPath("long"));
		assertNotNull(this.path.getNumberPath("int"));
		assertNotNull(this.path.getNumberPath("double"));
		assertNotNull(this.path.getNumberPath("float"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getNumberPath("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNumberPath("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNumberPath("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNumberPath("object");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNumberPath("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNumberPath("notExisting");
		});
	}

	@Test
	public void testGetNullableNumberPath() {
		assertNotNull(this.path.getNullableNumberPath("number"));
		assertNotNull(this.path.getNullableNumberPath("long"));
		assertNotNull(this.path.getNullableNumberPath("int"));
		assertNotNull(this.path.getNullableNumberPath("double"));
		assertNotNull(this.path.getNullableNumberPath("float"));
		assertNotNull(this.path.getNullableNumberPath("null"));
		assertNotNull(this.path.getNullableNumberPath("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableNumberPath("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableNumberPath("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableNumberPath("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getNullableNumberPath("object");
		});
	}

	@Test
	public void testGetString() {
		assertEquals("string", this.path.getString("string"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getString("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getString("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getString("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getString("object");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getString("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getString("notExisting");
		});
	}

	@Test
	public void testGetStringOrNull() {
		assertEquals("string", this.path.getStringOrNull("string"));
		assertNull(this.path.getStringOrNull("null"));
		assertNull(this.path.getStringOrNull("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getStringOrNull("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringOrNull("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringOrNull("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getStringOrNull("object");
		});
	}

	@Test
	public void testGetOptionalString() {
		assertEquals(Optional.of("string"), this.path.getOptionalString("string"));
		assertEquals(Optional.empty(), this.path.getOptionalString("null"));
		assertEquals(Optional.empty(), this.path.getOptionalString("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalString("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalString("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalString("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalString("object");
		});
	}

	@Test
	public void testGetUuid() {
		assertEquals(this.uuid, this.path.getUuid("uuid"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getUuid("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getUuid("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getUuid("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getUuid("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getUuid("object");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getUuid("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getUuid("notExisting");
		});
	}

	@Test
	public void testGetUuidOrNull() {
		assertEquals(this.uuid, this.path.getUuidOrNull("uuid"));
		assertNull(this.path.getUuidOrNull("null"));
		assertNull(this.path.getUuidOrNull("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getUuidOrNull("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getUuidOrNull("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getUuidOrNull("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getUuidOrNull("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getUuidOrNull("object");
		});
	}

	@Test
	public void testGetOptionalUuid() {
		assertEquals(Optional.of(this.uuid), this.path.getOptionalUuid("uuid"));
		assertEquals(Optional.empty(), this.path.getOptionalUuid("null"));
		assertEquals(Optional.empty(), this.path.getOptionalUuid("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalUuid("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalUuid("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalUuid("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalUuid("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalUuid("object");
		});
	}

	@Test
	public void testGetSemanticVersion() {
		assertEquals(this.semanticVersion, this.path.getSemanticVersion("semanticVersion"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getSemanticVersion("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getSemanticVersion("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getSemanticVersion("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getSemanticVersion("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getSemanticVersion("object");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getSemanticVersion("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getSemanticVersion("notExisting");
		});
	}

	@Test
	public void testGetSemanticVersionOrNull() {
		assertEquals(this.semanticVersion, this.path.getSemanticVersionOrNull("semanticVersion"));
		assertNull(this.path.getSemanticVersionOrNull("null"));
		assertNull(this.path.getSemanticVersionOrNull("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getSemanticVersionOrNull("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getSemanticVersionOrNull("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getSemanticVersionOrNull("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getSemanticVersionOrNull("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getSemanticVersionOrNull("object");
		});
	}

	@Test
	public void testGetOptionalSemanticVersion() {
		assertEquals(Optional.of(this.semanticVersion), this.path.getOptionalSemanticVersion("semanticVersion"));
		assertEquals(Optional.empty(), this.path.getOptionalSemanticVersion("null"));
		assertEquals(Optional.empty(), this.path.getOptionalSemanticVersion("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalSemanticVersion("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalSemanticVersion("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalSemanticVersion("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalSemanticVersion("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalSemanticVersion("object");
		});
	}

	@Test
	public void testGetEnum() {
		assertEquals(Level.WARNING, this.path.getEnum("enum", Level.class));

		assertThrows(RuntimeException.class, () -> {
			this.path.getEnum("string", Level.class);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getEnum("number", Level.class);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getEnum("boolean", Level.class);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getEnum("array", Level.class);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getEnum("object", Level.class);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getEnum("null", Level.class);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getEnum("notExisting", Level.class);
		});
	}

	@Test
	public void testGetEnumOrNull() {
		assertEquals(Level.WARNING, this.path.getEnumOrNull("enum", Level.class));
		assertNull(this.path.getEnumOrNull("null", Level.class));
		assertNull(this.path.getEnumOrNull("notExisting", Level.class));

		assertThrows(RuntimeException.class, () -> {
			this.path.getEnumOrNull("string", Level.class);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getEnumOrNull("number", Level.class);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getEnumOrNull("boolean", Level.class);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getEnumOrNull("array", Level.class);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getEnumOrNull("object", Level.class);
		});
	}

	@Test
	public void testGetOptionalEnum() {
		assertEquals(Optional.of(Level.WARNING), this.path.getOptionalEnum("enum", Level.class));
		assertEquals(Optional.empty(), this.path.getOptionalEnum("null", Level.class));
		assertEquals(Optional.empty(), this.path.getOptionalEnum("notExisting", Level.class));

		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalEnum("string", Level.class);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalEnum("number", Level.class);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalEnum("boolean", Level.class);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalEnum("array", Level.class);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalEnum("object", Level.class);
		});
	}

	@Test
	public void testGetZonedDateTimeString() {
		assertEquals(this.zonedDateTime, this.path.getZonedDateTime("zonedDateTime"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTime("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTime("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTime("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTime("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTime("object");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTime("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTime("notExisting");
		});
	}

	@Test
	public void testGetZonedDateTimeStringDateTimeFormatter() {
		assertEquals(this.zonedDateTime,
				this.path.getZonedDateTime("zonedDateTime", DateTimeFormatter.ISO_ZONED_DATE_TIME));

		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTime("string", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTime("number", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTime("boolean", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTime("array", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTime("object", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTime("null", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTime("notExisting", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
	}

	@Test
	public void testGetZonedDateTimeOrNullString() {
		assertEquals(this.zonedDateTime, this.path.getZonedDateTimeOrNull("zonedDateTime"));
		assertNull(this.path.getZonedDateTimeOrNull("null"));
		assertNull(this.path.getZonedDateTimeOrNull("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTimeOrNull("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTimeOrNull("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTimeOrNull("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTimeOrNull("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTimeOrNull("object");
		});
	}

	@Test
	public void testGetZonedDateTimeOrNullStringDateTimeFormatter() {
		assertEquals(this.zonedDateTime,
				this.path.getZonedDateTimeOrNull("zonedDateTime", DateTimeFormatter.ISO_ZONED_DATE_TIME));
		assertNull(this.path.getZonedDateTimeOrNull("null", DateTimeFormatter.ISO_ZONED_DATE_TIME));
		assertNull(this.path.getZonedDateTimeOrNull("notExisting", DateTimeFormatter.ISO_ZONED_DATE_TIME));

		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTimeOrNull("string", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTimeOrNull("number", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTimeOrNull("boolean", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTimeOrNull("array", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getZonedDateTimeOrNull("object", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
	}

	@Test
	public void testGetOptionalZonedDateTimeString() {
		assertEquals(Optional.of(this.zonedDateTime), this.path.getOptionalZonedDateTime("zonedDateTime"));
		assertEquals(Optional.empty(), this.path.getOptionalZonedDateTime("null"));
		assertEquals(Optional.empty(), this.path.getOptionalZonedDateTime("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalZonedDateTime("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalZonedDateTime("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalZonedDateTime("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalZonedDateTime("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalZonedDateTime("object");
		});
	}

	@Test
	public void testGetOptionalZonedDateTimeStringDateTimeFormatter() {
		assertEquals(Optional.of(this.zonedDateTime),
				this.path.getOptionalZonedDateTime("zonedDateTime", DateTimeFormatter.ISO_ZONED_DATE_TIME));
		assertEquals(Optional.empty(),
				this.path.getOptionalZonedDateTime("null", DateTimeFormatter.ISO_ZONED_DATE_TIME));
		assertEquals(Optional.empty(),
				this.path.getOptionalZonedDateTime("notExisting", DateTimeFormatter.ISO_ZONED_DATE_TIME));

		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalZonedDateTime("string", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalZonedDateTime("number", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalZonedDateTime("boolean", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalZonedDateTime("array", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalZonedDateTime("object", DateTimeFormatter.ISO_ZONED_DATE_TIME);
		});
	}

	@Test
	public void testGetLocalDateString() {
		assertEquals(this.localDate, this.path.getLocalDate("localDate"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDate("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDate("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDate("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDate("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDate("object");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDate("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDate("notExisting");
		});
	}

	@Test
	public void testGetLocalDateStringDateTimeFormatter() {
		assertEquals(this.localDate, this.path.getLocalDate("localDate", DateTimeFormatter.ISO_LOCAL_DATE));

		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDate("string", DateTimeFormatter.ISO_LOCAL_DATE);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDate("number", DateTimeFormatter.ISO_LOCAL_DATE);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDate("boolean", DateTimeFormatter.ISO_LOCAL_DATE);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDate("array", DateTimeFormatter.ISO_LOCAL_DATE);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDate("object", DateTimeFormatter.ISO_LOCAL_DATE);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDate("null", DateTimeFormatter.ISO_LOCAL_DATE);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDate("notExisting", DateTimeFormatter.ISO_LOCAL_DATE);
		});
	}

	@Test
	public void testGetLocalDateOrNullString() {
		assertEquals(this.localDate, this.path.getLocalDateOrNull("localDate"));
		assertNull(this.path.getLocalDateOrNull("null"));
		assertNull(this.path.getLocalDateOrNull("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDateOrNull("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDateOrNull("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDateOrNull("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDateOrNull("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDateOrNull("object");
		});
	}

	@Test
	public void testGetLocalDateOrNullStringDateTimeFormatter() {
		assertEquals(this.localDate, this.path.getLocalDateOrNull("localDate", DateTimeFormatter.ISO_LOCAL_DATE));
		assertNull(this.path.getLocalDateOrNull("null", DateTimeFormatter.ISO_LOCAL_DATE));
		assertNull(this.path.getLocalDateOrNull("notExisting", DateTimeFormatter.ISO_LOCAL_DATE));

		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDateOrNull("string", DateTimeFormatter.ISO_LOCAL_DATE);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDateOrNull("number", DateTimeFormatter.ISO_LOCAL_DATE);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDateOrNull("boolean", DateTimeFormatter.ISO_LOCAL_DATE);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDateOrNull("array", DateTimeFormatter.ISO_LOCAL_DATE);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLocalDateOrNull("object", DateTimeFormatter.ISO_LOCAL_DATE);
		});
	}

	@Test
	public void testGetOptionalLocalDateString() {
		assertEquals(Optional.of(this.localDate), this.path.getOptionalLocalDate("localDate"));
		assertEquals(Optional.empty(), this.path.getOptionalLocalDate("null"));
		assertEquals(Optional.empty(), this.path.getOptionalLocalDate("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalLocalDate("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalLocalDate("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalLocalDate("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalLocalDate("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalLocalDate("object");
		});
	}

	@Test
	public void testGetOptionalLocalDateStringDateTimeFormatter() {
		assertEquals(Optional.of(this.localDate),
				this.path.getOptionalLocalDate("localDate", DateTimeFormatter.ISO_LOCAL_DATE));
		assertEquals(Optional.empty(), this.path.getOptionalLocalDate("null", DateTimeFormatter.ISO_LOCAL_DATE));
		assertEquals(Optional.empty(), this.path.getOptionalLocalDate("notExisting", DateTimeFormatter.ISO_LOCAL_DATE));

		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalLocalDate("string", DateTimeFormatter.ISO_LOCAL_DATE);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalLocalDate("number", DateTimeFormatter.ISO_LOCAL_DATE);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalLocalDate("boolean", DateTimeFormatter.ISO_LOCAL_DATE);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalLocalDate("array", DateTimeFormatter.ISO_LOCAL_DATE);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalLocalDate("object", DateTimeFormatter.ISO_LOCAL_DATE);
		});
	}

	@Test
	public void testGetDouble() {
		assertEquals(10.5D, this.path.getDouble("double"), 0);
		assertEquals(10.5D, this.path.getDouble("float"), 0);
		assertEquals(10D, this.path.getDouble("long"), 0);
		assertEquals(10D, this.path.getDouble("int"), 0);

		assertThrows(RuntimeException.class, () -> {
			this.path.getDouble("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getDouble("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getDouble("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getDouble("object");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getDouble("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getDouble("notExisting");
		});
	}

	@Test
	public void testGetDoubleOrDefault() {
		assertEquals(10.5D, this.path.getDoubleOrDefault("double", 0), 0);
		assertEquals(10.5D, this.path.getDoubleOrDefault("float", 0), 0);
		assertEquals(10D, this.path.getDoubleOrDefault("long", 0), 0);
		assertEquals(10D, this.path.getDoubleOrDefault("int", 0), 0);
		assertEquals(0, this.path.getDoubleOrDefault("null", 0), 0);
		assertEquals(0, this.path.getDoubleOrDefault("notExisting", 0), 0);

		assertThrows(RuntimeException.class, () -> {
			this.path.getDoubleOrDefault("string", 0);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getDoubleOrDefault("boolean", 0);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getDoubleOrDefault("array", 0);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getDoubleOrDefault("object", 0);
		});
	}

	@Test
	public void testGetOptionalDouble() {
		assertEquals(Optional.of(10.5D), this.path.getOptionalDouble("double"));
		assertEquals(Optional.of(10.5D), this.path.getOptionalDouble("float"));
		assertEquals(Optional.of(10D), this.path.getOptionalDouble("long"));
		assertEquals(Optional.of(10D), this.path.getOptionalDouble("int"));
		assertEquals(Optional.empty(), this.path.getOptionalDouble("null"));
		assertEquals(Optional.empty(), this.path.getOptionalDouble("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalDouble("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalDouble("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalDouble("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalDouble("object");
		});
	}

	@Test
	public void testGetFloat() {
		assertEquals(10.5F, this.path.getFloat("double"), 0);
		assertEquals(10.5F, this.path.getFloat("float"), 0);
		assertEquals(10F, this.path.getFloat("long"), 0);
		assertEquals(10F, this.path.getFloat("int"), 0);

		assertThrows(RuntimeException.class, () -> {
			this.path.getFloat("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getFloat("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getFloat("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getFloat("object");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getFloat("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getFloat("notExisting");
		});
	}

	@Test
	public void testGetFloatOrDefault() {
		assertEquals(10.5F, this.path.getFloatOrDefault("double", 0), 0);
		assertEquals(10.5F, this.path.getFloatOrDefault("float", 0), 0);
		assertEquals(10F, this.path.getFloatOrDefault("long", 0), 0);
		assertEquals(10F, this.path.getFloatOrDefault("int", 0), 0);
		assertEquals(0F, this.path.getFloatOrDefault("null", 0), 0);
		assertEquals(0F, this.path.getFloatOrDefault("notExisting", 0), 0);

		assertThrows(RuntimeException.class, () -> {
			this.path.getFloatOrDefault("string", 0);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getFloatOrDefault("boolean", 0);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getFloatOrDefault("array", 0);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getFloatOrDefault("object", 0);
		});
	}

	@Test
	public void testGetOptionalFloat() {
		assertEquals(Optional.of(10.5F), this.path.getOptionalFloat("double"));
		assertEquals(Optional.of(10.5F), this.path.getOptionalFloat("float"));
		assertEquals(Optional.of(10F), this.path.getOptionalFloat("long"));
		assertEquals(Optional.of(10F), this.path.getOptionalFloat("int"));
		assertEquals(Optional.empty(), this.path.getOptionalFloat("null"));
		assertEquals(Optional.empty(), this.path.getOptionalFloat("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalFloat("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalFloat("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalFloat("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalFloat("object");
		});
	}

	@Test
	public void testGetLong() {
		assertEquals(10L, this.path.getLong("double"));
		assertEquals(10L, this.path.getLong("float"));
		assertEquals(10L, this.path.getLong("long"));
		assertEquals(10L, this.path.getLong("int"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getLong("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLong("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLong("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLong("object");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLong("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLong("notExisting");
		});
	}

	@Test
	public void testGetLongOrDefault() {
		assertEquals(10L, this.path.getLongOrDefault("double", 0));
		assertEquals(10L, this.path.getLongOrDefault("float", 0));
		assertEquals(10L, this.path.getLongOrDefault("long", 0));
		assertEquals(10L, this.path.getLongOrDefault("int", 0));
		assertEquals(0L, this.path.getLongOrDefault("null", 0));
		assertEquals(0L, this.path.getLongOrDefault("notExisting", 0));

		assertThrows(RuntimeException.class, () -> {
			this.path.getLongOrDefault("string", 0);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLongOrDefault("boolean", 0);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLongOrDefault("array", 0);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getLongOrDefault("object", 0);
		});
	}

	@Test
	public void testGetOptionalLong() {
		assertEquals(Optional.of(10L), this.path.getOptionalLong("double"));
		assertEquals(Optional.of(10L), this.path.getOptionalLong("float"));
		assertEquals(Optional.of(10L), this.path.getOptionalLong("long"));
		assertEquals(Optional.of(10L), this.path.getOptionalLong("int"));
		assertEquals(Optional.empty(), this.path.getOptionalLong("null"));
		assertEquals(Optional.empty(), this.path.getOptionalLong("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalLong("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalLong("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalLong("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalLong("object");
		});
	}

	@Test
	public void testGetInt() {
		assertEquals(10, this.path.getInt("double"));
		assertEquals(10, this.path.getInt("float"));
		assertEquals(10, this.path.getInt("long"));
		assertEquals(10, this.path.getInt("int"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getInt("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getInt("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getInt("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getInt("object");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getInt("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getInt("notExisting");
		});
	}

	@Test
	public void testGetIntOrDefault() {
		assertEquals(10, this.path.getIntOrDefault("double", 0));
		assertEquals(10, this.path.getIntOrDefault("float", 0));
		assertEquals(10, this.path.getIntOrDefault("long", 0));
		assertEquals(10, this.path.getIntOrDefault("int", 0));
		assertEquals(0, this.path.getIntOrDefault("null", 0));
		assertEquals(0, this.path.getIntOrDefault("notExisting", 0));

		assertThrows(RuntimeException.class, () -> {
			this.path.getIntOrDefault("string", 0);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getIntOrDefault("boolean", 0);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getIntOrDefault("array", 0);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getIntOrDefault("object", 0);
		});
	}

	@Test
	public void testGetOptionalInt() {
		assertEquals(Optional.of(10), this.path.getOptionalInt("double"));
		assertEquals(Optional.of(10), this.path.getOptionalInt("float"));
		assertEquals(Optional.of(10), this.path.getOptionalInt("long"));
		assertEquals(Optional.of(10), this.path.getOptionalInt("int"));
		assertEquals(Optional.empty(), this.path.getOptionalInt("null"));
		assertEquals(Optional.empty(), this.path.getOptionalInt("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalInt("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalInt("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalInt("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalInt("object");
		});
	}

	@Test
	public void testGetShort() {
		assertEquals(10, this.path.getShort("double"));
		assertEquals(10, this.path.getShort("float"));
		assertEquals(10, this.path.getShort("long"));
		assertEquals(10, this.path.getShort("int"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getShort("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getShort("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getShort("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getShort("object");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getShort("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getShort("notExisting");
		});
	}

	@Test
	public void testGetShortOrDefault() {
		assertEquals(10, this.path.getShortOrDefault("double", (short) 0));
		assertEquals(10, this.path.getShortOrDefault("float", (short) 0));
		assertEquals(10, this.path.getShortOrDefault("long", (short) 0));
		assertEquals(10, this.path.getShortOrDefault("int", (short) 0));
		assertEquals(0, this.path.getShortOrDefault("null", (short) 0));
		assertEquals(0, this.path.getShortOrDefault("notExisting", (short) 0));

		assertThrows(RuntimeException.class, () -> {
			this.path.getShortOrDefault("string", (short) 0);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getShortOrDefault("boolean", (short) 0);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getShortOrDefault("array", (short) 0);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getShortOrDefault("object", (short) 0);
		});
	}

	@Test
	public void testGetOptionalShort() {
		assertEquals(Optional.of((short) 10), this.path.getOptionalShort("double"));
		assertEquals(Optional.of((short) 10), this.path.getOptionalShort("float"));
		assertEquals(Optional.of((short) 10), this.path.getOptionalShort("long"));
		assertEquals(Optional.of((short) 10), this.path.getOptionalShort("int"));
		assertEquals(Optional.empty(), this.path.getOptionalShort("null"));
		assertEquals(Optional.empty(), this.path.getOptionalShort("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalShort("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalShort("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalShort("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalShort("object");
		});
	}

	@Test
	public void testGetByte() {
		assertEquals(10, this.path.getByte("double"));
		assertEquals(10, this.path.getByte("float"));
		assertEquals(10, this.path.getByte("long"));
		assertEquals(10, this.path.getByte("int"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getByte("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getByte("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getByte("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getByte("object");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getByte("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getByte("notExisting");
		});
	}

	@Test
	public void testGetByteOrDefault() {
		assertEquals(10, this.path.getByteOrDefault("double", (byte) 0));
		assertEquals(10, this.path.getByteOrDefault("float", (byte) 0));
		assertEquals(10, this.path.getByteOrDefault("long", (byte) 0));
		assertEquals(10, this.path.getByteOrDefault("int", (byte) 0));
		assertEquals(0, this.path.getByteOrDefault("null", (byte) 0));
		assertEquals(0, this.path.getByteOrDefault("notExisting", (byte) 0));

		assertThrows(RuntimeException.class, () -> {
			this.path.getByteOrDefault("string", (byte) 0);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getByteOrDefault("boolean", (byte) 0);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getByteOrDefault("array", (byte) 0);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getByteOrDefault("object", (byte) 0);
		});
	}

	@Test
	public void testGetOptionalByte() {
		assertEquals(Optional.of((byte) 10), this.path.getOptionalByte("double"));
		assertEquals(Optional.of((byte) 10), this.path.getOptionalByte("float"));
		assertEquals(Optional.of((byte) 10), this.path.getOptionalByte("long"));
		assertEquals(Optional.of((byte) 10), this.path.getOptionalByte("int"));
		assertEquals(Optional.empty(), this.path.getOptionalByte("null"));
		assertEquals(Optional.empty(), this.path.getOptionalByte("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalByte("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalByte("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalByte("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalByte("object");
		});
	}

	@Test
	public void testGetBooleanPath() {
		assertNotNull(this.path.getBooleanPath("boolean"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getBooleanPath("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getBooleanPath("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getBooleanPath("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getBooleanPath("object");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getBooleanPath("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getBooleanPath("notExisting");
		});
	}

	@Test
	public void testGetBoolean() {
		assertTrue(this.path.getBoolean("boolean"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getBoolean("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getBoolean("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getBoolean("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getBoolean("object");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getBoolean("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getBoolean("notExisting");
		});
	}

	@Test
	public void testGetBooleanPathNullable() {
		assertNotNull(this.path.getBooleanPathNullable("boolean"));
		assertNotNull(this.path.getBooleanPathNullable("null"));
		assertNotNull(this.path.getBooleanPathNullable("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getBooleanPathNullable("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getBooleanPathNullable("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getBooleanPathNullable("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getBooleanPathNullable("object");
		});
	}

	@Test
	public void testGetBooleanNullable() {
		assertTrue(this.path.getBooleanNullable("boolean"));
		assertNull(this.path.getBooleanNullable("null"));
		assertNull(this.path.getBooleanNullable("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getBoolean("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getBoolean("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getBoolean("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getBoolean("object");
		});
	}

	@Test
	public void testGetOptionalBoolean() {
		assertEquals(Optional.of(true), this.path.getOptionalBoolean("boolean"));
		assertEquals(Optional.empty(), this.path.getOptionalBoolean("null"));
		assertEquals(Optional.empty(), this.path.getOptionalBoolean("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalBoolean("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalBoolean("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalBoolean("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalBoolean("object");
		});
	}

	@Test
	public void testGetJsonObjectPath() {
		assertNotNull(this.path.getJsonObjectPath("object"));
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonObjectPath("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonObjectPath("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonObjectPath("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonObjectPath("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonObjectPath("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonObjectPath("notExisting");
		});
	}

	@Test
	public void testGetJsonObject() {
		assertNotNull(this.path.getJsonObject("object"));
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonObject("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonObject("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonObject("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonObject("array");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonObject("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonObject("notExisting");
		});
	}

	@Test
	public void testGetJsonArrayPath() {
		assertNotNull(this.path.getJsonArrayPath("array"));
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonArrayPath("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonArrayPath("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonArrayPath("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonArrayPath("object");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonArrayPath("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonArrayPath("notExisting");
		});
	}

	@Test
	public void testGetJsonArray() {
		assertNotNull(this.path.getJsonArray("array"));
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonArray("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonArray("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonArray("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonArray("object");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonArray("null");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonArray("notExisting");
		});
	}

	@Test
	public void testGetJsonArrayOrNull() {
		assertNotNull(this.path.getJsonArrayOrNull("array"));
		assertNull(this.path.getJsonArrayOrNull("null"));
		assertNull(this.path.getJsonArrayOrNull("notExisting"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonArray("string");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonArray("number");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonArray("boolean");
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getJsonArray("object");
		});
	}

	@Test
	public void testGetListStringFunctionOfJsonElementPathT() {
		final var list = this.path.getList("array", JsonElementPath::getAsString);
		assertEquals(1, list.size());
		assertEquals("string", list.get(0));

		assertThrows(RuntimeException.class, () -> {
			this.path.getList("string", JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getList("number", JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getList("boolean", JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getList("object", JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getList("null", JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getList("notExisting", JsonElementPath::getAsString);
		});
	}

	@Test
	public void testGetListStringJsonSerializerOfT() {
		final var list = this.path.getList("array", stringSerializer());
		assertEquals(1, list.size());
		assertEquals("string", list.get(0));

		assertThrows(RuntimeException.class, () -> {
			this.path.getList("string", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getList("number", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getList("boolean", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getList("object", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getList("null", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getList("notExisting", stringSerializer());
		});
	}

	@Test
	public void testGetOptionalListStringFunctionOfJsonElementPathT() {
		final var list = this.path.getOptionalList("array", JsonElementPath::getAsString).orElse(null);
		assertEquals(1, list.size());
		assertEquals("string", list.get(0));

		assertEquals(Optional.empty(), this.path.getOptionalList("null", JsonElementPath::getAsString));
		assertEquals(Optional.empty(), this.path.getOptionalList("notExisting", JsonElementPath::getAsString));

		assertThrows(RuntimeException.class, () -> {
			this.path.getList("string", JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getList("number", JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getList("boolean", JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getList("object", JsonElementPath::getAsString);
		});
	}

	@Test
	public void testGetOptionalListStringJsonSerializerOfT() {
		final var list = this.path.getOptionalList("array", stringSerializer()).orElse(null);
		assertEquals(1, list.size());
		assertEquals("string", list.get(0));

		assertEquals(Optional.empty(), this.path.getOptionalList("null", stringSerializer()));
		assertEquals(Optional.empty(), this.path.getOptionalList("notExisting", stringSerializer()));

		assertThrows(RuntimeException.class, () -> {
			this.path.getList("string", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getList("number", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getList("boolean", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getList("object", stringSerializer());
		});
	}

	@Test
	public void testGetArrayStringIntFunctionOfTFunctionOfJsonElementPathT() {
		final var array = this.path.getArray("array", String[]::new, JsonElementPath::getAsString);
		assertEquals(1, array.length);
		assertEquals("string", array[0]);

		assertThrows(RuntimeException.class, () -> {
			this.path.getArray("string", String[]::new, JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getArray("number", String[]::new, JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getArray("boolean", String[]::new, JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getArray("object", String[]::new, JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getArray("null", String[]::new, JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getArray("notExisting", String[]::new, JsonElementPath::getAsString);
		});
	}

	@Test
	public void testGetArrayStringIntFunctionOfTJsonSerializerOfT() {
		final var array = this.path.getArray("array", String[]::new, stringSerializer());
		assertEquals(1, array.length);
		assertEquals("string", array[0]);

		assertThrows(RuntimeException.class, () -> {
			this.path.getArray("string", String[]::new, stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getArray("number", String[]::new, stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getArray("boolean", String[]::new, stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getArray("object", String[]::new, stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getArray("null", String[]::new, stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getArray("notExisting", String[]::new, stringSerializer());
		});
	}

	@Test
	public void testGetOptionalArrayStringFunctionOfJsonElementPathT() {
		final var list = this.path.getOptionalArray("array", String[]::new, JsonElementPath::getAsString).orElse(null);
		assertEquals(1, list.length);
		assertEquals("string", list[0]);

		assertEquals(Optional.empty(), this.path.getOptionalArray("null", String[]::new, JsonElementPath::getAsString));
		assertEquals(Optional.empty(),
				this.path.getOptionalArray("notExisting", String[]::new, JsonElementPath::getAsString));

		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalArray("string", String[]::new, JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalArray("number", String[]::new, JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalArray("boolean", String[]::new, JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalArray("object", String[]::new, JsonElementPath::getAsString);
		});
	}

	@Test
	public void testGetOptionalArrayStringJsonSerializerOfT() {
		final var list = this.path.getOptionalArray("array", String[]::new, stringSerializer()).orElse(null);
		assertEquals(1, list.length);
		assertEquals("string", list[0]);

		assertEquals(Optional.empty(), this.path.getOptionalArray("null", String[]::new, stringSerializer()));
		assertEquals(Optional.empty(), this.path.getOptionalArray("notExisting", String[]::new, stringSerializer()));

		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalArray("string", String[]::new, stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalArray("number", String[]::new, stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalArray("boolean", String[]::new, stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalArray("object", String[]::new, stringSerializer());
		});
	}

	@Test
	public void testGetSetStringFunctionOfJsonElementPathT() {
		final var set = this.path.getSet("array", JsonElementPath::getAsString);
		assertEquals(1, set.size());
		assertTrue(set.contains("string"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getSet("string", JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getSet("number", JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getSet("boolean", JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getSet("object", JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getSet("null", JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getSet("notExisting", JsonElementPath::getAsString);
		});
	}

	@Test
	public void testGetSetStringJsonSerializerOfT() {
		final var set = this.path.getSet("array", stringSerializer());
		assertEquals(1, set.size());
		assertTrue(set.contains("string"));

		assertThrows(RuntimeException.class, () -> {
			this.path.getSet("string", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getSet("number", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getSet("boolean", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getSet("object", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getSet("null", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getSet("notExisting", stringSerializer());
		});
	}

	@Test
	public void testGetOptionalSetStringFunctionOfJsonElementPathT() {
		final var set = this.path.getOptionalSet("array", JsonElementPath::getAsString).orElse(null);
		assertEquals(1, set.size());
		assertTrue(set.contains("string"));

		assertEquals(Optional.empty(), this.path.getOptionalSet("null", JsonElementPath::getAsString));
		assertEquals(Optional.empty(), this.path.getOptionalSet("notExisting", JsonElementPath::getAsString));

		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalSet("string", JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalSet("number", JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalSet("boolean", JsonElementPath::getAsString);
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalSet("object", JsonElementPath::getAsString);
		});
	}

	@Test
	public void testGetOptionalSetStringJsonSerializerOfT() {
		final var set = this.path.getOptionalSet("array", stringSerializer()).orElse(null);
		assertEquals(1, set.size());
		assertTrue(set.contains("string"));

		assertEquals(Optional.empty(), this.path.getOptionalSet("null", stringSerializer()));
		assertEquals(Optional.empty(), this.path.getOptionalSet("notExisting", stringSerializer()));

		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalSet("string", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalSet("number", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalSet("boolean", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getOptionalSet("object", stringSerializer());
		});
	}

	@Test
	public void testGetObject() {
		assertEquals("string", this.path.getObject("string", stringSerializer()));

		assertThrows(RuntimeException.class, () -> {
			this.path.getObject("number", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getObject("boolean", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getObject("array", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getObject("object", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getObject("null", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getObject("notExisting", stringSerializer());
		});
	}

	@Test
	public void testGetObjectOrNull() {
		assertEquals("string", this.path.getObjectOrNull("string", stringSerializer()));
		assertNull(this.path.getObjectOrNull("null", stringSerializer()));
		assertNull(this.path.getObjectOrNull("notExisting", stringSerializer()));

		assertThrows(RuntimeException.class, () -> {
			this.path.getObjectOrNull("number", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getObjectOrNull("boolean", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getObjectOrNull("array", stringSerializer());
		});
		assertThrows(RuntimeException.class, () -> {
			this.path.getObjectOrNull("object", stringSerializer());
		});
	}

	@Test
	public void testGet() {
		assertNotNull(this.path.get());
	}

}
