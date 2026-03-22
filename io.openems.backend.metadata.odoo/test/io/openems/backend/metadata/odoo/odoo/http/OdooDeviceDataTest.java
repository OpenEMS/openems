package io.openems.backend.metadata.odoo.odoo.http;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;

public class OdooDeviceDataTest {

	@Test
	public void testSerializationCommentFalse() throws OpenemsError.OpenemsNamedException {
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
	public void testSerializationCommentString() throws OpenemsError.OpenemsNamedException {
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
	public void testSerializationLevelFalse() throws OpenemsError.OpenemsNamedException {
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
	public void testSerializationLevelString() throws OpenemsError.OpenemsNamedException {
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
	public void testSerializationLastMessageFalse() throws OpenemsError.OpenemsNamedException {
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
	public void testSerializationLastMessageString() throws OpenemsError.OpenemsNamedException {
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
	public void testSerializationProductTypeFalse() throws Exception {
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
	public void testSerializationProductTypeString() throws Exception {
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

	@Test(expected = OpenemsException.class)
	public void testSerializationProductTypeError() throws Exception {
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
	}

}