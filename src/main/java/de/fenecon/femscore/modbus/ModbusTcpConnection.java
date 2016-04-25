package de.fenecon.femscore.modbus;

import java.net.InetAddress;

import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.io.ModbusTransaction;
import net.wimpi.modbus.net.TCPMasterConnection;

public class ModbusTcpConnection extends ModbusConnection {
	private final Integer port;
	private final InetAddress ip;
	private TCPMasterConnection con = null;

	public ModbusTcpConnection(InetAddress ip, int cycle) {
		super(cycle);
		this.ip = ip;
		this.port = 502;
	}

	@Override
	protected ModbusTransaction getTransaction() throws Exception {
		if (con == null) {
			con = new TCPMasterConnection(this.ip);
			con.setPort(this.port);
		}
		if (!con.isConnected()) {
			con.connect();
		}
		ModbusTCPTransaction trans = new ModbusTCPTransaction(con);
		return trans;
	}

	@Override
	public void close() {
		if (con == null) {
			if (con.isConnected()) {
				con.close();
			}
		}
	}

	@Override
	public String toString() {
		return "ModbusTcpConnection [port=" + port + ", ip=" + ip + "]";
	}
}
