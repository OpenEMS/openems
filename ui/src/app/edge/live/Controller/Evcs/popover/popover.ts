// @ts-strict-ignore
import { Component } from "@angular/core";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";

type ChargeMode = "FORCE_CHARGE" | "EXCESS_POWER" | "OFF";
@Component({
    templateUrl: "./popover.html",
    standalone: false,
})

export class PopoverComponent extends AbstractModal {
    public chargeMode: ChargeMode;
}
