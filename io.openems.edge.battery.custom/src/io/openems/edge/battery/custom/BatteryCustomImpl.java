package io.openems.edge.battery.custom;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsException;

import io.openems.edge.battery.api.Battery;

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;

import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component(
        name = "io.openems.edge.battery.custom",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class BatteryCustomImpl
        extends AbstractOpenemsModbusComponent
        implements Battery,
                   ModbusComponent,
                   OpenemsComponent,
                   StartStoppable {

    @Reference
    private ConfigurationAdmin cm;

    @Reference
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    private StartStop startStop = StartStop.START;
    private Config config = null;

    public BatteryCustomImpl() {
        super(
                OpenemsComponent.ChannelId.values(),
                ModbusComponent.ChannelId.values(),
                Battery.ChannelId.values()
        );
    }

    @Activate
    private void activate(ComponentContext context, Config config)
            throws OpenemsException {

        if (super.activate(
                context,
                config.id(),
                config.alias(),
                config.enabled(),
                config.modbusUnitId(),
                this.cm,
                "Modbus",
                config.modbus_id())) {
            return;
        }

        this.config = config;

        /*
         * Fallback values — prevent null during startup or Modbus read gaps.
         *
         * Battery API requires these 4 to be non-null before ESS will start:
         *   CHARGE_MAX_VOLTAGE, CHARGE_MAX_CURRENT,
         *   DISCHARGE_MIN_VOLTAGE, DISCHARGE_MAX_CURRENT
         *
         * These are conservative minimums; real values come from
         * the FC3 read task every cycle.
         */
        this._setChargeMaxVoltage(480);
        this._setChargeMaxCurrent(20);
        this._setDischargeMinVoltage(450);
        this._setDischargeMaxCurrent(20);
        this._setVoltage(480);
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    /*
     * =========================================================
     * Modbus Register Map (Node-RED simulation → this battery)
     *
     * Reg | Battery.ChannelId          | Unit  | Node-RED value
     * ----|----------------------------|-------|----------------
     * 100 | SOC                        | %     | soc_pct (0-100)
     * 101 | SOH                        | %     | 98
     * 102 | VOLTAGE                    | V     | 450-510
     * 103 | CURRENT                    | A     | signed
     * 104 | CAPACITY                   | MWh   | 20 → ×10^6 = Wh
     * 105 | CHARGE_MAX_VOLTAGE         | V     | 550 (gate)
     * 106 | CHARGE_MAX_CURRENT         | A     | thermally derated (gate)
     * 107 | DISCHARGE_MIN_VOLTAGE      | V     | 400 (gate)
     * 108 | DISCHARGE_MAX_CURRENT      | A     | derated (gate)
     * 109 | MIN_CELL_TEMPERATURE       | °C    | signed
     * 110 | MAX_CELL_TEMPERATURE       | °C    | signed, drives derating
     * 111 | MIN_CELL_VOLTAGE           | mV    | 2900-3600
     * 112 | MAX_CELL_VOLTAGE           | mV    | min+30
     * 113 | INNER_RESISTANCE           | mOhm  | 5
     * =========================================================
     */
    @Override
    protected ModbusProtocol defineModbusProtocol() {
        return new ModbusProtocol(this,

            new FC3ReadRegistersTask(100, Priority.HIGH,

                /*
                 * SOC: Node-RED writes soc_pct directly (e.g. 9 = 9%)
                 * DIRECT_1_TO_1: no scaling
                 */
                m(Battery.ChannelId.SOC,
                    new SignedWordElement(100),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                /*
                 * SOH: 0-100 %
                 */
                m(Battery.ChannelId.SOH,
                    new SignedWordElement(101),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                /*
                 * VOLTAGE: V
                 */
                m(Battery.ChannelId.VOLTAGE,
                    new SignedWordElement(102),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                /*
                 * CURRENT: A signed (+discharge / -charge)
                 */
                m(Battery.ChannelId.CURRENT,
                    new SignedWordElement(103),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                /*
                 * CAPACITY: Node-RED writes MWh (20 = 20 MWh)
                 * Battery.ChannelId.CAPACITY expects Wh
                 * SCALE_FACTOR_6: multiply by 10^6
                 * → 20 × 1,000,000 = 20,000,000 Wh = 20 MWh ✓
                 *
                 * If SCALE_FACTOR_6 is not available in your OpenEMS version,
                 * use: new ElementToChannelConverterChain(
                 *          ElementToChannelConverter.SCALE_FACTOR_3,
                 *          ElementToChannelConverter.SCALE_FACTOR_3)
                 */
             // Register 104: CAPACITY
             // Node-RED writes kWh as unsigned (e.g. 20000 = 20000 kWh)
             // Chain ×10 ×10 ×10 = ×1000 → converts kWh to Wh
             // 20000 kWh × 1000 = 20,000,000 Wh ✓
             // UnsignedWordElement because 20000 > 32767 (INT16 max)
             m(Battery.ChannelId.CAPACITY,
                 new UnsignedWordElement(104),
                 ElementToChannelConverter.chain(
                     ElementToChannelConverter.SCALE_FACTOR_1,  // ×10
                     ElementToChannelConverter.SCALE_FACTOR_1,  // ×10
                     ElementToChannelConverter.SCALE_FACTOR_1   // ×10 → total ×1000
                 )
             ),
                /*
                 * CHARGE_MAX_VOLTAGE: V
                 * Gate channel — must not be null or ESS won't start
                 */
                m(Battery.ChannelId.CHARGE_MAX_VOLTAGE,
                    new SignedWordElement(105),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                /*
                 * CHARGE_MAX_CURRENT: A
                 * Gate channel — thermally derated in Node-RED
                 * AllowedChargeDischargeHandler uses: CHARGE_MAX_CURRENT × VOLTAGE
                 */
                m(Battery.ChannelId.CHARGE_MAX_CURRENT,
                    new SignedWordElement(106),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                /*
                 * DISCHARGE_MIN_VOLTAGE: V
                 * Gate channel — must not be null
                 */
                m(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE,
                    new SignedWordElement(107),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                /*
                 * DISCHARGE_MAX_CURRENT: A
                 * Gate channel — thermally derated above 50°C
                 * AllowedChargeDischargeHandler uses: DISCHARGE_MAX_CURRENT × VOLTAGE
                 */
                m(Battery.ChannelId.DISCHARGE_MAX_CURRENT,
                    new SignedWordElement(108),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                /*
                 * MIN_CELL_TEMPERATURE: °C signed
                 */
                m(Battery.ChannelId.MIN_CELL_TEMPERATURE,
                    new SignedWordElement(109),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                /*
                 * MAX_CELL_TEMPERATURE: °C signed
                 * Used for thermal derating in AllowedChargeDischargeHandler
                 */
                m(Battery.ChannelId.MAX_CELL_TEMPERATURE,
                    new SignedWordElement(110),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                /*
                 * MIN_CELL_VOLTAGE: mV
                 */
                m(Battery.ChannelId.MIN_CELL_VOLTAGE,
                    new SignedWordElement(111),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                /*
                 * MAX_CELL_VOLTAGE: mV
                 */
                m(Battery.ChannelId.MAX_CELL_VOLTAGE,
                    new SignedWordElement(112),
                    ElementToChannelConverter.DIRECT_1_TO_1),

                /*
                 * INNER_RESISTANCE: mOhm
                 */
                m(Battery.ChannelId.INNER_RESISTANCE,
                    new SignedWordElement(113),
                    ElementToChannelConverter.DIRECT_1_TO_1)
            )
        );
    }

    @Override
    public void setStartStop(StartStop startStop) {
        this.startStop = startStop;
    }

    @Override
    public StartStop getStartStop() {
        return this.startStop;
    }

    @Override
    public String debugLog() {
        return "SOC:"        + this.getSoc().asString()
             + " V:"         + this.getVoltage().asString()
             + " ChgMaxI:"   + this.getChargeMaxCurrent().asString()
             + " DisMaxI:"   + this.getDischargeMaxCurrent().asString()
             + " MaxTemp:"   + this.getMaxCellTemperature().asString()
             + " Capacity:"  + this.getCapacity().asString();
    }
}