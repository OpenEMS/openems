package io.openems.edge.raspberrypi.circuitboard;

import io.openems.edge.raspberrypi.circuitboard.api.adc.Adc;
import io.openems.edge.raspberrypi.circuitboard.api.boardtypes.TemperatureBoard;
import io.openems.edge.raspberrypi.spi.SpiInitial;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Designate(ocd = Config.class, factory = true)
@Component(name = "CircuitBoard", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)

public class CircuitBoard {
    @Reference
    private SpiInitial spiInitial;
    private String circuitBoardId;
    private String type;
    private String versionId;
    private short maxCapacity;
    private List<Adc> adcList = new ArrayList<>();

    @Activate
    public void activate(Config config) throws ConfigurationException {
        this.circuitBoardId = config.boardId();
        this.versionId = config.versionNumber();
        this.type = config.boardType();
        List<String> frequency = new ArrayList<>();
        List<Character> dipSwitch = new ArrayList<>();

        if (config.adcFrequency().contains(";")) {
            Collections.addAll(frequency, config.adcFrequency().split(";"));
        } else {
            frequency.add(config.adcFrequency());
        }
        for (Character dipSwitchUse : config.dipSwitches().toCharArray()) {
            dipSwitch.add(dipSwitchUse);
        }
        instantiateCorrectBoard(config, frequency, dipSwitch);
        spiInitial.getCircuitBoards().add(this);
    }

    private void instantiateCorrectBoard(Config config, List<String> frequency, List<Character> dipSwitch) throws ConfigurationException {
        boolean wasCreated = false;
        switch (config.boardType()) {
            case "Temperature":
                createTemperatureBoard(config.versionNumber(), frequency, dipSwitch);
                wasCreated = true;
                break;
        }
        if (!wasCreated) {
            throw new ConfigurationException("Something went wrong", "Wrong BoardType");
        }
    }

    private void createTemperatureBoard(String versionNumber, List<String> frequency, List<Character> dipSwitch) throws ConfigurationException {
        switch (versionNumber) {
            case "1":
                this.maxCapacity = TemperatureBoard.TEMPERATURE_BOARD_V_1.getMaxSize();
                short counter = 0;
                for (Adc mcpWantToCreate : TemperatureBoard.TEMPERATURE_BOARD_V_1.getMcpContainer()) {
                    createMcp(mcpWantToCreate, frequency.get(counter), dipSwitch.get(counter));
                    counter++;
                }
                break;
        }
    }

    private void createMcp(Adc mcpWantToCreate, String frequency, Character dipSwitch) {
        mcpWantToCreate.initialize(Character.getNumericValue(dipSwitch), Integer.parseInt(frequency), this.circuitBoardId);
        this.adcList.add(mcpWantToCreate);
    }

    public short getMaxCapacity() {
        return maxCapacity;
    }

    @Deactivate
    public void deactivate() {
        for (Adc adc : this.adcList) {
            this.adcList.remove(adc);
        }
    }

    public String getCircuitBoardId() {
        return circuitBoardId;
    }

    public String getType() {
        return type;
    }

    public String getVersionId() {
        return versionId;
    }

    public List<Adc> getAdcList() {
        return adcList;
    }
}
//Just an example function to explain Streams to myself
//private void temp() {
// Optional<String> any = this.getSensors().stream().filter(sensor -> sensor.isOn()).map(sensor -> sensor.getName()).findAny();
//}