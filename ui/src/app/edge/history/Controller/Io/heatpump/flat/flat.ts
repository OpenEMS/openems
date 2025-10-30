import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Filter } from "src/app/shared/components/shared/filter";

@Component({
    selector: "controller-io-heatpump-widget",
    templateUrl: "./flat.html",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {
    protected FORMAT_SECONDS_TO_DURATION = this.Converter.FORMAT_SECONDS_TO_DURATION(this.translate.getCurrentLang());
    protected FILTER_NULL_WITH_THRESHOLD: Filter = (value: number | string | null): boolean => value !== null && Number.isFinite(value) && value as number > 59;
}
