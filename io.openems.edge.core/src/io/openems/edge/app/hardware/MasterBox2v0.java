package io.openems.edge.app.hardware;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.modbusInternal;

import java.util.Arrays;
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
import io.openems.common.session.Role;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentDef;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

/**
 * Describes an App for MasterBox2v0.
 *
 * <pre>
  {
    "appId":"App.Hardware.MasterBox2v0",
    "alias": string,
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"IO_ID": "ioc0",
    	"HARDWARE_TYPE": "MODBERRY_X500_M40804_WB"
    },
    "appDescriptor": {
    	"websiteUrl": URL
    }
  }
 * </pre>
 */
@Component(name = "App.Hardware.MasterBox2v0")
public class MasterBox2v0 extends AbstractOpenemsAppWithProps<MasterBox2v0, MasterBox2v0.Property, BundleParameter>
		implements OpenemsApp {

	public enum Property implements Type<Property, MasterBox2v0, BundleParameter> {
		IOC_ID(AppDef.componentId("ioc0")), //
		ALIAS(AppDef.copyOfGeneric(CommonProps.alias())), //
		HARDWARE_TYPE(AppDef.copyOfGeneric(defaultDef(), appDef -> appDef //
				.setTranslatedLabel("App.Hardware.hardwareType.label") //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> //
				field.setOptions(Arrays.stream(GpioHardwareType.values())//
						.map(Enum::name)//
						.toList()))//
				.setRequired(true)//
				.setDefaultValue(GpioHardwareType.MODBERRY_X500_M40804_WB)//
				.bidirectional(IOC_ID, "hardwareType", ComponentManagerSupplier::getComponentManager))), //
		;

		private final AppDef<? super MasterBox2v0, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super MasterBox2v0, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, MasterBox2v0, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super MasterBox2v0, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<MasterBox2v0>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public MasterBox2v0(//
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
			final var bundle = AbstractOpenemsApp.getTranslationBundle(l);
			final var iocId = this.getId(t, p, Property.IOC_ID);
			final var meterId = "meter1";
			final var ioId = "io0";
			final var analogOutputId = "analogOutput0";
			final var internalModbusId = "modbus0";

			final var components = Lists.newArrayList(//
					HardwareComponents.ioc(bundle, iocId, internalModbusId), //
					HardwareComponents.masterBoxMeter(bundle, meterId, iocId), //
					HardwareComponents.masterBoxIo(bundle, ioId, iocId), //
					HardwareComponents.masterBoxAnalogOutput(bundle, analogOutputId, iocId), //
					ComponentDef.from(modbusInternal(bundle, t, internalModbusId),
							ComponentDef.Configuration.defaultConfig().withForceUpdateOrCreate(true)) //
			);

			return AppConfiguration.create() //
					.addTask(Tasks.componentFromComponentConfig(components)) //
					.build();
		};
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
	protected MasterBox2v0 getApp() {
		return this;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create() //
				.setCanSee(Role.ADMIN) //
				.setCanDelete(Role.ADMIN) //
				.build();
	}
}
