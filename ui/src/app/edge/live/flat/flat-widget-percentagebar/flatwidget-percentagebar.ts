import { Component, Input, TemplateRef, ViewChild, ViewContainerRef } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { Edge, Service } from "src/app/shared/shared";


@Component({
    selector: 'flat-widget-percentagebar',
    template: ` 
    <ng-template #percentagebar>
        <ng-container *ngIf="edge">
            <table class="full_width" *ngIf="edge.currentData | async as currentData">
                <tr content>
                    <td> 
                        <svg width="100%" height="20">
                            <rect width="100%" rx="5" ry="5" height="20" style="fill:#f4f4f4" />
                            <rect *ngIf="channels" x="1" y="2" rx="5" ry="5" attr.width="{{currentData.channel[channels]}}%" height="16" style="fill:#2d8fab" />
                            <text x="50%" y="58%" dominant-baseline="middle" text-anchor="middle" style="font-weight: 500">{{ currentData.channel[channels] |
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
    public edge: Edge = null
    @Input() channels: string;

    constructor(
        private route: ActivatedRoute,
        private service: Service,
        private viewContainerRef: ViewContainerRef
    ) { }
    ngOnInit() {
        this.viewContainerRef.createEmbeddedView(this.percentagebar);
        console.log("service", this.service)
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        });
        console.log(this.percentagebar);
    }
}
