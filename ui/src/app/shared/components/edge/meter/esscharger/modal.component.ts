import { Component, Input } from "@angular/core";
import { Converter } from "src/app/shared/components/shared/converter";
import { Utils } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { EdgeConfig } from "../../edgeconfig";

@Component({
    selector: "oe-ess-charger",
    templateUrl: "./MODAL.COMPONENT.HTML",
    standalone: false,
})
export class EssChargerComponent {
    @Input({ required: true }) public component!: EDGE_CONFIG.COMPONENT;
    protected readonly Role = Role;
    protected readonly Utils = Utils;
    protected readonly Converter = Converter;
}
