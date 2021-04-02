package io.openems.edge.controller.api.opcua.server;

import java.util.List;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegate;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;

public class MyNamespace extends ManagedNamespaceWithLifecycle {

	public static final String NAMESPACE_URI = "urn:openems:edge:";

	private final OpcuaServerApiControllerImpl parent;

	protected MyNamespace(OpcuaServerApiControllerImpl parent, OpcUaServer server) {
		super(server, NAMESPACE_URI);
		this.parent = parent;

		getLifecycleManager().addStartupTask(this::createAndAddNodes);
	}

	private void createAndAddNodes() {
		this.createChannelNodes();
	}

	private static final String CHANNELS = "Channels";

	private static class OpenemsChannelDelegate implements AttributeDelegate {

		private final OpcuaServerApiControllerImpl parent;
		private final ChannelAddress channelAddress;

		private OpenemsChannelDelegate(OpcuaServerApiControllerImpl parent, ChannelAddress channelAddress) {
			this.parent = parent;
			this.channelAddress = channelAddress;
		}

		@Override
		public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
			Object value;
			try {
				value = parent.componentManager.getChannel(channelAddress).value().getOrError();
			} catch (IllegalArgumentException | OpenemsNamedException e) {
				throw new UaException(e);
			}
			return new DataValue(new Variant(value));
		}
	}

	private void createChannelNodes() {
		UaFolderNode channelsFolder = new UaFolderNode(getNodeContext(), //
				newNodeId(CHANNELS), //
				newQualifiedName(CHANNELS), //
				LocalizedText.english(CHANNELS));
		getNodeManager().addNode(channelsFolder);

		// Make sure our new folder shows up under the server's Objects folder.
		channelsFolder.addReference(new Reference(//
				channelsFolder.getNodeId(), //
				Identifiers.Organizes, //
				Identifiers.ObjectsFolder.expanded(), //
				false));

		for (OpenemsComponent component : this.parent.componentManager.getEnabledComponents()) {
			// TODO refresh nodes on add/remove of Component/Channel

			final String componentPath = CHANNELS + "/" + component.id();
			UaFolderNode componentFolder = new UaFolderNode(getNodeContext(), //
					newNodeId(componentPath), //
					newQualifiedName(component.id()), //
					LocalizedText.english(component.id()));

			getNodeManager().addNode(componentFolder);
			channelsFolder.addOrganizes(componentFolder);

			for (Channel<?> channel : component.channels()) {
				UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
						.setNodeId(newNodeId(componentPath + "/" + channel.address().toString())) //
						// .setAccessLevel(AccessLevel.READ_ONLY) // causes Resolve error!
						// .setUserAccessLevel(AccessLevel.READ_ONLY) // causes Resolve error!
						.setBrowseName(newQualifiedName(channel.channelId().id())) //
						.setDisplayName(LocalizedText.english(channel.channelId().id())) //
						// .setDataType(Identifiers.Enumeration) //
						.setTypeDefinition(Identifiers.BaseDataVariableType) //
						.buildAndAdd();
				node.setAttributeDelegate(new OpenemsChannelDelegate(this.parent, channel.address()));
				getNodeManager().addNode(node);
				componentFolder.addOrganizes(node);
			}
		}

	}

	@Override
	public void onDataItemsCreated(List<DataItem> dataItems) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onDataItemsModified(List<DataItem> dataItems) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onDataItemsDeleted(List<DataItem> dataItems) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
		// TODO Auto-generated method stub
	}

}
