package io.openems.edge.app.heat;

import static io.openems.common.channel.Unit.SECONDS;
import static io.openems.common.channel.Unit.WATT;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.app.common.props.RelayProps.createPhaseInformation;
import static io.openems.edge.app.common.props.RelayProps.phaseGroup;
import static io.openems.edge.app.common.props.RelayProps.relayContactDef;
import static io.openems.edge.core.appmanager.TranslationUtil.translate;
import static io.openems.edge.core.appmanager.formly.builder.selectgroup.Option.buildOption;
import static io.openems.edge.core.appmanager.formly.builder.selectgroup.OptionGroup.buildOptionGroup;
import static io.openems.edge.core.appmanager.formly.enums.InputType.NUMBER;
import static io.openems.edge.core.appmanager.validator.Checkables.checkRelayCount;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.MeterType;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.common.props.PropsUtil;
import io.openems.edge.app.common.props.RelayProps;
import io.openems.edge.app.common.props.RelayProps.RelayContactInformation;
import io.openems.edge.app.common.props.RelayProps.RelayContactInformationProvider;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.app.enums.TranslatableEnum;
import io.openems.edge.app.heat.HeatingElement.HeatingElementParameter;
import io.openems.edge.app.heat.HeatingElement.Property;
import io.openems.edge.app.meter.EastronMeter;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ComponentUtil.PreferredRelay;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.dependency.DependencyUtil;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.enums.Operator;
import io.openems.edge.core.appmanager.formly.expression.BooleanExpression;
import io.openems.edge.core.appmanager.formly.expression.StringExpression;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;
import io.openems.edge.core.appmanager.validator.relaycount.CheckRelayCountFilters;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * Describes a App for a RTU Heating Element.
 *
 * <pre>
 * {
 * "appId":"App.Heat.HeatingElement",
 * "alias":"Heizstab",
 * "instanceId": UUID,
 * "image": base64,
 * "properties":{
 * "CTRL_IO_HEATING_ELEMENT_ID": "ctrlIoHeatingElement0",
 * "OUTPUT_CHANNEL_PHASE_L1": "io0/Relay1",
 * "OUTPUT_CHANNEL_PHASE_L2": "io0/Relay2",
 * "OUTPUT_CHANNEL_PHASE_L3": "io0/Relay3",
 * "POWER_PER_PHASE": 2000,
 * "HYSTERESIS": 60,
 * "IS_ELEMENT_MEASURED": false,
 *
 * },
 * "dependencies": [
 * {
 * "key": "RELAY",
 * "instanceId": UUID
 * }
 * ],
 * "appDescriptor": {
 * "websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
 * }
 * }
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

	private final AppManagerUtil appManagerUtil;

	private enum HeatingElementMeterIntegration implements TranslatableEnum {
		INTERN("App.Heat.HeatingElement.internal"), EXTERN("App.Heat.HeatingElement.external");

		private final String translationKey;

		private HeatingElementMeterIntegration(String translationKey) {
			this.translationKey = translationKey;
		}

		@Override
		public final String getTranslation(Language l) {
			final var bundle = AbstractOpenemsApp.getTranslationBundle(l);
			return TranslationUtil.getTranslation(bundle, this.translationKey);
		}
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
		POWER_PER_PHASE(AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".powerPerPhase.label") //
				.setTranslatedDescriptionWithAppPrefix(".powerPerPhase.description") //
				.setDefaultValue(2000) //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildInput, (app, property, l, parameter, field) -> {
					field.setInputType(NUMBER)//
							.setUnit(WATT, l)//
							.setMin(0);

				}))), //
		HYSTERESIS(AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".hysteresis.label") //
				.setTranslatedDescriptionWithAppPrefix(".hysteresis.description") //
				.setDefaultValue(60) //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildInput, (app, property, l, parameter, field) -> {
					field.setInputType(NUMBER)//
							.setUnit(SECONDS, l)//
							.setMin(0);
				})//
				.bidirectional(CTRL_IO_HEATING_ELEMENT_ID, "minimumSwitchingTime", //
						ComponentManagerSupplier::getComponentManager))), //
		IS_ELEMENT_MEASURED(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".measured")//
				.setDefaultValue(false)//
				.setField(JsonFormlyUtil::buildCheckboxFromNameable)//
				.setRequired(true))), //
		HOW_MEASURED(AppDef.copyOfGeneric(CommonProps.defaultDef(), de -> de //
				.setTranslatedLabelWithAppPrefix(".howMeasured") //
				.setDefaultValue(HeatingElementMeterIntegration.EXTERN) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					if (PropsUtil.isHomeInstalled(app.appManagerUtil)
							&& app.appManagerUtil.getInstantiatedAppsOf("App.FENECON.Home").isEmpty()) {
						field.setOptions(OptionsFactory.of(HeatingElementMeterIntegration.class), l);
					} else {
						field.setOptions(OptionsFactory.of(HeatingElementMeterIntegration.class, HeatingElementMeterIntegration.INTERN), l);
					}
					field.onlyShowIf(Exp.currentModelValue(IS_ELEMENT_MEASURED).notNull());
				}))), //
		METER_ID(AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".meter") //
				.setField(JsonFormlyUtil::buildSelectGroupFromNameable, (app, property, l, parameter, field) -> {
					List<String> ignoreIds = new ArrayList<String>(ComponentUtil.CORE_COMPONENT_IDS);
					field.addOption(buildOptionGroup("Meter",
							translate(parameter.bundle(), "App.Meter.consumtionMeter"))
							.addOptions(
									getExternMeter(app,
											new ArrayList<>(List.of(
													getMeterIdFromAlias(app,
															TranslationUtil.getTranslation(parameter.bundle(),
																	"App.IntegratedSystem.emergencyMeter.alias")),
													getMeterIdFromAlias(app,
															TranslationUtil.getTranslation(parameter.bundle(),
																	"App.Heat.HeatingElement.internalMeterAlias"))))),
									(meter) -> buildOption(meter.id())
											.setTitleExpression(getTitleExpression(app, parameter, meter, ignoreIds))
											.onlyIf(meterUsed(app, meter.id(), ignoreIds),
													b -> b.setDisabledExpression(
															isMeterNotFromCurrentHeatingElement(meter)))
											.build())
							.build());
					field.setMissingOptionsText(translate(parameter.bundle(), "App.Heat.HeatingElement.noMeter"));
					field.onlyShowIf(checkMeasuredAndExtern()).build();
				}).setDefaultValue((app, property, l, parameter) -> getExternDefaultValue(app, parameter))//
				.setTranslatedLabel("meterId.label")//
				.setTranslatedDescription("meterId.description").setRequired(true))), //
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

				return new HeatingElementParameter(//
						createResourceBundle(t.language), //
						createPhaseInformation(t.app.componentUtil, 3, //
								List.of(RelayProps.feneconHomeFilter(t.language, isHomeInstalled, true),
										RelayProps.gpioFilter()), //
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
			final var howMeasured = this.getString(p, Property.HOW_MEASURED);
			var meterId = "";

			final var dependencies = new ArrayList<DependencyDeclaration>();

			if (isElementMeasured) {

				if (howMeasured.equals(HeatingElementMeterIntegration.INTERN.toString())) {

					if (isInternMeterUsedByHeatingElement(this.getApp(),
							TranslationUtil.getTranslation(getTranslationBundle(l),
									"App.Heat.HeatingElement.internalMeterAlias"))
							&& t.isAddOrUpdate()
							&& !hasHeatingElementComponentInternMeter(heatingElementId, this.getApp(), l)) {
						throw new OpenemsNamedException(OpenemsError.GENERIC, "Intern meter already in use");
					}

					meterId = this.getNextMeterId(this.getApp(), TranslationUtil.getTranslation(getTranslationBundle(l),
							"App.Heat.HeatingElement.internalMeterAlias"));
					final var meterProperties = buildJsonObject()
							.addProperty(EastronMeter.Property.MODBUS_ID.name(), "modbus0")
							.addProperty(EastronMeter.Property.MODBUS_UNIT_ID.name(), 3)
							.addProperty(EastronMeter.Property.TYPE.name(), MeterType.CONSUMPTION_METERED.toString())
							.build();

					dependencies.add(new DependencyDeclaration("INTERN_METER", //
							DependencyDeclaration.CreatePolicy.IF_NOT_EXISTING, //
							DependencyDeclaration.UpdatePolicy.ALWAYS, //
							DependencyDeclaration.DeletePolicy.ALWAYS, //
							DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ONLY_UNCONFIGURED_PROPERTIES, //
							DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
							DependencyDeclaration.AppDependencyConfig.create() //
									.setAppId("App.Meter.Eastron") //
									.setAlias(translate(getTranslationBundle(l),
											"App.Heat.HeatingElement.internalMeterAlias")) //
									.setInitialProperties(buildJsonObject(meterProperties.deepCopy()) //
											.addProperty(EastronMeter.Property.METER_ID.name(), meterId).build())
									.setProperties(meterProperties) //
									.build()) //
					);
				} else {
					meterId = this.getString(p, l, Property.METER_ID);
					final var appIdOfMeter = DependencyUtil.getInstanceIdOfAppWhichHasComponent(this.componentManager,
							meterId);

					if (appIdOfMeter != null) {
						dependencies.add(new DependencyDeclaration("EXTERN_METER", //
								DependencyDeclaration.CreatePolicy.NEVER, //
								DependencyDeclaration.UpdatePolicy.NEVER, //
								DependencyDeclaration.DeletePolicy.NEVER, //
								DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ALL, //
								DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
								DependencyDeclaration.AppDependencyConfig.create() //
										.setSpecificInstanceId(appIdOfMeter) //
										.build()) //
						);
					}
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
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.setWebsiteUrl(oem.getAppWebsiteUrl(this.getAppId())) //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.HEAT };
	}

	@Override
	public ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create() //
				.setInstallableCheckableConfigs(checkRelayCount(3, CheckRelayCountFilters.feneconHome(true),
						CheckRelayCountFilters.deviceHardware()));
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

	private static BooleanExpression checkMeasuredAndExtern() {
		BooleanExpression condition1 = Exp.currentModelValue(Property.IS_ELEMENT_MEASURED).notNull();
		BooleanExpression condition2 = BooleanExpression.of(Exp.currentModelValue(Property.HOW_MEASURED), Operator.EQ,
				Exp.staticValue(HeatingElementMeterIntegration.EXTERN));
		return condition1.and(condition2);
	}

	private static boolean meterUsed(HeatingElement app, String meterId, List<String> ignoreIds) {
		final var componentUtil = app.getComponentUtil();
		ignoreIds.add(meterId);
		return componentUtil.anyComponentUses(meterId, ignoreIds);
	}

	private static List<ElectricityMeter> getExternMeter(HeatingElement app, List<String> meterIdsToNotInclude) {
		final var componentUtil = app.getComponentUtil();

		var components = componentUtil.getEnabledComponentsOfType(ElectricityMeter.class).stream().filter(meter -> {
			var toIgnore = meterIdsToNotInclude.stream().anyMatch(m -> meter.id().equals(m));
			return meter.getMeterType() == MeterType.CONSUMPTION_METERED && !toIgnore;
		}).sorted((Comparator.comparingInt(meter -> {
			Matcher m = Pattern.compile("\\d+").matcher(meter.id());
			if (m.find()) {
				return Integer.parseInt(m.group());
			}
			return 0;
		})));

		return components.toList();
	}

	private static boolean isInternMeterUsedByHeatingElement(HeatingElement app, String meterAlias) {
		var heatingElementComponents = app.componentUtil.getEnabledComponentsOfStartingId("ctrlIoHeatingElement");
		for (var heatingElement : heatingElementComponents) {
			var meterId = heatingElement.getComponentContext().getProperties().get("meter.id");
			if (meterId != null) {
				var meter = app.componentUtil.getComponent(meterId.toString(), "Meter.Microcare.SDM630");
				if (meter.isPresent() && meterAlias.equals(meter.get().getAlias())) {
					return true;
				}
			}
		}
		return false;
	}

	private static Optional<ElectricityMeter> findMeterId(HeatingElement app, String meterAlias) {
		var meterList = app.componentUtil.getEnabledComponentsOfType(ElectricityMeter.class);
		return meterList.stream().filter(m -> m.alias().equals(meterAlias)).findFirst();
	}

	private static String getMeterIdFromAlias(HeatingElement app, String meterAlias) {
		Optional<ElectricityMeter> optionalMeter = findMeterId(app, meterAlias);
		if (optionalMeter.isPresent()) {
			return optionalMeter.get().id();
		}
		return "";
	}

	private String getNextMeterId(HeatingElement app, String meterAlias) {
		var meterId = getMeterIdFromAlias(app, meterAlias);
		if (!meterId.isEmpty()) {
			return meterId;
		}
		return this.componentUtil.getNextAvailableId("meter", this.componentManager.getAllComponents().stream()
				.map(OpenemsComponent::id).filter(id -> id.startsWith("meter")).toList());
	}

	private static JsonElement getExternDefaultValue(HeatingElement app, HeatingElementParameter parameter) {
		var meterList = getExternMeter(app,
				new ArrayList<>(List.of(
						getMeterIdFromAlias(app,
								TranslationUtil.getTranslation(parameter.bundle(),
										"App.IntegratedSystem.emergencyMeter.alias")),
						getMeterIdFromAlias(app, TranslationUtil.getTranslation(parameter.bundle(),
								"App.Heat.HeatingElement.internalMeterAlias")))));
		var meter = meterList.stream()
				.filter(m -> !meterUsed(app, m.id(), new ArrayList<String>(ComponentUtil.CORE_COMPONENT_IDS)))
				.findFirst();
		return meter.isPresent() ? new JsonPrimitive(meter.get().id()) : JsonNull.INSTANCE;
	}

	private static StringExpression getTitleExpression(HeatingElement app, HeatingElementParameter parameter,
			ElectricityMeter meter, List<String> ignoreIds) {
		ignoreIds.add(meter.id());
		var componentUsing = app.componentUtil.getComponentUsing(meter.id(), ignoreIds).stream().findFirst();
		String showingString = "";
		if (componentUsing.isPresent()) {
			var componentString = componentUsing.get().alias().isEmpty() ? componentUsing.get().id()
					: componentUsing.get().alias();
			showingString = "\\'" + componentString + "\\'";
		}
		String display = meter.id();
		if (!meter.alias().isEmpty()) {
			display += " - " + meter.alias();
		}
		StringExpression used = StringExpression.of(display + " - " + TranslationUtil.getTranslation(parameter.bundle(),
				"App.Heat.HeatingElement.meterAlreadyUsed", showingString));
		StringExpression notUsed = StringExpression.of(display);
		if (meterUsed(app, meter.id(), ignoreIds)) {
			BooleanExpression exp = isMeterNotFromCurrentHeatingElement(meter);
			return Exp.ifElse(exp, used, notUsed);
		}
		return notUsed;
	}

	private static BooleanExpression isMeterNotFromCurrentHeatingElement(ElectricityMeter meter) {
		return Exp.initialModelValue(Nameable.of("METER_ID")).notEqual(Exp.staticValue(meter.id()));
	}

	private static boolean hasHeatingElementComponentInternMeter(String heatingElementId, HeatingElement app,
			Language l) {
		if (!heatingElementId.startsWith("ctrlIoHeatingElement")) {
			heatingElementId = "ctrlIoHeatingElement" + heatingElementId;
		}
		try {
			Dictionary<String, Object> props;
			props = app.componentManager.getComponent(heatingElementId).getComponentContext().getProperties();
			var meterId = props.get("meter.id");
			var internMeterId = getMeterIdFromAlias(app, TranslationUtil.getTranslation(getTranslationBundle(l),
					"App.Heat.HeatingElement.internalMeterAlias"));
			if (internMeterId.isEmpty()) {
				return false;
			}
			return meterId.toString().equals(internMeterId);
		} catch (OpenemsNamedException e) {
			return false;
		}
	}
}
