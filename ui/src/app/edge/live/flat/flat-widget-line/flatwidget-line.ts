import { Component, Input, TemplateRef, ViewChild, ViewContainerRef } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";


export abstract class WidgetLine {
    // subscribe

}
@Component({
    selector: 'flat-widget-line',
    template: ` 
    <ng-template #content> 
        <ng-container *ngIf="edge">
            <table class="full_width" *ngIf="edge.currentData | async as currentData">
                <tr content>
                    <td *ngIf="title"style="width:65%" translate>{{title}}</td>
                    <td style="width:35%" class="align_right" *ngIf="currentData.channel != null &&title && channels  &&(edge.currentData | async) as currentData">
                        <ng-container *ngIf="channels!= null">
                            {{ currentData.channel[channels]   | unitvalue:'kW'}}  
                        </ng-container>
                    </td>
                </tr>
            </table>
        </ng-container>
    </ng-template>
    <ng-template #empty>
        <td style="width:35%;" class="align_right">-</td>
    </ng-template>
`,
})
export class FlatWidgetLine extends WidgetLine {

    @ViewChild('content', { static: true }) content;
    @ViewChild('header', { static: true }) header;

    public edge: Edge = null
    @Input() title: string;
    public essComponents: EdgeConfig.Component[] = null;
    @Input() icon: string;
    @Input() item: string;
    @Input() channels: string;
    @Input() someparam?: string;
    @Input() percentagebar: string;
    @Input() value: string;
    static SELECTOR: string = 'flat-widget-line';

    constructor(
        private route: ActivatedRoute,
        private service: Service,
        private viewContainerRef: ViewContainerRef,
        private websocket: Websocket,
    ) {
        super();
    }
    ngOnInit() {
        this.viewContainerRef.createEmbeddedView(this.content);
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        });
    }

}
