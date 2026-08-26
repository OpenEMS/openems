import { Component, ChangeDetectionStrategy } from "@angular/core";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";

@Component({
    selector: "oe-common-grid-history-details",
    templateUrl: "./new-navigation.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class CommonGridExternalLimitationOverviewComponent extends AbstractModal {}
