package io.openems.edge.app.integratedsystem.fenecon.commercial;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.essLimiter14aToHardware;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.externalLimitationType;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.feedInLink;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.hasEssLimiter14a;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.maxFeedInPower;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.safetyCountry;
import static io.openems.edge.core.appmanager.TranslationUtil.translate;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.ExternalLimitationType;
import io.openems.edge.app.integratedsystem.FeneconHomeComponents;
import io.openems.edge.app.integratedsystem.fenecon.commercial.FeneconCommercial92ClusterMaster.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.AppManagerUtilSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.InterfaceConfiguration;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.enums.InputType;
import io.openems.edge.core.host.NetworkInterface.IpMasqueradeSetting;

@Component(name = "App.FENECON.Commercial.92.ClusterMaster")
public class FeneconCommercial92ClusterMaster
		extends AbstractOpenemsAppWithProps<FeneconCommercial92ClusterMaster, Property, Parameter.BundleParameter>
		implements OpenemsApp, AppManagerUtilSupplier {

	public enum Property implements Type<Property, FeneconCommercial92ClusterMaster, Parameter.BundleParameter> {
		ALIAS(alias()), //

		SAFETY_COUNTRY(AppDef.copyOfGeneric(safetyCountry(), def -> def //
				.setRequired(true))), //

		LINK_FEED_IN(feedInLink()), //
		// hidden until external limitation is implemented
		FEED_IN_TYPE(externalLimitationType(ExternalLimitationType.EXTERNAL_LIMITATION) //
				.appendIsAllowedToSee(AppDef.FieldValuesBiPredicate.FALSE)), //
		MAX_FEED_IN_POWER(maxFeedInPower(FEED_IN_TYPE)), //

		HAS_ESS_LIMITER_14A(hasEssLimiter14a()), //

		NUMBER_OF_SLAVES(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".numberOfSlaves.label") //
				.setDefaultValue(1) //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.setInputType(InputType.NUMBER);
					field.setMin(1);
					field.setMax(5);
				}))), //
		;

		private final AppDef<? super FeneconCommercial92ClusterMaster, ? super Property, ? super BundleParameter> def;

		private Property(
				AppDef<? super FeneconCommercial92ClusterMaster, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, FeneconCommercial92ClusterMaster, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super FeneconCommercial92ClusterMaster, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<FeneconCommercial92ClusterMaster>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	private final AppManagerUtil appManagerUtil;

	@Activate
	public FeneconCommercial92ClusterMaster(//
			@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil, //
			@Reference final AppManagerUtil appManagerUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
		this.appManagerUtil = appManagerUtil;
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.setWebsiteUrl(oem.getAppWebsiteUrl(this.getAppId())) //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.INTEGRATED_SYSTEM };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE_IN_CATEGORY;
	}

	@Override
	protected FeneconCommercial92ClusterMaster getApp() {
		return this;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var bundle = AbstractOpenemsApp.getTranslationBundle(l);

			final var numberOfSlaves = this.getInt(p, Property.NUMBER_OF_SLAVES);

			final var feedInType = this.getEnum(p, ExternalLimitationType.class, Property.FEED_IN_TYPE);

			final var hasEssLimiter14a = this.getBoolean(p, Property.HAS_ESS_LIMITER_14A);

			final var essId = "ess0";
			final var gridMeterId = "meter0";
			final var modbusToGridMeterAndExternalId = "modbus1";

			final var deviceHardware = this.appManagerUtil
					.getFirstInstantiatedAppByCategories(OpenemsAppCategory.OPENEMS_DEVICE_HARDWARE);

			final var components = Lists.<EdgeConfig.Component>newArrayList(//
					new EdgeConfig.Component(essId, translate(bundle, "App.IntegratedSystem.ess0.alias"), "Ess.Cluster",
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.add("ess.ids", IntStream.range(0, numberOfSlaves) //
											.mapToObj(i -> new JsonPrimitive("ess" + (i + 1))) //
											.collect(JsonUtils.toJsonArray())) //
									.addProperty("startStop", "START") //
									.build()), //
					FeneconHomeComponents.predictor(bundle, t), //
					FeneconHomeComponents.modbusInternal(bundle, t, "modbus0"), //
					FeneconCommercialComponents.modbusToGridMeterAndExternal(bundle, t, modbusToGridMeterAndExternalId) //
			);

			for (int i = 1; i <= numberOfSlaves; i++) {
				final var bridgeId = "bridge" + i;
				components.add(new EdgeConfig.Component(bridgeId,
						translate(bundle, "App.IntegratedSystem.bridgeToSlaveN.alias", i), "Bridge.Edge2Edge.Websocket",
						JsonUtils.buildJsonObject() //
								.addProperty("enabled", true) //
								.addProperty("ip", "10.5.0." + (10 + i)) //
								.addProperty("port", 8085) //
								.build()));
				components.add(
						new EdgeConfig.Component("ess" + i, translate(bundle, "App.IntegratedSystem.essN.alias", i),
								"Edge2Edge.Websocket.Ess", JsonUtils.buildJsonObject() //
										.addProperty("enabled", true) //
										.addProperty("remoteAccessMode", "READ_WRITE") //
										.addProperty("remoteComponentId", "ess0") //
										.addProperty("bridge.id", bridgeId) //
										.build()));
				components.add(new EdgeConfig.Component("battery" + i,
						translate(bundle, "App.IntegratedSystem.batteryN.alias", i),
						"Edge2Edge.Websocket.GenericReadComponent", JsonUtils.buildJsonObject() //
								.addProperty("enabled", true) //
								.addProperty("remoteComponentId", "battery0") //
								.addProperty("bridge.id", bridgeId) //
								.build()));
				components.add(new EdgeConfig.Component("batteryInverter" + i,
						translate(bundle, "App.IntegratedSystem.batteryInverterN.alias", i),
						"Edge2Edge.Websocket.GenericReadComponent", JsonUtils.buildJsonObject() //
								.addProperty("enabled", true) //
								.addProperty("remoteComponentId", "batteryInverter0") //
								.addProperty("bridge.id", bridgeId) //
								.build()));
			}

			final var dependencies = Lists.newArrayList(//
					FeneconHomeComponents.selfConsumptionOptimization(t, essId, gridMeterId), //
					FeneconHomeComponents.gridOptimizedCharge(t), //
					FeneconHomeComponents.prepareBatteryExtension(), //
					FeneconCommercialComponents.gridMeter(bundle, gridMeterId, modbusToGridMeterAndExternalId) //
			);

			if (hasEssLimiter14a) {
				dependencies.add(essLimiter14aToHardware(this.appManagerUtil, deviceHardware));
			}

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.staticIp(//
							new InterfaceConfiguration("eth0") //
									.setIpv4Forwarding(true),
							new InterfaceConfiguration("eth1") //
									.addIp("Slave com", "10.5.0.1/24") //
									.setIpMasquerade(IpMasqueradeSetting.IP_V4)))
					.addDependencies(dependencies) //
					.build();
		};
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create() //
				.setCanDelete(Role.INSTALLER) //
				.setCanSee(Role.INSTALLER) //
				.build();
	}

	@Override
	public AppManagerUtil getAppManagerUtil() {
		return this.appManagerUtil;
	}

}
