package io.openems.edge.app.evcs;

import java.util.List;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.google.common.collect.Lists;

import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractEnumOpenemsApp;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsAppCategory;

public abstract class AbstractEvcsApp<PROPERTY extends Enum<PROPERTY> & Nameable>
		extends AbstractEnumOpenemsApp<PROPERTY> {

	protected AbstractEvcsApp(ComponentManager componentManager, ComponentContext componentContext,
			ConfigurationAdmin cm, ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	public final OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.EVCS };
	}

	protected final List<EdgeConfig.Component> getComponents(String evcsId, String alias, String factorieId, String ip,
			String ctrlEvcsId) {
		return Lists.newArrayList(//
				new EdgeConfig.Component(evcsId, alias, factorieId, JsonUtils.buildJsonObject() //
						.addPropertyIfNotNull("ip", ip) //
						.build()), //
				new EdgeConfig.Component(ctrlEvcsId, "Data", "Controller.Evcs", JsonUtils.buildJsonObject() //
						.addProperty("evcs.id", evcsId) //
						.build())//
		);
	}

}
