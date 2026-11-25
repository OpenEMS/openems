import { TranslateService } from "@ngx-translate/core";
import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { ChannelAddress } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { CommonStorageSettingsComponent } from "./settings";

describe("CommonStorageSettingsComponent battery extension hold time", () => {
    const controllerId = "ctrlPrepareBatteryExtension0";
    const essConfig = DummyConfig.Component.ESS_GENERIC_MANAGEDSYMMETRIC("ess0");
    essConfig.properties["enabled"] = true;
    const controller = DummyConfig.Component.CONTROLLER_ESS_PREPARE_BATTERY_EXTENSION(controllerId);
    controller.properties["ess.id"] = "ess0";
    controller.properties["enabled"] = true;
    const config = DummyConfig.from(essConfig, controller);
    let instance: CommonStorageSettingsComponent;
    let translate: jasmine.SpyObj<TranslateService>;

    beforeEach(() => {
        instance = Object.create(CommonStorageSettingsComponent.prototype);
        translate = jasmine.createSpyObj<TranslateService>("TranslateService", ["instant"]);
        translate.instant.and.returnValue("");
        const edge = DummyConfig.dummyEdge({});
        spyOn(edge, "getCurrentConfig").and.returnValue(config);
        Object.assign(instance, { service: { currentEdge: () => edge } });
    });

    function renderInfo(planned: boolean, terminationBuffer: number, targetTimeBuffer: number): void {
        const ess = config.getComponentSafely("ess0");
        AssertionUtils.assertIsDefined(ess);
        const lines = instance["getPrepareBatteryExtensionForEss"](ess, config, translate);
        const infoLines = lines.filter((line) => line.type === "info-line");
        const line = infoLines[planned ? 0 : 1];
        if (line?.nameCallback == null) {
            throw new Error("Battery extension information line is missing");
        }
        line.nameCallback({
            "ess0-prepareBatteryExtensionTargetSoc": 30,
            "ess0-prepareBatteryExtensionTargetTime": "2026-12-01T10:00:00+01:00",
            "ess0-prepareBatteryExtensionExpectedStartEpochSeconds": 1796100000,
            "ess0-prepareBatteryExtensionTargetTimeBuffer": targetTimeBuffer,
            "ess0-prepareBatteryExtensionTerminationBuffer": terminationBuffer,
        });
    }

    it("shows the termination buffer for an immediate start", () => {
        renderInfo(false, 120, 30);
        expect(translate.instant).toHaveBeenCalledWith(
            "EDGE.INDEX.RETROFITTING.INFORMATION_FOR_INSTANT_START",
            jasmine.objectContaining({ targetSoc: 30, targetTimeBuffer: 120 }),
        );
    });

    it("adds the planning buffer for a planned extension", () => {
        renderInfo(true, 120, 30);
        expect(translate.instant).toHaveBeenCalledWith(
            "EDGE.INDEX.RETROFITTING.INFORMATION_FOR_PLANNED_EXPANSION",
            jasmine.objectContaining({ targetSoc: 30, targetTimeBuffer: 150 }),
        );
    });

    it("uses configured values instead of hardcoded defaults", () => {
        renderInfo(true, 45, 15);
        expect(translate.instant).toHaveBeenCalledWith(
            "EDGE.INDEX.RETROFITTING.INFORMATION_FOR_PLANNED_EXPANSION",
            jasmine.objectContaining({ targetTimeBuffer: 60 }),
        );
    });

    it("subscribes to the termination buffer", async () => {
        expect(await instance["getChannelAddresses"]()).toContain(
            new ChannelAddress(controllerId, "_PropertyTerminationBuffer"),
        );
    });

    it("subscribes to the target soc only once", async () => {
        const channelAddresses = await instance["getChannelAddresses"]();
        expect(
            channelAddresses.filter(
                (address) => address.toString() === new ChannelAddress(controllerId, "_PropertyTargetSoc").toString(),
            ),
        ).toHaveSize(1);
    });
});
