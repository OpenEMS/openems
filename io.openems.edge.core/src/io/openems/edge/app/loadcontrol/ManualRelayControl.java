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
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.loadcontrol.ManualRelayControl.Property;
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
    	"websiteUrl": <a href=
"https://fenecon.de/fems-2-2/fems-app-manuelle-relaissteuerung/">https://fenecon.de/fems-2-2/fems-app-manuelle-relaissteuerung/</a>
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.LoadControl.ManualRelayControl")
public class ManualRelayControl extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property implements DefaultEnum {
		// User values
		ALIAS("Manuelle Relaissteuerung"), //
		OUTPUT_CHANNEL("io0/Relay1"), //
		// Components
		CTRL_IO_FIX_DIGITAL_OUTPUT_ID("ctrlIoFixDigitalOutput0");

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

			List<Component> comp = new ArrayList<>();

			comp.add(new EdgeConfig.Component(ctrlIoFixDigitalOutputId, alias, "Controller.Io.FixDigitalOutput",
					JsonUtils.buildJsonObject() //
							.addProperty("outputChannelAddress", outputChannelAddress) //
							.build()));//

			return new AppConfiguration(comp);
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
				.setWebsiteUrl("https://fenecon.de/fems-2-2/fems-app-manuelle-relaissteuerung/") //
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
