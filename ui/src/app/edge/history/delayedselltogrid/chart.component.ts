// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";
import { YAxisType } from "src/app/shared/utils/utils";

import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from "../../../shared/shared";
import { AbstractHistoryChart } from "../abstracthistorychart";

@Component({
    selector: "delayedselltogridgchart",
    templateUrl: "../ABSTRACTHISTORYCHART.HTML",
    standalone: false,
})
export class DelayedSellToGridChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input({ required: true }) public period!: DEFAULT_TYPES.HISTORY_PERIOD;
    @Input({ required: true }) public componentId!: string;


    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        private route: ActivatedRoute,
    ) {
        super("delayedsellTogrid-chart", service, translate);
    }

    ngOnChanges() {
        THIS.UPDATE_CHART();
    }

    ngOnInit() {
        THIS.START_SPINNER();
    }

    ngOnDestroy() {
        THIS.UNSUBSCRIBE_CHART_REFRESH();
    }

    public getChartHeight(): number {
        return WINDOW.INNER_HEIGHT / 1.3;
    }

    protected updateChart() {
        THIS.AUTO_SUBSCRIBE_CHART_REFRESH();
        THIS.START_SPINNER();
        THIS.LOADING = true;
        THIS.COLORS = [];
        THIS.QUERY_HISTORIC_TIMESERIES_DATA(THIS.PERIOD.FROM, THIS.PERIOD.TO).then(response => {
            THIS.SERVICE.GET_CONFIG().then(config => {
                const meterIdActivePower = CONFIG.GET_COMPONENT(THIS.COMPONENT_ID).properties["METER.ID"] + "/ActivePower";
                const sellToGridPowerLimit = THIS.COMPONENT_ID + "/_PropertySellToGridPowerLimit";
                const continuousSellToGridPower = THIS.COMPONENT_ID + "/_PropertyContinuousSellToGridPower";
                const result = RESPONSE.RESULT;
                // convert labels
                const labels: Date[] = [];
                for (const timestamp of RESULT.TIMESTAMPS) {
                    LABELS.PUSH(new Date(timestamp));
                }
                THIS.LABELS = labels;

                // convert datasets
                const datasets = [];

                if (meterIdActivePower in RESULT.DATA) {
                    const data = RESULT.DATA[meterIdActivePower].map(value => {
                        if (value == null) {
                            return null;
                        } else if (value < 0) {
                            return (value * -1) / 1000;// convert to kW + positive GridSell values;
                        } else {
                            return 0;
                        }
                    });
                    DATASETS.PUSH({
                        label: THIS.TRANSLATE.INSTANT("GENERAL.GRID_SELL"),
                        data: data,
                        hidden: false,
                    });
                    THIS.COLORS.PUSH({
                        backgroundColor: "rgba(0,0,0,0.05)",
                        borderColor: "rgba(0,0,0,1)",
                    });
                }
                if (sellToGridPowerLimit in RESULT.DATA) {
                    const data = RESULT.DATA[sellToGridPowerLimit].map(value => {
                        if (value == null) {
                            return null;
                        } else if (value == 0) {
                            return 0;
                        } else {
                            return value / 1000; // convert to kW
                        }
                    });
                    DATASETS.PUSH({
                        label: THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.DELAYED_SELL_TO_GRID.SELL_TO_GRID_POWER_LIMIT"),
                        data: data,
                        hidden: false,
                        borderDash: [3, 3],
                    });
                    THIS.COLORS.PUSH({
                        backgroundColor: "rgba(0,0,0,0)",
                        borderColor: "rgba(0,223,0,1)",
                    });
                }
                if (continuousSellToGridPower in RESULT.DATA) {
                    const data = RESULT.DATA[continuousSellToGridPower].map(value => {
                        if (value == null) {
                            return null;
                        } else if (value == 0) {
                            return 0;
                        } else {
                            return value / 1000; // convert to kW
                        }
                    });
                    DATASETS.PUSH({
                        label: THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.DELAYED_SELL_TO_GRID.CONTINUOUS_SELL_TO_GRID_POWER"),
                        data: data,
                        hidden: false,
                        borderDash: [3, 3],
                    });
                    THIS.COLORS.PUSH({
                        backgroundColor: "rgba(0,0,0,0)",
                        borderColor: "rgba(200,0,0,1)",
                    });
                }
                if ("_sum/EssActivePower" in RESULT.DATA) {
                    /*
                     * Storage Charge
                     */
                    let effectivePower;
                    if ("_sum/ProductionDcActualPower" in RESULT.DATA && RESULT.DATA["_sum/ProductionDcActualPower"].length > 0) {
                        effectivePower = RESULT.DATA["_sum/ProductionDcActualPower"].map((value, index) => {
                            return UTILS.SUBTRACT_SAFELY(RESULT.DATA["_sum/EssActivePower"][index], value);
                        });
                    } else {
                        effectivePower = RESULT.DATA["_sum/EssActivePower"];
                    }
                    const chargeData = EFFECTIVE_POWER.MAP(value => {
                        if (value == null) {
                            return null;
                        } else if (value < 0) {
                            return value / -1000; // convert to kW;
                        } else {
                            return 0;
                        }
                    });
                    DATASETS.PUSH({
                        label: THIS.TRANSLATE.INSTANT("GENERAL.CHARGE"),
                        data: chargeData,
                        borderDash: [10, 10],
                    });
                    THIS.COLORS.PUSH({
                        backgroundColor: "rgba(0,223,0,0.05)",
                        borderColor: "rgba(0,223,0,1)",
                    });
                    /*
                     * Storage Discharge
                     */
                    const dischargeData = EFFECTIVE_POWER.MAP(value => {
                        if (value == null) {
                            return null;
                        } else if (value > 0) {
                            return value / 1000; // convert to kW
                        } else {
                            return 0;
                        }
                    });
                    DATASETS.PUSH({
                        label: THIS.TRANSLATE.INSTANT("GENERAL.DISCHARGE"),
                        data: dischargeData,
                        borderDash: [10, 10],
                    });
                    THIS.COLORS.PUSH({
                        backgroundColor: "rgba(200,0,0,0.05)",
                        borderColor: "rgba(200,0,0,1)",
                    });
                }
                THIS.DATASETS = datasets;
                THIS.LOADING = false;
                THIS.STOP_SPINNER();

            }).catch(reason => {
                CONSOLE.ERROR(reason); // TODO error message
                THIS.INITIALIZE_CHART();
                return;
            });

        }).catch(reason => {
            CONSOLE.ERROR(reason); // TODO error message
            THIS.INITIALIZE_CHART();
            return;
        }).finally(() => {
            THIS.UNIT = YAXIS_TYPE.ENERGY;
            THIS.SET_OPTIONS(THIS.OPTIONS);
        });
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            const result: ChannelAddress[] = [
                new ChannelAddress(THIS.COMPONENT_ID, "_PropertySellToGridPowerLimit"),
                new ChannelAddress(THIS.COMPONENT_ID, "_PropertyContinuousSellToGridPower"),
                new ChannelAddress(CONFIG.GET_COMPONENT(THIS.COMPONENT_ID).properties["METER.ID"], "ActivePower"),
                new ChannelAddress("_sum", "ProductionDcActualPower"),
                new ChannelAddress("_sum", "EssActivePower"),
            ];
            resolve(result);
        });
    }

    protected setLabel() {
        THIS.OPTIONS = THIS.CREATE_DEFAULT_CHART_OPTIONS();
    }

}
