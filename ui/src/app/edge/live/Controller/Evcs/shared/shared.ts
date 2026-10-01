import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { NavigationConstants, NavigationTree } from "src/app/shared/components/navigation/shared";
import { Name } from "src/app/shared/components/shared/name";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";

export namespace SharedEvcs {
    export function getChannelAddresses(
        component: EdgeConfig.Component,
        ctrl: EdgeConfig.Component | null,
    ): Promise<ChannelAddress[]> {
        const channels: ChannelAddress[] = [
            // EVCS state channels
            new ChannelAddress(component.id, "State"),
            new ChannelAddress(component.id, "Status"),
            new ChannelAddress(component.id, "ActivePower"),
            new ChannelAddress(component.id, "Plug"),
            new ChannelAddress(component.id, "Phases"),
            new ChannelAddress(component.id, "EnergySession"),

            // Used by old implementation
            new ChannelAddress(component.id, "MinimumHardwarePower"),
            new ChannelAddress(component.id, "MaximumHardwarePower"),
            new ChannelAddress(component.id, "SetChargePowerLimit"),
        ];

        if (ctrl == null) {
            return Promise.resolve(channels);
        }

        channels.push(
            new ChannelAddress(ctrl.id, "_PropertyChargeMode"),
            new ChannelAddress(ctrl.id, "_PropertyForceChargeMinPower"),
            new ChannelAddress(ctrl.id, "_PropertyEnergySessionLimit"),
            new ChannelAddress(ctrl.id, "_PropertyDefaultChargeMinPower"),
            new ChannelAddress(ctrl.id, "_PropertyPriority"),
            new ChannelAddress(ctrl.id, "_PropertyEnabledCharging"),
        );

        return Promise.resolve(channels);
    }

    export type EvcsViewModel = {
        hasConnection: boolean;
        isEnergySinceBeginningAllowed: boolean;
        isChargingEnabled: boolean;
        isReadOnly: boolean;
        isNotExcessPower: boolean;
        hasNoDefaultChargeMinPower: boolean;
        isNotForceCharge: boolean;
        hasNoEnergySessionLimit: boolean;
    };

    export function getFormGroup(): FormGroup {
        return new FormGroup({
            chargeMode: new FormControl(null),
            isEnergySinceBeginningAllowed: new FormControl(null),
            hasConnection: new FormControl(null),
            isChargingEnabled: new FormControl(null),
            isNotExcessPower: new FormControl(null),
            isNotForceCharge: new FormControl(null),
            isReadOnly: new FormControl(null),
            hasNoDefaultChargeMinPower: new FormControl(null),
            hasNoEnergySessionLimit: new FormControl(null),
            energyLimit: new FormControl(null),
            minGuarantee: new FormControl(null),
            defaultChargeMinPower: new FormControl(null),
            forceChargeMinPower: new FormControl(null),
            priority: new FormControl(null),
            // EnergySessionLimit as Wh value
            energySessionLimit: new FormControl(null),
            // EnergySessionLimit as kWh value, for ion-range
            energySessionLimitKwh: new FormControl(null),
            enabledCharging: new FormControl(null),
        });
    }

    export function getNavigationTree(
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): ConstructorParameters<typeof NavigationTree> {
        return new NavigationTree(
            component.id,
            { baseString: "evcs/" + component.id },
            { name: "oe-evcs", color: "success" },
            Name.METER_ALIAS_OR_ID(component),
            "label",
            [
                NavigationConstants.CommonNodes.SETTINGS(translate, component.id),
                NavigationConstants.CommonNodes.INFO(translate, component.id, {
                    source: component.id,
                }),
            ],
            null,
        ).toConstructorParams();
    }
}
