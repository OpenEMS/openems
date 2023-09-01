package io.openems.edge.app.meter;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.google.gson.JsonArray;

import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractEnumOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;

public abstract class AbstractMeterApp<PROPERTY extends Enum<PROPERTY> & Nameable>
		extends AbstractEnumOpenemsApp<PROPERTY> {

	protected AbstractMeterApp(ComponentManager componentManager, ComponentContext componentContext,
			ConfigurationAdmin cm, ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	public final OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.METER };
	}

	protected final JsonArray buildMeterOptions(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return JsonUtils.buildJsonArray() //
				.add(JsonUtils.buildJsonObject() //
						.addProperty("label", TranslationUtil.getTranslation(bundle, "App.Meter.production")) //
						.addProperty("value", "PRODUCTION") //
						.build())
				.add(JsonUtils.buildJsonObject() //
						.addProperty("label", TranslationUtil.getTranslation(bundle, "App.Meter.consumtionMeter")) //
						.addProperty("value", "CONSUMPTION_METERED") //
						.build())
				.build();
	}

}
