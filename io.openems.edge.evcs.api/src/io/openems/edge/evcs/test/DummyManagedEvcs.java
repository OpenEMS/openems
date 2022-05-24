package io.openems.edge.evcs.test;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;

public class DummyManagedEvcs extends AbstractOpenemsComponent implements Evcs, ManagedEvcs, OpenemsComponent {

	private final EvcsPower evcsPower;

	/**
	 * Constructor.
	 *
	 * @param id id
	 */
	public DummyManagedEvcs(String id, EvcsPower evcsPower) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				Evcs.ChannelId.values() //
		);
		this.evcsPower = evcsPower;
		this.setPriority(false);
		this._setChargePower(0);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}


	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	public void setPriority(boolean priority) {
		this._setIsPriority(priority);
	}

	@Override
	public int[] getPhaseConfiguration() {
		return new int[]{1, 2, 3};
	}

	public void setChargePower(int value) {
		this._setChargePower(value);
	}

	public void setMinimumPower(int value) {
		this._setMinimumPower(value);
	}

	public void setMinimumHardwarePower(int value) {
		this._setMinimumHardwarePower(value);
	}
}