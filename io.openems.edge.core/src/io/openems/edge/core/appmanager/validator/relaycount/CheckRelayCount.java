package io.openems.edge.core.appmanager.validator.relaycount;

import static java.util.Collections.emptyList;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.OpenemsConstants;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.validator.AbstractCheckable;
import io.openems.edge.core.appmanager.validator.Checkable;

@Component(//
		name = CheckRelayCount.COMPONENT_NAME, //
		scope = ServiceScope.PROTOTYPE //
)
public class CheckRelayCount extends AbstractCheckable implements Checkable {

	private final Logger log = LoggerFactory.getLogger(CheckRelayCount.class);

	public static final String COMPONENT_NAME = "Validator.Checkable.CheckRelayCount";

	private final ComponentUtil openemsAppUtil;
	private final OpenemsApp relayApp;

	@Reference
	private OpenemsEdgeOem oem;

	private String io;
	private int count;
	private List<InjectableComponentConfig> filter;

	private int availableRelays;

	@Activate
	public CheckRelayCount(//
			@Reference ComponentUtil openemsAppUtil, //
			ComponentContext componentContext, //
			@Reference(target = "(" + OpenemsConstants.PROPERTY_OSGI_COMPONENT_NAME
					+ "=App.Hardware.KMtronic8Channel)") OpenemsApp relayApp //
	) {
		super(componentContext);
		this.openemsAppUtil = openemsAppUtil;
		this.relayApp = relayApp;
	}

	private void init(String io, int count, InjectableComponentConfig[] filter) {
		this.io = io;
		this.count = count;
		this.filter = filter == null ? emptyList() : Arrays.asList(filter);
	}

	@Override
	public void setProperties(Map<String, ?> properties) {
		var io = (String) properties.get("io");
		var count = (int) properties.get("count");
		var filter = (InjectableComponentConfig[]) properties.get("filter");
		this.init(io, count, filter);
	}

	@Override
	public boolean check() {
		final var relayFilter = this.filter.stream() //
				.map(t -> {
					try {
						return InjectableComponent.inject(this.componentContext.getBundleContext(),
								CheckRelayCountFilter.class, t);
					} catch (Exception e) {
						this.log.error("Unable to inject " + t.name(), e);
						return null;
					}
				}) //
				.filter(Objects::nonNull) //
				.toList();

		final var relayInfos = this.openemsAppUtil.getAllRelayInfos(//
				t -> relayFilter.stream().allMatch(c -> c.componentFilter().test(t)), //
				(t, u) -> relayFilter.stream().allMatch(c -> c.channelFilter().test(t, u)), //
				(t, u) -> relayFilter.stream().flatMap(c -> c.disabledReasons().apply(t, u).stream()).toList() //
		);

		try {
			int availableRelays;
			if (this.io != null) {
				availableRelays = relayInfos.stream() //
						.filter(t -> t.id().equals(this.io)) //
						.flatMap(t -> t.channels().stream()) //
						.filter(t -> t.usingComponents().isEmpty()) //
						.filter(t -> t.disabledReasons().isEmpty()) //
						.toList().size();
			} else {
				availableRelays = relayInfos.stream() //
						.flatMap(t -> t.channels().stream()) //
						.filter(t -> t.usingComponents().isEmpty()) //
						.filter(t -> t.disabledReasons().isEmpty()) //
						.toList().size();
			}
			this.availableRelays = availableRelays;
			if (this.count <= availableRelays) {
				return true;
			}
		} catch (RuntimeException e) {
			// io not found so there are none available
			this.availableRelays = 0;
		}
		return false;
	}

	@Override
	public String getErrorMessage(Language language) {
		final var messageBuilder = new StringBuilder(//
				AbstractCheckable.getTranslation(language, //
						"Validator.Checkable.CheckRelayCount.Message", //
						this.count, this.availableRelays) //
		);

		// message to install additional relay
		if (this.relayApp != null) {
			messageBuilder.append(//
					AbstractCheckable.getTranslation(language, //
							"Validator.Checkable.CheckRelayCount.Message.AdditionalRelay", //
							this.relayApp.getAppDescriptor(this.oem).getWebsiteUrl(), //
							this.relayApp.getName(language)) //
			);
		}
		return messageBuilder.toString();
	}

	@Override
	public String getInvertedErrorMessage(Language language) {
		throw new UnsupportedOperationException();
	}

}
