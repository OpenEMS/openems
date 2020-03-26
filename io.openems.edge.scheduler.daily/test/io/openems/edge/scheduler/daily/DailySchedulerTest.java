package io.openems.edge.scheduler.daily;

import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.LinkedHashSet;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.api.Controller;

public class DailySchedulerTest {

	@Test
	public void test() throws OpenemsNamedException {
		TimeLeapClock clock = new TimeLeapClock(
				LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant(ZoneOffset.UTC),
				ZoneOffset.UTC);

		DailyScheduler s = new DailyScheduler(clock);
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
			public String[] alwaysRunBeforeController_ids() {
				return new String[] { "c3" };
			}

			@Override
			public String controllerScheduleJson() {
				return "[" //
						+ "  {" //
						+ "    \"time\": \"08:00:00\"," //
						+ "    \"controllers\": [" //
						+ "      \"c1\"" //
						+ "    ]" //
						+ "  },  {" //
						+ "    \"time\": \"13:45:00\"," //
						+ "    \"controllers\": [\"c5\"]" //
						+ "  }" //
						+ "]";
			}

			@Override
			public String[] alwaysRunAfterController_ids() {
				return new String[] { "c4", "c2" };
			}
		});

		{
			/*
			 * 00:00
			 */
			LinkedHashSet<Controller> cs = s.getControllers();
			Iterator<Controller> iter = cs.iterator();
			assertEquals("c3", iter.next().id());
			assertEquals("c5", iter.next().id());
			assertEquals("c4", iter.next().id());
			assertEquals("c2", iter.next().id());
		}

		{
			/*
			 * 12:00
			 */
			clock.leap(12, ChronoUnit.HOURS);
			LinkedHashSet<Controller> cs = s.getControllers();
			Iterator<Controller> iter = cs.iterator();
			assertEquals("c3", iter.next().id());
			assertEquals("c1", iter.next().id());
			assertEquals("c4", iter.next().id());
			assertEquals("c2", iter.next().id());
		}

		{
			/*
			 * 14:00
			 */
			clock.leap(2, ChronoUnit.HOURS);
			LinkedHashSet<Controller> cs = s.getControllers();
			Iterator<Controller> iter = cs.iterator();
			assertEquals("c3", iter.next().id());
			assertEquals("c5", iter.next().id());
			assertEquals("c4", iter.next().id());
			assertEquals("c2", iter.next().id());
		}
	}

}
