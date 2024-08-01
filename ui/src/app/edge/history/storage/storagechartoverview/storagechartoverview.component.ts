// @ts-strict-ignore
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, EdgeConfig, Service, Utils } from '../../../../shared/shared';

@Component({
    selector: StorageChartOverviewComponent.SELECTOR,
    templateUrl: './storagechartoverview.component.html',
})
export class StorageChartOverviewComponent implements OnInit {

    private static readonly SELECTOR = "storage-chart-overview";

    public edge: Edge = null;

    public essComponents: EdgeConfig.Component[] = null;
    public chargerComponents: EdgeConfig.Component[] = null;

    public showPhases: boolean = false;
    public showTotal: boolean = false;
    public isOnlyChart = null;

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
                this.essComponents = config.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss").filter(component => !component.factoryId.includes("Ess.Cluster"));
                this.chargerComponents = config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger");
                if (this.essComponents != null && this.essComponents.length == 1) {
                    this.isOnlyChart = true;
                } else if (this.essComponents.length > 1) {
                    // initialize total view only if more than one ess component
                    this.showTotal = false;
                    this.isOnlyChart = false;
                } else {
                    this.isOnlyChart = false;
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
