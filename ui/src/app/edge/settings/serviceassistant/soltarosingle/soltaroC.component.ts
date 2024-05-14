// @ts-strict-ignore
import { Component, Input, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { Edge, EdgeConfig, Service, Websocket } from '../../../../shared/shared';
import { AbstractBattery, CategorizedChannelDescription, ChannelChartDescription, ChannelDescription } from '../abstractbattery.component';

@Component({
    selector: "soltaroVersionC",
    templateUrl: './../abstractbattery.component.html',
})
export class SoltaroVersionCComponent extends AbstractBattery implements OnInit {

    @Input() private componentId: string;
    @Input() public override edge: Edge;
    @Input() public override config: EdgeConfig;

    public override component: EdgeConfig.Component = null;

    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        public override websocket: Websocket,
        private route: ActivatedRoute,
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
        });
        this.service.stopSpinner(this.spinnerId);
    }

    protected getImportantWriteChannelDescriptions(): CategorizedChannelDescription[] {
        return [
            {
                category: "Modul-Details",
                channels: [
                    { channelName: "AutoSetSlavesId", register: "0x2014", description: "Neuadressierung aller Zellspannungssensoren", requiredInput: "1" },
                    { channelName: "AutoSetSlavesTemperatureId", register: "0x2019", description: "Neuadressierung aller Zelltemperatursensoren", requiredInput: "1" },
                ],
            },
            {
                category: "Zell-Details",
                channels: [
                    { channelName: "Level1CellUnderVoltageProtection", register: "0x2046", description: "Minimale Zellspannung Lvl. 1 (Protection)", requiredInput: "2800", unit: "mV" },
                    { channelName: "Level1CellUnderVoltageRecover", register: "0x2047", description: "Minimale Zellspannung Lvl. 1 (Recover)", requiredInput: "2850", unit: "mV" },
                    { channelName: "Level2CellUnderVoltageProtection", register: "0x2406", description: "Minimale Zellspannung Lvl. 2 (Protection)", requiredInput: "2700", unit: "mV" },
                    { channelName: "Level2CellUnderVoltageRecover", register: "0x2407", description: "Minimale Zellspannung Lvl. 2 (Recover)", requiredInput: "2750", unit: "mV" },
                    { channelName: "Level1CellOverVoltageProtection", register: "0x2040", description: "Maximale Zellspannung Lvl. 1 (Protection)", requiredInput: "3650", unit: "mV" },
                    { channelName: "Level1CellOverVoltageRecover", register: "0x2041", description: "Maximale Zellspannung Lvl. 1 (Recover)", requiredInput: "3600", unit: "mV" },
                    { channelName: "Level2CellOverVoltageProtection", register: "0x2400", description: "Maximale Zellspannung Lvl. 2 (Protection)", requiredInput: "3700", unit: "mV" },
                    { channelName: "Level2CellOverVoltageRecover", register: "0x2401", description: "Maximale Zellspannung Lvl. 2 (Recover)", requiredInput: "3650", unit: "mV" },
                    { channelName: "Level1CellVoltageDifferenceProtection", register: "0x2058", description: "Maximale Zellspannungsdifferenz Lvl. 1 (Protection)", requiredInput: "500", unit: "mV" },
                    { channelName: "Level1CellVoltageDifferenceProtectionRecover", register: "0x2059", description: "Maximale Zellspannungsdifferenz Lvl. 1 (Recover)", requiredInput: "500", unit: "mV" },
                    { channelName: "Level2CellVoltageDifferenceProtection", register: "0x2418", description: "Maximale Zellspannungsdifferenz Lvl. 2 (Protection)", requiredInput: "1000", unit: "mV" },
                    { channelName: "Level2CellVoltageDifferenceProtectionRecover", register: "0x2419", description: "Maximale Zellspannungsdifferenz Lvl. 2 (Recover)", requiredInput: "1000", unit: "mV" },
                ],
            },
            {
                category: "Gesamtsystem",
                channels: [
                    { channelName: "SystemReset", register: "0x2004", description: "BMS neustart", requiredInput: "1" },
                    { channelName: "SetSoc", register: "0x20DF", description: "Soc", requiredInput: "0 - 100", unit: "%" },
                    { channelName: "WorkParameterPcsCommunicationRate", register: "0x20C1", description: "Anzahl der Module", requiredInput: "" },
                    { channelName: "PreChargeControl", register: "0x2010", description: "Precharge-Control", requiredInput: "1" },
                    { channelName: "EmsCommunicationTimeout", register: "0x201C", description: "Watchdog (EMS timeout protection)", requiredInput: "90", unit: "sec" },
                    { channelName: "VoltageLowProtection", register: "0x201E", description: "Abschaltspannung der Batterie", requiredInput: "" },
                    { channelName: "EmsAddress", register: "0x201B", description: "Aktuell nur lesbar. Zukünftig Unit-ID setzen (Vorsicht: BMS danach nicht mehr erreichbar - In BMS Komponente ändern)", requiredInput: "" },
                    { channelName: "Sleep", register: "0x201D", description: "Sleep", requiredInput: "" },
                ],
            },
            {
                category: "Isolationsüberwachung",
                channels: [
                    { channelName: "Level1InsulationProtection", register: "0x2056", description: "Isolationsüberwachung deaktivieren (Warnung Lvl 2)", requiredInput: "0" },
                    { channelName: "Level1InsulationProtectionRecover", register: "0x2057", description: "Isolationsüberwachung deaktivieren (Warnung Lvl 1)", requiredInput: "0" },
                    { channelName: "Level2InsulationProtection", register: "0x2416", description: "Isolationsüberwachung deaktivieren (Stop Lvl 2)", requiredInput: "0" },
                    { channelName: "Level2InsulationProtectionRecover", register: "0x2417", description: "Isolationsüberwachung deaktivieren (Stop Lvl 1)", requiredInput: "0" },
                ],
            },
        ];
    }

    protected getImportantSystemReadChannelDescriptions(): ChannelDescription[] {
        return [
            { channelName: "Soc", register: "0x2103", description: "Ladezustand", requiredInput: "", unit: "%" },
            { channelName: "WorkParameterPcsCommunicationRate", register: "0x20C1", description: "Anzahl der angeschlossenen Module", requiredInput: "", unit: "Module" },
            { channelName: "ChargeMaxCurrent", register: "", description: "Errechnete maximale Beladung", requiredInput: "", unit: "A" },
            { channelName: "DischargeMaxCurrent", register: "", description: "Errechnete maximale Entladung", requiredInput: "", unit: "A" },
        ];
    }

    protected getImportantCellVoltageReadChannelDescriptions(): ChannelDescription[] {
        return [
            { channelName: "Voltage", register: "0x2100", description: "Gesamtspannung", requiredInput: "", unit: "mV" },
            { channelName: "Cluster1MinCellVoltage", register: "0x2108", description: " Minimale Zellspannung(Entsprechende ID)", requiredInput: "", dependentChannelNames: [{ channelName: "Cluster1MinCellVoltageId", unit: "- id" }], unit: "mV" },
            { channelName: "MaxCellVoltage", register: "0x2106", description: " Maximale Zellspannung(Entsprechende ID)", requiredInput: "", dependentChannelNames: [{ channelName: "Cluster1MaxCellVoltageId", unit: "- id" }][""], unit: "mV" },
            { channelName: "Level1CellUnderVoltageProtection", register: "0x2046", description: "Minimale Zellspannung Lvl. 1 Protection(Recover)", requiredInput: "", dependentChannelNames: [{ channelName: "Level1CellUnderVoltageRecover", unit: "mV" }], unit: "mV" },
            { channelName: "Level2CellUnderVoltageProtection", register: "0x2406", description: "Minimale Zellspannung Lvl. 2 Protection(Recover)", requiredInput: "", dependentChannelNames: [{ channelName: "Level2CellUnderVoltageRecover", unit: "mV" }], unit: "mV" },
            { channelName: "Level1CellOverVoltageProtection", register: "0x2040", description: "Maximale Zellspannung Lvl. 1 Protection(Recover)", requiredInput: "", dependentChannelNames: [{ channelName: "Level1CellOverVoltageRecover", unit: "mV" }], unit: "mV" },
            { channelName: "Level2CellOverVoltageProtection", register: "0x2400", description: "Maximale Zellspannung Lvl. 2 Protection(Recover)", requiredInput: "", dependentChannelNames: [{ channelName: "Level2CellOverVoltageRecover", unit: "mV" }], unit: "mV" },
            { channelName: "Level1CellVoltageDifferenceProtection", register: "0x2058", description: "Maximale Zellspannungsdifferenz Lvl. 1 Protection(Recover)", requiredInput: "", dependentChannelNames: [{ channelName: "Level1CellVoltageDifferenceProtectionRecover", unit: "mV" }], unit: "mV" },
            { channelName: "Level2CellVoltageDifferenceProtection", register: "0x2418", description: "Maximale Zellspannungsdifferenz Lvl. 2 Protection(Recover)", requiredInput: "", dependentChannelNames: [{ channelName: "Level2CellVoltageDifferenceProtectionRecover", unit: "mV" }], unit: "mV" },
        ];
    }
    protected getImportantCellTemperatureReadChannelDescriptions(): ChannelDescription[] {
        return [
            { channelName: "MinCellTemperature", register: "0x210C", description: " Minimale Zelltemperatur(Entsprechende ID)", requiredInput: "", dependentChannelNames: [{ channelName: "Cluster1MinCellTemperatureId", unit: "- id" }], unit: "°C" },
            { channelName: "MaxCellTemperature", register: "0x210A", description: " Maximale Zelltemperatur(Entsprechende ID)", requiredInput: "", dependentChannelNames: [{ channelName: "Cluster1MaxCellTemperatureId", unit: "- id" }], unit: "°C" },
            { channelName: "Level1CellUnderTemperatureProtection", register: "0x204E", description: "Minimale Zelltemperatur Lvl. 1 Protection(Recover)", requiredInput: "", dependentChannelNames: [{ channelName: "Level1CellUnderTemperatureRecover", unit: "mV" }], unit: "°C" },
            { channelName: "Level2CellUnderTemperatureProtection", register: "0x240E", description: "Minimale Zelltemperatur Lvl. 2 Protection(Recover)", requiredInput: "", dependentChannelNames: [{ channelName: "Level2CellUnderTemperatureRecover", unit: "mV" }], unit: "°C" },
            { channelName: "Level1CellOverTemperatureProtection", register: "0x204C", description: "Maximale Zelltemperatur Lvl. 1 Protection(Recover)", requiredInput: "", dependentChannelNames: [{ channelName: "Level1CellOverTemperatureRecover", unit: "mV" }], unit: "°C" },
            { channelName: "Level2CellOverTemperatureProtection", register: "0x240C", description: "Maximale Zelltemperatur Lvl. 2 Protection(Recover)", requiredInput: "", dependentChannelNames: [{ channelName: "Level2CellOverTemperatureRecover", unit: "mV" }], unit: "°C" },
        ];
    }
    protected getImportantInsulationReadChannelDescriptions(): ChannelDescription[] {
        return [
            { channelName: "Level1InsulationProtection", register: "0x2056", description: "Isolationsüberwachung Lvl. 1 Protection(Recover)", requiredInput: "", dependentChannelNames: [{ channelName: "Level1InsulationProtectionRecover" }] },
            { channelName: "Level2InsulationProtection", register: "0x2416", description: "Isolationsüberwachung Lvl. 2 Protection(Recover)", requiredInput: "", dependentChannelNames: [{ channelName: "Level2InsulationProtectionRecover" }] },
        ];
    }

    protected getImportantAdditionalReadChannelDescriptions(): CategorizedChannelDescription[] {
        return [];
    }

    protected getImportantCellChartChannelDescriptions(): ChannelChartDescription[] {
        return [
            { label: "Minimale Zellspannung", channelName: "Cluster1MinCellVoltage", datasets: [], colorRgb: '45, 171, 91' },
            { label: "Maximale Zellspannung", channelName: "Cluster1MaxCellVoltage", datasets: [], colorRgb: '45, 123, 171' },

            { label: "Untere Zuschaltspannungen Lvl. 1 (Recover)", channelName: "Level1CellUnderVoltageRecover", datasets: [], colorRgb: '217, 149, 4' },
            { label: "Untere Zuschaltspannungen Lvl. 1 (Protection)", channelName: "Level1CellUnderVoltageProtection", datasets: [], colorRgb: '173, 24, 24' },
            { label: "Untere Zuschaltspannungen Lvl. 2 (Recover)", channelName: "Level2CellUnderVoltageRecover", datasets: [], colorRgb: '217, 149, 4' },
            { label: "Untere Zuschaltspannungen Lvl. 2 (Protection)", channelName: "Level2CellUnderVoltageProtection", datasets: [], colorRgb: '173, 24, 24' },

            { label: "Obere Abschaltspannungen Lvl. 1 (Recover)", channelName: "Level1CellOverVoltageRecover", datasets: [], colorRgb: '217, 149, 4' },
            { label: "Obere Abschaltspannungen Lvl. 1 (Protection)", channelName: "Level1CellOverVoltageProtection", datasets: [], colorRgb: '173, 24, 24' },
            { label: "Obere Abschaltspannungen Lvl. 2 (Recover)", channelName: "Level2CellOverVoltageRecover", datasets: [], colorRgb: '217, 149, 4' },
            { label: "Obere Abschaltspannungen Lvl. 2 (Protection)", channelName: "Level2CellOverVoltageProtection", datasets: [], colorRgb: '173, 24, 24' },
        ];
    }
}
