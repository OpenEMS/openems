import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";

@Component({
    selector: "controller-chp-soc-widget",
    templateUrl: "./FLAT.HTML",
})
export class FlatComponent extends AbstractFlatWidget {
    protected FORMAT_SECONDS_TO_DURATION = THIS.CONVERTER.FORMAT_SECONDS_TO_DURATION(THIS.TRANSLATE.CURRENT_LANG);
}
