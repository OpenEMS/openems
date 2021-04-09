import { Component, Input, ViewChild } from "@angular/core";
import { ChannelAddress, Edge, EdgeConfig } from "src/app/shared/shared";
import { Unit } from "src/app/shared/type/widget";

@Component({
    selector: 'flat-widget-line',
    templateUrl: './flatwidget-line.html'
})
/** FlatWidgetLine is one line in FlatWidget. you can give him a , a title_type, parameter_value, title_value_type, a channel and a value. */
// export class FlatWidgetLine extends AbstractFlatWidgetComponent implements OnDestroy {
export class FlatWidgetLine {

    /** ViewChild selects the ng-teplate with certain ID */
    @ViewChild('content', { static: true }) content;
    /** Title for parameter, displayed on the left side*/
    @Input() parameter_name: string;
    /** parameter_name_translate specifies, if there is a  to translate */
    @Input() parameter_name_translate: string;
    /** parameter_value defines value of the parameter, displayed on the right */
    @Input() parameter_value: string;
    /** parameter_value_translate specifies, if there is a parameter_value to translate */
    @Input() parameter_value_translate: string;
    /** Channel defines the channel, you need for this line */
    @Input() channel: string;
    /** value get used, if the channelAddress is not starting with currentData.channel, but your unit is kw. You have to hand over the whole path. */
    @Input() value: string;
    /** unit is set, when other 'format','otherUnit' or 'value' than default is needed. Default: no format, no value, unit is kW*/
    @Input() unit: Unit = null;
    /** title_addition is used, when your parametertitle is not only a string, but also needs data from variable or channel */
    @Input() title_addition: string;

    public edge: Edge = null;
    public essComponents: EdgeConfig.Component[] = null;
    public channelAddresses: ChannelAddress[] = null;

    // constructor(
    //     public route: ActivatedRoute,
    //     public service: Service,
    //     public viewContainerRef: ViewContainerRef,
    // ) {
    //     super()
    // }
    protected getChannelAddressess() {
    }
    /** RandomSelector has a random sequence of characters */

    //     ngOnDestroy() {
    //         this.unsubscribe(this.randomSelector);
    //     }
}

