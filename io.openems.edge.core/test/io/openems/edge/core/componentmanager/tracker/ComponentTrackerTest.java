package io.openems.edge.core.componentmanager.tracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.osgi.framework.Constants;

import io.openems.edge.core.componentmanager.MyDummyComponent;

public class ComponentTrackerTest {

	private static TrackedComponents newTrackedComponents() {
		return new TrackedComponents();
	}

	private static ComponentTracker newComponentTracker(TrackedComponents components) {
		return new ComponentTracker(components);
	}

	@Test
	public void testBind() throws Exception {
		final var components = newTrackedComponents();
		final var tracker = newComponentTracker(components);

		final var component = new MyDummyComponent("1");
		final var ref = Map.<String, Object>of(Constants.SERVICE_ID, 123L);

		assertEquals(0, components.getAllComponentIds().size());

		tracker.bindComponent(null, ref);
		assertEquals(0, components.getAllComponentIds().size());

		tracker.bindComponent(component, null);
		assertEquals(0, components.getAllComponentIds().size());

		tracker.bindComponent(component, ref);
		assertEquals(1, components.getAllComponentIds().size());
	}

	@Test
	public void testUnbind() throws Exception {
		final var components = newTrackedComponents();
		final var tracker = newComponentTracker(components);

		final var component = new MyDummyComponent("1");
		final var ref = Map.<String, Object>of(Constants.SERVICE_ID, 123L);

		assertEquals(0, components.getAllComponentIds().size());

		tracker.bindComponent(component, ref);
		assertEquals(1, components.getAllComponentIds().size());

		tracker.unbindComponent(null, ref);
		assertEquals(1, components.getAllComponentIds().size());

		tracker.unbindComponent(component, null);
		assertEquals(1, components.getAllComponentIds().size());

		tracker.unbindComponent(component, ref);
		assertEquals(0, components.getAllComponentIds().size());
	}

	@Test
	public void testDuplicates() throws Exception {
		final var components = newTrackedComponents();
		final var tracker = newComponentTracker(components);

		final var component1 = new MyDummyComponent("1");
		final var component2 = new MyDummyComponent("1"); // same ID, should be treated as duplicate
		final var ref1 = Map.<String, Object>of(Constants.SERVICE_ID, 123L);
		final var ref2 = Map.<String, Object>of(Constants.SERVICE_ID, 124L);

		assertEquals(0, components.getAllComponentIds().size());

		tracker.bindComponent(component1, ref1);
		assertEquals(1, components.getAllComponentIds().size());
		assertFalse(components.hasDuplicates());

		tracker.bindComponent(component2, ref2);
		assertEquals(1, components.getAllComponentIds().size());
		assertTrue(components.hasDuplicates());

		assertEquals(component1, components.getComponentById("1").get());

		component2.setId("2");
		tracker.updateComponent(component2, ref2);
		assertEquals(2, components.getAllComponentIds().size());
		assertFalse(components.hasDuplicates());

		component1.setId("2");
		tracker.updateComponent(component1, ref1);
		assertEquals(1, components.getAllComponentIds().size());
		assertTrue(components.hasDuplicates());

		assertEquals(component2, components.getComponentById("2").get());

		tracker.unbindComponent(component2, ref2);
		assertEquals(1, components.getAllComponentIds().size());
		assertFalse(components.hasDuplicates());
	}

}
