import { Component, Input } from "@angular/core";


@Component({
    selector: 'flat-widget-percentagebar',
    templateUrl: './flatwidget-percentagebar.html'
})


export class FlatWidgetPercentagebar {
    @Input() value: number;

    constructor(
    ) {
    }
    ngOnInit() {
    }
}