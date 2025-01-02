package io.openems.edge.core.appmanager.dependency.aggregatetask;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.InterfaceConfiguration;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.AppManagerAppHelperImpl;

@Component(//
		service = { //
				AggregateTask.class, //
				StaticIpAggregateTask.class, //
				StaticIpAggregateTaskImpl.class //
		}, //
		scope = ServiceScope.SINGLETON //
)
public class StaticIpAggregateTaskImpl implements StaticIpAggregateTask {

	private final boolean isWindows = System.getProperty("os.name").startsWith("Windows");

	private final ComponentUtil componentUtil;

	private List<InterfaceConfiguration> ips;
	private List<InterfaceConfiguration> ips2Delete;

	@Activate
	public StaticIpAggregateTaskImpl(@Reference ComponentUtil componentUtil) {
		this.componentUtil = componentUtil;
	}

	@Override
	public void reset() {
		this.ips = new LinkedList<>();
		this.ips2Delete = new LinkedList<>();
	}

	@Override
	public void aggregate(StaticIpConfiguration instance, StaticIpConfiguration oldConfig) {
		if (this.isWindows) {
			return;
		}
		if (instance != null) {
			this.ips.addAll(instance.interfaceConfiguration());
		}
		if (oldConfig != null) {
			this.ips2Delete.addAll(oldConfig.interfaceConfiguration());
		}
	}

	@Override
	public void create(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		this.execute(user, otherAppConfigurations, this.ips, this.ips2Delete);
	}

	@Override
	public void delete(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		this.execute(user, otherAppConfigurations, null, this.ips2Delete);
	}

	private void execute(//
			final User user, //
			final List<AppConfiguration> otherAppConfigurations, //
			final List<InterfaceConfiguration> ips, //
			final List<InterfaceConfiguration> ipsToDelete //
	) throws OpenemsNamedException {
		if (!this.anyChanges()) {
			return;
		}
		InterfaceConfiguration.removeDuplicatedIps(ipsToDelete, //
				AppConfiguration
						.flatMap(otherAppConfigurations, StaticIpAggregateTask.class, c -> c.interfaceConfiguration())
						.toList());
		this.componentUtil.updateHosts(//
				user, //
				ips == null ? null : InterfaceConfiguration.summarize(ips), //
				InterfaceConfiguration.summarize(ipsToDelete) //
		);
	}

	@Override
	public String getGeneralFailMessage(Language l) {
		final var bundle = AppManagerAppHelperImpl.getTranslationBundle(l);
		return TranslationUtil.getTranslation(bundle, "canNotUpdateStaticIps");
	}

	@Override
	public void validate(List<String> errors, AppConfiguration appConfiguration, StaticIpConfiguration config) {
		// setting ip configuration is not implemented for windows
		if (this.isWindows) {
			return;
		}

		if (config.interfaceConfiguration().isEmpty()) {
			return;
		}
		try {
			var interfaces = this.componentUtil.getInterfaces();
			config.interfaceConfiguration().stream() //
					.forEach(i -> {
						var existingInterface = interfaces.stream() //
								.filter(t -> t.getName().equals(i.interfaceName)) //
								.findFirst().orElse(null);

						if (existingInterface == null) {
							errors.add("Interface '" + i.interfaceName + "' not found.");
							return;
						}

						var missingIps = i.getIps().stream() //
								.filter(ip -> {
									if (existingInterface.getAddresses().getValue().stream() //
											.anyMatch(existingIp -> existingIp.isInSameNetwork(ip))) {
										return false;
									}
									return true;
								}).collect(Collectors.toList());

						if (missingIps.isEmpty()) {
							return;
						}
						errors.add("Address '"
								+ missingIps.stream().map(t -> t.toString()).collect(Collectors.joining(", ")) + "' "
								+ (missingIps.size() > 1 ? "are" : "is") + " not added on " + i.interfaceName);
					});
		} catch (OpenemsNamedException ex) {
			ex.printStackTrace();
		}
	}

	private final boolean anyChanges() {
		return !this.isWindows && (this.ips.isEmpty() || this.ips2Delete.isEmpty());
	}

}
