package io.openems.edge.app.loadcontrol;

import java.util.ArrayList;
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
import io.openems.common.types.EdgeConfig.Component;
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
    	"websiteUrl": <a href=
"https://fenecon.de/fems-2-2/fems-app-schwellwert-steuerung/">https://fenecon.de/fems-2-2/fems-app-schwellwert-steuerung/</a>
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.LoadControl.ThresholdControl")
public class ThresholdControl extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property implements DefaultEnum {
		// User values
		ALIAS("Schwellwertsteuerung"), //
		OUTPUT_CHANNELS("['io0/Relay1']"), //
		// Components
		CTRL_IO_CHANNEL_SINGLE_THRESHOLD_ID("ctrlIoChannelSingleThreshold0");

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

			List<Component> comp = new ArrayList<>();

			comp.add(new EdgeConfig.Component(ctrlIoChannelSingleThresholdId, alias,
					"Controller.IO.ChannelSingleThreshold", JsonUtils.buildJsonObject() //
							.onlyIf(t == ConfigurationTarget.ADD,
									j -> j.addProperty("inputChannelAddress", "_sum/EssSoc"))
							.add("outputChannelAddress", outputChannelAddress) //
							.onlyIf(t == ConfigurationTarget.ADD, b -> b.addProperty("threshold", 50)) //
							.build()));//

			return new AppConfiguration(comp);
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildSelect(Property.OUTPUT_CHANNELS) //
								.isMulti(true) //
								.setOptions(this.componentUtil.getAllRelays() //
										.stream().map(r -> r.relays).flatMap(List::stream) //
										.collect(Collectors.toList())) //
								.setDefaultValueWithStringSupplier(() -> {
									var relays = this.componentUtil.getPreferredRelays(Lists.newArrayList(),
											new int[] { 1 }, new int[] { 1 });
									return relays == null ? null : relays[0];
								}) //
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
				.setWebsiteUrl("https://fenecon.de/fems-2-2/fems-app-schwellwert-steuerung/") //
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

	@Override
	public String getImage() {
		return "data:image/png;base64,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";
	}

}
