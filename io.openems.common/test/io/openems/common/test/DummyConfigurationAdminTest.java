package io.openems.common.test;

import static io.openems.common.utils.DictionaryUtils.getAsString;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;

import io.openems.common.test.DummyConfigurationAdmin.DummyConfiguration;

public class DummyConfigurationAdminTest {

	@Test
	public void testListConfigurations() throws IOException, InvalidSyntaxException {
		var sut = new DummyConfigurationAdmin();
		sut.addConfiguration("foo", new DummyConfiguration() //
				.addProperty("id", "foo") //
				.addProperty("enabled", true));
		sut.addConfiguration("bar", new DummyConfiguration() //
				.addProperty("id", "bar") //
				.addProperty("enabled", false));
		{
			var cs = sut.listConfigurations(null);
			assertEquals(2, cs.length);
		}
		{
			var cs = sut.listConfigurations("(enabled=true)");
			assertEquals(1, cs.length);
			assertEquals("foo", getAsString(cs[0].getProperties(), "id"));
		}
		{
			var cs = sut.listConfigurations("(id=bar)");
			assertEquals(1, cs.length);
			assertEquals("bar", getAsString(cs[0].getProperties(), "id"));
		}
	}

}
