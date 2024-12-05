package io.openems.edge.predictor.lstmmodel.common;

import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class OffsetDateTimeAdapter implements JsonSerializer<OffsetDateTime>, JsonDeserializer<OffsetDateTime> {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	@Override
	public JsonElement serialize(OffsetDateTime src, Type typeOfSrc, JsonSerializationContext context) {
		return new JsonPrimitive(src.format(FORMATTER));
	}

	@Override
	public OffsetDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		return OffsetDateTime.parse(json.getAsString(), FORMATTER);
	}
}
