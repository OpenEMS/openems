package io.openems.edge.app.meter;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.google.gson.JsonArray;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.OpenemsAppCategory;

public abstract class AbstractMeterApp<PROPERTY extends Enum<PROPERTY>> extends AbstractOpenemsApp<PROPERTY> {

	protected AbstractMeterApp(ComponentManager componentManager, ComponentContext componentContext,
			ConfigurationAdmin cm, ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	public final OpenemsAppCategory[] getCategorys() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.METER };
	}

	protected final JsonArray buildMeterOptions() {
		return JsonUtils.buildJsonArray() //
				.add(JsonUtils.buildJsonObject() //
						.addProperty("label", "Erzeugung/Production") //
						.addProperty("value", "PRODUCTION") //
						.build())
				.add(JsonUtils.buildJsonObject() //
						.addProperty("label", "Netzzähler/Grid-Meter") //
						.addProperty("value", "GRID") //
						.build())
				.add(JsonUtils.buildJsonObject() //
						.addProperty("label", "Verbrauchszähler/Consumption-Meter") //
						.addProperty("value", "CONSUMPTION_METERED") //
						.build())
				.build();
	}

}
