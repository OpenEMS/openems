package io.openems.edge.app.api;

import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.api.ModbusTcpApiReadOnly.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;

/**
 * Describes a App for ReadOnly Modbus/TCP Api.
 *
 * <pre>
  {
    "appId":"App.Api.ModbusTcp.ReadOnly",
    "alias":"Modbus/TCP-Api Read-Only",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"ACTIVE": true,
    	"CONTROLLER_ID": "ctrlApiModbusTcp0"
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.Api.ModbusTcp.ReadOnly")
public class ModbusTcpApiReadOnly extends AbstractOpenemsAppWithProps<ModbusTcpApiReadOnly, Property, BundleParameter>
		implements OpenemsApp {

	public static enum Property implements Type<Property, ModbusTcpApiReadOnly, BundleParameter>, Nameable {
		// Components
		CONTROLLER_ID(AppDef.componentId("ctrlApiModbusTcp0")), //
		// Properties
		ALIAS(AppDef.of(ModbusTcpApiReadOnly.class) //
				.setDefaultValueToAppName()),
		ACTIVE(AppDef.of(ModbusTcpApiReadOnly.class) //
				.setDefaultValue((app, prop, l, param) -> {
					var active = app.componentManager.getEdgeConfig()
							.getComponentIdsByFactory("Controller.Api.ModbusTcp.ReadWrite").size() == 0;
					return new JsonPrimitive(active);
				})), //
		COMPONENT_IDS(AppDef.copyOfGeneric(ModbusTcpApiProps.pickModbusIds(), def -> def //
				.setDefaultValue((app, property, l, parameter) -> {
					return JsonUtils.buildJsonArray() //
							.add("_sum") //
							.build();
				}) //
				.bidirectional(CONTROLLER_ID, "component.ids", ComponentManagerSupplier::getComponentManager) //
				.appendIsAllowedToSee(AppDef.ofLeastRole(Role.ADMIN)))), //
		;

		private AppDef<? super ModbusTcpApiReadOnly, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super ModbusTcpApiReadOnly, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public AppDef<? super ModbusTcpApiReadOnly, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Property self() {
			return this;
		}

		@Override
		public Function<GetParameterValues<ModbusTcpApiReadOnly>, BundleParameter> getParamter() {
			return Type.Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	@Activate
	public ModbusTcpApiReadOnly(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/produkte/fems/fems-app-modbus-tcp-lesend/") //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.API };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			if (!this.getBoolean(p, Property.ACTIVE)) {
				return new AppConfiguration();
			}

			var controllerId = this.getId(t, p, Property.CONTROLLER_ID);

			final var componentIds = this.getJsonArray(p, Property.COMPONENT_IDS);

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(controllerId, this.getName(l), "Controller.Api.ModbusTcp.ReadOnly",
							JsonUtils.buildJsonObject() //
									.add("component.ids", componentIds) //
									.build()));

			return new AppConfiguration(components);
		};
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	protected ModbusTcpApiReadOnly getApp() {
		return this;
	}

}
