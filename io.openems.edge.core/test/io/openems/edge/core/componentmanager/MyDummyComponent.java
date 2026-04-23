package io.openems.edge.core.componentmanager;

import static java.util.UUID.randomUUID;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.common.test.DummyComponentContext;
import io.openems.edge.controller.api.Controller;

public class MyDummyComponent extends AbstractDummyOpenemsComponent<MyDummyComponent> implements OpenemsComponent {

	private String id;

	public MyDummyComponent(String id) {
		super(id, id, //
				new DummyComponentContext() //
						.addProperty("service.factoryPid", "My.Dummy.Component") //
						.addProperty("service.pid", "My.Dummy.Component." + randomUUID()), //
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values());
		this.id = id;
	}

	@Override
	protected MyDummyComponent self() {
		return this;
	}

	@Override
	public String id() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
