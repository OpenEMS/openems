package io.openems.edge.app.loadcontrol;

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
import io.openems.edge.app.loadcontrol.ManualRelayControl.Property;
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
import io.openems.edge.core.appmanager.validator.CheckRelayCount;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

/**
 * Describes a App for a manual relay control.
 *
 * <pre>
  {
    "appId":"App.LoadControl.ManualRelayControl",
    "alias":"Manuelle Relaissteuerung",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CTRL_IO_FIX_DIGITAL_OUTPUT_ID": "ctrlIoFixDigitalOutput0",
    	"OUTPUT_CHANNEL": "io1/Relay1"
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.LoadControl.ManualRelayControl")
public class ManualRelayControl extends AbstractEnumOpenemsApp<Property> implements OpenemsApp {

	public static enum Property implements DefaultEnum, Nameable {
		// Component-IDs
		CTRL_IO_FIX_DIGITAL_OUTPUT_ID("ctrlIoFixDigitalOutput0"), //
		// Properties
		ALIAS("Manuelle Relaissteuerung"), //
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
	public ManualRelayControl(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {

			final var ctrlIoFixDigitalOutputId = this.getId(t, p, Property.CTRL_IO_FIX_DIGITAL_OUTPUT_ID);

			final var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));
			final var outputChannelAddress = this.getValueOrDefault(p, Property.OUTPUT_CHANNEL);

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlIoFixDigitalOutputId, alias, "Controller.Io.FixDigitalOutput",
							JsonUtils.buildJsonObject() //
									.addProperty("outputChannelAddress", outputChannelAddress) //
									.build()) //
			);

			return new AppConfiguration(components);
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
										.toList()) //
								.setDefaultValueWithStringSupplier(() -> {
									var relays = this.componentUtil.getPreferredRelays(Lists.newArrayList(),
											new int[] { 1 }, new int[] { 1 });
									return relays == null ? null : relays[0];
								}) //
								.isRequired(true) //
								.setLabel(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".outputChannel.label")) //
								.setDescription(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".outputChannel.description")) //
								.build())
						.build())
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/produkte/fems/fems-app-manuelle-relaissteuerung/") //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.LOAD_CONTROL };
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
		return OpenemsAppCardinality.MULTIPLE;
	}

}
