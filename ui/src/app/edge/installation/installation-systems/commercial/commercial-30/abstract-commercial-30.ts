import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { ChannelAddress, Edge, Websocket } from 'src/app/shared/shared';
import { SerialNumberFormData } from '../../../shared/ibndatatypes';
import { AbstractCommercialIbn } from '../abstract-commercial';

export abstract class AbstractCommercial30Ibn extends AbstractCommercialIbn {
    private static readonly SELECTOR = 'Commercial';

    public readonly defaultNumberOfModules = 9;

    public getSerialNumbers(towerNr: number, edge: Edge, websocket: Websocket, numberOfModulesPerTower: number) {
        return new Promise((resolve) => {
            let isResolved = false;
            const channelAddresses: ChannelAddress[] = [];
            const subscriptionId = AbstractCommercial30Ibn.SELECTOR;
            const model: Object = {};

            // Gather channel addresses
            channelAddresses['batteryInverter'] = new ChannelAddress('batteryInverter0', 'SerialNumber');

            // Edge-subscribe
            edge.subscribeChannels(websocket, subscriptionId, Object.values(channelAddresses));

            // Subject to stop the subscription to currentData
            const stopOnRequest: Subject<void> = new Subject<void>();

            // Read data
            edge.currentData.pipe(
                takeUntil(stopOnRequest),
                filter(currentData => currentData != null)
            ).subscribe((currentData) => {

                // We currently are reading only one channel address so we are directly indexing to it.
                // Next batch of Commercial-30 will have possibility to read more registers.
                const channelAddressIndex: string = Object.keys(channelAddresses)[0];
                const channelAddress: ChannelAddress = channelAddresses[channelAddressIndex];
                const serialNumber: string = currentData.channel[channelAddress.componentId + '/' + channelAddress.channelId];

                // If one serial number is undefined return
                if (!serialNumber) {
                    return;
                }

                // Only take after first 5 digits.
                model[channelAddressIndex] = serialNumber.substring(5);

                // Resolve the promise
                isResolved = true;
                resolve(model);
            });
            setTimeout(() => {
                // If data isn't available after the timeout, the
                // promise gets resolved with an empty object
                if (!isResolved) {
                    resolve({});
                }

                // Unsubscribe to currentData and channels after timeout
                stopOnRequest.next();
                stopOnRequest.complete();
                edge.unsubscribeChannels(websocket, subscriptionId);
            }, 5000);

        });
    }

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
                max: 2,
                description: 'Minimum Strings: ' + 1 + ' und Maximum: ' + 2,
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
                min: 9,
                max: 17,
                description: 'Minimum modules pro String: ' + 9 + ' und Maximum: ' + 17,
                required: true
            },
            parsers: [Number],
            defaultValue: numberOfModulesPerTower
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
                    prefix: 'PWS00',
                    placeholder: 'xxxxxxxxxx'
                },
                validators: {
                    validation: ['commercial30BatteryInverterSerialNumber']
                },
                wrappers: ['input-serial-number']
            });

            fields.push({
                key: 'femsBox',
                type: 'input',
                templateOptions: {
                    label: 'FEMS Anschlussbox/Netztrennstelle',
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
        //     { value: "master", label: "Master" },
        //     { value: "submaster", label: "Submaster" }
        // ]);

        // fields.push({
        //     key: "bmsComponent",
        //     type: "select",
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
}
