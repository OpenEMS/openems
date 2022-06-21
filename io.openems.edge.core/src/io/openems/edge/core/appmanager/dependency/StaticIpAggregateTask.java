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

@Component(name = "AppManager.AggregateTask.StaticIpAggregateTask")
public class StaticIpAggregateTask implements AggregateTask {

	private final ComponentUtil componentUtil;

	private List<String> ips;
	private List<String> ips2Delete;

	@Activate
	public StaticIpAggregateTask(@Reference ComponentUtil componentUtil) {
		this.componentUtil = componentUtil;

		this.ips = new LinkedList<>();
		this.ips2Delete = new LinkedList<>();
	}

	@Override
	public void aggregate(AppConfiguration instance, AppConfiguration oldConfig) throws OpenemsNamedException {
		if (System.getProperty("os.name").startsWith("Windows")) {
			return;
		}
		if (instance != null) {
			ips.addAll(instance.ips);
		}
		if (oldConfig != null) {
			var diff = new ArrayList<>(oldConfig.ips);
			if (instance != null) {
				diff.removeAll(instance.ips);
			}
			ips2Delete.addAll(diff);
		}
	}

	@Override
	public void create(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		if (System.getProperty("os.name").startsWith("Windows")) {
			return;
		}

		this.componentUtil.updateHosts(user, ips, ips2Delete);

		this.ips = new LinkedList<>();
	}

	@Override
	public void delete(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		if (System.getProperty("os.name").startsWith("Windows")) {
			return;
		}
		ips.removeAll(AppManagerAppHelperImpl.getStaticIpsFromConfigs(otherAppConfigurations));
		this.componentUtil.updateHosts(user, null, ips2Delete);
		this.ips2Delete = new LinkedList<>();

	}

}
