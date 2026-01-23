package io.openems.common.utils;

import static io.openems.common.utils.ReflectionUtils.invokeMethodWithoutArgumentsViaReflection;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class ReflectionUtilsTest {

	private static class ParentClass {

		@SuppressWarnings("unused")
		private String parentMethod() {
			return "parent";
		}

	}

	private static class ChildClass extends ParentClass {

		@SuppressWarnings("unused")
		private String childMethod() {
			return "child";
		}

	}

	@Test
	public void testInvokeMethodWithoutArgumentsViaReflection() {
		final var instance = new ChildClass();
		assertEquals("parent", invokeMethodWithoutArgumentsViaReflection(ParentClass.class, instance, "parentMethod"));
		assertEquals("child", invokeMethodWithoutArgumentsViaReflection(ChildClass.class, instance, "childMethod"));
		assertEquals("child", invokeMethodWithoutArgumentsViaReflection(instance, "childMethod"));
		assertThrows(ReflectionUtils.ReflectionException.class, () -> {
			invokeMethodWithoutArgumentsViaReflection(ChildClass.class, instance, "parentMethod");
		});
		assertThrows(ReflectionUtils.ReflectionException.class, () -> {
			invokeMethodWithoutArgumentsViaReflection(instance, "parentMethod");
		});
	}
}