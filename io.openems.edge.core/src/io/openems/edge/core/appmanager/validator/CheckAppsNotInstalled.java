package io.openems.edge.core.appmanager.validator;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.AppManager;
import io.openems.edge.core.appmanager.AppManagerImpl;

@Component(//
		name = CheckAppsNotInstalled.COMPONENT_NAME, //
		scope = ServiceScope.PROTOTYPE //
)
public class CheckAppsNotInstalled extends AbstractCheckable implements Checkable {

	public static final String COMPONENT_NAME = "Validator.Checkable.CheckAppsNotInstalled";

	private final AppManager appManager;
	private String[] appIds;

	private List<String> installedApps = new LinkedList<>();

	@Activate
	public CheckAppsNotInstalled(@Reference AppManager appManager, ComponentContext componentContext) {
		super(componentContext);
		this.appManager = appManager;
	}

	@Override
	public void setProperties(Map<String, ?> properties) {
		this.appIds = (String[]) properties.get("appIds");
	}

	@Override
	public boolean check() {
		this.installedApps = new LinkedList<>();
		var appManagerImpl = this.getAppManagerImpl();
		if (appManagerImpl == null) {
			return false;
		}

		var instances = appManagerImpl.getInstantiatedApps();
		for (String item : this.appIds) {
			if (instances.stream().anyMatch(t -> t.appId.equals(item))) {
				this.installedApps.add(item);
			}
		}
		return this.installedApps.isEmpty();
	}

	private AppManagerImpl getAppManagerImpl() {
		if (this.appManager == null || !(this.appManager instanceof AppManagerImpl)) {
			return null;
		}
		return (AppManagerImpl) this.appManager;
	}

	@Override
	public String getErrorMessage(Language language) {
		final var appManagerImpl = this.getAppManagerImpl();
		var appNameStream = this.installedApps.stream();
		if (appManagerImpl != null) {
			appNameStream = appNameStream.map(id -> {
				final var app = appManagerImpl.findAppById(id).orElse(null);
				if (app != null) {
					return app.getName(language);
				}
				return id;
			});
		}
		return AbstractCheckable.getTranslation(language, "Validator.Checkable.CheckAppsNotInstalled.Message",
				appNameStream.collect(Collectors.joining(", ")));
	}

}
