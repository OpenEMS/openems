package io.openems.edge.core.appmanager;

import java.util.ArrayList;
import java.util.List;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.validator.Checkable;
import io.openems.edge.core.appmanager.validator.Validator;
import io.openems.edge.core.appmanager.validator.ValidatorConfig.CheckableConfig;

public class DummyValidator implements Validator {

	private List<Checkable> checkables;

	@Override
	public List<String> getErrorMessages(List<CheckableConfig> checkableConfigs, Language language,
			boolean returnImmediate) {
		var errors = new ArrayList<String>();
		for (var check : checkableConfigs) {
			var checkable = this.findCheckableByName(check.checkableComponentName());
			checkable.setProperties(check.properties());
			if (checkable.check() == check.invertResult()) {
				errors.add(check.invertResult() ? checkable.getInvertedErrorMessage(language)
						: checkable.getErrorMessage(language));
				return errors;
			}

		}
		return errors;
	}

	private Checkable findCheckableByName(String name) {
		return this.checkables.stream() //
				.filter(c -> c.getComponentName().equals(name)) //
				.findAny().get();
	}

	public void setCheckables(List<Checkable> checkables) {
		this.checkables = checkables;
	}

	public List<Checkable> getCheckables() {
		return this.checkables;
	}

}
