package io.openems.edge.relais;

import io.openems.edge.bridgei2c.task.I2cTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.relaisBoard.api.Mcp;
import io.openems.edge.relaisBoard.api.Mcp23008;


public class RelaisActuatorTask extends I2cTask {
    private int position;
    private Channel<Boolean> onOrOff;
    private WriteChannel<Boolean> writeOnOrOff;
    private boolean active = false;
    private boolean reverse;
    private Mcp register;

    public RelaisActuatorTask(Mcp register, int position, boolean isOpener, Channel<Boolean> onOrOff, WriteChannel<Boolean> writeOnOrOff, String relaisBoard) {
        super(relaisBoard);
        this.position = position;
        this.reverse = isOpener;
        this.onOrOff = onOrOff;
        this.writeOnOrOff = writeOnOrOff;
        this.register = register;
        if (reverse) {
            active = true;
        }
    }

    private void changed() {
        if (register instanceof Mcp23008) {
            if (reverse) {
                ((Mcp23008) register).setPosition(this.position, !active);
            } else {
                ((Mcp23008) register).setPosition(this.position, active);
            }
            ((Mcp23008) register).shift();
        }

    }


    @Override
    public void activate() {
        if (!active) {
            active = true;
            this.onOrOff.setNextValue(true);
            changed();


        }
    }

    @Override
    public void deactivate() {
        if (active) {
            active = false;
            this.onOrOff.setNextValue(false);
            changed();
        }

    }

    @Override
    public Channel<Boolean> getReadChannel() {
        return this.onOrOff;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public WriteChannel<Boolean> getWriteChannel() {
        return this.writeOnOrOff;
    }

    @Override
    public boolean isReverse() {
        return this.reverse;
    }

    @Override
    public int getPosition() {
        return this.position;
    }

}
