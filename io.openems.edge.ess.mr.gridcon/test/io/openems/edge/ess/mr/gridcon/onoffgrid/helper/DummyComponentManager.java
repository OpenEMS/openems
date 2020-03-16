package io.openems.edge.ess.mr.gridcon.onoffgrid.helper;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.ComponentContext;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.session.User;
import io.openems.common.types.EdgeConfig;
import io.openems.edge.battery.soltaro.SoltaroBattery;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.mr.gridcon.EssGridcon;
import io.openems.edge.ess.mr.gridcon.GridconPCS;

public class DummyComponentManager implements ComponentManager {
	
	private SoltaroBattery bms1 = createBms();
	private SoltaroBattery bms2 = createBms();
	private SoltaroBattery bms3 = createBms();
	private GridconPCS gridconPcs = createGridconPcs();
	private EssGridcon ess = createEss();

	private GridconPCS createGridconPcs() {
		return new DummyGridcon();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends OpenemsComponent> T getComponent(String componentId) throws OpenemsNamedException {
		if (Creator.ESS_ID.equals(componentId)) {
			return (T) ess;
		}
		if (Creator.BMS_A_ID.equals(componentId)) {
			return (T) bms1;
		}
		if (Creator.BMS_B_ID.equals(componentId)) {
			return (T) bms2;
		}
		if (Creator.BMS_C_ID.equals(componentId)) {
			return (T) bms3;
		}
		if (Creator.GRIDCON_ID.equals(componentId)) {
			return (T) gridconPcs;
		}
		return null;
	}

	private SoltaroBattery createBms() {
		return new DummyBattery();
	}

	private EssGridcon createEss() {

		return new DummyEss(
				new ChannelId[] {}
		);
	}


	@Override
	public CompletableFuture<JsonrpcResponseSuccess> handleJsonrpcRequest(User user, JsonrpcRequest request)
			throws OpenemsNamedException {
		return null;
	}

	@Override
	public boolean isEnabled() {
		return false;
	}

	@Override
	public String id() {
		return null;
	}

	@Override
	public ComponentContext getComponentContext() {
		return null;
	}

	@Override
	public Collection<Channel<?>> channels() {
		return null;
	}

	@Override
	public String alias() {
		return null;
	}

	@Override
	public Channel<?> _channel(String channelName) {
		return null;
	}

	@Override
	public List<OpenemsComponent> getEnabledComponents() {
		return null;
	}

	@Override
	public EdgeConfig getEdgeConfig() {
		return null;
	}

	@Override
	public List<OpenemsComponent> getAllComponents() {
		return null;
	}

	public void destroyEss() {
		this.ess = null;
	}

	public void initEss() {
		this.ess = createEss();
	}

//	public void destroyBms() {
//		this.bms = null;
//	}
//
//	public void initBms() {
//		this.bms = createBms();
//	}
}
