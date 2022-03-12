package io.openems.edge.ess.test;

import java.util.Arrays;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

/**
 * Provides a simple, simulated MetaEss (i.e. EssCluster) component that can be
 * used together with the OpenEMS Component test framework.
 */
public class DummyMetaEss extends AbstractOpenemsComponent
		implements ManagedAsymmetricEss, SymmetricEss, OpenemsComponent, MetaEss {

	public static final int MAX_APPARENT_POWER = Integer.MAX_VALUE;

	private final String[] essIds;
	private final Power power;

	public DummyMetaEss(String id, Power power, SymmetricEss... esss) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ManagedAsymmetricEss.ChannelId.values(), //
				SymmetricEss.ChannelId.values() //
		);
		this.power = power;
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
		// Add all ManagedSymmetricEss devices to this.essIds
		this.essIds = Arrays.stream(esss).map(SymmetricEss::id).toArray(String[]::new);
	}

	public DummyMetaEss(String id, SymmetricEss... esss) {
		this(id, new DummyPower(MAX_APPARENT_POWER), esss);
	}

	@Override
	public String[] getEssIds() {
		return this.essIds;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
	}

	@Override
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) {
		throw new IllegalArgumentException("DummyMetaEss.applyPower() should never be called.");
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

}
