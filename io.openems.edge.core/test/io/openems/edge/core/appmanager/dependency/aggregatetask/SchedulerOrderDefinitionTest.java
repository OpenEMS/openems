package io.openems.edge.core.appmanager.dependency.aggregatetask;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.Test;

import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderAggregateTaskImpl.SchedulerOrderDefinition;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration.SchedulerComponent;

public class SchedulerOrderDefinitionTest {

	@Test
	public void testOrderByFactoryId() {
		final var order = new SchedulerOrderDefinition() //
				.thenByFactoryId("1") //
				.thenByFactoryId("2") //
				.thenByFactoryId("3") //
				.thenByFactoryId("4");

		final var items = List.of(//
				new SchedulerComponent("", "1", ""), //
				new SchedulerComponent("", "2", ""), //
				new SchedulerComponent("", "3", ""), //
				new SchedulerComponent("", "4", ""));

		forEachCombination(items, () -> new TreeSet<>(order), combination -> {
			final var iterator = combination.iterator();

			assertTrue(iterator.hasNext());
			assertEquals(new SchedulerComponent("", "1", ""), iterator.next());
			assertTrue(iterator.hasNext());
			assertEquals(new SchedulerComponent("", "2", ""), iterator.next());
			assertTrue(iterator.hasNext());
			assertEquals(new SchedulerComponent("", "3", ""), iterator.next());
			assertTrue(iterator.hasNext());
			assertEquals(new SchedulerComponent("", "4", ""), iterator.next());

			assertFalse(iterator.hasNext());
		});
	}

	@Test
	public void testOrderByCreatedAppId() {
		final var order = new SchedulerOrderDefinition() //
				.thenByCreatedAppId("1") //
				.thenByCreatedAppId("2") //
				.thenByCreatedAppId("3") //
				.thenByCreatedAppId("4");

		final var items = List.of(//
				new SchedulerComponent("", "", "1"), //
				new SchedulerComponent("", "", "2"), //
				new SchedulerComponent("", "", "3"), //
				new SchedulerComponent("", "", "4"));

		forEachCombination(items, () -> new TreeSet<>(order), combination -> {
			final var iterator = combination.iterator();

			assertTrue(iterator.hasNext());
			assertEquals(new SchedulerComponent("", "", "1"), iterator.next());
			assertTrue(iterator.hasNext());
			assertEquals(new SchedulerComponent("", "", "2"), iterator.next());
			assertTrue(iterator.hasNext());
			assertEquals(new SchedulerComponent("", "", "3"), iterator.next());
			assertTrue(iterator.hasNext());
			assertEquals(new SchedulerComponent("", "", "4"), iterator.next());

			assertFalse(iterator.hasNext());
		});
	}

	@Test
	public void testOrderWithNestedOrder() {
		final var order = new SchedulerOrderDefinition() //
				.thenByFactoryId("1") //
				.thenBy(new SchedulerOrderDefinition() //
						.thenByFactoryId("2") //
						.thenByFactoryId("3")) //
				.thenByFactoryId("4");

		final var items = List.of(//
				new SchedulerComponent("", "1", ""), //
				new SchedulerComponent("", "2", ""), //
				new SchedulerComponent("", "3", ""), //
				new SchedulerComponent("", "4", ""));

		forEachCombination(items, () -> new TreeSet<>(order), combination -> {
			final var iterator = combination.iterator();

			assertTrue(iterator.hasNext());
			assertEquals(new SchedulerComponent("", "1", ""), iterator.next());
			assertTrue(iterator.hasNext());
			assertEquals(new SchedulerComponent("", "2", ""), iterator.next());
			assertTrue(iterator.hasNext());
			assertEquals(new SchedulerComponent("", "3", ""), iterator.next());
			assertTrue(iterator.hasNext());
			assertEquals(new SchedulerComponent("", "4", ""), iterator.next());

			assertFalse(iterator.hasNext());
		});
	}

	@Test
	public void testOrderWithFilter() {
		final var order = new SchedulerOrderDefinition() //
				.thenByFactoryId("1") //
				.thenBy(new SchedulerOrderDefinition() //
						.filterByFactoryId("2") //
						.thenByCreatedAppId("1") //
						.thenByCreatedAppId("2")) //
				.thenByFactoryId("3");

		final var items = List.of(//
				new SchedulerComponent("", "1", ""), //
				new SchedulerComponent("", "2", "1"), //
				new SchedulerComponent("", "2", "2"), //
				new SchedulerComponent("", "3", ""));

		forEachCombination(items, () -> new TreeSet<>(order), combination -> {
			final var iterator = combination.iterator();

			assertTrue(iterator.hasNext());
			assertEquals(new SchedulerComponent("", "1", ""), iterator.next());
			assertTrue(iterator.hasNext());
			assertEquals(new SchedulerComponent("", "2", "1"), iterator.next());
			assertTrue(iterator.hasNext());
			assertEquals(new SchedulerComponent("", "2", "2"), iterator.next());
			assertTrue(iterator.hasNext());
			assertEquals(new SchedulerComponent("", "3", ""), iterator.next());

			assertFalse(iterator.hasNext());
		});
	}

	@Test
	public void testOrderWithRest() {
		final var order = new SchedulerOrderDefinition() //
				.thenByFactoryId("1") //
				.thenBy(new SchedulerOrderDefinition() //
						.filterByFactoryId("2") //
						.thenByCreatedAppId("1") //
						.rest()) //
				.thenByFactoryId("3");

		final var items = List.of(//
				new SchedulerComponent("", "1", ""), //
				new SchedulerComponent("", "2", "1"), //
				new SchedulerComponent("", "2", "2"), //
				new SchedulerComponent("", "3", ""));

		forEachCombination(items, () -> new TreeSet<>(order), combination -> {
			final var iterator = combination.iterator();

			assertTrue(iterator.hasNext());
			assertEquals(new SchedulerComponent("", "1", ""), iterator.next());
			assertTrue(iterator.hasNext());
			assertEquals(new SchedulerComponent("", "2", "1"), iterator.next());
			assertTrue(iterator.hasNext());
			assertEquals(new SchedulerComponent("", "2", "2"), iterator.next());
			assertTrue(iterator.hasNext());
			assertEquals(new SchedulerComponent("", "3", ""), iterator.next());

			assertFalse(iterator.hasNext());
		});
	}

	private static <T> void forEachCombination(//
			final List<T> items, //
			final Supplier<Set<T>> setSupplier, //
			final Consumer<Set<T>> onEachCombination //
	) {
		if (items.size() == 1) {
			final var set = setSupplier.get();
			set.add(items.get(0));
			onEachCombination.accept(set);
			return;
		}
		for (int i = 0; i < items.size(); i++) {
			final var currentItem = items.get(i);
			final var sublist = new ArrayList<>(items);
			sublist.remove(i);
			forEachCombination(sublist, () -> {
				final var set = setSupplier.get();
				set.add(currentItem);
				return set;
			}, onEachCombination);
		}
	}
}
