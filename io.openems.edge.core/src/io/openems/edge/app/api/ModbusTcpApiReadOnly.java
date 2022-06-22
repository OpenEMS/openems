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
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

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
"https://fenecon.de/fems-2-2/fems-app-modbus-tcp-lesend-2/">https://fenecon.de/fems-2-2/fems-app-modbus-tcp-lesend-2/</a>
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
	public AppAssistant getAppAssistant(Language language) {
		return AppAssistant.create(this.getName(language)) //
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/fems-2-2/fems-app-modbus-tcp-lesend-2/") //
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

			List<EdgeConfig.Component> components = Lists.newArrayList(//
					new EdgeConfig.Component(controllerId, this.getName(l), "Controller.Api.ModbusTcp.ReadOnly",
							JsonUtils.buildJsonObject() //
									.build()));

			return new AppConfiguration(components);
		};
	}

	@Override
	protected io.openems.edge.core.appmanager.validator.ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create() //
				.setInstallableCheckableConfigs(
						Lists.newArrayList(new ValidatorConfig.CheckableConfig(CheckAppsNotInstalled.COMPONENT_NAME,
								new ValidatorConfig.MapBuilder<>(new TreeMap<String, Object>()) //
										.put("appIds", new String[] { "App.Api.ModbusTcp.ReadWrite" }) //
										.build())));
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public String getImage() {
		return "data:image/png;base64,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"
				+ "AAASUVORK5CYII=";
	}

}
