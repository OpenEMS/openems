package io.openems.edge.app.heat;

import static io.openems.common.channel.Unit.SECONDS;
import static io.openems.common.channel.Unit.WATT;
import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.heat.HeatProps.createPhaseInformation;
import static io.openems.edge.app.heat.HeatProps.phaseGroup;
import static io.openems.edge.app.heat.HeatProps.relayContactDef;
import static io.openems.edge.core.appmanager.formly.enums.InputType.NUMBER;
import static io.openems.edge.core.appmanager.validator.Checkables.checkRelayCount;

import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.heat.HeatProps.RelayContactInformation;
import io.openems.edge.app.heat.HeatProps.RelayContactInformationProvider;
import io.openems.edge.app.heat.HeatingElement.HeatingElementParameter;
import io.openems.edge.app.heat.HeatingElement.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ComponentUtil.PreferredRelay;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.dependency.DependencyUtil;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
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
public class HeatingElement extends AbstractOpenemsAppWithProps<HeatingElement, Property, HeatingElementParameter>
		implements OpenemsApp {

	public record HeatingElementParameter(//
			ResourceBundle bundle, //
			RelayContactInformation relayContactInformation //
	) implements BundleProvider, RelayContactInformationProvider {

	}

	public static enum Property implements Type<Property, HeatingElement, HeatingElementParameter>, Nameable {
		// Component-IDs
		CTRL_IO_HEATING_ELEMENT_ID(AppDef.componentId("ctrlIoHeatingElement0")), //
		// Properties
		ALIAS(alias()), //
		OUTPUT_CHANNEL_PHASE_L1(heatingElementRelayContactDef(1)), //
		OUTPUT_CHANNEL_PHASE_L2(heatingElementRelayContactDef(2)), //
		OUTPUT_CHANNEL_PHASE_L3(heatingElementRelayContactDef(3)), //
		OUTPUT_CHANNEL_PHASE_GROUP(phaseGroup(OUTPUT_CHANNEL_PHASE_L1, //
				OUTPUT_CHANNEL_PHASE_L2, OUTPUT_CHANNEL_PHASE_L3)), //
		POWER_PER_PHASE(AppDef.of(HeatingElement.class) //
				.setTranslatedLabelWithAppPrefix(".powerPerPhase.label") //
				.setTranslatedDescriptionWithAppPrefix(".powerPerPhase.description") //
				.setDefaultValue(2000) //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildInput, (app, property, l, parameter, field) -> {
					field.setInputType(NUMBER) //
							.setUnit(WATT, l) //
							.setMin(0);
				})), //
		HYSTERESIS(AppDef.of(HeatingElement.class) //
				.setTranslatedLabelWithAppPrefix(".hysteresis.label") //
				.setTranslatedDescriptionWithAppPrefix(".hysteresis.description") //
				.setDefaultValue(60) //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildInput, (app, property, l, parameter, field) -> {
					field.setInputType(NUMBER) //
							.setUnit(SECONDS, l) //
							.setMin(0);
				}) //
				.bidirectional(CTRL_IO_HEATING_ELEMENT_ID, "minimumSwitchingTime", //
						ComponentManagerSupplier::getComponentManager)), //
		;

		private final AppDef<? super HeatingElement, ? super Property, ? super HeatingElementParameter> def;

		private Property(AppDef<? super HeatingElement, ? super Property, ? super HeatingElementParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, HeatingElement, HeatingElementParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super HeatingElement, ? super Property, ? super HeatingElementParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<HeatingElement>, HeatingElementParameter> getParamter() {
			return t -> {
				return new HeatingElementParameter(//
						createResourceBundle(t.language), //
						createPhaseInformation(t.app.componentUtil, 3, //
								new PreferredRelay(4, new int[] { 1, 2, 3 }), //
								new PreferredRelay(8, new int[] { 4, 5, 6 })) //
				);
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
				return AppConfiguration.create() //
						.addTask(Tasks.component(components)) //
						.build();
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

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addDependencies(dependencies) //
					.build();
		};
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.HEAT };
	}

	@Override
	public ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create() //
				.setInstallableCheckableConfigs(checkRelayCount(3));
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

	private static <P extends BundleProvider & RelayContactInformationProvider> //
	AppDef<OpenemsApp, Nameable, P> heatingElementRelayContactDef(int contactPosition) {
		return AppDef.copyOfGeneric(relayContactDef(contactPosition, Nameable.of("OUTPUT_CHANNEL_PHASE_L1"), //
				Nameable.of("OUTPUT_CHANNEL_PHASE_L2"), Nameable.of("OUTPUT_CHANNEL_PHASE_L3")),
				b -> b //
						.setTranslatedLabelWithAppPrefix(".outputChannelPhaseL" + contactPosition + ".label") //
						.setTranslatedDescription("App.Heat.outputChannel.description") //
						.wrapField((app, property, l, parameter, field) -> field.isRequired(true)) //
						.setAutoGenerateField(false));
	}

}
