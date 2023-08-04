package io.openems.edge.common.test;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.currency.Currency;
import io.openems.edge.common.meta.Meta;

public class DummyMeta extends AbstractOpenemsComponent implements Meta {

	public DummyMeta(String id, Currency currency) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Meta.ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		this._setCurrency(currency);
		super.activate(null, id, "", true);
	}
}
