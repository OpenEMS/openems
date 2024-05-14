// @ts-strict-ignore
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { JsonrpcResponseSuccess } from 'src/app/shared/jsonrpc/base';
import { SetupProtocol, SubmitSetupProtocolRequest } from 'src/app/shared/jsonrpc/request/submitSetupProtocolRequest';
import { Edge, EdgeConfig, Service, Websocket } from 'src/app/shared/shared';

import { Category } from '../../../shared/category';
import { Coupler } from '../../../shared/coupler';
import { FeedInType, ModbusBridgeType, View } from '../../../shared/enums';
import { ComponentData } from '../../../shared/ibndatatypes';
import { IbnUtils } from '../../../shared/ibnutils';
import { SystemId } from '../../../shared/system';
import { ComponentConfigurator, ConfigurationMode } from '../../../views/configuration-execute/component-configurator';
import { SchedulerIdBehaviour } from '../../abstract-ibn';
import { AbstractCommercial30Ibn } from './abstract-commercial-30';

export class Commercial30NetztrennIbn extends AbstractCommercial30Ibn {

    public override readonly id: SystemId = SystemId.COMMERCIAL_30_NETZTRENNSTELLE;
    public override readonly emergencyPower = 'ENABLE';

    // configuration-emergency-reserve
    public override emergencyReserve?: {
        isEnabled: boolean;
        isReserveSocEnabled: boolean;
        minValue: number;
        value: number;
        coupler: Coupler
    } = {
            isEnabled: true,
            minValue: 15,
            value: 20,
            isReserveSocEnabled: false,
            coupler: Coupler.WAGO,
        };

    constructor(translate: TranslateService) {
        super([
            View.PreInstallation,
            View.PreInstallationUpdate,
            View.ConfigurationSystem,
            View.ConfigurationSubSystem,
            View.ConfigurationSystemVariant,
            View.ProtocolInstaller,
            View.ProtocolCustomer,
            View.ProtocolSystem,
            View.ConfigurationEmergencyReserve,
            View.ConfigurationLineSideMeterFuse,
            View.ConfigurationCommercialModbuBridge,
            View.ProtocolFeedInLimitation,
            View.ConfigurationSummary,
            View.ConfigurationExecute,
            View.ProtocolSerialNumbers,
            View.Completion,
        ], translate);
    }

    public override addCustomBatteryData() {
        const batteryData: ComponentData[] = [];
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
                , behaviour: SchedulerIdBehaviour.ALWAYS_INCLUDE,
            });
        }
        if (this.feedInLimitation.feedInType === FeedInType.DYNAMIC_LIMITATION) {
            this.requiredControllerIds.push({
                componentId: "ctrlGridOptimizedCharge0"
                , behaviour: SchedulerIdBehaviour.ALWAYS_INCLUDE,
            });
        }
        this.requiredControllerIds.push({
            componentId: "ctrlBalancing0"
            , behaviour: SchedulerIdBehaviour.ALWAYS_INCLUDE,
        });
    }

    public getComponentConfigurator(edge: Edge, config: EdgeConfig, websocket: Websocket, service: Service): ComponentConfigurator {
        const invalidateElementsAfterReadErrors: number = 3;
        const componentConfigurator: ComponentConfigurator = super.getCommercial30ComponentConfigurator(edge, config, websocket, invalidateElementsAfterReadErrors, service);

        // Modbus bridge type will already have modbus3 reserved for io0.
        const couplerComponentId: string = this.modbusBridgeType === ModbusBridgeType.TCP_IP ? 'modbus4' : 'modbus3';

        // modbus3/modbus4
        componentConfigurator.add({
            factoryId: 'Bridge.Modbus.Tcp',
            componentId: couplerComponentId,
            alias: Coupler.toAliasString(this.emergencyReserve.coupler, this.translate),
            properties: [
                { name: 'enabled', value: true },
                { name: 'ip', value: '192.168.1.50' },
                { name: 'port', value: '502' },
                { name: 'logVerbosity', value: 'NONE' },
                { name: 'invalidateElementsAfterReadErrors', value: 3 },
            ],
            mode: ConfigurationMode.RemoveAndConfigure,
        }, 3);

        // Add ip address to network configuration to communicate with the Coupler.
        if (!IbnUtils.addIpAddress('eth1', '192.168.1.49/30', edge, websocket)) {
            service.toast(this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.IP_ADDRESS_WARNING'), 'danger');
        }

        // io1
        switch (this.emergencyReserve.coupler) {
            case Coupler.WEIDMUELLER:
                componentConfigurator.add({
                    factoryId: Coupler.toFactoryId(this.emergencyReserve.coupler),
                    componentId: 'io1',
                    alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.FIELD_BUS_COUPLER'),
                    properties: [
                        { name: 'enabled', value: true },
                        { name: 'modbus.id', value: couplerComponentId },
                        { name: 'modbusUnitId', value: 1 },
                    ],
                    mode: ConfigurationMode.RemoveAndConfigure,
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
                        { name: 'outputGroundingContactor', value: 'io1/DigitalOutputM1C2' },
                        { name: 'outputMainContactor', value: 'io1/DigitalOutputM1C1' },
                    ],
                    mode: ConfigurationMode.RemoveAndConfigure,
                }, 6);
                break;
            case Coupler.WAGO:
                componentConfigurator.add({
                    factoryId: Coupler.toFactoryId(this.emergencyReserve.coupler),
                    componentId: 'io1',
                    alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.FIELD_BUS_COUPLER'),
                    properties: [
                        { name: 'enabled', value: true },
                        { name: 'modbus.id', value: couplerComponentId },
                        { name: 'username', value: 'admin' },
                        { name: 'password', value: 'wago' },
                    ],
                    mode: ConfigurationMode.RemoveAndConfigure,
                }, 5);

                // offGridSwitch0
                componentConfigurator.add({
                    factoryId: 'Io.Off.Grid.Switch',
                    componentId: 'offGridSwitch0',
                    alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.CONTROL_GRID_POINT'),
                    properties: [
                        { name: 'inputGridStatus', value: 'io1/DigitalInputM1C2' },
                        { name: 'inputGroundingContactor', value: 'io1/DigitalInputM1C3' },
                        { name: 'inputMainContactor', value: 'io1/DigitalInputM1C1' },
                        { name: 'outputGroundingContactor', value: 'io1/DigitalOutputM1C2' },
                        { name: 'outputMainContactor', value: 'io1/DigitalOutputM1C1' },
                    ],
                    mode: ConfigurationMode.RemoveAndConfigure,
                }, 6);
                break;
        }

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
                { name: 'battery.id', value: 'battery0' },
            ],
            mode: ConfigurationMode.RemoveAndConfigure,
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
                    value: this.emergencyReserve.value ?? 5, /* minimum allowed value */
                },
            ],
            mode: this.emergencyReserve.isEnabled
                ? ConfigurationMode.RemoveAndConfigure
                : ConfigurationMode.RemoveOnly,
        }, 12);

        return componentConfigurator;
    }

    public override getProtocol(edge: Edge, websocket: Websocket): Promise<string> {
        const protocol: SetupProtocol = super.getCommonProtocolItems(edge);

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

        // Subsequent Strings will have only 1 static component. BMS submaster.
        const subsequentStaticTowerComponents = 1;
        const categoryElement: string = 'INSTALLATION.PROTOCOL_SERIAL_NUMBERS.BATTERY_STRING';

        super.addProtocolSerialNumbers(protocol, this.numberOfModulesPerTower, subsequentStaticTowerComponents, categoryElement);

        return new Promise((resolve, reject) => {
            websocket.sendRequest(SubmitSetupProtocolRequest.translateFrom(protocol, this.translate)).then((response: JsonrpcResponseSuccess) => {
                resolve(response.result['setupProtocolId']);
            }).catch((reason) => {
                reject(reason);
            });
        });
    }

    public override getSerialNumberFields(stringNr: number, numberOfModulesPerString: number) {

        const fields: FormlyFieldConfig[] = this.getCommercial30SerialNumbersFields(stringNr, numberOfModulesPerString);

        if (stringNr === 0) {

            // Adds the ems box field only for Initial String.
            const emsbox: FormlyFieldConfig = {
                key: 'emsbox',
                type: 'input',
                templateOptions: {
                    label: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.EMS_BOX_GRID_CONNECTION_POINT_COMMERCIAL30'),
                    required: true,
                    placeholder: 'xxxx',
                },
                validators: {
                    validation: ['emsBoxNetztrennstelleSerialNumber'],
                },
                wrappers: ['input-serial-number'],
            };

            // ems box field is added at a specific position in array, because it is always displayed at specific position in UI.
            fields.splice(1, 0, emsbox);
        }

        return fields;
    }

    public override getAdditionalEmergencyReserveFields(fields: FormlyFieldConfig[]): FormlyFieldConfig[] {

        fields.push({
            key: "coupler",
            props: {
                label: this.translate.instant('INSTALLATION.CONFIGURATION_EMERGENCY_RESERVE.COUPLER_TITLE'),
                required: true,
                options: [
                    {
                        label: Coupler.toLabelString(Coupler.WAGO, this.translate),
                        value: Coupler.WAGO,
                        url: Coupler.toImageUrl(Coupler.WAGO),
                    },
                    {
                        label: Coupler.toLabelString(Coupler.WEIDMUELLER, this.translate),
                        value: Coupler.WEIDMUELLER,
                        url: Coupler.toImageUrl(Coupler.WEIDMUELLER),
                    },
                ],
            },
            wrappers: ['formly-field-radio-with-image'],
            defaultValue: this.emergencyReserve.coupler,
        });

        return fields;
    }
}
