package io.openems.edge.app.api;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingBiFunction;
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
import io.openems.edge.core.appmanager.validator.CheckNoComponentInstalledOfFactoryId;
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
	public AppAssistant getAppAssistant() {
		return AppAssistant.create(this.getName()) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildInput(Property.API_TIMEOUT) //
								.setLabel("Api-Timeout") //
								.setDescription("Sets the timeout in seconds for updates on Channels set by this Api.")
								.setDefaultValue(60) //
								.isRequired(true) //
								.setInputType(Type.NUMBER) //
								.setMin(30) //
								.setMax(120) //
								.build())
						.add(JsonFormlyUtil.buildSelect(Property.COMPONENT_IDS) //
								.isMulti(true) //
								.isRequired(true) //
								.setLabel("Component-IDs") //
								.setDescription("Components that should be made available via Modbus.")
								.setOptions(this.componentManager.getAllComponents(), t -> t.id() + ": " + t.alias(),
										OpenemsComponent::id)
								.setDefaultValue("_sum") //
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
	public String getImage() {
		return OpenemsApp.FALLBACK_IMAGE;
	}

	@Override
	public String getName() {
		return "Modbus/TCP-Api Read-Write";
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	protected ThrowingBiFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p) -> {

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
					new EdgeConfig.Component(controllerId, this.getName(), "Controller.Api.ModbusTcp.ReadWrite",
							JsonUtils.buildJsonObject() //
									.addProperty("apiTimeout", apiTimeout) //
									.add("component.ids", controllerIds).build()));

			return new AppConfiguration(components);
		};
	}

	@Override
	public Builder getValidateBuilder() {
		return Validator.create() //
				.setInstallableCheckableNames(new Validator.MapBuilder<>(new TreeMap<String, Map<String, ?>>()) //
						.put(CheckAppsNotInstalled.COMPONENT_NAME, //
								new Validator.MapBuilder<>(new TreeMap<String, Object>()) //
										.put("appIds", new String[] { "App.Api.ModbusTcp.ReadOnly" }) //
										.build())
						// TODO remove this if the free apps get created via App-Manager and an actual
						// app instance gets created
						.put(CheckNoComponentInstalledOfFactoryId.COMPONENT_NAME, //
								new Validator.MapBuilder<>(new TreeMap<String, Object>()) //
										.put("factorieId", "Controller.Api.ModbusTcp.ReadOnly") //
										.build())
						.build());
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

}
