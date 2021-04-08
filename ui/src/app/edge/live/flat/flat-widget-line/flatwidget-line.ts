import { Component, Input, OnDestroy, QueryList, TemplateRef, ViewChild, ViewChildren, ViewContainerRef } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { UUID } from "angular2-uuid";
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { AbstractWidgetLineComponent } from "../../abstractWidgetLine.component";

@Component({
    selector: 'flat-widget-line',
    templateUrl: './flatwidget-line.html'
})
/** FlatWidgetLine is one line in FlatWidget. you can give him a title, a title_type, title_value, title_value_type, a channel and a value. */
export class FlatWidgetLine extends AbstractWidgetLineComponent implements OnDestroy {

    /** SELECTOR defines, how to call this Widget */
    private static SELECTOR: string = 'flat-widget-line';
    /** ViewChild selects the ng-teplate with certain ID */
    @ViewChild('content', { static: true }) content;
    @ViewChildren(TemplateRef) templates: QueryList<TemplateRef<any>>;
    /** Title for parameter, displayed on the left side*/
    @Input() title: string;
    /** Title_Type specifies, if there is a title to translate */
    @Input() title_translate: string;
    /** Title_value defines value of the parameter, displayed on the right */
    @Input() title_value: string;
    /** Title_value_type specifies, if there is a title_value to translate */
    @Input() title_value_translate: string;
    /** Channel defines the channel, you need for this line */
    @Input() channel: string;
    /** value get used, if the channelAddress is not starting with currentData.channel, but your unit is kw. You have to hand over the whole path.  */
    @Input() value: string;
    /** unit is set, when other 'format','otherUnit' or 'value' than default is needed. Default: no format, no value, unit is kW*/
    @Input() unit: Unit = null;
    /** title_addition is used, when your parametertitle is not only a string, but also needs data from variable or channel */
    @Input() title_addition: string;

    public edge: Edge = null
    public essComponents: EdgeConfig.Component[] = null;
    public channelAddresses: ChannelAddress[] = [];
    public randomSelector: string;
    public somevalue: string;

    constructor(
        public route: ActivatedRoute,
        public service: Service,
        public viewContainerRef: ViewContainerRef,
        websocket: Websocket,
    ) {
        super(websocket)
    }
    ngOnInit() {
        this.viewContainerRef.createEmbeddedView(this.content);
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        });

        /** RandomSelector has a random order of characters  */
        this.randomSelector = UUID.UUID().toString();
        if (this.channel !== undefined && this.channel != null) {
            if (this.channel.includes('/')) {
                /** Calls method of parentClass AbstractWidgetLine for Subscribing on channels*/
                this.subscribeOnChannels(this.randomSelector, [ChannelAddress.fromString(this.channel)]);
            }
        }
    }
    ngOnDestroy() {
        this.unsubscribe(this.randomSelector);
    }
    /** method for subscribing, but not through the input-channels out of HTML, but through the associated Typescript-file.
     * Getting the channels, which are not called in the HTML, but are needed.
     * @param channelAddress the channel of the childClass which is not handed over through html
    */

    public subscribing(selector: string, channelAddress: string | string[]): void {
        let channels: ChannelAddress[] = [];
        if (typeof channelAddress === 'string') {
            channels.push(ChannelAddress.fromString(channelAddress));
        } else if (channelAddress.length > 1) {
            channelAddress.forEach(element => {
                channels.push(ChannelAddress.fromString(element));
            })
        }
        this.subscribeOnChannels(selector, channels);
    }

    /** Method for unsubscribing */
    public unsubcribing(selector: string) {
        this.unsubscribe(selector);
    }
}

/** new Type for compactness when calling */
export type Unit = {
    /** unit is set, when an other unit as 'kW' is needed.*/
    otherUnit: string;
    /** unit_value is set, when unitvalue has to be replaced */
    value: string;
    /** unit_format is needed, if your unit_value is changed from default and you have to give him a certain format */
    format: string;
}
