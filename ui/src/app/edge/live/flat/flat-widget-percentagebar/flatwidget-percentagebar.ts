import { Component, EventEmitter, Input, Output, TemplateRef, ViewChild, ViewContainerRef } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { CurrentData } from "src/app/shared/edge/currentdata";
import { Edge, Service } from "src/app/shared/shared";


@Component({
    selector: 'flat-widget-percentagebar',
    template: ` 
    <ng-template #percentagebar>
        <ng-container *ngIf="edge">
    <table class="full_width" *ngIf="edge.currentData | async as currentData">
        <tr content>
            <td> 
            <svg width="100%" height="20" *ngIf="channels">
    <rect width="100%" rx="5" ry="5" height="20" style="fill:#f4f4f4" />
    <rect x="1" y="2" rx="5" ry="5" attr.width="{{currentData.channel[channels]}}%" height="16" style="fill:#2d8fab" />
    <!-- <text x="50%" y="58%" dominant-baseline="middle" text-anchor="middle" style="font-weight: 500">{{ currentData.channel[channels] |
        unitvalue:'%' }}</text> -->
        <text x="50%" y="58%" dominant-baseline="middle" text-anchor="middle" style="font-weight: 500">{{ (currentData.channel[channels] ) |
        unitvalue:'%' }}</text>
</svg> 
<svg width="100%" height="20" *ngIf="summaryChannel">
    <rect width="100%" rx="5" ry="5" height="20" style="fill:#f4f4f4" />
    <rect x="1" y="2" rx="5" ry="5" attr.width="{{summaryChannel}}%" height="16" style="fill:#2d8fab" />
    <!-- <text x="50%" y="58%" dominant-baseline="middle" text-anchor="middle" style="font-weight: 500">{{ currentData.channel[channels] |
        unitvalue:'%' }}</text> -->
        <text x="50%" y="58%" dominant-baseline="middle" text-anchor="middle" style="font-weight: 500">{{ (summaryChannel ) |
        unitvalue:'%' }}</text>
</svg> 
            </td>
        </tr>
</table>
    </ng-container>
    </ng-template>
`,
})


export class FlatWidgetPercentagebar {

    @ViewChild('percentagebar', { static: true }) percentagebar;

    private something: string = 'system.autarchy'
    public edge: Edge = null;
    public channelexample: string;
    public summaryChannelexample: string;
    @Input() channels: string;
    @Input() summaryPath: string;
    @Output() executeLogic: EventEmitter<any> = new EventEmitter();
    @Input() summaryChannel: number;


    constructor(
        private route: ActivatedRoute,
        private service: Service,
        private viewContainerRef: ViewContainerRef
    ) {
    }
    ngOnInit() {
        console.log("summaryChannel +++++++++++++", this.summaryChannel)
        this.viewContainerRef.createEmbeddedView(this.percentagebar);
        console.log("service", this.service);

        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;

            let result = this.executeLogic.emit([this.edge.currentData]);
            console.log("Result ##### B - ) ", result);
            this.edge.currentData.subscribe(currentData => {
                console.log("autarchy: ", currentData.summary.system.autarchy);
            });
        });

        console.log(this.percentagebar);
    }

}
