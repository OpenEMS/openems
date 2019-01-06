package io.openems.edge.core.componentmanager;

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;

@Component( //
		name = "Core.ComponentManager", //
		immediate = true, //
		property = { //
				"id=" + OpenemsConstants.COMPONENT_MANAGER_ID, //
				"enabled=true" //
		})
public class ComponentManagerImpl extends AbstractOpenemsComponent
		implements ComponentManager, OpenemsComponent, JsonApi {

	private final OsgiValidateWorker osgiValidateWorker;

	private BundleContext bundleContext;

	@Reference
	private MetaTypeService metaTypeService;

	@Reference
	protected ConfigurationAdmin cm;

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		CONFIG_NOT_ACTIVATED(new Doc() //
				.text("A configured OpenEMS Component was not activated") //
				.type(OpenemsType.BOOLEAN) //
				.level(Level.WARNING));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(!(service.factoryPid=Core.ComponentManager)))")
	protected volatile List<OpenemsComponent> components = new CopyOnWriteArrayList<>();

	public ComponentManagerImpl() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));

		this.osgiValidateWorker = new OsgiValidateWorker(this);
	}

	@Activate
	void activate(ComponentContext componentContext, BundleContext bundleContext, Map<String, Object> properties) throws OpenemsException {
		super.activate(componentContext, properties, OpenemsConstants.COMPONENT_MANAGER_ID,
				true);

		this.bundleContext = bundleContext;

		// Start OSGi Validate Worker
		this.osgiValidateWorker.activate(this.id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();

		// Stop OSGi Validate Worker
		this.osgiValidateWorker.deactivate();
	}

	@SuppressWarnings("unchecked")
	public <T extends OpenemsComponent> T getComponent(String componentId) {
		List<OpenemsComponent> components = this.components;
		for (OpenemsComponent component : components) {
			if (component.id().equals(componentId)) {
				return (T) component;
			}
		}
		throw new IllegalArgumentException("Component [" + componentId + "] is not available.");
	}

	@Override
	public <T extends Channel<?>> T getChannel(ChannelAddress channelAddress) throws IllegalArgumentException {
		OpenemsComponent component = this.getComponent(channelAddress.getComponentId());
		return component.channel(channelAddress.getChannelId());
	}

	protected StateChannel configNotActivatedChannel() {
		return this.channel(ChannelId.CONFIG_NOT_ACTIVATED);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}

}
