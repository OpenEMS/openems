import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { JsonrpcResponseSuccess } from 'src/app/shared/jsonrpc/base';
import { SetupProtocol, SubmitSetupProtocolRequest } from 'src/app/shared/jsonrpc/request/submitSetupProtocolRequest';
import { Edge, EdgeConfig, Websocket } from 'src/app/shared/shared';
import { environment } from 'src/environments';
import { Category } from '../../../shared/category';
import { FeedInType } from '../../../shared/enums';
import { ComponentData } from '../../../shared/ibndatatypes';
import { Meter } from '../../../shared/meter';
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
            View.ConfigurationCommercialModbuBridgeComponent,
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
        const invalidateElementsAfterReadErrors: number = 3;
        const componentConfigurator: ComponentConfigurator = super.getCommercial30ComponentConfigurator(edge, config, websocket, invalidateElementsAfterReadErrors);

        //modbus3
        componentConfigurator.add({
            factoryId: 'Bridge.Modbus.Tcp',
            componentId: 'modbus3',
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.WEIDMUELLER_BRIDGE'),
            properties: [
                { name: 'enabled', value: true },
                { name: 'ip', value: '192.168.1.50' },
                { name: 'port', value: '502' },
                { name: 'logVerbosity', value: 'NONE' },
                { name: 'invalidateElementsAfterReadErrors', value: 3 }
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        }, 3);

        // io1
        componentConfigurator.add({
            factoryId: 'IO.Weidmueller.UR20',
            componentId: 'io1',
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.FIELD_BUS_COUPLER'),
            properties: [
                { name: 'enabled', value: true },
                { name: 'modbus.id', value: 'modbus3' },
                { name: 'modbusUnitId', value: 1 },
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        }, 5);

        // offGridSwitch0
        componentConfigurator.add({
            factoryId: 'Io.Off.Grid.Switch',
            componentId: 'offGridSwitch0',
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.CONTROL_GRID_POINT'),
            properties: [
                { name: 'enabled', value: true },
                { name: 'inputGridStatus', value: 'io1/DigitalInputM0C2' },
                { name: 'inputGroundingContactor', value: 'io1/DigitalInputM0C3' },
                { name: 'inputMainContactor', value: 'io1/DigitalInputM0C1' },
                { name: 'outputGroundingContactor', value: 'io1/DigitalOutputM0C2' },
                { name: 'outputMainContactor', value: 'io1/DigitalOutputM0C1' }
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        }, 6);

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
        }, 11);

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
        }, 12);

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
                value: Meter.toLabelString(element.meterType),
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
