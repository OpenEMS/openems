package io.openems.edge.pwmModule.api;
/*..
 * This Class is somewhat copied of the official pi4j GpioProvider,
 * got problems with it (Devices won't activate in Osgi bc of it)
 * Thats why I'm using same/edited functions etc of those classes.
 *
 * Credit still goes to the awesome Pi4j Team. So please check them out and support them :)
 * Thank you!*
 * */

import com.pi4j.io.gpio.exception.ValidationException;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import io.openems.common.exceptions.OpenemsException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Pca9685GpioProvider extends PcaGpioProvider implements IpcaGpioProvider {

    public static final int INTERNAL_CLOCK_FREQ = 25000000;
    public static final BigDecimal MIN_FREQUENCY = new BigDecimal("40");
    public static final BigDecimal MAX_FREQUENCY = new BigDecimal("1600");
    public static final BigDecimal ANALOG_SERVO_FREQUENCY = new BigDecimal("45.454");
    public static final BigDecimal DIGITAL_SERVO_FREQUENCY = new BigDecimal("90.909");
    public static final BigDecimal DEFAULT_FREQUENCY;
    public static final int PWM_STEPS = 4096;
    private boolean i2cBusOwner;
    private I2CBus bus;
    private I2CDevice device;
    private BigDecimal frequency;
    private int periodDurationMicros;
    private int maxPinPos = 7;


    public Pca9685GpioProvider(I2CBus bus, String address, BigDecimal targetFrequency, BigDecimal frequencyCorrectionFactor) throws IOException {

        this.i2cBusOwner = false;
        this.bus = bus;
        allocateAddress(address);
        this.device.write(0, (byte) 0);
        this.setFrequency(targetFrequency, frequencyCorrectionFactor);
    }

    private void allocateAddress(String address) {
        try {
            switch (address) {
                default:
                case "0x55":
                    this.device = bus.getDevice(0x55);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setPwm(int pinPos, int onPos, int offPos) throws OpenemsException {
        if (pinPos > maxPinPos) {

            throw new OpenemsException("pinPosition too Large, Check: Max Pin Value is: " + this.maxPinPos);
        }
        this.validatePositionPwmRange(onPos);
        this.validatePositionPwmRange(offPos);
        if (onPos == offPos) {
            throw new ValidationException("ON [" + onPos + "] and OFF [" + offPos + "] values must be different.");
        } else {
            try {
                this.device.write(6 + 4 * pinPos, (byte) (onPos & 255));
                this.device.write(7 + 4 * pinPos, (byte) (onPos >> 8));
                this.device.write(8 + 4 * pinPos, (byte) (offPos & 255));
                this.device.write(9 + 4 * pinPos, (byte) (offPos >> 8));
            } catch (IOException var6) {
                throw new RuntimeException("Unable to write to PWM channel [" + pinPos + "] values for ON [" + onPos + "] and OFF [" + offPos + "] position.", var6);
            }

        }

    }

    @Override
    public void setPwm(int pinPos, int offPos) throws OpenemsException {
        this.setPwm(pinPos, 0, offPos);
    }


    @Override
    public void validatePositionPwmRange(int onOrOffPos) {

        if (onOrOffPos < 0 || onOrOffPos > 4095) {
            throw new ValidationException("PWM position value [" + onOrOffPos + "] must be between 0 and 4095.");
        }

    }

    @Override
    public void setAlwaysOn(int pinPos) {
        try {

            this.device.write(6 + 4 * pinPos, (byte) 0);
            this.device.write(7 + 4 * pinPos, (byte) 16);
            this.device.write(8 + 4 * pinPos, (byte) 0);
            this.device.write(9 + 4 * pinPos, (byte) 0);
        } catch (IOException var6) {
            throw new RuntimeException("Error while trying to set channel [" + pinPos + "] always ON.", var6);
        }
    }

    @Override
    public void setAlwaysOff(int pinPos) {
        try {
            this.device.write(6 + 4 * pinPos, (byte) 0);
            this.device.write(7 + 4 * pinPos, (byte) 0);
            this.device.write(8 + 4 * pinPos, (byte) 0);
            this.device.write(9 + 4 * pinPos, (byte) 16);
        } catch (IOException var6) {
            throw new RuntimeException("Error while trying to set channel [" + pinPos + "] always OFF.", var6);
        }
    }


    @Override
    public void setFrequency(BigDecimal targetFrequency, BigDecimal frequencyCorrectionFactor) {
        this.validateFrequency(targetFrequency);
        this.frequency = targetFrequency;
        this.periodDurationMicros = this.calculatePeriodDuration();
        int prescale = this.calculatePrescale(frequencyCorrectionFactor);

        try {
            int oldMode = this.device.read(0);
            int newMode = oldMode & 127 | 16;
            this.device.write(0, (byte) newMode);
            this.device.write(254, (byte) prescale);
            this.device.write(0, (byte) oldMode);
            Thread.sleep(1L);
            this.device.write(0, (byte) (oldMode | 128));
        } catch (IOException var6) {
            throw new RuntimeException("Unable to set prescale value [" + prescale + "]", var6);
        } catch (InterruptedException var7) {
            throw new RuntimeException(var7);
        }
    }

    private void validateFrequency(BigDecimal frequency) {
        if (frequency.compareTo(MIN_FREQUENCY) == -1 || frequency.compareTo(MAX_FREQUENCY) == 1) {
            throw new ValidationException("Frequency [" + frequency + "] must be between 40.0 and 1600.0 Hz.");
        }
    }

    private int calculatePeriodDuration() {
        return (new BigDecimal("1000000")).divide(this.frequency, 0, RoundingMode.HALF_UP).intValue();
    }

    private int calculatePrescale(BigDecimal frequencyCorrectionFactor) {
        BigDecimal theoreticalPrescale = BigDecimal.valueOf(25000000L);
        theoreticalPrescale = theoreticalPrescale.divide(BigDecimal.valueOf(4096L), 3, RoundingMode.HALF_UP);
        theoreticalPrescale = theoreticalPrescale.divide(this.frequency, 0, RoundingMode.HALF_UP);
        theoreticalPrescale = theoreticalPrescale.subtract(BigDecimal.ONE);
        return theoreticalPrescale.multiply(frequencyCorrectionFactor).intValue();
    }

    static {
        DEFAULT_FREQUENCY = ANALOG_SERVO_FREQUENCY;
    }
}
