import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Edge, EdgeConfig, Service, Websocket } from '../../../../shared/shared';
import { AbstractBattery, CategorizedChannelDescription, ChannelChartDescription, ChannelDescription } from '../abstractbattery.component';

@Component({
    selector: SoltaroVersionBComponent.SELECTOR,
    templateUrl: './../abstractbattery.component.html'
})
export class SoltaroVersionBComponent extends AbstractBattery implements OnInit, OnDestroy {

    @Input() private componentId: string;
    @Input() public edge: Edge;
    @Input() public config: EdgeConfig;

    private static readonly SELECTOR = "soltaroVersionB";

    public component: EdgeConfig.Component = null;

    constructor(
        protected service: Service,
        protected translate: TranslateService,
        public websocket: Websocket,
        private route: ActivatedRoute
    ) { super(service, translate, websocket); }

    ngOnInit() {
        this.spinnerId = "soltaroC";
        this.service.startSpinner(this.spinnerId);
        this.service.setCurrentComponent('', this.route).then(edge => {

            this.edge = edge;

            this.service.getConfig().then(config => {
                this.config = config;
                this.component = this.config.getComponent(this.componentId);

                /*
                Todo: Subscribe in every single battery - Was not working that way 
                let channelAddresses = [];
                for (var channel in this.component.channels) {
                    channelAddresses.push(new ChannelAddress(this.component.id, channel));
                }
                this.edge.subscribeChannels(this.websocket, SoltaroVersionB.SELECTOR, channelAddresses);
                */
            });
            this.service.stopSpinner(this.spinnerId);
        });
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, SoltaroVersionBComponent.SELECTOR);
        }
    }


    protected getImportantWriteChannelDescriptions(): CategorizedChannelDescription[] {
        return [
            {
                category: "Modul-Details",
                channels: [
                    { channelName: "AutoSetSlavesId", register: "0x2014", description: "Neuadressierung aller Zellspannungssensoren", requiredInput: "1" },
                    { channelName: "AutoSetSlavesTemperatureId", register: "0x2019", description: "Neuadressierung aller Zelltemperatursensoren", requiredInput: "1" }
                ]
            },
            {
                category: "Zell-Details",
                channels: [
                    { channelName: "StopParameterCellUnderVoltageProtection", register: "0x2046", description: "Untere Zuschaltspannung(Protection)", requiredInput: "2700", unit: "mV" },
                    { channelName: "StopParameterCellUnderVoltageRecover", register: "0x2047", description: "Untere Zuschaltspannung(Recover)", requiredInput: "2750", unit: "mV" },
                    { channelName: "StopParameterCellOverVoltageProtection", register: "0x2040", description: "Obere Abschaltspannung(Protection)", requiredInput: "3700", unit: "mV" },
                    { channelName: "StopParameterCellOverVoltageRecover", register: "0x2041", description: "Obere Abschaltspannung(Recover)", requiredInput: "3650", unit: "mV" },
                    { channelName: "StopParameterCellVoltageDifferenceProtection", register: "0x2058", description: "Maximale Zellspannungsdifferenz(Protection)", requiredInput: "500", unit: "mV" },
                    { channelName: "StopParameterCellVoltageDifferenceProtectionRecover", register: "0x2059", description: "Maximale Zellspannungsdifferenz(Recover)", requiredInput: "500", unit: "mV" }
                ]
            },
            {
                category: "Gesamtsystem",
                channels: [
                    { channelName: "SystemReset", register: "0x2004", description: "BMS neustart", requiredInput: "1" },
                    { channelName: "SetSoc", register: "0x20DF", description: "Soc", requiredInput: "0 - 100", unit: "%" },
                    { channelName: "WorkParameterNumberOfModules", register: "0x20C1", description: "Anzahl der Module", requiredInput: "Anzahl" },
                    { channelName: "BmsContactorControl", register: "0x2010", description: "Precharge-Control", requiredInput: "1" },
                    { channelName: "EmsCommunicationTimeout", register: "0x201C", description: "Watchdog (EMS timeout protection)", requiredInput: "90", unit: "sec" },
                    { channelName: "VoltageLowProtection", register: "0x201E", description: "Abschaltspannung der Batterie", requiredInput: "", unit: "mV" },
                    { channelName: "SetEmsAddress", register: "0x201B", description: "Aktuell nur lesbar. Zukünftig Unit-ID setzen (Vorsicht: BMS danach nicht mehr erreichbar - In BMS Komponente ändern)", requiredInput: "" },
                    { channelName: "Sleep", register: "0x201D", description: "Sleep", requiredInput: "" }
                ]
            },
            {
                category: "Isolationsüberwachung",
                channels: [
                    { channelName: "WarnParameterInsulationAlarm", register: "0x2096", description: "Isolationsüberwachung deaktivieren (Warnung Lvl 2)", requiredInput: "0" },
                    { channelName: "WarnParameterInsulationAlarmRecover", register: "0x2097", description: "Isolationsüberwachung deaktivieren (Warnung Lvl 1)", requiredInput: "0" },
                    { channelName: "StopParameterInsulationProtection", register: "0x2056", description: "Isolationsüberwachung deaktivieren (Stop Lvl 2)", requiredInput: "0" },
                    { channelName: "StopParameterInsulationProtectionRecover", register: "0x2057", description: "Isolationsüberwachung deaktivieren (Stop Lvl 1)", requiredInput: "0" }
                ]
            }
        ];
    }

    protected getImportantSystemReadChannelDescriptions(): ChannelDescription[] {
        return [
            { channelName: "Soc", register: "0x2103", description: "Ladezustand", requiredInput: "", unit: "%" },
            { channelName: "WorkParameterNumberOfModules", register: "0x20C1", description: "Anzahl der angeschlossenen Module", requiredInput: "", unit: "Module" },
            { channelName: "ChargeMaxCurrent", register: "", description: "Errechnete maximale Beladung", requiredInput: "", unit: "A" },
            { channelName: "DischargeMaxCurrent", register: "", description: "Errechnete maximale Entladung", requiredInput: "", unit: "A" }
        ];
    }

    protected getImportantCellVoltageReadChannelDescriptions(): ChannelDescription[] {
        return [
            { channelName: "Voltage", register: "0x2100", description: "Gesamtspannung", requiredInput: "", unit: "mV" },
            { channelName: "Cluster1MinCellVoltage", register: "0x2108", description: " Minimale Zellspannung(Entsprechende ID)", requiredInput: "", dependentChannelNames: [{ channelName: "Cluster1MinCellVoltageId", unit: "- id" }], unit: "mV" },
            { channelName: "Cluster1MaxCellVoltage", register: "0x2106", description: " Maximale Zellspannung(Entsprechende ID)", requiredInput: "", dependentChannelNames: [{ channelName: "Cluster1MaxCellVoltageId", unit: "- id" }], unit: "mV" },
            { channelName: "StopParameterCellUnderVoltageProtection", register: "0x2046", description: "Untere Zuschaltspannung Protection(Recover)", requiredInput: "", dependentChannelNames: [{ channelName: "StopParameterCellUnderVoltageRecover", unit: "mV" }], unit: "mV" },
            { channelName: "StopParameterCellOverVoltageProtection", register: "0x2040", description: "Obere Abschaltspannung Protection(Recover)", requiredInput: "3700", dependentChannelNames: [{ channelName: "StopParameterCellOverVoltageRecover", unit: "mV" }], unit: "mV" },
            { channelName: "StopParameterCellVoltageDifferenceProtection", register: "0x2058", description: "Maximale Zellspannungsdifferenz Protection(Recover)", requiredInput: "500", dependentChannelNames: [{ channelName: "StopParameterCellVoltageDifferenceProtectionRecover", unit: "mV" }], unit: "mV" }

        ];
    }

    protected getImportantCellTemperatureReadChannelDescriptions(): ChannelDescription[] {
        return [
            { channelName: "MinCellTemperature", register: "0x210C", description: " Minimale Zelltemperatur(Entsprechende ID)", requiredInput: "", dependentChannelNames: [{ channelName: "Cluster1MinCellTemperatureId", unit: "- id" }], unit: "°C" },
            { channelName: "MaxCellTemperature", register: "0x210A", description: " Maximale Zelltemperatur(Entsprechende ID)", requiredInput: "", dependentChannelNames: [{ channelName: "Cluster1MaxCellTemperatureId", unit: "- id" }], unit: "°C" },
            { channelName: "StopParameterCellUnderTemperatureProtection", register: "0x204E", description: "Untere Zuschalttemperatur Protection(Recover)", requiredInput: "", dependentChannelNames: [{ channelName: "StopParameterCellUnderTemperatureRecover", unit: "°C" }], unit: "°C" },
            { channelName: "StopParameterCellOverTemperatureProtection", register: "0x204D", description: "Obere Abschalttemperatur Protection(Recover)", requiredInput: "3700", dependentChannelNames: [{ channelName: "StopParameterCellOverTemperatureRecover", unit: "°C" }], unit: "°C" },
            { channelName: "StopParameterCellTemperatureDifferenceProtection", register: "0x2060", description: "Maximale Zelltemperatursdifferenz Protection(Recover)", requiredInput: "500", dependentChannelNames: [{ channelName: "StopParameterCellTemperatureDifferenceProtectionRecover", unit: "°C" }], unit: "°C" }
        ];
    }
    protected getImportantInsulationReadChannelDescriptions(): ChannelDescription[] {
        return [
            { channelName: "WarnParameterInsulationAlarm", register: "0x2096", description: "Isolationsüberwachung Lvl. 1 Protection(Recover)", requiredInput: "", dependentChannelNames: [{ channelName: "WarnParameterInsulationAlarmRecover" }] },
            { channelName: "StopParameterInsulationProtection", register: "0x2056", description: "Isolationsüberwachung Lvl. 2 Protection(Recover)", requiredInput: "", dependentChannelNames: [{ channelName: "StopParameterInsulationProtectionRecover" }] }
        ];
    }

    protected getImportantAdditionalReadChannelDescriptions(): CategorizedChannelDescription[] {
        return [];
    }

    protected getImportantCellChartChannelDescriptions(): ChannelChartDescription[] {
        return [
            { label: "Minimale Zellspannung", channelName: "Cluster1MinCellVoltage", datasets: [], colorRgb: '45, 171, 91' },
            { label: "Maximale Zellspannung", channelName: "Cluster1MaxCellVoltage", datasets: [], colorRgb: '45, 123, 171' },

            { label: "Untere Zuschaltspannungen Lvl. 2 (Recover)", channelName: "StopParameterCellUnderVoltageRecover", datasets: [], colorRgb: '217, 149, 4' },
            { label: "Untere Zuschaltspannungen Lvl. 2 (Protection)", channelName: "StopParameterCellUnderVoltageProtection", datasets: [], colorRgb: '173, 24, 24' },

            { label: "Obere Abschaltspannungen Lvl. 2 (Recover)", channelName: "StopParameterCellOverVoltageRecover", datasets: [], colorRgb: '217, 149, 4' },
            { label: "Obere Abschaltspannungen Lvl. 2 (Protection)", channelName: "StopParameterCellOverVoltageProtection", datasets: [], colorRgb: '173, 24, 24' }
        ];
    }
}
