import { ActivatedRoute } from '@angular/router';
import { Component } from '@angular/core';
import { EdgeConfig, Service, Utils, Edge } from '../../../../shared/shared';

@Component({
    selector: ProductionChartOverviewComponent.SELECTOR,
    templateUrl: './productionchartoverview.component.html'
})
export class ProductionChartOverviewComponent {

    private static readonly SELECTOR = "production-chart-overview";

    public edge: Edge = null;
    public config: EdgeConfig = null;

    public productionMeterComponents: EdgeConfig.Component[] = null;
    public chargerComponents: EdgeConfig.Component[] = null;
    public showTotal: boolean = null;
    public showPhases: boolean = false;
    public isOnlyChart: boolean = null;

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
                this.productionMeterComponents = config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter").filter(component => config.isProducer(component));
                this.chargerComponents = config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
                if ((this.productionMeterComponents != null && this.productionMeterComponents.length > 0 && this.chargerComponents != null && this.chargerComponents.length > 0)
                    || (this.productionMeterComponents != null && this.productionMeterComponents.length == 0 && this.chargerComponents != null && this.chargerComponents.length > 1)
                    || (this.productionMeterComponents != null && this.productionMeterComponents.length > 1 && this.chargerComponents != null && this.chargerComponents.length == 0)) {
                    this.showTotal = false;
                }
                if (((this.chargerComponents != null && this.chargerComponents.length == 1) && (this.productionMeterComponents != null && this.productionMeterComponents.length == 0))
                    || ((this.productionMeterComponents != null && this.productionMeterComponents.length == 0) && (this.chargerComponents != null && this.chargerComponents.length == 1))) {
                    this.isOnlyChart = true;
                } else {
                    this.isOnlyChart = false;
                }
                if (this.productionMeterComponents != null && this.productionMeterComponents.length == 0) {
                    this.showPhases = null;
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