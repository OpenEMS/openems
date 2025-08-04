package io.openems.edge.core.meta;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.CurrencyConfig;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.meta.types.SubdivisionCode;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	public static class Builder {

		private CurrencyConfig currency;
		private GridFeedInLimitationType gridFeedInLimitationType = GridFeedInLimitationType.NO_LIMITATION;
		private boolean isEssChargeFromGridAllowed;
		private int maximumGridFeedInLimit = 0;
		private int gridConnectionPointFuseLimit;
		private SubdivisionCode subdivisionCode = SubdivisionCode.UNDEFINED;
		private String placeName = "";
		private String postcode = "";
		private double latitude = -999.0;
		private double longitude = -999.0;
		private String timezone = "";

		private Builder() {
		}

		public Builder setCurrency(CurrencyConfig currency) {
			this.currency = currency;
			return this;
		}

		public Builder setIsEssChargeFromGridAllowed(boolean isEssChargeFromGridAllowed) {
			this.isEssChargeFromGridAllowed = isEssChargeFromGridAllowed;
			return this;
		}

		public Builder setGridConnectionPointFuseLimit(int gridConnectionPointFuseLimit) {
			this.gridConnectionPointFuseLimit = gridConnectionPointFuseLimit;
			return this;
		}

		public Builder setGridFeedInLimitationType(GridFeedInLimitationType gridFeedInLimitationType) {
			this.gridFeedInLimitationType = gridFeedInLimitationType;
			return this;
		}

		public Builder setSubdivisionCode(SubdivisionCode subdivisionCode) {
			this.subdivisionCode = subdivisionCode;
			return this;
		}

		public Builder setPlaceName(String placeName) {
			this.placeName = placeName;
			return this;
		}

		public Builder setPostcode(String postcode) {
			this.postcode = postcode;
			return this;
		}

		public Builder setLatitude(double latitude) {
			this.latitude = latitude;
			return this;
		}

		public Builder setLongitude(double longitude) {
			this.longitude = longitude;
			return this;
		}

		public Builder setTimezone(String timezone) {
			this.timezone = timezone;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	/**
	 * Create a Config builder.
	 *
	 * @return a {@link Builder}
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	private MyConfig(Builder builder) {
		super(Config.class, Meta.SINGLETON_COMPONENT_ID);
		this.builder = builder;
	}

	@Override
	public CurrencyConfig currency() {
		return this.builder.currency;
	}

	@Override
	public GridFeedInLimitationType gridFeedInLimitationType() {
		return this.builder.gridFeedInLimitationType;
	}

	@Override
	public boolean isEssChargeFromGridAllowed() {
		return this.builder.isEssChargeFromGridAllowed;
	}

	@Override
	public int gridConnectionPointFuseLimit() {
		return this.builder.gridConnectionPointFuseLimit;
	}

	@Override
	public int maximumGridFeedInLimit() {
		return this.builder.maximumGridFeedInLimit;
	}

	@Override
	public SubdivisionCode subdivisionCode() {
		return this.builder.subdivisionCode;
	}

	@Override
	public String placeName() {
		return this.builder.placeName;
	}

	@Override
	public String postcode() {
		return this.builder.postcode;
	}

	@Override
	public double latitude() {
		return this.builder.latitude;
	}

	@Override
	public double longitude() {
		return this.builder.longitude;
	}

	@Override
	public String timezone() {
		return this.builder.timezone;
	}
}
