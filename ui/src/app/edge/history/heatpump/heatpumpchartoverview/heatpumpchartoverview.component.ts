import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { Edge, EdgeConfig, Service } from '../../../../shared/shared';

@Component({
    selector: HeatPumpChartOverviewComponent.SELECTOR,
    templateUrl: './heatpumpchartoverview.component.html'
})
export class HeatPumpChartOverviewComponent implements OnInit {

    private static readonly SELECTOR = "heatpump-chart-overview";

    public edge: Edge = null;
    public component: EdgeConfig.Component = null;

    constructor(
        public service: Service,
        public modalCtrl: ModalController,
        private route: ActivatedRoute
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.service.getConfig().then(config => {
                this.component = config.getComponent(this.route.snapshot.params.componentId);
                this.service.getConfig().then(config => {
                    this.edge = edge;
                    this.component = config.getComponent(this.route.snapshot.params.componentId);
                });
            });
        });
    }
}