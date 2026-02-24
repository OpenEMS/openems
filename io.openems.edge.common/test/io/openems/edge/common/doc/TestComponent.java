package io.openems.edge.common.doc;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.test.DummyOptionsEnum;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DynamicStateChannelDoc;
import io.openems.edge.common.channel.dynamicdoctext.ParameterProvider;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.common.type.TextProvider;

public class TestComponent extends AbstractDummyOpenemsComponent<TestComponent> implements OpenemsComponent {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		TEST_ENUM_CHANNEL(Doc.of(DummyOptionsEnum.values()) //
				.initialValue(DummyOptionsEnum.UNDEFINED)),

		TEST_INTEGER_CHANNEL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),

		TEST_STRING_CHANNEL(Doc.of(OpenemsType.STRING)),

		TEST_ERROR_CHANNEL_FOR_ENUM(DynamicStateChannelDoc.builder(Level.WARNING) //
				.setDynamicText(//
						TextProvider.byStatic("{0}"), //
						ParameterProvider.byEnumChannel(DummyOptionsEnum.class, TEST_ENUM_CHANNEL) //
								.when(DummyOptionsEnum.VALUE_1, TextProvider.byStatic("value is 1")) //
								.defaultText(TextProvider.byStatic("undefined"))) //
				.build()),

		TEST_ERROR_CHANNEL_FOR_INTEGER(DynamicStateChannelDoc.builder(Level.WARNING) //
				.setDynamicText(//
						TextProvider.byStatic("{0}"), //
						ParameterProvider.byNumberChannel(TEST_INTEGER_CHANNEL) //
								.whenIsAtLeast(5000, TextProvider.byStatic("power is too high")) //
								.whenIsNegative(TextProvider.byStatic("power is negative")) //
								.whenIsInRange(2000, 3000, TextProvider.byStatic("power has a consistent power range")) //
								.defaultText(TextProvider.byStatic("power is fine"))) //
				.build()),

		TEST_ERROR_CHANNEL_FOR_STRING(DynamicStateChannelDoc.builder(Level.WARNING) //
				.setDynamicText(//
						TextProvider.byStatic("{0}"), //
						ParameterProvider.byStringChannel(TEST_STRING_CHANNEL) //
								.when("Deadpool", TextProvider.byStatic(":)")) //
								.when("Interstellar", TextProvider.byStatic("<3")) //
								.whenStringContains("Jedi Knight", TextProvider.byStatic(":(")) //
								.defaultText(TextProvider.byStatic(":|")) //
				).build()),

		TEST_COMBINED(DynamicStateChannelDoc.builder(Level.WARNING) //
				.setDynamicText(//
						TextProvider.byStatic("{0} {1}"), //
						ParameterProvider.byChannel(TEST_INTEGER_CHANNEL), //
						ParameterProvider.byChannel(TEST_STRING_CHANNEL) //
				).build());

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
