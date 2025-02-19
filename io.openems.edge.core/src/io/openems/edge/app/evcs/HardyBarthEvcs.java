package io.openems.edge.app.evcs;

import static io.openems.edge.core.appmanager.formly.enums.Wrappers.PANEL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Function;

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
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.evcs.HardyBarthEvcs.PropertyParent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.host.Host;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.HostSupplier;
import io.openems.edge.core.appmanager.InterfaceConfiguration;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration.SchedulerComponent;
import io.openems.edge.core.appmanager.formly.Case;
import io.openems.edge.core.appmanager.formly.DefaultValueOptions;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.builder.FormlyBuilder;
import io.openems.edge.core.appmanager.formly.expression.StringExpression;

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
      "PHASE_ROTATION":"L1_L2_L3",
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
public class HardyBarthEvcs
		extends AbstractOpenemsAppWithProps<HardyBarthEvcs, PropertyParent, Parameter.BundleParameter>
		implements OpenemsApp, HostSupplier {

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
		NUMBER_OF_CHARGING_STATIONS(AppDef.copyOfGeneric(EvcsProps.numberOfChargePoints(2))), //
		WRAPPER_FIRST_CHARGE_POINT(AppDef.of(HardyBarthEvcs.class) //
				.setTranslatedLabel("App.Evcs.chargingStation.label", 1)
				.setField(JsonFormlyUtil::buildFieldGroupFromNameable, (app, property, l, parameter, field) -> {
					field.addWrapper(PANEL) //
							.setFieldGroup(SubPropertyFirstChargepoint.fields(app, l, parameter)) //
							.setLabelExpression(Exp.ifElse(
									Exp.currentModelValue(Property.NUMBER_OF_CHARGING_STATIONS)
											.equal(Exp.staticValue(1)),
									StringExpression.of(""), //
									StringExpression.of(TranslationUtil.getTranslation(parameter.bundle,
											"App.Evcs.chargingStation.label", 1))))
							.hideKey(); //
				})), //
		WRAPPER_SECOND_CHARGE_POINT(AppDef.of(HardyBarthEvcs.class) //
				.setTranslatedLabel("App.Evcs.chargingStation.label", 2)
				.setField(JsonFormlyUtil::buildFieldGroupFromNameable, (app, property, l, parameter, field) -> {
					field.addWrapper(PANEL) //
							.setFieldGroup(SubPropertySecondChargepoint.fields(app, l, parameter)) //
							.onlyShowIf(Exp.currentModelValue(NUMBER_OF_CHARGING_STATIONS) //
									.equal(Exp.staticValue(2)))
							.hideKey();
				})), //
		MAX_HARDWARE_POWER_ACCEPT_PROPERTY(AppDef.of() //
				.setAllowedToSave(false)), //
		MAX_HARDWARE_POWER(AppDef.copyOfGeneric(EvcsProps.clusterMaxHardwarePower(MAX_HARDWARE_POWER_ACCEPT_PROPERTY),
				def -> def //
						.setDefaultValue(0) //
						.wrapField((app, property, l, parameter, field) -> {
							final var existingEvcs = EvcsProps.getEvcsComponents(app.componentUtil);

							if (existingEvcs.isEmpty()) {
								field.onlyShowIf(Exp.currentModelValue(NUMBER_OF_CHARGING_STATIONS) //
										.equal(Exp.staticValue(2)));
								return;
							}
							field.onlyShowIf(Exp.currentModelValue(NUMBER_OF_CHARGING_STATIONS) //
									.equal(Exp.staticValue(2)) //
									.or(existingEvcs.stream().map(OpenemsComponent::id) //
											.map(Exp::staticValue) //
											.collect(Exp.toArrayExpression()) //
											.every(i -> Exp.currentModelValue(EVCS_ID).notEqual(i))));
						}))), //
		PHASE_ROTATION(AppDef.copyOfGeneric(EvcsProps.phaseRotation())), //
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
				.setRequired(true) //
				.setDefaultValue((app, property, l, parameter) -> //
				new JsonPrimitive(TranslationUtil.getTranslation(parameter.bundle(), "App.Evcs.HardyBarth.alias.value", //
						TranslationUtil.getTranslation(parameter.bundle(), "right")))) //
				.wrapField((app, property, l, parameter, field) -> field
						.setDefaultValueCases(new DefaultValueOptions(Property.NUMBER_OF_CHARGING_STATIONS, //
								new Case(1, app.getName(l)), //
								new Case(2, TranslationUtil.getTranslation(parameter.bundle(), //
										"App.Evcs.HardyBarth.alias.value", //
										TranslationUtil.getTranslation(parameter.bundle(), "right"))))))), //
		IP(AppDef.copyOfGeneric(CommunicationProps.excludingIp()) //
				.setDefaultValue("192.168.25.30") //
				.setAutoGenerateField(false) //
				.setRequired(true)), //
		;

		private final AppDef<? super HardyBarthEvcs, ? super Nameable, ? super BundleParameter> def;

		private SubPropertyFirstChargepoint(
				AppDef<? super HardyBarthEvcs, ? super Nameable, ? super BundleParameter> def) {
			this.def = def;
		}

		/**
		 * Gets the {@link AppDef}.
		 * 
		 * @return the {@link AppDef}
		 */
		public AppDef<? super HardyBarthEvcs, ? super Nameable, ? super BundleParameter> def() {
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
				new JsonPrimitive(TranslationUtil.getTranslation(parameter.bundle(), "App.Evcs.HardyBarth.alias.value", //
						TranslationUtil.getTranslation(parameter.bundle(), "left")))) //
				.setRequired(true)), //
		IP_CP_2(AppDef.copyOfGeneric(CommunicationProps.excludingIp()) //
				.setDefaultValue("192.168.25.31") //
				.setAutoGenerateField(false) //
				.setRequired(true)), //
		;

		private final AppDef<? super HardyBarthEvcs, ? super Nameable, ? super BundleParameter> def;

		private SubPropertySecondChargepoint(
				AppDef<? super HardyBarthEvcs, ? super Nameable, ? super BundleParameter> def) {
			this.def = def;
		}

		/**
		 * Gets the {@link AppDef}.
		 * 
		 * @return the {@link AppDef}
		 */
		public AppDef<? super HardyBarthEvcs, ? super Nameable, ? super BundleParameter> def() {
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

	private final Host host;

	@Activate
	public HardyBarthEvcs(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil, @Reference Host host) {
		super(componentManager, componentContext, cm, componentUtil);
		this.host = host;
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
				maxHardwarePowerPerPhase = OptionalInt.of(this.getInt(p, Property.MAX_HARDWARE_POWER));
			}

			final var schedulerIds = new ArrayList<SchedulerComponent>();

			final var alias = this.getString(p, l, SubPropertyFirstChargepoint.ALIAS);
			final var ip = this.getString(p, l, SubPropertyFirstChargepoint.IP);
			final var evcsId = this.getId(t, p, Property.EVCS_ID);
			final var phaseRotation = this.getString(p, l, Property.PHASE_ROTATION);
			final var ctrlEvcsId = this.getId(t, p, Property.CTRL_EVCS_ID);
			schedulerIds.add(new SchedulerComponent(ctrlEvcsId, "Controller.Evcs", this.getAppId()));

			final var factorieId = "Evcs.HardyBarth";
			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(evcsId, alias, factorieId, JsonUtils.buildJsonObject() //
							.addProperty("ip", ip) //
							.addPropertyIfNotNull("phaseRotation", phaseRotation) //
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
				schedulerIds.add(new SchedulerComponent(ctrlEvcsIdCp2, "Controller.Evcs", this.getAppId()));

				components.add(new EdgeConfig.Component(evcsIdCp2, aliasCp2, factorieId, JsonUtils.buildJsonObject() //
						.addProperty("ip", ipCp2) //
						.addPropertyIfNotNull("phaseRotation", phaseRotation) //
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

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.schedulerByCentralOrder(schedulerIds)) //
					.throwingOnlyIf(ip.startsWith("192.168.25."),
							b -> b.addTask(Tasks.staticIp(new InterfaceConfiguration("eth0") //
									.addIp("Evcs", "192.168.25.10/24")))) //
					.addDependencies(clusterDependency) //
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

	@Override
	public Host getHost() {
		return this.host;
	}

}
