package io.openems.common.utils;

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

public class ComparatorUtilsTest {

	@Test
	public void testComparatorIdList() {
		record Item(String value) {
		}

		final var order = List.of(//
				"Item2", //
				"Item3", //
				"Item4" //
		);

		final var items = List.of(//
				new Item("Item1"), //
				new Item("Item2"), //
				new Item("Item3"), //
				new Item("Item4"), //
				new Item("Item5"), //
				new Item(null) //
		);
		final var comparator = ComparatorUtils.comparatorIdList(order, Item::value);

		// Try every combination to have the same order
		forEachCombination(items, () -> new TreeSet<>(comparator), combination -> {
			final var iterator = combination.iterator();

			// initial order based on the order list
			assertTrue(iterator.hasNext());
			assertEquals(new Item("Item2"), iterator.next());
			assertTrue(iterator.hasNext());
			assertEquals(new Item("Item3"), iterator.next());
			assertTrue(iterator.hasNext());
			assertEquals(new Item("Item4"), iterator.next());

			// "random" order after the order list but still the same for every combination
			assertTrue(iterator.hasNext());
			assertEquals(new Item("Item1"), iterator.next());
			assertTrue(iterator.hasNext());
			assertEquals(new Item("Item5"), iterator.next());
			assertTrue(iterator.hasNext());
			assertEquals(new Item(null), iterator.next());
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
