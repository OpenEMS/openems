package io.openems.edge.common.test;

import java.time.ZoneId;

import io.openems.common.channel.AccessMode;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.currency.Currency;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.meta.types.Coordinates;
import io.openems.edge.common.meta.types.SubdivisionCode;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;

public class DummyMeta extends AbstractDummyOpenemsComponent<DummyMeta> implements Meta {

	private final OpenemsEdgeOem oem = new DummyOpenemsEdgeOem();

	private int gridConnectionPointFuseLimit; // [A]
	private SubdivisionCode subdivisionCode;
	private String placeName;
	private String postcode;
	private Coordinates coordinates;
	private ZoneId timezone;

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

	@Override
	public int getGridConnectionPointFuseLimit() {
		return this.gridConnectionPointFuseLimit;
	}

	@Override
	public SubdivisionCode getSubdivisionCode() {
		return this.subdivisionCode;
	}

	@Override
	public String getPlaceName() {
		return this.placeName;
	}

	@Override
	public String getPostcode() {
		return this.postcode;
	}

	@Override
	public Coordinates getCoordinates() {
		return this.coordinates;
	}

	@Override
	public ZoneId getTimezone() {
		return this.timezone;
	}

	/**
	 * Set the Grid-Connection-Point Fuse limit.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyMeta withGridConnectionPointFuseLimit(int value) {
		this.gridConnectionPointFuseLimit = value;
		return this.self();
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

	/**
	 * Sets the subdivision code for this {@link DummyMeta} instance and returns the
	 * instance itself.
	 *
	 * @param subdivisionCode the subdivision code
	 * @return myself
	 */
	public DummyMeta withSubdivisionCode(SubdivisionCode subdivisionCode) {
		this.subdivisionCode = subdivisionCode;
		return this.self();
	}

	/**
	 * Sets the place name for this {@link DummyMeta} instance and returns the
	 * instance itself.
	 *
	 * @param placeName the place name
	 * @return myself
	 */
	public DummyMeta withPlaceName(String placeName) {
		this.placeName = placeName;
		return this.self();
	}

	/**
	 * Sets the postcode for this {@link DummyMeta} instance and returns the
	 * instance itself.
	 *
	 * @param postcode the postcode
	 * @return myself
	 */
	public DummyMeta withPostcode(String postcode) {
		this.postcode = postcode;
		return this.self();
	}

	/**
	 * Sets the coordinates for this {@link DummyMeta} instance and returns the
	 * instance itself.
	 *
	 * @param coordinates the coordinates
	 * @return the current {@link DummyMeta} instance with updated coordinates
	 */
	public DummyMeta withCoordinates(Coordinates coordinates) {
		this.coordinates = coordinates;
		return this.self();
	}

	/**
	 * Sets the time zone for this {@link DummyMeta} instance and returns the
	 * instance itself.
	 *
	 * @param timezone the timezone
	 * @return myself
	 */
	public DummyMeta withTimezone(ZoneId timezone) {
		this.timezone = timezone;
		return this.self();
	}
}
