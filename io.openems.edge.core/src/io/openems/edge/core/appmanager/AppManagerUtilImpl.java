package io.openems.edge.core.appmanager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.edge.common.component.ComponentManager;

@Component
public class AppManagerUtilImpl implements AppManagerUtil {

	private final ComponentManager componentManager;

	@Activate
	public AppManagerUtilImpl(@Reference ComponentManager componentManager) {
		this.componentManager = componentManager;
	}

	@Override
	public Optional<OpenemsApp> findAppById(String id) {
		return this.getAppManagerImpl().findAppById(id);
	}

	@Override
	public Optional<OpenemsAppInstance> findInstanceById(UUID id) {
		return this.getAppManagerImpl().findInstanceById(id);
	}

	@Override
	public AppConfiguration getAppConfiguration(ConfigurationTarget target, OpenemsApp app, String alias,
			JsonObject properties, Language language) throws OpenemsNamedException {
		if (alias != null) {
			properties.addProperty("ALIAS", alias);
		}
		try {
			return app.getAppConfiguration(target, properties, language);
		} finally {
			if (alias != null) {
				properties.remove("ALIAS");
			}
		}
	}

	@Override
	public List<OpenemsAppInstance> getAppsWithDependencyTo(OpenemsAppInstance instance) {
		return this.getAppManagerImpl().getInstantiatedApps().stream()
				.filter(t -> t.dependencies != null && !t.dependencies.isEmpty())
				.filter(t -> t.dependencies.stream().anyMatch(d -> d.instanceId.equals(instance.instanceId)))
				.collect(Collectors.toList());
	}

	private final AppManagerImpl getAppManagerImpl() {
		var appManagerList = this.componentManager.getEnabledComponentsOfType(AppManager.class);
		if (appManagerList.size() == 0) {
			return null;
		}
		return (AppManagerImpl) appManagerList.get(0);
	}

}
