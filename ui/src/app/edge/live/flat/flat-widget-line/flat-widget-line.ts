import { Component, Input } from "@angular/core";
import { ChannelAddress } from "src/app/shared/shared";
import { Unit } from "src/app/shared/type/widget";
import { AbstractFlatWidgetLine } from "./abstract-flat-widget-line";

@Component({
    selector: 'flat-widget-line',
    templateUrl: './flat-widget-line.html'
})
/** FlatWidgetLine is one line in FlatWidget. you can give him a , a title_type, parameter_value, title_value_type, a channel and a value. */
// export class FlatWidgetLine extends AbstractFlatWidgetComponent implements OnDestroy {
export class FlatWidgetLine extends AbstractFlatWidgetLine {

    /** Name for parameter, displayed on the left side*/
    @Input()
    name: string;

    /** value defines value of the parameter, displayed on the right */
    @Input()
    set value(value: any) {
        this.setValue(value);
    }

    /** Channel defines the channel, you need for this line */
    @Input()
    set channelAddress(channelAddress: string) {
        this.subscribe(ChannelAddress.fromString(channelAddress));
    }

    /** unit is set, when other 'format','otherUnit' or 'value' than default is needed. Default: no format, no value, unit is kW*/
    @Input() unit: Unit = null;

}

