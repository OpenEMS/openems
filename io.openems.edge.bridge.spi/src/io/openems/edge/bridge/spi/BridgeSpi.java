package io.openems.edge.bridge.spi;

import io.openems.edge.bridge.spi.task.SpiTask;
import io.openems.edge.temperature.board.api.Adc;
import org.osgi.service.cm.ConfigurationException;

import java.util.Map;
import java.util.Set;


public interface BridgeSpi {
    void addAdc(Adc adc);

    Set<Adc> getAdcs();

    void removeAdc(Adc adc);

    void addSpiTask(String id, SpiTask spiTask) throws ConfigurationException;

    void removeSpiTask(String id);

    Map<String, SpiTask> getTasks();
}
