package io.openems.edge.common.test;

import java.time.ZoneId;
import java.util.Optional;
import java.util.function.Consumer;

import io.openems.common.channel.AccessMode;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.currency.Currency;
import io.openems.edge.common.meta.GridBuyLimit;
import io.openems.edge.common.meta.GridFeedInLimitationType;
import io.openems.edge.common.meta.GridSellLimit;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.meta.ThirdPartyUsageAcceptance;
import io.openems.edge.common.meta.types.Coordinates;
import io.openems.edge.common.meta.types.Environment;
import io.openems.edge.common.meta.types.SubdivisionCode;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;

public class DummyMeta extends AbstractDummyOpenemsComponent<DummyMeta> implements Meta {

	private final OpenemsEdgeOem oem = new DummyOpenemsEdgeOem();

	private int gridConnectionPointFuseLimit = 32; // [A]
	private SubdivisionCode subdivisionCode;
	private String placeName;
	private String postcode;
	private Coordinates coordinates;
	private ZoneId timezone;
	private int gridSellHardLimit;
	private int gridSellHardLimitWithBuffer;
	private GridBuyLimit gridBuyLimit = new GridBuyLimit(//
			new GridBuyLimit.Hard(0), //
			JSCalendar.Tasks.empty());
	private ThirdPartyUsageAcceptance thirdPartyUsageAcceptance;
	private Environment environment = Environment.STABLE;
	private Optional<Integer> configuredMaximumGridFeedInLimit = Optional.empty();

	public DummyMeta() {
		super(Meta.SINGLETON_COMPONENT_ID, Meta.SINGLETON_SERVICE_PID, //
				OpenemsComponent.ChannelId.values(), //
				Meta.ChannelId.values());
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

	@Override
	public int getGridSellHardLimit() {
		return this.gridSellHardLimit;
	}

	@Override
	public int getGridSellHardLimitWithBuffer() {
		return this.gridSellHardLimitWithBuffer;
	}

	@Override
	public GridSellLimit getGridSellLimit() {
		return new GridSellLimit(new GridSellLimit.Hard(//
				this.gridSellHardLimit, //
				this.gridSellHardLimitWithBuffer));
	}

	@Override
	public int getEssDischargeToGridLimit() {
		if (this.getIsEssDischargeToGridAllowed()) {
			return this.getGridSellHardLimit();
		}
		return 0;
	}

	@Override
	public GridBuyLimit getGridBuyLimit() {
		return this.gridBuyLimit;
	}

	@Override
	public Environment getEnvironment() {
		return this.environment;
	}

	/**
	 * Set the Grid-Connection-Point Fuse limit in [A].
	 *
	 * @param value the value in Ampere
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

	/**
	 * Sets the Grid-Sell Hard-Limit for this {@link DummyMeta} instance and returns
	 * the instance itself.
	 *
	 * @param gridSellHardLimit the value
	 * @return myself
	 */
	public DummyMeta withGridSellHardLimit(int gridSellHardLimit) {
		this.gridSellHardLimit = gridSellHardLimit;
		return this.self();
	}

	@Override
	public Optional<Integer> getConfiguredMaximumGridFeedInLimit() {
		return this.getGridFeedInLimitationType() == GridFeedInLimitationType.DYNAMIC_LIMITATION //
				? this.configuredMaximumGridFeedInLimit.filter(limit -> limit >= 0) //
				: Optional.empty();
	}

	/**
	 * Sets the Grid-Sell Hard-Limit with safety buffer for this {@link DummyMeta}
	 * instance and returns the instance itself.
	 *
	 * @param gridSellHardLimitWithBuffer the value
	 * @return myself
	 */
	public DummyMeta withGridSellHardLimitWithBuffer(int gridSellHardLimitWithBuffer) {
		this.gridSellHardLimitWithBuffer = gridSellHardLimitWithBuffer;
		return this.self();
	}

	/**
	 * Set {@link Meta.ChannelId#IS_ESS_DISCHARGE_TO_GRID_ALLOWED}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyMeta withIsEssDischargeToGridAllowed(boolean value) {
		TestUtils.withValue(this, Meta.ChannelId.IS_ESS_DISCHARGE_TO_GRID_ALLOWED, value);
		return this.self();
	}

	/**
	 * Sets the {@link GridBuyLimit} for this {@link DummyMeta} instance and returns
	 * the instance itself.
	 *
	 * @param gridBuyLimit the grid-buy limit
	 * @return myself
	 */
	public DummyMeta withGridBuyLimit(GridBuyLimit gridBuyLimit) {
		this.gridBuyLimit = gridBuyLimit;
		return this.self();
	}

	/**
	 * Sets the {@link GridBuyLimit} for this {@link DummyMeta} instance and returns
	 * the instance itself.
	 *
	 * @param callback callback with {@link GridBuyLimit.Builder}
	 * @return myself
	 */
	public DummyMeta withGridBuyLimit(Consumer<GridBuyLimit.Builder> callback) {
		var b = GridBuyLimit.create();
		callback.accept(b);
		this.gridBuyLimit = b.build();
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
	 * Set {@link Meta.ChannelId#GRID_FEED_IN_LIMITATION_TYPE}.
	 *
	 * @param gridFeedInLimitationType grid feed in limitation type
	 * @return myself
	 */
	public DummyMeta withGridFeedInLimitationType(GridFeedInLimitationType gridFeedInLimitationType) {
		TestUtils.withValue(this, Meta.ChannelId.GRID_FEED_IN_LIMITATION_TYPE, gridFeedInLimitationType);
		return this.self();
	}

	/**
	 * Sets the directly configured dynamic Grid-Feed-In limit for a sum of all
	 * three phases for this {@link DummyMeta} instance and returns the instance
	 * itself.
	 *
	 * @param configuredMaximumGridFeedInLimit the value in [W]
	 * @return myself
	 */
	public DummyMeta withConfiguredMaximumGridFeedInLimit(int configuredMaximumGridFeedInLimit) {
		this.configuredMaximumGridFeedInLimit = Optional.of(configuredMaximumGridFeedInLimit);
		return this.self();
	}
}
