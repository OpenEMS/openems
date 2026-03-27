package io.openems.edge.app.evse;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommunicationProps.modbusUnitId;

import java.util.ArrayList;
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
import io.openems.edge.app.common.props.AppInstanceProps;
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.evcs.EvcsProps;
import io.openems.edge.app.evse.AppMennekesEvse.Property;
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
import io.openems.edge.core.appmanager.MetaSupplier;
import io.openems.edge.core.appmanager.Nameable;
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
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentDef;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentDef.Configuration;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentProperties;

@Component(name = AppMennekesEvse.APP_EVSE_MENNEKES)
public class AppMennekesEvse extends AbstractOpenemsAppWithProps<AppMennekesEvse, Property, Parameter.BundleParameter>
		implements OpenemsApp, HostSupplier, MetaSupplier, AppManagerUtilSupplier {

	public static final String APP_EVSE_MENNEKES = "App.Evse.ChargePoint.Mennekes";

	public enum Property implements Type<Property, AppMennekesEvse, Parameter.BundleParameter>, Nameable {
		// Component-IDs
		EVCS_ID(AppDef.componentId("evcs0")), //
		CTRL_SINGLE_ID(AppDef.componentId("ctrlEvseSingle0")), //
		MODBUS_ID(AppDef.componentId("modbus0")), //
		// Properties
		ALIAS(alias()), //
		IP(AppDef.copyOfGeneric(CommunicationProps.excludingIp())//
				.setDefaultValue("192.168.25.11")//
				.setRequired(true)), //
		MODBUS_UNIT_ID(AppDef.copyOfGeneric(modbusUnitId(), def -> def//
				.setDefaultValue(1))), //
		ELECTRIC_VEHICLE_ID(AppInstanceProps.pickInstanceId("App.Evse.ElectricVehicle.Generic")//
				.setRequired(true)//
				.setTranslatedLabel("App.Evse.pickVehicleId.label")//
				.setTranslatedDescription("App.Evse.pickVehicleId.description")), //
		WIRING(AppDef.copyOfGeneric(EvseProps.wiring())), //
		PHASE_ROTATION(AppDef.copyOfGeneric(EvcsProps.phaseRotation())), //
		;

		private final AppDef<? super AppMennekesEvse, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super AppMennekesEvse, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, AppMennekesEvse, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super AppMennekesEvse, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<AppMennekesEvse>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	private final Host host;
	private final Meta meta;
	private final AppManagerUtil appManagerUtil;

	@Activate
	public AppMennekesEvse(//
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

			// values which are being auto generated by the appmanager
			final var evcsId = this.getId(t, p, Property.EVCS_ID);
			var modbusId = this.getId(t, p, Property.MODBUS_ID);

			// values the user enters
			final var ip = this.getString(p, l, Property.IP);
			final var alias = this.getString(p, l, Property.ALIAS);
			final var phaseRotation = this.getString(p, l, Property.PHASE_ROTATION);

			final var components = new ArrayList<ComponentDef>();

			var modbusProps = new ArrayList<ComponentProperties.Property>();
			modbusProps.add(ComponentProperties.Property.of("ip").withValue(ip));
			if (t == ConfigurationTarget.ADD) {
				modbusProps.add(ComponentProperties.Property.of("port").withValue(502));
			}

			components.add(//
					new ComponentDef(//
							modbusId, //
							TranslationUtil.getTranslation(bundle, "App.Evse.ChargePoint.communication.alias"), //
							"Bridge.Modbus.Tcp", //
							new ComponentProperties(modbusProps), //
							Configuration.defaultConfig()));
			var modbusUnitId = this.getInt(p, Property.MODBUS_UNIT_ID);
			var wiring = this.getEnum(p, SingleOrThreePhase.class, Property.WIRING);
			var vehicleId = UUID.fromString(this.getString(p, Property.ELECTRIC_VEHICLE_ID));

			components.add(//
					new ComponentDef(evcsId, alias, "Evse.ChargePoint.Mennekes", new ComponentProperties(List.of(//
							ComponentProperties.Property.of("phaseRotation").withValue(phaseRotation), //
							ComponentProperties.Property.of("wiring").withValue(wiring.toString()), //
							ComponentProperties.Property.of("modbus.id").withValue(modbusId), //
							ComponentProperties.Property.of("modbusUnitId").withValue(modbusUnitId) //

			)), //
							Configuration.defaultConfig())

			);
			var instance = this.appManagerUtil.findInstanceById(vehicleId);
			var vehicleComponentId = "";
			if (instance.isPresent()) {
				var appConfiguration = this.appManagerUtil.getAppConfiguration(ConfigurationTarget.VALIDATE,
						instance.get(), l);
				vehicleComponentId = appConfiguration.getComponents().stream().map(ComponentDef::id)
						.filter(b -> b.startsWith("evseElectricVehicle")).findFirst().get();
			} else if (!t.isDeleteOrTest()) {
				throw new RuntimeException("Unable to find Vehicle-App Instance");
			}

			var ctrlSingleId = this.getId(t, p, Property.CTRL_SINGLE_ID);
			components.add(new ComponentDef(ctrlSingleId, alias, "Evse.Controller.Single",
					new ComponentProperties(List.of(//
							ComponentProperties.Property.of("electricVehicle.id").withValue(vehicleComponentId),
							ComponentProperties.Property.of("chargePoint.id").withValue(evcsId))),
					Configuration.defaultConfig()));

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

			var appConfig = AppConfiguration.create();
			dependencies.addAll(AppEvseCluster.dependency());
			appConfig.addTask(Tasks.cluster(ctrlSingleId));
			appConfig.addDependencies(dependencies);
			appConfig.addTask(Tasks.componentFromComponentConfig(components));

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
	protected AppMennekesEvse getApp() {
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
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create()//
				.setCanDelete(Role.ADMIN)//
				.setCanInstall(List.of(Role.ADMIN))//
				.setCanSee(Role.ADMIN)//
				.build();
	}
}