import { Component, Input, ChangeDetectionStrategy } from "@angular/core";

@Component({
    selector: "oe-chart-legend",
    templateUrl: "./legend.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class ChartLegendComponent {
    @Input({ required: true }) public title: string | null = null;
    @Input({ required: true }) public description: string | null = null;
}
