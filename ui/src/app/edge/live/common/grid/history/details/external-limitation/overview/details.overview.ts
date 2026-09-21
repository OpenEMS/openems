import { Component, ChangeDetectionStrategy } from "@angular/core";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";

@Component({
    templateUrl: "./details.overview.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class CommonGridDetailsExternalLimitationOverviewComponent extends AbstractModal {}
