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
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.validator.CheckAppsNotInstalled;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

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
    	"websiteUrl": <a href=
"https://fenecon.de/fems-2-2/fems-app-modbus-tcp-schreibzugriff-2/">https://fenecon.de/fems-2-2/fems-app-modbus-tcp-schreibzugriff-2/</a>
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
								.setLabel(TranslationUtil.getTranslation(bundle, "App.Api.apiTimeout.label")) //
								.setDescription(
										TranslationUtil.getTranslation(bundle, "App.Api.apiTimeout.description")) //
								.setDefaultValue(60) //
								.isRequired(true) //
								.setInputType(Type.NUMBER) //
								.setMin(30) //
								.setMax(120) //
								.build())
						.add(JsonFormlyUtil.buildSelect(Property.COMPONENT_IDS) //
								.isMulti(true) //
								.isRequired(true) //
								.setLabel(
										TranslationUtil.getTranslation(bundle, this.getAppId() + ".componentIds.label")) //
								.setDescription(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".componentIds.description")) //
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
				.setWebsiteUrl("https://fenecon.de/fems-2-2/fems-app-modbus-tcp-schreibzugriff-2/") //
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
	protected io.openems.edge.core.appmanager.validator.ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create() //
				.setInstallableCheckableConfigs(
						Lists.newArrayList(new ValidatorConfig.CheckableConfig(CheckAppsNotInstalled.COMPONENT_NAME,
								new ValidatorConfig.MapBuilder<>(new TreeMap<String, Object>()) //
										.put("appIds", new String[] { "App.Api.ModbusTcp.ReadOnly" }) //
										.build())));
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public String getImage() {
		return "data:image/png;base64,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";
	}

}
