import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { JsonrpcResponseSuccess } from 'src/app/shared/jsonrpc/base';
import { SetupProtocol, SubmitSetupProtocolRequest } from 'src/app/shared/jsonrpc/request/submitSetupProtocolRequest';
import { Edge, EdgeConfig, Websocket } from 'src/app/shared/shared';
import { environment } from 'src/environments';
import { Category } from '../../../shared/category';
import { FeedInType } from '../../../shared/enums';
import { ComponentData } from '../../../shared/ibndatatypes';
import { ComponentConfigurator, ConfigurationMode } from '../../../views/configuration-execute/component-configurator';
import { SchedulerIdBehaviour, View } from '../../abstract-ibn';
import { AbstractCommercial30Ibn } from './abstract-commercial-30';

export class Commercial30NetztrennIbn extends AbstractCommercial30Ibn {

    public readonly type: string = 'Fenceon-Commercial-30';

    public readonly id: string = 'commercial-30-netztrennstelle';

    constructor(translate: TranslateService) {
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
        ], translate);
    }

    public addCustomBatteryData(batteryData: ComponentData[]) {
        batteryData.push({
            label: this.translate.instant('INSTALLATION.CONFIGURATION_EMERGENCY_RESERVE.IS_ACTIVATED'),
            value: this.emergencyReserve.isEnabled ? this.translate.instant('General.yes') : this.translate.instant('General.no'),
        });

        if (this.emergencyReserve.isEnabled) {
            batteryData.push({
                label: this.translate.instant('INSTALLATION.CONFIGURATION_EMERGENCY_RESERVE.EMERGENCY_RESERVE_VALUE'),
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
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.COMMUNICATION_WITH_BATTERY'),
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
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.COMMUNICATION_WITH_BATTERY_INVERTER'),
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
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.COMMUNICATION_WITH_METER'),
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
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.WAGO_BRIDGE'),
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
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.RELAY_BOARD'),
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
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.FIELD_BUS_COUPLER'),
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
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.CONTROL_GRID_POINT'),
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
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.GRID_METER'),
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
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.BATTERY'),
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
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.BATTERY_INVERTER'),
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
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.STORAGE_SYSTEM'),
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
                alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.GRID_OPTIMIZED_CHARGE'),
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
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.EMERGENCY_CAPACITY_RESERVE'),
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
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.SELF_CONSUMPTION'),
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
            name: this.translate.instant('INSTALLATION.CONFIGURATION_EMERGENCY_RESERVE.EMERGENCY_RESERVE', { symbol: '?' }),
            value: emergencyReserve.isEnabled ? this.translate.instant('General.yes') : this.translate.instant('General.no'),
        });

        if (emergencyReserve.isEnabled) {
            protocol.items.push({
                category: Category.EMERGENCY_RESERVE,
                name: this.translate.instant('INSTALLATION.CONFIGURATION_EMERGENCY_RESERVE.EMERGENCY_RESERVE', { symbol: '[%]' }),
                value: emergencyReserve.value ? emergencyReserve.value.toString() : '',
            });
        }

        let lineSideMeterFuseValue: number;
        lineSideMeterFuseValue = lineSideMeterFuse.otherValue;

        protocol.items.push({
            category: this.lineSideMeterFuse.category,
            name: this.translate.instant('INSTALLATION.CONFIGURATION_LINE_SIDE_METER_FUSE.VALUE'),
            value: lineSideMeterFuseValue ? lineSideMeterFuseValue.toString() : '',
        });

        const feedInLimitation = this.feedInLimitation;
        protocol.items.push(
            {
                category: Category.FEED_IN_MANAGEMENT,
                name: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.EXTERNAL_CONTROLLER_RECIEVER'),
                value: feedInLimitation.feedInType == FeedInType.EXTERNAL_LIMITATION
                    ? this.translate.instant('General.yes')
                    : this.translate.instant('General.no')
            },
            {
                category: Category.FEED_IN_MANAGEMENT,
                name: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.DYNAMIC_LIMITATION'),
                value: feedInLimitation.feedInType == FeedInType.DYNAMIC_LIMITATION
                    ? this.translate.instant('General.yes')
                    : this.translate.instant('General.no')
            });

        if (feedInLimitation.feedInType == FeedInType.DYNAMIC_LIMITATION) {
            protocol.items.push({
                category: Category.FEED_IN_MANAGEMENT,
                name: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.MAXIMUM_FEED_IN_VALUE'),
                value: feedInLimitation.maximumFeedInPower
                    ? feedInLimitation.maximumFeedInPower.toString()
                    : (0).toString(),
            });
        }

        const additionalAcCategory: Category = Category.ADDITIONAL_AC_PRODUCERS
        for (let index = 0; index < ac.length; index++) {
            const element = ac[index];
            const label = 'AC';
            const acNr = (index + 1);

            protocol.items.push(
                {
                    category: additionalAcCategory,
                    name: this.translate.instant('INSTALLATION.ALIAS_WITH_LABEL', { label: label, number: acNr }),
                    value: element.alias,
                },
                {
                    category: additionalAcCategory,
                    name: this.translate.instant('INSTALLATION.VALUE_WITH_LABEL', { label: label, number: acNr, symbol: '[Wp]' }),
                    value: element.value ? element.value.toString() : '',
                });

            element.orientation && protocol.items.push({
                category: additionalAcCategory,
                name: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.ORIENTATION_WITH_LABEL', { label: label, number: acNr }),
                value: element.orientation,
            });

            element.moduleType && protocol.items.push({
                category: additionalAcCategory,
                name: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.MODULE_TYPE_WITH_LABEL', { label: label, number: acNr }),
                value: element.moduleType,
            });

            element.modulesPerString && protocol.items.push({
                category: additionalAcCategory,
                name: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.NUMBER_OF_MODULES_WITH_LABEL', { label: label, number: acNr }),
                value: element.modulesPerString
                    ? element.modulesPerString.toString()
                    : '',
            });

            element.meterType && protocol.items.push({
                category: additionalAcCategory,
                name: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.METER_TYPE_WITH_LABEL', { label: label, number: acNr }),
                value: element.meterType,
            });

            element.modbusCommunicationAddress && protocol.items.push({
                category: additionalAcCategory,
                name: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.MODBUS_WITH_LABEL', { label: label, number: acNr }),
                value: element.modbusCommunicationAddress
                    ? element.modbusCommunicationAddress.toString()
                    : '',
            });
        }

        protocol.items.push({
            category: Category.EMS_DETAILS,
            name: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.EDGE_NUMBER', { edgeShortName: environment.edgeShortName }),
            value: edge.id
        });

        protocol = this.getProtocolSerialNumbers(protocol);

        return new Promise((resolve, reject) => {
            websocket.sendRequest(SubmitSetupProtocolRequest.translateFrom(protocol, this.translate)).then((response: JsonrpcResponseSuccess) => {
                resolve(response.result['setupProtocolId']);
            }).catch((reason) => {
                reject(reason);
            });
        });
    }

    public getFields(stringNr: number, numberOfModulesPerString: number) {

        const fields: FormlyFieldConfig[] = this.getCommercial30SerialNumbersFields(stringNr, numberOfModulesPerString);

        if (stringNr === 0) {

            // Adds the ems box field only for Initial String.
            const emsbox: FormlyFieldConfig = {
                key: 'emsbox',
                type: 'input',
                templateOptions: {
                    label: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.EMS_BOX_GRID_CONNECTION_POINT_COMMERCIAL30', { edgeShortName: environment.edgeShortName }),
                    required: true,
                    placeholder: 'xxxx'
                },
                validators: {
                    validation: ['emsBoxNetztrennstelleSerialNumber']
                },
                wrappers: ['input-serial-number']
            }

            // ems box field is added at a specific position in array, because it is always displayed at specific position in UI.
            fields.splice(1, 0, emsbox);
        }

        return fields;
    }
}
