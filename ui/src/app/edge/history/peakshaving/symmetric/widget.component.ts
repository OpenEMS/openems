import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { Edge, EdgeConfig, Service } from 'src/app/shared/shared';

@Component({
    selector: SymmetricPeakshavingWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class SymmetricPeakshavingWidgetComponent implements OnInit {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() public componentId: string;

    private static readonly SELECTOR = "symmetricPeakshavingWidget";

    public edge: Edge = null;
    public component: EdgeConfig.Component = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute
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

