package io.openems.edge.app.evcs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.OptionalInt;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.evcs.AlpitronicEvcs.ParentProperty;
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
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.formly.Case;
import io.openems.edge.core.appmanager.formly.DefaultValueOptions;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.enums.Wrappers;
import io.openems.edge.core.appmanager.formly.expression.StringExpression;

/**
 * Describes a Alpitronic evcs app.
 *
 * <pre>
  {
    "appId":"App.Evcs.Alpitronic",
    "alias":"Alpitronic Ladestation",
    "instanceId": UUID,
    "image": base64,
    "properties":{
      "MODBUS_ID": "modbus0",
      "NUMBER_OF_CONNECTORS": 4,
      "IP":"192.168.25.11",
      "CP_ALIAS_[1-3]": "Alpitronic Ladestation - Ladepunkt [1-3]",
      "EVCS_ID_[0-3]": "evcs[0-3]",
      "CTRL_EVCS_ID[0-3]": "ctrlEvcs[0-3]"
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.Evcs.Alpitronic")
public class AlpitronicEvcs extends
		AbstractOpenemsAppWithProps<AlpitronicEvcs, ParentProperty, Parameter.BundleParameter> implements OpenemsApp {

	public static interface ParentProperty extends Type<ParentProperty, AlpitronicEvcs, Parameter.BundleParameter> {

	}

	private static final class ParentPropertyImpl extends
			Type.AbstractType<ParentProperty, AlpitronicEvcs, Parameter.BundleParameter> implements ParentProperty {

		public ParentPropertyImpl(String name,
				AppDef<? super AlpitronicEvcs, ? super ParentProperty, ? super BundleParameter> def) {
			super(name, def, Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle));
		}

	}

	public enum Property implements ParentProperty {
		// Component-IDs
		MODBUS_ID(AppDef.componentId("modbus0")), //
		// Properties
		NUMBER_OF_CONNECTORS(AppDef.copyOfGeneric(EvcsProps.numberOfChargePoints(4))),
		IP(AppDef.copyOfGeneric(CommunicationProps.ip()) //
				.setDefaultValue("192.168.1.100")), //
		MAX_HARDWARE_POWER_ACCEPT_PROPERTY(AppDef.of() //
				.setAllowedToSave(false)), //
		MAX_HARDWARE_POWER(AppDef.copyOfGeneric(//
				EvcsProps.clusterMaxHardwarePower(MAX_HARDWARE_POWER_ACCEPT_PROPERTY), def -> {
					def.wrapField((app, property, l, parameter, field) -> {
						final var existingEvcs = EvcsProps.getEvcsComponents(app.getComponentUtil());

						if (existingEvcs.isEmpty()) {
							field.onlyShowIf(Exp.currentModelValue(NUMBER_OF_CONNECTORS) //
									.greaterThanEqual(Exp.staticValue(2)));
							return;
						}
						final var expressionForSingleUpdate = existingEvcs.stream().map(OpenemsComponent::id) //
								.map(Exp::staticValue) //
								.collect(Exp.toArrayExpression())
								.every(v -> v.notEqual(Exp.currentModelValue(Nameable.of(EVCS_ID.apply(0)))));

						field.onlyShowIf(Exp.currentModelValue(NUMBER_OF_CONNECTORS) //
								.greaterThanEqual(Exp.staticValue(2)) //
								.or(expressionForSingleUpdate));
					}); //
				})), //
		;

		private static AppDef<//
				? super AlpitronicEvcs, //
				? super ParentProperty, //
				? super BundleParameter //
		> chargePointAlias(int number) {
			return AppDef.of(AlpitronicEvcs.class) //
					.setTranslatedLabel("App.Evcs.chargingStation.label", number) //
					.setDefaultValue((app, property, l, parameter) -> {
						return new JsonPrimitive(TranslationUtil.getTranslation(parameter.bundle(),
								"App.Evcs.Alpitronic.chargingStation.label", number));
					}) //
					.setField(JsonFormlyUtil::buildFieldGroupFromNameable, (app, property, l, parameter, field) -> {
						field.addWrapper(Wrappers.PANEL) //
								.hideKey() //
								.onlyIf(number == 1,
										b -> b.setLabelExpression(Exp.ifElse(
												Exp.currentModelValue(Property.NUMBER_OF_CONNECTORS) //
														.equal(Exp.staticValue(1)),
												StringExpression.of(""),
												StringExpression.of(TranslationUtil.getTranslation(parameter.bundle(),
														"App.Evcs.chargingStation.label", number)))))
								.onlyShowIf(Exp.currentModelValue(Property.NUMBER_OF_CONNECTORS)
										.greaterThanEqual(Exp.staticValue(number))) //
								.setFieldGroup(JsonUtils.buildJsonArray() //
										.add(CommonProps.alias().getField().get(app, property, l, parameter) //
												.setDefaultValue(TranslationUtil.getTranslation(parameter.bundle(),
														"App.Evcs.Alpitronic.chargingStation.label", number)) //
												.onlyIf(number == 1, b -> {
													b.setDefaultValueCases(new DefaultValueOptions(
															Property.NUMBER_OF_CONNECTORS, //
															buildFirstAliasDefaultCases(app, l, parameter.bundle())));
												}).build()) //
										.build());
					});
		}

		private static Case[] buildFirstAliasDefaultCases(AlpitronicEvcs app, Language l, ResourceBundle bundle) {
			return IntStream.rangeClosed(1, MAX_NUMBER_OF_CHARGEPOINTS) //
					.mapToObj(value -> {
						if (value == 1) {
							return new Case(1, app.getName(l));
						} else {
							return new Case(value, TranslationUtil.getTranslation(bundle,
									"App.Evcs.Alpitronic.chargingStation.label", 1));
						}
					}).toArray(Case[]::new);
		}

		private final AppDef<? super AlpitronicEvcs, ? super ParentProperty, ? super BundleParameter> def;

		private Property(AppDef<? super AlpitronicEvcs, ? super ParentProperty, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<ParentProperty, AlpitronicEvcs, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super AlpitronicEvcs, ? super ParentProperty, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<AlpitronicEvcs>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	private static final int MAX_NUMBER_OF_CHARGEPOINTS = 4;
	private static final IntFunction<String> EVCS_ALIAS = value -> "CP_ALIAS_" + value;
	private static final IntFunction<String> EVCS_ID = value -> "EVCS_ID_" + value;
	private static final IntFunction<String> CTRL_EVCS_ID = value -> "CTRL_EVCS_ID_" + value;

	private final Map<String, ParentProperty> chargePointsDef = new TreeMap<>();

	@Activate
	public AlpitronicEvcs(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
		for (int i = 0; i < MAX_NUMBER_OF_CHARGEPOINTS; i++) {
			final var name = EVCS_ALIAS.apply(i);
			this.chargePointsDef.put(name,
					new ParentPropertyImpl(i == 0 ? "ALIAS" : name, Property.chargePointAlias(i + 1)));
			final var evcsComponentId = EVCS_ID.apply(i);
			this.chargePointsDef.put(evcsComponentId,
					new ParentPropertyImpl(evcsComponentId, AppDef.componentId("evcs0")));
			final var evcsControllerComponentId = CTRL_EVCS_ID.apply(i);
			this.chargePointsDef.put(evcsControllerComponentId,
					new ParentPropertyImpl(evcsControllerComponentId, AppDef.componentId("ctrlEvcs0")));
		}
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<ParentProperty, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var translationBundle = AbstractOpenemsApp.getTranslationBundle(l);
			final var controllerAlias = TranslationUtil.getTranslation(translationBundle, "App.Evcs.controller.alias");

			var maxHardwarePowerPerPhase = OptionalInt.empty();
			if (p.containsKey(Property.MAX_HARDWARE_POWER)) {
				maxHardwarePowerPerPhase = OptionalInt.of(this.getInt(p, Property.MAX_HARDWARE_POWER));
			}

			final var components = new ArrayList<EdgeConfig.Component>();
			final var schedulerIds = new ArrayList<String>();
			final var addedEvcsIds = new ArrayList<String>();

			final var ip = this.getString(p, l, Property.IP);
			final var numberOfConnectors = this.getInt(p, Property.NUMBER_OF_CONNECTORS);
			final var modbusId = this.getId(t, p, Property.MODBUS_ID);

			for (int i = 0; i < numberOfConnectors; i++) {
				final var aliasDef = this.chargePointsDef.get(EVCS_ALIAS.apply(i));
				final var evcsIdDef = this.chargePointsDef.get(EVCS_ID.apply(i));
				final var evcsId = this.getId(t, p, evcsIdDef);
				final var evcsCtrlIdDef = this.chargePointsDef.get(CTRL_EVCS_ID.apply(i));
				final var ctrlEvcsId = this.getId(t, p, evcsCtrlIdDef);

				schedulerIds.add(ctrlEvcsId);
				addedEvcsIds.add(evcsId);

				components.add(new EdgeConfig.Component(evcsId, this.getString(p, l, aliasDef),
						"Evcs.AlpitronicHypercharger", JsonUtils.buildJsonObject() //
								.addProperty("connector", "SLOT_" + i) //
								.addProperty("modbus.id", modbusId) //
								.build()));
				components.add(new EdgeConfig.Component(ctrlEvcsId, controllerAlias, "Controller.Evcs",
						JsonUtils.buildJsonObject() //
								.addProperty("evcs.id", evcsId) //
								.build()));
			}

			components.add(new EdgeConfig.Component(modbusId,
					TranslationUtil.getTranslation(translationBundle, "App.Evcs.Alpitronic.modbus.alias"),
					"Bridge.Modbus.Tcp", JsonUtils.buildJsonObject() //
							.addProperty("ip", ip) //
							.build()));

			schedulerIds.add("ctrlBalancing0");

			var ips = Lists.newArrayList(//
					new InterfaceConfiguration("eth0") //
							// range from 192.168.1.96 - 192.168.1.111
							.addIp("Evcs", "192.168.1.97/28") //
			);

			return new AppConfiguration(//
					components, //
					schedulerIds, //
					ip.startsWith("192.168.1.") ? ips : null, //
					EvcsCluster.dependency(t, this.componentManager, this.componentUtil, maxHardwarePowerPerPhase,
							addedEvcsIds.stream().toArray(String[]::new)) //
			);
		};
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/fenecon-fems/fems-app-dc-ladestation/") //
				.build();
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.EVCS };
	}

	@Override
	protected AlpitronicEvcs getApp() {
		return this;
	}

	@Override
	protected ParentProperty[] propertyValues() {
		final var builder = ImmutableList.<ParentProperty>builder() //
				.addAll(Arrays.stream(Property.values()).filter(p -> Stream.of(//
						Property.MAX_HARDWARE_POWER_ACCEPT_PROPERTY, //
						Property.MAX_HARDWARE_POWER //
				).allMatch(t -> p != t)).toList());

		builder.addAll(this.chargePointsDef.values());

		builder.add(Property.MAX_HARDWARE_POWER_ACCEPT_PROPERTY);
		builder.add(Property.MAX_HARDWARE_POWER);

		return builder.build().toArray(ParentProperty[]::new);
	}

}
