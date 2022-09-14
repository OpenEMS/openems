import { JsonrpcResponseSuccess } from 'src/app/shared/jsonrpc/base';
import { SetupProtocol, SubmitSetupProtocolRequest } from 'src/app/shared/jsonrpc/request/submitSetupProtocolRequest';
import { Edge, EdgeConfig, Websocket } from 'src/app/shared/shared';
import { environment } from 'src/environments';
import { Category, FeedInType } from '../../../shared/enums';
import { ComponentData } from '../../../shared/ibndatatypes';
import { ComponentConfigurator, ConfigurationMode } from '../../../views/configuration-execute/component-configurator';
import { SchedulerIdBehaviour, View } from '../../abstract-ibn';
import { AbstractCommercial30Ibn } from './abstract-commercial-30';

export class Commercial30NetztrennIbn extends AbstractCommercial30Ibn {

    public readonly type: string = 'Fenceon-Commercial-30';

    public readonly id: string = 'commercial-30-netztrennstelle';

    constructor() {
        super([
            View.PreInstallation,
            View.PreInstallationUpdate,
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
        this.requiredControllerIds = [];
        if (this.emergencyReserve.isEnabled) {
            this.requiredControllerIds.push({
                componentId: "ctrlEmergencyCapacityReserve0"
                , behaviour: SchedulerIdBehaviour.ALWAYS_INCLUDE
            });
        }
        if (this.feedInLimitation.feedInType === FeedInType.DYNAMIC_LIMITATION) {
            this.requiredControllerIds.push({
                componentId: "ctrlGridOptimizedCharge0"
                , behaviour: SchedulerIdBehaviour.ALWAYS_INCLUDE
            });
        }
        this.requiredControllerIds.push({
            componentId: "ctrlBalancing0"
            , behaviour: SchedulerIdBehaviour.ALWAYS_INCLUDE
        });
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
            componentId: 'meter1',
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

        if (this.feedInLimitation.feedInType === FeedInType.DYNAMIC_LIMITATION) {
            // ctrlGridOptimizedCharge0
            componentConfigurator.add({
                factoryId: 'Controller.Ess.GridOptimizedCharge',
                componentId: 'ctrlGridOptimizedCharge0',
                alias: 'Netzdienliche Beladung',
                properties: [
                    { name: 'enabled', value: true },
                    { name: 'ess.id', value: 'ess0' },
                    { name: 'meter.id', value: 'meter0' },
                    { name: 'sellToGridLimitEnabled', value: true },
                    {
                        name: 'maximumSellToGridPower',
                        value: this.feedInLimitation.maximumFeedInPower,
                    },
                    { name: 'delayChargeRiskLevel', value: 'MEDIUM' },
                    { name: 'mode', value: 'AUTOMATIC' },
                    { name: 'manualTargetTime', value: '17:00' },
                    { name: 'debugMode', value: false },
                    { name: 'sellToGridLimitRampPercentage', value: 2 },
                ],
                mode: ConfigurationMode.RemoveAndConfigure,
            });
        }

        //Emergency reserve
        componentConfigurator.add({
            factoryId: 'Controller.Ess.EmergencyCapacityReserve',
            componentId: 'ctrlEmergencyCapacityReserve0',
            alias: 'Ansteuerung der Notstromreserve',
            properties: [
                { name: 'enabled', value: true },
                { name: 'ess.id', value: 'ess0' },
                { name: 'isReserveSocEnabled', value: this.emergencyReserve.isReserveSocEnabled },
                {
                    name: 'reserveSoc',
                    value: this.emergencyReserve.value ?? 5 /* minimum allowed value */,
                },
            ],
            mode: this.emergencyReserve.isEnabled
                ? ConfigurationMode.RemoveAndConfigure
                : ConfigurationMode.RemoveOnly,
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

    public getProtocol(edge: Edge, websocket: Websocket): Promise<string> {

        const installer = this.installer;
        const customer = this.customer;
        const pv = this.pv;
        const lineSideMeterFuse = this.lineSideMeterFuse;
        const ac = pv.ac;

        const installerObj: any = {
            firstname: installer.firstName,
            lastname: installer.lastName
        };

        const customerObj: any = {
            firstname: customer.firstName,
            lastname: customer.lastName,
            email: customer.email,
            phone: customer.phone,
            address: {
                street: customer.street,
                city: customer.city,
                zip: customer.zip,
                country: customer.country
            }
        };

        if (customer.isCorporateClient) {
            customerObj.company = {
                name: customer.companyName
            };
        }

        let protocol: SetupProtocol = {
            fems: {
                id: edge.id
            },
            installer: installerObj,
            customer: customerObj,
            oem: environment.theme
        };

        // If location data is different to customer data, the location
        // data gets sent too
        if (!this.location.isEqualToCustomerData) {
            const location = this.location;

            protocol.location = {
                firstname: location.firstName,
                lastname: location.lastName,
                email: location.email,
                phone: location.phone,
                address: {
                    street: location.street,
                    city: location.city,
                    zip: location.zip,
                    country: location.country
                },
                company: {
                    name: location.companyName
                }
            };
        }

        protocol.items = [];

        const emergencyReserve = this.emergencyReserve;
        protocol.items.push({
            category: Category.EMERGENCY_RESERVE,
            name: 'Notstrom?',
            value: emergencyReserve.isEnabled ? 'ja' : 'nein',
        });

        if (emergencyReserve.isEnabled) {
            protocol.items.push({
                category: Category.EMERGENCY_RESERVE,
                name: 'Notstromreserve [%]',
                value: emergencyReserve.value ? emergencyReserve.value.toString() : '',
            });
        }

        let lineSideMeterFuseValue: number;
        lineSideMeterFuseValue = lineSideMeterFuse.otherValue;

        protocol.items.push({
            category: this.lineSideMeterFuseTitle,
            name: 'Wert [A]',
            value: lineSideMeterFuseValue ? lineSideMeterFuseValue.toString() : '',
        });

        const feedInLimitation = this.feedInLimitation;
        protocol.items.push(
            {
                category: Category.FEED_IN_MANAGEMENT,
                name: 'Rundsteuerempfänger',
                value: feedInLimitation.feedInType == FeedInType.EXTERNAL_LIMITATION
                    ? "ja"
                    : "nein"
            },
            {
                category: Category.FEED_IN_MANAGEMENT,
                name: 'Netzdienliche Beladung (z.B. 70% Abregelung)',
                value: feedInLimitation.feedInType == FeedInType.DYNAMIC_LIMITATION
                    ? "ja"
                    : "nein"
            });

        if (feedInLimitation.feedInType == FeedInType.DYNAMIC_LIMITATION) {
            protocol.items.push({
                category: Category.FEED_IN_MANAGEMENT,
                name: 'Maximale Einspeiseleistung [W]',
                value: feedInLimitation.maximumFeedInPower
                    ? feedInLimitation.maximumFeedInPower.toString()
                    : (0).toString(),
            });
        }

        for (let index = 0; index < ac.length; index++) {
            const element = ac[index];
            const label = 'AC' + (index + 1);

            protocol.items.push(
                {
                    category: Category.ADDITIONAL_AC_PRODUCERS,
                    name: 'Alias ' + label,
                    value: element.alias,
                },
                {
                    category: Category.ADDITIONAL_AC_PRODUCERS,
                    name: 'Wert ' + label + ' [Wp]',
                    value: element.value ? element.value.toString() : '',
                });

            element.orientation && protocol.items.push({
                category: Category.ADDITIONAL_AC_PRODUCERS,
                name: 'Ausrichtung ' + label,
                value: element.orientation,
            });

            element.moduleType && protocol.items.push({
                category: Category.ADDITIONAL_AC_PRODUCERS,
                name: 'Modultyp ' + label,
                value: element.moduleType,
            });

            element.modulesPerString && protocol.items.push({
                category: Category.ADDITIONAL_AC_PRODUCERS,
                name: 'Anzahl PV-Module ' + label,
                value: element.modulesPerString
                    ? element.modulesPerString.toString()
                    : '',
            });

            element.meterType && protocol.items.push({
                category: Category.ADDITIONAL_AC_PRODUCERS,
                name: 'Zählertyp ' + label,
                value: element.meterType,
            });

            element.modbusCommunicationAddress && protocol.items.push({
                category: Category.ADDITIONAL_AC_PRODUCERS,
                name: 'Modbus Kommunikationsadresse ' + label,
                value: element.modbusCommunicationAddress
                    ? element.modbusCommunicationAddress.toString()
                    : '',
            });
        }

        protocol.items.push({
            category: Category.FEMS_DETAILS,
            name: 'FEMS Nummer',
            value: edge.id
        });

        protocol = this.getProtocolSerialNumbers(protocol);

        return new Promise((resolve, reject) => {
            websocket.sendRequest(new SubmitSetupProtocolRequest({ protocol })).then((response: JsonrpcResponseSuccess) => {
                resolve(response.result['setupProtocolId']);
            }).catch((reason) => {
                reject(reason);
            });
        });
    }
}
