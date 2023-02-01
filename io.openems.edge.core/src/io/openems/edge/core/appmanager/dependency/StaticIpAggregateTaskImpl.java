package io.openems.edge.core.appmanager.dependency;

import java.util.LinkedList;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.InterfaceConfiguration;

@Component
public class StaticIpAggregateTaskImpl implements AggregateTask, AggregateTask.StaticIpAggregateTask {

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
	public void aggregate(AppConfiguration instance, AppConfiguration oldConfig) {
		if (this.isWindows) {
			return;
		}
		if (instance != null) {
			this.ips.addAll(instance.ips);
		}
		if (oldConfig != null) {
			this.ips2Delete.addAll(oldConfig.ips);
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
		if (this.isWindows) {
			return;
		}
		InterfaceConfiguration.removeDuplicatedIps(ipsToDelete, //
				AppManagerAppHelperImpl.getStaticIpsFromConfigs(otherAppConfigurations));
		this.componentUtil.updateHosts(//
				user, //
				ips == null ? null : InterfaceConfiguration.summarize(ips), //
				InterfaceConfiguration.summarize(ipsToDelete) //
		);
	}

}
