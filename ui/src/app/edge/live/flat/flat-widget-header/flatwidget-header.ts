import { Component, Input, TemplateRef, ViewChild, ViewContainerRef } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { Edge, Service } from "src/app/shared/shared";


@Component({
    selector: 'flat-widget-header',
    template: ` 
    <ng-template #header>
    <ion-item [color]="color" lines="full">
                <ion-avatar slot="start" *ngIf="icon.includes('assets/img')">
                    <img src={{icon}}>
                </ion-avatar>
               
                <ion-label translate>General.{{title}}</ion-label>
            </ion-item>
    </ng-template>
`,
})


export class FlatWidgetHeader {

    @ViewChild('header', { static: true }) header;


    public edge: Edge = null
    @Input() title: string;
    @Input() icon: string;
    @Input() color: string;


    constructor(
        private route: ActivatedRoute,
        private service: Service,
        private viewContainerRef: ViewContainerRef
    ) {
    }
    ngOnInit() {
        this.viewContainerRef.createEmbeddedView(this.header);
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;

        });
    }

}
