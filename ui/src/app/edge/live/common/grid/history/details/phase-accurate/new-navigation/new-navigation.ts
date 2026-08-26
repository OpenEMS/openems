import { Component, ChangeDetectionStrategy } from "@angular/core";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";

@Component({
    selector: "oe-common-grid-phase-accurate-overview",
    templateUrl: "./new-navigation.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class CommonGridPhaseAccurateOverviewComponent extends AbstractModal {}
