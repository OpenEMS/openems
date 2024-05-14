// @ts-strict-ignore
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { SetChannelValueRequest } from "src/app/shared/jsonrpc/request/setChannelValueRequest";
import { TranslateService } from "@ngx-translate/core";

export abstract class AbstractBattery {

    protected edge: Edge;
    protected component: EdgeConfig.Component;
    protected config: EdgeConfig;
    public spinnerId: string = "";
    public _abstractBatterySpinner: string = "AbstractBatterySpinner";

    // Write Channels
    protected importantWriteChannelDescriptions: CategorizedChannelDescription[] = [];

    // Read Channels
    protected importantReadChannelDescriptions: CategorizedChannelDescription[] = [];

    // Cell-Chart: Channels and information
    protected importantCellChartChannelDescriptions: ChannelChartDescription[] = [];

    constructor(
        protected service: Service,
        protected translate: TranslateService,
        public websocket: Websocket,
    ) {
        this.service.startSpinner(this._abstractBatterySpinner);
        this.importantWriteChannelDescriptions = this.getImportantWriteChannelDescriptions();
        this.importantReadChannelDescriptions.push(
            { category: "Edge.Service.entireSystem", channels: this.getImportantSystemReadChannelDescriptions() },
            { category: "Edge.Service.Cell.voltages", channels: this.getImportantCellVoltageReadChannelDescriptions() },
            { category: "Edge.Service.Cell.temperatures", channels: this.getImportantCellTemperatureReadChannelDescriptions() },
            { category: "Edge.Service.Cell.insulation", channels: this.getImportantInsulationReadChannelDescriptions() },
        );

        this.getImportantAdditionalReadChannelDescriptions().forEach(categorizedChannelDescription => {
            this.importantReadChannelDescriptions.push(categorizedChannelDescription);
        });

        this.importantCellChartChannelDescriptions = this.getImportantCellChartChannelDescriptions();
        this.service.stopSpinner(this._abstractBatterySpinner);
    }

    /**
     *  Gets a list of important write channels.
     *
     *  E.g.
     *  [
            {
                category: "Modul-details",
                channels: [
                    { channelName: "AutoSetSlavesId", register: "0x2014", description: "", requiredInput: "1" },
                    { channelName: "AutoSetSlavesTemperatureId", register: "0x2019", description: "", requiredInput: "1" },
                ]
            },
        ];
     */
    protected abstract getImportantWriteChannelDescriptions(): CategorizedChannelDescription[];

    /**
     *  Gets a list of important system relevant read channels.
     *
     *  E.g.
     *  [
            { channelName: "Soc", register: "0x2103", description: "", requiredInput: "", unit:"%" },
            { channelName: "WorkParameterPcsCommunicationRate", register: "0x20C1", description: "", requiredInput: "" },
        ]
     */
    protected abstract getImportantSystemReadChannelDescriptions(): ChannelDescription[];

    /**
     *  Gets a list of important cell voltage read channels.
     *
     *  E.g.
     *  [
            { channelName: "Voltage", register: "0x2100", description: "", requiredInput: "" },
            { channelName: "Cluster1MinCellVoltage", register: "0x2108", description: "", requiredInput: "" },
            { channelName: "Cluster1MinCellVoltageId", register: "0x2107", description: "", requiredInput: "" },
            { channelName: "MaxCellVoltage", register: "0x2106", description: "", requiredInput: "" },
            { channelName: "Level1CellUnderVoltageProtection", register: "0x2046", description: "", requiredInput: "" },
        ]
     */
    protected abstract getImportantCellVoltageReadChannelDescriptions(): ChannelDescription[];


    /*
    *  Gets a list of important cell temperature read channels.
    *
    *  E.g.
    *  [
            { channelName: "MinCellTemperature", register: "0x210C", description: "", requiredInput: "" },
            { channelName: "Cluster1MinCellTemperatureId", register: "0x210B", description: "", requiredInput: "" },
            { channelName: "Level1CellUnderTemperatureProtection", register: "0x204E", description: "", requiredInput: "" },
       ]
    */
    protected abstract getImportantCellTemperatureReadChannelDescriptions(): ChannelDescription[];

    /*
    *  Gets a list of important insulation read channels.
    *
    *  E.g.
    *  [
            { channelName: "StopParameterInsulationProtection", register: "0x2056", description: "", requiredInput: "0" },
       ]
    */
    protected abstract getImportantInsulationReadChannelDescriptions(): ChannelDescription[];


    /*
    *  Gets a list of additional read channels.
    *
    *  E.g.
    *  [
            {
                category: "Solaro Version A - Additional channels",
                channels: [
                    { channelName: "AutoSetSlavesId", register: "0x2014", description: "", requiredInput: "1" },
                ]
            },
        ];
    */
    protected abstract getImportantAdditionalReadChannelDescriptions(): CategorizedChannelDescription[];

    /**
     *  Gets a list of important cell channels as ChannelChartDescription.
     *
     *  E.g.
     *  {
     *       label: "Minimum cellvoltage", channelName: "Cluster1MinCellVoltage", datasets: [], colorRgb: '45, 171, 91'
     *  },
     */
    protected abstract getImportantCellChartChannelDescriptions(): ChannelChartDescription[];


    getChannelAddress(componentId: string, channel: string): ChannelAddress {
        return new ChannelAddress(componentId, channel);
    }

    setChannelValue(address: ChannelAddress, value: any) {
        if (this.edge) {
            this.edge.sendRequest(
                this.service.websocket,
                new SetChannelValueRequest({
                    componentId: address.componentId,
                    channelId: address.channelId,
                    value: value,
                }),
            ).then(response => {

                // The value should be only set once and not for the configured backend timout time - e.g. for system reset it would restart the battery very often till the timeout.
                if (value != null) {
                    this.service.toast("Successfully set " + address.toString() + " to [" + value + "]", "success");
                    setTimeout(() => {
                        this.setChannelValue(address, null);
                    }, 10000);
                }
            }).catch(reason => {
                this.service.toast("Error setting " + address.toString() + " to [" + value + "]", 'danger');
            });
        }
    }
}

export type CategorizedChannelDescription = {
    category: string,
    channels: ChannelDescription[]
}

export type ChannelDescription = {
    channelName: string,
    register?: string,
    description?: string,
    requiredInput?: string,

    // If the major information of a Channel can be extended by another channel - e.g. "MinCellVoltage" could have configured "MinCellVoltageId".
    dependentChannelNames?: ChannelDescription[],

    // The EdgeConfig has no unit values, given by the Edge, if it is connected via backend.
    unit?: string,
}

export type ChannelChartDescription = {
    label: string,
    channelName: string,
    datasets: number[],
    colorRgb: string,
}
