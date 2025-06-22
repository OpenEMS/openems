package io.openems.edge.common.test;

import java.util.Optional;

import io.openems.common.channel.AccessMode;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.currency.Currency;
import io.openems.edge.common.meta.Coordinates;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;

public class DummyMeta extends AbstractDummyOpenemsComponent<DummyMeta> implements Meta {

	private final OpenemsEdgeOem oem = new DummyOpenemsEdgeOem();
	private Optional<Coordinates> coordinates = Optional.empty();

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

	/**
	 * Set {@link Meta.ChannelId#IS_ESS_CHARGE_FROM_GRID_ALLOWED}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyMeta withIsEssChargeFromGridAllowed(boolean value) {
		TestUtils.withValue(this, Meta.ChannelId.IS_ESS_CHARGE_FROM_GRID_ALLOWED, value);
		return this.self();
	}

	@Override
	public int getGridConnectionPointFuseLimit() {
		return 32; // [A]
	}

	@Override
	public Optional<Coordinates> getCoordinates() {
		return this.coordinates;
	}

	/**
	 * Sets the coordinates for this {@link DummyMeta} instance and returns the
	 * instance itself.
	 *
	 * @param coordinates the optional coordinates
	 * @return the current {@link DummyMeta} instance with updated coordinates
	 */
	public DummyMeta withCoordinates(Coordinates coordinates) {
		this.coordinates = Optional.of(coordinates);
		return this.self();
	}
}
