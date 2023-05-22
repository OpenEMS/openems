package io.openems.edge.app.loadcontrol;

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
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.loadcontrol.ThresholdControl.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.DefaultEnum;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.validator.CheckRelayCount;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

/**
 * Describes a App for a Threshold Controller.
 *
 * <pre>
  {
    "appId":"App.LoadControl.ThresholdControl",
    "alias":"Schwellwertsteuerung",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CTRL_IO_CHANNEL_SINGLE_THRESHOLD_ID": "ctrlIoChannelSingleThreshold0",
    	"OUTPUT_CHANNELS":['io1/Relay1', 'io1/Relay2']
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.LoadControl.ThresholdControl")
public class ThresholdControl extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property implements DefaultEnum {
		// Component-IDs
		CTRL_IO_CHANNEL_SINGLE_THRESHOLD_ID("ctrlIoChannelSingleThreshold0"), //
		// Properties
		ALIAS("Schwellwertsteuerung"), //
		OUTPUT_CHANNELS("['io0/Relay1']"), //
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
	public ThresholdControl(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {

			final var ctrlIoChannelSingleThresholdId = this.getId(t, p, Property.CTRL_IO_CHANNEL_SINGLE_THRESHOLD_ID);

			final var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));
			final var outputChannelAddress = EnumUtils.getAsJsonArray(p, Property.OUTPUT_CHANNELS);

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlIoChannelSingleThresholdId, alias,
							"Controller.IO.ChannelSingleThreshold", JsonUtils.buildJsonObject() //
									.onlyIf(t == ConfigurationTarget.ADD,
											j -> j.addProperty("inputChannelAddress", "_sum/EssSoc"))
									.add("outputChannelAddress", outputChannelAddress) //
									.onlyIf(t == ConfigurationTarget.ADD, b -> b.addProperty("threshold", 50)) //
									.build()) //
			);

			return new AppConfiguration(components);
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		var relays = this.componentUtil.getPreferredRelays(Lists.newArrayList(), new int[] { 1 }, new int[] { 1 });
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildSelect(Property.OUTPUT_CHANNELS) //
								.isMulti(true) //
								.setOptions(this.componentUtil.getAllRelays() //
										.stream().map(r -> r.relays).flatMap(List::stream) //
										.collect(Collectors.toList())) //
								.onlyIf(relays != null, t -> t.setDefaultValue(//
										JsonUtils.buildJsonArray() //
												.add(relays[0]) //
												.build())) //
								.isRequired(true) //
								.setLabel(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".outputChannels.label")) //
								.setDescription(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".outputChannels.description")) //
								.build())
						.build())
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/produkte/fems/fems-app-schwellwert-steuerung/") //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategorys() {
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
