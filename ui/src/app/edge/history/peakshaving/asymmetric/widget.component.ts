// @ts-strict-ignore
import { ActivatedRoute } from '@angular/router';
import { Component, Input, OnInit } from '@angular/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { Edge, Service, EdgeConfig } from 'src/app/shared/shared';

@Component({
    selector: AsymmetricPeakshavingWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html',
})
export class AsymmetricPeakshavingWidgetComponent implements OnInit {

    private static readonly SELECTOR = "asymmetricPeakshavingWidget";
    @Input({ required: true }) public period!: DefaultTypes.HistoryPeriod;
    @Input({ required: true }) public componentId!: string;


    public edge: Edge = null;
    public component: EdgeConfig.Component = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            this.service.getConfig().then(config => {
                this.component = config.getComponent(this.componentId);
            });
        });
    }
}
