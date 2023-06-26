package io.openems.edge.wago;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;

import io.openems.edge.bridge.modbus.api.task.FC1ReadCoilsTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class IoWagoImplTest {

	private static final String IO_ID = "io0";
	private static final String MODBUS_ID = "modbus0";

	/**
	 * This is an example "ea-config.xml" downloaded from a WAGO Fieldbus coupler.
	 */
	private static final String EA_CONFIG = """
			<?xml version="1.0" encoding="ISO-8859-1"?>
			<?xml-stylesheet type="text/xsl" href="../webserv/cplcfg/EA-config.xsl" ?>
			<WAGO>
			 <Module ARTIKELNR="750-5xx" MODULETYPE="DO" CHANNELCOUNT="2" MAP="FB1">
			  <Kanal CHANNELNAME="M001Ch1" CHANNELTYPE="DO">
			1
			  </Kanal>
			  <Kanal CHANNELNAME="M001Ch2" CHANNELTYPE="DO">
			0
			  </Kanal>
			 </Module>
			 <Module ARTIKELNR="750-5xx" MODULETYPE="DO/DIA" CHANNELCOUNT="2" MAP="FB1">
			  <Kanal CHANNELNAME="M002Ch1" CHANNELTYPE="DO">
			0
			  </Kanal>
			  <Kanal CHANNELNAME="M002Ch2" CHANNELTYPE="DO">
			0
			  </Kanal>
			  <Kanal CHANNELNAME="M002Ch3" CHANNELTYPE="DIA">
			0
			  </Kanal>
			  <Kanal CHANNELNAME="M002Ch4" CHANNELTYPE="DIA">
			0
			  </Kanal>
			 </Module>
			</WAGO>
			""";

	@Test
	public void test() throws Exception {
		var sut = new IoWagoImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(IO_ID) //
						.setModbusId(MODBUS_ID) //
						.setUsername("foo") //
						.setPassword("bar") //
						.build()) //
		;

		InputStream dummyXml = new ByteArrayInputStream(EA_CONFIG.getBytes());
		var doc = IoWagoImpl.parseXmlToDocument(dummyXml);

		var modules = sut.parseXml(doc);
		assertEquals(Fieldbus5xxDO.class, modules.get(0).getClass());
		assertEquals(Fieldbus523RO1Ch.class, modules.get(1).getClass());

		sut.createProtocolFromModules(modules);
		var tasks = sut.protocol.getTaskManager().getTasks();

		assertEquals(6, tasks.size());
		assertTrue(tasks.stream() //
				.anyMatch(t -> t instanceof FC1ReadCoilsTask && t.getStartAddress() == 0 && t.getLength() == 2));
		assertTrue(tasks.stream() //
				.anyMatch(t -> t instanceof FC1ReadCoilsTask && t.getStartAddress() == 512 && t.getLength() == 4));
		assertTrue(tasks.stream() //
				.anyMatch(t -> t instanceof FC5WriteCoilTask && t.getStartAddress() == 512 && t.getLength() == 1));
		assertTrue(tasks.stream() //
				.anyMatch(t -> t instanceof FC5WriteCoilTask && t.getStartAddress() == 513 && t.getLength() == 1));
		assertTrue(tasks.stream() //
				.anyMatch(t -> t instanceof FC5WriteCoilTask && t.getStartAddress() == 514 && t.getLength() == 1));
		assertTrue(tasks.stream() //
				.anyMatch(t -> t instanceof FC5WriteCoilTask && t.getStartAddress() == 515 && t.getLength() == 1));
	}
}
