package io.openems.edge.app.evcs;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.app.common.props.CommunicationProps.modbusUnitId;

import java.util.ArrayList;
import java.util.Map;
import java.util.OptionalInt;
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
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.app.enums.TranslatableEnum;
import io.openems.edge.app.evcs.KebaEvcs.Property;
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
import io.openems.edge.core.appmanager.MetaSupplier;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration.SchedulerComponent;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

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
		implements OpenemsApp, HostSupplier, MetaSupplier {

	public enum HardwareType implements TranslatableEnum {
		P30, P40;

		@Override
		public String getTranslation(Language language) {
			return this.name();
		}
	}

	public enum Property implements Type<Property, KebaEvcs, Parameter.BundleParameter>, Nameable {
		// Component-IDs
		EVCS_ID(AppDef.componentId("evcs0")), //
		CTRL_EVCS_ID(AppDef.componentId("ctrlEvcs0")), //
		// Properties
		ALIAS(alias()), //

		HARDWARE_TYPE(AppDef.copyOfGeneric(defaultDef())//
				.setTranslatedLabelWithAppPrefix(".hardwareType.label")
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(OptionsFactory.of(HardwareType.class), l);
				})//
				.setRequired(true)//
				.setDefaultValue(HardwareType.P30)),

		IP(AppDef.copyOfGeneric(CommunicationProps.excludingIp()) //
				.setDefaultValue("192.168.25.11") //
				.setRequired(true)), //
		MAX_HARDWARE_POWER_ACCEPT_PROPERTY(AppDef.of() //
				.setAllowedToSave(false)), //
		MAX_HARDWARE_POWER(AppDef.copyOfGeneric(//
				EvcsProps.clusterMaxHardwarePowerSingleCp(MAX_HARDWARE_POWER_ACCEPT_PROPERTY, EVCS_ID))), //
		PHASE_ROTATION(AppDef.copyOfGeneric(EvcsProps.phaseRotation())), //
		// Properties for P40 app
		MODBUS_ID(AppDef.componentId("modbus0")), //
		MODBUS_UNIT_ID(AppDef.copyOfGeneric(modbusUnitId(), def -> def //
				.setDefaultValue(255)//
				.wrapField((app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(HARDWARE_TYPE) //
							.equal(Exp.staticValue(HardwareType.P40)));
				}))), //
		READ_ONLY(EvcsProps.readOnly() //
				.setDefaultValue(true) //
				.setIsAllowedToSee(AppDef.ofLeastRole(Role.ADMIN))//
				.wrapField((app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(HARDWARE_TYPE) //
							.equal(Exp.staticValue(HardwareType.P40)));
				}));

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

	@Activate
	public KebaEvcs(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil, //
			@Reference Host host, //
			@Reference Meta meta //
	) {
		super(componentManager, componentContext, cm, componentUtil);
		this.host = host;
		this.meta = meta;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var bundle = AbstractOpenemsApp.getTranslationBundle(l);

			final var controllerAlias = TranslationUtil.getTranslation(AbstractOpenemsApp.getTranslationBundle(l),
					"App.Evcs.controller.alias");

			// values the user enters
			final var ip = this.getString(p, l, Property.IP);
			final var alias = this.getString(p, l, Property.ALIAS);
			final var phaseRotation = this.getString(p, l, Property.PHASE_ROTATION);
			final var hardwareType = this.getString(p, l, Property.HARDWARE_TYPE);
			final var readOnly = this.getBoolean(p, Property.READ_ONLY);

			// values which are being auto generated by the appmanager
			final var evcsId = this.getId(t, p, Property.EVCS_ID);
			final var ctrlEvcsId = this.getId(t, p, Property.CTRL_EVCS_ID);

			var maxHardwarePowerPerPhase = OptionalInt.empty();
			if (p.containsKey(Property.MAX_HARDWARE_POWER)) {
				maxHardwarePowerPerPhase = OptionalInt.of(this.getInt(p, Property.MAX_HARDWARE_POWER));
			}

			ArrayList<io.openems.common.types.EdgeConfig.Component> components;
			if (HardwareType.valueOf(hardwareType) == HardwareType.P30) {
				components = Lists.newArrayList(//
						new EdgeConfig.Component(evcsId, alias, "Evcs.Keba.KeContact", JsonUtils.buildJsonObject() //
								.addPropertyIfNotNull("ip", ip) //
								.addPropertyIfNotNull("phaseRotation", phaseRotation) //
								.build()), //
						new EdgeConfig.Component(ctrlEvcsId, controllerAlias, "Controller.Evcs",
								JsonUtils.buildJsonObject() //
										.addProperty("evcs.id", evcsId) //
										.build())//
				);
			} else {
				final var modbusId = this.getId(t, p, Property.MODBUS_ID);
				final var modbusUnitId = this.getInt(p, Property.MODBUS_UNIT_ID);
				components = Lists.newArrayList(//
						new EdgeConfig.Component(evcsId, alias, "Evcs.Keba.P40", JsonUtils.buildJsonObject() //
								.addPropertyIfNotNull("modbus.id", modbusId)//
								.addPropertyIfNotNull("modbusUnitId", modbusUnitId)//
								.addPropertyIfNotNull("phaseRotation", phaseRotation) //
								.addPropertyIfNotNull("readOnly", readOnly)//
								.build()),
						new EdgeConfig.Component(modbusId,
								TranslationUtil.getTranslation(bundle, "App.Evcs.Keba.modbus.alias"),
								"Bridge.Modbus.Tcp", JsonUtils.buildJsonObject() //
										.addProperty("ip", ip) //
										.onlyIf(t == ConfigurationTarget.ADD, b -> b //
												.addProperty("port", 502)) //
										.build())); //
				if (!readOnly) {
					components.add(new EdgeConfig.Component(ctrlEvcsId, controllerAlias, "Controller.Evcs",
							JsonUtils.buildJsonObject() //
									.addProperty("evcs.id", evcsId) //
									.build()));
				}
			}

			var appConfig = AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.schedulerByCentralOrder(
							new SchedulerComponent(ctrlEvcsId, "Controller.Evcs", this.getAppId()))) //
					.throwingOnlyIf(ip.startsWith("192.168.25."),
							b -> b.addTask(Tasks.staticIp(new InterfaceConfiguration("eth0") //
									.addIp("Evcs", "192.168.25.10/24"))));

			if (!readOnly) {
				appConfig.addDependencies(EvcsCluster.dependency(t, this.componentManager, this.componentUtil,
						maxHardwarePowerPerPhase, evcsId));
			}

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

}
