package io.openems.edge.common.test;

import io.openems.common.channel.AccessMode;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.currency.Currency;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;

public class DummyMeta extends AbstractDummyOpenemsComponent<DummyMeta> implements Meta {

	private final OpenemsEdgeOem oem = new DummyOpenemsEdgeOem();

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

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return Meta.getModbusSlaveTable(accessMode, this.oem);
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
