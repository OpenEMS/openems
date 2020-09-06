import { Component } from '@angular/core';
import { EdgeConfig, Service, Edge } from '../../../../shared/shared';
import { ActivatedRoute } from '@angular/router';

@Component({
    selector: ConsumptionChartOverviewComponent.SELECTOR,
    templateUrl: './consumptionchartoverview.component.html'
})
export class ConsumptionChartOverviewComponent {

    private static readonly SELECTOR = "consumption-chart-overview";

    public edge: Edge | null = null;

    public evcsComponents: EdgeConfig.Component[] = [];
    public consumptionMeterComponents: EdgeConfig.Component[] = [];
    public showPhases: boolean = false;
    public showTotal: boolean | null = null;
    public isOnlyChart: boolean | null = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.service.getConfig().then(config => {
                this.edge = edge;
                this.evcsComponents = config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs").filter(component => !(component.factoryId == 'Evcs.Cluster' || component.factoryId == 'Evcs.Cluster.PeakShaving' || component.factoryId == 'Evcs.Cluster.SelfConsumtion'))
                this.consumptionMeterComponents = config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter").filter(component => component.properties['type'] == 'CONSUMPTION_METERED');
                // determine if singlechart is the only chart that is shown
                // disable total option to choose for chartoptions component
                if (this.evcsComponents.length > 0) {
                    this.showTotal = false;
                    this.isOnlyChart = false;
                } else if (this.evcsComponents.length == 0) {
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