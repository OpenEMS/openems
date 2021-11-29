import { ActivatedRoute } from '@angular/router';
import { Component } from '@angular/core';
import { EdgeConfig, Service, Edge } from '../../../../shared/shared';

@Component({
    selector: ConsumptionChartOverviewComponent.SELECTOR,
    templateUrl: './consumptionchartoverview.component.html'
})
export class ConsumptionChartOverviewComponent {

    private static readonly SELECTOR = "consumption-chart-overview";

    public edge: Edge = null;

    public evcsComponents: EdgeConfig.Component[] = null;
    public consumptionMeterComponents: EdgeConfig.Component[] = null;
    public showPhases: boolean = false;
    public showTotal: boolean = null;
    public isOnlyChart: boolean = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.service.getConfig().then(config => {
                this.edge = edge;
                this.evcsComponents = config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs").filter(component => !(component.factoryId == 'Evcs.Cluster' || component.factoryId == 'Evcs.Cluster.PeakShaving' || component.factoryId == 'Evcs.Cluster.SelfConsumption'))
                this.consumptionMeterComponents = config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter").filter(component => config.isTypeConsumptionMetered(component));
                // determine if singlechart is the only chart that is shown
                // disable total option to choose for chartoptions component
                if (this.evcsComponents.length > 0 || this.consumptionMeterComponents.length > 0) {
                    this.showTotal = true;
                    this.isOnlyChart = false;
                } else {
                    this.isOnlyChart = true;
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