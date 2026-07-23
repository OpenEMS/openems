import { Component, Input, ChangeDetectionStrategy } from "@angular/core";

@Component({
    selector: "percentagebar",
    templateUrl: "./percentagebar.component.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class PercentageBarComponent {
    @Input({ required: true }) public value!: number;
    @Input() public showPercentageValue: boolean = true;

    constructor() {}
}
