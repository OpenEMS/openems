package io.openems.edge.wago;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;

import io.openems.edge.bridge.modbus.api.task.FC1ReadCoilsTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.bridge.modbus.api.task.ReadTask;
import io.openems.edge.bridge.modbus.api.task.WriteTask;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class WagoTest {

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
		var sut = new Wago();
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
		var doc = Wago.parseXmlToDocument(dummyXml);

		var modules = sut.parseXml(doc);
		assertEquals(Fieldbus5xxDO.class, modules.get(0).getClass());
		assertEquals(Fieldbus523RO1Ch.class, modules.get(1).getClass());

		sut.createProtocolFromModules(modules);

		{
			var readTasks = sut.protocol.getReadTasksManager().getTasks();
			ReadTask t;
			{
				t = readTasks.get(0);
				assertEquals(FC1ReadCoilsTask.class, t.getClass());
				assertEquals(0, t.getStartAddress());
				assertEquals(2, t.getLength());
			}
			{
				t = readTasks.get(1);
				assertEquals(FC1ReadCoilsTask.class, t.getClass());
				assertEquals(512, t.getStartAddress());
				assertEquals(4, t.getLength());
			}
		}

		{
			var writeTasks = sut.protocol.getWriteTasksManager().getTasks();
			System.out.println(writeTasks);
			WriteTask t;
			{
				t = writeTasks.get(0);
				assertEquals(FC5WriteCoilTask.class, t.getClass());
				assertEquals(512, t.getStartAddress());
			}
			{
				t = writeTasks.get(1);
				assertEquals(FC5WriteCoilTask.class, t.getClass());
				assertEquals(513, t.getStartAddress());
			}
			{
				t = writeTasks.get(2);
				assertEquals(FC5WriteCoilTask.class, t.getClass());
				assertEquals(514, t.getStartAddress());
			}
			{
				t = writeTasks.get(3);
				assertEquals(FC5WriteCoilTask.class, t.getClass());
				assertEquals(515, t.getStartAddress());
			}
		}

	}
}
