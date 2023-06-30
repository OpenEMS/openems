import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, EdgeConfig, Service } from '../../../../shared/shared';

@Component({
    selector: ConsumptionChartOverviewComponent.SELECTOR,
    templateUrl: './consumptionchartoverview.component.html'
})
export class ConsumptionChartOverviewComponent implements OnInit {

    private static readonly SELECTOR = "consumption-chart-overview";

    public edge: Edge = null;

    public evcsComponents: EdgeConfig.Component[] = [];
    public consumptionMeterComponents: EdgeConfig.Component[] = [];
    public showPhases: boolean = false;
    public showTotal: boolean = false;
    public isOnlyChart: boolean = false;

    constructor(
        public service: Service,
        private route: ActivatedRoute
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.service.getConfig().then(config => {
                this.edge = edge;
                this.evcsComponents = config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
                    .filter(component =>
                        !(component.factoryId == 'Evcs.Cluster'
                            || component.factoryId == 'Evcs.Cluster.PeakShaving'
                            || component.factoryId == 'Evcs.Cluster.SelfConsumption'));
                this.consumptionMeterComponents = config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
                    .filter(component => config.isTypeConsumptionMetered(component));
                // determine if singlechart is the only chart that is shown
                // disable total option to choose for chartoptions component
                if (this.evcsComponents.length > 0 || this.consumptionMeterComponents.length > 0) {
                    this.showTotal = true;
                    this.isOnlyChart = false;
                } else {
                    this.isOnlyChart = true;
                }
            });
        });
    }

    onNotifyPhases(showPhases: boolean): void {
        this.showPhases = showPhases;
    }

    onNotifyTotal(showTotal: boolean): void {
        this.showTotal = showTotal;
    }
}