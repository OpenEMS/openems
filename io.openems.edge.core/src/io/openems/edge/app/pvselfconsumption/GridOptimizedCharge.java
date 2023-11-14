package io.openems.edge.app.pvselfconsumption;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.pvselfconsumption.GridOptimizedCharge.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractEnumOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.enums.InputType;

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
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.PvSelfConsumption.GridOptimizedCharge")
public class GridOptimizedCharge extends AbstractEnumOpenemsApp<Property> implements OpenemsApp {

	public static enum Property implements Nameable {
		// Component-IDs
		CTRL_GRID_OPTIMIZED_CHARGE_ID,
		// Properties
		ALIAS, //
		SELL_TO_GRID_LIMIT_ENABLED, //
		MAXIMUM_SELL_TO_GRID_POWER, //
		MODE, //
		;
	}

	@Activate
	public GridOptimizedCharge(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {

			final var ctrlGridOptimizedChargeId = this.getValueOrDefault(p, Property.CTRL_GRID_OPTIMIZED_CHARGE_ID,
					"ctrlGridOptimizedCharge0");

			final var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));
			final var sellToGridLimitEnabled = EnumUtils.getAsOptionalBoolean(p, Property.SELL_TO_GRID_LIMIT_ENABLED)
					.orElse(true);
			final var mode = EnumUtils.getAsOptionalString(p, Property.MODE)
					.orElse(sellToGridLimitEnabled ? "AUTOMATIC" : "OFF");

			final int maximumSellToGridPower;
			if (sellToGridLimitEnabled) {
				maximumSellToGridPower = EnumUtils.getAsInt(p, Property.MAXIMUM_SELL_TO_GRID_POWER);
			} else {
				maximumSellToGridPower = 0;
			}

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlGridOptimizedChargeId, alias, "Controller.Ess.GridOptimizedCharge",
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.onlyIf(t == ConfigurationTarget.ADD, //
											j -> j.addProperty("ess.id", "ess0") //
													.addProperty("meter.id", "meter0"))
									.addProperty("sellToGridLimitEnabled", sellToGridLimitEnabled) //
									// always set the maximumSellToGridPower value
									.addProperty("maximumSellToGridPower", maximumSellToGridPower) //
									.onlyIf(t != ConfigurationTarget.VALIDATE, j -> j.addProperty("mode", mode))//
									.build()) //
			);

			var schedulerExecutionOrder = Lists.newArrayList("ctrlGridOptimizedCharge0", "ctrlEssSurplusFeedToGrid0");

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.scheduler(schedulerExecutionOrder)) //
					.build();
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildCheckbox(Property.SELL_TO_GRID_LIMIT_ENABLED) //
								.setLabel(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".sellToGridLimitEnabled.label")) //
								.build())
						.add(JsonFormlyUtil.buildInput(Property.MAXIMUM_SELL_TO_GRID_POWER) //
								.setInputType(InputType.NUMBER) //
								.isRequired(true) //
								.setMin(0) //
								.onlyShowIf(Exp.currentModelValue(Property.SELL_TO_GRID_LIMIT_ENABLED).notNull())
								.setLabel(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".maximumSellToGridPower.label")) //
								.build())
						.add(JsonFormlyUtil.buildSelect(Property.MODE) //
								.setLabel(TranslationUtil.getTranslation(bundle, this.getAppId() + ".mode.label")) //
								.setOptions(getModeOptions(bundle)) //
								.setDefaultValue("AUTOMATIC") //
								.build())
						.build())
				.build();
	}

	@SuppressWarnings("unchecked")
	private static final Set<Entry<String, String>> getModeOptions(ResourceBundle bundle) {
		return Sets.newHashSet(//
				Map.entry(TranslationUtil.getTranslation(bundle, "App.PvSelfConsumption.GridOptimizedCharge.mode.off"),
						"OFF"), //
				Map.entry(TranslationUtil.getTranslation(bundle,
						"App.PvSelfConsumption.GridOptimizedCharge.mode.automatic"), "AUTOMATIC"), //
				Map.entry(
						TranslationUtil.getTranslation(bundle, "App.PvSelfConsumption.GridOptimizedCharge.mode.manual"),
						"MANUAL") //
		);
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/produkte/fems/fems-app-netzdienliche-beladung/") //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
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
