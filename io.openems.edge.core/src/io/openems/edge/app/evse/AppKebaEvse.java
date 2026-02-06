package io.openems.edge.app.evse;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.app.common.props.CommonProps.phaseRotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.AppInstanceProps;
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.enums.KebaHardwareType;
import io.openems.edge.app.evse.AppKebaEvse.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.host.Host;
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
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

@Component(name = "App.Evse.ChargePoint.Keba")
public class AppKebaEvse extends AbstractOpenemsAppWithProps<AppKebaEvse, Property, Parameter.BundleParameter>
		implements OpenemsApp, HostSupplier, AppManagerUtilSupplier {

	public enum Property implements Type<Property, AppKebaEvse, Parameter.BundleParameter> {
		EVSE_SINGLE_ID(AppDef.componentId("ctrlEvseSingle0")), //
		CHARGEPOINT_ID(AppDef.componentId("evcs0")), //
		MODBUS_ID(AppDef.componentId("modbus0")), //

		ALIAS(AppDef.copyOfGeneric(alias())), //
		ELECTRIC_VEHICLE_ID(AppInstanceProps.pickInstanceId("App.Evse.ElectricVehicle.Generic")//
				.setRequired(true)//
				.setTranslatedLabel("App.Evse.pickVehicleId.label")),
		HARDWARE_TYPE(AppDef.copyOfGeneric(defaultDef())//
				.setTranslatedLabelWithAppPrefix(".hardwareType.label")
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(Arrays.stream(KebaHardwareType.values())//
							.map(Enum::name)//
							.toList());
				})//
				.setRequired(true)//
				.setDefaultValue(KebaHardwareType.P40)),

		// Configurations
		IP(AppDef.copyOfGeneric(CommunicationProps.excludingIp())//
				.setDefaultValue("192.168.25.11")//
				.setRequired(true)),
		PHASE_ROTATION(AppDef.copyOfGeneric(phaseRotation()//
				.setTranslatedDescription("App.Evse.phaseRotation.description"))), //
		WIRING(AppDef.copyOfGeneric(EvseProps.wiring())), //
		PHASE_SWITCHING(AppDef.copyOfGeneric(EvseProps.p30hasPhaseSwitch())//
				.wrapField((app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(HARDWARE_TYPE)//
							.equal(Exp.staticValue(KebaHardwareType.P30.name())));
				})), //
		READ_ONLY(EvseProps.readOnly()),

		// only for modbus
		MODBUS_UNIT_ID(EvseProps.unitId().wrapField((app, property, l, parameter, field) -> {
			field.onlyShowIf(Exp.currentModelValue(HARDWARE_TYPE)//
					.equal(Exp.staticValue(KebaHardwareType.P40.name())));
		})), //
		;

		private final AppDef<? super AppKebaEvse, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super AppKebaEvse, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, AppKebaEvse, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super AppKebaEvse, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<AppKebaEvse>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	private final Host host;
	private final AppManagerUtil appManagerUtil;

	@Activate
	public AppKebaEvse(//
			@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil, //
			@Reference final AppManagerUtil appManagerUtil, //
			@Reference final Host host) {
		super(componentManager, componentContext, cm, componentUtil);
		this.appManagerUtil = appManagerUtil;
		this.host = host;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var bundle = AbstractOpenemsApp.getTranslationBundle(l);
			var cpId = this.getId(t, p, Property.CHARGEPOINT_ID);
			var ctrlSingleId = this.getId(t, p, Property.EVSE_SINGLE_ID);

			final var vehicleId = UUID.fromString(this.getString(p, Property.ELECTRIC_VEHICLE_ID));
			final var hardwareType = this.getEnum(p, KebaHardwareType.class, Property.HARDWARE_TYPE);

			final var components = new ArrayList<EdgeConfig.Component>();

			var alias = this.getString(p, l, Property.ALIAS);
			var wiring = this.getString(p, Property.WIRING);
			var phaseRotation = this.getString(p, Property.PHASE_ROTATION);
			var ip = this.getString(p, Property.IP);

			switch (hardwareType) {
			case P30 -> {
				// UDP Component
				var phaseSwitching = this.getBoolean(p, Property.PHASE_SWITCHING);
				components.add(//
						new EdgeConfig.Component(//
								cpId, //
								alias, //
								"Evse.ChargePoint.Keba.UDP", //
								JsonUtils.buildJsonObject() //
										.addProperty("wiring", wiring) //
										.addProperty("phaseRotation", phaseRotation) //
										.addProperty("p30hasS10PhaseSwitching", phaseSwitching) //
										.addProperty("ip", ip) //
										.build() //
				));
			}
			case P40 -> {
				// Modbus Component
				var modbusId = this.getId(t, p, Property.MODBUS_ID);
				var modbusUnitId = this.getInt(p, Property.MODBUS_UNIT_ID);
				components.add(//
						new EdgeConfig.Component(//
								cpId, //
								alias, //
								"Evse.ChargePoint.Keba.Modbus", //
								JsonUtils.buildJsonObject() //
										.addProperty("modbus.id", modbusId)//
										.addProperty("wiring", wiring) //
										.addProperty("phaseRotation", phaseRotation) //
										.addProperty("modbusUnitId", modbusUnitId) //
										.build() //
				) //
				); //

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
			}
			}

			var instance = this.appManagerUtil.findInstanceById(vehicleId);
			var vehicleComponentId = "";
			if (instance.isPresent()) {
				var appConfig = this.appManagerUtil.getAppConfiguration(ConfigurationTarget.VALIDATE, instance.get(),
						l);
				vehicleComponentId = appConfig.getComponents().stream().map(b -> b.id())
						.filter(b -> b.startsWith("evseElectricVehicle")).findFirst().get();
			}

			components.add(new EdgeConfig.Component(ctrlSingleId, alias, "Evse.Controller.Single",
					JsonUtils.buildJsonObject()//
							.addProperty("electricVehicle.id", vehicleComponentId)//
							.addProperty("chargePoint.id", cpId)//
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

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.cluster(ctrlSingleId))//
					.addDependencies(AppEvseCluster.dependency())//
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
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.EVCS };
	}

	@Override
	protected AppKebaEvse getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create()//
				.setCanSee(Role.ADMIN)//
				.setCanInstall(List.of(Role.ADMIN))//
				.setCanDelete(Role.ADMIN)//
				.build();
	}

	@Override
	public Host getHost() {
		return this.host;
	}

	@Override
	public AppManagerUtil getAppManagerUtil() {
		return this.appManagerUtil;
	}
}
