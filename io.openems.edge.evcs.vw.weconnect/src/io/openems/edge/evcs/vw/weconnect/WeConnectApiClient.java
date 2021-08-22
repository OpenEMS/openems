package io.openems.edge.evcs.vw.weconnect;

import io.openems.edge.evcs.api.Status;

public class WeConnectApiClient {
	
	private ConnectionId connection;
	
	public WeConnectApiClient(Config config) {
		this.connection = new ConnectionId(config.user(),config.password());
	}

	public void login() {
		connection.login();
	}

	public void logout() {
		this.connection.logout();
	}

	public void update() {
		if(!connection.isLoginRunning()) {
			connection.updateVehicles();
		}
	}

	public int getCurrentSoC() {
		return this.connection.getVehicleSoc();
	}

	public Status getCurrentStatus() {
		if(this.connection.isVehicleCharging()) {
			return Status.CHARGING;
		}
		
		if(this.connection.isVehicleConnected()) {
			return Status.READY_FOR_CHARGING;
		}

		return Status.NOT_READY_FOR_CHARGING;
	}

	public void sentStartCharging() {
		this.connection.startCharingConnectedVehicle();
	}

	public void sentStopCharging() {
		this.connection.stopCharingConnectedVehicle();
	}
}
