import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { ChannelAddress, Edge, EdgeConfig, Websocket } from 'src/app/shared/shared';
import { FeedInType } from '../../../shared/enums';
import { SerialNumberFormData } from '../../../shared/ibndatatypes';
import { Meter } from '../../../shared/meter';
import { ComponentConfigurator, ConfigurationMode } from '../../../views/configuration-execute/component-configurator';
import { AbstractCommercialIbn } from '../abstract-commercial';

export abstract class AbstractCommercial30Ibn extends AbstractCommercialIbn {

    public readonly defaultNumberOfModules = 9;

    public readonly type: string = 'Fenecon-Commercial-30';

    public fillForms(
        numberOfTowers: number,
        numberOfModulesPerTower: number,
        models: any,
        forms: SerialNumberFormData[]) {
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
                max: 2,
                description: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.MINIMUM_AND_MAXIMUM_STRINGS', { min: 1, max: 2 }),
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
                min: 9,
                max: 17,
                description: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.MINIMUM_AND_MAXIMUM_MODULES_PER_STRINGS', { min: 9, max: 17 }),
                required: true
            },
            parsers: [Number],
            defaultValue: numberOfModulesPerTower
        });
        return fields;
    }

    protected override getChannels(towerNr: number, numberOfModulesPerTower: number): ChannelAddress[] {
        const channelAddresses = super.getChannels(towerNr, numberOfModulesPerTower);
        channelAddresses['batteryInverter'] = new ChannelAddress('batteryInverter0', 'SerialNumber');
        return channelAddresses;
    }

    protected override addSerialNumbersToModel(key: string, model: Object, serialNumber: string): boolean {
        if (super.addSerialNumbersToModel(key, model, serialNumber)) {
            // already serial number is parsed for the key.
            return true;
        } else if (key.startsWith('batteryInverter')) {
            // Battery inverter serial number.
            model[key] = serialNumber.substring(5);
            return true;
        }

        return false;
    }

    /**
     * Returns thhe common serial number fields for commercial systems.
     * 
     * @param stringNr Number of Strings configured.
     * @param numberOfModulesPerString Number of Modules per String.
     * @returns the fields.
     */
    protected getCommercial30SerialNumbersFields(stringNr: number, numberOfModulesPerString: number): FormlyFieldConfig[] {
        const fields: FormlyFieldConfig[] = [];

        if (stringNr === 0) {

            fields.push({
                key: 'batteryInverter',
                type: 'input',
                templateOptions: {
                    label: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.INVERTER'),
                    required: true,
                    prefix: 'PWS00',
                    placeholder: 'xxxxxxxxxx'
                },
                validators: {
                    validation: ['commercial30BatteryInverterSerialNumber']
                },
                wrappers: ['input-serial-number']
            });

            fields.push({
                key: 'bmsBoxMaster',
                type: 'input',
                templateOptions: {
                    label: 'BMS Box Master',
                    required: true,
                    prefix: 'WSDEM3822',
                    placeholder: 'xxxxxxxxxx'
                },
                validators: {
                    validation: ['commercialBmsBoxSerialNumber']
                },
                wrappers: ['input-serial-number']
            });
        } else {
            fields.push({
                key: 'bmsBoxSubMaster' + stringNr,
                type: 'input',
                templateOptions: {
                    label: 'BMS Box Submaster',
                    required: true,
                    prefix: 'WSDESM3822',
                    placeholder: 'xxxxxxxxxx'
                },
                validators: {
                    validation: ['commercialBmsBoxSerialNumber']
                },
                wrappers: ['input-serial-number']
            });
        }

        for (let moduleNr = 0; moduleNr < numberOfModulesPerString; moduleNr++) {
            fields.push({
                key: 'module' + moduleNr,
                type: 'input',
                templateOptions: {
                    label: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.BATTERY_MODULE') + (moduleNr + 1),
                    required: true,
                    // Note: Edit also validator (substring 12) if removing prefix
                    prefix: 'WSDE...',
                    placeholder: 'xxxxxxxx',
                    description: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.BATTERY_MODULE_DESCRIPTION')
                },
                validators: {
                    validation: ['commercialBatteryModuleSerialNumber']
                },
                wrappers: ['input-serial-number']
            });
        }
        return fields;

    }

    /**
     * View Configuration-execute.
     * Returns the common required configuration object for commercial-30 systems with components specific to the system.
     * 
     * @param edge the current edge.
     * @param config the EdgeConfig.
     * @param websocket the Websocket connection.
     * @param invalidateElementsAfterReadErrors specifies the number of times to check before invalidating.
     * 
     * @returns ComponentConfigurator
     */
    public getCommercial30ComponentConfigurator(edge: Edge, config: EdgeConfig, websocket: Websocket, invalidateElementsAfterReadErrors: number) {

        const componentConfigurator: ComponentConfigurator = new ComponentConfigurator(edge, config, websocket);

        // modbus0
        componentConfigurator.add(super.getModbusBridgeComponent(this.modbusBridgeType, invalidateElementsAfterReadErrors, this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.COMMUNICATION_WITH_BATTERY')));

        // modbus1
        componentConfigurator.add({
            factoryId: 'Bridge.Modbus.Tcp',
            componentId: 'modbus1',
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.COMMUNICATION_WITH_BATTERY_INVERTER'),
            properties: [
                { name: 'enabled', value: true },
                { name: 'ip', value: '192.168.1.11' },
                { name: 'port', value: '502' },
                { name: 'logVerbosity', value: 'NONE' },
                { name: 'invalidateElementsAfterReadErrors', value: invalidateElementsAfterReadErrors }
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
                { name: 'invalidateElementsAfterReadErrors', value: invalidateElementsAfterReadErrors }
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
                { name: 'modbusUnitId', value: 5 },
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
                { name: 'countryCode', value: 'GERMANY' },
                { name: 'startStop', value: 'AUTO' },
            ],
            mode: ConfigurationMode.RemoveAndConfigure
        });

        // PV Meter optional
        componentConfigurator.add(super.addAcPvMeter('modbus2'));

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
        componentConfigurator.add({ // Clearify with Productmanagement if a different App like peak shaving could be selected in IBN
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
}
