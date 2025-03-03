package io.openems.edge.app.api;

import static io.openems.edge.app.common.props.CommonProps.alias;

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
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.api.ModbusRtuApiReadOnly.Property;
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
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration.SchedulerComponent;

/**
 * Describes a App for ReadOnly Modbus/RTU Api.
 *
 * <pre>
  {
    "appId":"App.Api.ModbusRtu.ReadOnly",
    "alias":"Modbus/RTU-Api Read-Only",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"ACTIVE": true,
    	"CONTROLLER_ID": "ctrlApiModbusRtu0"
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.Api.ModbusRtu.ReadOnly")
public class ModbusRtuApiReadOnly extends AbstractOpenemsAppWithProps<ModbusRtuApiReadOnly, Property, BundleParameter>
		implements OpenemsApp {

	public static enum Property implements Type<Property, ModbusRtuApiReadOnly, BundleParameter>, Nameable {
		// Component-IDs
		CONTROLLER_ID(AppDef.componentId("ctrlApiModbusRtu0")), //
		// Properties
		ALIAS(alias()), //
		API_TIMEOUT(ModbusApiProps.apiTimeout() //
				.setRequired(true)), //
		COMPONENT_IDS(ModbusApiProps.componentIds(CONTROLLER_ID) //
				.setRequired(true)), //
		PORT_NAME(ModbusApiProps.portName() //
				.setRequired(true)), //
		BAUDRATE(ModbusApiProps.baudrate() //
				.setRequired(true)), //
		DATABITS(ModbusApiProps.databits() //
				.setRequired(true)),
		STOPBITS(ModbusApiProps.stopbits() //
				.setRequired(true)), //
		PARITY(ModbusApiProps.parity() //
				.setRequired(true)); //

		private final AppDef<? super ModbusRtuApiReadOnly, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super ModbusRtuApiReadOnly, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, ModbusRtuApiReadOnly, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super ModbusRtuApiReadOnly, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<ModbusRtuApiReadOnly>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public ModbusRtuApiReadOnly(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.setWebsiteUrl(oem.getAppWebsiteUrl(this.getAppId())) //
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
			final var portName = this.getString(p, Property.PORT_NAME);
			final var alias = this.getString(p, Property.ALIAS);
			final var controllerId = this.getId(t, p, Property.CONTROLLER_ID);
			final var apiTimeout = this.getInt(p, Property.API_TIMEOUT);
			final var controllerIds = this.getJsonArray(p, Property.COMPONENT_IDS);
			final var baudrate = this.getInt(p, Property.BAUDRATE);
			final var databits = this.getInt(p, Property.DATABITS);
			final var stopbits = this.getString(p, Property.STOPBITS);
			final var parity = this.getString(p, Property.PARITY);

			// remove self if selected
			for (var i = 0; i < controllerIds.size(); i++) {
				if (controllerIds.get(i).getAsString().equals(controllerId)) {
					controllerIds.remove(i);
					break;
				}
			}

			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(controllerId, alias, "Controller.Api.ModbusRtu.ReadOnly",
							JsonUtils.buildJsonObject() //
									.addProperty("apiTimeout", apiTimeout) //
									.add("component.ids", controllerIds) //
									.addProperty("portName", portName) //
									.addProperty("baudRate", baudrate) //
									.addProperty("databits", databits) //
									.addProperty("stopbits", stopbits) //
									.addProperty("parity", parity) //
									.build()) //
			);

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.schedulerByCentralOrder(//
							new SchedulerComponent(controllerId, "Controller.Api.ModbusRtu.ReadOnly", this.getAppId()))) //
					.build();
		};
	}

	@Override
	protected ModbusRtuApiReadOnly getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create()//
				.setCanDelete(Role.ADMIN)//
				.setCanSee(Role.ADMIN)//
				.build();
	}

}