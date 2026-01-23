package io.openems.edge.common.doc;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.test.DummyOptionsEnum;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DynamicDocText;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;

public class TestComponent extends AbstractDummyOpenemsComponent<TestComponent> implements OpenemsComponent {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		TEST_ENUM_CHANNEL(Doc.of(DummyOptionsEnum.values()) //
				.initialValue(DummyOptionsEnum.UNDEFINED)),

		TEST_INTEGER_CHANNEL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),

		TEST_STRING_CHANNEL(Doc.of(OpenemsType.STRING)),

		TEST_ERROR_CHANNEL_FOR_ENUM(Doc.of(Level.values()).textByChannel(TestComponent.class, TEST_ENUM_CHANNEL,
				DynamicDocText.fromEnumChannel(DummyOptionsEnum.class) //
						.when("value is 1", DummyOptionsEnum.VALUE_1) //
						.defaultText("undefined"))),

		TEST_ERROR_CHANNEL_FOR_INTEGER(Doc.of(Level.values()).textByChannel(TestComponent.class, TEST_INTEGER_CHANNEL,
				DynamicDocText.fromNumberChannel(Integer.class) //
						.whenIsAtLeast("power is too high", 5000) //
						.whenIsNegative("power is negative") //
						.whenIsInRange("power has a consistent power range", 2000, 3000) //
						.defaultText("power is fine"))),

		TEST_ERROR_CHANNEL_FOR_STRING(Doc.of(Level.values()).textByChannel(TestComponent.class, TEST_STRING_CHANNEL,
				DynamicDocText.fromStringChannel() //
						.when(":)", "Deadpool") //
						.when("<3", "Interstellar") //
						.whenStringContains(":(", "Jedi Knight") //
						.defaultText(":|")));

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public TestComponent(String id) {
		super(id, OpenemsComponent.ChannelId.values(), //
				ChannelId.values());
	}

	@Override
	protected TestComponent self() {
		return this;
	}

}
