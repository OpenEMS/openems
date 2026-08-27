// @ts-strict-ignore
import { Component, ChangeDetectionStrategy } from "@angular/core";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";

type ChargeMode = "FORCE_CHARGE" | "EXCESS_POWER" | "OFF";
@Component({
    templateUrl: "./popover.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class PopoverComponent extends AbstractModal {
    public chargeMode: ChargeMode;
}
