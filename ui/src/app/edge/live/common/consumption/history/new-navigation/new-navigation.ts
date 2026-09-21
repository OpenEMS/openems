import { Component, ChangeDetectionStrategy } from "@angular/core";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";

@Component({
    selector: "oe-common-consumption-new-navigation",
    templateUrl: "./new-navigation.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class CommonConsumptionHistoryComponent extends AbstractModal {}
