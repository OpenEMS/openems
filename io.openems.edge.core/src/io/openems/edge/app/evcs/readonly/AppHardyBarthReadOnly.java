package io.openems.edge.app.evcs.readonly;

import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
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
import io.openems.edge.app.evcs.EvcsProps;
import io.openems.edge.app.evcs.readonly.AppHardyBarthReadOnly.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.host.Host;
import io.openems.edge.common.meta.Meta;
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
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.Case;
import io.openems.edge.core.appmanager.formly.DefaultValueOptions;
import io.openems.edge.core.appmanager.formly.Exp;

@Component(name = "App.Evcs.HardyBarth.ReadOnly")
public class AppHardyBarthReadOnly
		extends AbstractOpenemsAppWithProps<AppHardyBarthReadOnly, Property, Parameter.BundleParameter>
		implements OpenemsApp, HostSupplier {

	public static enum Property implements Nameable, Type<Property, AppHardyBarthReadOnly, Parameter.BundleParameter> {
		// Component-IDs
		EVCS_ID(AppDef.componentId("evcs0")), //
		EVCS_ID_CP_2(AppDef.componentId("evcs0")), //
		// Properties
		NUMBER_OF_CHARGING_STATIONS(AppDef.copyOfGeneric(EvcsProps.numberOfChargePoints(2))), //
		PHASE_ROTATION(EvcsProps.phaseRotation()), //
		// First ChargePoint
		ALIAS(AppDef.copyOfGeneric(CommonProps.alias()) //
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
		IP(AppDef.copyOfGeneric(CommunicationProps.excludingIp())//
				.setDefaultValue("192.168.25.30")//
				.setRequired(true)), //

		// Second ChargePoint
		ALIAS_CP_2(AppDef.copyOfGeneric(CommonProps.alias()) //
				.setDefaultValue((app, property, l, parameter) -> //
				new JsonPrimitive(TranslationUtil.getTranslation(parameter.bundle(), "App.Evcs.HardyBarth.alias.value", //
						TranslationUtil.getTranslation(parameter.bundle(), "left")))) //
				.wrapField((app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(NUMBER_OF_CHARGING_STATIONS)//
							.equal(Exp.staticValue(2)));
				})//
				.setRequired(true)), //
		IP_CP_2(AppDef.copyOfGeneric(CommunicationProps.excludingIp()) //
				.setDefaultValue("192.168.25.31") //
				.wrapField((app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(NUMBER_OF_CHARGING_STATIONS)//
							.equal(Exp.staticValue(2)));
				})//
				.setRequired(true)), //
		;

		private final AppDef<? super AppHardyBarthReadOnly, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super AppHardyBarthReadOnly, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, AppHardyBarthReadOnly, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super AppHardyBarthReadOnly, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<AppHardyBarthReadOnly>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	private final Host host;

	@Activate
	public AppHardyBarthReadOnly(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil, //
			@Reference Host host, //
			@Reference Meta meta //
	) {
		super(componentManager, componentContext, cm, componentUtil);
		this.host = host;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, //
			Map<Property, JsonElement>, //
			Language, //
			AppConfiguration, //
			OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var numberOfChargingStations = this.getInt(p, Property.NUMBER_OF_CHARGING_STATIONS);
			if (numberOfChargingStations <= 0 || numberOfChargingStations > 2) {
				throw new OpenemsException("Number of charging stations can only be 0 < n <= 2.");
			}

			final var alias = this.getString(p, l, Property.ALIAS);
			final var ip = this.getString(p, Property.IP);
			final var evcsId = this.getId(t, p, Property.EVCS_ID);
			final var phaseRotation = this.getString(p, Property.PHASE_ROTATION);

			final var factoryId = "Evcs.HardyBarth";
			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(evcsId, alias, factoryId, JsonUtils.buildJsonObject() //
							.addProperty("ip", ip) //
							.addPropertyIfNotNull("phaseRotation", phaseRotation) //
							.addProperty("readOnly", true)//
							.build())//
			);

			if (numberOfChargingStations == 2) {
				final var aliasCp2 = this.getString(p, l, Property.ALIAS_CP_2);
				final var ipCp2 = this.getString(p, l, Property.IP_CP_2);
				final var evcsIdCp2 = this.getId(t, p, Property.EVCS_ID_CP_2);

				components.add(new EdgeConfig.Component(evcsIdCp2, aliasCp2, factoryId, JsonUtils.buildJsonObject() //
						.addProperty("ip", ipCp2) //
						.addPropertyIfNotNull("phaseRotation", phaseRotation) //
						.addProperty("readOnly", true)//
						.build()));
			}

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.throwingOnlyIf(ip.startsWith("192.168.25."),
							b -> b.addTask(Tasks.staticIp(new InterfaceConfiguration("eth0") //
									.addIp("Evcs", "192.168.25.10/24")))) //
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
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	protected AppHardyBarthReadOnly getApp() {
		return this;
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.EVCS_READ_ONLY };
	}

	@Override
	public Host getHost() {
		return this.host;
	}

}
