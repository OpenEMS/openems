package io.openems.edge.core.appmanager.dependency;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.ComponentUtil;

@Component
public class StaticIpAggregateTaskImpl implements AggregateTask, AggregateTask.StaticIpAggregateTask {

	private final ComponentUtil componentUtil;

	private List<String> ips;
	private List<String> ips2Delete;

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
		if (System.getProperty("os.name").startsWith("Windows")) {
			return;
		}
		if (instance != null) {
			this.ips.addAll(instance.ips);
		}
		if (oldConfig != null) {
			var diff = new ArrayList<>(oldConfig.ips);
			if (instance != null) {
				diff.removeAll(instance.ips);
			}
			this.ips2Delete.addAll(diff);
		}
	}

	@Override
	public void create(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		if (System.getProperty("os.name").startsWith("Windows")) {
			return;
		}

		this.componentUtil.updateHosts(user, this.ips, this.ips2Delete);
	}

	@Override
	public void delete(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		if (System.getProperty("os.name").startsWith("Windows")) {
			return;
		}
		this.ips.removeAll(AppManagerAppHelperImpl.getStaticIpsFromConfigs(otherAppConfigurations));
		this.componentUtil.updateHosts(user, null, this.ips2Delete);
	}

}
