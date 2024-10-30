// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { DefaultTypes } from "src/app/shared/service/defaulttypes";
import { YAxisType } from "src/app/shared/service/utils";

import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from "../../../../shared/shared";
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
                //const propertyPeakshavingThresholdPower = this.componentId + "/_PropertyPeakShavingThresholdPower";
                const propertyPeakshavingPower = this.componentId + "/_PropertyPeakShavingPower";
                const propertyRechargePower = this.componentId + "/_PropertyRechargePower";
                const stateMachine = this.componentId + "/StateMachine";
                const peakShavedPower = this.componentId + "/PeakShavedPower";
                const peakShavingTargetPower = this.componentId + "/PeakShavingTragetPower";
                const result = response.result;

                console.log("Debug Result:", result); // <-- Hier wird das Resultat geloggt
                console.log("Keys in result.data:", Object.keys(result.data)); // Check if expected keys are present

                console.log("Debug Result:", peakShavedPower); // <-- Hier wird das Resultat geloggt

                Object.keys(result.data).forEach(key => {
                    if (key != stateMachine && key != meterIdActivePower) {
                        result.data[stateMachine].forEach((value, stateIndex) => {
                            if (value != 3) {
                                result.data[key][stateIndex] = null;
                            }
                        });
                    }
                });

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
                        borderColor: "rgba(200,0,0,1)",
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
                        borderDash: [3, 3],
                    });
                    this.colors.push({
                        backgroundColor: "rgba(0,0,0,0)",
                        borderColor: "rgba(200,0,0,1)",
                    });
                }
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
                if ("_sum/EssActivePower" in result.data) {
                    /*
                     * Storage Charge
                     */
                    let effectivePower;
                    if ("_sum/ProductionDcActualPower" in result.data && result.data["_sum/ProductionDcActualPower"].length > 0) {
                        effectivePower = result.data["_sum/ProductionDcActualPower"].map((value, index) => {
                            return Utils.subtractSafely(result.data["_sum/EssActivePower"][index], value);
                        });
                    } else {
                        effectivePower = result.data["_sum/EssActivePower"];
                    }
                    const chargeData = effectivePower.map(value => {
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
                    const dischargeData = effectivePower.map(value => {
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
                new ChannelAddress(this.componentId, "_PropertyPeakShavingPower"),
                new ChannelAddress(this.componentId, "StateMachine"),
                new ChannelAddress(this.componentId, "PeakShavedPower"),
                new ChannelAddress(this.componentId, "PeakShavingTargetPower"),
                new ChannelAddress(config.getComponent(this.componentId).properties["meter.id"], "ActivePower"),
                new ChannelAddress("_sum", "ProductionDcActualPower"),
                new ChannelAddress("_sum", "EssActivePower"),
            ];
            resolve(result);
        });
    }

    protected setLabel() {
        this.options = this.createDefaultChartOptions();
    }

}
