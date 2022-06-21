package io.openems.edge.app.api;

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
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.api.ModbusTcpApiReadWrite.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
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
import io.openems.edge.core.appmanager.validator.CheckAppsNotInstalled;
import io.openems.edge.core.appmanager.validator.Validator;
import io.openems.edge.core.appmanager.validator.Validator.Builder;

/**
 * Describes a App for ReadWrite Modbus/TCP Api.
 *
 * <pre>
  {
    "appId":"App.Api.ModbusTcp.ReadWrite",
    "alias":"Modbus/TCP-Api Read-Write",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CONTROLLER_ID": "ctrlApiModbusTcp0",
    	"API_TIMEOUT": 60,
    	"COMPONENT_IDS": ["_sum", ...]
    },
    "appDescriptor": {
    	"websiteUrl": URL
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.Api.ModbusTcp.ReadWrite")
public class ModbusTcpApiReadWrite extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property {
		// Components
		CONTROLLER_ID, //
		// User-Values
		API_TIMEOUT, //
		COMPONENT_IDS;
	}

	@Activate
	public ModbusTcpApiReadWrite(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildInput(Property.API_TIMEOUT) //
								.setLabel(bundle.getString("App.Api.apiTimeout.label")) //
								.setDescription(bundle.getString("App.Api.apiTimeout.description")) //
								.setDefaultValue(60) //
								.isRequired(true) //
								.setInputType(Type.NUMBER) //
								.setMin(30) //
								.setMax(120) //
								.build())
						.add(JsonFormlyUtil.buildSelect(Property.COMPONENT_IDS) //
								.isMulti(true) //
								.isRequired(true) //
								.setLabel(bundle.getString(this.getAppId() + ".componentIds.label")) //
								.setDescription(bundle.getString(this.getAppId() + ".componentIds.description"))
								.setOptions(this.componentManager.getAllComponents(), t -> t.id() + ": " + t.alias(),
										OpenemsComponent::id)
								.setDefaultValue(JsonUtils.buildJsonArray().add("_sum").build()) //
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
		return new OpenemsAppCategory[] { OpenemsAppCategory.API };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {

			var controllerId = this.getId(t, p, Property.CONTROLLER_ID, "ctrlApiModbusTcp0");
			var apiTimeout = EnumUtils.getAsInt(p, Property.API_TIMEOUT);
			var controllerIds = EnumUtils.getAsJsonArray(p, Property.COMPONENT_IDS);

			// remove self if selected
			for (var i = 0; i < controllerIds.size(); i++) {
				if (controllerIds.get(i).getAsString().equals(controllerId)) {
					controllerIds.remove(i);
					break;
				}
			}

			List<EdgeConfig.Component> components = Lists.newArrayList(//
					new EdgeConfig.Component(controllerId, this.getName(l), "Controller.Api.ModbusTcp.ReadWrite",
							JsonUtils.buildJsonObject() //
									.addProperty("apiTimeout", apiTimeout) //
									.add("component.ids", controllerIds).build()));

			return new AppConfiguration(components);
		};
	}

	@Override
	public Builder getValidateBuilder() {
		return Validator.create() //
				.setInstallableCheckableConfigs(Lists.newArrayList(//
						new Validator.CheckableConfig(CheckAppsNotInstalled.COMPONENT_NAME,
								new Validator.MapBuilder<>(new TreeMap<String, Object>()) //
										.put("appIds", new String[] { "App.Api.ModbusTcp.ReadOnly" }) //
										.build())));
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

}
