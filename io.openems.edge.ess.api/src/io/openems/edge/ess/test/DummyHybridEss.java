package io.openems.edge.ess.test;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

/**
 * Provides a simple, simulated {@link HybridEss} that is also a
 * {@link ManagedSymmetricEss} component and can be used together with the
 * OpenEMS Component test framework.
 */
public class DummyHybridEss extends AbstractOpenemsComponent
		implements HybridEss, ManagedSymmetricEss, SymmetricEss, OpenemsComponent {

	public static final int MAX_APPARENT_POWER = Integer.MAX_VALUE;

	private final Power power;

	private Integer surplusPower = null;

	public DummyHybridEss(String id, Power power) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				SymmetricEss.ChannelId.values() //
		);
		this.power = power;
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

	public DummyHybridEss(String id) {
		this(id, new DummyPower(MAX_APPARENT_POWER));
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	public void setDummySurplusPower(Integer surplusPower) {
		this.surplusPower = surplusPower;
	}

	@Override
	public Integer getSurplusPower() {
		return this.surplusPower;
	}

	public void setDummyMaxApparentPower(int maxApparentPower) {
		this._setMaxApparentPower(maxApparentPower);
		this.getMaxApparentPowerChannel().nextProcessImage();
		if (this.power instanceof DummyPower) {
			((DummyPower) this.power).setMaxApparentPower(maxApparentPower);
		}
	}

}
