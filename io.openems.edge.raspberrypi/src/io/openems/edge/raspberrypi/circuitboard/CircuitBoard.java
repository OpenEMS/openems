package io.openems.edge.raspberrypi.circuitboard;

import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.raspberrypi.circuitboard.api.adc.Adc;
import io.openems.edge.raspberrypi.circuitboard.api.boardtypes.TemperatureBoard;
import io.openems.edge.raspberrypi.spi.SpiInitial;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Designate(ocd = Config.class, factory = true)
@Component(name = "CircuitBoard", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)

public class CircuitBoard extends AbstractOpenemsComponent implements ConsolinnoBoards, OpenemsComponent {
    @Reference
    private SpiInitial spiInitial;
    private String circuitBoardId;
    private String type;
    private String versionId;
    private short maxCapacity;
    private List<Adc> adcList = new ArrayList<>();

    public CircuitBoard() {
        super(OpenemsComponent.ChannelId.values(),
                ConsolinnoBoards.ChannelId.values(),
                ChannelId.values());
    }


    @Activate
    public void activate(ComponentContext context, Config config) throws ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.circuitBoardId = config.id();
        this.versionId = config.versionNumber();
        this.type = config.boardType();
        String adcFrequency = config.adcFrequency();
        String dipSwitches = config.dipSwitches();
        List<String> frequency = new ArrayList<>();
        List<Character> dipSwitch = new ArrayList<>();

        if (adcFrequency.contains(";")) {
            String[] parts = adcFrequency.split(";");
            frequency.addAll(Arrays.asList(parts));
        } else {
            frequency.add(adcFrequency);
        }
        for (Character dipSwitchUse : dipSwitches.toCharArray()) {
            dipSwitch.add(dipSwitchUse);
        }
        instantiateCorrectBoard(this.type, this.versionId, frequency, dipSwitch);
        spiInitial.addCircuitBoards(this);
    }

    private void instantiateCorrectBoard(String boardType, String versionId, List<String> frequency, List<Character> dipSwitch) throws ConfigurationException {
        boolean wasCreated = false;
        switch (boardType) {
            case "Temperature":
                createTemperatureBoard(versionId, frequency, dipSwitch);
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
        spiInitial.removeCircuitBoard(this);
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

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        ;

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }
}
//Just an example function to explain Streams to myself
//private void temp() {
// Optional<String> any = this.getSensors().stream().filter(sensor -> sensor.isOn()).map(sensor -> sensor.getName()).findAny();
//}