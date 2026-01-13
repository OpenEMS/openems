package io.openems.edge.app.integratedsystem;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.core.appmanager.validator.Checkables.checkRelayCount;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.ComponentProps;
import io.openems.edge.app.enums.TranslatableLedOrder;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.dependency.DependencyUtil;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;
import io.openems.edge.core.appmanager.validator.relaycount.CheckRelayCountFilters;

/**
 * Describes a App for the State-LED on a home system.
 * 
 * <pre>
 *  {
 *     "appId":"App.System.Fenecon.Home",
 *     "alias":"Status-LED",
 *     "instanceId": UUID,
 *     "image": base64,
 *     "properties":{
 *     	 "CTRL_IO_HEATING_ELEMENT_ID": "system0",
 *     	 "DIGITAL_OUTPUT_CHANNEL_1": "io1/DigitalOutput1",
 *     	 "DIGITAL_OUTPUT_CHANNEL_2": "io1/DigitalOutput2",
 *     	 "DIGITAL_OUTPUT_CHANNEL_3": "io1/DigitalOutput3"
 *     },
 *     "dependencies": [
 *     	  {
 *         	"key": "RELAY",
 *         	"instanceId": UUID
 *        }
 *     ],
 *     "appDescriptor": {
 *     		"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
 *     }
 *   }   
 * </pre>
 */
@Component(name = "App.System.Fenecon.Home")
public class SystemFeneconHome extends
		AbstractOpenemsAppWithProps<SystemFeneconHome, SystemFeneconHome.Property, Type.Parameter.BundleParameter>
		implements OpenemsApp {

	public static enum Property implements Type<Property, SystemFeneconHome, Type.Parameter.BundleParameter>, Nameable {
		SYSTEM_FENECON_HOME_ID(AppDef.componentId("system0")), //
		ALIAS(alias()), //
		RELAY_ID(ComponentProps.pickComponentId("io",
				(openemsComponent -> openemsComponent.serviceFactoryPid().equals("IO.Gpio")))),
		LED_ORDER(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".ledOrder.label") //
				.setDefaultValue(TranslatableLedOrder.DEFAULT_RED_BLUE_GREEN) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(TranslatableLedOrder.optionsFactory(), l);
				})));

		private final AppDef<? super SystemFeneconHome, ? super Property, ? super Type.Parameter.BundleParameter> def;

		private Property(
				AppDef<? super SystemFeneconHome, ? super Property, ? super Type.Parameter.BundleParameter> def) {
			this.def = def;
		}

		@Override
		public AppDef<? super SystemFeneconHome, ? super Property, ? super Type.Parameter.BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<SystemFeneconHome>, Type.Parameter.BundleParameter> getParamter() {
			return t -> {

				return new Type.Parameter.BundleParameter(//
						createResourceBundle(t.language) //
				);
			};
		}

		@Override
		public Type<Property, SystemFeneconHome, Type.Parameter.BundleParameter> self() {
			return this;
		}
	}

	@Activate
	public SystemFeneconHome(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil, //
			@Reference AppManagerUtil appManagerUtil //

	) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsError.OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var id = this.getId(t, p, Property.SYSTEM_FENECON_HOME_ID);
			final var alias = this.getString(p, l, Property.ALIAS);
			final var relayId = this.getString(p, l, Property.RELAY_ID);
			final var ledOrder = this.getString(p, l, Property.LED_ORDER);

			final var appIdOfRelay = DependencyUtil.getInstanceIdOfAppWhichHasComponent(this.componentManager, relayId);

			final var components = List.of(//
					new EdgeConfig.Component(id, alias, "System.Fenecon.Home", JsonUtils.buildJsonObject() //
							.addProperty("enabled", true) //
							.addProperty("relayId", relayId) //
							.addProperty("ledOrder", ledOrder) //
							.build()));

			if (appIdOfRelay == null) {
				// relay may be created but not as a app
				return AppConfiguration.create() //
						.addTask(Tasks.component(components)) //
						.build();
			}

			final var dependencies = Lists.newArrayList(new DependencyDeclaration("RELAY", //
					DependencyDeclaration.CreatePolicy.NEVER, //
					DependencyDeclaration.UpdatePolicy.NEVER, //
					DependencyDeclaration.DeletePolicy.NEVER, //
					DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ALL, //
					DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
					DependencyDeclaration.AppDependencyConfig.create() //
							.setSpecificInstanceId(appIdOfRelay) //
							.build()) //
			);

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addDependencies(dependencies) //
					.build();
		};
	}

	@Override
	protected SystemFeneconHome getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.setWebsiteUrl(oem.getAppWebsiteUrl(this.getAppId())) //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.INTEGRATED_SYSTEM };
	}

	@Override
	public ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create() //
				.setInstallableCheckableConfigs(checkRelayCount(3, //
						CheckRelayCountFilters.feneconHome(true), //
						CheckRelayCountFilters.deviceHardware()));
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions //
				.create() //
				.setCanSee(Role.ADMIN).build();
	}
}
