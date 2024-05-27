package io.openems.edge.app.api;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.core.appmanager.formly.enums.InputType.NUMBER;

import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
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
import io.openems.edge.app.api.ModbusRtuApiReadWrite.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration.SchedulerComponent;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

/**
 * Describes a App for ReadWrite Modbus/Rtu Api.
 *
 * <pre>
  {
    "appId":"App.Api.ModbusRtu.ReadWrite",
    "alias":"Modbus/Rtu-Api Read-Write",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CONTROLLER_ID": "ctrlApiModbusRtu0",
    	"API_TIMEOUT": 60,
    	"COMPONENT_IDS": ["_sum", ...]
    },
    "dependencies": [
    	{
        	"key": "READ_ONLY",
        	"instanceId": UUID
    	}
    ],
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.Api.ModbusRtu.ReadWrite")
public class ModbusRtuApiReadWrite extends AbstractOpenemsAppWithProps<ModbusRtuApiReadWrite, Property, BundleParameter>
		implements OpenemsApp {

	public static enum Property implements Type<Property, ModbusRtuApiReadWrite, BundleParameter> {
		// Component-IDs
		CONTROLLER_ID(AppDef.componentId("ctrlApiModbusRtu0")), //
		// Properties
		API_TIMEOUT(AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("App.Api.apiTimeout.label") //
				.setTranslatedDescription("App.Api.apiTimeout.description") //
				.setDefaultValue(60) //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildInput, (app, property, l, parameter, field) -> {
					field.setInputType(NUMBER) //
							.setMin(0);
				}) //
		)), //
		COMPONENT_IDS(AppDef.copyOfGeneric(ModbusRtuApiProps.pickModbusIds(), def -> def //
				.setDefaultValue((app, property, l, parameter) -> {
					final var jsonArrayBuilder = JsonUtils.buildJsonArray() //
							.add("_sum");

					// add ess ids
					app.getComponentUtil().getEnabledComponentsOfStartingId("ess").stream() //
							.sorted((o1, o2) -> o1.id().compareTo(o2.id())) //
							.forEach(ess -> jsonArrayBuilder.add(ess.id()));

					return jsonArrayBuilder.build();
				}) //
				.bidirectional(CONTROLLER_ID, "component.ids", ComponentManagerSupplier::getComponentManager) //
				.appendIsAllowedToSee(AppDef.ofLeastRole(Role.ADMIN)))), //
		;

		private final AppDef<? super ModbusRtuApiReadWrite, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super ModbusRtuApiReadWrite, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, ModbusRtuApiReadWrite, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super ModbusRtuApiReadWrite, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<ModbusRtuApiReadWrite>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public ModbusRtuApiReadWrite(@Reference ComponentManager componentManager, ComponentContext context,
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
			final var controllerId = this.getId(t, p, Property.CONTROLLER_ID);
			final var apiTimeout = this.getInt(p, Property.API_TIMEOUT);
			final var controllerIds = this.getJsonArray(p, Property.COMPONENT_IDS);

			// remove self if selected
			for (var i = 0; i < controllerIds.size(); i++) {
				if (controllerIds.get(i).getAsString().equals(controllerId)) {
					controllerIds.remove(i);
					break;
				}
			}

			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(controllerId, this.getName(l), "Controller.Api.ModbusRtu.ReadWrite",
							JsonUtils.buildJsonObject() //
									.addProperty("apiTimeout", apiTimeout) //
									.add("component.ids", controllerIds) //
									.addProperty("port", 502) //
									.build()) //
			);

			final var dependencies = Lists.newArrayList(//
					new DependencyDeclaration("READ_ONLY", //
							DependencyDeclaration.CreatePolicy.NEVER, //
							DependencyDeclaration.UpdatePolicy.ALWAYS, //
							DependencyDeclaration.DeletePolicy.NEVER, //
							DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ONLY_UNCONFIGURED_PROPERTIES, //
							DependencyDeclaration.DependencyDeletePolicy.ALLOWED, //
							DependencyDeclaration.AppDependencyConfig.create() //
									.setAppId("App.Api.ModbusRtu.ReadOnly") //
									.setProperties(JsonUtils.buildJsonObject() //
											.addProperty(ModbusRtuApiReadOnly.Property.ACTIVE.name(),
													t == ConfigurationTarget.DELETE) //
											.build())
									.build()) //
			);

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.schedulerByCentralOrder(//
							new SchedulerComponent(controllerId, "Controller.Api.ModbusRtu.ReadWrite",
									this.getAppId()))) //
					.addDependencies(dependencies) //
					.build();
		};
	}

	@Override
	protected ModbusRtuApiReadWrite getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

}
