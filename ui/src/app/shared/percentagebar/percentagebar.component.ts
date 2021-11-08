import { Component, Input } from '@angular/core';

@Component({
    selector: 'percentagebar',
    templateUrl: './percentagebar.component.html'
})
export class PercentageBarComponent {

    @Input() public value: number;

    constructor(
    ) { }
}