package io.openems.backend.metadata.odoo.odoo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.backend.metadata.odoo.odoo.Domain.Operator;

public class OdooUtilsTest {

	@Test
	public void testGetAsStringArray() {
		var arr = OdooUtils.getAsStringArray(EdgeDevice.APIKEY, EdgeDevice.COMMENT);
		assertEquals(EdgeDevice.APIKEY.id(), arr[0]);
		assertEquals(EdgeDevice.COMMENT.id(), arr[1]);
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

}
