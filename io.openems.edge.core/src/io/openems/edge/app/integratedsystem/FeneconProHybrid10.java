package io.openems.edge.app.integratedsystem;

import static io.openems.edge.app.common.props.CommonProps.alias;

import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.AppManagerUtilSupplier;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.enums.InputType;

/**
 * Describes a FENECON Pro Hybrid 10 system.
 *
 * <pre>
 	{
		 "appId":"App.FENECON.ProHybrid.10",
		 "alias":"FENECON Pro Hybrid 10",
		 "instanceId": UUID,
		 "image": base64,
		 "properties":{
			 "SERIAL_NUMBER" : null,
			 "IP" : null,
			 "USER_KEY" : "xxx"
	 		}
 	}
 * </pre>
 */
@Component(name = "App.FENECON.ProHybrid.10")
public class FeneconProHybrid10
		extends AbstractOpenemsAppWithProps<FeneconProHybrid10, FeneconProHybrid10.Property, Parameter.BundleParameter>
		implements OpenemsApp, AppManagerUtilSupplier {

	public enum Property implements Type<Property, FeneconProHybrid10, Parameter.BundleParameter> {
		ALIAS(alias()), //
		SERIAL_NUMBER(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".serialNumber.label") //
				.setTranslatedDescriptionWithAppPrefix(".serialNumber.description") //
				.setRequired(false) //
				.setField(JsonFormlyUtil::buildInputFromNameable))), //
		IP(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".ip.label") //
				.setTranslatedDescriptionWithAppPrefix(".ip.description") //
				.setRequired(false) //
				.setField(JsonFormlyUtil::buildInputFromNameable))), //
		USER_KEY(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".userkey.label") //
				.setTranslatedDescriptionWithAppPrefix(".userkey.description") //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildInput, (app, prop, l, params, field) -> {
					field.setInputType(InputType.PASSWORD);
				}) //
				.bidirectional("kacoCore0", "userkey", ComponentManagerSupplier::getComponentManager, t -> {
					return JsonUtils.getAsOptionalString(t) //
							.map(s -> {
								if (s.isEmpty()) {
									return null;
								}
								return new JsonPrimitive("xxx");
							}) //
							.orElse(null);
				})));

		private final AppDef<? super FeneconProHybrid10, ? super Property, ? super Parameter.BundleParameter> def;

		Property(AppDef<? super FeneconProHybrid10, ? super Property, ? super Parameter.BundleParameter> def) {
			this.def = def;
		}

		@Override
		public AppDef<? super FeneconProHybrid10, ? super Property, ? super Parameter.BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<FeneconProHybrid10>, Parameter.BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

		@Override
		public Type<Property, FeneconProHybrid10, Parameter.BundleParameter> self() {
			return this;
		}
	}

	private final AppManagerUtil appManagerUtil;

	@Activate
	public FeneconProHybrid10(@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil, //
			@Reference final AppManagerUtil appManagerUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
		this.appManagerUtil = appManagerUtil;
	}

	@Override
	protected FeneconProHybrid10 getApp() {
		return this;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsError.OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var serialNumber = this.getStringOrNull(p, l, Property.SERIAL_NUMBER);
			final var ip = this.getStringOrNull(p, l, Property.IP);
			final var userkey = this.getStringOrNull(p, l, Property.USER_KEY);

			final var gridMeterId = "meter0";
			final var essId = "ess0";
			final var kacoCoreId = "kacoCore0";
			final var chargerId = "charger0";

			final var components = Lists.newArrayList(//
					ProHybrid10Components.kacoCore(kacoCoreId, serialNumber, ip, userkey), //
					ProHybrid10Components.ess(essId), //
					ProHybrid10Components.gridMeter(gridMeterId), //
					ProHybrid10Components.charger(chargerId));

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
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
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.setWebsiteUrl(oem.getAppWebsiteUrl(this.getAppId())) //
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
