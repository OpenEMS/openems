package io.openems.edge.app.ess;

import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.ess.PrepareBatteryExtension.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

/**
 * Describes a prepare battery extension app.
 *
 * <pre>
  {
    "appId":"App.Ess.PrepareBatteryExtension",
    "alias":"Batterie Nachrüstung",
    "instanceId": UUID,
    "image": base64,
    "properties":{
      "CTRL_PREPARE_BATTERY_EXTENSION_ID": "ctrlPrepareBatteryExtension0",
      "TARGET_SOC": 30
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.Ess.PrepareBatteryExtension")
public class PrepareBatteryExtension
		extends AbstractOpenemsAppWithProps<PrepareBatteryExtension, Property, Parameter.BundleParameter>
		implements OpenemsApp {

	public enum Property implements Type<Property, PrepareBatteryExtension, Parameter.BundleParameter>, Nameable {
		// Components
		CTRL_PREPARE_BATTERY_EXTENSION_ID(AppDef.of(PrepareBatteryExtension.class) //
				.setDefaultValue("ctrlPrepareBatteryExtension0")), //

		// Properties
		ALIAS(AppDef.of(PrepareBatteryExtension.class) //
				.setDefaultValueToAppName()), //
		TARGET_SOC(AppDef.of(PrepareBatteryExtension.class) //
				.setTranslatedLabelWithAppPrefix(".targetSoc.label") //
				.setDefaultValue(30) //
				.setField(JsonFormlyUtil::buildRange, //
						(app, prop, l, param, f) -> f.isRequired(true) //
								.setMin(0) //
								.setMax(100))), //
		;

		private final AppDef<PrepareBatteryExtension, Property, BundleParameter> def;

		private Property(AppDef<PrepareBatteryExtension, Property, BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Property self() {
			return this;
		}

		@Override
		public AppDef<PrepareBatteryExtension, Property, BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<PrepareBatteryExtension>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public PrepareBatteryExtension(//
			@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.ESS };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var ctrlPrepareBatteryExtensionId = this.getId(t, p, Property.CTRL_PREPARE_BATTERY_EXTENSION_ID);

			final var alias = this.getString(p, l, Property.ALIAS);
			final var targetSoc = this.getInt(p, Property.TARGET_SOC);

			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlPrepareBatteryExtensionId, alias,
							"Controller.Ess.PrepareBatteryExtension", //
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("targetSoc", targetSoc) //
									.onlyIf(t == ConfigurationTarget.ADD, //
											b -> b.addProperty("enabled", true) //
													.addProperty("ess_id", "ess0") //
													.addProperty("isRunning", false) //
													.addProperty("targetTimeSpecified", false) //
													.addProperty("targetTimeBuffer", 30) //
													.addProperty("selfTermination", true) //
													.addProperty("terminationBuffer", 120) //
													.addProperty("conditionalTermination", true) //
													.addProperty("endCondition", "CAPACITY_CHANGED"))
									.build()) //
			);

			final var schedulerIds = Lists.newArrayList(//
					ctrlPrepareBatteryExtensionId, //
					"ctrlEmergencyCapacityReserve0", //
					"ctrlGridOptimizedCharge0", //
					"ctrlEssSurplusFeedToGrid0", //
					"ctrlBalancing0" //
			);

			return new AppConfiguration(components, schedulerIds);
		};
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create() //
				.setCanSee(Role.ADMIN) //
				.build();
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	protected PrepareBatteryExtension getApp() {
		return this;
	}
}
