package io.openems.edge.ess.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.SymmetricEss;

public class DummySymmetricEss extends AbstractDummySymmetricEss<DummySymmetricEss> implements SymmetricEss {

	public DummySymmetricEss(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values() //
		);
	}

	@Override
	protected DummySymmetricEss self() {
		return this;
	}

}
