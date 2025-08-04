package io.openems.edge.app.evcs.readonly;

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
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.evcs.readonly.AppGoeEvcsReadOnly.Property;
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
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;

/**
 * Describes a GoE evcs App.
 *
 * <pre>
 {
 "appId":"App.Evcs.Goe.ReadOnly",
 "alias":"Goe Ladestation",
 "instanceId": UUID,
 "image": base64,
 "properties":{
 "EVCS_ID": "evcs0",
 "IP":"192.168.178.85",
 },
 "appDescriptor": {
 "websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
 }
 }
 * </pre>
 */
@Component(name = "App.Evcs.Goe.ReadOnly")
public class AppGoeEvcsReadOnly extends AbstractOpenemsAppWithProps<AppGoeEvcsReadOnly, Property, BundleParameter>
		implements OpenemsApp, HostSupplier {

	public enum Property implements Type<Property, AppGoeEvcsReadOnly, BundleParameter>, Nameable {
		// Component-IDs
		EVCS_ID(AppDef.componentId("evcs0")), //
		ALIAS(CommonProps.alias()), //
		IP(AppDef.copyOfGeneric(CommunicationProps.excludingIp()) //
				.setDefaultValue("192.168.25.11") //
				.setRequired(true)), //
		;

		private final AppDef<? super AppGoeEvcsReadOnly, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super AppGoeEvcsReadOnly, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, AppGoeEvcsReadOnly, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super AppGoeEvcsReadOnly, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<AppGoeEvcsReadOnly>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	private final Host host;

	@Activate
	public AppGoeEvcsReadOnly(@Reference ComponentManager componentManager, //
							  ComponentContext componentContext, //
							  @Reference ConfigurationAdmin cm, //
							  @Reference ComponentUtil componentUtil, //
							  @Reference Host host //
	) {
		super(componentManager, componentContext, cm, componentUtil);
		this.host = host;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			// values the user enters
			final var ip = this.getString(p, Property.IP);
			final var alias = this.getString(p, l, Property.ALIAS);
			final var evcsId = this.getId(t, p, Property.EVCS_ID);

			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(evcsId, //
							alias, "Evcs.Goe.Http", //
							JsonUtils.buildJsonObject() //
									.addProperty("ip", ip) //
									.build())
			);

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
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
	protected AppGoeEvcsReadOnly getApp() {
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
