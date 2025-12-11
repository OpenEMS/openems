package io.openems.edge.app.evcs;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommunicationProps.modbusUnitId;

import java.util.ArrayList;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
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
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.AppInstanceProps;
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.enums.EMobilityArchitectureType;
import io.openems.edge.app.enums.KebaHardwareType;
import io.openems.edge.app.evcs.KebaEvcs.Property;
import io.openems.edge.app.evse.AppEvseCluster;
import io.openems.edge.app.evse.EvseProps;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.host.Host;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.AppManagerUtilSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.HostSupplier;
import io.openems.edge.core.appmanager.InterfaceConfiguration;
import io.openems.edge.core.appmanager.MetaSupplier;
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
import io.openems.edge.core.appmanager.flag.Flag;
import io.openems.edge.core.appmanager.flag.Flags;
import io.openems.edge.core.appmanager.formly.Exp;

/**
 * Describes a Keba evcs App.
 *
 * <pre>
  {
    "appId":"App.Evcs.Keba",
    "alias":"KEBA Ladestation",
    "instanceId": UUID,
    "image": base64,
    "properties":{
      "EVCS_ID": "evcs0",
      "CTRL_EVCS_ID": "ctrlEvcs0",
      "IP":"192.168.25.11",
      "PHASE_ROTATION":"L1_L2_L3"
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.Evcs.Keba")
public class KebaEvcs extends AbstractOpenemsAppWithProps<KebaEvcs, Property, Parameter.BundleParameter>
		implements OpenemsApp, HostSupplier, MetaSupplier, AppManagerUtilSupplier {

	public enum Property implements Type<Property, KebaEvcs, Parameter.BundleParameter>, Nameable {
		// Component-IDs
		EVCS_ID(AppDef.componentId("evcs0")), //
		CTRL_EVCS_ID(AppDef.componentId("ctrlEvcs0")), //
		CTRL_SINGLE_ID(AppDef.componentId("ctrlEvseSingle0")), //
		// Properties
		ALIAS(alias()), //

		ARCHITECTURE_TYPE(EvcsProps.architectureType(EVCS_ID)), //
		HARDWARE_TYPE(EvcsProps.hardwareType(EVCS_ID)//
				.wrapField((app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(ARCHITECTURE_TYPE)//
							.equal(Exp.staticValue(EMobilityArchitectureType.EVCS)));
				})), //

		ELECTRIC_VEHICLE_ID(AppInstanceProps.pickInstanceId("App.Evse.ElectricVehicle.Generic")//
				.setRequired(true) //
				.setTranslatedLabel("App.Evse.pickVehicleId.label").wrapField((app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(ARCHITECTURE_TYPE)//
							.equal(Exp.staticValue(EMobilityArchitectureType.EVSE)));
				})), //
		WIRING(AppDef.copyOfGeneric(EvseProps.wiring())//
				.wrapField((app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(ARCHITECTURE_TYPE)//
							.equal(Exp.staticValue(EMobilityArchitectureType.EVSE)));
				})), //

		IP(AppDef.copyOfGeneric(CommunicationProps.excludingIp())//
				.setDefaultValue("192.168.25.11")//
				.setRequired(true)), //
		MAX_HARDWARE_POWER_ACCEPT_PROPERTY(AppDef.of()//
				.setAllowedToSave(false)), //

		MAX_HARDWARE_POWER(AppDef.copyOfGeneric(//
				EvcsProps.clusterMaxHardwarePowerSingleCp(MAX_HARDWARE_POWER_ACCEPT_PROPERTY, EVCS_ID))
				.wrapField((app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(ARCHITECTURE_TYPE)//
							.equal(Exp.staticValue(EMobilityArchitectureType.EVCS)));
				})), //
		PHASE_ROTATION(AppDef.copyOfGeneric(EvcsProps.phaseRotation())), //
		// Properties for P40 app
		MODBUS_ID(AppDef.componentId("modbus0")), //
		MODBUS_UNIT_ID(AppDef.copyOfGeneric(modbusUnitId(), def -> def //
				.setDefaultValue(255)//
				.wrapField((app, property, l, parameter, field) -> {
					final var hardwareType = Exp.currentModelValue(HARDWARE_TYPE)//
							.equal(Exp.staticValue(KebaHardwareType.P40));
					final var architectureType = Exp.currentModelValue(ARCHITECTURE_TYPE)//
							.equal(Exp.staticValue(EMobilityArchitectureType.EVSE));
					field.onlyShowIf(hardwareType.or(architectureType));
				}))), //
		READ_ONLY(EvcsProps.readOnly());

		private final AppDef<? super KebaEvcs, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super KebaEvcs, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, KebaEvcs, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super KebaEvcs, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<KebaEvcs>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	private final Host host;
	private final Meta meta;
	private final AppManagerUtil appManagerUtil;

	@Activate
	public KebaEvcs(//
			@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil, //
			@Reference final AppManagerUtil appManagerUtil, //
			@Reference final Host host, //
			@Reference final Meta meta //
	) {
		super(componentManager, componentContext, cm, componentUtil);
		this.host = host;
		this.meta = meta;
		this.appManagerUtil = appManagerUtil;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var bundle = AbstractOpenemsApp.getTranslationBundle(l);

			// values the user enters
			final var evcsId = this.getId(t, p, Property.EVCS_ID);
			final var ip = this.getString(p, l, Property.IP);
			final var alias = this.getString(p, l, Property.ALIAS);
			final var phaseRotation = this.getString(p, l, Property.PHASE_ROTATION);
			final var hardwareType = this.getEnum(p, KebaHardwareType.class, Property.HARDWARE_TYPE);
			final var architectureType = this.getEnum(p, EMobilityArchitectureType.class, Property.ARCHITECTURE_TYPE);
			final var readOnly = this.getBoolean(p, Property.READ_ONLY);

			// values which are being auto generated by the appmanager

			var maxHardwarePowerPerPhase = OptionalInt.empty();
			if (p.containsKey(Property.MAX_HARDWARE_POWER)) {
				maxHardwarePowerPerPhase = OptionalInt.of(this.getInt(p, Property.MAX_HARDWARE_POWER));
			}

			var appConfig = AppConfiguration.create() //
					.throwingOnlyIf(ip.startsWith("192.168.25."),
							b -> b.addTask(Tasks.staticIp(new InterfaceConfiguration("eth0") //
									.addIp("Evcs", "192.168.25.10/24"))));
			final var components = new ArrayList<EdgeConfig.Component>();
			switch (architectureType) {
			case EVCS -> {
				switch (hardwareType) {
				case P30 -> {
					components.add(new EdgeConfig.Component(evcsId, alias, "Evcs.Keba.KeContact",
							JsonUtils.buildJsonObject() //
									.addPropertyIfNotNull("ip", ip) //
									.addPropertyIfNotNull("phaseRotation", phaseRotation) //
									.addProperty("readOnly", readOnly) //
									.build())//
					);
				}
				case P40 -> {
					final var modbusId = this.getId(t, p, Property.MODBUS_ID);
					final var modbusUnitId = this.getInt(p, Property.MODBUS_UNIT_ID);
					components.addAll(Lists.newArrayList(//
							new EdgeConfig.Component(modbusId,
									TranslationUtil.getTranslation(bundle, "App.Evcs.Keba.modbus.alias"),
									"Bridge.Modbus.Tcp", JsonUtils.buildJsonObject() //
											.addProperty("ip", ip) //
											.onlyIf(t == ConfigurationTarget.ADD, b -> b //
													.addProperty("port", 502)) //
											.build()),
							new EdgeConfig.Component(evcsId, alias, "Evcs.Keba.P40", JsonUtils.buildJsonObject() //
									.addPropertyIfNotNull("modbus.id", modbusId)//
									.addPropertyIfNotNull("modbusUnitId", modbusUnitId)//
									.addPropertyIfNotNull("phaseRotation", phaseRotation) //
									.addPropertyIfNotNull("readOnly", readOnly)//
									.build()))); //
				}
				}
				if (!readOnly) {
					final var controllerAlias = TranslationUtil
							.getTranslation(AbstractOpenemsApp.getTranslationBundle(l), "App.Evcs.controller.alias");
					final var ctrlEvcsId = this.getId(t, p, Property.CTRL_EVCS_ID);
					components.add(new EdgeConfig.Component(ctrlEvcsId, controllerAlias, "Controller.Evcs",
							JsonUtils.buildJsonObject() //
									.addProperty("evcs.id", evcsId) //
									.build()));

					appConfig
							.addDependencies(EvcsCluster.dependency(t, this.componentManager, this.componentUtil,
									maxHardwarePowerPerPhase, evcsId))
							.addTask(Tasks.schedulerByCentralOrder(
									new SchedulerComponent(ctrlEvcsId, "Controller.Evcs", this.getAppId())));
				}
			}
			case EVSE -> {
				// Modbus Component
				var modbusId = this.getId(t, p, Property.MODBUS_ID);
				var modbusUnitId = this.getInt(p, Property.MODBUS_UNIT_ID);
				var wiring = this.getEnum(p, SingleOrThreePhase.class, Property.WIRING);
				var vehicleId = UUID.fromString(this.getString(p, Property.ELECTRIC_VEHICLE_ID));
				components.add(//
						new EdgeConfig.Component(//
								modbusId, //
								TranslationUtil.getTranslation(bundle, "App.Evse.ChargePoint.Keba.modbus.alias"), //
								"Bridge.Modbus.Tcp", //
								JsonUtils.buildJsonObject() //
										.addProperty("ip", ip) //
										.onlyIf(t == ConfigurationTarget.ADD, b -> b.addProperty("port", 502)) //
										.build() //
				));
				components.add(//
						new EdgeConfig.Component(//
								evcsId, //
								alias, //
								"Evse.ChargePoint.Keba.Modbus", //
								JsonUtils.buildJsonObject() //
										.addProperty("modbus.id", modbusId)//
										.addProperty("wiring", wiring) //
										.addProperty("phaseRotation", phaseRotation) //
										.addProperty("modbusUnitId", modbusUnitId) //
										.build()));

				var instance = this.appManagerUtil.findInstanceById(vehicleId);
				var vehicleComponentId = "";
				if (instance.isPresent()) {
					var appConfiguration = this.appManagerUtil.getAppConfiguration(ConfigurationTarget.VALIDATE,
							instance.get(), l);
					vehicleComponentId = appConfiguration.getComponents().stream().map(b -> b.id())
							.filter(b -> b.startsWith("evseElectricVehicle")).findFirst().get();
				} else if (!t.isDeleteOrTest()) {
					throw new RuntimeException("Unable to find Vehicle-App Instance");
				}

				var ctrlSingleId = this.getId(t, p, Property.CTRL_SINGLE_ID);

				components.add(new EdgeConfig.Component(ctrlSingleId, alias, "Evse.Controller.Single",
						JsonUtils.buildJsonObject()//
								.addProperty("electricVehicle.id", vehicleComponentId)//
								.addProperty("chargePoint.id", evcsId)//
								.build()));

				final var dependencies = Lists.newArrayList(new DependencyDeclaration("VEHICLE", //
						DependencyDeclaration.CreatePolicy.NEVER, //
						DependencyDeclaration.UpdatePolicy.NEVER, //
						DependencyDeclaration.DeletePolicy.NEVER, //
						DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ALL, //
						DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
						DependencyDeclaration.AppDependencyConfig.create() //
								.setSpecificInstanceId(vehicleId) //
								.build()) //
				);

				dependencies.addAll(AppEvseCluster.dependency());
				appConfig.addTask(Tasks.cluster(ctrlSingleId));
				appConfig.addDependencies(dependencies);
			}
			}

			appConfig.addTask(Tasks.component(components));

			return appConfig.build();
		};
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.setWebsiteUrl(oem.getAppWebsiteUrl(this.getAppId())) //
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
	protected KebaEvcs getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public Host getHost() {
		return this.host;
	}

	@Override
	public Meta getMeta() {
		return this.meta;
	}

	@Override
	public AppManagerUtil getAppManagerUtil() {
		return this.appManagerUtil;
	}

	@Override
	public Flag[] flags() {
		final var flags = Lists.newArrayList(super.flags());
		flags.add(Flags.CAN_SWITCH_ARCHITECTURE);
		return flags.toArray(Flag[]::new);
	}
}
