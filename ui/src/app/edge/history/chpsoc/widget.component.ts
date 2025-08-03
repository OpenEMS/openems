import { Component, Input, OnChanges, OnDestroy, OnInit, inject } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { QueryHistoricTimeseriesDataResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";

import { ChannelAddress, Edge, EdgeConfig, Service } from "../../../shared/shared";
import { AbstractHistoryWidget } from "../abstracthistorywidget";
import { calculateActiveTimeOverPeriod } from "../shared";

@Component({
    selector: ChpSocWidgetComponent.SELECTOR,
    templateUrl: "./widget.component.html",
    standalone: false,
})
export class ChpSocWidgetComponent extends AbstractHistoryWidget implements OnInit, OnChanges, OnDestroy {
    override service: Service;
    private route = inject(ActivatedRoute);


    private static readonly SELECTOR = "chpsocWidget";
    @Input({ required: true }) public period!: DefaultTypes.HistoryPeriod;
    @Input({ required: true }) public componentId!: string;

    public activeSecondsOverPeriod: number | null = null;
    public edge: Edge | null = null;
    public component: EdgeConfig.Component | null = null;

    /** Inserted by Angular inject() migration for backwards compatibility */
    constructor(...args: unknown[]);

    constructor() {
        const service = inject(Service);

        super(service);
    
        this.service = service;
    }

    ngOnInit() {
        this.service.getCurrentEdge().then(edge => {
            this.edge = edge;
            this.service.getConfig().then(config => {
                this.component = config.getComponent(this.componentId);
            });
        });
    }

    ngOnDestroy() {
        this.unsubscribeWidgetRefresh();
    }

    ngOnChanges() {
        this.updateValues();
    }

    // Gather result & timestamps to calculate effective active time in %
    protected updateValues() {
        this.queryHistoricTimeseriesData(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to).then(response => {
            this.service.getConfig().then(config => {
                const result = (response as QueryHistoricTimeseriesDataResponse).result;
                const outputChannel = ChannelAddress.fromString(config.getComponentProperties(this.componentId)["outputChannelAddress"]);
                this.activeSecondsOverPeriod = calculateActiveTimeOverPeriod(outputChannel, result);
            });
        });
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            const outputChannel = ChannelAddress.fromString(config.getComponentProperties(this.componentId)["outputChannelAddress"]);
            const channeladdresses = [outputChannel];
            resolve(channeladdresses);
        });
    }
}

