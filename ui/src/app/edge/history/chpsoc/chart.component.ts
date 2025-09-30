// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";
import { YAxisType } from "src/app/shared/utils/utils";
import { ChannelAddress, Edge, EdgeConfig, Service } from "../../../shared/shared";
import { AbstractHistoryChart } from "../abstracthistorychart";

@Component({
    selector: "chpsocchart",
    templateUrl: "../ABSTRACTHISTORYCHART.HTML",
    standalone: false,
})
export class ChpSocChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input({ required: true }) public period!: DEFAULT_TYPES.HISTORY_PERIOD;
    @Input({ required: true }) public componentId!: string;

    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        private route: ActivatedRoute,
    ) {
        super("chpsoc-chart", service, translate);
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
        THIS.QUERY_HISTORIC_TIMESERIES_DATA(THIS.PERIOD.FROM, THIS.PERIOD.TO).then(response => {
            THIS.SERVICE.GET_CURRENT_EDGE().then(() => {
                THIS.SERVICE.GET_CONFIG().then(config => {
                    const outputChannel = CONFIG.GET_COMPONENT_PROPERTIES(THIS.COMPONENT_ID)["outputChannelAddress"];
                    const inputChannel = CONFIG.GET_COMPONENT_PROPERTIES(THIS.COMPONENT_ID)["inputChannelAddress"];
                    const lowThreshold = THIS.COMPONENT_ID + "/_PropertyLowThreshold";
                    const highThreshold = THIS.COMPONENT_ID + "/_PropertyHighThreshold";
                    const result = RESPONSE.RESULT;
                    // convert labels
                    const labels: Date[] = [];
                    for (const timestamp of RESULT.TIMESTAMPS) {
                        LABELS.PUSH(new Date(timestamp));
                    }
                    THIS.LABELS = labels;

                    // convert datasets
                    const datasets = [];

                    // convert datasets
                    for (const channel in RESULT.DATA) {
                        if (channel == outputChannel) {
                            const address = CHANNEL_ADDRESS.FROM_STRING(channel);
                            const data = RESULT.DATA[channel].map(value => {
                                if (value == null) {
                                    return null;
                                } else {
                                    return value * 100; // convert to % [0,100]
                                }
                            });
                            DATASETS.PUSH({
                                label: ADDRESS.CHANNEL_ID,
                                data: data,
                            });
                            THIS.COLORS.PUSH({
                                backgroundColor: "rgba(0,191,255,0.05)",
                                borderColor: "rgba(0,191,255,1)",
                            });
                        } else {
                            const data = RESULT.DATA[channel].map(value => {
                                if (value == null) {
                                    return null;
                                } else if (value > 100 || value < 0) {
                                    return null;
                                } else {
                                    return value;
                                }
                            });
                            if (channel == inputChannel) {
                                DATASETS.PUSH({
                                    label: THIS.TRANSLATE.INSTANT("GENERAL.SOC"),
                                    data: data,
                                });
                                THIS.COLORS.PUSH({
                                    backgroundColor: "rgba(0,0,0,0)",
                                    borderColor: "rgba(0,223,0,1)",
                                });
                            }
                            if (channel == lowThreshold) {
                                DATASETS.PUSH({
                                    label: THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.CHP.LOW_THRESHOLD"),
                                    data: data,
                                    borderDash: [3, 3],
                                });
                                THIS.COLORS.PUSH({
                                    backgroundColor: "rgba(0,0,0,0)",
                                    borderColor: "rgba(0,191,255,1)",
                                });
                            }
                            if (channel == highThreshold) {
                                DATASETS.PUSH({
                                    label: THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.CHP.HIGH_THRESHOLD"),
                                    data: data,
                                    borderDash: [3, 3],
                                });
                                THIS.COLORS.PUSH({
                                    backgroundColor: "rgba(0,0,0,0)",
                                    borderColor: "rgba(0,191,255,1)",
                                });
                            }
                        }
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
            });
        }).catch(reason => {
            CONSOLE.ERROR(reason); // TODO error message
            THIS.INITIALIZE_CHART();
            return;
        }).finally(() => {
            THIS.UNIT = YAXIS_TYPE.PERCENTAGE;
            THIS.SET_OPTIONS(THIS.OPTIONS);
        });
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            const outputChannel = CHANNEL_ADDRESS.FROM_STRING(CONFIG.GET_COMPONENT_PROPERTIES(THIS.COMPONENT_ID)["outputChannelAddress"]);
            const inputChannel = CHANNEL_ADDRESS.FROM_STRING(CONFIG.GET_COMPONENT_PROPERTIES(THIS.COMPONENT_ID)["inputChannelAddress"]);
            const result: ChannelAddress[] = [
                outputChannel,
                inputChannel,
                new ChannelAddress(THIS.COMPONENT_ID, "_PropertyHighThreshold"),
                new ChannelAddress(THIS.COMPONENT_ID, "_PropertyLowThreshold"),
            ];
            resolve(result);
        });
    }

    protected setLabel() {
        THIS.OPTIONS = THIS.CREATE_DEFAULT_CHART_OPTIONS();
    }

}
