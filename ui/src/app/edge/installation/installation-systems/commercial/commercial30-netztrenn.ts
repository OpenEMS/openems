import { Edge, EdgeConfig, Websocket } from 'src/app/shared/shared';
import { ComponentData } from 'src/app/shared/type/componentData';
import { ComponentConfigurator, ConfigurationMode } from '../../views/configuration-execute/component-configurator';
import { AbstractCommercialIbn } from './abstract-commercial';
import { View } from '../abstract-ibn';

export class Commercial30NetztrennIbn extends AbstractCommercialIbn {

    public readonly type = 'Fenceon-Commercial-30';

    public readonly id = 'commercial-30-netztrennstelle';

    public readonly imageUrl = 'assets/img/Commercial-30_label.PNG';

    public readonly manualLink = 'https://fenecon.de/download/12-montage-und-serviceanleitung-commercial-30/';

    constructor() {
        super([
            View.PreInstallation,
            View.ConfigurationSystem,
            View.ConfigurationCommercialComponent,
            View.ProtocolInstaller,
            View.ProtocolCustomer,
            View.ProtocolSystem,
            View.ConfigurationEmergencyReserve,
            View.ConfigurationLineSideMeterFuse,
            View.ProtocolAdditionalAcProducers,
            View.ProtocolFeedInLimitation,
            View.ConfigurationSummary,
            View.ConfigurationExecute,
            View.ProtocolSerialNumbers,
            View.Completion
        ]);
    }

    public addCustomBatteryData(batteryData: ComponentData[]) {
        batteryData.push({
            label: 'Notstromfunktion aktiviert?',
            value: this.emergencyReserve.isEnabled ? 'ja' : 'nein',
        });

        if (this.emergencyReserve.isEnabled) {
            batteryData.push({
                label: 'Notstromfunktion Wert',
                value: this.emergencyReserve.value,
            });
        }
        return batteryData;
    }

    public setRequiredControllers() {
        let requiredControllerIds: string[];
        if (this.emergencyReserve.isEnabled) {
            requiredControllerIds = [
                'ctrlEmergencyCapacityReserve0',
                'ctrlBalancing0',
            ];
        } else {
            requiredControllerIds = [
                'ctrlBalancing0',
            ];
        }
        this.requiredControllerIds = requiredControllerIds;
    }

    public getComponentConfigurator(edge: Edge, config: EdgeConfig, websocket: Websocket): ComponentConfigurator {
        const componentConfigurator: ComponentConfigurator = new ComponentConfigurator(edge, config, websocket);

        // modbus0
        componentConfigurator.add({
            factoryId: 'Bridge.Modbus.Serial',
            componentId: 'modbus0',
            alias: 'Kommunikation mit der Batterie',
            properties: [
                { name: 'enabled', value: true },
                { name: 'portName', value: '/dev/ttyAMA0' }, // TODO: Check if this could be changed to default '/dev/ttyAMA0'
                { name: 'baudRate', value: 9600 }, // TODO: Check if this schould be changed to 57600
                { name: 'databits', value: 8 },
                { name: 'stopbits', value: 'ONE' },
                { name: 'parity', value: 'NONE' },
                { name: 'logVerbosity', value: 'NONE' },
                { name: 'invalidateElementsAfterReadErrors', value: 3 }
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        });

        // modbus1
        componentConfigurator.add({
            factoryId: 'Bridge.Modbus.Tcp',
            componentId: 'modbus1',
            alias: 'Kommunikation mit dem Batterie-Wechselrichter',
            properties: [
                { name: 'enabled', value: true },
                { name: 'ip', value: '192.168.1.11' }, // TODO: Change it to 192.168.1.11 !!!!!
                { name: 'port', value: '502' },
                { name: 'logVerbosity', value: 'NONE' },
                { name: 'invalidateElementsAfterReadErrors', value: 3 }
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        });

        //modbus2
        componentConfigurator.add({
            factoryId: 'Bridge.Modbus.Serial',
            componentId: 'modbus2',
            alias: 'Kommunikation mit den Zählern',
            properties: [
                { name: 'enabled', value: true },
                { name: 'portName', value: '/dev/ttySC0' },
                { name: 'baudRate', value: 9600 },
                { name: 'databits', value: 8 },
                { name: 'stopbits', value: 'ONE' },
                { name: 'parity', value: 'NONE' },
                { name: 'logVerbosity', value: 'NONE' },
                { name: 'invalidateElementsAfterReadErrors', value: 3 }
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        });

        //modbus3
        componentConfigurator.add({
            factoryId: 'Bridge.Modbus.Tcp',
            componentId: 'modbus3',
            alias: 'Wago Bridge',
            properties: [
                { name: 'enabled', value: true },
                { name: 'ip', value: '192.168.1.50' },
                { name: 'port', value: '502' },
                { name: 'logVerbosity', value: 'NONE' },
                { name: 'invalidateElementsAfterReadErrors', value: 3 }
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        });

        // io0
        componentConfigurator.add({
            factoryId: 'IO.KMtronic',
            componentId: 'io0',
            alias: 'Relaisboard',
            properties: [
                { name: 'enabled', value: true },
                { name: 'modbus.id', value: 'modbus0' },
                { name: 'modbusUnitId', value: 6 }
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        });

        // io1
        componentConfigurator.add({
            factoryId: 'IO.WAGO',
            componentId: 'io1',
            alias: 'WAGO feldbuskoppler',
            properties: [
                { name: 'enabled', value: true },
                { name: 'modbus.id', value: 'modbus3' },
                { name: 'username', value: 'admin' },
                { name: 'password', value: 'wago' }
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        });

        // offGridSwitch0
        componentConfigurator.add({
            factoryId: 'Io.Off.Grid.Switch',
            componentId: 'offGridSwitch0',
            alias: 'Ansteuerung der Netztrennstelle',
            properties: [
                { name: 'enabled', value: true },
                { name: 'inputGridStatus', value: 'io1/DigitalInputM1C2' },
                { name: 'inputGroundingContactor', value: 'io1/DigitalInputM1C3' },
                { name: 'inputMainContactor', value: 'io1/DigitalInputM1C1' },
                { name: 'outputGroundingContactor', value: 'io1/DigitalOutputM1C2' },
                { name: 'outputMainContactor', value: 'io1/DigitalOutputM1C1' }
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        });

        // meter0
        componentConfigurator.add({
            factoryId: 'Meter.Socomec.Threephase',
            componentId: 'meter0',
            alias: 'Netz',
            properties: [
                { name: 'enabled', value: true },
                { name: 'modbus.id', value: 'modbus2' },
                { name: 'type', value: 'GRID' },
                { name: 'modbusUnitId', value: 5 }
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        });

        // meter1
        componentConfigurator.add({
            factoryId: 'Meter.Socomec.Threephase',
            componentId: 'meter1',
            alias: 'Produktion',
            properties: [
                { name: 'enabled', value: true },
                { name: 'modbus.id', value: 'modbus2' },
                { name: 'type', value: 'PRODUCTION' },
                { name: 'modbusUnitId', value: 6 }
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        });

        // battery0
        componentConfigurator.add({
            factoryId: 'Battery.Fenecon.Commercial',
            componentId: 'battery0',
            alias: 'Batterie',
            properties: [
                { name: 'enabled', value: true },
                { name: 'startStop', value: 'AUTO' },
                { name: 'modbus.id', value: 'modbus0' },
                { name: 'batteryStartStopRelay', value: 'io0/Relay8' },
                { name: 'modbusUnitId', value: 1 }
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        });

        // batteryInverter0
        componentConfigurator.add({
            factoryId: 'Battery-Inverter.Sinexcel',
            componentId: 'batteryInverter0',
            alias: 'Batterie-Wechselrichter',
            properties: [
                { name: 'enabled', value: true },
                { name: 'modbus.id', value: 'modbus1' },
                { name: 'startStop', value: 'AUTO' },
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        });

        // Optional meter2 - aditional AC PV
        const acArray = this.pv.ac;
        const isAcCreated: boolean = acArray.length >= 1;

        const acAlias = isAcCreated ? acArray[0].alias : '';
        const acModbusUnitId = isAcCreated ? acArray[0].modbusCommunicationAddress : 0;

        componentConfigurator.add({
            factoryId: 'Meter.Socomec.Threephase',
            componentId: 'meter2',
            alias: acAlias,
            properties: [
                { name: 'enabled', value: true },
                { name: 'type', value: 'PRODUCTION' },
                { name: 'modbus.id', value: 'modbus2' },
                { name: 'modbusUnitId', value: acModbusUnitId },
                { name: 'invert', value: false }
            ],
            mode: isAcCreated ? ConfigurationMode.RemoveAndConfigure : ConfigurationMode.RemoveOnly
        });

        // ess0
        componentConfigurator.add({
            factoryId: 'Ess.Generic.OffGrid',
            componentId: 'ess0',
            alias: 'Speichersystem',
            properties: [
                { name: 'enabled', value: true },
                { name: 'startStop', value: 'START' },
                { name: 'batteryInverter.id', value: 'batteryInverter0' },
                { name: 'offGridSwitch.id', value: 'offGridSwitch0' },
                { name: 'battery.id', value: 'battery0' }
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        });

        // ctrlBalancing0
        componentConfigurator.add({
            factoryId: 'Controller.Symmetric.Balancing',
            componentId: 'ctrlBalancing0',
            alias: 'Eigenverbrauchsoptimierung',
            properties: [
                { name: 'enabled', value: true },
                { name: 'ess.id', value: 'ess0' },
                { name: 'meter.id', value: 'meter0' },
                { name: 'targetGridSetpoint', value: 0 }
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        });

        return componentConfigurator;
    }
}
