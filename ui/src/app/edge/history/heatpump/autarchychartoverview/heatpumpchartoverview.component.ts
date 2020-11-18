import { ActivatedRoute } from '@angular/router';
import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service, Edge } from '../../../../shared/shared';

@Component({
    selector: HeatPumptChartOverviewComponent.SELECTOR,
    templateUrl: './heatpumpchartoverview.component.html'
})
export class HeatPumptChartOverviewComponent {

    private static readonly SELECTOR = "heatpumpt-chart-overview";

    public edge: Edge = null;

    constructor(
        public service: Service,
        public modalCtrl: ModalController,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        });
    }
}