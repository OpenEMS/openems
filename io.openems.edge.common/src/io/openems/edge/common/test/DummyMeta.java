package io.openems.edge.common.test;

import static io.openems.edge.common.test.TestUtils.withValue;

import java.time.ZoneId;
import java.util.function.Consumer;

import io.openems.common.channel.AccessMode;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.utils.IntUtils;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.currency.Currency;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.meta.ThirdPartyUsageAcceptance;
import io.openems.edge.common.meta.types.Coordinates;
import io.openems.edge.common.meta.types.Environment;
import io.openems.edge.common.meta.types.GridLimit;
import io.openems.edge.common.meta.types.SubdivisionCode;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;

public class DummyMeta extends AbstractDummyOpenemsComponent<DummyMeta> implements Meta {

	private final OpenemsEdgeOem oem = new DummyOpenemsEdgeOem();
	private final DummyGridLimit gridLimit;

	private SubdivisionCode subdivisionCode;
	private String placeName;
	private String postcode;
	private Coordinates coordinates;
	private ZoneId timezone;
	private ThirdPartyUsageAcceptance thirdPartyUsageAcceptance;
	private Environment environment = Environment.STABLE;

	public DummyMeta() {
		super(Meta.SINGLETON_COMPONENT_ID, Meta.SINGLETON_SERVICE_PID, //
				OpenemsComponent.ChannelId.values(), //
				Meta.ChannelId.values());
		this.gridLimit = new DummyGridLimit(this);
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

	@Override
	public GridLimit.Immutable getGridLimit() {
		return this.gridLimit.toImmutable();
	}

	@Override
	public Environment getEnvironment() {
		return this.environment;
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

	/**
	 * Sets the {@link ThirdPartyUsageAcceptance} for this {@link DummyMeta}
	 * instance and returns the instance itself.
	 *
	 * @param thirdPartyUsageAcceptance the acceptance status of third-party usage
	 * @return myself
	 */
	public DummyMeta withThirdPartyUsageAcceptance(ThirdPartyUsageAcceptance thirdPartyUsageAcceptance) {
		this.thirdPartyUsageAcceptance = thirdPartyUsageAcceptance;
		return this.self();
	}

	/**
	 * Sets the {@link Environment} for this {@link DummyMeta} instance and returns
	 * the instance itself.
	 *
	 * @param environment the environment
	 * @return myself
	 */
	public DummyMeta withEnvironment(Environment environment) {
		this.environment = environment;
		return this.self();
	}

	@Override
	public ThirdPartyUsageAcceptance getThirdPartyUsageAcceptance() {
		return this.thirdPartyUsageAcceptance;
	}

	/**
	 * Update the {@link GridLimit}.
	 *
	 * @param callback callback for {@link DummyGridLimit}
	 * @return myself
	 */
	public DummyMeta withGridLimit(Consumer<DummyGridLimit> callback) {
		callback.accept(this.gridLimit);
		return this.self();
	}

	public static class DummyGridLimit extends GridLimit.Mutable {

		private final DummyMeta parent;

		// Buy
		private Integer gridBuyHardLimit;
		private Integer gridBuyHardLimitWithBuffer;

		// Sell
		private Integer gridSellHardLimit;
		private Integer gridSellHardLimitWithBuffer;

		private DummyGridLimit(DummyMeta parent) {
			this.parent = parent;
		}

		public DummyGridLimit setGridBuyHardLimit(Integer gridBuyHardLimit) {
			this.gridBuyHardLimit = gridBuyHardLimit;
			this.invalidateImmutable();
			return this;
		}

		@Override
		public synchronized int gridBuyHardLimit() {
			return IntUtils.minInt(super.gridBuyHardLimit(), this.gridBuyHardLimit);
		}

		public DummyGridLimit setGridBuyHardLimitWithBuffer(Integer gridBuyHardLimitWithBuffer) {
			this.gridBuyHardLimitWithBuffer = gridBuyHardLimitWithBuffer;
			this.invalidateImmutable();
			return this;
		}

		@Override
		public synchronized int gridBuyHardLimitWithBuffer() {
			return IntUtils.minInt(this.gridBuyHardLimit(), super.gridBuyHardLimitWithBuffer(),
					this.gridBuyHardLimitWithBuffer);
		}

		public DummyGridLimit setGridSellHardLimit(int gridSellHardLimit) {
			this.gridSellHardLimit = gridSellHardLimit;
			this.invalidateImmutable();
			return this;
		}

		@Override
		public synchronized int gridSellHardLimit() {
			return IntUtils.minInt(super.gridSellHardLimit(), this.gridSellHardLimit);
		}

		public DummyGridLimit setGridSellHardLimitWithBuffer(Integer gridSellHardLimitWithBuffer) {
			this.gridSellHardLimitWithBuffer = gridSellHardLimitWithBuffer;
			this.invalidateImmutable();
			return this;
		}

		@Override
		public synchronized int gridSellHardLimitWithBuffer() {
			return IntUtils.minInt(this.gridSellHardLimit(), super.gridSellHardLimitWithBuffer(),
					this.gridSellHardLimitWithBuffer);
		}

		@Override
		public synchronized Mutable setEssChargeFromGridAllowed(boolean isEssChargeFromGridAllowed) {
			withValue(this.parent, Meta.ChannelId.IS_ESS_CHARGE_FROM_GRID_ALLOWED, isEssChargeFromGridAllowed);
			return super.setEssChargeFromGridAllowed(isEssChargeFromGridAllowed);
		}

		@Override
		public synchronized Mutable setEssDischargeToGridAllowed(boolean isEssDischargeToGridAllowed) {
			withValue(this.parent, Meta.ChannelId.IS_ESS_DISCHARGE_TO_GRID_ALLOWED, isEssDischargeToGridAllowed);
			return super.setEssDischargeToGridAllowed(isEssDischargeToGridAllowed);
		}
	}
}
