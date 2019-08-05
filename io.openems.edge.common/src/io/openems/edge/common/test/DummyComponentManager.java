package io.openems.edge.common.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.osgi.service.component.ComponentContext;

import io.openems.common.OpenemsConstants;
import io.openems.common.types.EdgeConfig;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import org.slf4j.Logger;

/**
 * Simulates a ComponentManager for the OpenEMS Component test framework.
 */
public class DummyComponentManager implements ComponentManager {

    private final List<OpenemsComponent> components = new ArrayList<>();

    public DummyComponentManager() {
    }

    @Override
    public List<OpenemsComponent> getComponents() {
        return Collections.unmodifiableList(this.components);
    }

    /**
     * Specific for this Dummy implementation.
     *
     * @param component
     */
    public void addComponent(OpenemsComponent component) {
        if (component != this) {
            this.components.add(component);
        }
    }

    @Override
    public EdgeConfig getEdgeConfig() {
        return new EdgeConfig();
	}

	@Override
	public String id() {
		return OpenemsConstants.COMPONENT_MANAGER_ID;
	}

	@Override
	public String alias() {
		return OpenemsConstants.COMPONENT_MANAGER_ID;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public ComponentContext getComponentContext() {
		return null;
	}

	@Override
	public Channel<?> _channel(String channelName) {
		return null;
	}

	@Override
	public Collection<Channel<?>> channels() {
		return new ArrayList<>();
    }

    @Override
    public boolean isComponentActivated(String componentId, String pid) {
        return false;
        // TODO take care of this
    }

    @Override
    public void logWarn(Logger log, String s) {
    // TODO take care of this
    }

    @Override
    public void logError(Logger log, String message) {
    // TODO take care of this
    }

    @Override
    public List<String> checkForNotActivatedComponents() {
        return null;
    }

}