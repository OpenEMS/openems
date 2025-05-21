package io.openems.backend.metadata.odoo.odoo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import org.junit.Test;

import io.openems.backend.metadata.odoo.Field;
import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.backend.metadata.odoo.odoo.Domain.Operator;

public class OdooUtilsTest {

	private static class Utility {
		private static enum Enum {
			FOO, BAR
		}

		private static enum Fields implements Field {
			INTEGER, STRING, ENUM, UNAVAILABLE, BOOLEAN, NOT_AVAILABLE;

			@Override
			public String id() {
				return this.name();
			}

			@Override
			public int index() {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean isQuery() {
				throw new UnsupportedOperationException();
			}
		}
	}

	@Test
	public void testGetAsStringArray() {
		var arr = OdooUtils.getAsStringArray(EdgeDevice.APIKEY, EdgeDevice.COMMENT);
		assertEquals(EdgeDevice.APIKEY.id(), arr[0]);
		assertEquals(EdgeDevice.COMMENT.id(), arr[1]);
	}

	@Test
	public void testDateTimeConvertion() {
		var time = ZonedDateTime.of(2000, 7, 26, 23, 54, 20, 4, ZoneId.of("Europe/Berlin"));
		var timeStr = OdooUtils.DateTime.dateTimeToString(time);

		var timeConv = OdooUtils.DateTime.stringToDateTime(timeStr);

		assertNotEquals(time, timeConv);

		var simpleTime = time.withZoneSameInstant(OdooUtils.DateTime.SERVER_TIMEZONE).withNano(0);

		assertEquals(timeConv, simpleTime);

		assertNull(OdooUtils.DateTime.dateTimeToString(null));
		assertNull(OdooUtils.DateTime.stringToDateTime(null));

		assertNull(OdooUtils.DateTime.stringToDateTime("Not a valid date"));
	}

	@Test
	public void testGetAsObjectArray() {
		var arr = OdooUtils.getAsObjectArray(new Domain(EdgeDevice.APIKEY, Operator.EQ, "foo"),
				new Domain(EdgeDevice.COMMENT, Operator.EQ, "bar"));
		{
			var v = (Object[]) arr[0];
			assertEquals(EdgeDevice.APIKEY.id(), v[0]);
			assertEquals(Operator.EQ.getValue(), v[1]);
			assertEquals("foo", v[2]);
		}
		{
			var v = (Object[]) arr[1];
			assertEquals(EdgeDevice.COMMENT.id(), v[0]);
			assertEquals(Operator.EQ.getValue(), v[1]);
			assertEquals("bar", v[2]);
		}
	}

	@Test
	public void testGetOdooReferenceId() {
		var reference = new Object[] { 1, "id" };
		var wrongObject = new Object[] { "foo", "bar" };

		assertEquals(Integer.valueOf(1), OdooUtils.getOdooReferenceId(reference).get());
		assertTrue(OdooUtils.getOdooReferenceId(wrongObject).isEmpty());
		assertTrue(OdooUtils.getOdooReferenceId(null).isEmpty());
	}

	@Test
	public void testValueConverter() {
		assertThrows(UnsupportedOperationException.class, Utility.Fields.INTEGER::isQuery);
		assertThrows(UnsupportedOperationException.class, Utility.Fields.INTEGER::index);

		var testData = Map.of(//
				Utility.Fields.INTEGER.id(), 0, //
				Utility.Fields.STRING.id(), "Test", //
				Utility.Fields.ENUM.id(), Utility.Enum.FOO, Utility.Fields.BOOLEAN.id(), true,
				Utility.Fields.UNAVAILABLE.id(), false);

		var integerVal = OdooUtils.getAs(Utility.Fields.INTEGER, testData, Integer.class);
		var stringVal = OdooUtils.getAs(Utility.Fields.STRING, testData, String.class);
		var enumVal = OdooUtils.getAsEnum(Utility.Fields.ENUM, testData, Utility.Enum.class, Utility.Enum.BAR);

		assertEquals(integerVal, Integer.valueOf(0));
		assertEquals(stringVal, "Test");
		assertEquals(enumVal, Utility.Enum.FOO);

		assertEquals(true, OdooUtils.getAsOrElse(Utility.Fields.BOOLEAN, testData, Boolean.class, false));
		assertEquals("else", OdooUtils.getAsOrElse(Utility.Fields.UNAVAILABLE, testData, String.class, "else"));

		assertNull(OdooUtils.getAs(Utility.Fields.INTEGER, null, Integer.class));
		assertNull(OdooUtils.getAs(null, testData, Integer.class));
		assertNull(OdooUtils.getAs(Utility.Fields.NOT_AVAILABLE, testData, Integer.class));

		// False types
		assertEquals("else", OdooUtils.getAsOrElse(Utility.Fields.INTEGER, testData, String.class, "else"));
		assertEquals(Integer.valueOf(0), OdooUtils.getAsOrElse(Utility.Fields.STRING, testData, Integer.class, 0));
		assertEquals(EdgeDevice.ID,
				OdooUtils.getAsEnum(Utility.Fields.ENUM, testData, EdgeDevice.class, EdgeDevice.ID));
		assertEquals(Utility.Enum.BAR,
				OdooUtils.getAsEnum(Utility.Fields.UNAVAILABLE, testData, Utility.Enum.class, Utility.Enum.BAR));
	}

}
