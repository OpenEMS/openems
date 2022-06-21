package io.openems.edge.core.appmanager.validator;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.AppManager;
import io.openems.edge.core.appmanager.AppManagerImpl;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppInstance;

@Component(name = CheckCardinality.COMPONENT_NAME)
public class CheckCardinality extends AbstractCheckable implements Checkable {

	public static final String COMPONENT_NAME = "Validator.Checkable.CheckCardinality";

	private final AppManager appManager;
	private OpenemsApp openemsApp;

	private String errorMessage = null;

	@Activate
	public CheckCardinality(@Reference AppManager appManager, ComponentContext componentContext) {
		super(componentContext);
		this.appManager = appManager;
	}

	@Override
	public void setProperties(Map<String, ?> properties) {
		this.openemsApp = (OpenemsApp) properties.get("openemsApp");
	}

	@Override
	public boolean check() {
		this.errorMessage = null;
		if (this.appManager == null) {
			this.errorMessage = "App Manager not available!";
			return false;
		}
		if (!(this.appManager instanceof AppManagerImpl)) {
			this.errorMessage = "Wrong AppManager active!";
			return false;
		}
		var appManagerImpl = (AppManagerImpl) this.appManager;
		var instantiatedApps = appManagerImpl.getInstantiatedApps();

		switch (this.openemsApp.getCardinality()) {
		case SINGLE:
			if (instantiatedApps.stream().anyMatch(t -> t.appId.equals(this.openemsApp.getAppId()))) {
				// only create one instance of this app
				this.errorMessage = "An instance of the app[" + this.openemsApp.getAppId() + "] is already created!";
			}
			break;
		case SINGLE_IN_CATEGORY:
			var matchedCategorie = this.getMatchingCategorie(appManagerImpl, instantiatedApps);
			if (matchedCategorie != null) {
				// only create one instance with the same category of this app
				this.errorMessage = "An instance of an app with the same category[" + matchedCategorie.name()
						+ "] is already created!";
			}
			break;
		case MULTIPLE:
			// any number of this app can be instantiated
			break;
		default:
			this.errorMessage = "Usage '" + this.openemsApp.getCardinality().name() + "' is not implemented.";
		}

		return this.errorMessage == null;
	}

	private OpenemsAppCategory getMatchingCategorie(AppManagerImpl appManager,
			List<OpenemsAppInstance> instantiatedApps) {
		for (var openemsAppInstance : instantiatedApps) {
			try {
				var app = appManager.findAppById(openemsAppInstance.appId);
				if (app.getCardinality() != OpenemsAppCardinality.SINGLE_IN_CATEGORY) {
					continue;
				}
				for (var cat : app.getCategorys()) {
					for (var catOther : this.openemsApp.getCategorys()) {
						if (cat == catOther) {
							return cat;
						}
					}
				}
			} catch (NoSuchElementException e) {
				// if app instance is reworked there may be no app for the instance
				continue;
			}
		}
		return null;
	}

	@Override
	public String getErrorMessage(Language language) {
		return this.errorMessage;
	}

}
