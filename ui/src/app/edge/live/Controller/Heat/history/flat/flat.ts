import { Component, ChangeDetectionStrategy } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";

@Component({
    selector: "controller-heat-widget",
    templateUrl: "./flat.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {
    protected FORMAT_SECONDS_TO_DURATION = this.Converter.FORMAT_SECONDS_TO_DURATION(this.translate.getCurrentLang());
}
