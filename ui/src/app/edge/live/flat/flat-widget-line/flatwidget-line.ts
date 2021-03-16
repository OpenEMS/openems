import { Component, Input, TemplateRef, ViewChild, ViewContainerRef } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";

@Component({
    selector: 'flat-widget-line',
    templateUrl: './flatwidget-line.html'
})
/** FlatWidgetLine is one line in FlatWidget. you can give him a title, a title_type, title_value, title_value_type, a channel and a value. */
export class FlatWidgetLine {

    /** SELECTOR defines, how to call this Widget */
    static SELECTOR: string = 'flat-widget-line';
    /** ViewChild selects the ng-teplate with certain ID */
    @ViewChild('content', { static: true }) content;
    /** Title for parameter, displayed on the left side*/
    @Input() title: string;
    /** Title_Type specifies, if there is a title to translate */
    @Input() title_type: string;
    /** Title_value defines value of the parameter, displayed on the right */
    @Input() title_value: string;
    /** Title_value_type specifies, if there is a title to translate */
    @Input() title_value_type: string;
    /** Channel defines the channel, you need for this line */
    @Input() channel: string;
    /** value get used, if the channelAddress is not starting with currentData.channel, but your unit is kw. You have to hand over the whole path.  */
    @Input() value: string;

    public edge: Edge = null
    public essComponents: EdgeConfig.Component[] = null;

    constructor(
        private route: ActivatedRoute,
        private service: Service,
        private viewContainerRef: ViewContainerRef,

    ) {
    }
    ngOnInit() {
        /** attaches content with viewContainerRef to component*/
        this.viewContainerRef.createEmbeddedView(this.content);
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        });
    }
}
