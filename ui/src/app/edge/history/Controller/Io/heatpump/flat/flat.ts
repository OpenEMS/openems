import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Filter } from "src/app/shared/components/shared/filter";

@Component({
    selector: "controller-io-heatpump-widget",
    templateUrl: "./FLAT.HTML",
    standalone: false,
})
export class FlatComponent extends AbstractFlatWidget {
    protected FORMAT_SECONDS_TO_DURATION = THIS.CONVERTER.FORMAT_SECONDS_TO_DURATION(THIS.TRANSLATE.CURRENT_LANG);
    protected FILTER_NULL_WITH_THRESHOLD: Filter = (value: number | string | null): boolean => value !== null && NUMBER.IS_FINITE(value) && value as number > 59;
}
