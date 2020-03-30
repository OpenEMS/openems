package io.openems.edge.scheduler.allalphabetically;

import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.LinkedHashSet;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.api.Controller;

public class AllAlphabeticallyTest {

	@Test
	public void test() throws OpenemsNamedException {
		AllAlphabetically s = new AllAlphabetically();
		s.componentManager = new DummyComponentManager() //
				.addComponent(new DummyController("c1")) //
				.addComponent(new DummyController("c2")) //
				.addComponent(new DummyController("c3")) //
				.addComponent(new DummyController("c4")) //
				.addComponent(new DummyController("c5"));

		s.activate(null, new Config() {

			@Override
			public Class<? extends Annotation> annotationType() {
				return null;
			}

			@Override
			public String webconsole_configurationFactory_nameHint() {
				return null;
			}

			@Override
			public String id() {
				return "scheduler0";
			}

			@Override
			public String alias() {
				return "";
			}

			@Override
			public boolean enabled() {
				return true;
			}

			@Override
			public String[] controllers_ids() {
				return new String[] { "c3", "c2" };
			}
		});

		LinkedHashSet<Controller> cs = s.getControllers();
		Iterator<Controller> iter = cs.iterator();
		assertEquals("c3", iter.next().id());
		assertEquals("c2", iter.next().id());
		assertEquals("c1", iter.next().id());
		assertEquals("c4", iter.next().id());
		assertEquals("c5", iter.next().id());
	}
}
