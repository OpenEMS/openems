// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { Cumulated } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { DefaultTypes } from "src/app/shared/service/defaulttypes";
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from "../../../shared/shared";
import { AbstractHistoryWidget } from "../abstracthistorywidget";

@Component({
    selector: StorageComponent.SELECTOR,
    templateUrl: "./widget.component.html",
})
export class StorageComponent extends AbstractHistoryWidget implements OnInit, OnChanges, OnDestroy {

    private static readonly SELECTOR = "storageWidget";
    @Input({ required: true }) public period!: DefaultTypes.HistoryPeriod;

    // reference to the Utils method to access via html
    public isLastElement = Utils.isLastElement;

    public data: Cumulated | null = null;
    public edge: Edge | null = null;
    public essComponents: EdgeConfig.Component[] = [];

    constructor(
        public override service: Service,
        private route: ActivatedRoute,

    ) {
        super(service);
    }

    ngOnInit() {
        this.service.setCurrentComponent("", this.route).then(response => {
            this.edge = response;
        });
    }

    ngOnDestroy() {
        this.unsubscribeWidgetRefresh();
    }

    ngOnChanges() {
        this.updateValues();
    }

    protected updateValues() {
        this.service.getConfig().then(config => {
            this.getChannelAddresses(this.edge, config).then(channels => {
                this.service.queryEnergy(this.period.from, this.period.to, channels).then(response => {
                    this.data = response.result.data;
                }).catch(() => {
                    this.data = null;
                });
            });
        });
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            const channels: ChannelAddress[] = [];
            channels.push(
                new ChannelAddress("_sum", "EssDcChargeEnergy"),
                new ChannelAddress("_sum", "EssDcDischargeEnergy"),
            );
            resolve(channels);
        });
    }
}
