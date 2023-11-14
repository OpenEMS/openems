package io.openems.edge.app.hardware;

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
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.hardware.KMtronic8Channel.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.InterfaceConfiguration;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;

/**
 * Describes a App for KMtronic 8-Channel Relay.
 *
 * <pre>
  {
    "appId":"App.Hardware.KMtronic8Channel",
    "alias":"FEMS Relais 8-Kanal",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"IO_ID": "io0",
    	"MODBUS_ID": "modbus10",
    	"IP": "192.168.1.199"
    },
    "appDescriptor": {
    	"websiteUrl": URL
    }
  }
 * </pre>
 */
@Component(name = "App.Hardware.KMtronic8Channel")
public class KMtronic8Channel extends AbstractOpenemsAppWithProps<KMtronic8Channel, Property, Parameter.BundleParameter>
		implements OpenemsApp {

	public static enum Property implements Type<Property, KMtronic8Channel, Parameter.BundleParameter> {
		// Component-IDs
		IO_ID(AppDef.componentId("io1")), //
		MODBUS_ID(AppDef.componentId("modbus10")), //
		// Properties
		ALIAS(AppDef.copyOfGeneric(CommonProps.alias())), //
		IP(AppDef.copyOfGeneric(CommunicationProps.ip(), //
				def -> def.setTranslatedDescriptionWithAppPrefix(".ip.description") //
						.setDefaultValue("192.168.1.199") //
						.setRequired(true))), //
		CHECK(AppDef.copyOfGeneric(CommonProps.installationHint(//
				(app, property, l, parameter) -> TranslationUtil.getTranslation(parameter.bundle, //
						"App.Hardware.KMtronic8Channel.installationHint")))), //
		;

		private final AppDef<? super KMtronic8Channel, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super KMtronic8Channel, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, KMtronic8Channel, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super KMtronic8Channel, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<KMtronic8Channel>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public KMtronic8Channel(//
			@Reference ComponentManager componentManager, //
			ComponentContext context, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil //
	) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var alias = this.getString(p, l, Property.ALIAS);
			final var ip = this.getString(p, Property.IP);

			final var modbusId = this.getId(t, p, Property.MODBUS_ID);
			final var ioId = this.getId(t, p, Property.IO_ID);

			final var comp = Lists.newArrayList(//
					new EdgeConfig.Component(ioId, alias, "IO.KMtronic", //
							JsonUtils.buildJsonObject() //
									.addProperty("modbus.id", modbusId) //
									.build()), //
					new EdgeConfig.Component(modbusId, "bridge", "Bridge.Modbus.Tcp", JsonUtils.buildJsonObject() //
							.addProperty("ip", ip) //
							.build())//
			);

			return AppConfiguration.create() //
					.addTask(Tasks.component(comp)) //
					.throwingOnlyIf(ip.startsWith("192.168.1."),
							b -> b.addTask(Tasks.staticIp(new InterfaceConfiguration("eth0") //
									.addIp("Relay", "192.168.1.198/28")))) //
					.build();
		};
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/produkte/fems/fems-relais/") //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.HARDWARE };
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	protected KMtronic8Channel getApp() {
		return this;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

}
