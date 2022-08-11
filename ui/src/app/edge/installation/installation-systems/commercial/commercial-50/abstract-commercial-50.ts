import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { Edge, EdgeConfig, Websocket } from 'src/app/shared/shared';
import { ComponentData, SerialNumberFormData } from '../../../shared/ibndatatypes';
import { ComponentConfigurator, ConfigurationMode } from '../../../views/configuration-execute/component-configurator';
import { AbstractCommercialIbn } from '../abstract-commercial';

export abstract class AbstractCommercial50Ibn extends AbstractCommercialIbn {

    public commercial50Feature: {

        feature: {
            type: 'Eigenverbrauchsoptimierung'
        } | {
            type: 'PhasengenaueLastspitzenkappung' | 'Lastspitzenkappung',
            entladungÜber: number;
            beladungUnter: number;
        }
    } = {
            // Initialization
            feature: {
                type: 'Eigenverbrauchsoptimierung'
            }
        }

    public readonly type: string = 'Fenecon-Commercial-50';

    public readonly defaultNumberOfModules: number = 20;

    public addPeakShavingData(peakShavingData: ComponentData[]) {
        if (this.commercial50Feature.feature.type !== 'Eigenverbrauchsoptimierung') {
            peakShavingData.push(
                {
                    label: 'Entladung Über',
                    value: this.commercial50Feature.feature.entladungÜber
                },
                {
                    label: 'Beladung Unter',
                    value: this.commercial50Feature.feature.beladungUnter
                })
        }
        return peakShavingData;
    }

    public setCommercialfeature(commercial50Feature: any) {

        if (commercial50Feature.feature) {
            // Directly copy from Session Storage
            this.commercial50Feature.feature = commercial50Feature.feature;
        } else {
            // From Peak Shaving view.
            if (this.commercial50Feature.feature.type !== 'Eigenverbrauchsoptimierung') {
                this.commercial50Feature.feature.beladungUnter = commercial50Feature.beladungUnter;
                this.commercial50Feature.feature.entladungÜber = commercial50Feature.entladungÜber;
            }
        }
    }

    public getPeakShavingHeader() {
        return this.commercial50Feature.feature.type === 'Lastspitzenkappung'
            ? 'Einstellungen Lastspitzenkappung'
            : 'Einstellungen Phasengenaue Lastspitzenkappung';
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
                header: numberOfTowers === 1 ? 'Speichersystemkomponenten' : ('Batteriestring ' + (i + 1))
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
                label: 'Anzahl Strings',
                min: 1,
                max: 4,
                description: 'Minimum Strings: ' + 1 + ' und Maximum: ' + 4,
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
                label: 'Anzahl Module pro String',
                min: 20,
                max: 20,
                description: 'Modules pro String für Commercial-50 system: ' + 20,
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
                    label: 'Wechselrichter',
                    required: true,
                    prefix: '50.0TL01S',
                    placeholder: 'xxxxxxxxxx'
                },
                validators: {
                    validation: ['commercial50BatteryInverterSerialNumber']
                },
                wrappers: ['input-serial-number']
            });

            fields.push({
                key: 'femsBox',
                type: 'input',
                templateOptions: {
                    label: 'FEMS Anschlussbox',
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
                    label: 'Batteriemodul ' + (moduleNr + 1),
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
            alias: 'Schnittstelle Batterie',
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
            alias: 'Kommunikation mit dem Wechselrichter',
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
            alias: 'Schnittstelle Meter',
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
            alias: 'Relaisboard',
            properties: [
                { name: 'enabled', value: true },
                { name: 'modbus.id', value: 'modbus0' },
                { name: 'modbusUnitId', value: 6 }
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
                { name: 'masterStartUpRelay', value: 'io0/Relay8' },
                { name: 'modbusUnitId', value: 1 }
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        });

        // TODO: Maybe we have to set a static IP in fems for Kaco
        // batteryInverter0
        componentConfigurator.add({
            factoryId: 'Battery-Inverter.Kaco.BlueplanetGridsave',
            componentId: 'batteryInverter0',
            alias: 'Batterie-Wechselrichter',
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
            alias: 'Speichersystem',
            properties: [
                { name: 'enabled', value: true },
                { name: 'startStop', value: 'START' },
                { name: 'batteryInverter.id', value: 'batteryInverter0' },
                { name: 'battery.id', value: 'battery0' }
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        });

        // Optional meter2 - aditional AC PV
        const acArray = this.pv.ac;
        const isAcCreated: boolean = acArray.length >= 1;

        // TODO: Tell the customer in the View that one Production socomec meter is installed per default!
        // TODO if more than 1 meter should be created, this logic must be changed
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
        return componentConfigurator;
    }
}
