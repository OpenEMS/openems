import { FormlyFieldConfig } from '@ngx-formly/core';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { JsonrpcResponseSuccess } from 'src/app/shared/jsonrpc/base';
import { SetupProtocol, SubmitSetupProtocolRequest } from 'src/app/shared/jsonrpc/request/submitSetupProtocolRequest';
import { ChannelAddress, Edge, Service, Websocket } from 'src/app/shared/shared';

import { Category } from '../../shared/category';
import { FeedInType, ModbusBridgeType, WebLinks } from '../../shared/enums';
import { ComponentData } from '../../shared/ibndatatypes';
import { IbnUtils } from '../../shared/ibnutils';
import { Meter } from '../../shared/meter';
import { SubSystemType, SystemType } from '../../shared/system';
import { ComponentConfigurator, ConfigurationMode, ConfigurationObject } from '../../views/configuration-execute/component-configurator';
import { AbstractIbn } from '../abstract-ibn';

export abstract class AbstractCommercialIbn extends AbstractIbn {
    private static readonly SELECTOR = 'Commercial';

    public override readonly type: SystemType = SystemType.COMMERCIAL;
    public override readonly showRundSteuerManual: boolean = false;
    public override showViewCount: boolean = false;
    public modbusBridgeType: ModbusBridgeType;

    // configuration-emergency-reserve
    public override emergencyReserve? = {
        isEnabled: true,
        minValue: 15,
        value: 20,
        isReserveSocEnabled: false,
    };

    // protocol-dynamic-feed-in-limitation
    public override feedInLimitation? = {
        feedInType: FeedInType.EXTERNAL_LIMITATION,
        maximumFeedInPower: 0,
    };

    // Protocol line side meter fuse
    public override lineSideMeterFuse?: {
        category: Category;
        fixedValue?: number;
        otherValue?: number;
        meterType: Meter.GridMeter;
    } = {
            category: Category.LINE_SIDE_METER_FUSE_COMMERCIAL,
            meterType: Meter.GridMeter.KDK,
        };

    public numberOfModulesPerTower: number;

    public setFeedInLimitFields(model: any) {
        this.feedInLimitation.feedInType = model.feedInType;
        return this.feedInLimitation;
    }

    //Configuration Summary
    public override gtcAndWarrantyLinks: {
        gtcLink: WebLinks;
        warrantyLink: WebLinks;
    } = {
            gtcLink: WebLinks.GTC_LINK,
            warrantyLink: WebLinks.WARRANTY_LINK_COMMERCIAL,
        };

    public override getLineSideMeterFuseFields() {
        const fields: FormlyFieldConfig[] = [];

        fields.push({
            key: "otherValue",
            type: "input",
            templateOptions: {
                label: this.translate.instant('INSTALLATION.CONFIGURATION_LINE_SIDE_METER_FUSE.VALUE'),
                type: 'number',
                description: this.translate.instant('INSTALLATION.CONFIGURATION_LINE_SIDE_METER_FUSE.FIXED_VALUE_DESCRIPTION'),
                min: 0,
                required: true,
            },
            parsers: [Number],
            validators: {
                validation: ["onlyPositiveInteger"],
            },
        });

        fields.push({
            key: "meterType",
            type: "select",
            templateOptions: {
                label: this.translate.instant('INSTALLATION.CONFIGURATION_LINE_SIDE_METER_FUSE.METER_LABEL'),
                required: true,
                options: [
                    { label: "SOCOMEC", value: Meter.GridMeter.SOCOMEC },
                    { label: "KDK", value: Meter.GridMeter.KDK },
                ],
            },
            defaultValue: Meter.GridMeter.KDK,
        });
        return fields;
    }

    public getFeedInLimitFields() {

        // Update the feedInlimitation field
        let totalPvPower = 0;
        this.feedInLimitation.maximumFeedInPower = totalPvPower;

        return super.getCommonFeedInLimitsFields(totalPvPower);
    }

    public override addCustomBatteryInverterData() {
        const batteryInverterData: ComponentData[] = [];

        this.feedInLimitation.feedInType === FeedInType.DYNAMIC_LIMITATION
            ? batteryInverterData.push(
                {
                    label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.MAXIMUM_FEED_IN_VALUE'),
                    value: this.feedInLimitation.maximumFeedInPower,
                },
            )
            : this.feedInLimitation.feedInType === FeedInType.EXTERNAL_LIMITATION
                ? batteryInverterData.push(
                    {
                        label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.EXTERNAL_CONTROLLER_RECIEVER'),
                        value: this.translate.instant('General.yes'),
                    })
                : batteryInverterData.push(
                    {
                        label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.NO_LIMITATION'),
                        value: this.translate.instant('General.yes'),
                    });

        return batteryInverterData;
    }

    public override getPreSettingInformationFromEdge(edge: Edge, websocket: Websocket):
        Promise<{
            numberOfTowers: number;
            numberOfModulesPerTower: number;
        }> {
        return new Promise((resolve) => {
            let isResolved = false;

            // Edge-subscribe
            edge.subscribeChannels(websocket, 'commercial', [
                new ChannelAddress('battery0', 'NumberOfTowers'),
                new ChannelAddress('battery0', 'NumberOfModulesPerTower'),
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
                            numberOfModulesPerTower: parseInt(numberOfModulesPerTower, 10),
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
                edge.unsubscribeChannels(websocket, AbstractCommercialIbn.SELECTOR);
            }, 5000);
        });
    }

    public override getProtocol(edge: Edge, websocket: Websocket): Promise<string> {
        const protocol: SetupProtocol = super.getCommonProtocolItems(edge);

        const feedInLimitation = this.feedInLimitation;
        protocol.items.push(
            {
                category: Category.FEED_IN_MANAGEMENT,
                name: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.EXTERNAL_CONTROLLER_RECIEVER'),
                value: feedInLimitation.feedInType == FeedInType.EXTERNAL_LIMITATION
                    ? this.translate.instant('General.yes')
                    : this.translate.instant('General.no'),
            },
            {
                category: Category.FEED_IN_MANAGEMENT,
                name: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.DYNAMIC_LIMITATION'),
                value: feedInLimitation.feedInType == FeedInType.DYNAMIC_LIMITATION
                    ? this.translate.instant('General.yes')
                    : this.translate.instant('General.no'),
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

    /**
     * Adds the Modbus bridge and IO component.
     *
     * adds modbus0 and io0 if RTU is selected. otherwise modbus0, modbus3, io0 is added.
     *
     * @param modbusBridgeType Modbus bridge type (TCP or RS485).
     * @param invalidateElementsAfterReadErrors the maximum read errors allowed.
     * @param alias alias for the component.
     * @param componentConfigurator configuration object.
     * @returns configuration object with modbus bridge components added.
     */
    public addModbusBridgeAndIoComponents(modbusBridgeType: ModbusBridgeType, invalidateElementsAfterReadErrors: number, alias: string, componentConfigurator: ComponentConfigurator,
        edge: Edge, websocket: Websocket, service: Service): ComponentConfigurator {

        let ioComponentId: string;

        switch (modbusBridgeType) {
            case ModbusBridgeType.TCP_IP:
                // modbus0
                componentConfigurator.add({
                    factoryId: 'Bridge.Modbus.Tcp',
                    componentId: 'modbus0',
                    alias: alias,
                    properties: [
                        { name: 'enabled', value: true },
                        { name: 'ip', value: '192.168.0.7' },
                        { name: 'port', value: '20108' },
                        { name: 'logVerbosity', value: 'NONE' },
                        { name: 'invalidateElementsAfterReadErrors', value: invalidateElementsAfterReadErrors },
                    ],
                    mode: ConfigurationMode.RemoveAndConfigure,
                });

                // Add ip address to network configuration
                if (!IbnUtils.addIpAddress('eth1', '192.168.0.9/24', edge, websocket)) {
                    service.toast('Eine für die Batterie notwendige IP-Adresse konnte nicht zur Netzwerkkonfiguration hinzugefügt werden.'
                        , 'danger');
                }

                ioComponentId = 'modbus3'; // To communicate with io.

                // modbus3
                componentConfigurator.add(AbstractCommercialIbn.getSerialModbusBridgeComponent(ioComponentId, invalidateElementsAfterReadErrors, this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.COMMUNICATION_WITH_RELAY')));
                break;

            case ModbusBridgeType.RS485:
                ioComponentId = 'modbus0'; // To communicate with battery and io.

                // modbus0
                componentConfigurator.add(AbstractCommercialIbn.getSerialModbusBridgeComponent(ioComponentId, invalidateElementsAfterReadErrors, alias));
                break;
        }

        // io0
        componentConfigurator.add({
            factoryId: 'IO.KMtronic',
            componentId: 'io0',
            alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.RELAY_BOARD'),
            properties: [
                { name: 'enabled', value: true },
                { name: 'modbus.id', value: ioComponentId },
                { name: 'modbusUnitId', value: 6 },
            ],
            mode: ConfigurationMode.RemoveAndConfigure,
        }, 3);

        return componentConfigurator;
    }

    /**
     * Returns the Modbus Serial bridge component.
     *
     * @param ioComponentId The component id of the component.
     * @param invalidateElementsAfterReadErrors the maximum read errors allowed.
     * @param alias the alias string
     * @returns The modbus serial Configuration Object.
     */
    private static getSerialModbusBridgeComponent(ioComponentId: string, invalidateElementsAfterReadErrors: number, alias: string): ConfigurationObject {
        return {
            factoryId: 'Bridge.Modbus.Serial',
            componentId: ioComponentId,
            alias: alias,
            properties: [
                { name: 'enabled', value: true },
                { name: 'portName', value: '/dev/ttyAMA0' },
                { name: 'baudRate', value: 9600 },
                { name: 'databits', value: 8 },
                { name: 'stopbits', value: 'ONE' },
                { name: 'parity', value: 'NONE' },
                { name: 'logVerbosity', value: 'NONE' },
                { name: 'invalidateElementsAfterReadErrors', value: invalidateElementsAfterReadErrors },
            ],
            mode: ConfigurationMode.RemoveAndConfigure,
        };
    }

    /**
     * Returns the channel addresses for reading serial numbers of the system.
     *
     * @param towerNr number of towers.
     * @param numberOfModulesPerTower number of modules per tower.
     * @returns The channel addresses.
     */
    protected getChannels(towerNr: number, numberOfModulesPerTower: number): ChannelAddress[] {

        const channelAddresses: ChannelAddress[] = [];
        channelAddresses['bmsBoxMaster'] = new ChannelAddress('battery0', 'MasterSerialNumber');

        for (let moduleNr = 0; moduleNr < numberOfModulesPerTower; moduleNr++) {
            channelAddresses['module' + moduleNr] = new ChannelAddress('battery0', 'Tower' + towerNr + 'Module' + moduleNr + 'SerialNumber');
        }

        return channelAddresses;
    }

    public override getSerialNumbersFromEdge(towerNr: number, edge: Edge, websocket: Websocket, numberOfModulesPerTower: number) {
        return new Promise((resolve) => {
            let isResolved = false;
            const channelAddresses: ChannelAddress[] = this.getChannels(towerNr, numberOfModulesPerTower);
            const subscriptionId = AbstractCommercialIbn.SELECTOR + '-tower' + towerNr;
            let model: Object = {};

            // Subject to stop the subscription to currentData
            const stopOnRequest: Subject<void> = new Subject<void>();

            // Read data
            edge.currentData.pipe(
                takeUntil(stopOnRequest),
                filter(currentData => currentData != null),
            ).subscribe((currentData) => {
                let anyNullorUndefined: boolean = false;

                for (const [key, channelAddress] of Object.entries(channelAddresses)) {

                    const serialNumber: string = currentData.channel[channelAddress.toString()];

                    // If serial number is undefined or null, return
                    if (!serialNumber) {
                        anyNullorUndefined = true;
                        continue;
                    }

                    if (!this.addSerialNumbersToModel(key, model, serialNumber)) {
                        // unhandled parsing
                        // eg: battery module serial numbers are taken directly.
                        model[key] = serialNumber;
                    }
                }

                if (!anyNullorUndefined) {
                    // Resolve the promise
                    isResolved = true;
                    resolve(model);
                }
            });

            // Edge-subscribe.
            // TODO for method in edge to read channels once.
            edge.subscribeChannels(websocket, subscriptionId, Object.values(channelAddresses));

            setTimeout(() => {
                // If data isn't available after the timeout, the
                // promise gets resolved with an empty object
                if (!isResolved) {
                    resolve(model);
                }

                // Unsubscribe to currentData and channels after timeout
                stopOnRequest.next();
                stopOnRequest.complete();
                edge.unsubscribeChannels(websocket, subscriptionId);
            }, 5000);
        });
    }

    /**
     * Adds serial numbers to the model.
     *
     * Returns true if adding serial numbers to the model was successful. If not, returns false.
     *
     * @param key the channel address
     * @param model the model.
     * @param serialNumber the serial number from the channel.
     */
    protected addSerialNumbersToModel(key: string, model: Object, serialNumber: string): boolean {
        if (key.startsWith('bmsBox')) {
            // BMS master.
            model[key] = serialNumber.substring(14);
            return true;
        }
        return false;
    }

    public override setNonAbstractFields(ibnString: any) {

        // Configuration commercial modbus bridge
        if ('modbusBridgeType' in ibnString) {
            this.modbusBridgeType = ibnString.modbusBridgeType;
        }

    }

    public override getSubSystemFields(): FormlyFieldConfig[] {
        const fields: FormlyFieldConfig[] = [];
        const componentLabel = ([
            { value: SubSystemType.COMMERCIAL_30, label: SubSystemType.COMMERCIAL_30 },
            { value: SubSystemType.COMMERCIAL_50, label: SubSystemType.COMMERCIAL_50 },
        ]);

        fields.push({
            key: 'subType',
            type: 'radio',
            className: 'line-break',
            templateOptions: {
                label: this.translate.instant('INSTALLATION.CONFIGURATION_SYSTEM.PRODUCT_NAME'),
                options: componentLabel,
                required: true,
            },
        });

        return fields;
    }
}
