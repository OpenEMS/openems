import { Component, Input } from "@angular/core";


@Component({
    selector: 'flat-widget-percentagebar',
    templateUrl: './flatwidget-percentagebar.html'
})


export class FlatWidgetPercentagebar {

    /** value is the channel the percentagebar is reffering to */
    @Input() value: number;

    constructor(
    ) {
    }
    ngOnInit() {
    }
}