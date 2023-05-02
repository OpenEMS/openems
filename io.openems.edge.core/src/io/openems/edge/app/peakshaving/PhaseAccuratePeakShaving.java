package io.openems.edge.app.peakshaving;

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
import io.openems.edge.app.common.props.ComponentProps;
import io.openems.edge.app.peakshaving.PhaseAccuratePeakShaving.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;

/**
 * Describes a asymmetric peak shaving app.
 *
 * <pre>
  {
    "appId":"App.PeakShaving.PeakShaving",
    "alias":"Phasengenaue Lastspitzenkappung",
    "instanceId": UUID,
    "image": base64,
    "properties":{
      "CTRL_PEAK_SHAVING_ID": "ctrlPeakShaving0",
      "ESS_ID": "ess0",
      "METER_ID":"meter0",
      "PEAK_SHAVING_POWER": 7000,
      "RECHARGE_POWER": 6000
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.PeakShaving.PhaseAccuratePeakShaving")
public class PhaseAccuratePeakShaving
		extends AbstractOpenemsAppWithProps<PhaseAccuratePeakShaving, Property, Parameter.BundleParameter>
		implements OpenemsApp {

	public enum Property implements Type<Property, PhaseAccuratePeakShaving, Parameter.BundleParameter>, Nameable {
		// Component-IDs
		CTRL_PEAK_SHAVING_ID(AppDef.componentId("ctrlPeakShaving0")), //
		// Properties
		ALIAS(AppDef.copyOfGeneric(CommonProps.alias())), //
		ESS_ID(AppDef.copyOfGeneric(ComponentProps.pickManagedSymmetricEssId(),
				def -> def.wrapField((app, property, l, parameter, field) -> field.isRequired(true)) //
						.bidirectional(CTRL_PEAK_SHAVING_ID, "ess.id", //
								ComponentManagerSupplier::getComponentManager))), //
		METER_ID(AppDef.copyOfGeneric(ComponentProps.pickSymmetricGridMeterId(),
				def -> def.wrapField((app, property, l, parameter, field) -> field.isRequired(true)) //
						.bidirectional(CTRL_PEAK_SHAVING_ID, "meter.id", //
								ComponentManagerSupplier::getComponentManager))), //
		PEAK_SHAVING_POWER(AppDef.copyOf(Property.class, PeakShavingProps.peakShavingPowerPerPhase()) //
				.wrapField((app, property, l, parameter, field) -> {
					field.isRequired(true);
				}) //
				.setAutoGenerateField(false) //
				.bidirectional(CTRL_PEAK_SHAVING_ID, "peakShavingPower", //
						ComponentManagerSupplier::getComponentManager)), //
		RECHARGE_POWER(AppDef.copyOf(Property.class, PeakShavingProps.rechargePowerPerPhase()) //
				.wrapField((app, property, l, parameter, field) -> {
					field.isRequired(true);
				}) //
				.setAutoGenerateField(false) //
				.bidirectional(CTRL_PEAK_SHAVING_ID, "rechargePower", //
						ComponentManagerSupplier::getComponentManager)), //
		PEAK_SHAVING_RECHARGE_POWER_GROUP(AppDef.copyOfGeneric(PeakShavingProps.<PhaseAccuratePeakShaving, Property>//
				peakShavingRechargePowerGroup(PEAK_SHAVING_POWER, RECHARGE_POWER)) //
				.setTranslationBundleSupplier(BundleParameter::getBundle)), //
		;

		private final AppDef<? super PhaseAccuratePeakShaving, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super PhaseAccuratePeakShaving, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, PhaseAccuratePeakShaving, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super PhaseAccuratePeakShaving, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<PhaseAccuratePeakShaving>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	@Activate
	public PhaseAccuratePeakShaving(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/fenecon-fems/fems-app-phasengenaue-lastspitzenkappung/") //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.PEAK_SHAVING };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE_IN_CATEGORY;
	}

	@Override
	protected PhaseAccuratePeakShaving getApp() {
		return this;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, m, l) -> {
			final var ctrlPeakShavingId = this.getId(t, m, Property.CTRL_PEAK_SHAVING_ID);

			final var alias = this.getString(m, l, Property.ALIAS);
			final var essId = this.getString(m, l, Property.ESS_ID);
			final var meterId = this.getString(m, l, Property.METER_ID);
			final var peakShavingPower = this.getInt(m, Property.PEAK_SHAVING_POWER);
			final var rechargePower = this.getInt(m, Property.RECHARGE_POWER);

			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlPeakShavingId, alias, "Controller.Asymmetric.PeakShaving",
							JsonUtils.buildJsonObject() //
									.addProperty("ess.id", essId) //
									.addProperty("meter.id", meterId) //
									.addProperty("peakShavingPower", peakShavingPower) //
									.addProperty("rechargePower", rechargePower) //
									.build()));

			return new AppConfiguration(components);
		};
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

}
