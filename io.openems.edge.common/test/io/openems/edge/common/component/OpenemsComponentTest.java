package io.openems.edge.common.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

public class OpenemsComponentTest {

	private static class MyComponent extends AbstractOpenemsComponent {

		protected MyComponent() {
			super(new io.openems.edge.common.channel.ChannelId[0]);
		}
	}

	@Test
	public void test() throws NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		var method = OpenemsComponent.class.getDeclaredMethod("getComponentIdentifier", OpenemsComponent.class);
		method.setAccessible(true);

		// Component is 'null'
		assertNull(method.invoke(null, (OpenemsComponent) null));

		var sut = new MyComponent();
		// 'id' is null
		assertEquals(MyComponent.class.getSimpleName(), method.invoke(null, sut));

		sut.activate(null, "foo", null, true);
		// 'id' is set
		assertEquals("foo", method.invoke(null, sut));
	}

}
