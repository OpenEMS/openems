package io.openems.backend.metadata.odoo.odoo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import io.openems.backend.metadata.odoo.Field;
import io.openems.backend.metadata.odoo.odoo.Domain.Operator;

public class DomainTest {

	private static final String TEST_FIELD_ID = "test_field";
	private static final Field TEST_FIELD = new Field() {
		@Override
		public String id() {
			return TEST_FIELD_ID;
		}

		@Override
		public String name() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int index() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isQuery() {
			throw new UnsupportedOperationException();
		}
	};

	@Test
	public void testDummy() {
		assertThrows(UnsupportedOperationException.class, TEST_FIELD::name);
		assertThrows(UnsupportedOperationException.class, TEST_FIELD::index);
		assertThrows(UnsupportedOperationException.class, TEST_FIELD::isQuery);
	}

	@Test
	public void testConstructor() {
		final var domain1 = new Domain(TEST_FIELD, Operator.LIKE, "Lorem ipsum");

		assertEquals("like", domain1.operator);
		assertEquals("Lorem ipsum", domain1.value);
		assertEquals(TEST_FIELD.id(), domain1.field);

		final var domain2 = new Domain(new Field[] { TEST_FIELD, TEST_FIELD }, Operator.EQ, "Lorem ipsum");
		final var domain3 = new Domain(TEST_FIELD_ID + "." + TEST_FIELD_ID, Operator.EQ, "Lorem ipsum");

		assertNotEquals(domain1, domain3);
		assertEquals(domain3, domain3);
		assertEquals(domain2, domain3);
		assertNotEquals(domain3, null);
		assertNotEquals(domain3, 1);

		assertEquals(domain3.hashCode(), domain2.hashCode());
		assertNotEquals(domain1.hashCode(), domain2.hashCode());
	}
}
