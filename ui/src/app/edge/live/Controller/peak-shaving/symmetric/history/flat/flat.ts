import { Component, ChangeDetectionStrategy } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";

@Component({
    selector: "oe-controller-peakshaving-symmetric",
    templateUrl: "./flat.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {}
