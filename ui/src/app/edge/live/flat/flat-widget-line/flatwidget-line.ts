import { Component, Input, TemplateRef, ViewChild, ViewContainerRef } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { Edge, Service } from "src/app/shared/shared";

@Component({
    selector: 'flat-widget-line',
    template: ` 
    <ng-template #template>
    <table class="full_width" *ngIf="edge.currentData | async as currentData">
        <tr content>
            <td style="width:65%" translate>General.{{title}}</td>
            <td style="width:35%" class="align_right" *ngIf="(edge.currentData | async) as currentData">
                {{ currentData['channel'][channel] | unitvalue:'kW'}} 
</td>
        </tr>
</table>
    </ng-template>
`,
})

export class FlatWidgetLine {

    @ViewChild('template', { static: true }) template;

    public something: string = '<div>test</div>'

    public edge: Edge = null
    @Input() test: string;
    @Input() title: string;
    @Input() icon: string;
    @Input() item: string;
    @Input() channel: string;
    @Input() percentagebarvalue: string;

    constructor(
        private route: ActivatedRoute,
        private service: Service,
        private viewContainerRef: ViewContainerRef
    ) {
    }
    ngOnInit() {
        this.viewContainerRef.createEmbeddedView(this.template);
        console.log("service", this.service)
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            console.log("edge", this.edge);

        });
        console.log(this.template);
    }

}
