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
import com.google.gson.JsonNull;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.common.props.PropsUtil;
import io.openems.edge.app.common.props.RelayProps;
import io.openems.edge.app.common.props.RelayProps.RelayContactInformation;
import io.openems.edge.app.common.props.RelayProps.RelayContactInformationProvider;
import io.openems.edge.app.enums.MeterIntegration;
import io.openems.edge.app.enums.OptionsFactory;
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
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.expression.BooleanExpression;
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

	/**
	 * The names of all six relay contact {@link Property Properties}, three for
	 * Heating Element A and three for the optional Heating Element B. Used to
	 * disable already selected relay contacts in the other select fields.
	 */
	private static final Nameable[] ALL_OUTPUT_CHANNELS = { //
			Nameable.of("OUTPUT_CHANNEL_PHASE_L1"), //
			Nameable.of("OUTPUT_CHANNEL_PHASE_L2"), //
			Nameable.of("OUTPUT_CHANNEL_PHASE_L3"), //
			Nameable.of("OUTPUT_CHANNEL_PHASE_L1_HEATING_ELEMENT_B"), //
			Nameable.of("OUTPUT_CHANNEL_PHASE_L2_HEATING_ELEMENT_B"), //
			Nameable.of("OUTPUT_CHANNEL_PHASE_L3_HEATING_ELEMENT_B"), //
	};

	private static final String HEATING_ELEMENT_LABEL_A = "App.Heat.HeatingElement.outputChannelPhaseLNHeatingElementA.label";
	private static final String HEATING_ELEMENT_LABEL_B = "App.Heat.HeatingElement.outputChannelPhaseLNHeatingElementB.label";

	public enum Property implements Type<Property, HeatingElement, HeatingElementParameter>, Nameable {

		// Component-IDs
		CTRL_IO_HEATING_ELEMENT_ID(AppDef.componentId("ctrlIoHeatingElement0")), //
		// Properties
		ALIAS(alias()), //
		NUMBER_OF_HEATING_ELEMENTS(AppDef.copyOfGeneric(defaultDef(), appDef -> appDef //
				.setIsAllowedToSee(AppDef.ofLeastRole(Role.ADMIN)) //
				.setTranslatedLabelWithAppPrefix(".numberOfHeatingElements.label") //
				.setTranslatedDescriptionWithAppPrefix(".numberOfHeatingElements.description") //
				.setDefaultValue(NumberOfHeatingElements.ONE) //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(OptionsFactory.of(NumberOfHeatingElements.class), l);
				}))), //
		OUTPUT_CHANNEL_PHASE_L1(heatingElementRelayContactDef(1, HEATING_ELEMENT_LABEL_A, 1, //
				ALL_OUTPUT_CHANNELS)), //
		OUTPUT_CHANNEL_PHASE_L2(heatingElementRelayContactDef(2, HEATING_ELEMENT_LABEL_A, 2, //
				ALL_OUTPUT_CHANNELS)), //
		OUTPUT_CHANNEL_PHASE_L3(heatingElementRelayContactDef(3, HEATING_ELEMENT_LABEL_A, 3, //
				ALL_OUTPUT_CHANNELS)), //
		OUTPUT_CHANNEL_PHASE_L1_HEATING_ELEMENT_B(heatingElementBRelayContactDef(4, HEATING_ELEMENT_LABEL_B, 1)), //
		OUTPUT_CHANNEL_PHASE_L2_HEATING_ELEMENT_B(heatingElementBRelayContactDef(5, HEATING_ELEMENT_LABEL_B, 2)), //
		OUTPUT_CHANNEL_PHASE_L3_HEATING_ELEMENT_B(heatingElementBRelayContactDef(6, HEATING_ELEMENT_LABEL_B, 3)), //
		OUTPUT_CHANNEL_PHASE_GROUP(phaseGroup(Map.of(//
				OUTPUT_CHANNEL_PHASE_L1_HEATING_ELEMENT_B, isTwoHeatingElements(), //
				OUTPUT_CHANNEL_PHASE_L2_HEATING_ELEMENT_B, isTwoHeatingElements(), //
				OUTPUT_CHANNEL_PHASE_L3_HEATING_ELEMENT_B, isTwoHeatingElements()), //
				OUTPUT_CHANNEL_PHASE_L1, OUTPUT_CHANNEL_PHASE_L2, OUTPUT_CHANNEL_PHASE_L3, //
				OUTPUT_CHANNEL_PHASE_L1_HEATING_ELEMENT_B, OUTPUT_CHANNEL_PHASE_L2_HEATING_ELEMENT_B, //
				OUTPUT_CHANNEL_PHASE_L3_HEATING_ELEMENT_B)), //
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
		POWER_PER_PHASE_HEATING_ELEMENT_B(AppDef.copyOfGeneric(defaultDef(), appDef -> appDef //
				.setIsAllowedToSee(AppDef.ofLeastRole(Role.ADMIN)) //
				.setTranslatedLabelWithAppPrefix(".powerPerPhaseHeatingElementB.label") //
				.setTranslatedDescriptionWithAppPrefix(".powerPerPhaseHeatingElementB.description") //
				.setDefaultValue(2000) //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildInput, (app, property, l, parameter, field) -> field//
						.setInputType(NUMBER)//
						.setUnit(WATT, l)//
						.setMin(0)//
				) //
				.wrapField((app, property, l, parameter, field) -> field.onlyShowIf(isTwoHeatingElements())))), //
		DISTRIBUTION(AppDef.copyOfGeneric(defaultDef(), appDef -> appDef //
				.setIsAllowedToSee(AppDef.ofLeastRole(Role.ADMIN)) //
				.setTranslatedLabelWithAppPrefix(".distribution.label") //
				.setTranslatedDescriptionWithAppPrefix(".distribution.description") //
				.setDefaultValue(Distribution.SEQUENTIAL) //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field//
							.setOptions(OptionsFactory.of(Distribution.class), l)//
							.onlyShowIf(isTwoHeatingElements());
				}))), //
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
						// numberOfRelays is always 6 (3 for Heating Element A + 3 for the optional
						// Heating Element B), independent of the currently selected
						// NUMBER_OF_HEATING_ELEMENTS, since the relay contact properties for
						// Heating Element B always exist (they are just hidden if not needed).
						createPhaseInformation(t.app.componentUtil, 6, //
								List.of(//
										RelayProps.feneconHomeFilter(t.language, isHomeInstalled, true, deviceHardware), //
										RelayProps.techbaseCm4Gen3Filter(t.language, true, deviceHardware), //
										RelayProps.gpioFilter(), //
										RelayProps.shellyFilter() //
				), //
								List.of(RelayProps.feneconHome2030PreferredRelays(isHomeInstalled,
										new int[] { 1, 2, 3 }), //
										PreferredRelay.of(4, new int[] { 1, 2, 3 }), //
										// Keep the existing defaults for Heating Element A and use the remaining
										// contacts for Heating Element B.
										PreferredRelay.of(8, new int[] { 4, 5, 6, 1, 2, 3 }))) //
				);
			};
		}

		/**
		 * Gets the {@link BooleanExpression} which evaluates to true if
		 * {@link #NUMBER_OF_HEATING_ELEMENTS} is set to
		 * {@link NumberOfHeatingElements#TWO}.
		 *
		 * @return the {@link BooleanExpression}
		 */
		private static BooleanExpression isTwoHeatingElements() {
			return Exp.currentModelValue(NUMBER_OF_HEATING_ELEMENTS) //
					.equal(Exp.staticValue(NumberOfHeatingElements.TWO));
		}

		private static <P extends BundleProvider & RelayContactInformationProvider> //
				AppDef<OpenemsApp, Nameable, P> heatingElementBRelayContactDef(//
						int contactPosition, //
						String translationKey, //
						int phaseNumber //
		) {
			return AppDef.copyOfGeneric(
					heatingElementRelayContactDef(contactPosition, translationKey, phaseNumber, ALL_OUTPUT_CHANNELS),
					def -> def//
							.wrapField((app, property, l, parameter, field) -> {
								field.onlyShowIf(isTwoHeatingElements());
							}));
		}

		private static <P extends BundleProvider & RelayContactInformationProvider> //
				AppDef<OpenemsApp, Nameable, P> heatingElementRelayContactDef(//
						int contactPosition, //
						String translationKey, //
						int phaseNumber, //
						Nameable... allContacts //
		) {
			return AppDef.copyOfGeneric(relayContactDef(contactPosition, allContacts), b -> b //
					.setTranslatedLabel(translationKey, phaseNumber)
					.setTranslatedDescription("App.Heat.outputChannel.description") //
					.setRequired(true) //
					.setAutoGenerateField(false));
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
			final var numberOfHeatingElements = this.getEnum(p, NumberOfHeatingElements.class,
					Property.NUMBER_OF_HEATING_ELEMENTS);
			final var outputChannelPhaseL1 = this.getString(p, l, Property.OUTPUT_CHANNEL_PHASE_L1);
			final var outputChannelPhaseL2 = this.getString(p, l, Property.OUTPUT_CHANNEL_PHASE_L2);
			final var outputChannelPhaseL3 = this.getString(p, l, Property.OUTPUT_CHANNEL_PHASE_L3);

			final var powerPerPhase = this.getInt(p, Property.POWER_PER_PHASE);
			final var hysteresis = this.getInt(p, Property.HYSTERESIS);
			final var isElementMeasured = this.getBoolean(p, Property.IS_ELEMENT_MEASURED);
			final var howMeasured = this.getEnum(p, MeterIntegration.class, Property.HOW_MEASURED);
			var meterId = "";

			// The relay contact properties of Heating Element B may not have a valid
			// default value (e.g. if there is no free relay left), which is only a
			// problem if Heating Element B is actually used; so these properties are
			// only read if NUMBER_OF_HEATING_ELEMENTS is TWO.
			String outputChannelPhaseL1HeatingElementB = null;
			String outputChannelPhaseL2HeatingElementB = null;
			String outputChannelPhaseL3HeatingElementB = null;
			var powerPerPhaseHeatingElementB = 0;
			var distribution = Distribution.SEQUENTIAL;
			if (numberOfHeatingElements == NumberOfHeatingElements.TWO) {
				outputChannelPhaseL1HeatingElementB = this.getString(p, l,
						Property.OUTPUT_CHANNEL_PHASE_L1_HEATING_ELEMENT_B);
				outputChannelPhaseL2HeatingElementB = this.getString(p, l,
						Property.OUTPUT_CHANNEL_PHASE_L2_HEATING_ELEMENT_B);
				outputChannelPhaseL3HeatingElementB = this.getString(p, l,
						Property.OUTPUT_CHANNEL_PHASE_L3_HEATING_ELEMENT_B);
				powerPerPhaseHeatingElementB = this.getInt(p, Property.POWER_PER_PHASE_HEATING_ELEMENT_B);
				distribution = this.getEnum(p, Distribution.class, Property.DISTRIBUTION);
			}

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
			final var componentConfig = buildJsonObject() //
					.addProperty("outputChannelPhaseL1", outputChannelPhaseL1) //
					.addProperty("outputChannelPhaseL2", outputChannelPhaseL2) //
					.addProperty("outputChannelPhaseL3", outputChannelPhaseL3) //
					.addProperty("powerPerPhase", powerPerPhase) //
					.addProperty("minimumSwitchingTime", hysteresis) //
					.addProperty("numberOfHeatingElements", numberOfHeatingElements.name());
			if (numberOfHeatingElements == NumberOfHeatingElements.TWO) {
				componentConfig //
						.addProperty("outputChannelPhaseL1HeatingElementB", outputChannelPhaseL1HeatingElementB) //
						.addProperty("outputChannelPhaseL2HeatingElementB", outputChannelPhaseL2HeatingElementB) //
						.addProperty("outputChannelPhaseL3HeatingElementB", outputChannelPhaseL3HeatingElementB) //
						.addProperty("powerPerPhaseHeatingElementB", powerPerPhaseHeatingElementB) //
						.addProperty("distribution", distribution.name());
			} else if (t != ConfigurationTarget.VALIDATE) {
				this.componentManager.getEdgeConfig().getComponent(heatingElementId).ifPresent(component -> {
					for (final var property : List.of("outputChannelPhaseL1HeatingElementB",
							"outputChannelPhaseL2HeatingElementB", "outputChannelPhaseL3HeatingElementB")) {
						if (component.getProperty(property).isPresent()) {
							componentConfig.add(property, JsonNull.INSTANCE);
						}
					}
				});
			}
			componentConfig.onlyIf(t != ConfigurationTarget.VALIDATE, b -> b.addProperty("meter.id", tmpMeterId));

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(heatingElementId, alias, "Controller.IO.HeatingElement",
							componentConfig.build()) //
			);

			final var componentIdOfRelay = outputChannelPhaseL1.substring(0, outputChannelPhaseL1.indexOf('/'));
			final var appIdOfRelay = DependencyUtil.getInstanceIdOfAppWhichHasComponent(this.componentManager,
					componentIdOfRelay);

			if (appIdOfRelay != null) {
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
			}

			if (numberOfHeatingElements == NumberOfHeatingElements.TWO) {
				final var componentIdOfRelay2 = outputChannelPhaseL1HeatingElementB.substring(0,
						outputChannelPhaseL1HeatingElementB.indexOf('/'));
				if (!componentIdOfRelay2.equals(componentIdOfRelay)) {
					final var appIdOfRelay2 = DependencyUtil.getInstanceIdOfAppWhichHasComponent(this.componentManager,
							componentIdOfRelay2);

					if (appIdOfRelay2 != null) {
						dependencies.add(new DependencyDeclaration("RELAY2", //
								DependencyDeclaration.CreatePolicy.NEVER, //
								DependencyDeclaration.UpdatePolicy.NEVER, //
								DependencyDeclaration.DeletePolicy.NEVER, //
								DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ALL, //
								DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
								DependencyDeclaration.AppDependencyConfig.create() //
										.setSpecificInstanceId(appIdOfRelay2) //
										.build()) //
						);
					}
				}

			}

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
		// min. 3 relays for one heating element; 6 relays for 2 heating elements
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
}
