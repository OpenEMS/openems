import { FormlyFieldConfig } from '@ngx-formly/core';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { JsonrpcResponseSuccess } from 'src/app/shared/jsonrpc/base';
import { SetupProtocol, SubmitSetupProtocolRequest } from 'src/app/shared/jsonrpc/request/submitSetupProtocolRequest';
import { ChannelAddress, Edge, Websocket } from 'src/app/shared/shared';
import { environment } from 'src/environments';
import { Category, FeedInType } from '../../shared/enums';
import { ComponentData } from '../../shared/ibndatatypes';
import { AbstractIbn } from '../abstract-ibn';

export abstract class AbstractCommercialIbn extends AbstractIbn {

    public readonly lineSideMeterFuseTitle = Category.LINE_SIDE_METER_FUSE_COMMERCIAL;

    public readonly showRundSteuerManual: boolean = false;

    public showViewCount: boolean = false;

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

    public numberOfModulesPerTower: number;

    public setFeedInLimitsFields(model: any) {
        this.feedInLimitation.feedInType = model.feedInType;
        return this.feedInLimitation;
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

    public getSettings(edge: Edge, websocket: Websocket):
        Promise<{
            numberOfTowers: number;
            numberOfModulesPerTower: number;
        }> {
        return new Promise((resolve) => {
            let isResolved = false;

            // Edge-subscribe
            edge.subscribeChannels(websocket, 'commercial', [
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
                    resolve({ numberOfTowers: 1, numberOfModulesPerTower: this.defaultNumberOfModules });
                }

                // Unsubscribe to currentData and channels after timeout
                stopOnRequest.next();
                stopOnRequest.complete();
                edge.unsubscribeChannels(websocket, 'commercial');
            }, 5000);
        });
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

    public getProtocolSerialNumbers(protocol: SetupProtocol): SetupProtocol {

        protocol.lots = [];

        const serialNumbers = this.serialNumbers;

        // Initial tower has 3 static components other than modules such as Welcherischter, BMS and EMS box.
        const initialStaticTowerComponents = 3;

        // Subsequent towers will have only 2 static components. Paralell box and BMS box.
        const subsequentStaticTowerComponents = 1;

        // Total number of components each tower contains, so that easier to categorize the serial numbers based on towers.
        const numberOfComponentsTower1 = this.numberOfModulesPerTower + initialStaticTowerComponents;
        const numberOfComponentsTower2 = numberOfComponentsTower1 + this.numberOfModulesPerTower + subsequentStaticTowerComponents;
        const numberOfComponentsTower3 = numberOfComponentsTower2 + this.numberOfModulesPerTower + subsequentStaticTowerComponents;
        const numberOfComponentsTower4 = numberOfComponentsTower3 + this.numberOfModulesPerTower + subsequentStaticTowerComponents;

        for (let componentCount = 0; componentCount < serialNumbers.modules.length; componentCount++) {
            if (serialNumbers.modules[componentCount].value !== null && serialNumbers.modules[componentCount].value !== '') {
                // String 1
                if (componentCount < numberOfComponentsTower1) {
                    protocol.lots.push({
                        category: 'Speichersystemkomponenten',
                        name: serialNumbers.modules[componentCount].label + ' Seriennummer',
                        serialNumber: serialNumbers.modules[componentCount].value,
                    });
                }
                // String 2
                else if (componentCount < numberOfComponentsTower2) {
                    protocol.lots.push({
                        category: 'Batterie String 2',
                        name: serialNumbers.modules[componentCount].label + ' Seriennummer',
                        serialNumber: serialNumbers.modules[componentCount].value,
                    });
                }
                // String 3
                else if (componentCount < numberOfComponentsTower3) {
                    protocol.lots.push({
                        category: 'Batterie String 3',
                        name: serialNumbers.modules[componentCount].label + ' Seriennummer',
                        serialNumber: serialNumbers.modules[componentCount].value,
                    });
                }
                // String 4
                else if (componentCount < numberOfComponentsTower4) {
                    protocol.lots.push({
                        category: 'Batterie String 4',
                        name: serialNumbers.modules[componentCount].label + ' Seriennummer',
                        serialNumber: serialNumbers.modules[componentCount].value,
                    });
                }
            }
        }
        return protocol;
    }
}
