package io.openems.edge.controller.ess.limitdischargecellvoltage.helper;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.common.component.AbstractOpenemsComponent;

public class DummyEss extends AbstractOpenemsComponent implements ManagedSymmetricEss{

	protected DummyEss(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	public void setMinimalCellVoltage(int minimalCellVoltage) {
		this.getMinCellVoltage().setNextValue(minimalCellVoltage);
		this.getMinCellVoltage().nextProcessImage();
	}

	@Override
	public Power getPower() {
		return null;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
	}

	@Override
	public int getPowerPrecision() {
		return 0;
	}
	
	
}
