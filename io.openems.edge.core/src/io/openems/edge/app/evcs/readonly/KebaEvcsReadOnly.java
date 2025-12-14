package io.openems.edge.app.evcs.readonly;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommunicationProps.modbusUnitId;

import java.util.ArrayList;
import java.util.Map;
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
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.enums.KebaHardwareType;
import io.openems.edge.app.evcs.EvcsProps;
import io.openems.edge.app.evcs.readonly.KebaEvcsReadOnly.Property;
import io.openems.edge.common.component.ComponentManager;
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
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.Exp;

/**
 * Describes a Keba evcs App for modbus read only.
 *
 * <pre>
  {
    "appId":"App.Evcs.Keba",
    "alias":"KEBA Ladestation",
    "instanceId": UUID,
    "image": base64,
    "properties":{
      "EVCS_ID": "evcs0",
      "IP":"192.168.25.11",
      "MODBUS_UNIT_ID": 255,
      "PHASE_ROTATION":"L1_L2_L3"
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.Evcs.Keba.ReadOnly")
public class KebaEvcsReadOnly extends AbstractOpenemsAppWithProps<KebaEvcsReadOnly, Property, Parameter.BundleParameter>
		implements OpenemsApp, HostSupplier {

	public enum Property implements Type<Property, KebaEvcsReadOnly, Parameter.BundleParameter>, Nameable {
		// Component-IDs
		EVCS_ID(AppDef.componentId("evcs0")), //
		// Properties
		ALIAS(alias()), //
		HARDWARE_TYPE(EvcsProps.hardwareType(EVCS_ID)), //
		IP(AppDef.copyOfGeneric(CommunicationProps.excludingIp())//
				.setDefaultValue("192.168.25.11")//
				.setRequired(true)), //
		PHASE_ROTATION(AppDef.copyOfGeneric(EvcsProps.phaseRotation())), //
		// Properties for P40
		MODBUS_ID(AppDef.componentId("modbus0")), //
		MODBUS_UNIT_ID(AppDef.copyOfGeneric(modbusUnitId(), def -> def //
				.setDefaultValue(255) //
				.wrapField((app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(HARDWARE_TYPE)//
							.equal(Exp.staticValue(KebaHardwareType.P40)));
				}))), //
		;

		private final AppDef<? super KebaEvcsReadOnly, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super KebaEvcsReadOnly, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, KebaEvcsReadOnly, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super KebaEvcsReadOnly, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<KebaEvcsReadOnly>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	private final Host host;

	@Activate
	public KebaEvcsReadOnly(@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil, //
			@Reference Host host) {
		super(componentManager, componentContext, cm, componentUtil);
		this.host = host;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var bundle = AbstractOpenemsApp.getTranslationBundle(l);

			// values the user enters
			final var ip = this.getString(p, l, Property.IP);
			final var alias = this.getString(p, l, Property.ALIAS);
			final var hardwareType = this.getEnum(p, KebaHardwareType.class, Property.HARDWARE_TYPE);
			final var phaseRotation = this.getString(p, l, Property.PHASE_ROTATION);

			// values which are being auto generated by the appmanager
			final var evcsId = this.getId(t, p, Property.EVCS_ID);

			ArrayList<io.openems.common.types.EdgeConfig.Component> components = Lists.newArrayList();
			switch (hardwareType) {
			case P30 -> {
				components.add(new EdgeConfig.Component(evcsId, alias, "Evcs.Keba.KeContact",
						JsonUtils.buildJsonObject() //
								.addPropertyIfNotNull("ip", ip) //
								.addPropertyIfNotNull("phaseRotation", phaseRotation) //
								.addProperty("readOnly", true).build()));
			}
			case P40 -> {
				final var modbusId = this.getId(t, p, Property.MODBUS_ID);
				final var modbusUnitId = this.getInt(p, Property.MODBUS_UNIT_ID);
				components.addAll(Lists.newArrayList(//
						new EdgeConfig.Component(evcsId, alias, "Evcs.Keba.P40", JsonUtils.buildJsonObject() //
								.addPropertyIfNotNull("modbus.id", modbusId)//
								.addPropertyIfNotNull("modbusUnitId", modbusUnitId)//
								.addPropertyIfNotNull("phaseRotation", phaseRotation) //
								.addProperty("readOnly", true).build()),
						new EdgeConfig.Component(modbusId,
								TranslationUtil.getTranslation(bundle, "App.Evcs.Keba.modbus.alias"),
								"Bridge.Modbus.Tcp", JsonUtils.buildJsonObject() //
										.addProperty("ip", ip) //
										.onlyIf(t == ConfigurationTarget.ADD, b -> b //
												.addProperty("port", 502)) //
										.build()))); //
			}
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
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.EVCS_READ_ONLY };
	}

	@Override
	protected KebaEvcsReadOnly getApp() {
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

}
