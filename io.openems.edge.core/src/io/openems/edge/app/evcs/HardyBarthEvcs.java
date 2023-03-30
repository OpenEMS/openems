package io.openems.edge.app.evcs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.evcs.HardyBarthEvcs.PropertyParent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.InterfaceConfiguration;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.JsonFormlyUtil.Case;
import io.openems.edge.core.appmanager.JsonFormlyUtil.DefaultValueOptions;
import io.openems.edge.core.appmanager.JsonFormlyUtil.ExpressionBuilder;
import io.openems.edge.core.appmanager.JsonFormlyUtil.ExpressionBuilder.Operator;
import io.openems.edge.core.appmanager.JsonFormlyUtil.FormlyBuilder;
import io.openems.edge.core.appmanager.JsonFormlyUtil.Wrappers;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;

/**
 * Describes a Hardy Barth evcs App.
 *
 * <pre>
  {
    "appId":"App.Evcs.HardyBarth",
    "alias":"eCharge Hardy Barth Ladestation",
    "instanceId": UUID,
    "image": base64,
    "properties":{
      "EVCS_ID": "evcs0",
      "CTRL_EVCS_ID": "ctrlEvcs0",
      "IP": "192.168.25.30",
      "NUMBER_OF_CHARGING_STATIONS": 1,
      "EVCS_ID_CP_2": "evcs0",
      "CTRL_EVCS_ID_CP_2": "ctrlEvcs0",
      "ALIAS_CP_2": "eCharge Hardy Barth Ladestation - Rechts",
      "IP_CP_2": "192.168.25.31"
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.Evcs.HardyBarth")
public class HardyBarthEvcs extends
		AbstractOpenemsAppWithProps<HardyBarthEvcs, PropertyParent, Parameter.BundleParameter> implements OpenemsApp {

	public interface PropertyParent extends Nameable, Type<PropertyParent, HardyBarthEvcs, Parameter.BundleParameter> {

	}

	public static enum Property implements PropertyParent {
		// Component-IDs
		EVCS_ID(AppDef.componentId("evcs0")), //
		CTRL_EVCS_ID(AppDef.componentId("ctrlEvcs0")), //
		EVCS_ID_CP_2(AppDef.componentId("evcs0")), //
		CTRL_EVCS_ID_CP_2(AppDef.componentId("ctrlEvcs0")), //
		// Properties
		// TODO maybe make this immutable after first installation?
		NUMBER_OF_CHARGING_STATIONS(AppDef.of(HardyBarthEvcs.class) //
				.setTranslatedLabelWithAppPrefix(".numberOfChargingStations.label") //
				.setDefaultValue(1) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> //
				field.setOptions(Lists.newArrayList(1, 2), JsonPrimitive::new, JsonPrimitive::new))), //
		WRAPPER_FIRST_CHARGE_POINT(AppDef.of(HardyBarthEvcs.class) //
				.setTranslatedLabel("App.Evcs.chargingStation.label", 1)
				.setField(JsonFormlyUtil::buildFieldGroupFromNameable, (app, property, l, parameter, field) -> {
					field.addWrapper(Wrappers.PANEL) //
							.setFieldGroup(SubPropertyFirstChargepoint.fields(app, l, parameter)) //
							.setLabelExpression(
									ExpressionBuilder.of(Property.NUMBER_OF_CHARGING_STATIONS, Operator.EQ, "1"), //
									"", TranslationUtil.getTranslation(parameter.bundle,
											"App.Evcs.chargingStation.label", 1))
							.hideKey(); //
				})), //
		WRAPPER_SECOND_CHARGE_POINT(AppDef.of(HardyBarthEvcs.class) //
				.setTranslatedLabel("App.Evcs.chargingStation.label", 2)
				.setField(JsonFormlyUtil::buildFieldGroupFromNameable, (app, property, l, parameter, field) -> {
					field.addWrapper(Wrappers.PANEL) //
							.setFieldGroup(SubPropertySecondChargepoint.fields(app, l, parameter)) //
							.onlyShowIfValueEquals(NUMBER_OF_CHARGING_STATIONS, "2") //
							.hideKey();
				})), //
		MAX_HARDWARE_POWER_ACCEPT_PROPERTY(AppDef.of() //
				.setAllowedToSave(false)), //
		MAX_HARDWARE_POWER(AppDef.<HardyBarthEvcs, PropertyParent, Parameter.BundleParameter, //
				HardyBarthEvcs, Nameable, Parameter.BundleParameter>copyOfGeneric(
						EvcsProps.clusterMaxHardwarePower(MAX_HARDWARE_POWER_ACCEPT_PROPERTY)) //
				.setDefaultValue(0) //
				.wrapField((app, property, l, parameter, field) -> {
					final var existingEvcs = app.componentUtil.getEnabledComponentsOfStartingId("evcs").stream() //
							.filter(t -> !t.id().startsWith("evcsCluster")) //
							.collect(Collectors.toList());
					final var expressionForSingleUpdate = ExpressionBuilder.ofNotIn(EVCS_ID,
							existingEvcs.stream().map(OpenemsComponent::id) //
									.toArray(String[]::new));
					field.onlyShowIf(ExpressionBuilder.of(NUMBER_OF_CHARGING_STATIONS, Operator.EQ, "2") //
							.or(expressionForSingleUpdate));
				})), //
		;

		private final AppDef<? super HardyBarthEvcs, ? super PropertyParent, ? super BundleParameter> def;

		private Property(AppDef<? super HardyBarthEvcs, ? super PropertyParent, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<PropertyParent, HardyBarthEvcs, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super HardyBarthEvcs, ? super PropertyParent, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<HardyBarthEvcs>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	public enum SubPropertyFirstChargepoint implements PropertyParent {
		ALIAS(AppDef.copyOfGeneric(CommonProps.alias()) //
				.setAutoGenerateField(false) //
				.setDefaultValue((app, property, l, parameter) -> //
				new JsonPrimitive(TranslationUtil.getTranslation(parameter.bundle, "App.Evcs.HardyBarth.alias.value", //
						TranslationUtil.getTranslation(parameter.bundle, "right")))) //
				.wrapField((app, property, l, parameter, field) -> field.isRequired(true) //
						.setDefaultValueCases(new DefaultValueOptions(Property.NUMBER_OF_CHARGING_STATIONS, //
								new Case(1, app.getName(l)), //
								new Case(2, TranslationUtil.getTranslation(parameter.bundle, //
										"App.Evcs.HardyBarth.alias.value", //
										TranslationUtil.getTranslation(parameter.bundle, "right"))))))), //
		IP(AppDef.copyOfGeneric(CommunicationProps.ip()) //
				.setDefaultValue("192.168.25.30") //
				.setAutoGenerateField(false) //
				.wrapField((app, property, l, parameter, field) -> field.isRequired(true))), //
		;

		private final AppDef<OpenemsApp, Nameable, BundleParameter> def;

		private SubPropertyFirstChargepoint(AppDef<OpenemsApp, Nameable, BundleParameter> def) {
			this.def = def;
		}

		/**
		 * Gets the {@link AppDef}.
		 * 
		 * @return the {@link AppDef}
		 */
		public AppDef<OpenemsApp, Nameable, BundleParameter> def() {
			return this.def;
		}

		/**
		 * Gets the fields of this enum.
		 * 
		 * @param app   the input {@link OpenemsApp}
		 * @param l     the {@link Language}
		 * @param param the parameter values
		 * @return the input fields
		 */
		public static JsonArray fields(HardyBarthEvcs app, Language l, BundleParameter param) {
			return Arrays.stream(SubPropertyFirstChargepoint.values()) //
					.map(prop -> prop.def.getField().get(app, prop, l, param)) //
					.map(FormlyBuilder::build) //
					.collect(JsonUtils.toJsonArray());
		}

		@Override
		public Function<GetParameterValues<HardyBarthEvcs>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

		@Override
		public Type<PropertyParent, HardyBarthEvcs, BundleParameter> self() {
			return this;
		}

	}

	public enum SubPropertySecondChargepoint implements PropertyParent {
		ALIAS_CP_2(AppDef.copyOfGeneric(CommonProps.alias()) //
				.setAutoGenerateField(false) //
				.setDefaultValue((app, property, l, parameter) -> //
				new JsonPrimitive(TranslationUtil.getTranslation(parameter.bundle, "App.Evcs.HardyBarth.alias.value", //
						TranslationUtil.getTranslation(parameter.bundle, "left")))) //
				.wrapField((app, property, l, parameter, field) -> field.isRequired(true))), //
		IP_CP_2(AppDef.copyOfGeneric(CommunicationProps.ip()) //
				.setDefaultValue("192.168.25.31") //
				.setAutoGenerateField(false) //
				.wrapField((app, property, l, parameter, field) -> field.isRequired(true))), //
		;

		private final AppDef<OpenemsApp, Nameable, BundleParameter> def;

		private SubPropertySecondChargepoint(AppDef<OpenemsApp, Nameable, BundleParameter> def) {
			this.def = def;
		}

		/**
		 * Gets the {@link AppDef}.
		 * 
		 * @return the {@link AppDef}
		 */
		public AppDef<OpenemsApp, Nameable, BundleParameter> def() {
			return this.def;
		}

		/**
		 * Gets the fields of this enum.
		 * 
		 * @param app   the input {@link OpenemsApp}
		 * @param l     the {@link Language}
		 * @param param the parameter values
		 * @return the input fields
		 */
		public static JsonArray fields(HardyBarthEvcs app, Language l, BundleParameter param) {
			return Arrays.stream(SubPropertySecondChargepoint.values()) //
					.map(prop -> prop.def.getField().get(app, prop, l, param)) //
					.map(FormlyBuilder::build) //
					.collect(JsonUtils.toJsonArray());
		}

		@Override
		public Function<GetParameterValues<HardyBarthEvcs>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

		@Override
		public Type<PropertyParent, HardyBarthEvcs, BundleParameter> self() {
			return this;
		}

	}

	@Activate
	public HardyBarthEvcs(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, //
			Map<PropertyParent, JsonElement>, //
			Language, //
			AppConfiguration, //
			OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var controllerAlias = TranslationUtil.getTranslation(AbstractOpenemsApp.getTranslationBundle(l),
					"App.Evcs.controller.alias");

			final var numberOfChargingStations = this.getInt(p, Property.NUMBER_OF_CHARGING_STATIONS);
			if (numberOfChargingStations <= 0 || numberOfChargingStations > 2) {
				throw new OpenemsException("Number of charging stations can only be 0 < n <= 2.");
			}

			var maxHardwarePowerPerPhase = OptionalInt.empty();
			if (p.containsKey(Property.MAX_HARDWARE_POWER)) {
				maxHardwarePowerPerPhase = OptionalInt
						.of(this.getInt(p, Property.MAX_HARDWARE_POWER) / EvcsProps.NUMBER_OF_PHASES);
			}

			final var schedulerIds = new ArrayList<String>();

			final var alias = this.getString(p, l, SubPropertyFirstChargepoint.ALIAS);
			final var ip = this.getString(p, l, SubPropertyFirstChargepoint.IP);
			final var evcsId = this.getId(t, p, Property.EVCS_ID);
			final var ctrlEvcsId = this.getId(t, p, Property.CTRL_EVCS_ID);
			schedulerIds.add(ctrlEvcsId);

			final var factorieId = "Evcs.HardyBarth";
			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(evcsId, alias, factorieId, JsonUtils.buildJsonObject() //
							.addProperty("ip", ip) //
							.build()), //
					new EdgeConfig.Component(ctrlEvcsId, controllerAlias, "Controller.Evcs", JsonUtils.buildJsonObject() //
							.addProperty("evcs.id", evcsId) //
							.build())//
			);
			final List<DependencyDeclaration> clusterDependency;
			if (numberOfChargingStations == 2) {
				final var aliasCp2 = this.getString(p, l, SubPropertySecondChargepoint.ALIAS_CP_2);
				final var ipCp2 = this.getString(p, l, SubPropertySecondChargepoint.IP_CP_2);
				final var evcsIdCp2 = this.getId(t, p, Property.EVCS_ID_CP_2);
				final var ctrlEvcsIdCp2 = this.getId(t, p, Property.CTRL_EVCS_ID_CP_2);
				schedulerIds.add(ctrlEvcsIdCp2);

				components.add(new EdgeConfig.Component(evcsIdCp2, aliasCp2, factorieId, JsonUtils.buildJsonObject() //
						.addProperty("ip", ipCp2) //
						.build()));
				components.add(new EdgeConfig.Component(ctrlEvcsIdCp2, controllerAlias, "Controller.Evcs",
						JsonUtils.buildJsonObject() //
								.addProperty("evcs.id", evcsIdCp2) //
								.build()));
				clusterDependency = EvcsCluster.dependency(t, this.componentManager, this.componentUtil,
						maxHardwarePowerPerPhase, evcsId, evcsIdCp2);
			} else {
				var removeIds = Collections.<String>emptyList();
				if (p.containsKey(Property.EVCS_ID_CP_2)) {
					removeIds = Lists.newArrayList(this.getId(t, p, Property.EVCS_ID_CP_2));
				}
				clusterDependency = EvcsCluster.dependency(t, this.componentManager, this.componentUtil,
						maxHardwarePowerPerPhase, removeIds, evcsId);
			}

			final var ips = Lists.newArrayList(//
					new InterfaceConfiguration("eth0") //
							.addIp("Evcs", "192.168.25.10/24") //
			);

			schedulerIds.add("ctrlBalancing0");
			return new AppConfiguration(//
					components, //
					schedulerIds, //
					ip.startsWith("192.168.25.") ? ips : null, //
					clusterDependency //
			);
		};
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/produkte/fems/fems-app-echarge-hardy-barth-ladestation/") //
				.build();
	}

	@Override
	protected PropertyParent[] propertyValues() {
		return ImmutableList.<PropertyParent>builder() //
				.addAll(Arrays.asList(Property.values())) //
				.addAll(Arrays.asList(SubPropertyFirstChargepoint.values())) //
				.addAll(Arrays.asList(SubPropertySecondChargepoint.values())) //
				.build().toArray(PropertyParent[]::new);
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	protected HardyBarthEvcs getApp() {
		return this;
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.EVCS };
	}

}
