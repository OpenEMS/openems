package io.openems.edge.core.componentmanager.jsonrpc;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.AccessMode.WRITE_ONLY;
import static io.openems.common.channel.Unit.AMPERE;
import static io.openems.common.types.OpenemsType.LONG;
import static io.openems.common.types.OpenemsType.STRING;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.channel.Level;
import io.openems.common.test.DummyOptionsEnum;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId.ChannelIdImpl;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.internal.AbstractDoc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.core.componentmanager.jsonrpc.ChannelExportXlsxResponse.ChannelRow;

public class ChannelExportXlsxResponseTest {

	private static class DummyComponent extends AbstractDummyOpenemsComponent<DummyComponent>
			implements OpenemsComponent {

		public DummyComponent(String id) {
			super(id, id, OpenemsComponent.ChannelId.values());
		}

		@Override
		protected DummyComponent self() {
			return this;
		}

		@Override
		protected Channel<?> addChannel(io.openems.edge.common.channel.ChannelId channelId) {
			return super.addChannel(channelId);
		}
	}

	private static final DummyComponent COMPONENT = new DummyComponent("ctrl0");

	private static Channel<?> generateChannel(String name, AbstractDoc<?> doc) {
		return COMPONENT.addChannel(new ChannelIdImpl(name, doc));
	}

	@Test
	public void testChannelRow() {
		var c = generateChannel("Foo0", new IntegerDoc());
		assertEquals(//
				new ChannelRow("Foo0", "UNDEFINED", "", "", "", "RO", "Integer", ""), //
				ChannelRow.fromChannel(c));
		TestUtils.withValue(c, 123);
		// NOTE for future improvement: handle int as int and not as String
		assertEquals(//
				new ChannelRow("Foo0", "123", "", "", "", "RO", "Integer", ""), //
				ChannelRow.fromChannel(c));

		c = generateChannel("Foo1", Doc.of(LONG) //
				.unit(AMPERE) //
				.text("bar") //
				.accessMode(READ_WRITE));
		TestUtils.withValue(c, 456);
		assertEquals(//
				new ChannelRow("Foo1", "456", "A", "bar", "", "RW", "Long", ""), //
				ChannelRow.fromChannel(c));

		c = generateChannel("Foo2", Doc.of(STRING) //
				.unit(AMPERE) //
				.text("bar") //
				.accessMode(WRITE_ONLY));
		TestUtils.withValue(c, 456);
		c.setMetaInfo("meta_info");
		assertEquals(//
				new ChannelRow("Foo2", "", "A", "bar", "meta_info", "WO", "String", ""), //
				ChannelRow.fromChannel(c));
	}

	@Test
	public void testChannelRowStateCollector() {
		final var foo3 = (StateChannel) generateChannel("Foo3", Doc.of(Level.WARNING));
		TestUtils.withValue(foo3, true);
		TestUtils.activateNextProcessImage(COMPONENT);
		assertEquals(//
				new ChannelRow("Foo3", "true", "", "", "", "RO", "State", ""), //
				ChannelRow.fromChannel(foo3));

		final var state = COMPONENT.getStateChannel();
		state.nextProcessImage();
		assertEquals(//
				new ChannelRow("State", "2:Warning", "", "0:Ok, 1:Info, 2:Warning, 3:Fault | WARNING: Foo3", "", "RO",
						"Enum", "OpenemsComponent"), //
				ChannelRow.fromChannel(state));
	}

	@Test
	public void testChannelRowEnum() {
		var c = generateChannel("Foo4", Doc.of(DummyOptionsEnum.values()));
		assertEquals(//
				new ChannelRow("Foo4", "-1:Undefined", "", "1:One", "", "RO", "Enum", ""), //
				ChannelRow.fromChannel(c));
		TestUtils.withValue(c, DummyOptionsEnum.VALUE_1);
		assertEquals(//
				new ChannelRow("Foo4", "1:One", "", "1:One", "", "RO", "Enum", ""), //
				ChannelRow.fromChannel(c));
	}
}
