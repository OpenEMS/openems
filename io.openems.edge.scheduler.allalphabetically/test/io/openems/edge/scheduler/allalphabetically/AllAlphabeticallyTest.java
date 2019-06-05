package io.openems.edge.scheduler.allalphabetically;

import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;
import java.util.List;

import org.junit.Test;

import io.openems.edge.controller.api.Controller;

public class AllAlphabeticallyTest {

	@Test
	public void test() {
		AllAlphabetically s = new AllAlphabetically();

		s.addController(new DummyController("c1"));
		s.addController(new DummyController("c2"));
		s.addController(new DummyController("c3"));
		s.addController(new DummyController("c4"));
		s.addController(new DummyController("c5"));

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
			public int cycleTime() {
				return 0;
			}

			@Override
			public String[] controllers_ids() {
				return new String[] { "c3", "c2", "c6" };
			}
		});

		List<Controller> cs = s.getControllers();
		assertEquals("c3", cs.get(0).id());
		assertEquals("c2", cs.get(1).id());
		assertEquals("c1", cs.get(2).id());
		assertEquals("c4", cs.get(3).id());
		assertEquals("c5", cs.get(4).id());
	}
}
