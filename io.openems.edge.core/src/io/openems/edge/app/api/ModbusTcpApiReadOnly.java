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
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.api.ModbusTcpApiReadOnly.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.validator.CheckAppsNotInstalled;
import io.openems.edge.core.appmanager.validator.Validator;
import io.openems.edge.core.appmanager.validator.Validator.Builder;

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
    	"CONTROLLER_ID": "ctrlApiModbusTcp0"
    },
    "appDescriptor": {
    	"websiteUrl": <a href=
"https://docs.fenecon.de/de/_/latest/fems/apis.html#_fems_app_modbustcp_api_lesend">https://docs.fenecon.de/de/_/latest/fems/apis.html#_fems_app_modbustcp_api_lesend</a>
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.Api.ModbusTcp.ReadOnly")
public class ModbusTcpApiReadOnly extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property {
		// Components
		CONTROLLER_ID;
	}

	@Activate
	public ModbusTcpApiReadOnly(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	public AppAssistant getAppAssistant() {
		return AppAssistant.create(this.getName()) //
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://docs.fenecon.de/de/_/latest/fems/apis.html#_fems_app_modbustcp_api_lesend") //
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
		return "Modbus/TCP-Api Read-Only";
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	protected ThrowingBiFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p) -> {

			var controllerId = this.getId(t, p, Property.CONTROLLER_ID, "ctrlApiModbusTcp0");

			List<EdgeConfig.Component> components = Lists.newArrayList(//
					new EdgeConfig.Component(controllerId, this.getName(), "Controller.Api.ModbusTcp.ReadOnly",
							JsonUtils.buildJsonObject() //
									.build()));

			return new AppConfiguration(components);
		};
	}

	@Override
	public Builder getValidateBuilder() {
		return Validator.create() //
				.setInstallableCheckableNames(new Validator.MapBuilder<>(new TreeMap<String, Map<String, ?>>()) //
						.put(CheckAppsNotInstalled.COMPONENT_NAME, //
								new Validator.MapBuilder<>(new TreeMap<String, Object>()) //
										.put("appIds", new String[] { "App.Api.ModbusTcp.ReadWrite" }) //
										.build())
						.build());
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

}
