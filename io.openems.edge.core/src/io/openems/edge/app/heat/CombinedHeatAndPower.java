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
import io.openems.edge.app.heat.CombinedHeatAndPower.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractEnumOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.DefaultEnum;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.dependency.DependencyUtil;
import io.openems.edge.core.appmanager.validator.CheckRelayCount;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

/**
 * Describes a App for a Heating Element.
 *
 * <pre>
  {
    "appId":"App.Heat.CHP",
    "alias":"Blockheizkraftwerk (BHKW)",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CTRL_CHP_SOC_ID": "ctrlChpSoc0",
    	"OUTPUT_CHANNEL": "io0/Relay1"
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
@org.osgi.service.component.annotations.Component(name = "App.Heat.CHP")
public class CombinedHeatAndPower extends AbstractEnumOpenemsApp<Property> implements OpenemsApp {

	public static enum Property implements DefaultEnum, Nameable {
		// Component-IDs
		CTRL_CHP_SOC_ID("ctrlChpSoc0"), //
		// Properties
		ALIAS("Blockheizkraftwerk"), //
		OUTPUT_CHANNEL("io0/Relay1"), //
		;

		private final String defaultValue;

		private Property(String defaultValue) {
			this.defaultValue = defaultValue;
		}

		@Override
		public String getDefaultValue() {
			return this.defaultValue;
		}

	}

	@Activate
	public CombinedHeatAndPower(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {
			final var chpId = this.getId(t, p, Property.CTRL_CHP_SOC_ID);

			final var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));
			final var outputChannelAddress = this.getValueOrDefault(p, Property.OUTPUT_CHANNEL);

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(chpId, alias, "Controller.CHP.SoC", JsonUtils.buildJsonObject() //
							.addProperty("inputChannelAddress", "_sum/EssSoc")
							.addProperty("outputChannelAddress", outputChannelAddress) //
							.onlyIf(t == ConfigurationTarget.ADD, b -> b.addProperty("lowThreshold", 20)) //
							.onlyIf(t == ConfigurationTarget.ADD, b -> b.addProperty("highThreshold", 80)) //
							.build()) //
			);

			var componentIdOfRelay = outputChannelAddress.substring(0, outputChannelAddress.indexOf('/'));

			var instanceIdOfRelay = DependencyUtil.getInstanceIdOfAppWhichHasComponent(this.componentManager,
					componentIdOfRelay);

			if (instanceIdOfRelay == null) {
				// relay may be created but not as a app
				return new AppConfiguration(components);
			}

			var dependencies = Lists.newArrayList(new DependencyDeclaration("RELAY", //
					DependencyDeclaration.CreatePolicy.NEVER, //
					DependencyDeclaration.UpdatePolicy.NEVER, //
					DependencyDeclaration.DeletePolicy.NEVER, //
					DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ALL, //
					DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
					DependencyDeclaration.AppDependencyConfig.create() //
							.setSpecificInstanceId(instanceIdOfRelay) //
							.build()));

			return new AppConfiguration(components, null, null, dependencies);
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildSelect(Property.OUTPUT_CHANNEL) //
								.setOptions(this.componentUtil.getAllRelays() //
										.stream().map(r -> r.relays).flatMap(List::stream) //
										.collect(Collectors.toList())) //
								.setDefaultValueWithStringSupplier(() -> {
									var relays = this.componentUtil.getPreferredRelays(Lists.newArrayList(),
											new int[] { 1 }, new int[] { 1 });
									if (relays == null) {
										return Property.OUTPUT_CHANNEL.getDefaultValue();
									}
									return relays[0];
								}) //
								.setLabel(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".outputChannel.label")) //
								.setDescription(TranslationUtil.getTranslation(bundle, //
										"App.Heat.outputChannel.description")) //
								.build())
						.build())
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/fenecon-fems/fems-app-power-to-heat/") //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.HEAT };
	}

	@Override
	public ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create() //
				.setInstallableCheckableConfigs(Lists.newArrayList(//
						new ValidatorConfig.CheckableConfig(CheckRelayCount.COMPONENT_NAME,
								new ValidatorConfig.MapBuilder<>(new TreeMap<String, Object>()) //
										.put("count", 1) //
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
