package io.openems.edge.common.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.currency.Currency;
import io.openems.edge.common.meta.Meta;

public class DummyMeta extends AbstractDummyOpenemsComponent<DummyMeta> implements Meta {

	public DummyMeta(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				Meta.ChannelId.values() //
		);
	}

	@Override
	protected DummyMeta self() {
		return this;
	}

	/**
	 * Set {@link Meta.ChannelId#CURRENCY}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyMeta withCurrency(Currency value) {
		TestUtils.withValue(this, Meta.ChannelId.CURRENCY, value);
		return this.self();
	}
}
