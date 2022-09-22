package io.openems.edge.bridge.sml;

import org.osgi.framework.Constants;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.bridge.sml.api.AbstractOpenemsSmlComponent;
import io.openems.edge.bridge.sml.api.BridgeSml;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.test.DummyComponentContext;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.DummyConfigurationAdmin.DummyConfiguration;

public abstract class DummySmlComponent extends AbstractOpenemsSmlComponent implements BridgeSml {
	public DummySmlComponent(String id, BridgeSmlSerialImpl bridge, int unitId,io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) throws OpenemsException {
		super(//
//				OpenemsComponent.ChannelId.values(), //
//				BridgeSml.ChannelId.values(), //
				firstInitialChannelIds, //
				furtherInitialChannelIds//
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}

		var context = new DummyComponentContext();
		context.addProperty(Constants.SERVICE_PID, Constants.SERVICE_PID);
		var cm = new DummyConfigurationAdmin();
		var dummyConfiguration = new DummyConfiguration();
		dummyConfiguration.addProperty("Sml.target",
				ConfigUtils.generateReferenceTargetFilter(Constants.SERVICE_PID, bridge.id()));
		cm.addConfiguration(Constants.SERVICE_PID, dummyConfiguration);
		super.activate(context, "", id, true, cm, bridge.id());
	}
}
