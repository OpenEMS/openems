import { Injector } from "@angular/core";
import { Component, DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { Alpitronic, EvseChargepoint, P30KebaUdp } from "./evse-chargepoint";

function getEvseChargepoint(factoryId: string, properties: { [property: string]: any }): EvseChargepoint | null {
    const config = DummyConfig.from({
        id: "chargePoint0",
        alias: "chargePoint0",
        factory: {
            // 'Evse.ChargePoint.Alpitronic' is not part of WidgetFactory, hence the cast
            id: factoryId as Component["factory"]["id"],
            natureIds: [],
        },
        properties: properties,
        channels: {},
    });
    return EvseChargepoint.getEvseChargepoint(config.getComponent("chargePoint0"));
}

describe("EvseChargepoint", () => {
    it("#showPhaseSwitching() falls back to #hasPhaseSwitchingAbility()", async () => {
        const keba = getEvseChargepoint("Evse.ChargePoint.Keba.UDP", {
            wiring: "THREE_PHASE",
            p30hasS10PhaseSwitching: true,
        });
        expect(keba).toBeInstanceOf(P30KebaUdp);
        expect(keba?.hasPhaseSwitchingAbility()).toBe(true);
        await expectAsync(keba?.showPhaseSwitching(Injector.NULL)).toBeResolvedTo(true);

        const alpitronic = getEvseChargepoint("Evse.ChargePoint.Alpitronic", {});
        expect(alpitronic).toBeInstanceOf(Alpitronic);
        await expectAsync(alpitronic?.showPhaseSwitching(Injector.NULL)).toBeResolvedTo(false);
    });

    it("#showPhaseSwitching() respects phase-switching property gating", async () => {
        const kebaWithoutS10 = getEvseChargepoint("Evse.ChargePoint.Keba.UDP", {
            wiring: "THREE_PHASE",
        });
        expect(kebaWithoutS10?.hasPhaseSwitchingAbility()).toBe(false);
        await expectAsync(kebaWithoutS10?.showPhaseSwitching(Injector.NULL)).toBeResolvedTo(false);
    });
});
