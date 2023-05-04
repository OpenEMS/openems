package io.openems.edge.app.heat;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.heat.HeatingElement.ChannelBundle;
import io.openems.edge.app.heat.HeatingElement.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.dependency.DependencyUtil;
import io.openems.edge.core.appmanager.validator.CheckRelayCount;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

/**
 * Describes a App for a RTU Heating Element.
 *
 * <pre>
  {
    "appId":"App.Heat.HeatingElement",
    "alias":"Heizstab",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CTRL_IO_HEATING_ELEMENT_ID": "ctrlIoHeatingElement0",
    	"OUTPUT_CHANNEL_PHASE_L1": "io0/Relay1",
    	"OUTPUT_CHANNEL_PHASE_L2": "io0/Relay2",
    	"OUTPUT_CHANNEL_PHASE_L3": "io0/Relay3",
    	"POWER_PER_PHASE": 2000
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
@Component(name = "App.Heat.HeatingElement")
public class HeatingElement extends AbstractOpenemsAppWithProps<HeatingElement, Property, ChannelBundle>
		implements OpenemsApp {

	public static final class ChannelBundle extends Parameter.BundleParameter {

		private final String[] relays;
		private final List<String> relayOptions;

		public ChannelBundle(ResourceBundle bundle, String[] relays, List<String> relayOptions) {
			super(bundle);
			this.relays = relays;
			this.relayOptions = relayOptions;
		}

		public List<String> getRelayOptions() {
			return this.relayOptions;
		}

		public String[] getRelays() {
			return this.relays;
		}

	}

	private static final AppDef<? super OpenemsApp, ? super Nameable, ? super ChannelBundle> phaseDef(int phaseCount) {
		return AppDef.<OpenemsApp, Nameable, ChannelBundle, //
				OpenemsApp, Nameable, BundleParameter>copyOfGeneric(CommonProps.defaultDef()) //
				.setTranslatedLabelWithAppPrefix(".outputChannelPhaseL" + phaseCount + ".label") //
				.setTranslatedDescription("App.Heat.outputChannel.description") //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(parameter.getRelayOptions());
				}) //
				.setDefaultValue((app, property, l, parameter) -> {
					if (parameter.getRelays() == null) {
						return JsonNull.INSTANCE;
					}
					return new JsonPrimitive(parameter.getRelays()[phaseCount - 1]);
				});
	}

	public static enum Property implements Type<Property, HeatingElement, ChannelBundle>, Nameable {
		// Component-IDs
		CTRL_IO_HEATING_ELEMENT_ID(AppDef.componentId("ctrlIoHeatingElement0")), //
		// Properties
		ALIAS(AppDef.copyOfGeneric(CommonProps.alias())), //
		OUTPUT_CHANNEL_PHASE_L1(AppDef.copyOfGeneric(phaseDef(1))), //
		OUTPUT_CHANNEL_PHASE_L2(AppDef.copyOfGeneric(phaseDef(2))), //
		OUTPUT_CHANNEL_PHASE_L3(AppDef.copyOfGeneric(phaseDef(3))), //
		POWER_PER_PHASE(AppDef.of(HeatingElement.class) //
				.setTranslatedLabelWithAppPrefix(".powerPerPhase.label") //
				.setTranslatedDescriptionWithAppPrefix(".powerPerPhase.description") //
				.setDefaultValue(2000) //
				.setField(JsonFormlyUtil::buildInput, (app, property, l, parameter, field) -> {
					field.setInputType(JsonFormlyUtil.InputBuilder.Type.NUMBER) //
							.setUnit(Unit.WATT, l) //
							.isRequired(true) //
							.setMin(0);
				})), //
		HYSTERESIS(AppDef.of(HeatingElement.class) //
				.setTranslatedLabelWithAppPrefix(".hysteresis.label") //
				.setTranslatedDescriptionWithAppPrefix(".hysteresis.description") //
				.setDefaultValue(60) //
				.setField(JsonFormlyUtil::buildInput, (app, property, l, parameter, field) -> {
					field.setInputType(JsonFormlyUtil.InputBuilder.Type.NUMBER) //
							.setUnit(Unit.SECONDS, l) //
							.isRequired(true) //
							.setMin(0);
				}) //
				.bidirectional(CTRL_IO_HEATING_ELEMENT_ID, "minimumSwitchingTime", //
						ComponentManagerSupplier::getComponentManager)), //
		;

		private final AppDef<? super HeatingElement, ? super Property, ? super ChannelBundle> def;

		private Property(AppDef<? super HeatingElement, ? super Property, ? super ChannelBundle> def) {
			this.def = def;
		}

		@Override
		public Type<Property, HeatingElement, ChannelBundle> self() {
			return this;
		}

		@Override
		public AppDef<? super HeatingElement, ? super Property, ? super ChannelBundle> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<HeatingElement>, ChannelBundle> getParamter() {
			return values -> {
				var relays = values.app.componentUtil.getPreferredRelays(Lists.newArrayList(), new int[] { 1, 2, 3 },
						new int[] { 4, 5, 6 });
				var options = values.app.componentUtil.getAllRelays() //
						.stream().map(r -> r.relays).flatMap(List::stream) //
						.collect(Collectors.toList());
				return new ChannelBundle(AbstractOpenemsApp.getTranslationBundle(values.language), relays, options);
			};
		}

	}

	@Activate
	public HeatingElement(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var heatingElementId = this.getId(t, p, Property.CTRL_IO_HEATING_ELEMENT_ID);

			final var alias = this.getString(p, l, Property.ALIAS);
			final var outputChannelPhaseL1 = this.getString(p, l, Property.OUTPUT_CHANNEL_PHASE_L1);
			final var outputChannelPhaseL2 = this.getString(p, l, Property.OUTPUT_CHANNEL_PHASE_L2);
			final var outputChannelPhaseL3 = this.getString(p, l, Property.OUTPUT_CHANNEL_PHASE_L3);

			final var powerPerPhase = this.getInt(p, Property.POWER_PER_PHASE);
			final var hysteresis = this.getInt(p, Property.HYSTERESIS);

			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(heatingElementId, alias, "Controller.IO.HeatingElement",
							JsonUtils.buildJsonObject() //
									.addProperty("outputChannelPhaseL1", outputChannelPhaseL1) //
									.addProperty("outputChannelPhaseL2", outputChannelPhaseL2) //
									.addProperty("outputChannelPhaseL3", outputChannelPhaseL3) //
									.addProperty("powerPerPhase", powerPerPhase) //
									.addProperty("minimumSwitchingTime", hysteresis) //
									.build()) //
			);

			final var componentIdOfRelay = outputChannelPhaseL1.substring(0, outputChannelPhaseL1.indexOf('/'));
			final var appIdOfRelay = DependencyUtil.getInstanceIdOfAppWhichHasComponent(this.componentManager,
					componentIdOfRelay);

			if (appIdOfRelay == null) {
				// relay may be created but not as a app
				return new AppConfiguration(components);
			}

			final var dependencies = Lists.newArrayList(new DependencyDeclaration("RELAY", //
					DependencyDeclaration.CreatePolicy.NEVER, //
					DependencyDeclaration.UpdatePolicy.NEVER, //
					DependencyDeclaration.DeletePolicy.NEVER, //
					DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ALL, //
					DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
					DependencyDeclaration.AppDependencyConfig.create() //
							.setSpecificInstanceId(appIdOfRelay) //
							.build()) //
			);

			return new AppConfiguration(components, null, null, dependencies);
		};
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
										.put("count", 3) //
										.build())));
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	protected HeatingElement getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

}
