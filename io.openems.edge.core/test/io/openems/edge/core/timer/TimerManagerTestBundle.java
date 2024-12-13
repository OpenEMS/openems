package io.openems.edge.core.timer;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class TimerManagerTestBundle {
    public final DummyConfigurationAdmin cm;
    public final DummyComponentManager componentManger;
    public final TimerManagerImpl tm;

    public TimerManagerTestBundle() throws Exception {

	this.cm = new DummyConfigurationAdmin();
	this.cm.getOrCreateEmptyConfiguration(TimerManager.SINGLETON_SERVICE_PID);

	this.componentManger = new DummyComponentManager();
	JsonObject initialComponentConfig = JsonUtils.buildJsonObject() //
		.add(TimerManager.SINGLETON_COMPONENT_ID, JsonUtils.buildJsonObject() //
			.addProperty("factoryId", TimerManager.SINGLETON_SERVICE_PID) //
			.addProperty("alias", "") //
			.add("properties", JsonUtils.buildJsonObject() //
				.addProperty("no", "{}") //
				.build()) //
			.build()) //
		.build();
	this.componentManger.setConfigJson(JsonUtils.buildJsonObject() //
		.add("components", initialComponentConfig) //
		.add("factories", JsonUtils.buildJsonObject() //
			.build()) //
		.build() //
	);

	this.tm = new TimerManagerImpl();
	this.componentManger.addComponent(this.tm);
	this.componentManger.setConfigurationAdmin(this.cm);

	MyConfig config = MyConfig.create().build();

	new ComponentTest(this.tm) //
		.addReference("cm", this.cm) //
		.addReference("componentManager", this.componentManger) //
		.activate(config);
    }

    protected TimerManagerImpl getTimerManager() {
	return this.tm;
    }

}
