package io.openems.edge.ess.saxpower.ess;

import io.openems.common.channel.AccessMode;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.type.Phase;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import org.junit.Test;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SaxPowerImplTest {

    @Test
    public void test() throws Exception {
        new ComponentTest(new SaxPowerImpl())
                .addReference("cm", new DummyConfigurationAdmin())
                .addReference("setModbus", new DummyModbusBridge("modbus0"))
                .activate(MyConfig.create()
                        .setId("ess0")
                        .setModbusId("modbus0")
                        .setModbusUnitId(64)
                        .setCapacity(7000)
                        .build()
                )
                .next(new TestCase()
                        .output(SymmetricEss.ChannelId.CAPACITY, 7000)
                )
                .deactivate();
    }

    @Test
    public void testLimits() throws Exception {
        new ComponentTest(new SaxPowerImpl())
                .addReference("cm", new DummyConfigurationAdmin())
                .addReference("setModbus", new DummyModbusBridge("modbus0"))
                .activate(MyConfig.create()
                        .setId("ess0")
                        .setModbusId("modbus0")
                        .setModbusUnitId(64)
                        .setCapacity(7000)
                        .setMaxDischargePower(4600)
                        .setMaxChargePower(1400)
                        .setMinSoc(10)
                        .build()
                )
                .next(new TestCase()
                        .input(SymmetricEss.ChannelId.SOC, 80)
                        .output(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, -1400)
                        .output(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, 4600)
                )
                .next(new TestCase()
                        .input(SymmetricEss.ChannelId.SOC, 100)
                        .output(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, 0)
                        .output(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, 4600)
                )
                .next(new TestCase()
                        .input(SymmetricEss.ChannelId.SOC, 10)
                        .output(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, -1400)
                        .output(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, 0)
                )
                .next(new TestCase()
                        .input(SymmetricEss.ChannelId.SOC, null)
                        .output(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, null)
                        .output(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, null)
                )
                .deactivate();
    }

    @Test
    public void testActivePowerChannelId() {
        assertEquals(
                AsymmetricEss.ChannelId.ACTIVE_POWER_L1,
                SaxPowerImpl.activePowerChannelId(Phase.SinglePhase.L1)
        );

        assertEquals(
                AsymmetricEss.ChannelId.ACTIVE_POWER_L2,
                SaxPowerImpl.activePowerChannelId(Phase.SinglePhase.L2)
        );

        assertEquals(
                AsymmetricEss.ChannelId.ACTIVE_POWER_L3,
                SaxPowerImpl.activePowerChannelId(Phase.SinglePhase.L3)
        );
    }

    @Test
    public void testDefineModbusProtocol() throws Exception {
        var sut = new SaxPowerImpl();
        new ComponentTest(sut)
                .addReference("cm", new DummyConfigurationAdmin())
                .addReference("setModbus", new DummyModbusBridge("modbus0"))
                .activate(MyConfig.create()
                        .setId("ess0")
                        .setModbusId("modbus0")
                        .setModbusUnitId(64)
                        .setPhase(Phase.SinglePhase.L1)
                        .setCapacity(7000)
                        .setMaxChargePower(1400)
                        .setMaxDischargePower(4600)
                        .setMinSoc(10)
                        .build()
                );

        ModbusProtocol protocol = sut.defineModbusProtocol();

        List<Task> tasks = protocol.getTaskManager().getTasks();

        assertEquals(2, tasks.size());

        FC3ReadRegistersTask readTask = tasks.stream()
                .filter(t -> t instanceof FC3ReadRegistersTask)
                .map(t -> (FC3ReadRegistersTask) t)
                .findFirst()
                .orElseThrow();

        assertEquals(45, readTask.getStartAddress());
        assertEquals(Priority.HIGH, readTask.getPriority());

        FC16WriteRegistersTask writeTask = tasks.stream()
                .filter(t -> t instanceof FC16WriteRegistersTask)
                .map(t -> (FC16WriteRegistersTask) t)
                .findFirst()
                .orElseThrow();

        assertEquals(41, writeTask.getStartAddress());
    }

    @Test
    public void testApplyPower() throws Exception {
        var sut = new SaxPowerImpl();
        new ComponentTest(sut)
                .addReference("cm", new DummyConfigurationAdmin())
                .addReference("setModbus", new DummyModbusBridge("modbus0"))
                .activate(MyConfig.create()
                        .setId("ess0")
                        .setModbusId("modbus0")
                        .setModbusUnitId(64)
                        .setCapacity(7000)
                        .build()
                );

        var lastWriteField = SaxPowerImpl.class.getDeclaredField("lastWrite");
        lastWriteField.setAccessible(true);

        sut.applyPower(1000, 0);
        assertEquals(
                Integer.valueOf(1000 + 16384),
                sut.getActivePowerSetPointChannel().getNextWriteValue().orElse(null)
        );

        lastWriteField.set(sut, Instant.now());
        sut.applyPower(2000, 0);
        assertEquals(
                Integer.valueOf(1000 + 16384),
                sut.getActivePowerSetPointChannel().getNextWriteValue().orElse(null)
        );

        lastWriteField.set(sut, Instant.now().minusSeconds(6));
        sut.applyPower(2000, 0);
        assertEquals(
                Integer.valueOf(2000 + 16384),
                sut.getActivePowerSetPointChannel().getNextWriteValue().orElse(null)
        );

        lastWriteField.set(sut, Instant.now().minusSeconds(6));
        sut.applyPower(0xFFFF - 16384, 0);
        assertEquals(
                Integer.valueOf(0xFFFF),
                sut.getActivePowerSetPointChannel().getNextWriteValue().orElse(null)
        );

        lastWriteField.set(sut, Instant.now().minusSeconds(6));
        sut.applyPower(-16384, 0);
        assertEquals(
                Integer.valueOf(0),
                sut.getActivePowerSetPointChannel().getNextWriteValue().orElse(null)
        );

        sut.deactivate();
    }

    @Test
    public void testDebugLog() throws Exception {
        var sut = new SaxPowerImpl();
        new ComponentTest(sut)
                .addReference("cm", new DummyConfigurationAdmin())
                .addReference("setModbus", new DummyModbusBridge("modbus0"))
                .activate(MyConfig.create()
                        .setId("ess0")
                        .setModbusId("modbus0")
                        .setModbusUnitId(64)
                        .setCapacity(7000)
                        .build()
                )

                .next(new TestCase()
                        .input(SymmetricEss.ChannelId.SOC, 80)
                        .input(SymmetricEss.ChannelId.ACTIVE_POWER, 1500)
                );

        String log = sut.debugLog();
        assertEquals("SoC:80 %|L:1500 W", log);

        sut.deactivate();
    }

    @Test
    public void testGetModbusSlaveTable() throws Exception {
        var sut = new SaxPowerImpl();
        new ComponentTest(sut)
                .addReference("cm", new DummyConfigurationAdmin())
                .addReference("setModbus", new DummyModbusBridge("modbus0"))
                .activate(MyConfig.create()
                        .setId("ess0")
                        .setModbusId("modbus0")
                        .setModbusUnitId(64)
                        .setCapacity(7000)
                        .build()
                );

        assertNotNull(sut.getModbusSlaveTable(AccessMode.READ_ONLY));
        assertNotNull(sut.getModbusSlaveTable(AccessMode.READ_WRITE));
        assertNotNull(sut.getModbusSlaveTable(AccessMode.WRITE_ONLY));

        sut.deactivate();
    }
}
