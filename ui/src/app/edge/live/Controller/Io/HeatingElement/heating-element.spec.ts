import { EventEmitter, signal } from "@angular/core";
import { FormBuilder, FormGroup } from "@angular/forms";
import { ModalController } from "@ionic/angular";
import { Subject } from "rxjs";
import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { ModalComponent as OeModalComponent } from "src/app/shared/components/modal/modal";
import { OeFormlyViewTester } from "src/app/shared/components/shared/testing/tester";
import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/utils.spec";
import sharedDe from "src/app/shared/i18n/de.json";
import { CurrentData, Edge, EdgeConfig, Websocket } from "src/app/shared/shared";
import { Mode, WorkMode } from "src/app/shared/type/general";
import { Role } from "src/app/shared/type/role";
import { ChartAxis, YAxisType } from "src/app/shared/utils/utils";
import globalDe from "src/assets/i18n/de.json";
import { FlatComponent } from "./flat/flat";
import { ControllerIoHeatingElementChartComponent } from "./history/chart/chart";
import { ControllerHeatingElementChartComponent } from "./history/flat/flat";
import { ModalComponent } from "./modal/modal";
import { ControllerIoHeatingElementHomeComponent } from "./new-navigation/new-navigation";
import { ControllerIoHeatingElementSettingsComponent } from "./settings/settings";
import { SharedControllerIoHeatingElement } from "./shared/shared";
import { HeatingElementComponent, HeatingElementLevelValue, HeatingElementMode } from "./util/utils";

const OLD_EDGE_VERSION = "2026.10.1";
const NEW_EDGE_VERSION = HeatingElementComponent.MIN_EDGE_VERSION;
const PHASE_COLORS = ["#39b8ff", "#6fd400", "#ffba00"];

function createHeatingElementComponent(properties: Record<string, any> = {}): EdgeConfig.Component {
    return new EdgeConfig.Component("ctrlIoHeatingElement0", "Heizstab", true, true, "Controller.Io.HeatingElement", {
        defaultLevel: "LEVEL_1",
        endTime: "14:00",
        endTimeWithMeter: "15:00",
        "meter.id": "meter0",
        minEnergylimit: 5000,
        minKwh: 5,
        minTime: 2,
        mode: Mode.AUTOMATIC,
        outputChannelPhaseL1: "io0/Relay1",
        outputChannelPhaseL2: "io0/Relay2",
        outputChannelPhaseL3: "io0/Relay3",
        workMode: WorkMode.TIME,
        ...properties,
    });
}

function createEdgeWithVersion(version: any): Edge {
    const edge = DummyConfig.dummyEdge({ version });
    (edge as any).version = version;
    return edge;
}

class SettingsHarness extends ControllerIoHeatingElementSettingsComponent {
    private testComponent!: EdgeConfig.Component;
    private testEdge!: Edge;

    public static create(component: EdgeConfig.Component, edge: Edge): SettingsHarness {
        const instance = Object.create(SettingsHarness.prototype) as SettingsHarness;
        instance.testComponent = component;
        instance.testEdge = edge;
        Object.defineProperty(instance, "form", { value: signal(instance.getFormGroup()) });
        return instance;
    }

    public getForm(): FormGroup {
        return this.form();
    }

    public receive(data: CurrentData): void {
        this.onCurrentData(data);
    }

    public save(context: TestContext, component: EdgeConfig.Component, edge: Edge): void {
        Object.defineProperty(this, "translate", { value: context.translate });
        this.applyChanges(this.form(), context.service, context.service.websocket, component, edge);
    }

    protected override getComponent(): EdgeConfig.Component {
        return this.testComponent;
    }

    protected override getEdge(): Edge {
        return this.testEdge;
    }
}

describe("Controller.Io.HeatingElement", () => {
    let testContext: TestContext;

    beforeEach(async () => {
        testContext = await TestingUtils.sharedSetup();
        testContext.translate.setTranslation("de", globalDe, true);
        testContext.translate.setTranslation("de", sharedDe, true);
        testContext.translate.use("de");
    });

    describe("HeatingElementConfig & version compatibility", () => {
        it("translates HeatingElement modes with its dedicated converter", () => {
            const convertMode = HeatingElementComponent.createModeLabelConverter(testContext.translate);

            expect(convertMode(HeatingElementMode.FAST_HEAT)).toBe("Schnell heizen");
            expect(convertMode(HeatingElementMode.MANUAL_ON)).toBe("An");
            expect(convertMode(HeatingElementMode.AUTOMATIC)).toBe("Automatisch");
            expect(convertMode(HeatingElementMode.MANUAL_OFF)).toBe("Aus");
            expect(convertMode(HeatingElementMode.MANUAL)).toBe("Manuell");
            expect(convertMode("toString")).toBe("-");
            expect(convertMode("constructor")).toBe("-");
            expect(convertMode("UNKNOWN")).toBe("-");
            expect(convertMode(null)).toBe("-");
            expect(convertMode(undefined)).toBe("-");
        });

        for (const scenario of [
            { role: Role.ADMIN, version: NEW_EDGE_VERSION, supported: true },
            { role: Role.ADMIN, version: OLD_EDGE_VERSION, supported: false },
            { role: Role.OWNER, version: NEW_EDGE_VERSION, supported: false },
            { role: Role.INSTALLER, version: NEW_EDGE_VERSION, supported: false },
        ]) {
            it(`limits new features for ${Role[scenario.role]} on Edge ${scenario.version}`, () => {
                const component = createHeatingElementComponent({ numberOfHeatingElements: "TWO" });
                const edge = DummyConfig.dummyEdge({ role: scenario.role, version: scenario.version });
                const config = new HeatingElementComponent(component, edge);
                const expectedLevels = scenario.supported ? [1, 2, 3, 4, 5, 6] : [1, 2, 3];
                const expectedLevelValues: HeatingElementLevelValue[] = scenario.supported
                    ? ["LEVEL_1", "LEVEL_2", "LEVEL_3", "LEVEL_4", "LEVEL_5", "LEVEL_6"]
                    : ["LEVEL_1", "LEVEL_2", "LEVEL_3"];
                const expectedModes: HeatingElementMode[] = scenario.supported
                    ? [
                          HeatingElementMode.MANUAL_ON,
                          HeatingElementMode.FAST_HEAT,
                          HeatingElementMode.AUTOMATIC,
                          HeatingElementMode.MANUAL_OFF,
                      ]
                    : [HeatingElementMode.MANUAL_ON, HeatingElementMode.AUTOMATIC, HeatingElementMode.MANUAL_OFF];

                expect(HeatingElementComponent.isSupportedEdgeVersion(edge)).toBe(
                    scenario.version === NEW_EDGE_VERSION,
                );
                expect(config.canUseTwoHeatingElementsAndFastHeat()).toBe(scenario.supported);
                expect(config.supportsTwoHeatingElements()).toBe(scenario.supported);
                expect(config.getActiveLevels()).toEqual(expectedLevels);
                expect(config.getSupportedLevelValues()).toEqual(expectedLevelValues);
                expect(config.isSupportedLevelValue("LEVEL_0")).toBeFalse();
                expect(config.getLevelOptions(testContext.translate).map((option) => option.value)).toEqual(
                    expectedLevelValues,
                );
                expect(config.getAvailableModes()).toEqual(expectedModes);
                expect(config.getModeButtons(testContext.translate).map((button) => button.value)).toEqual(
                    expectedModes,
                );
                expect(config.getModeButtons(testContext.translate).map((button) => button.name)).toEqual(
                    scenario.supported ? ["An", "Schnell heizen", "Automatisch", "Aus"] : ["An", "Automatisch", "Aus"],
                );
            });
        }

        it("1. Legacy ONE with missing numberOfHeatingElements defaults to ONE (Levels 1-3) and legacy modes on old Edge", () => {
            const component = createHeatingElementComponent();
            const oldEdge = createEdgeWithVersion(OLD_EDGE_VERSION);
            const config = new HeatingElementComponent(component, oldEdge);

            expect(config.canUseTwoHeatingElementsAndFastHeat()).toBeFalse();
            expect(config.isTwoHeatingElements()).toBeFalse();
            expect(config.supportsTwoHeatingElements()).toBeFalse();
            expect(config.getActiveLevels()).toEqual([1, 2, 3]);
            expect(config.getSupportedLevelValues()).toEqual(["LEVEL_1", "LEVEL_2", "LEVEL_3"]);
            expect(config.getAvailableModes()).toEqual([
                HeatingElementMode.MANUAL_ON,
                HeatingElementMode.AUTOMATIC,
                HeatingElementMode.MANUAL_OFF,
            ]);
            expect(config.getChartYAxisType("line")).toBe(YAxisType.HEATING_ELEMENT);
            expect(config.getChartYAxisType("bar")).toBe(YAxisType.TIME);
        });

        it("2. New ONE supports Levels 0-3 (active 1-3) and FAST_HEAT on supported Edge", () => {
            const component = createHeatingElementComponent({ numberOfHeatingElements: "ONE" });
            const newEdge = createEdgeWithVersion(NEW_EDGE_VERSION);
            const config = new HeatingElementComponent(component, newEdge);

            expect(config.canUseTwoHeatingElementsAndFastHeat()).toBeTrue();
            expect(config.isTwoHeatingElements()).toBeFalse();
            expect(config.supportsTwoHeatingElements()).toBeFalse();
            expect(config.getActiveLevels()).toEqual([1, 2, 3]);
            expect(config.getLevelOptions(testContext.translate)).toEqual([
                { name: "Level 1", value: "LEVEL_1" },
                { name: "Level 2", value: "LEVEL_2" },
                { name: "Level 3", value: "LEVEL_3" },
            ]);
            expect(config.getAvailableModes()).toEqual([
                HeatingElementMode.MANUAL_ON,
                HeatingElementMode.FAST_HEAT,
                HeatingElementMode.AUTOMATIC,
                HeatingElementMode.MANUAL_OFF,
            ]);
            expect(config.getChartYAxisType("line")).toBe(YAxisType.HEATING_ELEMENT);
        });

        it("2b. New Edge with missing numberOfHeatingElements treats configuration as ONE and enables FAST_HEAT", () => {
            const component = createHeatingElementComponent();
            const newEdge = createEdgeWithVersion(NEW_EDGE_VERSION);
            const config = new HeatingElementComponent(component, newEdge);

            expect(config.canUseTwoHeatingElementsAndFastHeat()).toBeTrue();
            expect(config.isTwoHeatingElements()).toBeFalse();
            expect(config.supportsTwoHeatingElements()).toBeFalse();
            expect(config.getActiveLevels()).toEqual([1, 2, 3]);
            expect(config.getAvailableModes()).toContain(HeatingElementMode.FAST_HEAT);
        });

        it("3. New TWO supports Levels 0-6 (active 1-6) and FAST_HEAT on supported Edge", () => {
            const component = createHeatingElementComponent({ numberOfHeatingElements: "TWO" });
            const newEdge = createEdgeWithVersion(NEW_EDGE_VERSION);
            const config = new HeatingElementComponent(component, newEdge);

            expect(config.canUseTwoHeatingElementsAndFastHeat()).toBeTrue();
            expect(config.isTwoHeatingElements()).toBeTrue();
            expect(config.supportsTwoHeatingElements()).toBeTrue();
            expect(config.getActiveLevels()).toEqual([1, 2, 3, 4, 5, 6]);
            expect(config.getLevelOptions(testContext.translate)).toEqual([
                { name: "Level 1", value: "LEVEL_1" },
                { name: "Level 2", value: "LEVEL_2" },
                { name: "Level 3", value: "LEVEL_3" },
                { name: "Level 4", value: "LEVEL_4" },
                { name: "Level 5", value: "LEVEL_5" },
                { name: "Level 6", value: "LEVEL_6" },
            ]);
            expect(config.getChartYAxisType("line")).toBe(YAxisType.HEATING_ELEMENT_TWO);
            expect(config.getChartYAxisType("bar")).toBe(YAxisType.TIME);
        });

        it("4 & 5. Separates TWO configuration from Edge capabilities on unknown, invalid, or older Edge versions", () => {
            const twoComponent = createHeatingElementComponent({ numberOfHeatingElements: "TWO" });

            const invalidEdges: (Edge | null | undefined)[] = [
                null,
                undefined,
                createEdgeWithVersion(""),
                createEdgeWithVersion("unknown"),
                createEdgeWithVersion("invalid-version"),
                createEdgeWithVersion(null),
                createEdgeWithVersion(OLD_EDGE_VERSION),
            ];

            for (const edge of invalidEdges) {
                const config = new HeatingElementComponent(twoComponent, edge);
                // Configuration reflects component property
                expect(config.isTwoHeatingElements()).toBeTrue();
                // Capability gating requires supported Edge version
                expect(config.canUseTwoHeatingElementsAndFastHeat()).toBeFalse();
                expect(config.supportsTwoHeatingElements()).toBeFalse();
                expect(config.getActiveLevels()).toEqual([1, 2, 3]);
                expect(config.getAvailableModes()).toEqual([
                    HeatingElementMode.MANUAL_ON,
                    HeatingElementMode.AUTOMATIC,
                    HeatingElementMode.MANUAL_OFF,
                ]);
                expect(config.getChartYAxisType("line")).toBe(YAxisType.HEATING_ELEMENT);
            }
        });
    });

    describe("New Navigation (Home & Settings) and submission guards", () => {
        for (const mode of [
            HeatingElementMode.FAST_HEAT,
            HeatingElementMode.AUTOMATIC,
            HeatingElementMode.MANUAL_OFF,
        ]) {
            it(`settings reads ${mode} without dirtying the form`, () => {
                const component = createHeatingElementComponent();
                const edge = createEdgeWithVersion(NEW_EDGE_VERSION);
                const settings = SettingsHarness.create(component, edge);
                settings.receive({ allComponents: { [component.id + "/_PropertyMode"]: mode } });

                expect(settings.getForm().controls["mode"].value).toBe(mode);
                expect(settings.getForm().pristine).toBeTrue();
            });
        }

        it("settings saves the selected mode and reports the accepted update", async () => {
            const component = createHeatingElementComponent();
            const edge = createEdgeWithVersion(NEW_EDGE_VERSION);
            const update = spyOn(edge, "updateComponentConfig").and.resolveTo();
            const startSpinner = spyOn(testContext.service, "startSpinner");
            const stopSpinner = spyOn(testContext.service, "stopSpinner");
            const toast = spyOn(testContext.service, "toast").and.resolveTo();
            const stoppedSpinner = new Promise<void>((resolve) => stopSpinner.and.callFake(() => resolve()));
            const settings = SettingsHarness.create(component, edge);
            const mode = HeatingElementMode.MANUAL_OFF;
            settings.receive({
                allComponents: { [component.id + "/_PropertyMode"]: HeatingElementMode.AUTOMATIC },
            });
            settings.getForm().controls["mode"].setValue(mode);
            settings.getForm().controls["mode"].markAsDirty();

            settings.save(testContext, component, edge);

            expect(update).toHaveBeenCalledWith(testContext.service.websocket, component.id, [
                { name: "mode", value: mode },
            ]);
            await stoppedSpinner;
            expect(startSpinner).toHaveBeenCalledWith("formly-field-modal");
            expect(toast).toHaveBeenCalledWith(testContext.translate.instant("GENERAL.CHANGE_ACCEPTED"), "success");
            expect(stopSpinner).toHaveBeenCalledWith("formly-field-modal");
            expect(settings.getForm().pristine).toBeTrue();
        });

        it("continues displaying received mode and active level values (including FAST_HEAT and Level 0..6)", () => {
            const component = createHeatingElementComponent({ mode: Mode.AUTOMATIC });
            const view = ControllerIoHeatingElementHomeComponent.generateView(
                testContext.translate,
                component,
                Mode.AUTOMATIC,
            );

            const fastHeatApplied = OeFormlyViewTester.apply(view, {
                "ctrlIoHeatingElement0/_PropertyMode": "FAST_HEAT" as any,
                "ctrlIoHeatingElement0/Status": 1,
                "ctrlIoHeatingElement0/Level": 5,
            });
            expect(fastHeatApplied.lines).toContain(
                jasmine.objectContaining({
                    type: "value-from-channels-line",
                    name: "Modus",
                    value: "Schnell heizen",
                }),
            );
            expect(fastHeatApplied.lines).toContain(
                jasmine.objectContaining({
                    type: "value-from-channels-line",
                    name: "Aktives Level",
                    value: "Level 5",
                }),
            );

            const flatStub: any = Object.create(FlatComponent.prototype);
            const langChange$ = new EventEmitter<any>();
            let activeLanguage = "de";
            const translate = jasmine.createSpyObj("TranslateService", ["instant"], {
                onLangChange: langChange$,
            });
            translate.instant.and.callFake((key: string) =>
                activeLanguage === "de" ? testContext.translate.instant(key) : "Fast heat",
            );
            flatStub.component = component;
            flatStub.config = { getComponent: () => null };
            flatStub.translate = translate;
            flatStub.CONVERT_HEATING_ELEMENT_MODE = HeatingElementComponent.createModeLabelConverter(translate);
            flatStub.stopOnDestroy = new Subject<void>();
            (FlatComponent.prototype as any).afterIsInitialized.call(flatStub);
            (FlatComponent.prototype as any).onCurrentData.call(flatStub, {
                allComponents: {
                    "ctrlIoHeatingElement0/_PropertyMode": "FAST_HEAT",
                    "ctrlIoHeatingElement0/Level": 6,
                    "ctrlIoHeatingElement0/Status": 1,
                },
            });
            expect(flatStub.mode).toBe("Schnell heizen");
            expect(flatStub.level).toBe(6);

            activeLanguage = "en";
            langChange$.emit({ lang: "en" });

            expect(flatStub.mode).toBe("Fast heat");
            flatStub.stopOnDestroy.next();
        });

        it("exposes only legacy modes and Levels 1-3 on older Edge in Formly view, preserves normal dirty/pristine behavior, and filters unsupported submissions", () => {
            const component = createHeatingElementComponent({ numberOfHeatingElements: "TWO" });
            const oldEdge = createEdgeWithVersion(OLD_EDGE_VERSION);

            const view = SharedControllerIoHeatingElement.getFormlyView(testContext.translate, component, oldEdge);
            const formGroup = SharedControllerIoHeatingElement.getFormGroup(component);
            formGroup.patchValue({ mode: Mode.MANUAL_ON, workMode: WorkMode.TIME });

            const modeButtonsLine = view.lines
                .filter((line) => line.type === "buttons-from-form-control-line")
                .find((line) => line.controlName === "mode");
            expect(modeButtonsLine?.buttons.map((b) => b.value)).toEqual([
                Mode.MANUAL_ON,
                Mode.AUTOMATIC,
                Mode.MANUAL_OFF,
            ]);

            const selectLine = view.lines.find(
                (line) => line.type === "select-line" && line.controlName === "defaultLevel",
            ) as any;
            expect(selectLine.options.map((o: any) => o.value)).toEqual(["LEVEL_1", "LEVEL_2", "LEVEL_3"]);

            // Normal Angular FormControl dirty/pristine behavior is preserved
            expect(formGroup.controls["mode"].pristine).toBeTrue();
            formGroup.controls["mode"].setValue(Mode.AUTOMATIC);
            formGroup.controls["mode"].markAsDirty();
            expect(formGroup.controls["mode"].dirty).toBeTrue();
            expect(formGroup.controls["mode"].value).toBe(Mode.AUTOMATIC);

            // Supported legacy mode and level are included in update payload on old Edge
            const settingsInstance: any = Object.create(ControllerIoHeatingElementSettingsComponent.prototype);
            settingsInstance.getComponent = () => component;
            settingsInstance.service = { currentEdge: () => oldEdge };
            formGroup.controls["defaultLevel"].setValue("LEVEL_2");
            formGroup.controls["defaultLevel"].markAsDirty();
            expect(settingsInstance.buildUpdateComponentArr(formGroup)).toEqual([
                { name: "mode", value: Mode.AUTOMATIC },
                { name: "defaultLevel", value: "LEVEL_2" },
            ]);

            // Unsupported FAST_HEAT or LEVEL_6 on older/unknown Edge are filtered out before submission
            formGroup.controls["mode"].setValue("FAST_HEAT");
            formGroup.controls["mode"].markAsDirty();
            formGroup.controls["defaultLevel"].setValue("LEVEL_6");
            formGroup.controls["defaultLevel"].markAsDirty();
            formGroup.controls["minTime"].setValue(4);
            formGroup.controls["minTime"].markAsDirty();

            const updates = settingsInstance.buildUpdateComponentArr(formGroup);
            expect(updates).toEqual([{ name: "minTime", value: 4 }]);
        });

        it("exposes FAST_HEAT and Levels 1-6 on new Edge with TWO heating elements and allows submitting them", () => {
            const component = createHeatingElementComponent({ numberOfHeatingElements: "TWO" });
            const newEdge = createEdgeWithVersion(NEW_EDGE_VERSION);

            const view = SharedControllerIoHeatingElement.getFormlyView(testContext.translate, component, newEdge);
            const formGroup = SharedControllerIoHeatingElement.getFormGroup(component);

            const modeLine = view.lines.find(
                (line) => line.type === "buttons-from-form-control-line" && line.controlName === "mode",
            ) as any;
            expect(modeLine.buttons.map((b: any) => b.value)).toEqual([
                Mode.MANUAL_ON,
                "FAST_HEAT",
                Mode.AUTOMATIC,
                Mode.MANUAL_OFF,
            ]);

            const selectLine = view.lines.find(
                (line) => line.type === "select-line" && line.controlName === "defaultLevel",
            ) as any;
            expect(selectLine.options.map((o: any) => o.value)).toEqual([
                "LEVEL_1",
                "LEVEL_2",
                "LEVEL_3",
                "LEVEL_4",
                "LEVEL_5",
                "LEVEL_6",
            ]);

            formGroup.controls["mode"].setValue("FAST_HEAT");
            formGroup.controls["mode"].markAsDirty();
            formGroup.controls["defaultLevel"].setValue("LEVEL_6");
            formGroup.controls["defaultLevel"].markAsDirty();

            const settingsInstance: any = Object.create(ControllerIoHeatingElementSettingsComponent.prototype);
            settingsInstance.getComponent = () => component;
            settingsInstance.service = { currentEdge: () => newEdge };

            const updates = settingsInstance.buildUpdateComponentArr(formGroup);
            expect(updates).toEqual([
                { name: "mode", value: "FAST_HEAT" },
                { name: "defaultLevel", value: "LEVEL_6" },
            ]);
        });
    });

    describe("Legacy ModalComponent", () => {
        function createModalStub(component: EdgeConfig.Component, edge: Edge | null): any {
            const langChange$ = new EventEmitter<any>();
            const translateSpy = jasmine.createSpyObj("TranslateService", ["instant"], {
                onLangChange: langChange$,
            });
            translateSpy.instant.and.callFake((key: string) => testContext.translate.instant(key));

            const modal: any = Object.create(ModalComponent.prototype);
            modal.component = component;
            modal.edge = edge;
            modal.mode = Mode.AUTOMATIC;
            modal.formBuilder = new FormBuilder();
            modal.translate = translateSpy;
            modal.subscription = { add: jasmine.createSpy("add") };
            modal.formGroup = modal.getFormGroup();
            modal.onIsInitialized();
            return { modal, translateSpy, langChange$ };
        }

        it("updates the energy limit in Wh and marks it dirty when the kWh control changes", () => {
            const { modal } = createModalStub(createHeatingElementComponent(), null);
            const energyLimit = modal.formGroup.controls["minEnergylimit"];

            modal.formGroup.controls["minEnergyLimitInKwh"].setValue(8);

            expect(energyLimit.value).toBe(8000);
            expect(energyLimit.dirty).toBeTrue();
        });

        it("increases the energy slider maximum based on the total phase power", () => {
            const { modal } = createModalStub(createHeatingElementComponent(), null);
            modal.config = DummyConfig.from(DummyConfig.Component.SOCOMEC_CONSUMPTION_METER("meter0"));
            modal.maxPower = 15;

            modal.onCurrentData({
                allComponents: {
                    "ctrlIoHeatingElement0/Phase1AvgPower": 2000,
                    "ctrlIoHeatingElement0/Phase2AvgPower": 2000,
                    "ctrlIoHeatingElement0/Phase3AvgPower": 2000,
                },
            });

            expect(modal.maxPower).toBe(30);
        });

        it("does not reduce the slider maximum when the rounded predicted energy is lower", () => {
            const { modal } = createModalStub(createHeatingElementComponent(), null);
            modal.config = DummyConfig.from(DummyConfig.Component.SOCOMEC_CONSUMPTION_METER("meter0"));
            modal.maxPower = 16;

            modal.onCurrentData({
                allComponents: {
                    "ctrlIoHeatingElement0/Phase1AvgPower": 1100,
                    "ctrlIoHeatingElement0/Phase2AvgPower": 1100,
                    "ctrlIoHeatingElement0/Phase3AvgPower": 1100,
                },
            });

            expect(modal.maxPower).toBe(16);
        });

        for (const scenario of [
            { requiredPower: 1000, isUnreachable: true },
            { requiredPower: 0, isUnreachable: false },
            { requiredPower: null, isUnreachable: false },
        ]) {
            it(`treats missing phase powers as zero when required power is ${scenario.requiredPower}`, () => {
                const { modal } = createModalStub(createHeatingElementComponent(), null);
                modal.config = DummyConfig.from(DummyConfig.Component.SOCOMEC_CONSUMPTION_METER("meter0"));
                modal.maxPower = 15;
                modal.isUnreachable = !scenario.isUnreachable;
                spyOn(modal, "getRequiredPower").and.returnValue(scenario.requiredPower);

                modal.onCurrentData({
                    allComponents: {
                        "ctrlIoHeatingElement0/Phase1AvgPower": null,
                        "ctrlIoHeatingElement0/Phase2AvgPower": null,
                        "ctrlIoHeatingElement0/Phase3AvgPower": null,
                    },
                });

                expect(modal.maxPower).toBe(15);
                expect(modal.isUnreachable).toBe(scenario.isUnreachable);
            });
        }

        it("calculates levelOptions and modeButtons once on initialization and updates on language change", () => {
            const component = createHeatingElementComponent({ numberOfHeatingElements: "TWO" });
            const newEdge = createEdgeWithVersion(NEW_EDGE_VERSION);
            const { modal, translateSpy, langChange$ } = createModalStub(component, newEdge);

            expect(modal.levelOptions.map((o: any) => o.value)).toEqual([
                "LEVEL_1",
                "LEVEL_2",
                "LEVEL_3",
                "LEVEL_4",
                "LEVEL_5",
                "LEVEL_6",
            ]);
            expect(modal.modeButtons.map((b: any) => b.value)).toEqual([
                Mode.MANUAL_ON,
                "FAST_HEAT",
                Mode.AUTOMATIC,
                Mode.MANUAL_OFF,
            ]);

            translateSpy.instant.calls.reset();
            // Reading properties multiple times (as happens during 100ms change detection) does not rebuild options
            const _read1 = modal.levelOptions;
            const _read2 = modal.levelOptions;
            const _read3 = modal.modeButtons;
            expect(_read1).toBe(_read2);
            expect(_read3).toBe(modal.modeButtons);
            expect(translateSpy.instant).not.toHaveBeenCalled();

            // Emitting language change recomputes the cached options
            langChange$.emit({ lang: "en" });
            expect(translateSpy.instant).toHaveBeenCalled();
        });

        for (const scenario of [
            { name: "legacy ONE", version: OLD_EDGE_VERSION, count: "ONE", maxLevel: 3, fastHeat: false },
            { name: "unknown Edge", version: "unknown", count: "TWO", maxLevel: 3, fastHeat: false },
            { name: "new ONE", version: NEW_EDGE_VERSION, count: "ONE", maxLevel: 3, fastHeat: true },
            { name: "new TWO", version: NEW_EDGE_VERSION, count: "TWO", maxLevel: 6, fastHeat: true },
        ]) {
            it(`restricts selectable values and preserves shared modal saving for ${scenario.name}`, async () => {
                const component = createHeatingElementComponent({ numberOfHeatingElements: scenario.count });
                const edge = createEdgeWithVersion(scenario.version);
                const updateSpy = spyOn(edge, "updateComponentConfig").and.resolveTo();
                spyOn(testContext.service, "getCurrentEdge").and.resolveTo(edge);
                spyOn(testContext.service, "startSpinner");
                const stopSpinner = spyOn(testContext.service, "stopSpinner");
                const toast = spyOn(testContext.service, "toast");
                const { modal } = createModalStub(component, edge);
                const websocket = jasmine.createSpyObj<Websocket>("Websocket", ["sendRequest"]);
                const sharedModal = new (class extends OeModalComponent {
                    constructor() {
                        super(
                            jasmine.createSpyObj<ModalController>("ModalController", ["dismiss"]),
                            websocket,
                            testContext.service,
                            testContext.translate,
                        );
                        this.component = component;
                        this.formGroup = modal.formGroup;
                    }
                })();
                await Promise.resolve();

                const expectedModes = scenario.fastHeat
                    ? [Mode.MANUAL_ON, "FAST_HEAT", Mode.AUTOMATIC, Mode.MANUAL_OFF]
                    : [Mode.MANUAL_ON, Mode.AUTOMATIC, Mode.MANUAL_OFF];
                const expectedLevels = Array.from({ length: scenario.maxLevel }, (_, i) => `LEVEL_${i + 1}`);
                expect(modal.modeButtons.map((button: { value: string }) => button.value)).toEqual(expectedModes);
                expect(modal.levelOptions.map((option: { value: string }) => option.value)).toEqual(expectedLevels);
                expect(modal.formGroup.pristine).toBeTrue();

                for (const mode of expectedModes) {
                    for (const level of expectedLevels) {
                        modal.formGroup.controls["mode"].setValue(mode);
                        modal.formGroup.controls["mode"].markAsDirty();
                        modal.formGroup.controls["defaultLevel"].setValue(level);
                        modal.formGroup.controls["defaultLevel"].markAsDirty();
                        expect(modal.formGroup.dirty).toBeTrue();

                        await new Promise<void>((resolve) => {
                            stopSpinner.and.callFake(() => resolve());
                            sharedModal.applyChanges();
                        });

                        expect(updateSpy).toHaveBeenCalledWith(websocket, component.id, [
                            { name: "defaultLevel", value: level },
                            { name: "mode", value: mode },
                        ]);
                        expect(modal.formGroup.pristine).toBeTrue();
                        expect(toast).toHaveBeenCalledWith(
                            testContext.translate.instant("GENERAL.CHANGE_ACCEPTED"),
                            "success",
                        );
                        expect(stopSpinner).toHaveBeenCalledWith("spinner");
                        updateSpy.calls.reset();
                    }
                }
            });
        }
    });

    describe("History chart and history flat widget", () => {
        it("6. History chart requests only supported level channels and uses correct Y-axis scaling", () => {
            const meter = DummyConfig.Component.SOCOMEC_CONSUMPTION_METER("meter0", "Meter");
            const twoComponent = createHeatingElementComponent({ numberOfHeatingElements: "TWO" });
            const edgeConfig = DummyConfig.from(meter);
            edgeConfig.components[twoComponent.id] = twoComponent as any;

            const oldEdge = createEdgeWithVersion(OLD_EDGE_VERSION);
            const newEdge = createEdgeWithVersion(NEW_EDGE_VERSION);

            // Older Edge with TWO property falls back to Levels 1-3 and HEATING_ELEMENT axis
            const oldLineChart = ControllerIoHeatingElementChartComponent.getChartData(
                edgeConfig,
                testContext.translate,
                twoComponent,
                PHASE_COLORS,
                "line",
                oldEdge,
            );
            expect(
                oldLineChart.input
                    .filter((i) => i.energyChannel?.channelId.startsWith("Level"))
                    .map((i) => i.energyChannel?.channelId),
            ).toEqual(["Level1CumulatedTime", "Level2CumulatedTime", "Level3CumulatedTime"]);
            expect(oldLineChart.yAxes).toContain(
                jasmine.objectContaining({ unit: YAxisType.HEATING_ELEMENT, yAxisId: ChartAxis.LEFT }),
            );

            // New Edge with ONE uses Levels 1-3 and HEATING_ELEMENT axis
            const oneComponent = createHeatingElementComponent({ numberOfHeatingElements: "ONE" });
            const newOneLineChart = ControllerIoHeatingElementChartComponent.getChartData(
                edgeConfig,
                testContext.translate,
                oneComponent,
                PHASE_COLORS,
                "line",
                newEdge,
            );
            expect(
                newOneLineChart.input
                    .filter((i) => i.energyChannel?.channelId.startsWith("Level"))
                    .map((i) => i.energyChannel?.channelId),
            ).toEqual(["Level1CumulatedTime", "Level2CumulatedTime", "Level3CumulatedTime"]);
            expect(newOneLineChart.yAxes).toContain(
                jasmine.objectContaining({ unit: YAxisType.HEATING_ELEMENT, yAxisId: ChartAxis.LEFT }),
            );

            // New Edge with TWO uses Levels 1-6 and HEATING_ELEMENT_TWO axis for line, TIME axis for bar
            const newTwoLineChart = ControllerIoHeatingElementChartComponent.getChartData(
                edgeConfig,
                testContext.translate,
                twoComponent,
                PHASE_COLORS,
                "line",
                newEdge,
            );
            expect(
                newTwoLineChart.input
                    .filter((i) => i.energyChannel?.channelId.startsWith("Level"))
                    .map((i) => i.energyChannel?.channelId),
            ).toEqual([
                "Level1CumulatedTime",
                "Level2CumulatedTime",
                "Level3CumulatedTime",
                "Level4CumulatedTime",
                "Level5CumulatedTime",
                "Level6CumulatedTime",
            ]);
            expect(newTwoLineChart.yAxes).toContain(
                jasmine.objectContaining({ unit: YAxisType.HEATING_ELEMENT_TWO, yAxisId: ChartAxis.LEFT }),
            );

            const newTwoBarChart = ControllerIoHeatingElementChartComponent.getChartData(
                edgeConfig,
                testContext.translate,
                twoComponent,
                PHASE_COLORS,
                "bar",
                newEdge,
            );
            expect(newTwoBarChart.yAxes).toContain(
                jasmine.objectContaining({ unit: YAxisType.TIME, yAxisId: ChartAxis.LEFT }),
            );

            // Verify Level 0 line values stay at 0 (0 * 1000 = 0)
            const lineDatasets = newTwoLineChart.output({
                [twoComponent.id]: [0, 3, 6],
                "meter0/ActivePower": [0, 1000, 2000],
            });
            expect(lineDatasets[0].converter()).toEqual([0, 3000, 6000]);
        });

        it("History flat widget uses supported levels based on HeatingElementConfig", () => {
            const twoComponent = createHeatingElementComponent({ numberOfHeatingElements: "TWO" });
            const oldEdge = createEdgeWithVersion(OLD_EDGE_VERSION);
            const newEdge = createEdgeWithVersion(NEW_EDGE_VERSION);

            const flatWidget: any = Object.create(ControllerHeatingElementChartComponent.prototype);
            flatWidget.component = twoComponent;
            flatWidget.config = { getComponent: () => new EdgeConfig.Component() };

            flatWidget.edge = oldEdge;
            flatWidget.afterIsInitialized();
            expect(flatWidget.levels).toEqual([1, 2, 3]);

            flatWidget.edge = newEdge;
            flatWidget.afterIsInitialized();
            expect(flatWidget.levels).toEqual([1, 2, 3, 4, 5, 6]);
        });
    });
});
