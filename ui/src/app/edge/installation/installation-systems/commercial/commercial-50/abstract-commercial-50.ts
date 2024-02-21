import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { Edge, EdgeConfig, Service, Websocket } from 'src/app/shared/shared';
import { environment } from 'src/environments';

import { FeedInType } from '../../../shared/enums';
import { SerialNumberFormData } from '../../../shared/ibndatatypes';
import { Meter } from '../../../shared/meter';
import { ComponentConfigurator, ConfigurationMode } from '../../../views/configuration-execute/component-configurator';
import { SchedulerIdBehaviour } from '../../abstract-ibn';
import { AbstractCommercialIbn } from '../abstract-commercial';

export abstract class AbstractCommercial50Ibn extends AbstractCommercialIbn {

    public override readonly defaultNumberOfModules: number = 20;
    public override showViewCount: boolean = true;

    public override fillSerialNumberForms(
        numberOfTowers: number,
        numberOfModulesPerTower: number,
        models: any,
        forms: SerialNumberFormData[],
    ) {
        this.numberOfModulesPerTower = numberOfModulesPerTower;
        for (let i = 0; i < numberOfTowers; i++) {
            forms[i] = {
                fieldSettings: this.getSerialNumberFields(i, numberOfModulesPerTower),
                model: models[i],
                formTower: new FormGroup({}),
                header: numberOfTowers === 1
                    ? this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.BESS_COMPONENTS')
                    : this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.BATTERY_STRING', { number: (i + 1) }),
            };
        }
        return forms;
    }

    public override getPreSettingsFields(numberOfModulesPerTower: number, numberOfTowers: number) {
        const fields: FormlyFieldConfig[] = [];

        fields.push({
            key: 'numberOfTowers',
            type: 'input',
            templateOptions: {
                type: 'number',
                label: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.NUMBER_OF_STRINGS'),
                min: 1,
                max: 4,
                description: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.MINIMUM_AND_MAXIMUM_STRINGS', { min: 1, max: 4 }),
                required: true,
            },
            parsers: [Number],
            defaultValue: numberOfTowers,
        });

        fields.push({
            key: 'numberOfModulesPerTower',
            type: 'input',
            templateOptions: {
                type: 'number',
                label: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.NUMBER_OF_MODULES_PER_STRINGS'),
                max: 20,
                description: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.MODULES_PER_STRINGS_DESCRIPTION', { number: 20 }),
                required: true,
            },
            parsers: [Number],
            defaultValue: numberOfModulesPerTower, // Acts as minimum value through "defaultAsMinimumValue" validator
            validators: {
                validation: ["defaultAsMinimumValue"],
            },
        });
        return fields;
    }

    public override getSerialNumberFields(towerNr: number, numberOfModulesPerTower: number) {
        const fields: FormlyFieldConfig[] = [];

        if (towerNr === 0) {

            fields.push({
                key: 'batteryInverter',
                type: 'input',
                templateOptions: {
                    label: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.INVERTER'),
                    required: true,
                    prefix: '50.0TL01S',
                    placeholder: 'xxxxxx',
                },
                validators: {
                    validation: ['commercial50BatteryInverterSerialNumber'],
                },
                wrappers: ['input-serial-number'],
            });

            fields.push({
                key: 'emsBox',
                type: 'input',
                templateOptions: {
                    label: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.EMS_BOX_COMMERCIAL', { edgeShortname: environment.edgeShortName }),
                    required: true,
                    placeholder: 'xxxxxxxxxxxx',
                },
                validators: {
                    validation: ['emsBoxSerialNumber'],
                },
                wrappers: ['input-serial-number'],
            });

            fields.push({
                key: 'bmsBoxMaster',
                type: 'input',
                templateOptions: {
                    label: 'BMS Box Master',
                    required: true,
                    prefix: 'WSDEM3822',
                    placeholder: 'xxxxxxxxxx',
                },
                // hideExpression: model => model.bmsComponent !== 'master',
                validators: {
                    validation: ['commercialBmsBoxSerialNumber'],
                },
                wrappers: ['input-serial-number'],
            });
        } else {
            fields.push({
                key: 'bmsBoxSubmaster' + towerNr,
                type: 'input',
                templateOptions: {
                    label: 'BMS Box Submaster',
                    required: true,
                    prefix: 'WSDESM3822',
                    placeholder: 'xxxxxxxxxx',
                },
                // hideExpression: model => model.bmsComponent !== 'submaster',
                validators: {
                    validation: ['commercialBmsBoxSerialNumber'],
                },
                wrappers: ['input-serial-number'],
            });
        }

        for (let moduleNr = 0; moduleNr < numberOfModulesPerTower; moduleNr++) {
            fields.push({
                key: 'module' + moduleNr,
                type: 'input',
                templateOptions: {
                    label: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.BATTERY_MODULE') + (moduleNr + 1),
                    required: true,
                    // Note: Edit also validator (substring 12) if removing prefix
                    prefix: 'WSDE...',
                    placeholder: 'xxxxxxxx',
                    description: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.BATTERY_MODULE_DESCRIPTION'),
                },
                validators: {
                    validation: ['commercialBatteryModuleSerialNumber'],
                },
                wrappers: ['input-serial-number'],
            });
        }
        return fields;
    }

    public override getComponentConfigurator(edge: Edge, config: EdgeConfig, websocket: Websocket, service?: Service): ComponentConfigurator {

        const componentConfigurator: ComponentConfigurator = new ComponentConfigurator(edge, config, websocket);
        const invalidateElementsAfterReadErrors: number = 3; // static value for commercial-50 systems.

        // adds Modbus 0, io0 (also modbus3 for Modbusbridge type TCP )
        super.addModbusBridgeAndIoComponents(this.modbusBridgeType, invalidateElementsAfterReadErrors, this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.BATTERY_INTERFACE'), componentConfigurator,
            edge, websocket, service);

        // modbus1
        componentConfigurator.add({
            factoryId: 'Bridge.Modbus.Tcp',
            componentId: 'modbus1',
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.COMMUNICATION_WITH_BATTERY_INVERTER'),
            properties: [
                { name: 'enabled', value: true },
                { name: 'ip', value: '10.4.0.10' },
                { name: 'port', value: '502' },
                { name: 'logVerbosity', value: 'NONE' },
                { name: 'invalidateElementsAfterReadErrors', value: invalidateElementsAfterReadErrors },
            ],
            mode: ConfigurationMode.RemoveAndConfigure,
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
                { name: 'invalidateElementsAfterReadErrors', value: invalidateElementsAfterReadErrors },
            ],
            mode: ConfigurationMode.RemoveAndConfigure,
        });

        // meter0
        componentConfigurator.add({
            factoryId: Meter.toFactoryId(this.lineSideMeterFuse.meterType),
            componentId: 'meter0',
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.GRID_METER'),
            properties: [
                { name: 'enabled', value: true },
                { name: 'modbus.id', value: 'modbus2' },
                { name: 'type', value: 'GRID' },
                { name: 'modbusUnitId', value: 5 },
            ],
            mode: ConfigurationMode.RemoveAndConfigure,
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
                { name: 'modbusUnitId', value: 1 },
            ],
            mode: ConfigurationMode.RemoveAndConfigure,
        });

        // TODO: Maybe we have to set a static IP in fems for Kaco
        // batteryInverter0
        componentConfigurator.add({
            factoryId: 'Battery-Inverter.Kaco.BlueplanetGridsave',
            componentId: 'batteryInverter0',
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.BATTERY_INVERTER'),
            properties: [
                { name: 'enabled', value: true },
                { name: 'modbus.id', value: 'modbus1' },
                { name: 'startStop', value: 'AUTO' },
            ],
            mode: ConfigurationMode.RemoveAndConfigure,
        });

        // ess0
        componentConfigurator.add({
            factoryId: 'Ess.Generic.ManagedSymmetric',
            componentId: 'ess0',
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.STORAGE_SYSTEM'),
            properties: [
                { name: 'enabled', value: true },
                { name: 'startStop', value: 'START' },
                { name: 'batteryInverter.id', value: 'batteryInverter0' },
                { name: 'battery.id', value: 'battery0' },
            ],
            mode: ConfigurationMode.RemoveAndConfigure,
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

        // ctrlBalancing0
        componentConfigurator.add({
            factoryId: 'Controller.Symmetric.Balancing',
            componentId: 'ctrlBalancing0',
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_FEATURES_STORAGE_SYSTEM.BALANCING'),
            properties: [
                { name: 'enabled', value: true },
                { name: 'ess.id', value: 'ess0' },
                { name: 'meter.id', value: 'meter0' },
                { name: 'targetGridSetpoint', value: 0 },
            ],
            mode: ConfigurationMode.RemoveAndConfigure,
        });

        return componentConfigurator;
    }

    public setRequiredControllers() {
        this.requiredControllerIds = [];
        if (this.feedInLimitation.feedInType === FeedInType.DYNAMIC_LIMITATION) {
            this.requiredControllerIds.push({
                componentId: "ctrlGridOptimizedCharge0",
                behaviour: SchedulerIdBehaviour.ALWAYS_INCLUDE,
            });
        }

        this.requiredControllerIds.push({
            componentId: "ctrlBalancing0",
            behaviour: SchedulerIdBehaviour.ALWAYS_INCLUDE,
        });
    }

    public override getSystemVariantFields(): FormlyFieldConfig[] {
        return [];
    }
}
