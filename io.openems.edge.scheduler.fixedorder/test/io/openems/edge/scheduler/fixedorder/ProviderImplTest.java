package io.openems.edge.scheduler.fixedorder;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.edge.scheduler.fixedorder.FixedOrder;

/*
 * Example JUNit test case
 *
 */

public class ProviderImplTest {

	/*
	 * Example test method
	 */

	@Test
	public void simple() {
		FixedOrder impl = new FixedOrder();
		assertNotNull(impl);
	}

}
