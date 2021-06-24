import { Component, Input } from "@angular/core";
import { ChannelAddress } from "src/app/shared/shared";
import { AbstractFlatWidgetLine } from "../../flat/flat-widget-line/abstract-flat-widget-line";


@Component({
    selector: 'oe-modal-line',
    templateUrl: './modal-line.html',
})
export class ModalLineComponent extends AbstractFlatWidgetLine {
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

    @Input() indentation_from_left: string;
}