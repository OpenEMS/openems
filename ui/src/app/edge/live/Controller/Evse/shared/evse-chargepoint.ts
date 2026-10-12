import { effect, Injector, runInInjectionContext } from "@angular/core";
import { EdgeConfig } from "src/app/shared/components/edge/edgeconfig";
import { PartialedIcon } from "src/app/shared/components/navigation/shared";
import { OeImageComponent } from "src/app/shared/components/oe-img/oe-img";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { ChannelAddress, Service } from "src/app/shared/shared";
import { environment } from "src/environments";

/**
 * Wrapper around an EVSE charge point {@link EdgeConfig.Component}.
 *
 * Deliberately does not extend {@link EdgeConfig.Component}, so that this module has no runtime dependency on
 * 'edgeconfig'. That would create a circular import edgeconfig -> widgets -> Evse/shared -> evse-chargepoint ->
 * edgeconfig, whose 'extends' dereferences a not yet initialized module.
 */
export abstract class EvseChargepoint {
    public icon: PartialedIcon = { color: "normal", name: "oe-evcs" };
    public abstract img: OeImageComponent["img"];

    constructor(public readonly component: EdgeConfig.Component) {}

    public static getEvseChargepoint(chargePoint: EdgeConfig.Component | null): EvseChargepoint | null {
        if (chargePoint == null) {
            return null;
        }

        switch (chargePoint.factoryId) {
            case "Evse.ChargePoint.Keba.UDP":
                return new P30KebaUdp(chargePoint);
            case "Evse.ChargePoint.Keba.Modbus":
                return new P40KebaModbus(chargePoint);
            case "Evse.ChargePoint.HardyBarth":
                return new HardyBarth(chargePoint);
            case "Evse.ChargePoint.Alpitronic":
                return new Alpitronic(chargePoint);
            case "Evse.ChargePoint.Mennekes":
                return new Mennekes(chargePoint);
            case "Evse.ChargePoint.Voltie":
                return new Voltie(chargePoint);
            case "Evse.ChargePoint.Alfen":
                return new Alfen(chargePoint);
            case "Simulator.Evse.ChargePoint":
                return new Simulator(chargePoint);
            case null:
            default:
                return null;
        }
    }

    public showPhaseSwitching(injector: Injector): Promise<boolean> {
        return Promise.resolve(false);
    }

    public hasPropertyValue<T>(propertyName: string, value: T): boolean {
        return this.component.hasPropertyValue<T>(propertyName, value);
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
        return true;
    }

    public override showPhaseSwitching(injector: Injector): Promise<boolean> {
        const edge = injector.get(Service).currentEdge();

        if (!edge.isVersionAtLeast("2026.9.4")) {
            return Promise.resolve(false);
        }
        const dataService = injector.get(DataService);
        const channel = new ChannelAddress(this.component.id, "DeviceRole");

        dataService.subscribeChannels([channel], edge, this.component.id);
        return new Promise<boolean>((resolve) => {
            const dispose = runInInjectionContext(injector, () => {
                return effect(() => {
                    const _currentValue = dataService.currentValue();
                    const deviceRole = _currentValue?.allComponents[channel.toString()];

                    if (deviceRole != null) {
                        dispose.destroy();
                        dataService.unsubscribeFromChannels([channel]);
                        resolve(deviceRole === DeviceRole.SLAVE);
                    }
                });
            });
        });
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

export class Mennekes extends EvseChargepoint {
    public img = {
        url: environment.images.EVSE.MENNEKES,
    };
    public override hasPhaseSwitchingAbility(): boolean {
        return true;
    }
}

export class Voltie extends EvseChargepoint {
    public img = {
        url: environment.images.EVSE.VOLTIE,
    };

    /**
     * The charge-point reports phase-switching support in its capability bitmask, and the Edge component offers the
     * ability only when it is set; the UI can only check the configured wiring.
     */
    public override hasPhaseSwitchingAbility(): boolean {
        return this.hasPropertyValue("wiring", "THREE_PHASE");
    }
}

export class Alfen extends EvseChargepoint {
    public img = {
        url: environment.images.EVSE.ALFEN,
    };
    public override hasPhaseSwitchingAbility(): boolean {
        return this.hasPropertyValue("wiring", "THREE_PHASE");
    }
}

export class Simulator extends EvseChargepoint {
    public img = {
        url: environment.images.EVSE.SIMULATOR,
    };

    public override hasPhaseSwitchingAbility(): boolean {
        return (
            this.hasPropertyValue("wiring", "THREE_PHASE") && //
            this.hasPropertyValue("supportsPhaseSwitching", true)
        );
    }
}

export enum DeviceRole {
    MASTER = 0,
    SLAVE = 1,
    UNKNOWN = 2,
}
