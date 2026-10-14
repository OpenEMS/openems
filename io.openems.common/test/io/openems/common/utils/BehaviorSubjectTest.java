package io.openems.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

class BehaviorSubjectTest {

	@Test
	void testSubscribe() {
		final var subject = new BehaviorSubject<>("");

		final Consumer<String> subscriber = mock();
		subject.subscribe(subscriber);
		verify(subscriber, times(1)).accept("");

		subject.setValue("newValue");
		verify(subscriber, times(1)).accept("newValue");
	}

	@Test
	void unsubscribe() {
		final var subject = new BehaviorSubject<>("");

		final Consumer<String> subscriber = mock();
		subject.subscribe(subscriber);
		verify(subscriber, times(1)).accept("");

		subject.unsubscribe(subscriber);
		subject.setValue("newValue");
		verify(subscriber, times(0)).accept("newValue");
	}

	@Test
	void getValue() {
		final var subject = new BehaviorSubject<>("value");
		assertEquals("value", subject.getValue());
	}

	@Test
	void hasSubscribers() {
		final var subject = new BehaviorSubject<>("value");
		assertFalse(subject.hasSubscribers());

		final Consumer<String> subscriber = mock();
		subject.subscribe(subscriber);
		assertTrue(subject.hasSubscribers());

		subject.unsubscribe(subscriber);
		assertFalse(subject.hasSubscribers());
	}

	@Test
	void setValue() {
		final var subject = new BehaviorSubject<>("value");
		assertEquals("value", subject.getValue());
		subject.setValue("newValue");
		assertEquals("newValue", subject.getValue());
	}

	@Test
	void updateValue() {
		final var subject = new BehaviorSubject<>("value");
		subject.updateValue(s -> s + "_updated");
		assertEquals("value_updated", subject.getValue());
	}

	@Test
	void testUnsubscribeDuringNotification() {
		final var subject = new BehaviorSubject<>("initial");

		final var activeValue = new AtomicReference<String>();
		final Consumer<String> subscriber2 = new Consumer<>() {
			@Override
			public void accept(String value) {
				activeValue.set(value);
				if ("unsubscribe".equals(value)) {
					subject.unsubscribe(this);
				}
			}
		};

		subject.subscribe(mock());
		subject.subscribe(subscriber2);
		subject.subscribe(mock());

		assertEquals("initial", activeValue.get());
		subject.setValue("newValue");
		assertEquals("newValue", activeValue.get());

		subject.setValue("unsubscribe");
		assertEquals("unsubscribe", activeValue.get());

		subject.setValue("newValue2");
		assertEquals("unsubscribe", activeValue.get());
	}

}
