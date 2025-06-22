import { Component, Input, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";

@Component({
    selector: TimeslotPeakshavingWidgetComponent.SELECTOR,
    templateUrl: "./widget.component.html",
    standalone: false,
})
export class TimeslotPeakshavingWidgetComponent implements OnInit {

    private static readonly SELECTOR = "timeslotPeakshavingWidget";
    @Input({ required: true }) public period!: DefaultTypes.HistoryPeriod;
    @Input({ required: true }) public componentId!: string;

    public edge: Edge | null = null;
    public component: EdgeConfig.Component | null = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.getCurrentEdge().then(edge => {
            this.edge = edge;
            this.service.getConfig().then(config => {
                this.component = config.getComponent(this.componentId);
            });
        });
    }
}

