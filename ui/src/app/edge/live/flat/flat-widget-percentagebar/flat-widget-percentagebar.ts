import { Component, Input } from "@angular/core";
import { ChannelAddress } from "src/app/shared/shared";
import { AbstractFlatWidgetLine } from "../flat-widget-line/abstract-flat-widget-line";


@Component({
    selector: 'oe-flat-widget-percentagebar',
    templateUrl: './flat-widget-percentagebar.html'
})


export class FlatWidgetPercentagebar extends AbstractFlatWidgetLine {

    /** value is the channel the percentagebar is refering to */
    @Input()
    set value(value: any) {
        this.setValue(value);
    }
    @Input() set channelAddress(channelAddress: string) {
        this.subscribe(ChannelAddress.fromString(channelAddress))
    }
}