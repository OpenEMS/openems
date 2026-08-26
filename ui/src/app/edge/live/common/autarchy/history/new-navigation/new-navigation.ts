import { Component, ChangeDetectionStrategy } from "@angular/core";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";

@Component({
    selector: "oe-common-autarchy-history",
    templateUrl: "./new-navigation.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class CommonAutarchyHistoryComponent extends AbstractModal {}
