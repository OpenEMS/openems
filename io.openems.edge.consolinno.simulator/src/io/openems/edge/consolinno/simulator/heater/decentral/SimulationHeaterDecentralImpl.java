package io.openems.edge.consolinno.simulator.heater.decentral;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.consolinno.simulator.heater.api.SimulationHeaterDecentral;
import io.openems.edge.controller.api.Controller;
import org.joda.time.DateTime;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Simulation.Heater.Decentral",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true
)
public class SimulationHeaterDecentralImpl extends AbstractOpenemsComponent implements OpenemsComponent, Controller, SimulationHeaterDecentral {

    Map<Integer, ChannelPair> positionChannelPairMap = new HashMap<>();
    Map<Integer, DateTime> positionWorkTimePairMap = new HashMap<>();
    Map<Integer, Boolean> positionCoolDown = new HashMap<>();
    Map<Integer, String> positionToRelayControl = new HashMap<>();
    Random random = new Random();
    private static final int WORK_AT_LEAST_THIS_IN_MINUTES = 5;
    private static final int MAX_CHANNEL_PAIRS = 6;

    public SimulationHeaterDecentralImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values(),
                SimulationHeaterDecentral.ChannelId.values());
    }

    @Reference
    ComponentManager cpm;

    @Activate
    public void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        List<String> channelList = Arrays.asList(config.channelToWrite());
        for (int x = 1; x <= MAX_CHANNEL_PAIRS; x++) {
            this.positionChannelPairMap.put(x, new ChannelPair(this.getNeedHeatByNumber(x), this.getNeedHeatEnableByNumber(x)));
            this.positionCoolDown.put(x, false);
            //Get Channel to Control
            if (channelList.size() >= x) {
                positionToRelayControl.put(x, channelList.get(x - 1));
            }
        }

    }

    @Deactivate
    public void deactivate() {
        this.channels().parallelStream().forEach(channel -> channel.setNextValue(false));
        super.deactivate();
    }


    @Override
    public void run() throws OpenemsError.OpenemsNamedException {
        //add Requests and add To WorkingList if Allowed to else remove from worklist
        this.positionChannelPairMap.forEach((key, value) -> {
            value.updateValues();
            boolean valueInWorkMap = this.positionWorkTimePairMap.containsKey(key);
            boolean valueEnabled = value.isEnabledSignal();
            boolean hasChannelToWrite = this.positionToRelayControl.containsKey(key);
            //Request?
            if (value.hasRequest()) {
                //Enabled by CommunicationMaster
                if (valueEnabled) {
                    //only if not already working
                    if (valueInWorkMap == false) {
                        this.positionWorkTimePairMap.put(key, new DateTime());
                    }
                    if (hasChannelToWrite) {
                        writeToChannel(this.positionToRelayControl.get(key), true);
                    }
                } else {
                    //Remove bc it's not allowed to Work
                    this.positionWorkTimePairMap.remove(key);
                    if (hasChannelToWrite) {
                        writeToChannel(this.positionToRelayControl.get(key), false);
                    }
                }
            } else {
                if (valueInWorkMap) {
                    this.positionWorkTimePairMap.remove(key);
                }
                if (random.nextBoolean() && random.nextInt(100) < 70 && this.positionCoolDown.get(key) == false) {
                    this.getNeedHeatByNumber(key).setNextValue(true);
                }
            }
        });
        //Check for WorkTime --> Worked long enough AND is satisfied ?
        if (this.positionWorkTimePairMap.size() > 0) {
            List<Integer> keysToRemoveFromWorkList = new ArrayList<>();
            //check if Working position && hasRequest && worked at Least x Time
            this.positionWorkTimePairMap.forEach((key, value) -> {
                DateTime now = new DateTime();
                DateTime then = new DateTime(value);
                //worked long enough --> random if it is satisfied;
                if (now.isAfter(then.plusMinutes(WORK_AT_LEAST_THIS_IN_MINUTES))) {
                    if (random.nextBoolean() && random.nextInt(100) <= 90) {
                        keysToRemoveFromWorkList.add(key);
                    }
                }
            });
            if (keysToRemoveFromWorkList.size() > 0) {
                keysToRemoveFromWorkList.forEach(key -> {
                    this.positionCoolDown.replace(key, true);
                    this.getNeedHeatByNumber(key).setNextValue(false);
                });
            }
        }
        //RemoveCooldown
        List<Integer> unsetCooldown = new ArrayList<>();
        this.positionCoolDown.forEach((key, value) -> {
            if (value && random.nextInt(100) <= 30) {
                unsetCooldown.add(key);
            }
        });
        unsetCooldown.forEach(key -> {
            this.positionCoolDown.replace(key, false);
        });

    }

    private void writeToChannel(String channel, boolean onOrOff) {
        try {
            ChannelAddress address = ChannelAddress.fromString(channel);
            Channel<?> channelToGet = this.cpm.getChannel(address);
            if (channelToGet instanceof WriteChannel<?> && channelToGet.getType().equals(OpenemsType.BOOLEAN)) {
                WriteChannel<Boolean> writeChannel = this.cpm.getChannel(address);
                writeChannel.setNextWriteValue(onOrOff);
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
        }
    }
}
