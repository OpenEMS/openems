import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, EdgeConfig, Service, Utils } from '../../../../shared/shared';

@Component({
    selector: ProductionChartOverviewComponent.SELECTOR,
    templateUrl: './productionchartoverview.component.html'
})
export class ProductionChartOverviewComponent implements OnInit {

    private static readonly SELECTOR = "production-chart-overview";

    public edge: Edge = null;
    public config: EdgeConfig = null;

    public productionMeterComponents: EdgeConfig.Component[] = [];
    public chargerComponents: EdgeConfig.Component[] = [];
    public showTotal: boolean = false;
    public showPhases: boolean = false;
    public isOnlyChart: boolean = false;

    // reference to the Utils method to access via html
    public isLastElement = Utils.isLastElement;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.service.getConfig().then(config => {
                this.edge = edge;
                this.config = config;
                this.productionMeterComponents = config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter").filter(component => config.isProducer(component));
                this.chargerComponents = config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
                if ((this.productionMeterComponents.length > 0 && this.chargerComponents.length > 0)
                    || (this.productionMeterComponents.length == 0 && this.chargerComponents.length > 1)
                    || (this.productionMeterComponents.length > 1 && this.chargerComponents.length == 0)) {
                    this.showTotal = false;
                }
                if ((this.chargerComponents.length == 1 && this.productionMeterComponents.length == 0)
                    || (this.productionMeterComponents.length == 0 && this.chargerComponents.length == 1)) {
                    this.isOnlyChart = true;
                } else {
                    this.isOnlyChart = false;
                }
                if (this.productionMeterComponents.length == 0) {
                    this.showPhases = false;
                }
            })
        })
    }

    onNotifyPhases(showPhases: boolean): void {
        this.showPhases = showPhases;
    }

    onNotifyTotal(showTotal: boolean): void {
        this.showTotal = showTotal;
    }
}