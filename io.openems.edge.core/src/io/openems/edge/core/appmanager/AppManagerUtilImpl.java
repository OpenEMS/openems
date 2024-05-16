package io.openems.edge.core.appmanager;

import static java.util.Collections.emptyList;

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
	public List<OpenemsAppInstance> getInstantiatedApps() {
		return Optional.ofNullable(this.getAppManagerImpl()) //
				.map(AppManagerImpl::getInstantiatedApps) //
				.orElse(emptyList());
	}

	@Override
	public Optional<OpenemsApp> findAppById(String id) {
		return Optional.ofNullable(this.getAppManagerImpl()) //
				.flatMap(t -> t.findAppById(id));
	}

	@Override
	public Optional<OpenemsAppInstance> findInstanceById(UUID id) {
		return Optional.ofNullable(this.getAppManagerImpl()) //
				.flatMap(t -> t.findInstanceById(id));
	}

	@Override
	public AppConfiguration getAppConfiguration(ConfigurationTarget target, OpenemsApp app, String alias,
			JsonObject properties, Language language) throws OpenemsNamedException {
		final var copy = properties.deepCopy();
		if (alias != null) {
			copy.addProperty("ALIAS", alias);
		}

		return app.getAppConfiguration(target, copy, language);
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
