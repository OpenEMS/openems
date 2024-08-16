// @ts-strict-ignore
import { Component, Input } from "@angular/core";
import { Icon } from "src/app/shared/type/widget";

/**
 * Inserts a transparent divider
 */
@Component({
    selector: 'oe-flat-widget-line-divider',
    templateUrl: './flat-widget-line-divider.html',
})
export class FlatWidgetLineDividerComponent {

    /**
     * Info-Text, displayed on the right side, optional style for all lines
     * Multiple lines with own style is possible
     */
    @Input() public info: { text: string, lineStyle?: string }[] | string;

    /** Icon, displayed on the left side */
    @Input() protected icon: Icon;

    @Input() protected lineStyle: string;

    @Input() protected rowStyle: string;
}
