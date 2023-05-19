import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, EdgeConfig, Service } from '../../../../shared/shared';

@Component({
    selector: TimeOfUseTariffDischargeChartOverviewComponent.SELECTOR,
    templateUrl: './timeofusetariffdischargechartoverview.component.html'
})
export class TimeOfUseTariffDischargeChartOverviewComponent implements OnInit {

    private static readonly SELECTOR = "timeofusetariffdischarge-chart-overview";

    public edge: Edge = null;

    public component: EdgeConfig.Component = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.service.getConfig().then(config => {
                this.edge = edge;
                this.component = config.getComponent(this.route.snapshot.params.componentId);
            });
        });
    }
}