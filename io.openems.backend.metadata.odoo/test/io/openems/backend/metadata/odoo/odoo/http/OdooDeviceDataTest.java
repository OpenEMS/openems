package io.openems.backend.metadata.odoo.odoo.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;

class OdooDeviceDataTest {

	@Test
	void testSerializationCommentFalse() throws Exception {
		final var deviceData = OdooDeviceData.serializer().deserialize("""
				        {
				            "id": 1,
				            "name": "edge0",
				            "comment": false,
				            "producttype": "",
				            "role": "guest",
				            "openems_sum_state_level": "ok"
				        }
				""".stripIndent());

		assertEquals(new OdooDeviceData(//
				"edge0", //
				"", //
				"", //
				Role.GUEST, //
				null, //
				Level.OK, //
				null, //
				null //
		), deviceData);
	}

	@Test
	void testSerializationCommentString() throws Exception {
		final var deviceData = OdooDeviceData.serializer().deserialize("""
				        {
				            "id": 1,
				            "name": "edge0",
				            "comment": "edge0 - Very Great Edge",
				            "producttype": "",
				            "role": "guest",
				            "openems_sum_state_level": "ok"
				        }
				""".stripIndent());

		assertEquals(new OdooDeviceData(//
				"edge0", //
				"edge0 - Very Great Edge", //
				"", //
				Role.GUEST, //
				null, //
				Level.OK, //
				null, //
				null //
		), deviceData);
	}

	@Test
	void testSerializationLevelFalse() throws Exception {
		final var deviceData = OdooDeviceData.serializer().deserialize("""
				        {
				            "id": 1,
				            "name": "edge0",
				            "comment": false,
				            "producttype": "",
				            "role": "guest",
				            "openems_sum_state_level": false
				        }
				""".stripIndent());

		assertEquals(new OdooDeviceData(//
				"edge0", //
				"", //
				"", //
				Role.GUEST, //
				null, //
				Level.OK, //
				null, //
				null //
		), deviceData);
	}

	@Test
	void testSerializationLevelString() throws Exception {
		final var deviceData = OdooDeviceData.serializer().deserialize("""
				        {
				            "id": 1,
				            "name": "edge0",
				            "comment": false,
				            "producttype": "",
				            "role": "guest",
				            "openems_sum_state_level": "fault"
				        }
				""".stripIndent());

		assertEquals(new OdooDeviceData(//
				"edge0", //
				"", //
				"", //
				Role.GUEST, //
				null, //
				Level.FAULT, //
				null, //
				null //
		), deviceData);
	}

	@Test
	void testSerializationLastMessageFalse() throws Exception {
		final var deviceData = OdooDeviceData.serializer().deserialize("""
				        {
				            "id": 1,
				            "name": "edge0",
				            "comment": "edge0 - Very Great Edge",
				            "producttype": "",
				            "role": "guest",
				            "openems_sum_state_level": "ok",
				            "lastmessage": false
				        }
				""".stripIndent());

		assertEquals(new OdooDeviceData(//
				"edge0", //
				"edge0 - Very Great Edge", //
				"", //
				Role.GUEST, //
				null, //
				Level.OK, //
				null, //
				null //
		), deviceData);
	}

	@Test
	void testSerializationLastMessageString() throws Exception {
		final var deviceData = OdooDeviceData.serializer().deserialize("""
				        {
				            "id": 1,
				            "name": "edge0",
				            "comment": "edge0 - Very Great Edge",
				            "producttype": "",
				            "role": "guest",
				            "openems_sum_state_level": "ok",
				            "lastmessage": "2020-01-01 10:10:10"
				        }
				""".stripIndent());

		assertEquals(new OdooDeviceData(//
				"edge0", //
				"edge0 - Very Great Edge", //
				"", //
				Role.GUEST, //
				ZonedDateTime.of(LocalDate.of(2020, 1, 1), LocalTime.of(10, 10, 10), ZoneId.of("UTC")), //
				Level.OK, //
				null, //
				null //
		), deviceData);
	}

	@Test
	void testSerializationProductTypeFalse() throws Exception {
		final var deviceData = OdooDeviceData.serializer().deserialize("""
				        {
				            "id": 1,
				            "name": "edge0",
				            "comment": "edge0 - Very Great Edge",
				            "producttype": false,
				            "role": "guest",
				            "openems_sum_state_level": "ok"
				        }
				""".stripIndent());

		assertEquals(new OdooDeviceData(//
				"edge0", //
				"edge0 - Very Great Edge", //
				"", //
				Role.GUEST, //
				null, //
				Level.OK, //
				null, //
				null //
		), deviceData);
	}

	@Test
	void testSerializationProductTypeString() throws Exception {
		final var deviceData = OdooDeviceData.serializer().deserialize("""
				        {
				            "id": 1,
				            "name": "edge0",
				            "comment": "edge0 - Very Great Edge",
				            "producttype": "gen1",
				            "role": "guest",
				            "openems_sum_state_level": "ok"
				        }
				""".stripIndent());

		assertEquals(new OdooDeviceData(//
				"edge0", //
				"edge0 - Very Great Edge", //
				"gen1", //
				Role.GUEST, //
				null, //
				Level.OK, //
				null, //
				null //
		), deviceData);
	}

	@Test
	void testSerializationProductTypeError() {
		assertThrows(OpenemsException.class, () -> {
			OdooDeviceData.serializer().deserialize("""
					        {
					            "id": 1,
					            "name": "edge0",
					            "comment": "edge0 - Very Great Edge",
					            "producttype": 123,
					            "role": "guest",
					            "openems_sum_state_level": "ok"
					        }
					""".stripIndent());
		});
	}

	@Test
	void testParseInvalidSettings() {
		assertThrows(OpenemsException.class, () -> {
			OdooDeviceData.serializer().deserialize("""
					        {
					            "id": 1,
					            "name": "edge0",
					            "comment": "edge0 - Very Great Edge",
					            "producttype": "home",
					            "role": "guest",
					            "openems_sum_state_level": "ok",
					            "settings": ""
					        }
					""".stripIndent());
		});
	}

	@Test
	void testParseSettings() throws Exception {
		var settings = OdooDeviceData.serializer().deserialize("""
				        {
				            "id": 1,
				            "name": "edge0",
				            "comment": "edge0 - Very Great Edge",
				            "producttype": "home",
				            "role": "guest",
				            "openems_sum_state_level": "ok",
				            "settings": {"annualReview": ""}
				        }
				""".stripIndent()).settings();

		assertEquals(JsonUtils.buildJsonObject() //
				.addProperty("annualReview", "") //
				.build(), settings);
	}

}