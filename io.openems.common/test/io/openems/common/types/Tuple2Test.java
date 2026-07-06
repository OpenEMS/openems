package io.openems.common.types;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class Tuple2Test {

	@Test
	void of() {
		final var tuple = Tuple2.of("A", "B");
		assertEquals(new Tuple2<>("A", "B"), tuple);
	}

	@Test
	void withA() {
		final var tuple = Tuple2.of("A", "B");
		final var updated = tuple.withA("AA");

		assertEquals(new Tuple2<>("AA", "B"), updated);
		assertEquals(new Tuple2<>("A", "B"), tuple);
	}

	@Test
	void withB() {
		final var tuple = Tuple2.of("A", "B");
		final var updated = tuple.withB("BB");

		assertEquals(new Tuple2<>("A", "BB"), updated);
		assertEquals(new Tuple2<>("A", "B"), tuple);
	}

	@Test
	void mapA() {
		final var tuple = Tuple2.of("ABC", 123);
		final var mapped = tuple.mapA(String::length);

		assertEquals(new Tuple2<>(3, 123), mapped);
	}

	@Test
	void mapB() {
		final var tuple = Tuple2.of("ABC", "DEF");
		final var mapped = tuple.mapB(String::length);

		assertEquals(new Tuple2<>("ABC", 3), mapped);
	}

	@Test
	void map() {
		final var tuple = Tuple2.of("AB", "123");
		final var mapped = tuple.map(String::length, Integer::parseInt);

		assertEquals(new Tuple2<>(2, 123), mapped);
	}

	@Test
	void combine() {
		final var tupleA = Tuple2.of("A", "B");
		final var tupleB = Tuple2.of("C", "D");
		final var combined = tupleA.combine(tupleB, (a, b) -> a + b, (a, b) -> a + "-" + b);

		assertEquals(new Tuple2<>("AC", "B-D"), combined);
	}

	@Test
	void swap() {
		final var tuple = Tuple2.of("A", 1);

		assertEquals(new Tuple2<>(1, "A"), tuple.swap());
	}

	@Test
	void apply() {
		final var tuple = Tuple2.of("A", 2);

		assertEquals("AA", tuple.apply(String::repeat));
	}

}