package io.openems.edge.controller.ess.ripplecontrolreceiver.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.controller.ess.ripplecontrolreceiver.EssRestrictionLevel;
import io.openems.edge.controller.ess.ripplecontrolreceiver.PowerProductionLimiterComponent;

import java.util.function.Supplier;

public class DummyPowerProductionLimiter extends AbstractDummyOpenemsComponent<DummyPowerProductionLimiter>
		implements PowerProductionLimiterComponent {

	private int maxNominalProductionPowerInW;
	private Supplier<Integer> gridFeedInLimitFunc;

	public DummyPowerProductionLimiter(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				PowerProductionLimiterComponent.ChannelId.values() //
		);
	}

	@Override
	protected DummyPowerProductionLimiter self() {
		return this;
	}

	@Override
	public void setMaxNominalProductionPower(int maxNominalProductionPowerInW) {
		this.maxNominalProductionPowerInW = maxNominalProductionPowerInW;
	}

	public DummyPowerProductionLimiter withGridFeedInLimit(Integer gridFeedInLimit) {
		if (gridFeedInLimit == null) {
			this.gridFeedInLimitFunc = null;
		} else {
			this.gridFeedInLimitFunc = () -> gridFeedInLimit;
		}
		return this.self();
	}

	public DummyPowerProductionLimiter withRestrictionLevel(EssRestrictionLevel restrictionLevel) {
		if (restrictionLevel == null || restrictionLevel == EssRestrictionLevel.NO_RESTRICTION) {
			this.gridFeedInLimitFunc = null;
		} else {
			this.gridFeedInLimitFunc = () -> (int) (this.maxNominalProductionPowerInW * restrictionLevel.getLimitationFactor());
		}
		return this.self();
	}

	@Override
	public Integer getGridFeedInLimit() {
		return this.gridFeedInLimitFunc.get();
	}
}
