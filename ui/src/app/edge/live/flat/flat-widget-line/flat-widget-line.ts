import { Component, Input } from "@angular/core";
import { ChannelAddress } from "src/app/shared/shared";
import { AbstractFlatWidgetLine } from "./abstract-flat-widget-line";

@Component({
    selector: 'oe-flat-widget-line',
    templateUrl: './flat-widget-line.html'
})
export class FlatWidgetLine extends AbstractFlatWidgetLine {

    /** Name for parameter, displayed on the left side */
    @Input()
    name: string;

    /** shows @Input() value when 0 */
    @Input() showWhenValueEquals0?: boolean = false;

    /** value defines value of the parameter, displayed on the right */
    @Input()
    set value(value: any) {
        if (this.showWhenValueEquals0 != null) {
            this.setValue(value, this.showWhenValueEquals0);
        }
    }

    /** Channel defines the channel, you need for this line */
    @Input()
    set channelAddress(channelAddress: string) {
        this.subscribe(ChannelAddress.fromString(channelAddress));
    }
}

