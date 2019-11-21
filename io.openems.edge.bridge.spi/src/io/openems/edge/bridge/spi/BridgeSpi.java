package io.openems.edge.bridge.spi;

import io.openems.edge.bridge.spi.task.SpiTask;
import io.openems.edge.temperatureBoard.api.Adc;
import org.osgi.service.cm.ConfigurationException;

import java.util.List;


public interface BridgeSpi {
    void addAdc(Adc adc);

    List<Adc> getAdcList();

    void removeAdc(Adc adc);

    void addSpiTask(String id, SpiTask spiTask) throws ConfigurationException;

    void removeSpiTask(String id);
}
