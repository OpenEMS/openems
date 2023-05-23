import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { Edge, EdgeConfig, Websocket } from 'src/app/shared/shared';
import { environment } from 'src/environments';
import { Category } from '../../../shared/category';
import { ComponentData, SerialNumberFormData } from '../../../shared/ibndatatypes';
import { Meter } from '../../../shared/meter';
import { ComponentConfigurator, ConfigurationMode } from '../../../views/configuration-execute/component-configurator';
import { AbstractCommercialIbn } from '../abstract-commercial';

export abstract class AbstractCommercial50Ibn extends AbstractCommercialIbn {

    public commercial50Feature: {

        feature: {
            type: Category.BALANCING
        } | {
            type: Category.PEAK_SHAVING_SYMMETRIC | Category.PEAK_SHAVING_ASYMMETRIC,
            entladungÜber: number;
            beladungUnter: number;
        }
    } = {
            // Initialization
            feature: {
                type: Category.BALANCING
            }
        };

    public readonly type: string = 'Fenecon-Commercial-50';

    public readonly defaultNumberOfModules: number = 20;

    public addPeakShavingData(peakShavingData: ComponentData[]) {
        if (this.commercial50Feature.feature.type !== Category.BALANCING) {
            peakShavingData.push(
                {
                    label: this.translate.instant('INSTALLATION.CONFIGURATION_PEAK_SHAVING.DISCHARGE_ABOVE_LABEL'),
                    value: this.commercial50Feature.feature.entladungÜber
                },
                {
                    label: this.translate.instant('INSTALLATION.CONFIGURATION_PEAK_SHAVING.CHARGE_BELOW_LABEL'),
                    value: this.commercial50Feature.feature.beladungUnter
                });
        }
        return peakShavingData;
    }

    public setCommercialfeature(commercial50Feature: any) {

        if (commercial50Feature.feature) {
            // Directly copy from Session Storage
            this.commercial50Feature.feature = commercial50Feature.feature;
        } else {
            // From Peak Shaving view.
            if (this.commercial50Feature.feature.type !== Category.BALANCING) {
                this.commercial50Feature.feature.beladungUnter = commercial50Feature.beladungUnter;
                this.commercial50Feature.feature.entladungÜber = commercial50Feature.entladungÜber;
            }
        }
    }

    public getPeakShavingHeader() {
        return this.commercial50Feature.feature.type === Category.PEAK_SHAVING_SYMMETRIC
            ? Category.PEAK_SHAVING_SYMMETRIC_HEADER
            : Category.PEAK_SHAVING_ASYMMETRIC_HEADER;
    }

    public getSerialNumbers(towerNr: number, edge: Edge, websocket: Websocket, numberOfModulesPerTower: number) {
        return new Promise((resolve) => {
            // We cannot read any serial numbers automatically from commercial-50.
            resolve({});
        });
    }

    public fillForms(
        numberOfTowers: number,
        numberOfModulesPerTower: number,
        models: any,
        forms: SerialNumberFormData[]
    ) {
        this.numberOfModulesPerTower = numberOfModulesPerTower;
        for (let i = 0; i < numberOfTowers; i++) {
            forms[i] = {
                fieldSettings: this.getFields(i, numberOfModulesPerTower),
                model: models[i],
                formTower: new FormGroup({}),
                header: numberOfTowers === 1
                    ? this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.BESS_COMPONENTS')
                    : this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.BATTERY_STRING', { stringNumber: (i + 1) })
            };
        }
        return forms;
    }

    public getSettingsFields(numberOfModulesPerTower: number, numberOfTowers: number) {
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
                required: true
            },
            parsers: [Number],
            defaultValue: numberOfTowers
        });

        fields.push({
            key: 'numberOfModulesPerTower',
            type: 'input',
            templateOptions: {
                type: 'number',
                label: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.NUMBER_OF_MODULES_PER_STRINGS'),
                min: 20,
                max: 20,
                description: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.MODULES_PER_STRINGS_DESCRIPTION', { number: 20 }),
                required: true
            },
            parsers: [Number],
            defaultValue: numberOfModulesPerTower,
        });
        return fields;
    }

    public getFields(towerNr: number, numberOfModulesPerTower: number) {
        const fields: FormlyFieldConfig[] = [];

        if (towerNr === 0) {

            fields.push({
                key: 'batteryInverter',
                type: 'input',
                templateOptions: {
                    label: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.INVERTER'),
                    required: true,
                    prefix: '50.0TL01S',
                    placeholder: 'xxxxxx'
                },
                validators: {
                    validation: ['commercial50BatteryInverterSerialNumber']
                },
                wrappers: ['input-serial-number']
            });

            fields.push({
                key: 'emsBox',
                type: 'input',
                templateOptions: {
                    label: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.EMS_BOX_COMMERCIAL', { edgeShortname: environment.edgeShortName }),
                    required: true,
                    prefix: 'FC',
                    placeholder: 'xxxxxxxxx'
                },
                validators: {
                    validation: ['emsBoxSerialNumber']
                },
                wrappers: ['input-serial-number']
            });

            fields.push({
                key: 'bmsBox',
                type: 'input',
                templateOptions: {
                    label: 'BMS Box Master',
                    required: true,
                    prefix: 'WSDEM3822',
                    placeholder: 'xxxxxxxxxx'
                },
                // hideExpression: model => model.bmsComponent !== 'master',
                validators: {
                    validation: ['commercialBmsBoxSerialNumber']
                },
                wrappers: ['input-serial-number']
            });
        } else {
            fields.push({
                key: 'bmsBox',
                type: 'input',
                templateOptions: {
                    label: 'BMS Box Submaster',
                    required: true,
                    prefix: 'WSDESM3822',
                    placeholder: 'xxxxxxxxxx'
                },
                // hideExpression: model => model.bmsComponent !== 'submaster',
                validators: {
                    validation: ['commercialBmsBoxSerialNumber']
                },
                wrappers: ['input-serial-number']
            });
        }

        // Bms Master and Submaster.
        // const bmsComponents = ([
        //     { value: 'master', label: 'Master' },
        //     { value: 'submaster', label: 'Submaster' }
        // ]);

        // fields.push({
        //     key: 'bmsComponent',
        //     type: 'select',
        //     templateOptions: {
        //         label: 'BMS Box',
        //         options: bmsComponents,
        //         required: true
        //     },
        //     wrappers: ['formly-select-field-wrapper']
        // });

        for (let moduleNr = 0; moduleNr < numberOfModulesPerTower; moduleNr++) {
            fields.push({
                key: 'module' + moduleNr,
                type: 'input',
                templateOptions: {
                    label: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.BATTERY_MODULE') + (moduleNr + 1),
                    required: true,
                    // Note: Edit also validator (substring 12) if removing prefix
                    prefix: 'WSDE213822',
                    placeholder: 'xxxxxxxxxx'
                },
                validators: {
                    validation: ['commercialBatteryModuleSerialNumber']
                },
                wrappers: ['input-serial-number']
            });
        }
        return fields;
    }

    public getComponentConfigurator(edge: Edge, config: EdgeConfig, websocket: Websocket) {

        const componentConfigurator: ComponentConfigurator = new ComponentConfigurator(edge, config, websocket);

        // modbus0
        componentConfigurator.add({
            factoryId: 'Bridge.Modbus.Serial',
            componentId: 'modbus0',
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.BATTERY_INTERFACE'),
            properties: [
                { name: 'enabled', value: true },
                { name: 'portName', value: '/dev/ttyAMA0' },
                { name: 'baudRate', value: 9600 },
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
                { name: 'ip', value: '10.4.0.10' },
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

        // meter0
        componentConfigurator.add({
            factoryId: Meter.toFactoryId(this.lineSideMeterFuse.meterType),
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
            mode: ConfigurationMode.RemoveAndConfigure
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
                { name: 'battery.id', value: 'battery0' }
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        });

        // PV Meter optional
        componentConfigurator.add(super.addAcPvMeter('modbus2'));

        return componentConfigurator;
    }
}
