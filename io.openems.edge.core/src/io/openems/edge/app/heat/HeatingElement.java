package io.openems.edge.app.heat;

import static io.openems.common.channel.Unit.SECONDS;
import static io.openems.common.channel.Unit.WATT;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.app.common.props.ComponentProps.externMeterIdsForMeterIntegration;
import static io.openems.edge.app.common.props.ComponentProps.howMeasured;
import static io.openems.edge.app.common.props.MeterIntegrationUtil.resolveInternMeterDependencyAndGetMeterId;
import static io.openems.edge.app.common.props.MeterIntegrationUtil.retrieveExternMeterDependency;
import static io.openems.edge.app.common.props.RelayProps.createPhaseInformation;
import static io.openems.edge.app.common.props.RelayProps.phaseGroup;
import static io.openems.edge.app.common.props.RelayProps.relayContactDef;
import static io.openems.edge.core.appmanager.formly.enums.InputType.NUMBER;
import static io.openems.edge.core.appmanager.validator.Checkables.checkRelayCount;

import java.util.ArrayList;
import java.util.List;
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
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.common.props.PropsUtil;
import io.openems.edge.app.common.props.RelayProps;
import io.openems.edge.app.common.props.RelayProps.RelayContactInformation;
import io.openems.edge.app.common.props.RelayProps.RelayContactInformationProvider;
import io.openems.edge.app.enums.MeterIntegration;
import io.openems.edge.app.heat.HeatingElement.HeatingElementParameter;
import io.openems.edge.app.heat.HeatingElement.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.AppManagerUtilSupplier;
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
import io.openems.edge.core.appmanager.validator.relaycount.CheckRelayCountFilters;

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
		 "POWER_PER_PHASE": 2000,
		 "HYSTERESIS": 60,
		 "IS_ELEMENT_MEASURED": false,
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
		implements OpenemsApp, AppManagerUtilSupplier {

	public record HeatingElementParameter(//
			ResourceBundle bundle, //
			RelayContactInformation relayContactInformation //
	) implements BundleProvider, RelayContactInformationProvider {

	}

	private final AppManagerUtil appManagerUtil;

	public enum Property implements Type<Property, HeatingElement, HeatingElementParameter>, Nameable {

		// Component-IDs
		CTRL_IO_HEATING_ELEMENT_ID(AppDef.componentId("ctrlIoHeatingElement0")), //
		// Properties
		ALIAS(alias()), //
		OUTPUT_CHANNEL_PHASE_L1(heatingElementRelayContactDef(1)), //
		OUTPUT_CHANNEL_PHASE_L2(heatingElementRelayContactDef(2)), //
		OUTPUT_CHANNEL_PHASE_L3(heatingElementRelayContactDef(3)), //
		OUTPUT_CHANNEL_PHASE_GROUP(phaseGroup(OUTPUT_CHANNEL_PHASE_L1, //
				OUTPUT_CHANNEL_PHASE_L2, OUTPUT_CHANNEL_PHASE_L3)), //
		POWER_PER_PHASE(AppDef.copyOfGeneric(defaultDef(), appDef -> appDef //
				.setTranslatedLabelWithAppPrefix(".powerPerPhase.label") //
				.setTranslatedDescriptionWithAppPrefix(".powerPerPhase.description") //
				.setDefaultValue(2000) //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildInput, (app, property, l, parameter, field) -> field//
						.setInputType(NUMBER)//
						.setUnit(WATT, l)//
						.setMin(0)//
				))), //
		HYSTERESIS(AppDef.copyOfGeneric(defaultDef(), appDef -> appDef //
				.setTranslatedLabelWithAppPrefix(".hysteresis.label") //
				.setTranslatedDescriptionWithAppPrefix(".hysteresis.description") //
				.setDefaultValue(60) //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildInput, (app, property, l, parameter, field) -> field//
						.setInputType(NUMBER)//
						.setUnit(SECONDS, l)//
						.setMin(0))//
				.bidirectional(CTRL_IO_HEATING_ELEMENT_ID, "minimumSwitchingTime", //
						ComponentManagerSupplier::getComponentManager))), //
		IS_ELEMENT_MEASURED(AppDef.copyOfGeneric(CommonProps.defaultDef(), appDef -> appDef//
				.setTranslatedLabelWithAppPrefix(".measured")//
				.setDefaultValue(false)//
				.setField(JsonFormlyUtil::buildCheckboxFromNameable)//
				.setRequired(true))), //
		HOW_MEASURED(howMeasured(IS_ELEMENT_MEASURED)//
				.setDefaultValue(MeterIntegration.EXTERN)), //
		METER_ID(externMeterIdsForMeterIntegration(IS_ELEMENT_MEASURED, HOW_MEASURED)), //
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
				final var isHomeInstalled = PropsUtil.isHomeInstalled(t.app.appManagerUtil);
				final var deviceHardware = t.app.appManagerUtil //
						.getFirstInstantiatedAppByCategories(OpenemsAppCategory.OPENEMS_DEVICE_HARDWARE);

				return new HeatingElementParameter(//
						createResourceBundle(t.language), //
						createPhaseInformation(t.app.componentUtil, 3, //
								List.of(//
										RelayProps.feneconHomeFilter(t.language, isHomeInstalled, true, deviceHardware), //
										RelayProps.techbaseCm4Gen3Filter(t.language, true, deviceHardware), //
										RelayProps.gpioFilter(), //
										RelayProps.shellyFilter() //
				), //
								List.of(RelayProps.feneconHome2030PreferredRelays(isHomeInstalled,
										new int[] { 1, 2, 3 }), //
										PreferredRelay.of(4, new int[] { 1, 2, 3 }), //
										PreferredRelay.of(8, new int[] { 4, 5, 6 }))) //
				);
			};
		}

	}

	@Activate
	public HeatingElement(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil, //
			@Reference AppManagerUtil appManagerUtil) {
		super(componentManager, componentContext, cm, componentUtil);
		this.appManagerUtil = appManagerUtil;
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
			final var isElementMeasured = this.getBoolean(p, Property.IS_ELEMENT_MEASURED);
			final var howMeasured = this.getEnum(p, MeterIntegration.class, Property.HOW_MEASURED);
			var meterId = "";

			final var deviceHardware = this.appManagerUtil
					.getFirstInstantiatedAppByCategories(OpenemsAppCategory.OPENEMS_DEVICE_HARDWARE);

			final var dependencies = new ArrayList<DependencyDeclaration>();

			if (isElementMeasured) {

				if (howMeasured == MeterIntegration.INTERN) {
					meterId = resolveInternMeterDependencyAndGetMeterId(this, l, t, deviceHardware, dependencies);
				} else {
					meterId = this.getString(p, l, Property.METER_ID);
					dependencies.add(retrieveExternMeterDependency(this, meterId));
				}
			}

			final var tmpMeterId = meterId;
			var components = Lists.newArrayList(//
					new EdgeConfig.Component(heatingElementId, alias, "Controller.IO.HeatingElement", buildJsonObject() //
							.addProperty("outputChannelPhaseL1", outputChannelPhaseL1) //
							.addProperty("outputChannelPhaseL2", outputChannelPhaseL2) //
							.addProperty("outputChannelPhaseL3", outputChannelPhaseL3) //
							.addProperty("powerPerPhase", powerPerPhase) //
							.addProperty("minimumSwitchingTime", hysteresis) //
							.onlyIf(t != ConfigurationTarget.VALIDATE, b -> b.addProperty("meter.id", tmpMeterId)) //
							.build()) //
			);

			final var componentIdOfRelay = outputChannelPhaseL1.substring(0, outputChannelPhaseL1.indexOf('/'));
			final var appIdOfRelay = DependencyUtil.getInstanceIdOfAppWhichHasComponent(this.componentManager,
					componentIdOfRelay);

			if (appIdOfRelay == null) {
				// relay may be created but not as a app
				return AppConfiguration.create() //
						.addTask(Tasks.component(components)) //
						.addDependencies(dependencies) //
						.build();
			}

			dependencies.add(new DependencyDeclaration("RELAY", //
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
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.HEAT };
	}

	@Override
	public ValidatorConfig.Builder getValidateBuilder() {
		final var deviceHardware = this.appManagerUtil
				.getFirstInstantiatedAppByCategories(OpenemsAppCategory.OPENEMS_DEVICE_HARDWARE);
		return ValidatorConfig.create() //
				.setInstallableCheckableConfigs(//
						checkRelayCount(3, //
								CheckRelayCountFilters.feneconHome(true, deviceHardware), //
								CheckRelayCountFilters.techbaseCm4sGen3(true, deviceHardware), //
								CheckRelayCountFilters.gpio(), //
								CheckRelayCountFilters.shelly()));
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	protected HeatingElement getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public AppManagerUtil getAppManagerUtil() {
		return this.appManagerUtil;
	}

	private static <P extends BundleProvider & RelayContactInformationProvider> //
			AppDef<OpenemsApp, Nameable, P> heatingElementRelayContactDef(int contactPosition) {
		return AppDef.copyOfGeneric(relayContactDef(contactPosition, Nameable.of("OUTPUT_CHANNEL_PHASE_L1"), //
				Nameable.of("OUTPUT_CHANNEL_PHASE_L2"), Nameable.of("OUTPUT_CHANNEL_PHASE_L3")),
				b -> b //
						.setTranslatedLabelWithAppPrefix(".outputChannelPhaseL" + contactPosition + ".label") //
						.setTranslatedDescription("App.Heat.outputChannel.description") //
						.setRequired(true) //
						.setAutoGenerateField(false));
	}
}
