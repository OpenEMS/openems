package io.openems.edge.app.heat;

import java.util.EnumMap;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

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
import io.openems.edge.app.heat.HeatPump.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.dependency.DependencyUtil;
import io.openems.edge.core.appmanager.validator.CheckRelayCount;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

/**
 * Describes a App for a Heat Pump.
 *
 * <pre>
  {
    "appId":"App.Heat.HeatPump",
    "alias":""SG-Ready" Wärmepumpe",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CTRL_IO_HEAT_PUMP_ID": "ctrlIoHeatPump0",
    	"OUTPUT_CHANNEL_1": "io0/Relay2",
    	"OUTPUT_CHANNEL_2": "io0/Relay3"
    },
    "dependencies": [
    	{
        	"key": "RELAY",
        	"instanceId": UUID
    	}
    ],
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.Heat.HeatPump")
public class HeatPump extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property {
		// Component-IDs
		CTRL_IO_HEAT_PUMP_ID, //
		// Properties
		ALIAS, //
		OUTPUT_CHANNEL_1, //
		OUTPUT_CHANNEL_2 //
		;
	}

	@Activate
	public HeatPump(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {
			final var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));
			final var ctrlIoHeatPumpId = this.getId(t, p, Property.CTRL_IO_HEAT_PUMP_ID, "ctrlIoHeatPump0");

			var outputChannel1 = this.getValueOrDefault(p, Property.OUTPUT_CHANNEL_1, "io0/Relay2");
			var outputChannel2 = this.getValueOrDefault(p, Property.OUTPUT_CHANNEL_2, "io0/Relay3");

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlIoHeatPumpId, alias, "Controller.Io.HeatPump.SgReady",
							JsonUtils.buildJsonObject() //
									.addProperty("outputChannel1", outputChannel1) //
									.addProperty("outputChannel2", outputChannel2) //
									.build()) //
			);

			var componentIdOfRelay = outputChannel1.substring(0, outputChannel1.indexOf('/'));
			var appIdOfRelay = DependencyUtil.getInstanceIdOfAppWhichHasComponent(this.componentManager,
					componentIdOfRelay);

			if (appIdOfRelay == null) {
				// relay may be created but not as an app
				return new AppConfiguration(components);
			}

			var dependencies = Lists.newArrayList(new DependencyDeclaration("RELAY", //
					DependencyDeclaration.CreatePolicy.NEVER, //
					DependencyDeclaration.UpdatePolicy.NEVER, //
					DependencyDeclaration.DeletePolicy.NEVER, //
					DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ALL, //
					DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
					DependencyDeclaration.AppDependencyConfig.create() //
							.setSpecificInstanceId(appIdOfRelay) //
							.build()));

			return new AppConfiguration(components, null, null, dependencies);
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		var relays = this.componentUtil.getPreferredRelays(Lists.newArrayList(), new int[] { 2, 3 },
				new int[] { 2, 3 });
		var options = this.componentUtil.getAllRelays() //
				.stream().map(r -> r.relays).flatMap(List::stream) //
				.collect(Collectors.toList());
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildSelect(Property.OUTPUT_CHANNEL_1) //
								.setOptions(options) //
								.onlyIf(relays != null, t -> t.setDefaultValue(relays[0])) //
								.setLabel(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".outputChannel1.label"))
								.setDescription(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".outputChannel1.description"))
								.build())
						.add(JsonFormlyUtil.buildSelect(Property.OUTPUT_CHANNEL_2) //
								.setOptions(options) //
								.onlyIf(relays != null, t -> t.setDefaultValue(relays[1])) //
								.setLabel(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".outputChannel2.label"))
								.setDescription(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".outputChannel2.description"))
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
		return new OpenemsAppCategory[] { OpenemsAppCategory.HEAT };
	}

	@Override
	public ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create() //
				.setInstallableCheckableConfigs(Lists.newArrayList(//
						new ValidatorConfig.CheckableConfig(CheckRelayCount.COMPONENT_NAME,
								new ValidatorConfig.MapBuilder<>(new TreeMap<String, Object>()) //
										.put("count", 2) //
										.build())));
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

}
