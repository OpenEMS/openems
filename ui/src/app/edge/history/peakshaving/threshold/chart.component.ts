// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { DefaultTypes } from "src/app/shared/service/defaulttypes";
import { YAxisType } from "src/app/shared/service/utils";

import { ChannelAddress, Edge, EdgeConfig, Service } from "../../../../shared/shared";
import { AbstractHistoryChart } from "../../abstracthistorychart";

@Component({
    selector: "thresholdpeakshavingchart",
    templateUrl: "../../abstracthistorychart.html",
})
export class ThresholdPeakshavingChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input({ required: true }) public period!: DefaultTypes.HistoryPeriod;
    @Input({ required: true }) public componentId!: string;

    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        private route: ActivatedRoute,
    ) {
        super("thresholdpeakshaving-chart", service, translate);
    }

    ngOnChanges() {
        this.updateChart();
    }

    ngOnInit() {
        this.startSpinner();
    }

    ngOnDestroy() {
        this.unsubscribeChartRefresh();
    }

    public getChartHeight(): number {
        return window.innerHeight / 1.3;
    }

    protected updateChart() {
        this.autoSubscribeChartRefresh();
        this.startSpinner();
        this.loading = true;
        this.colors = [];
        this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {


            this.service.getConfig().then(config => {
                const meterIdActivePower = config.getComponent(this.componentId).properties["meter.id"] + "/ActivePower";
                const propertyPeakshavingThresholdPower = this.componentId + "/_PropertyPeakShavingThresholdPower";
                const propertyPeakshavingPower = this.componentId + "/_PropertyPeakShavingPower";
                const propertyRechargePower = this.componentId + "/_PropertyRechargePower";
                const stateMachine = this.componentId + "/StateMachine";
                const peakShavedPower = this.componentId + "/PeakShavedPower";
                const peakShavingTargetPower = this.componentId + "/PeakShavingTargetPower";
                const essSoc = this.componentId + "/EssSoc";
                const essPower = this.componentId + "/EssPower";

                const result = response.result;

                console.log("Debug Result:", result); // <-- Hier wird das Resultat geloggt
                console.log("Keys in result.data:", Object.keys(result.data)); // Check if expected keys are present

                console.log("Debug Result:", peakShavedPower); // <-- Hier wird das Resultat geloggt

                // convert labels
                const labels: Date[] = [];
                for (const timestamp of result.timestamps) {
                    labels.push(new Date(timestamp));
                }
                this.labels = labels;

                // convert datasets
                const datasets = [];



                if (meterIdActivePower in result.data) {
                    const data = result.data[meterIdActivePower].map(value => {
                        if (value == null) {
                            return null;
                        } else if (value == 0) {
                            return 0;
                        } else {
                            return value / 1000; // convert to kW
                        }
                    });
                    datasets.push({
                        label: this.translate.instant("Edge.Index.Widgets.Peakshaving.gridConsumption"),
                        data: data,
                        hidden: false,
                    });
                    this.colors.push({
                        backgroundColor: "rgba(0,0,0,0.05)",
                        borderColor: "rgba(0,0,0,1)",
                        borderWidth: 0.5,
                    });
                }

                if (peakShavingTargetPower in result.data) {
                    const data = result.data[peakShavingTargetPower].map(value => {
                        if (value == null) {
                            return null;
                        } else if (value == 0) {
                            return 0;
                        } else {
                            return value / 1000; // convert to kW
                        }
                    });
                    datasets.push({
                        label: this.translate.instant("Edge.Index.Widgets.Peakshaving.peakshavingTarget"),
                        data: data,
                        hidden: false,
                    });
                    this.colors.push({
                        backgroundColor: "rgba(0,0,0,0)",
                        borderColor: "rgba(200,140,28,1)",
                        borderDash: [3, 3],
                    });
                }

                if (peakShavedPower in result.data) {
                    const data = result.data[peakShavedPower].map(value => {
                        if (value == null) {
                            return null;
                        } else if (value == 0) {
                            return 0;
                        } else {
                            console.log("Value:", value);
                            return value / 1000; // convert to kW
                        }
                    });
                    datasets.push({
                        label: this.translate.instant("Edge.Index.Widgets.Peakshaving.peakshavingActive"),
                        data: data,
                        hidden: false,

                    });
                    this.colors.push({
                        backgroundColor: "rgba(0,0,0,0)",
                        borderColor: "rgba(0,230,128,1)",
                    });
                }

                if (propertyPeakshavingThresholdPower in result.data) {
                    const data = result.data[propertyPeakshavingThresholdPower].map(value => {
                        if (value == null) {
                            return null;
                        } else if (value == 0) {
                            return 0;
                        } else {
                            return value / 1000; // convert to kW
                        }
                    });
                    datasets.push({
                        label: this.translate.instant("Edge.Index.Widgets.Peakshaving.peakShavingThresholdPower"),
                        data: data,
                        hidden: false,
                        borderDash: [3, 3],
                    });
                    this.colors.push({
                        backgroundColor: "rgba(0,0,0,0)",
                        borderColor: "rgba(50,199,199,1)",
                    });
                }
                if (stateMachine in result.data && propertyRechargePower in result.data && propertyPeakshavingPower in result.data) {
                    const stateMachineValue = result.data[stateMachine]?.[0]; // Nimmt den ersten Wert von StateMachine
                    const rechargePowerData = result.data[propertyRechargePower].map(value => value ? value / 1000 : null);
                    const peakShavingPowerData = result.data[propertyPeakshavingPower].map(value => value ? value / 1000 : null);

                    let fillColor;
                    switch (stateMachineValue) {
                        case -1:
                            fillColor = "rgba(255, 0, 0, 0.2)"; // UNDEFINED
                            break;
                        case 0:
                            fillColor = "rgba(0, 0, 0, 0)"; // StandBy - transparent
                            break;
                        case 1:
                            fillColor = "rgba(255, 0, 0, 0.2)"; // ERROR
                            break;
                        case 2:
                            fillColor = "rgba(255, 223, 0, 0.2)"; // Peakshaver active
                            break;
                        case 3:
                            fillColor = "rgba(0, 223, 0, 0.2)"; // Charging active
                            break;
                        default:
                            fillColor = "rgba(150,199,199,0.2)"; // Standardfarbe (z.B. Blau) für andere Zustände
                    }

                    // Untere Linie (propertyRechargePower)
                    datasets.push({
                        label: this.translate.instant("Edge.Index.Widgets.Peakshaving.rechargePower"),
                        data: rechargePowerData,
                        borderColor: "rgba(50,199,199,1)",
                        backgroundColor: "rgba(0,0,0,0)", // Keine Füllfarbe für die Linie selbst
                        fill: false, // Kein Füllen von dieser Linie aus
                    });
                    this.colors.push({
                        backgroundColor: "rgba(0,0,0,0)",
                        borderColor: "rgba(50,199,199,1)"
                    });

                    // Obere Linie (propertyPeakshavingPower) mit Füllung nach unten
                    datasets.push({
                        label: this.translate.instant("Edge.Index.Widgets.Peakshaving.peakshavingPower"),
                        data: peakShavingPowerData,
                        borderColor: "rgba(150,199,199,1)",
                        backgroundColor: fillColor, // Transparente Füllfarbe für den Bereich zwischen den Linien
                        fill: "-1", // Fülle den Bereich bis zur unteren Linie (rechargePower)
                    });
                    this.colors.push({
                        backgroundColor: fillColor, // Füllfarbe für den Bereich
                        borderColor: "rgba(150,199,199,1)",
                    });
                } else {


                    if (propertyPeakshavingPower in result.data) {
                        const data = result.data[propertyPeakshavingPower].map(value => {
                            if (value == null) {
                                return null;
                            } else if (value == 0) {
                                return 0;
                            } else {
                                return value / 1000; // convert to kW
                            }
                        });
                        datasets.push({
                            label: this.translate.instant("Edge.Index.Widgets.Peakshaving.peakshavingPower"),
                            data: data,
                            hidden: false,
                            borderDash: [3, 3],
                        });
                        this.colors.push({
                            backgroundColor: "rgba(0,0,0,0)",
                            borderColor: "rgba(150,199,199,1)",
                        });
                    }
                    if (propertyRechargePower in result.data) {
                        const data = result.data[propertyRechargePower].map(value => {
                            if (value == null) {
                                return null;
                            } else if (value == 0) {
                                return 0;
                            } else {
                                return value / 1000; // convert to kW
                            }
                        });
                        datasets.push({
                            label: this.translate.instant("Edge.Index.Widgets.Peakshaving.rechargePower"),
                            data: data,
                            hidden: false,
                            borderDash: [3, 3],
                        });
                        this.colors.push({
                            backgroundColor: "rgba(0,0,0,0)",
                            borderColor: "rgba(200,199,199,1)",
                        });
                    }
                }

                if (essSoc in result.data) {
                    const data = result.data[essSoc].map(value => {
                        if (value == null) {
                            return null;
                        } else if (value == 0) {
                            return 0;
                        } else {
                            return value; // conversion necessary?
                        }
                    });
                    datasets.push({
                        label: this.translate.instant("General.soc"),
                        data: data,
                        hidden: false,
                        borderDash: [3, 3],
                    });
                    this.colors.push({
                        backgroundColor: "rgba(0,223,0,0.05)",
                        borderColor: "rgba(0,223,0,1)",
                    });
                }

                if (essPower in result.data) {
                    /*
                     * Storage Charge
                     */
                    const chargeData = result.data[essPower].map(value => {
                        if (value == null) {
                            return null;
                        } else if (value < 0) {
                            return value / -1000; // convert to kW;
                        } else {
                            return 0;
                        }
                    });
                    datasets.push({
                        label: this.translate.instant("General.chargePower"),
                        data: chargeData,
                        borderDash: [10, 10],
                    });
                    this.colors.push({
                        backgroundColor: "rgba(0,223,0,0.05)",
                        borderColor: "rgba(0,223,0,1)",
                    });
                    /*
                     * Storage Discharge
                     */
                    const dischargeData = result.data[essPower].map(value => {
                        if (value == null) {
                            return null;
                        } else if (value > 0) {
                            return value / 1000; // convert to kW
                        } else {
                            return 0;
                        }
                    });
                    datasets.push({
                        label: this.translate.instant("General.dischargePower"),
                        data: dischargeData,
                        borderDash: [10, 10],
                    });
                    this.colors.push({
                        backgroundColor: "rgba(200,0,0,0.05)",
                        borderColor: "rgba(200,0,0,1)",
                    });
                }
                this.datasets = datasets;
                this.loading = false;
                this.stopSpinner();

            }).catch(reason => {
                console.error(reason); // TODO error message
                this.initializeChart();
                return;
            });

        }).catch(reason => {
            console.error(reason); // TODO error message
            this.initializeChart();
            return;
        }).finally(async () => {
            this.unit = YAxisType.ENERGY;
            await this.setOptions(this.options);
        });
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            const result: ChannelAddress[] = [
                new ChannelAddress(this.componentId, "_PropertyRechargePower"),
                new ChannelAddress(this.componentId, "_PropertyPeakShavingThresholdPower"),
                new ChannelAddress(this.componentId, "_PropertyPeakShavingPower"),
                new ChannelAddress(this.componentId, "StateMachine"),
                new ChannelAddress(this.componentId, "PeakShavedPower"),
                new ChannelAddress(this.componentId, "PeakShavingTargetPower"),
                new ChannelAddress(this.componentId, "EssSoc"),
                new ChannelAddress(this.componentId, "EssPower"),
                new ChannelAddress(config.getComponent(this.componentId).properties["meter.id"], "ActivePower"),
                //new ChannelAddress("_sum", "EssActivePower"),
            ];
            resolve(result);
        });
    }

    protected setLabel() {
        this.options = this.createDefaultChartOptions();
    }

}
