package io.openems.edge.common.channel;

import static io.openems.edge.common.channel.ChannelUtils.setWriteValueIfNotRead;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingRunnable;
import io.openems.common.test.DummyOptionsEnum;
import io.openems.common.types.OpenemsType;
import io.openems.common.types.Tuple2;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.common.test.TestUtils;

class ChannelUtilsTest {

	@Test
	void testSetWriteValueIfNotReadBoolean() throws OpenemsNamedException {
		var channel = (BooleanWriteChannel) Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.createChannelInstance(null, null);
		testSetWriteValueIfNotRead(//
				channel, false, true, () -> setWriteValueIfNotRead(channel, true));
	}

	@Test
	void testSetWriteValueIfNotReadInteger() throws OpenemsNamedException {
		var channel = (IntegerWriteChannel) Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.createChannelInstance(null, null);
		testSetWriteValueIfNotRead(//
				channel, 0, 123, () -> setWriteValueIfNotRead(channel, 123));
	}

	@Test
	void testSetWriteValueIfNotReadEnum() throws OpenemsNamedException {
		var channel = (EnumWriteChannel) Doc.of(DummyOptionsEnum.values()) //
				.accessMode(AccessMode.READ_WRITE) //
				.createChannelInstance(null, null);
		testSetWriteValueIfNotRead(//
				channel, DummyOptionsEnum.UNDEFINED.getValue(), DummyOptionsEnum.VALUE_1.getValue(),
				() -> setWriteValueIfNotRead(channel, DummyOptionsEnum.VALUE_1));
	}

	private static <T, C extends WriteChannel<?>> void testSetWriteValueIfNotRead(C channel, T testValue1, T testValue2,
			ThrowingRunnable<OpenemsNamedException> method) throws OpenemsNamedException {
		// prepare
		var setvalue = new AtomicReference<Object>(null);
		channel.onSetNextWrite(v -> setvalue.set(v));

		// initialize
		TestUtils.withValue(channel, testValue1);
		assertEquals(testValue1, channel.value().get());
		assertEquals(Optional.empty(), channel.getNextWriteValue());
		assertNull(setvalue.get());

		// set value
		method.run();
		assertEquals(testValue2, setvalue.getAndSet(null)); // value was set
		assertEquals(testValue2, channel.getNextWriteValue().get());
		TestUtils.withValue(channel, testValue2);

		// NO-OP
		method.run();
		assertNull(setvalue.getAndSet(null)); // value was not set
		assertEquals(testValue2, channel.getNextWriteValue().get());
	}

	@Test
	void testSubscribeOnUpdate() {
		final var component = new DummyComponent("comp0");

		Consumer<Value<Integer>> onUpdate = mock();

		final var unsubscribe = ChannelUtils.subscribeOnUpdate(component, DummyComponent.ChannelId.INTEGER_CHANNEL,
				onUpdate);

		component.getIntegerChannel().setNextValue(1);
		component.getIntegerChannel().nextProcessImage();

		verify(onUpdate, times(1)).accept(any());

		unsubscribe.dispose();

		component.getIntegerChannel().setNextValue(1);
		component.getIntegerChannel().nextProcessImage();

		verify(onUpdate, times(1)).accept(any());
	}

	@Test
	void testSubscribeOnUpdateMultipleComponentsTwoChannels() {
		final var component1 = new DummyComponent("comp1");
		final var component2 = new DummyComponent("comp2");

		Consumer<Map<OpenemsComponent, Tuple2<Value<Integer>, Value<String>>>> onUpdate = mock();
		final var unsubscribe = ChannelUtils.subscribeOnUpdate(List.of(component1, component2),
				DummyComponent.ChannelId.INTEGER_CHANNEL, DummyComponent.ChannelId.STRING_CHANNEL, onUpdate);

		component1.getIntegerChannel().setNextValue(1);
		component1.getIntegerChannel().nextProcessImage();

		verify(onUpdate, times(1)).accept(argThat(values -> {
			final var comp1Values = values.get(component1);
			return values.size() == 1 && comp1Values.a().get() == 1 && comp1Values.b() == null;
		}));

		component1.getStringChannel().setNextValue("a");
		component1.getStringChannel().nextProcessImage();

		verify(onUpdate, times(2)).accept(argThat(values -> {
			final var comp1Values = values.get(component1);
			return values.size() == 1 && comp1Values.a().get() == 1 && comp1Values.b().get().equals("a");
		}));

		component2.getIntegerChannel().setNextValue(2);
		component2.getIntegerChannel().nextProcessImage();

		verify(onUpdate, times(3)).accept(argThat(values -> {
			final var comp1Values = values.get(component1);
			final var comp2Values = values.get(component2);
			return values.size() == 2 && comp1Values.a().get() == 1 && comp1Values.b().get().equals("a")//
					&& comp2Values.a().get() == 2 && comp2Values.b() == null;
		}));

		component2.getStringChannel().setNextValue("b");
		component2.getStringChannel().nextProcessImage();

		verify(onUpdate, times(4)).accept(argThat(values -> {
			final var comp1Values = values.get(component1);
			final var comp2Values = values.get(component2);
			return values.size() == 2 && comp1Values.a().get() == 1 && comp1Values.b().get().equals("a")//
					&& comp2Values.a().get() == 2 && comp2Values.b().get().equals("b");
		}));

		unsubscribe.dispose();

		component1.getIntegerChannel().setNextValue(10);
		component1.getIntegerChannel().nextProcessImage();
		component1.getStringChannel().setNextValue("aa");
		component1.getStringChannel().nextProcessImage();
		component2.getIntegerChannel().setNextValue(20);
		component2.getIntegerChannel().nextProcessImage();
		component2.getStringChannel().setNextValue("bb");
		component2.getStringChannel().nextProcessImage();

		verify(onUpdate, times(4)).accept(any());
	}

	@Test
	void testSubscribeOnUpdateMultipleComponentsOneChannel() {
		final var component1 = new DummyComponent("comp1");
		final var component2 = new DummyComponent("comp2");

		BiConsumer<Map<OpenemsComponent, Value<Integer>>, ChannelUtils.ChangedValue<OpenemsComponent, Integer>> onUpdate = mock();
		final var unsubscribe = ChannelUtils.subscribeOnUpdate(List.of(component1, component2),
				DummyComponent.ChannelId.INTEGER_CHANNEL, onUpdate);

		component1.getIntegerChannel().setNextValue(1);
		component1.getIntegerChannel().nextProcessImage();

		verify(onUpdate, times(1)).accept(argThat(values -> {
			final var comp1Values = values.get(component1);
			return values.size() == 1 && comp1Values.get() == 1;
		}), argThat(o -> {
			return o.component().equals(component1) && o.channelId().equals(DummyComponent.ChannelId.INTEGER_CHANNEL)
					&& o.newValue().get() == 1 && o.prevValue() == null;
		}));

		component2.getIntegerChannel().setNextValue(2);
		component2.getIntegerChannel().nextProcessImage();

		verify(onUpdate, times(1)).accept(argThat(values -> {
			final var comp1Values = values.get(component1);
			final var comp2Values = values.get(component2);
			return values.size() == 2 && comp1Values.get() == 1 && comp2Values.get() == 2;
		}), argThat(o -> {
			return o.component().equals(component2) && o.channelId().equals(DummyComponent.ChannelId.INTEGER_CHANNEL)
					&& o.newValue().get() == 2 && o.prevValue() == null;
		}));

		component1.getIntegerChannel().setNextValue(11);
		component1.getIntegerChannel().nextProcessImage();

		verify(onUpdate, times(1)).accept(argThat(values -> {
			final var comp1Values = values.get(component1);
			final var comp2Values = values.get(component2);
			return values.size() == 2 && comp1Values.get() == 11 && comp2Values.get() == 2;
		}), argThat(o -> {
			return o.component().equals(component1) && o.channelId().equals(DummyComponent.ChannelId.INTEGER_CHANNEL)
					&& o.newValue().get() == 11 && o.prevValue().get() == 1;
		}));

		component2.getIntegerChannel().setNextValue(22);
		component2.getIntegerChannel().nextProcessImage();

		verify(onUpdate, times(1)).accept(argThat(values -> {
			final var comp1Values = values.get(component1);
			final var comp2Values = values.get(component2);
			return values.size() == 2 && comp1Values.get() == 11 && comp2Values.get() == 22;
		}), argThat(o -> {
			return o.component().equals(component2) && o.channelId().equals(DummyComponent.ChannelId.INTEGER_CHANNEL)
					&& o.newValue().get() == 22 && o.prevValue().get() == 2;
		}));

		unsubscribe.dispose();

		component1.getIntegerChannel().setNextValue(111);
		component1.getIntegerChannel().nextProcessImage();
		component2.getIntegerChannel().setNextValue(222);
		component2.getIntegerChannel().nextProcessImage();

		verify(onUpdate, times(4)).accept(any(), any());
	}

	@Test
	void testSubscribeOnSetNextValue() {
		final var component = new DummyComponent("comp0");

		Consumer<Value<Integer>> onSetNextValue = mock();

		final var unsubscribe = ChannelUtils.subscribeOnSetNextValue(component,
				DummyComponent.ChannelId.INTEGER_CHANNEL, onSetNextValue);

		var inOrder = inOrder(onSetNextValue);

		// initial value published
		inOrder.verify(onSetNextValue, times(1)).accept(any());

		component.getIntegerChannel().setNextValue(1);
		inOrder.verify(onSetNextValue, times(1)).accept(any());

		unsubscribe.dispose();

		component.getIntegerChannel().setNextValue(2);
		inOrder.verifyNoMoreInteractions();
	}

	@Test
	void testSubscribeOnSetNextValueMultipleComponentsTwoChannels() {
		final var component1 = new DummyComponent("comp1");
		final var component2 = new DummyComponent("comp2");

		Consumer<Map<OpenemsComponent, Tuple2<Value<Integer>, Value<String>>>> onUpdate = mock();
		final var unsubscribe = ChannelUtils.subscribeOnSetNextValue(List.of(component1, component2),
				DummyComponent.ChannelId.INTEGER_CHANNEL, DummyComponent.ChannelId.STRING_CHANNEL, onUpdate);

		var inOrder = inOrder(onUpdate);

		inOrder.verify(onUpdate, times(4)).accept(any());

		component1.getIntegerChannel().setNextValue(1);
		inOrder.verify(onUpdate).accept(argThat(values -> {
			final var comp1Values = values.get(component1);
			return comp1Values.a().get() == 1 && comp1Values.b().get() == null;
		}));

		component1.getStringChannel().setNextValue("a");
		inOrder.verify(onUpdate).accept(argThat(values -> {
			final var comp1Values = values.get(component1);
			return comp1Values.a().get() == 1 && comp1Values.b().get().equals("a");
		}));

		component2.getIntegerChannel().setNextValue(2);
		inOrder.verify(onUpdate).accept(argThat(values -> {
			final var comp1Values = values.get(component1);
			final var comp2Values = values.get(component2);
			return comp1Values.a().get() == 1 && comp1Values.b().get().equals("a") && comp2Values.a().get() == 2
					&& comp2Values.b().get() == null;
		}));

		component2.getStringChannel().setNextValue("b");
		inOrder.verify(onUpdate).accept(argThat(values -> {
			final var comp1Values = values.get(component1);
			final var comp2Values = values.get(component2);
			return comp1Values.a().get() == 1 && comp1Values.b().get().equals("a") && comp2Values.a().get() == 2
					&& comp2Values.b().get().equals("b");
		}));

		unsubscribe.dispose();

		component1.getIntegerChannel().setNextValue(10);
		component1.getStringChannel().setNextValue("aa");
		component2.getIntegerChannel().setNextValue(20);
		component2.getStringChannel().setNextValue("bb");

		inOrder.verifyNoMoreInteractions();
	}

	@Test
	void testSubscribeOnSetNextValueMultipleComponentsOneChannel() {
		final var component1 = new DummyComponent("comp1");
		final var component2 = new DummyComponent("comp2");

		BiConsumer<Map<OpenemsComponent, Value<Integer>>, ChannelUtils.ChangedValue<OpenemsComponent, Integer>> onUpdate = mock();
		final var unsubscribe = ChannelUtils.subscribeOnSetNextValue(List.of(component1, component2),
				DummyComponent.ChannelId.INTEGER_CHANNEL, onUpdate);

		var inOrder = inOrder(onUpdate);

		inOrder.verify(onUpdate, times(2)).accept(argThat(values -> {
			return values.values().stream().allMatch(v -> v.get() == null);
		}), argThat(o -> {
			return o.newValue().get() == null;
		}));

		component1.getIntegerChannel().setNextValue(1);

		inOrder.verify(onUpdate, times(1)).accept(argThat(values -> {
			final var comp1Values = values.get(component1);
			return comp1Values.get() == 1;
		}), argThat(o -> {
			return o.component().equals(component1) && o.channelId().equals(DummyComponent.ChannelId.INTEGER_CHANNEL)
					&& o.newValue().get() == 1 && o.prevValue().get() == null;
		}));

		component2.getIntegerChannel().setNextValue(2);

		inOrder.verify(onUpdate, times(1)).accept(argThat(values -> {
			final var comp1Values = values.get(component1);
			final var comp2Values = values.get(component2);
			return comp1Values.get() == 1 && comp2Values.get() == 2;
		}), argThat(o -> {
			return o.component().equals(component2) && o.channelId().equals(DummyComponent.ChannelId.INTEGER_CHANNEL)
					&& o.newValue().get() == 2 && o.prevValue().get() == null;
		}));

		component1.getIntegerChannel().setNextValue(11);

		inOrder.verify(onUpdate, times(1)).accept(argThat(values -> {
			final var comp1Values = values.get(component1);
			final var comp2Values = values.get(component2);
			return comp1Values.get() == 11 && comp2Values.get() == 2;
		}), argThat(o -> {
			return o.component().equals(component1) && o.channelId().equals(DummyComponent.ChannelId.INTEGER_CHANNEL)
					&& o.newValue().get() == 11 && o.prevValue().get() == 1;
		}));

		component2.getIntegerChannel().setNextValue(22);

		inOrder.verify(onUpdate, times(1)).accept(argThat(values -> {
			final var comp1Values = values.get(component1);
			final var comp2Values = values.get(component2);
			return comp1Values.get() == 11 && comp2Values.get() == 22;
		}), argThat(o -> {
			return o.component().equals(component2) && o.channelId().equals(DummyComponent.ChannelId.INTEGER_CHANNEL)
					&& o.newValue().get() == 22 && o.prevValue().get() == 2;
		}));

		unsubscribe.dispose();

		component1.getIntegerChannel().setNextValue(111);
		component2.getIntegerChannel().setNextValue(222);

		inOrder.verifyNoMoreInteractions();
	}

	private static class DummyComponent extends AbstractDummyOpenemsComponent<DummyComponent> {

		enum ChannelId implements io.openems.edge.common.channel.ChannelId {
			INTEGER_CHANNEL(Doc.of(OpenemsType.INTEGER)), //
			STRING_CHANNEL(Doc.of(OpenemsType.STRING)), //
			;

			private final Doc doc;

			ChannelId(Doc doc) {
				this.doc = doc;
			}

			@Override
			public Doc doc() {
				return this.doc;
			}

		}

		protected DummyComponent(String id) {
			super(id, ChannelId.values());
		}

		@Override
		protected DummyComponent self() {
			return this;
		}

		Channel<Integer> getIntegerChannel() {
			return this.channel(ChannelId.INTEGER_CHANNEL);
		}

		Channel<String> getStringChannel() {
			return this.channel(ChannelId.STRING_CHANNEL);
		}

	}

}
