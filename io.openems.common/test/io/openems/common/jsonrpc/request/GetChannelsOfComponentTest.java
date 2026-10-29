package io.openems.common.jsonrpc.request;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.ChannelCategory;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;

public class GetChannelsOfComponentTest {

	@Test
	public void testRequestSerializer() {
		final var result = GetChannelsOfComponent.Request.serializer().deserialize(JsonUtils.buildJsonObject() //
				.addProperty("componentId", "ess0") //
				.build());
		assertEquals(new GetChannelsOfComponent.Request("ess0", false), result);
	}

	@Test
	public void testResponseSerializer() {
		// Create a sample channel record with string options
		List<GetChannelsOfComponent.ChannelRecord.OptionsEnumEntry> options = Arrays.asList(//
				new GetChannelsOfComponent.ChannelRecord.OptionsEnumEntry("Ok", 0), //
				new GetChannelsOfComponent.ChannelRecord.OptionsEnumEntry("Info", 1), //
				new GetChannelsOfComponent.ChannelRecord.OptionsEnumEntry("Warning", 2), //
				new GetChannelsOfComponent.ChannelRecord.OptionsEnumEntry("Fault", 3)//
		);
		var plainChannelRecord = new GetChannelsOfComponent.ChannelRecord(//
				"State", //
				AccessMode.READ_ONLY, //
				PersistencePriority.VERY_HIGH, //
				"0:Ok, 1:Info, 2:Warning, 3:Fault", //
				OpenemsType.INTEGER, //
				Unit.NONE, //
				ChannelCategory.ENUM, //
				null, // level
				null, // stringOptions
				options //
		);
		List<String> stringOptions = Arrays.asList("NONE", "DEBUG_LOG", "TRACE");
		var attributedefinitionRecord = new GetChannelsOfComponent.ChannelRecord(//
				"_PropertyLogVerbosity", //
				AccessMode.READ_WRITE, //
				PersistencePriority.HIGH, //
				"", //
				OpenemsType.STRING, //
				Unit.NONE, //
				ChannelCategory.OPENEMS_TYPE, //
				null, // level
				stringOptions, //
				null // options
		);

		// Create a response with the channel record
		var originalResponse = new GetChannelsOfComponent.Response(
				Arrays.asList(plainChannelRecord, attributedefinitionRecord));

		// Serialize to JSON string
		var json = GetChannelsOfComponent.Response.serializer().serialize(originalResponse);
		String jsonString = json.toString();

		// Deserialize back from JSON string
		JsonElement stringJson = JsonParser.parseString(jsonString);
		var result = GetChannelsOfComponent.Response.serializer().deserialize(stringJson);

		// Verify the deserialized response matches the original
		assertEquals(originalResponse, result);
		assertEquals(2, result.channels().size());
		assertEquals("State", result.channels().get(0).id());
		assertEquals(stringOptions, result.channels().get(1).stringOptions());
	}

}