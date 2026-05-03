package io.openems.edge.app.integratedsystem;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.integratedsystem.FeneconMiniProps.essPhase;
import static io.openems.edge.app.integratedsystem.FeneconMiniProps.essReadOnly;

import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.edge.app.enums.Phase;
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
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.dependency.Tasks;

/**
 * Describes a FENECON Mini ES 3-3 system.
 *
 * <pre>
 {
 "appId":"App.FENECON.Mini.ES.3.3",
 "alias":"FENECON Mini ES 3-3",
 "instanceId": UUID,
 "image": base64,
 "properties":{
 "ESS_PHASE": "l1"
 "ESS_READ_ONLY": true
 }
 }
 * </pre>
 */
@Component(name = "App.FENECON.Mini.ES.3.3")
public class FeneconMiniEs33
		extends AbstractOpenemsAppWithProps<FeneconMiniEs33, FeneconMiniEs33.Property, Parameter.BundleParameter>
		implements OpenemsApp, AppManagerUtilSupplier {

	public enum Property implements Type<Property, FeneconMiniEs33, Parameter.BundleParameter> {
		ALIAS(alias()), //
		ESS_PHASE(essPhase()), //
		ESS_READ_ONLY(essReadOnly());

		private final AppDef<? super FeneconMiniEs33, ? super Property, ? super Parameter.BundleParameter> def;

		Property(AppDef<? super FeneconMiniEs33, ? super Property, ? super Parameter.BundleParameter> def) {
			this.def = def;
		}

		@Override
		public AppDef<? super FeneconMiniEs33, ? super Property, ? super Parameter.BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<FeneconMiniEs33>, Parameter.BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

		@Override
		public Type<Property, FeneconMiniEs33, Parameter.BundleParameter> self() {
			return this;
		}
	}

	private final AppManagerUtil appManagerUtil;

	@Activate
	public FeneconMiniEs33(@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil, //
			@Reference final AppManagerUtil appManagerUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
		this.appManagerUtil = appManagerUtil;
	}

	@Override
	protected FeneconMiniEs33 getApp() {
		return this;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsError.OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {

			final var gridMeterId = "meter0";
			final var pvMeterId = "meter1";
			final var essId = "ess0";
			final var modbusId = "modbus0";

			final var essPhase = this.getEnum(p, Phase.class, Property.ESS_PHASE);
			final var essReadOnly = this.getBoolean(p, Property.ESS_READ_ONLY);

			final var components = Lists.newArrayList(//
					FeneconMiniComponents.ess(essId, modbusId, essPhase, essReadOnly), //
					FeneconMiniComponents.gridMeter(gridMeterId, modbusId), //
					FeneconMiniComponents.pvMeter(pvMeterId, modbusId), //
					FeneconMiniComponents.modbus(modbusId) //
			);

			return AppConfiguration.create() //
					.addTask(Tasks.componentFromComponentConfig(components)) //
					.build();
		};
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public AppManagerUtil getAppManagerUtil() {
		return this.appManagerUtil;
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem, Language language) {
		return AppDescriptor.create() //
				.setWebsiteUrl(oem.getAppWebsiteUrl(this.getAppId(), language)) //
				.build();
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create() //
				.setCanSee(Role.INSTALLER) //
				.setCanDelete(Role.INSTALLER) //
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
}
