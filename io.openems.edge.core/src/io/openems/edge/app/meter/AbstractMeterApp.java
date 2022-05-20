package io.openems.edge.app.meter;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.google.gson.JsonArray;

import io.openems.common.session.Language;
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

	protected final JsonArray buildMeterOptions(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return JsonUtils.buildJsonArray() //
				.add(JsonUtils.buildJsonObject() //
						.addProperty("label", bundle.getString("App.Meter.production")) //
						.addProperty("value", "PRODUCTION") //
						.build())
				.add(JsonUtils.buildJsonObject() //
						.addProperty("label", bundle.getString("App.Meter.gridMeter")) //
						.addProperty("value", "GRID") //
						.build())
				.add(JsonUtils.buildJsonObject() //
						.addProperty("label", bundle.getString("App.Meter.consumtionMeter")) //
						.addProperty("value", "CONSUMPTION_METERED") //
						.build())
				.build();
	}

}
