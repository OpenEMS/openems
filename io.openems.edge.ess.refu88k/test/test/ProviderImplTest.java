package test;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import test.ProviderImplTest;

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
		ProviderImplTest impl = new ProviderImplTest();
		assertNotNull(impl);
	}

}
