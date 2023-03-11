package io.openems.backend.metadata.odoo.odoo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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
	public void testConstructor() {
		final var domain_1 = new Domain(TEST_FIELD, Operator.LIKE, "Lorem ipsum");

		assertEquals("like", domain_1.operator);
		assertEquals("Lorem ipsum", domain_1.value);
		assertEquals(TEST_FIELD.id(), domain_1.field);

		final var domain_3 = new Domain(new Field[] { TEST_FIELD, TEST_FIELD }, Operator.EQ, "Lorem ipsum");
		final var domain_4 = new Domain(TEST_FIELD_ID + "." + TEST_FIELD_ID, Operator.EQ, "Lorem ipsum");

		assertEquals(domain_3, domain_3);
		assertEquals(domain_3, domain_4);
		assertNotEquals(domain_3, null);
		assertNotEquals(domain_3, 1);
	}
}
