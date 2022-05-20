package io.openems.edge.app.pvselfconsumption;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.TreeMap;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.pvselfconsumption.GridOptimizedCharge.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder.Type;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.validator.CheckHome;
import io.openems.edge.core.appmanager.validator.Validator;
import io.openems.edge.core.appmanager.validator.Validator.Builder;

/**
 * Describes a App for a Grid Optimized Charge.
 *
 * <pre>
  {
    "appId":"App.PvSelfConsumption.GridOptimizedCharge",
    "alias":"Netzdienliche Beladung",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CTRL_GRID_OPTIMIZED_CHARGE_ID": "ctrlGridOptimizedCharge0",
    	"MAXIMUM_SELL_TO_GRID_POWER": 10000
    },
    "appDescriptor": {
    	"websiteUrl": <a href=
"https://fenecon.de/fems-2-2/fems-app-netzdienliche-beladung/">https://fenecon.de/fems-2-2/fems-app-netzdienliche-beladung/</a>
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.PvSelfConsumption.GridOptimizedCharge")
public class GridOptimizedCharge extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property {
		// User values
		ALIAS, //
		MAXIMUM_SELL_TO_GRID_POWER, //
		// Components
		CTRL_GRID_OPTIMIZED_CHARGE_ID;

	}

	@Activate
	public GridOptimizedCharge(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {

			final var ctrlIoFixDigitalOutputId = this.getId(t, p, Property.CTRL_GRID_OPTIMIZED_CHARGE_ID,
					"ctrlGridOptimizedCharge0");

			final var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));

			final var maximumSellToGridPower = EnumUtils.getAsInt(p, Property.MAXIMUM_SELL_TO_GRID_POWER);

			List<Component> comp = new ArrayList<>();

			comp.add(new EdgeConfig.Component(ctrlIoFixDigitalOutputId, alias, "Controller.Ess.GridOptimizedCharge",
					JsonUtils.buildJsonObject() //
							.addProperty("enabled", true) //
							.onlyIf(t == ConfigurationTarget.ADD, //
									j -> j.addProperty("ess.id", "ess0") //
											.addProperty("meter.id", "meter0"))
							.addProperty("sellToGridLimitEnabled", true) //
							.addProperty("maximumSellToGridPower", maximumSellToGridPower) //
							.build()));//

			return new AppConfiguration(comp);
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildInput(Property.MAXIMUM_SELL_TO_GRID_POWER) //
								.setInputType(Type.NUMBER) //
								.isRequired(true) //
								.setMin(0) //
								.setLabel(bundle.getString(this.getAppId() + ".maximumSellToGridPower.label")) //
								.setDescription(
										bundle.getString(this.getAppId() + ".maximumSellToGridPower.description")) //
								.build())
						.build())
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/fems-2-2/fems-app-netzdienliche-beladung/") //
				.build();
	}

	@Override
	public Builder getValidateBuilder() {
		return Validator.create() // TODO remove later when home has dependency
				.setCompatibleCheckableConfigs(Lists.newArrayList(//
						new Validator.CheckableConfig(CheckHome.COMPONENT_NAME, true,
								new Validator.MapBuilder<>(new TreeMap<String, Object>()) //
										.build())));
	}

	@Override
	public OpenemsAppCategory[] getCategorys() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.PV_SELF_CONSUMPTION };
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	public String getImage() {
		return "data:image/png;base64,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"
				+ "AAASUVORK5CYII=";
	}

}
