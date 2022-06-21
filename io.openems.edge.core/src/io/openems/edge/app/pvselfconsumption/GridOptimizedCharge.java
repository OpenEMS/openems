package io.openems.edge.app.pvselfconsumption;

import java.util.EnumMap;
import java.util.List;

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
    	"SELL_TO_GRID_LIMIT_ENABLED": true,
    	"CTRL_GRID_OPTIMIZED_CHARGE_ID": "ctrlGridOptimizedCharge0",
    	"MAXIMUM_SELL_TO_GRID_POWER": 10000
    },
    "appDescriptor": {
    	"websiteUrl": URL
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.PvSelfConsumption.GridOptimizedCharge")
public class GridOptimizedCharge extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property {
		// User values
		ALIAS, //
		SELL_TO_GRID_LIMIT_ENABLED, //
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

			final var sellToGridLimitEnabled = EnumUtils.getAsOptionalBoolean(p, Property.SELL_TO_GRID_LIMIT_ENABLED)
					.orElse(true);

			final int maximumSellToGridPower;
			if (sellToGridLimitEnabled) {
				maximumSellToGridPower = EnumUtils.getAsInt(p, Property.MAXIMUM_SELL_TO_GRID_POWER);
			} else {
				maximumSellToGridPower = 0;
			}

			List<Component> comp = Lists.newArrayList(new EdgeConfig.Component(ctrlIoFixDigitalOutputId, alias,
					"Controller.Ess.GridOptimizedCharge", JsonUtils.buildJsonObject() //
							.addProperty("enabled", true) //
							.onlyIf(t == ConfigurationTarget.ADD, //
									j -> j.addProperty("ess.id", "ess0") //
											.addProperty("meter.id", "meter0"))
							.addProperty("sellToGridLimitEnabled", sellToGridLimitEnabled) //
							.onlyIf(sellToGridLimitEnabled,
									o -> o.addProperty("maximumSellToGridPower", maximumSellToGridPower)) //
							.build()));//

			var schedulerExecutionOrder = Lists.newArrayList("ctrlGridOptimizedCharge0", "ctrlEssSurplusFeedToGrid0");

			return new AppConfiguration(comp, schedulerExecutionOrder);
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildCheckbox(Property.SELL_TO_GRID_LIMIT_ENABLED) //
								.setLabel(bundle.getString(this.getAppId() + ".sellToGridLimitEnabled.label")) //
								.setDescription(
										bundle.getString(this.getAppId() + ".sellToGridLimitEnabled.description")) //
								.build())
						.add(JsonFormlyUtil.buildInput(Property.MAXIMUM_SELL_TO_GRID_POWER) //
								.setInputType(Type.NUMBER) //
								.isRequired(true) //
								.setMin(0) //
								.onlyShowIfChecked(Property.SELL_TO_GRID_LIMIT_ENABLED) //
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
				.build();
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

}
