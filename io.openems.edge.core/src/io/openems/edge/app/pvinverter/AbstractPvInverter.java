package io.openems.edge.app.pvinverter;

import java.util.List;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.google.common.collect.Lists;

import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.OpenemsAppCategory;

public abstract class AbstractPvInverter<PROPERTY extends Enum<PROPERTY>> extends AbstractOpenemsApp<PROPERTY> {

	protected AbstractPvInverter(ComponentManager componentManager, ComponentContext componentContext,
			ConfigurationAdmin cm, ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	public OpenemsAppCategory[] getCategorys() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.PV_INVERTER };
	}

	protected final List<Component> getComponents(String factoryId, String pvInverterId, //
			String modbusId, String alias, String ip, int port) {

		return Lists.newArrayList(//
				new EdgeConfig.Component(pvInverterId, alias, factoryId, //
						JsonUtils.buildJsonObject() //
								.addProperty("modbus.id", modbusId) //
								.build()), //
				new EdgeConfig.Component(modbusId, alias, "Bridge.Modbus.Tcp", JsonUtils.buildJsonObject() //
						.addProperty("ip", ip) //
						.addProperty("port", port) //
						.build())//
		);
	}

}
