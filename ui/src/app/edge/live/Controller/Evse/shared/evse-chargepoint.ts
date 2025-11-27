import { NavigationTree, PartialedIcon } from "src/app/shared/components/navigation/shared";
import { OeImageComponent } from "src/app/shared/components/oe-img/oe-img";
import { EdgeConfig } from "src/app/shared/shared";
import { environment } from "src/environments";

export abstract class EvseChargepoint extends EdgeConfig.Component {
    public icon: PartialedIcon = { color: "normal", name: "oe-evcs" };
    public abstract img: OeImageComponent["img"];

    constructor(
        component: EdgeConfig.Component,
    ) {
        super(component.id, component.alias, component.isEnabled, component.factoryId, component.properties, component.channels);
    }

    public static getEvseChargepoint(chargePoint: EdgeConfig.Component): EvseChargepoint | null {
        switch (chargePoint?.factoryId ?? null) {
            case "Evse.ChargePoint.Keba.UDP":
                return new P30KebaUdp(chargePoint);
            case "Evse.ChargePoint.Keba.Modbus":
                return new P40KebaModbus(chargePoint);
            case "Evse.ChargePoint.HardyBarth":
                return new HardyBarth(chargePoint);
            case "Evse.ChargePoint.Alpitronic":
                return new Alpitronic(chargePoint);
            case null:
            default:
                return null;
        }
    }
    /**
     * Gets the navigation tree for phase switching if the evse chargepoint supports phase switching.
     *
     * @param controller the evse controller
     * @returns a navigation tree, if phase switching is allowed, else null
     */
    public getPhaseSwitchingNavigationTree(controller: EdgeConfig.Component): NavigationTree | null {
        if (this.hasPhaseSwitchingAbility() == false) {
            return null;
        }

        return new NavigationTree("phase-switching", { baseString: "phase-switching" }, { name: "stats-chart-outline", color: "warning" }, "phase-switching", "label", [], null);
    }

    public abstract hasPhaseSwitchingAbility(): boolean;
}

export class P30KebaUdp extends EvseChargepoint {
    public img = {
        url: environment.images.EVSE.KEBA_P30,
    };

    public override hasPhaseSwitchingAbility(): boolean {
        return this.hasPropertyValue("wiring", "THREE_PHASE") && this.hasPropertyValue("p30hasS10PhaseSwitching", true);
    }
}

export class HardyBarth extends EvseChargepoint {

    public img = {
        url: environment.images.EVSE.HARDY_BARTH,
    };
    public override hasPhaseSwitchingAbility(): boolean {
        return false;
    }
}

export class P40KebaModbus extends EvseChargepoint {

    public img = {
        url: environment.images.EVSE.KEBA_P40,
    };

    public override hasPhaseSwitchingAbility(): boolean {
        return this.hasPropertyValue("wiring", "THREE_PHASE");
    }
}

export class Alpitronic extends EvseChargepoint {

    public img = {
        url: environment.images.EVSE.ALPITRONIC,
    };

    public override hasPhaseSwitchingAbility(): boolean {
        return false;
    }
}
