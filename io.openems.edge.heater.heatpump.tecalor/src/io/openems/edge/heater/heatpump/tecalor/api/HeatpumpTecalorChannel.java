package io.openems.edge.heater.heatpump.tecalor.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.heater.api.HeatpumpSmartGridGeneralizedChannel;

public interface HeatpumpTecalorChannel extends HeatpumpSmartGridGeneralizedChannel {


    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {


        // Input Registers, read only. They are 16 bit signed numbers unless stated otherwise.
        // Addresses are 1 based, so 40001 is the first holding register. OpenEMS Modbus is 0 based,
        // so 40001 has address 0.

        /**
         * Outside temperature.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR507_AUSSENTEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Heating circuit 1 actual temperature.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR508_ISTTEMPHK1(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Heating circuit 1 setpoint temperature. Software version WPM 3i
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR509_SOLLTEMPHK1(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Heating circuit 1 setpoint temperature. Software version WPMsystem and WPM 3
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR510_SOLLTEMPHK1(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Heating circuit 1 setpoint temperature. Channel that is independent of software version.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        SOLLTEMPHK1(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Heating circuit 2 actual temperature.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR511_ISTTEMPHK2(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Heating circuit 2 setpoint temperature.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR512_SOLLTEMPHK2(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Forward temperature heat pump, actual.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR513_VORLAUFISTTEMPWP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Forward temperature backup heater, actual.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR514_VORLAUFISTTEMPNHZ(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Forward temperature, actual.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR515_VORLAUFISTTEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Rewind temperature, actual.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR516_RUECKLAUFISTTEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Fix temperature setpoint.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR517_FESTWERTSOLLTEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Buffer tank temperature, actual.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR518_PUFFERISTTEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Buffer tank temperature, setpoint.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR519_PUFFERSOLLTEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Heating circuit pressure.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: centi bar (bar e-2)
         * </ul>
         */
        IR520_HEIZUNGSDRUCK(Doc.of(OpenemsType.INTEGER).unit(Unit.CENTI_BAR)),

        /**
         * Heating circuit current.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: decimal liters per minute (l/min e-1)
         * </ul>
         */
        IR521_VOLUMENSTROM(Doc.of(OpenemsType.INTEGER).unit(Unit.DECILITER_PER_MINUTE)),

        /**
         * Warm water temperature, actual.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR522_WWISTTEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Warm water temperature, setpoint.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR523_WWSOLLTEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Ventilation cooling temperature, actual.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Kelvin
         * </ul>
         */
        IR524_GEBLAESEISTTEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_KELVIN)),

        /**
         * Ventilation cooling temperature, setpoint.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Kelvin
         * </ul>
         */
        IR525_GEBLAESESOLLTEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_KELVIN)),

        /**
         * Surface cooling temperature, actual.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Kelvin
         * </ul>
         */
        IR526_FLAECHEISTTEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_KELVIN)),

        /**
         * Surface cooling temperature, setpoint.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Kelvin
         * </ul>
         */
        IR527_FLAECHESOLLTEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_KELVIN)),

        /**
         * Minimum operation temperature heating circuit.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR533_EINSATZGRENZEHZG(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Minimum operation temperature warm water.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR534_EINSATZGRENZEWW(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Status bits.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR2501_STATUSBITS(Doc.of(OpenemsType.INTEGER)),

        /**
         * EVU clearance.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        IR2502_EVUFREIGABE(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Error status. False for no error.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        IR2504_ERRORSTATUS(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * BUS status.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR2505_BUSSTATUS(Doc.of(OpenemsType.INTEGER)),

        /**
         * Defrost engaged.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        IR2506_DEFROST(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Error number.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR2507_ERRORNUMBER(Doc.of(OpenemsType.INTEGER)),

        /**
         * Produced heat for heating circuits, all heat pumps combined for this day.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3501_HEATPRODUCED_VDHEIZENTAG(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Produced heat for heating circuits total, all heat pumps combined. Low value.
         * This is the kWh value. Anything above 999 is transferred to the MWh value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3502_HEATPRODUCED_VDHEIZENSUMKWH(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Produced heat for heating circuits total, all heat pumps combined. High value.
         * The kWh value needs to be added to this value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: megawatt hours
         * </ul>
         */
        IR3503_HEATPRODUCED_VDHEIZENSUMMWH(Doc.of(OpenemsType.INTEGER).unit(Unit.MEGAWATT_HOURS)),

        /**
         * Produced heat for heating circuits total, all heat pumps combined.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        HEATPRODUCED_VDHEIZENSUM(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Produced heat for warm water, all heat pumps combined for this day.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3504_HEATPRODUCED_VDWWTAG(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Produced heat for warm water total, all heat pumps combined. Low value.
         * This is the kWh value. Anything above 999 is transferred to the MWh value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3505_HEATPRODUCED_VDWWSUMKWH(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Produced heat for warm water total, all heat pumps combined. High value.
         * The kWh value needs to be added to this value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: megawatt hours
         * </ul>
         */
        IR3506_HEATPRODUCED_VDWWSUMMWH(Doc.of(OpenemsType.INTEGER).unit(Unit.MEGAWATT_HOURS)),

        /**
         * Produced heat for warm water total, all heat pumps combined.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        HEATPRODUCED_VDWWSUM(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Produced heat for heating circuits total, backup heater. Low value.
         * This is the kWh value. Anything above 999 is transferred to the MWh value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3507_HEATPRODUCED_NZHHEIZENSUMKWH(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Produced heat for heating circuits total, backup heater. High value.
         * The kWh value needs to be added to this value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3508_HEATPRODUCED_NZHHEIZENSUMMWH(Doc.of(OpenemsType.INTEGER).unit(Unit.MEGAWATT_HOURS)),

        /**
         * Produced heat for heating circuits total, backup heater.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        HEATPRODUCED_NZHHEIZENSUM(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Produced heat for warm water total, backup heater. Low value.
         * This is the kWh value. Anything above 999 is transferred to the MWh value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3509_HEATPRODUCED_NZHWWSUMKWH(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Produced heat for warm water total, backup heater. High value.
         * The kWh value needs to be added to this value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3510_HEATPRODUCED_NZHWWSUMMWH(Doc.of(OpenemsType.INTEGER).unit(Unit.MEGAWATT_HOURS)),

        /**
         * Produced heat for warm water total, backup heater.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        HEATPRODUCED_NZHWWSUM(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Consumed power for heating the heating circuits, all heat pumps combined for this day.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3511_CONSUMEDPOWER_VDHEIZENTAG(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Consumed power for heating the heating circuits total, all heat pumps combined. Low value.
         * This is the kWh value. Anything above 999 is transferred to the MWh value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3512_CONSUMEDPOWER_VDHEIZENSUMKWH(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Consumed power for heating the heating circuits total, all heat pumps combined. High value.
         * The kWh value needs to be added to this value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: megawatt hours
         * </ul>
         */
        IR3513_CONSUMEDPOWER_VDHEIZENSUMMWH(Doc.of(OpenemsType.INTEGER).unit(Unit.MEGAWATT_HOURS)),

        /**
         * Consumed power for heating the heating circuits total, all heat pumps combined.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        CONSUMEDPOWER_VDHEIZENSUM(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Consumed power for heating warm water, all heat pumps combined for this day.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3514_CONSUMEDPOWER_VDWWTAG(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Consumed power for heating warm water total, all heat pumps combined. Low value.
         * This is the kWh value. Anything above 999 is transferred to the MWh value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        IR3515_CONSUMEDPOWER_VDWWSUMKWH(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * Consumed power for heating warm water total, all heat pumps combined. High value.
         * The kWh value needs to be added to this value.
         * This is just for modbus, don't use this.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: megawatt hours
         * </ul>
         */
        IR3516_CONSUMEDPOWER_VDWWSUMMWH(Doc.of(OpenemsType.INTEGER).unit(Unit.MEGAWATT_HOURS)),

        /**
         * Consumed power for heating warm water total, all heat pumps combined.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: kilowatt hours
         * </ul>
         */
        CONSUMEDPOWER_VDWWSUM(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),

        /**
         * SG-Ready Operating mode.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 1 ... 4
         *      <li> State 1: Die Anlage darf nicht starten. Nur der Frostschutz wird gewährleistet.
         *      <li> State 2: Normaler Betrieb der Anlage. Automatik/Programmbetrieb gemäß BI der angeschlossenen Wärmepumpe.
         *      <li> State 3: Forcierter Betrieb der Anlage mit erhöhten Werten für Heiz- und/oder Warmwassertemperatur.
         *      <li> State 4: Sofortige Ansteuerung der Maximalwerte für Heiz- und Warmwassertemperatur.
         * </ul>
         */
        IR5001_SGREADY_OPERATINGMODE(Doc.of(OpenemsType.INTEGER)),

        /**
         * Reglerkennung.
         * <ul>
         *      <li> Type: Integer
         *      <li> Value 103: THZ 303, 403 (Integral/SOL), THD 400 AL, THZ 304 eco, 404 eco, THZ 304/404 FLEX,
         *      THZ 5.5 eco, THZ 5.5 FLEX, TCO 2.5
         *      <li> Value 104: THZ 304, 404 (SOL), THZ 504
         *      <li> Value 390: WPM 3
         *      <li> Value 391: WPM 3i
         *      <li> Value 449: WPMsystem
         * </ul>
         */
        IR5002_REGLERKENNUNG(Doc.of(OpenemsType.INTEGER)),



        // Holding Registers, read/write. Signed 16 bit, unless stated otherwise.

        /**
         * Operating mode.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0 ... 5
         *      <li> State 0: Notbetrieb
         *      <li> State 1: Bereitschaftsbetrieb
         *      <li> State 2: Programmbetrieb
         *      <li> State 3: Komfortbetrieb
         *      <li> State 4: ECO-Betrieb
         *      <li> State 5: Warmwasserbetrieb
         * </ul>
         */
        HR1501_BERTIEBSART(Doc.of(OperatingMode.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * Comfort temperature setting, heating circuit 1.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1502_KOMFORTTEMPHK1(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * ECO temperature setting, heating circuit 1.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1503_ECOTEMPHK1(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heating curve slope setting, heating circuit 1.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR1504_SLOPEHK1(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),

        /**
         * Comfort temperature setting, heating circuit 2.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1505_KOMFORTTEMPHK2(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * ECO temperature setting, heating circuit 2.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1506_ECOTEMPHK2(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heating curve slope setting, heating circuit 2.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR1507_SLOPEHK2(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),

        /**
         * Static temperature mode setting. 0x9000 disables this mode, a temperature value between 200 and 700 enables
         * this mode.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1508_FESTWERTBETRIEB(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Backup heater activation temperature, heating circuit. Below this temperature the backup heater will
         * activate, depending on heat demand.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1509_BIVALENZTEMPERATURHZG(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Comfort temperature setting, warm water.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1510_KOMFORTTEMPWW(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * ECO temperature setting, warm water.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1511_ECOTEMPWW(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Number of stages, warm water.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR1512_WARMWASSERSTUFEN(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),

        /**
         * Backup heater activation temperature, warm water. Below this temperature the backup heater will
         * activate, depending on heat demand.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1513_BIVALENZTEMPERATURWW(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Cooling: Forward temperature, setpoint, surface cooling.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1514_VORLAUFSOLLTEMPFLAECHENKUEHLUNG(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Cooling: Forward temperature hysteresis, surface cooling.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Kelvin
         * </ul>
         */
        HR1515_HYSTERESEVORLAUFTEMPFLAECHENKUEHLUNG(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_KELVIN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Cooling: Room temperature, setpoint, surface cooling.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1516_RAUMSOLLTEMPFLAECHENKUEHLUNG(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Cooling: Forward temperature, setpoint, ventilation cooling.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1517_VORLAUFSOLLTEMPGEBLAESEKUEHLUNG(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Cooling: Forward temperature hysteresis, ventilation cooling.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Kelvin
         * </ul>
         */
        HR1518_HYSTERESEVORLAUFTEMPGEBLAESEKUEHLUNG(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_KELVIN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Cooling: Room temperature, setpoint, ventilation cooling.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1519_RAUMSOLLTEMPGEBLAESEKUEHLUNG(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Reset.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 1 ... 3
         *      <li> State 1: Reset System
         *      <li> State 2: Reset Error
         *      <li> State 3: Reset Heatpump
         * </ul>
         */
        HR1520_RESET(Doc.of(Reset.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * Restart internet service gateway (ISG).
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0 ... 2
         *      <li> State 0: Off
         *      <li> State 1: Restart
         *      <li> State 2: Service button
         * </ul>
         */
        HR1521_RESTART_ISG(Doc.of(Restart.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * Turn on/off SG-Ready capabilities.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        HR4001_SGREADY_ONOFF(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * SG-Ready input 1.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        HR4002_SGREADY_INPUT1(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * SG-Ready input 2.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        HR4003_SGREADY_INPUT2(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE));


        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    // Input Registers. Read only.

    /**
     * Gets the Channel for {@link ChannelId#IR507_AUSSENTEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getAussentempChannel() {
        return this.channel(ChannelId.IR507_AUSSENTEMP);
    }

    /**
     * Outside temperature. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getAussentemp() { return this.getAussentempChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR508_ISTTEMPHK1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getIstTempHk1Channel() {
        return this.channel(ChannelId.IR508_ISTTEMPHK1);
    }

    /**
     * Heating circuit 1 actual temperature. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getIstTempHk1() { return this.getIstTempHk1Channel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#SOLLTEMPHK1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getSetpointTempHk1Channel() {
        return this.channel(ChannelId.SOLLTEMPHK1);
    }

    /**
     * Heating circuit 1 setpoint temperature. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getSetpointTempHk1() { return this.getIstTempHk1Channel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR511_ISTTEMPHK2}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getIstTempHk2Channel() {
        return this.channel(ChannelId.IR511_ISTTEMPHK2);
    }

    /**
     * Heating circuit 2 actual temperature. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getIstTempHk2() { return this.getIstTempHk2Channel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR512_SOLLTEMPHK2}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getSetpointTempHk2Channel() {
        return this.channel(ChannelId.IR512_SOLLTEMPHK2);
    }

    /**
     * Heating circuit 2 setpoint temperature. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getSetpointTempHk2() { return this.getSetpointTempHk2Channel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR513_VORLAUFISTTEMPWP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getForwardTempHeatPumpActualChannel() {
        return this.channel(ChannelId.IR513_VORLAUFISTTEMPWP);
    }

    /**
     * Forward temperature heat pump, actual. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getForwardTempHeatPumpActual() { return this.getForwardTempHeatPumpActualChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR514_VORLAUFISTTEMPNHZ}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getForwardTempBackupHeaterActualChannel() {
        return this.channel(ChannelId.IR514_VORLAUFISTTEMPNHZ);
    }

    /**
     * Forward temperature backup heater, actual. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getForwardTempBackupHeaterActual() { return this.getForwardTempBackupHeaterActualChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR515_VORLAUFISTTEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getForwardTempActualChannel() {
        return this.channel(ChannelId.IR515_VORLAUFISTTEMP);
    }

    /**
     * Forward temperature, actual. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getForwardTempActual() { return this.getForwardTempActualChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR516_RUECKLAUFISTTEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getRewindTempActualChannel() {
        return this.channel(ChannelId.IR516_RUECKLAUFISTTEMP);
    }

    /**
     * Rewind temperature, actual. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getRewindTempActual() { return this.getRewindTempActualChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR517_FESTWERTSOLLTEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getFixTempSetpointChannel() {
        return this.channel(ChannelId.IR517_FESTWERTSOLLTEMP);
    }

    /**
     * Fix temperature setpoint. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getFixTempSetpoint() { return this.getFixTempSetpointChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR518_PUFFERISTTEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getBuffetTankTempActualChannel() {
        return this.channel(ChannelId.IR518_PUFFERISTTEMP);
    }

    /**
     * Buffer tank temperature, actual. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getBuffetTankTempActual() { return this.getBuffetTankTempActualChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR519_PUFFERSOLLTEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getBuffetTankTempSetpointChannel() {
        return this.channel(ChannelId.IR519_PUFFERSOLLTEMP);
    }

    /**
     * Buffer tank temperature, setpoint. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getBuffetTankTempSetpoint() { return this.getBuffetTankTempSetpointChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR520_HEIZUNGSDRUCK}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHeatingCircuitPressureChannel() {
        return this.channel(ChannelId.IR520_HEIZUNGSDRUCK);
    }

    /**
     * Heating circuit pressure. Unit is centi bar (bar e-2).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHeatingCircuitPressure() { return this.getHeatingCircuitPressureChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR521_VOLUMENSTROM}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHeatingCircuitCurrentChannel() {
        return this.channel(ChannelId.IR521_VOLUMENSTROM);
    }

    /**
     * Heating circuit current. Unit is decimal liters per minute (l/min e-1).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHeatingCircuitCurrent() { return this.getHeatingCircuitCurrentChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR522_WWISTTEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getWarmWaterTempActualChannel() { return this.channel(ChannelId.IR522_WWISTTEMP); }

    /**
     * Warm water temperature, actual. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getWarmWaterTempActual() { return this.getWarmWaterTempActualChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR523_WWSOLLTEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getWarmWaterTempSetpointChannel() { return this.channel(ChannelId.IR523_WWSOLLTEMP); }

    /**
     * Warm water temperature, setpoint. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getWarmWaterTempSetpoint() { return this.getWarmWaterTempSetpointChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR524_GEBLAESEISTTEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getVentilationCoolingTempActualChannel() { return this.channel(ChannelId.IR524_GEBLAESEISTTEMP); }

    /**
     * Ventilation cooling temperature, actual. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getVentilationCoolingTempActual() { return this.getVentilationCoolingTempActualChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR525_GEBLAESESOLLTEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getVentilationCoolingTempSetpointChannel() { return this.channel(ChannelId.IR525_GEBLAESESOLLTEMP); }

    /**
     * Ventilation cooling temperature, setpoint. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getVentilationCoolingTempSetpoint() { return this.getVentilationCoolingTempSetpointChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR526_FLAECHEISTTEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getSurfaceCoolingTempActualChannel() { return this.channel(ChannelId.IR526_FLAECHEISTTEMP); }

    /**
     * Surface cooling temperature, actual. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getSurfaceCoolingTempActual() { return this.getSurfaceCoolingTempActualChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR527_FLAECHESOLLTEMP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getSurfaceCoolingTempSetpointChannel() { return this.channel(ChannelId.IR527_FLAECHESOLLTEMP); }

    /**
     * Surface cooling temperature, setpoint. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getSurfaceCoolingTempSetpoint() { return this.getSurfaceCoolingTempSetpointChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR533_EINSATZGRENZEHZG}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getMinOperationTempHeatingCircuitChannel() { return this.channel(ChannelId.IR533_EINSATZGRENZEHZG); }

    /**
     * Minimum operation temperature heating circuit. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getMinOperationTempHeatingCircuit() { return this.getMinOperationTempHeatingCircuitChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR534_EINSATZGRENZEWW}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getMinOperationTempWarmWaterChannel() { return this.channel(ChannelId.IR534_EINSATZGRENZEWW); }

    /**
     * Minimum operation temperature warm water. Unit is decimal degree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getMinOperationTempWarmWater() { return this.getMinOperationTempWarmWaterChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR2501_STATUSBITS}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getStatusBitsChannel() { return this.channel(ChannelId.IR2501_STATUSBITS); }

    /**
     * Status bits.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getStatusBits() { return this.getStatusBitsChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR2502_EVUFREIGABE}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getEvuClearanceChannel() { return this.channel(ChannelId.IR2502_EVUFREIGABE); }

    /**
     * EVU clearance.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getEvuClearance() { return this.getEvuClearanceChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR2504_ERRORSTATUS}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getErrorStatusChannel() { return this.channel(ChannelId.IR2504_ERRORSTATUS); }

    /**
     * Error status. False for no error.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getErrorStatus() { return this.getErrorStatusChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR2505_BUSSTATUS}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getBusStatusChannel() { return this.channel(ChannelId.IR2505_BUSSTATUS); }

    /**
     * BUS status.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getBusStatus() { return this.getBusStatusChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR2506_DEFROST}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getDefrostActiveChannel() { return this.channel(ChannelId.IR2506_DEFROST); }

    /**
     * Defrost engaged.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getDefrostActive() { return this.getDefrostActiveChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR2507_ERRORNUMBER}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getErrorNumberChannel() { return this.channel(ChannelId.IR2507_ERRORNUMBER); }

    /**
     * Error number.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getErrorNumber() { return this.getErrorNumberChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR3501_HEATPRODUCED_VDHEIZENTAG}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getProducedHeatForHkDailyChannel() { return this.channel(ChannelId.IR3501_HEATPRODUCED_VDHEIZENTAG); }

    /**
     * Produced heat for heating circuits, all heat pumps combined for this day.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getProducedHeatForHkDaily() { return this.getProducedHeatForHkDailyChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#HEATPRODUCED_VDHEIZENSUM}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getProducedHeatForHkSumChannel() { return this.channel(ChannelId.HEATPRODUCED_VDHEIZENSUM); }

    /**
     * Produced heat for heating circuits total, all heat pumps combined.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getProducedHeatForHkSum() { return this.getProducedHeatForHkSumChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR3504_HEATPRODUCED_VDWWTAG}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getProducedHeatForWwDailyChannel() { return this.channel(ChannelId.IR3504_HEATPRODUCED_VDWWTAG); }

    /**
     * Produced heat for warm water, all heat pumps combined for this day.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getProducedHeatForWwDaily() { return this.getProducedHeatForWwDailyChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#HEATPRODUCED_VDWWSUM}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getProducedHeatForWwSumChannel() { return this.channel(ChannelId.HEATPRODUCED_VDWWSUM); }

    /**
     * Produced heat for warm water total, all heat pumps combined.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getProducedHeatForWwSum() { return this.getProducedHeatForWwSumChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#HEATPRODUCED_NZHHEIZENSUM}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getProducedHeatNhzForHkSumChannel() { return this.channel(ChannelId.HEATPRODUCED_NZHHEIZENSUM); }

    /**
     * Produced heat for heating circuits total, backup heater.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getProducedHeatNhzForHkSum() { return this.getProducedHeatNhzForHkSumChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#HEATPRODUCED_NZHWWSUM}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getProducedHeatNhzForWwSumChannel() { return this.channel(ChannelId.HEATPRODUCED_NZHWWSUM); }

    /**
     * Produced heat for warm water total, backup heater.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getProducedHeatNhzForWwSum() { return this.getProducedHeatNhzForWwSumChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR3511_CONSUMEDPOWER_VDHEIZENTAG}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getConsumedPowerForHkDailyChannel() { return this.channel(ChannelId.IR3511_CONSUMEDPOWER_VDHEIZENTAG); }

    /**
     * Consumed power for heating the heating circuits, all heat pumps combined for this day.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getConsumedPowerForHkDaily() { return this.getConsumedPowerForHkDailyChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#CONSUMEDPOWER_VDHEIZENSUM}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getConsumedPowerForHkSumChannel() { return this.channel(ChannelId.CONSUMEDPOWER_VDHEIZENSUM); }

    /**
     * Consumed power for heating the heating circuits total, all heat pumps combined.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getConsumedPowerForHkSum() { return this.getConsumedPowerForHkSumChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR3514_CONSUMEDPOWER_VDWWTAG}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getConsumedPowerForWwDailyChannel() { return this.channel(ChannelId.IR3514_CONSUMEDPOWER_VDWWTAG); }

    /**
     * Consumed power for heating warm water, all heat pumps combined for this day.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getConsumedPowerForWwDaily() { return this.getConsumedPowerForWwDailyChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#CONSUMEDPOWER_VDWWSUM}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getConsumedPowerForWwSumChannel() { return this.channel(ChannelId.CONSUMEDPOWER_VDWWSUM); }

    /**
     * Consumed power for heating warm water total, all heat pumps combined.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getConsumedPowerForWwSum() { return this.getConsumedPowerForWwSumChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR5001_SGREADY_OPERATINGMODE}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getSgReadyOperatingModeChannel() { return this.channel(ChannelId.IR5001_SGREADY_OPERATINGMODE); }

    /**
     * SG-Ready Operating mode.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 1 ... 4
     *      <li> State 1: Die Anlage darf nicht starten. Nur der Frostschutz wird gewährleistet.
     *      <li> State 2: Normaler Betrieb der Anlage. Automatik/Programmbetrieb gemäß BI der angeschlossenen Wärmepumpe.
     *      <li> State 3: Forcierter Betrieb der Anlage mit erhöhten Werten für Heiz- und/oder Warmwassertemperatur.
     *      <li> State 4: Sofortige Ansteuerung der Maximalwerte für Heiz- und Warmwassertemperatur.
     * </ul>
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getSgReadyOperatingMode() { return this.getSgReadyOperatingModeChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR5002_REGLERKENNUNG}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getReglerkennungChannel() { return this.channel(ChannelId.IR5002_REGLERKENNUNG); }

    /**
     * Reglerkennung.
     * <ul>
     *      <li> Type: Integer
     *      <li> Value 103: THZ 303, 403 (Integral/SOL), THD 400 AL, THZ 304 eco, 404 eco, THZ 304/404 FLEX,
     *      THZ 5.5 eco, THZ 5.5 FLEX, TCO 2.5
     *      <li> Value 104: THZ 304, 404 (SOL), THZ 504
     *      <li> Value 390: WPM 3
     *      <li> Value 391: WPM 3i
     *      <li> Value 449: WPMsystem
     * </ul>
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getReglerkennung() { return this.getReglerkennungChannel().value(); }


    // Holding Registers. Read/Write.

    /**
     * Gets the Channel for {@link ChannelId#HR1501_BERTIEBSART}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getOperatingModeChannel() { return this.channel(ChannelId.HR1501_BERTIEBSART); }

    /**
     * get operating mode.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 5
     *      <li> State 0: Notbetrieb
     *      <li> State 1: Bereitschaftsbetrieb
     *      <li> State 2: Programmbetrieb
     *      <li> State 3: Komfortbetrieb
     *      <li> State 4: ECO-Betrieb
     *      <li> State 5: Warmwasserbetrieb
     * </ul>
	 * See {@link ChannelId#HR1501_BERTIEBSART}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getOperatingMode() {
		return this.getOperatingModeChannel().value();
	}
	
	/**
     * Set operating mode.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 5
     *      <li> State 0: Notbetrieb
     *      <li> State 1: Bereitschaftsbetrieb
     *      <li> State 2: Programmbetrieb
     *      <li> State 3: Komfortbetrieb
     *      <li> State 4: ECO-Betrieb
     *      <li> State 5: Warmwasserbetrieb
     * </ul> 
	 * See {@link ChannelId#HR1501_BERTIEBSART}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setOperatingMode(Integer value) throws OpenemsNamedException {
		this.getOperatingModeChannel().setNextWriteValue(value);
	}
    
    /**
     * Gets the Channel for {@link ChannelId#HR1502_KOMFORTTEMPHK1}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getComfortTempHk1Channel() { return this.channel(ChannelId.HR1502_KOMFORTTEMPHK1); }
    
    /**
     * Get the comfort temperature setting, heating circuit 1. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1502_KOMFORTTEMPHK1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getComfortTempHk1() {
		return this.getComfortTempHk1Channel().value();
	}
	
	/**
     * Set the comfort temperature setting, heating circuit 1. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1502_KOMFORTTEMPHK1}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setComfortTempHk1(Integer value) throws OpenemsNamedException {
		this.getComfortTempHk1Channel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#HR1503_ECOTEMPHK1}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getEcoTempHk1Channel() { return this.channel(ChannelId.HR1503_ECOTEMPHK1); }

    /**
     * Get the ECO temperature setting, heating circuit 1. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1503_ECOTEMPHK1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEcoTempHk1() {
		return this.getEcoTempHk1Channel().value();
	}
	
	/**
     * Set the ECO temperature setting, heating circuit 1. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1503_ECOTEMPHK1}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setEcoTempHk1(Integer value) throws OpenemsNamedException {
		this.getEcoTempHk1Channel().setNextWriteValue(value);
	}
    
    /**
     * Gets the Channel for {@link ChannelId#HR1504_SLOPEHK1}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getHeatingCurveSlopeHk1Channel() { return this.channel(ChannelId.HR1504_SLOPEHK1); }

    /**
     * Get the Heating curve slope setting, heating circuit 1.
	 * See {@link ChannelId#HR1504_SLOPEHK1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingCurveSlopeHk1() {
		return this.getHeatingCurveSlopeHk1Channel().value();
	}
	
	/**
     * Set the Heating curve slope setting, heating circuit 1.
	 * See {@link ChannelId#HR1504_SLOPEHK1}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveSlopeHk1(Integer value) throws OpenemsNamedException {
		this.getHeatingCurveSlopeHk1Channel().setNextWriteValue(value);
	}
    
    /**
     * Gets the Channel for {@link ChannelId#HR1505_KOMFORTTEMPHK2}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getComfortTempHk2Channel() { return this.channel(ChannelId.HR1505_KOMFORTTEMPHK2); }

    /**
     * Get the comfort temperature setting, heating circuit 2. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1505_KOMFORTTEMPHK2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getComfortTempHk2() {
		return this.getComfortTempHk2Channel().value();
	}
	
	/**
     * Set the comfort temperature setting, heating circuit 2. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1505_KOMFORTTEMPHK2}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setComfortTempHk2(Integer value) throws OpenemsNamedException {
		this.getComfortTempHk2Channel().setNextWriteValue(value);
	}
    
    /**
     * Gets the Channel for {@link ChannelId#HR1506_ECOTEMPHK2}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getEcoTempHk2Channel() { return this.channel(ChannelId.HR1506_ECOTEMPHK2); }

    /**
     * Get the ECO temperature setting, heating circuit 2. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1506_ECOTEMPHK2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEcoTempHk2() {
		return this.getEcoTempHk2Channel().value();
	}
	
	/**
     * Set the ECO temperature setting, heating circuit 2. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1506_ECOTEMPHK2}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setEcoTempHk2(Integer value) throws OpenemsNamedException {
		this.getEcoTempHk2Channel().setNextWriteValue(value);
	}
    
    /**
     * Gets the Channel for {@link ChannelId#HR1507_SLOPEHK2}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getHeatingCurveSlopeHk2Channel() { return this.channel(ChannelId.HR1507_SLOPEHK2); }

    /**
     * Get the Heating curve slope setting, heating circuit 2.
	 * See {@link ChannelId#HR1507_SLOPEHK2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingCurveSlopeHk2() {
		return this.getHeatingCurveSlopeHk2Channel().value();
	}
	
	/**
     * Set the Heating curve slope setting, heating circuit 2.
	 * See {@link ChannelId#HR1507_SLOPEHK2}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingCurveSlopeHk2(Integer value) throws OpenemsNamedException {
		this.getHeatingCurveSlopeHk2Channel().setNextWriteValue(value);
	}
    
    /**
     * Gets the Channel for {@link ChannelId#HR1508_FESTWERTBETRIEB}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getFixTempOperationModeChannel() { return this.channel(ChannelId.HR1508_FESTWERTBETRIEB); }

    /**
     * Get the static temperature mode setting. A value of 0x9000 means disabled. When enabled, the value will be 
     * a temperature value between 200 and 700. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1508_FESTWERTBETRIEB}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getFixTempOperationMode() {
		return this.getFixTempOperationModeChannel().value();
	}
	
	/**
     * Set the static temperature mode setting. 0x9000 disables this mode, a temperature value between 200 and 700
     * enables this mode. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1508_FESTWERTBETRIEB}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setFixTempOperationMode(Integer value) throws OpenemsNamedException {
		this.getFixTempOperationModeChannel().setNextWriteValue(value);
	}
    
    /**
     * Gets the Channel for {@link ChannelId#HR1509_BIVALENZTEMPERATURHZG}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getBackupHeaterActivationTempHkChannel() { return this.channel(ChannelId.HR1509_BIVALENZTEMPERATURHZG); }

    /**
     * Get the backup heater activation temperature of the heating circuit. Below this temperature the backup 
     * heater will activate, depending on heat demand. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1509_BIVALENZTEMPERATURHZG}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBackupHeaterActivationTempHk() {
		return this.getBackupHeaterActivationTempHkChannel().value();
	}
	
	/**
     * Set the backup heater activation temperature of the heating circuit. Below this temperature the backup 
     * heater will activate, depending on heat demand. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1509_BIVALENZTEMPERATURHZG}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setBackupHeaterActivationTempHk(Integer value) throws OpenemsNamedException {
		this.getBackupHeaterActivationTempHkChannel().setNextWriteValue(value);
	}
    
    /**
     * Gets the Channel for {@link ChannelId#HR1510_KOMFORTTEMPWW}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getWarmWaterComfortTempChannel() { return this.channel(ChannelId.HR1510_KOMFORTTEMPWW); }

    /**
     * Get the comfort temperature setting for warm water. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1510_KOMFORTTEMPWW}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getWarmWaterComfortTemp() {
		return this.getWarmWaterComfortTempChannel().value();
	}
	
	/**
     * Set the comfort temperature setting for warm water. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1510_KOMFORTTEMPWW}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setWarmWaterComfortTemp(Integer value) throws OpenemsNamedException {
		this.getWarmWaterComfortTempChannel().setNextWriteValue(value);
	}
    
    /**
     * Gets the Channel for {@link ChannelId#HR1511_ECOTEMPWW}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getWarmWaterEcoTempChannel() { return this.channel(ChannelId.HR1511_ECOTEMPWW); }

    /**
     * Get the ECO temperature setting for warm water. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1511_ECOTEMPWW}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getWarmWaterEcoTemp() {
		return this.getWarmWaterEcoTempChannel().value();
	}
	
	/**
     * Set the ECO temperature setting for warm water. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1511_ECOTEMPWW}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setWarmWaterEcoTemp(Integer value) throws OpenemsNamedException {
		this.getWarmWaterEcoTempChannel().setNextWriteValue(value);
	}
    
    /**
     * Gets the Channel for {@link ChannelId#HR1512_WARMWASSERSTUFEN}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getWarmWaterStageNumberChannel() { return this.channel(ChannelId.HR1512_WARMWASSERSTUFEN); }
    
    /**
     * Get the number of stages for warm water.
	 * See {@link ChannelId#HR1512_WARMWASSERSTUFEN}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getWarmWaterStageNumber() {
		return this.getWarmWaterStageNumberChannel().value();
	}
	
	/**
     * Set the Number of stages for warm water.
	 * See {@link ChannelId#HR1512_WARMWASSERSTUFEN}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setWarmWaterStageNumber(Integer value) throws OpenemsNamedException {
		this.getWarmWaterStageNumberChannel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#HR1513_BIVALENZTEMPERATURWW}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getBackupHeaterActivationTempWwChannel() {
        return this.channel(ChannelId.HR1513_BIVALENZTEMPERATURWW);
    }
    
    /**
     * Get the backup heater activation temperature of the warm water. Below this temperature the backup 
     * heater will activate, depending on heat demand. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1513_BIVALENZTEMPERATURWW}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBackupHeaterActivationTempWw() {
		return this.getBackupHeaterActivationTempWwChannel().value();
	}
	
	/**
     * Set the backup heater activation temperature of the warm water. Below this temperature the backup 
     * heater will activate, depending on heat demand. Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1513_BIVALENZTEMPERATURWW}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setBackupHeaterActivationTempWw(Integer value) throws OpenemsNamedException {
		this.getBackupHeaterActivationTempWwChannel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#HR1514_VORLAUFSOLLTEMPFLAECHENKUEHLUNG}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getSurfaceCoolingForwardTempChannel() {
        return this.channel(ChannelId.HR1514_VORLAUFSOLLTEMPFLAECHENKUEHLUNG);
    }
    
    /**
     * Get the cooling mode forward temperature setpoint (surface cooling). Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1514_VORLAUFSOLLTEMPFLAECHENKUEHLUNG}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSurfaceCoolingForwardTemp() {
		return this.getSurfaceCoolingForwardTempChannel().value();
	}
	
	/**
     * Set the cooling mode forward temperature setpoint (surface cooling). Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1514_VORLAUFSOLLTEMPFLAECHENKUEHLUNG}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSurfaceCoolingForwardTemp(Integer value) throws OpenemsNamedException {
		this.getSurfaceCoolingForwardTempChannel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#HR1515_HYSTERESEVORLAUFTEMPFLAECHENKUEHLUNG}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getSurfaceCoolingForwardTempHysteresisChannel() {
        return this.channel(ChannelId.HR1515_HYSTERESEVORLAUFTEMPFLAECHENKUEHLUNG);
    }
    
    /**
     * Get the cooling mode forward temperature hysteresis (surface cooling). Unit is dezidegree Kelvin.
	 * See {@link ChannelId#HR1515_HYSTERESEVORLAUFTEMPFLAECHENKUEHLUNG}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSurfaceCoolingForwardTempHysteresis() {
		return this.getSurfaceCoolingForwardTempHysteresisChannel().value();
	}
	
	/**
     * Set the cooling mode forward temperature hysteresis (surface cooling). Unit is dezidegree Kelvin.
	 * See {@link ChannelId#HR1515_HYSTERESEVORLAUFTEMPFLAECHENKUEHLUNG}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSurfaceCoolingForwardTempHysteresis(Integer value) throws OpenemsNamedException {
		this.getSurfaceCoolingForwardTempHysteresisChannel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#HR1516_RAUMSOLLTEMPFLAECHENKUEHLUNG}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getSurfaceCoolingRoomTempChannel() {
        return this.channel(ChannelId.HR1516_RAUMSOLLTEMPFLAECHENKUEHLUNG);
    }
    
    /**
     * Get the cooling mode forward temperature setpoint (surface cooling). Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1516_RAUMSOLLTEMPFLAECHENKUEHLUNG}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSurfaceCoolingRoomTemp() {
		return this.getSurfaceCoolingRoomTempChannel().value();
	}
	
	/**
     * Set the cooling mode forward temperature setpoint (surface cooling). Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1516_RAUMSOLLTEMPFLAECHENKUEHLUNG}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSurfaceCoolingRoomTemp(Integer value) throws OpenemsNamedException {
		this.getSurfaceCoolingRoomTempChannel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#HR1517_VORLAUFSOLLTEMPGEBLAESEKUEHLUNG}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getVentilationCoolingForwardTempChannel() {
        return this.channel(ChannelId.HR1517_VORLAUFSOLLTEMPGEBLAESEKUEHLUNG);
    }
    
    /**
     * Get the cooling mode forward temperature setpoint (fan cooling). Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1517_VORLAUFSOLLTEMPGEBLAESEKUEHLUNG}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVentilationCoolingForwardTemp() {
		return this.getVentilationCoolingForwardTempChannel().value();
	}
	
	/**
     * Set the cooling mode forward temperature setpoint (fan cooling). Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1517_VORLAUFSOLLTEMPGEBLAESEKUEHLUNG}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setVentilationCoolingForwardTemp(Integer value) throws OpenemsNamedException {
		this.getVentilationCoolingForwardTempChannel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#HR1518_HYSTERESEVORLAUFTEMPGEBLAESEKUEHLUNG}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getVentilationCoolingForwardTempHysteresisChannel() {
        return this.channel(ChannelId.HR1518_HYSTERESEVORLAUFTEMPGEBLAESEKUEHLUNG);
    }

    /**
     * Get the cooling mode forward temperature hysteresis (fan cooling). Unit is dezidegree Kelvin.
	 * See {@link ChannelId#HR1518_HYSTERESEVORLAUFTEMPGEBLAESEKUEHLUNG}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVentilationCoolingForwardTempHysteresis() {
		return this.getVentilationCoolingForwardTempHysteresisChannel().value();
	}
	
	/**
     * Set the cooling mode forward temperature hysteresis (fan cooling). Unit is dezidegree Kelvin.
	 * See {@link ChannelId#HR1518_HYSTERESEVORLAUFTEMPGEBLAESEKUEHLUNG}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setVentilationCoolingForwardTempHysteresis(Integer value) throws OpenemsNamedException {
		this.getVentilationCoolingForwardTempHysteresisChannel().setNextWriteValue(value);
	}
    
    /**
     * Gets the Channel for {@link ChannelId#HR1519_RAUMSOLLTEMPGEBLAESEKUEHLUNG}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getVentilationCoolingRoomTempChannel() {
        return this.channel(ChannelId.HR1519_RAUMSOLLTEMPGEBLAESEKUEHLUNG);
    }

    /**
     * Get the cooling mode forward temperature setpoint (fan cooling). Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1519_RAUMSOLLTEMPGEBLAESEKUEHLUNG}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVentilationCoolingRoomTemp() {
		return this.getVentilationCoolingRoomTempChannel().value();
	}
	
	/**
     * Set the cooling mode forward temperature setpoint (fan cooling). Unit is dezidegree Celsius.
	 * See {@link ChannelId#HR1519_RAUMSOLLTEMPGEBLAESEKUEHLUNG}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setVentilationCoolingRoomTemp(Integer value) throws OpenemsNamedException {
		this.getVentilationCoolingRoomTempChannel().setNextWriteValue(value);
	}
    
    /**
     * Gets the Channel for {@link ChannelId#HR1520_RESET}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getResetChannel() { return this.channel(ChannelId.HR1520_RESET); }
    
    /**
     * Get reset function.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 1 ... 3
     *      <li> State 1: Reset System
     *      <li> State 2: Reset Error
     *      <li> State 3: Reset Heatpump
     * </ul>
	 * See {@link ChannelId#HR1520_RESET}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getReset() {
		return this.getResetChannel().value();
	}
	
	/**
     * Set reset function.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 1 ... 3
     *      <li> State 1: Reset System
     *      <li> State 2: Reset Error
     *      <li> State 3: Reset Heatpump
     * </ul>
	 * See {@link ChannelId#HR1520_RESET}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setReset(Integer value) throws OpenemsNamedException {
		this.getResetChannel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#HR1521_RESTART_ISG}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getRestartIsgChannel() { return this.channel(ChannelId.HR1521_RESTART_ISG); }
    
    /**
     * Get "restart internet service gateway (ISG)" function.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 2
     *      <li> State 0: Off
     *      <li> State 1: Restart
     *      <li> State 2: Service button
     * </ul>
	 * See {@link ChannelId#HR1521_RESTART_ISG}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getRestartIsg() {
		return this.getRestartIsgChannel().value();
	}
	
	/**
     * Set "restart internet service gateway (ISG)" function.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 2
     *      <li> State 0: Off
     *      <li> State 1: Restart
     *      <li> State 2: Service button
     * </ul>
	 * See {@link ChannelId#HR1521_RESTART_ISG}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setRestartIsg(Integer value) throws OpenemsNamedException {
		this.getRestartIsgChannel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#HR4001_SGREADY_ONOFF}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getSgReadyOnOffChannel() { return this.channel(ChannelId.HR4001_SGREADY_ONOFF); }

    /**
     * Get on/off state of SG-Ready capabilities. True = on, false = off. Will automatically be set to "true" when
     * using setSmartGridState() of HeatpumpSmartGridGeneralizedChannel interface.
	 * See {@link ChannelId#HR4001_SGREADY_ONOFF}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getSgReadyOnOff() {
		return this.getSgReadyOnOffChannel().value();
	}
	
	/**
     * Turn on/off SG-Ready capabilities. True = on, false = off. Will automatically be set to "true" when using
     * setSmartGridState() of HeatpumpSmartGridGeneralizedChannel interface.
	 * See {@link ChannelId#HR4001_SGREADY_ONOFF}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSgReadyOnOff(Boolean value) throws OpenemsNamedException {
		this.getSgReadyOnOffChannel().setNextWriteValue(value);
	}
    
    /**
     * Gets the Channel for {@link ChannelId#HR4002_SGREADY_INPUT1}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getSgReadyInput1Channel() { return this.channel(ChannelId.HR4002_SGREADY_INPUT1); }

    /**
     * Gets the Channel for {@link ChannelId#HR4003_SGREADY_INPUT2}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getSgReadyInput2Channel() { return this.channel(ChannelId.HR4003_SGREADY_INPUT2); }


}
