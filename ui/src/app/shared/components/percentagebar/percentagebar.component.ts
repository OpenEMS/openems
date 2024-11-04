import { Component, Input } from "@angular/core";

@Component({
    selector: "percentagebar",
    templateUrl: "./percentagebar.component.html",
})
export class PercentageBarComponent {

    @Input({ required: true }) public value!: number;
    @Input() public showPercentageValue: boolean = true;

    constructor(
    ) { }
}
