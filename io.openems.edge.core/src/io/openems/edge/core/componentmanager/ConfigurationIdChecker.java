package io.openems.edge.core.componentmanager;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.FrameworkEvent;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This component checks all existing configurations for missing 'id' properties
 * and sets them to their default value as defined in the MetaType information.
 *
 * <p>
 * Components with no id persisted in their configuration cannot be searched
 * with {@link ConfigurationAdmin#listConfigurations(String)} with a filter on
 * the 'id' property.
 * </p>
 */
@Component(immediate = true)
public class ConfigurationIdChecker {

	private final Logger log = LoggerFactory.getLogger(ConfigurationIdChecker.class);

	private final ComponentContext context;
	private final ConfigurationAdmin configurationAdmin;
	private final MetaTypeService metaTypeService;

	private final AtomicBoolean finished = new AtomicBoolean(false);

	@Activate
	public ConfigurationIdChecker(//
			ComponentContext context, //
			@Reference ConfigurationAdmin configurationAdmin, //
			@Reference MetaTypeService metaTypeService //
	) {
		this.context = context;
		this.configurationAdmin = configurationAdmin;
		this.metaTypeService = metaTypeService;

		this.context.getBundleContext().addFrameworkListener(this::frameworkEvent);
	}

	@Deactivate
	private void deactivate() {
		this.context.getBundleContext().removeFrameworkListener(this::frameworkEvent);
	}

	private void frameworkEvent(FrameworkEvent event) {
		// listening for first start level change which indicates that the system has
		// started all initial bundles and their configurations are in place
		if (event.getType() != FrameworkEvent.STARTLEVEL_CHANGED) {
			return;
		}

		if (!this.finished.compareAndSet(false, true)) {
			return;
		}

		try {
			this.setMissingComponentIds();
			this.context.getComponentInstance().dispose();
		} catch (Exception e) {
			this.log.error("Unable to resolve configurations", e);
		}
	}

	private void setMissingComponentIds() throws Exception {
		final var configs = this.configurationAdmin.listConfigurations(null);

		for (var config : configs) {
			if (config.getAttributes().contains(Configuration.ConfigurationAttribute.READ_ONLY)) {
				continue;
			}

			final var factoryPid = config.getFactoryPid();
			if (factoryPid == null) {
				continue;
			}

			final var props = config.getProperties();
			final var id = props.get("id");
			if (id != null) {
				continue;
			}

			final var defaultId = this.getDefaultId(factoryPid);
			if (defaultId == null) {
				this.log.debug("Configuration {} has no 'id' property and no default value could be found.",
						factoryPid);
				continue;
			}

			this.log.info("Set missing 'id' property of configuration '{}' to default value '{}'.", factoryPid,
					defaultId);
			final var newProps = config.getProperties();
			newProps.put("id", defaultId);
			config.update(newProps);
		}
	}

	private String getDefaultId(String factoryPid) {
		for (var bundle : this.context.getBundleContext().getBundles()) {
			final var metaTypeInformation = this.metaTypeService.getMetaTypeInformation(bundle);
			if (metaTypeInformation == null) {
				continue;
			}

			if (Arrays.stream(metaTypeInformation.getFactoryPids()).noneMatch(t -> t.equals(factoryPid))) {
				continue;
			}

			final var ocd = metaTypeInformation.getObjectClassDefinition(factoryPid, null);
			for (var attributeDefinition : ocd.getAttributeDefinitions(ObjectClassDefinition.ALL)) {
				if (!attributeDefinition.getID().equals("id")) {
					continue;
				}

				final var defaultValue = attributeDefinition.getDefaultValue();
				if (defaultValue == null || defaultValue.length == 0) {
					return null;
				}
				return defaultValue[0];
			}

			break;
		}
		return null;
	}

}
