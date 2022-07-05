import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { JsonrpcResponseSuccess } from 'src/app/shared/jsonrpc/base';
import { SetupProtocol, SubmitSetupProtocolRequest } from 'src/app/shared/jsonrpc/request/submitSetupProtocolRequest';
import { ChannelAddress, Edge, Websocket } from 'src/app/shared/shared';
import { ComponentData, SerialNumberFormData } from 'src/app/shared/type/componentData';
import { FeedInSetting, FeedInType } from 'src/app/shared/type/feedinsettings';
import { environment } from 'src/environments';
import { AbstractIbn } from '../abstract-ibn';

export abstract class AbstractCommercialIbn extends AbstractIbn {
    private static readonly SELECTOR = 'Commercial';

    public readonly lineSideMeterFuseTitle = 'Vorsicherung Netzanschlusspunkt / Zählervorsicherung';

    public readonly showRundSteuerManual = false;

    public showViewCount = true;

    // configuration-emergency-reserve
    public emergencyReserve?= {
        isEnabled: true,
        minValue: 15,
        value: 20,
        isReserveSocEnabled: false,
    };

    // protocol-dynamic-feed-in-limitation
    public feedInLimitation?= {
        feedInType: FeedInType.EXTERNAL_LIMITATION,
        maximumFeedInPower: 0
    };

    public setFeedInLimitsFields(model: any) {
        this.feedInLimitation.feedInType = model.feedInType;
        return this.feedInLimitation;
    }

    public getSerialNumbers(towerNr: number, edge: Edge, websocket: Websocket, numberOfModulesPerTower: number) {
        return new Promise((resolve) => {
            let isResolved = false;
            const channelAddresses: { [key: string]: ChannelAddress } = {};
            const subscriptionId = AbstractCommercialIbn.SELECTOR;
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
                for (const key in channelAddresses) {
                    if (channelAddresses.hasOwnProperty(key)) {
                        const channelAddress: ChannelAddress = channelAddresses[key];
                        const serialNumber: string = currentData.channel[channelAddress.componentId + '/' + channelAddress.channelId];

                        // If one serial number is undefined return
                        if (!serialNumber) {
                            return;
                        }

                        // Only take after first 5 digits.
                        model[key] = serialNumber.substr(5);
                    }
                }

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
        forms: Array<SerialNumberFormData>) {
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
            key: 'numberOfStrings',
            type: 'input',
            templateOptions: {
                type: 'number',
                label: 'Anzahl Strings',
                min: 1,
                max: 2,
                required: true
            },
            parsers: [Number],
            defaultValue: numberOfTowers
        });

        fields.push({
            key: 'numberOfModulesPerString',
            type: 'input',
            templateOptions: {
                type: 'number',
                label: 'Anzahl Module pro String',
                min: 9,
                max: 17,
                required: true
            },
            parsers: [Number],
            defaultValue: numberOfModulesPerTower
        });
        return fields;
    }

    public getLineSideMeterFuseFields() {
        const fields: FormlyFieldConfig[] = [];

        fields.push({
            key: "otherValue",
            type: "input",
            templateOptions: {
                label: "Wert [A]",
                type: 'number',
                description: "Mit welcher Stromstärke ist der Zähler abgesichert?",
                min: 0,
                required: true
            },
            parsers: [Number],
            validators: {
                validation: ["onlyPositiveInteger"]
            }
        });
        return fields;
    }

    public getFields(towerNr: number, numberOfModulesPerTower: number) {
        // TODO add validation: no duplicate serial number entries
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
                    validation: ['commercialBatteryInverterSerialNumber']
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
                    prefix: 'WSDE2138',
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

    public getSettings(edge: Edge, websocket: Websocket):
        Promise<{
            numberOfTowers: number;
            numberOfModulesPerTower: number;
        }> {
        return new Promise((resolve) => {
            let isResolved = false;

            // Edge-subscribe
            edge.subscribeChannels(websocket, AbstractCommercialIbn.SELECTOR, [
                new ChannelAddress('battery0', 'NumberOfTowers'),
                new ChannelAddress('battery0', 'NumberOfModulesPerTower')
            ]);

            // Subject to stop the subscription to currentData
            const stopOnRequest: Subject<void> = new Subject<void>();

            // Read tower and module numbers
            edge.currentData
                .pipe(
                    takeUntil(stopOnRequest),
                    filter(currentData => currentData != null))
                .subscribe((currentData) => {
                    const numberOfTowers = currentData.channel['battery0/NumberOfTowers'];
                    const numberOfModulesPerTower = currentData.channel['battery0/NumberOfModulesPerTower'];

                    // If values are available, resolve the promise with them
                    if (numberOfTowers && numberOfModulesPerTower) {
                        isResolved = true;
                        resolve({
                            // 10 is given as radix parameter.
                            // 2 = binary, 8 = octal, 10 = decimal, 16 = hexadecimal.
                            numberOfTowers: parseInt(numberOfTowers, 10),
                            numberOfModulesPerTower: parseInt(numberOfModulesPerTower, 10)
                        });
                    }
                });

            setTimeout(() => {
                // If data isn't available after the timeout, the
                // promise gets resolved with default values
                if (!isResolved) {
                    resolve({ numberOfTowers: 1, numberOfModulesPerTower: 9 });
                }

                // Unsubscribe to currentData and channels after timeout
                stopOnRequest.next();
                stopOnRequest.complete();
                // edge.unsubscribeChannels(websocket, AbstractCommercialIbn.SELECTOR);
            }, 5000);
        });
    }

    public getFeedInLimitFields() {

        const fields: FormlyFieldConfig[] = [];
        const pv = this.pv;
        let totalPvPower = 0;

        for (const ac of pv.ac) {
            totalPvPower += ac.value ?? 0;
        }

        // Update the feedInlimitation field
        this.feedInLimitation.maximumFeedInPower = parseInt((totalPvPower * 0.7).toFixed(0), 10);

        fields.push({
            key: "feedInType",
            type: "select",
            className: "white-space-initial",
            templateOptions: {
                label: "Typ",
                placeholder: "Select Option",
                options: [
                    { label: "Netzdienliche Beladung (z.B. 70% Abregelung)", value: FeedInType.DYNAMIC_LIMITATION },
                    { label: "Rundsteuerempfänger (Externe Abregelung durch Netzbetreiber)", value: FeedInType.EXTERNAL_LIMITATION }
                ],
                required: true,
            }
        })

        fields.push({
            key: 'maximumFeedInPower',
            type: 'input',
            templateOptions: {
                type: 'number',
                label: 'Maximale Einspeiseleistung [W]',
                description: 'Diesen Wert entnehmen Sie der Anschlussbestätigung des Netzbetreibers',
                required: true
            },
            parsers: [Number],
            // 10 is given as radix parameter.
            // 2 = binary, 8 = octal, 10 = decimal, 16 = hexadecimal.
            defaultValue: totalPvPower,
            hideExpression: model => model.feedInType != FeedInType.DYNAMIC_LIMITATION
        });
        return fields;
    }

    public addCustomBatteryInverterData(batteryInverterData: ComponentData[]) {

        const feedInLimitation = this.feedInLimitation;
        feedInLimitation.feedInType === FeedInType.DYNAMIC_LIMITATION
            ? batteryInverterData.push(
                {
                    label: 'Maximale Einspeiseleistung',
                    value: feedInLimitation.maximumFeedInPower,
                }
            )
            : batteryInverterData.push(
                {
                    label: "Rundsteuerempfänger",
                    value: "ja"
                })

        return batteryInverterData;
    }

    public addCustomPvData(pvData: ComponentData[]) {
        return pvData;
    }

    public getProtocol(edge: Edge, websocket: Websocket): Promise<string> {

        const installer = this.installer;
        const customer = this.customer;
        const pv = this.pv;
        const lineSideMeterFuse = this.lineSideMeterFuse;
        const serialNumbers = this.serialNumbers;
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

        const protocol: SetupProtocol = {
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

        let lineSideMeterFuseValue: number;
        lineSideMeterFuseValue = lineSideMeterFuse.otherValue;

        protocol.items.push({
            category: this.lineSideMeterFuseTitle,
            name: 'Wert [A]',
            value: lineSideMeterFuseValue ? lineSideMeterFuseValue.toString() : '',
        });

        const feedInLimitation = this.feedInLimitation;
        protocol.items.push({
            category: 'Einspeisemanagement',
            name: 'Rundsteuerempfänger',
            value: feedInLimitation.feedInType == FeedInType.EXTERNAL_LIMITATION
                ? "ja"
                : "nein"
        });

        protocol.items.push({
            category: 'Einspeisemanagement',
            name: 'Netzdienliche Beladung (z.B. 70% Abregelung)',
            value: feedInLimitation.feedInType == FeedInType.DYNAMIC_LIMITATION
                ? "ja"
                : "nein"
        });

        if (feedInLimitation.feedInType == FeedInType.DYNAMIC_LIMITATION) {
            protocol.items.push({
                category: 'Einspeisemanagement',
                name: 'Maximale Einspeiseleistung [W]',
                value: feedInLimitation.maximumFeedInPower
                    ? feedInLimitation.maximumFeedInPower.toString()
                    : (0).toString(),
            });
        }

        for (let index = 0; index < ac.length; index++) {
            const element = ac[index];
            const label = 'AC' + (index + 1);

            protocol.items.push({
                category: 'Zusätzliche AC-Erzeuger',
                name: 'Alias ' + label,
                value: element.alias
            });

            protocol.items.push({
                category: 'Zusätzliche AC-Erzeuger',
                name: 'Wert ' + label + ' [Wp]',
                value: element.value ? element.value.toString() : ''
            });

            protocol.items.push({
                category: 'Zusätzliche AC-Erzeuger',
                name: 'Ausrichtung ' + label,
                value: element.orientation
            });

            protocol.items.push({
                category: 'Zusätzliche AC-Erzeuger',
                name: 'Modultyp ' + label,
                value: element.moduleType
            });

            protocol.items.push({
                category: 'Zusätzliche AC-Erzeuger',
                name: 'Anzahl PV-Module ' + label,
                value: element.modulesPerString ? element.modulesPerString.toString() : ''
            });

            protocol.items.push({
                category: 'Zusätzliche AC-Erzeuger',
                name: 'Zählertyp ' + label,
                value: element.meterType
            });

            protocol.items.push({
                category: 'Zusätzliche AC-Erzeuger',
                name: 'Modbus Kommunikationsadresse ' + label,
                value: element.modbusCommunicationAddress ? element.modbusCommunicationAddress.toString() : ''
            });
        }

        protocol.items.push({
            category: 'FEMS',
            name: 'FEMS Nummer',
            value: edge.id
        });

        protocol.lots = [];

        // Speichersystemkomponenten
        for (const serialNumber of serialNumbers.modules) {
            if (serialNumber.value !== null && serialNumber.value !== '') {
                protocol.lots.push({
                    category: 'Speichersystemkomponenten',
                    name: serialNumber.label + ' Seriennummer',
                    serialNumber: serialNumber.value
                });
            }
        }

        return new Promise((resolve, reject) => {
            websocket.sendRequest(new SubmitSetupProtocolRequest({ protocol })).then((response: JsonrpcResponseSuccess) => {
                resolve(response.result['setupProtocolId']);
            }).catch((reason) => {
                reject(reason);
            });
        });
    }
}

