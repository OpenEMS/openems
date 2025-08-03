package io.openems.edge.app.evse.vehicle;

import static io.openems.edge.app.common.props.CommonProps.alias;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.evse.vehicle.AppGenericVehicle.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;

@Component(name = "App.Evse.ElectricVehicle.Generic")
public class AppGenericVehicle extends
		AbstractOpenemsAppWithProps<AppGenericVehicle, Property, Parameter.BundleParameter> implements OpenemsApp {

	public enum Property implements Type<Property, AppGenericVehicle, Parameter.BundleParameter> {
		VEHICLE_ID(AppDef.componentId("evseElectricVehicle0")), //
		ALIAS(AppDef.copyOfGeneric(alias())), //
		MAX_POWER_SINGLE_PHASE(VehicleProps.maxPowerSinglePhase()), //
		MAX_POWER_THREE_PHASE(VehicleProps.maxPowerThreePhase()), //
		CAN_INTERRUPT(VehicleProps.canInterupt());

		private final AppDef<? super AppGenericVehicle, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super AppGenericVehicle, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, AppGenericVehicle, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super AppGenericVehicle, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<AppGenericVehicle>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public AppGenericVehicle(//
			@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {

			final var id = this.getId(t, p, Property.VEHICLE_ID);
			final var alias = this.getString(p, l, Property.ALIAS);
			final var maxPowerSinglePhase = this.getInt(p, Property.MAX_POWER_SINGLE_PHASE);
			final var maxPowerThreePhase = this.getInt(p, Property.MAX_POWER_THREE_PHASE);
			final var canInterrupt = this.getBoolean(p, Property.CAN_INTERRUPT);

			final var components = new ArrayList<EdgeConfig.Component>();

			components.add(new EdgeConfig.Component(id, alias, "Evse.ElectricVehicle.Generic",
					JsonUtils.buildJsonObject()//
							.addProperty("maxPowerSinglePhase", maxPowerSinglePhase)//
							.addProperty("maxPowerThreePhase", maxPowerThreePhase)//
							.addProperty("canInterrupt", canInterrupt)//
							.build()));

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.build();
		};
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.ELECTRIC_VEHCILE };
	}

	@Override
	protected AppGenericVehicle getApp() {
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
				.setCanDelete(Role.ADMIN)//
				.setCanInstall(List.of(Role.ADMIN))//
				.build();
	}
}
