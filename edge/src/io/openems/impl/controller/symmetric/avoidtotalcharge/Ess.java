package io.openems.impl.controller.symmetric.avoidtotalcharge;

/**
 * Created by maxo2 on 29.08.2017.
 */

import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.core.utilities.hysteresis.Hysteresis;

@IsThingMap(type = SymmetricEssNature.class)
public class Ess extends ThingMap {

    public final ReadChannel<Integer> minSoc;
    public final WriteChannel<Long> setActivePower;
    public final ReadChannel<Long> soc;
    public final ReadChannel<Long> systemState;
    public int maxPowerPercent = 100;
    public final ReadChannel<Long> allowedDischarge;
    public final ReadChannel<Long> allowedCharge;
    public final ReadChannel<Integer> chargeSoc;
    public Hysteresis socMinHysteresis;
    public State
            currentState = State.NORMAL;

    public enum State {
        NORMAL, MINSOC, CHARGESOC, FULL;
    }

    public Ess(SymmetricEssNature ess) {
        super(ess);
        setActivePower = ess.setActivePower().required();
        systemState = ess.systemState().required();
        soc = ess.soc().required();
        minSoc = ess.minSoc().required();
        allowedDischarge = ess.allowedDischarge().required();
        allowedCharge = ess.allowedCharge().required();
        chargeSoc = ess.chargeSoc().required();
        ChannelChangeListener hysteresisCreator = new ChannelChangeListener() {

            @Override
            public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
                if (minSoc.valueOptional().isPresent() && chargeSoc.valueOptional().isPresent()) {
                    socMinHysteresis = new Hysteresis(chargeSoc.valueOptional().get(), minSoc.valueOptional().get());
                } else if (minSoc.valueOptional().isPresent()) {
                    socMinHysteresis = new Hysteresis(minSoc.valueOptional().get() - 3, minSoc.valueOptional().get());
                }
            }
        };
        minSoc.addChangeListener(hysteresisCreator);
        chargeSoc.addChangeListener(hysteresisCreator);

        hysteresisCreator.channelChanged(null, null, null);
    }
}