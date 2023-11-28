package io.openems.edge.controller.api.backend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;

import org.junit.Test;

import com.google.common.collect.Lists;

import io.openems.common.function.ThrowingFunction;
import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.backend.SendChannelValuesWorkerTest.DummyComponent.DummyEnum;

public class SendChannelValuesWorkerTest {

	@Test
	public void testAggregateNaturalCumulated() {
		final var value = SendChannelValuesWorker.aggregate(true, OpenemsType.LONG, //
				Lists.newArrayList(2, 4));
		assertEquals(4, value.getAsLong());
	}

	@Test
	public void testAggregateNaturalNotCumulated() {
		final var value = SendChannelValuesWorker.aggregate(false, OpenemsType.LONG, //
				Lists.newArrayList(2, 7));
		assertEquals(5, value.getAsLong());
	}

	@Test
	public void testAggregateFloatingCumulated() {
		final var value = SendChannelValuesWorker.aggregate(true, OpenemsType.DOUBLE, //
				Lists.newArrayList(2.23, 4.75));
		assertEquals(4.75, value.getAsDouble(), 0);
	}

	@Test
	public void testAggregateFloatingNotCumulated() {
		final var value = SendChannelValuesWorker.aggregate(false, OpenemsType.DOUBLE, //
				Lists.newArrayList(2.9, 7.1));
		assertEquals(5, value.getAsDouble(), 0);
	}

	@Test
	public void testAggregateStringCumulated() {
		final var value = SendChannelValuesWorker.aggregate(true, OpenemsType.STRING, //
				Lists.newArrayList("a", "b", "c", "d", "e"));
		assertEquals("a", value.getAsString());
	}

	@Test
	public void testAggregateStringNotCumulated() {
		final var value = SendChannelValuesWorker.aggregate(false, OpenemsType.STRING, //
				Lists.newArrayList("a", "b", "c", "d", "e"));
		assertEquals("a", value.getAsString());
	}

	@Test
	public void testAggregateEnumDocExactValueInsteadOfRounded() throws Exception {
		testAggregateEnumChannel(3, channel -> {
			writeValue(channel, DummyEnum.VALUE_1);
			writeValue(channel, DummyEnum.VALUE_3);
			writeValue(channel, DummyEnum.VALUE_3);

			return DummyEnum.VALUE_3;
		});
	}

	@Test
	public void testAggregateEnumChannelHandleNulls() throws Exception {
		testAggregateEnumChannel(4, channel -> {
			writeValue(channel, DummyEnum.VALUE_1);
			writeValue(channel, null);
			writeValue(channel, DummyEnum.VALUE_3);
			writeValue(channel, DummyEnum.VALUE_3);

			return DummyEnum.VALUE_3;
		});
	}

	@Test
	public void testAggregateEnumChannelSameAmountButLatests() throws Exception {
		testAggregateEnumChannel(4, channel -> {
			writeValue(channel, DummyEnum.VALUE_1);
			writeValue(channel, DummyEnum.VALUE_1);
			writeValue(channel, DummyEnum.VALUE_3);
			writeValue(channel, DummyEnum.VALUE_3);

			return DummyEnum.VALUE_3;
		});
	}

	@Test
	public void testAggregateEnumChannelSameAmountButLatestsWithAll() throws Exception {
		testAggregateEnumChannel(3, channel -> {
			writeValue(channel, DummyEnum.VALUE_1);
			writeValue(channel, DummyEnum.VALUE_2);
			writeValue(channel, DummyEnum.VALUE_3);

			return DummyEnum.VALUE_3;
		});
	}

	@Test
	public void testAggregateEnumChannelSameAmountButLatestsWithAllViceVersa() throws Exception {
		testAggregateEnumChannel(3, channel -> {
			writeValue(channel, DummyEnum.VALUE_3);
			writeValue(channel, DummyEnum.VALUE_2);
			writeValue(channel, DummyEnum.VALUE_1);

			return DummyEnum.VALUE_1;
		});
	}

	private static void testAggregateEnumChannel(int numberOfValues,
			ThrowingFunction<Channel<?>, DummyEnum, Exception> test) throws Exception {
		final var component = new DummyComponent("component0");
		final var channel = component.<Channel<DummyEnum>>channel(DummyComponent.ChannelId.DUMMY_ENUM_CHANNEL);
		final var start = LocalDateTime.now();
		final var expectedValue = test.apply(channel);
		final var end = LocalDateTime.now().plusSeconds(1);

		assertEquals(numberOfValues, channel.getPastValues().size());

		final var aggregatedValue = SendChannelValuesWorker.aggregateEnumChannel(channel, start, end);
		assertTrue(aggregatedValue.isJsonPrimitive());
		assertTrue(aggregatedValue.getAsJsonPrimitive().isNumber());
		assertEquals(expectedValue.getValue(), aggregatedValue.getAsInt());
	}

	private static void writeValue(Channel<?> channel, Object value) throws InterruptedException {
		// Needs sleep to not overwrite the latest value
		Thread.sleep(1);
		channel.setNextValue(value);
		channel.nextProcessImage();
	}

	public static class DummyComponent extends AbstractOpenemsComponent implements OpenemsComponent {

		public enum DummyEnum implements OptionsEnum {
			UNDEFINED(-1, "Undefined"), //
			VALUE_1(1, "Value 1"), //
			VALUE_2(2, "Value 2"), //
			VALUE_3(3, "Value 3"), //
			;

			private final int value;
			private final String name;

			private DummyEnum(int value, String name) {
				this.value = value;
				this.name = name;
			}

			@Override
			public int getValue() {
				return this.value;
			}

			@Override
			public String getName() {
				return this.name;
			}

			@Override
			public OptionsEnum getUndefined() {
				return DummyEnum.UNDEFINED;
			}

		}

		public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
			/**
			 * Dummy state channels for testing.
			 */
			DUMMY_ENUM_CHANNEL(Doc.of(DummyEnum.values())), //
			;

			private final Doc doc;

			private ChannelId(Doc doc) {
				this.doc = doc;
			}

			@Override
			public Doc doc() {
				return this.doc;
			}
		}

		public DummyComponent(String id) {
			super(//
					OpenemsComponent.ChannelId.values(), //
					ChannelId.values() //
			);
			super.activate(null, id, "", true);
		}

	}

}
