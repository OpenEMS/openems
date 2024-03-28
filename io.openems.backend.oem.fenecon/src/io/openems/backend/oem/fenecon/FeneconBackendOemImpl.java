package io.openems.backend.oem.fenecon;

import static java.util.stream.Collectors.toSet;

import java.util.Set;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.oem.OpenemsBackendOem;
import io.openems.common.session.AbstractUser;
import io.openems.common.utils.StringUtils;

@Designate(ocd = Config.class, factory = false)
@Component(name = "Oem.Fenecon")
public class FeneconBackendOemImpl implements OpenemsBackendOem {

	private Config config;
	private Set<String> userIds;

	/**
	 * Activate.
	 * 
	 * @param config the configuration
	 */
	@Activate
	public void activate(Config config) {
		this.applyConfig(config);
	}

	/**
	 * Modified.
	 * 
	 * @param config the configuration
	 */
	@Modified
	public void modified(Config config) {
		this.applyConfig(config);
	}

	private final void applyConfig(Config config) {
		this.config = config;
		this.userIds = Stream.of(config.demoUserIds()).collect(toSet());
	}

	@Override
	public String getAppCenterMasterKey() {
		return this.config.appCenterMasterKey();
	}

	@Override
	public String getInfluxdbTag() {
		return "fems";
	}

	@Override
	public String anonymizeEdgeComment(AbstractUser user, String comment, String edgeId) {
		if (!this.userIds.contains(user.getId())) {
			return comment;
		}

		final var edgeNumber = StringUtils.parseNumberFromName(edgeId);
		if (edgeNumber.isEmpty()) {
			return edgeId;
		}

		return "FEMS " + edgeNumber.getAsInt();
	}

}
