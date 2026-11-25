import { ApplicationInitStatus, ChangeDetectorRef, signal } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { FormGroup } from "@angular/forms";
import { By } from "@angular/platform-browser";
import { ActivatedRoute } from "@angular/router";
import { IonicModule, ModalController } from "@ionic/angular";
import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { ModalButtonsComponent } from "src/app/shared/components/modal/modal-button/modal-button";
import { ModalComponentsModule } from "src/app/shared/components/modal/modal.module";
import { NavigationTree } from "src/app/shared/components/navigation/shared";
import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/utils.spec";
import { CurrentData, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { Widgets } from "src/app/shared/type/widgets";
import globalDe from "src/assets/i18n/de.json";
import { environment } from "src/environments";
import heatDe from "../../../Heat/i18n/de.json";
import { HeatingElementMode } from "../util/utils";
import { ControllerIoHeatingElementBaseModeComponent } from "./basemode";

class BaseModeHarness extends ControllerIoHeatingElementBaseModeComponent {
    private testComponent!: EdgeConfig.Component;
    private testEdge!: Edge;

    public static create(component: EdgeConfig.Component, edge: Edge): BaseModeHarness {
        const instance = Object.create(BaseModeHarness.prototype) as BaseModeHarness;
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

describe("HeatingElement base mode", () => {
    let testContext: TestContext;
    let config: EdgeConfig;
    let component: EdgeConfig.Component;
    let edge: Edge;

    beforeEach(async () => {
        testContext = await TestingUtils.sharedSetup();
        testContext.translate.setTranslation("de", globalDe, true);
        testContext.translate.setTranslation("de", heatDe, true);
        const fixture = DummyConfig.Component.CONTROLLER_IO_HEATINGELEMENT();
        fixture.properties = { mode: HeatingElementMode.AUTOMATIC, minTime: 2, minEnergylimit: 5000 };
        config = DummyConfig.from(fixture);
        component = config.getComponent("ctrlIoHeatingElement0");
        edge = DummyConfig.dummyEdge({ role: Role.ADMIN, version: "2026.11.1" });
    });

    for (const scenario of [
        { role: Role.ADMIN, version: "2026.11.1", allowed: true },
        { role: Role.ADMIN, version: "2026.10.1", allowed: false },
        { role: Role.OWNER, version: "2026.11.1", allowed: false },
        { role: Role.INSTALLER, version: "2026.11.1", allowed: false },
    ]) {
        it(`offers base mode only for eligible ${Role[scenario.role]} on ${scenario.version}`, () => {
            const routedEdge = DummyConfig.dummyEdge({ role: scenario.role, version: scenario.version });
            const params = Widgets.getControllerNavigationTree(
                routedEdge,
                { name: "Controller.IO.HeatingElement", componentId: component.id, alias: component.alias },
                testContext.translate,
                config,
            );
            expect(params).not.toBeNull();
            const tree = new NavigationTree(...params!);

            const baseMode = tree.children.find((child) => child.id === component.id + "-base-mode");
            expect(baseMode != null).toBe(scenario.allowed);
            if (scenario.allowed) {
                expect(baseMode?.routerLink.baseString).toBe("base-mode");
            }
            expect(tree.children.some((child) => child.routerLink.baseString === "settings")).toBeTrue();
        });
    }

    it("hides base mode until the edge role is known", () => {
        Object.defineProperty(edge, "role", { value: null });
        const params = Widgets.getControllerNavigationTree(
            edge,
            { name: "Controller.IO.HeatingElement", componentId: component.id, alias: component.alias },
            testContext.translate,
            config,
        );
        expect(params).not.toBeNull();
        expect(
            new NavigationTree(...params!).children.some((child) => child.id === component.id + "-base-mode"),
        ).toBeFalse();
    });

    it("offers exactly three controller modes without protection-cycle promises", () => {
        const view = ControllerIoHeatingElementBaseModeComponent.generateView(testContext.translate, component, edge);
        const line = view.lines.find((entry) => entry.type === "radio-buttons-from-form-control-line");
        expect(line?.controlName).toBe("mode");
        expect(
            line?.buttons.map((button) => ({
                name: button.name,
                value: button.value,
                description: button.description,
            })),
        ).toEqual([
            {
                name: "Schnell heizen",
                value: HeatingElementMode.FAST_HEAT,
                description: "Der Heizstab heizt mit der maximal verfügbaren Heizstufe.",
            },
            {
                name: "PV-Überschuss",
                value: HeatingElementMode.AUTOMATIC,
                description: "Der Heizstab passt seine Heizleistung automatisch an den verfügbaren PV-Überschuss an.",
            },
            {
                name: "Keine Beheizung",
                value: HeatingElementMode.MANUAL_OFF,
                description: "Der Heizstab bleibt ausgeschaltet.",
            },
        ]);
    });

    it("shows a small heating element title icon and generic artwork above the mode options", () => {
        const view = ControllerIoHeatingElementBaseModeComponent.generateView(testContext.translate, component, edge);
        expect(view.title).toBe("Grundmodus");
        expect(view.icon?.name).toBe("oe-heating-element");
        expect(view.icon?.size).toBe("small");
        expect(view.lines.map((line) => line.type)).toEqual(["image-line", "radio-buttons-from-form-control-line"]);
        const image = view.lines.find((line) => line.type === "image-line");
        expect(image?.img?.url).toBe(environment.icons.COMPONENT.HEATING_ELEMENT);
        expect(image?.img?.width).toBeGreaterThan(20);
    });

    it("reads MANUAL_ON without replacing it, dirtying the form or writing", () => {
        const update = spyOn(edge, "updateComponentConfig").and.resolveTo();
        const page = BaseModeHarness.create(component, edge);
        page.receive({ allComponents: { [component.id + "/_PropertyMode"]: HeatingElementMode.MANUAL_ON } });

        expect(page.getForm().value).toEqual({ mode: HeatingElementMode.MANUAL_ON });
        expect(page.getForm().pristine).toBeTrue();
        expect(update).not.toHaveBeenCalled();
        page.save(testContext, component, edge);
        expect(update).not.toHaveBeenCalled();
    });

    it("does not send an empty update when FAST_HEAT is filtered for an older edge", () => {
        const oldEdge = DummyConfig.dummyEdge({ role: Role.ADMIN, version: "2026.10.1" });
        const update = spyOn(oldEdge, "updateComponentConfig").and.resolveTo();
        const page = BaseModeHarness.create(component, oldEdge);
        page.getForm().controls["mode"].setValue(HeatingElementMode.FAST_HEAT);
        page.getForm().controls["mode"].markAsDirty();

        page.save(testContext, component, oldEdge);

        expect(update).not.toHaveBeenCalled();
        expect(page.getForm().pristine).toBeTrue();
    });

    for (const mode of [HeatingElementMode.FAST_HEAT, HeatingElementMode.AUTOMATIC, HeatingElementMode.MANUAL_OFF]) {
        it(`reads ${mode} without dirtying the form`, () => {
            const page = BaseModeHarness.create(component, edge);
            page.receive({ allComponents: { [component.id + "/_PropertyMode"]: mode } });

            expect(page.getForm().controls["mode"].value).toBe(mode);
            expect(page.getForm().pristine).toBeTrue();
        });
    }

    it("saves the selected mode and reports the accepted update", async () => {
        const update = spyOn(edge, "updateComponentConfig").and.resolveTo();
        const startSpinner = spyOn(testContext.service, "startSpinner");
        const stopSpinner = spyOn(testContext.service, "stopSpinner");
        const toast = spyOn(testContext.service, "toast").and.resolveTo();
        const stoppedSpinner = new Promise<void>((resolve) => stopSpinner.and.callFake(() => resolve()));
        const page = BaseModeHarness.create(component, edge);
        const mode = HeatingElementMode.FAST_HEAT;
        page.receive({ allComponents: { [component.id + "/_PropertyMode"]: HeatingElementMode.AUTOMATIC } });
        page.getForm().controls["mode"].setValue(mode);
        page.getForm().controls["mode"].markAsDirty();

        page.save(testContext, component, edge);

        expect(update).toHaveBeenCalledWith(testContext.service.websocket, component.id, [
            { name: "mode", value: mode },
        ]);
        await stoppedSpinner;
        expect(startSpinner).toHaveBeenCalledWith("formly-field-modal");
        expect(toast).toHaveBeenCalledWith(testContext.translate.instant("GENERAL.CHANGE_ACCEPTED"), "success");
        expect(stopSpinner).toHaveBeenCalledWith("formly-field-modal");
        expect(page.getForm().pristine).toBeTrue();
    });
});

describe("HeatingElement base mode radio rendering", () => {
    for (const scenario of [
        { mode: HeatingElementMode.MANUAL_ON, selectedMode: null },
        { mode: HeatingElementMode.MANUAL_OFF, selectedMode: HeatingElementMode.MANUAL_OFF },
    ]) {
        it(`renders ${scenario.mode} with the expected radio selection`, async () => {
            const testContext = await TestingUtils.mergeSetup<TestContext>([
                {
                    name: "service",
                    provider: Service,
                    metadata: {
                        imports: [IonicModule.forRoot(), ModalComponentsModule],
                        providers: [
                            { provide: Websocket, useValue: {} },
                            { provide: ActivatedRoute, useValue: { snapshot: { params: {} } } },
                            { provide: ModalController, useValue: {} },
                        ],
                    },
                },
            ]);
            await TestBed.inject(ApplicationInitStatus).donePromise;
            expect(customElements.get("ion-radio")).toBeDefined();
            const config = DummyConfig.from(DummyConfig.Component.CONTROLLER_IO_HEATINGELEMENT());
            const component = config.getComponent("ctrlIoHeatingElement0");
            const edge = DummyConfig.dummyEdge({ version: "2026.11.1" });
            testContext.translate.setTranslation("de", globalDe, true);
            testContext.translate.setTranslation("de", heatDe, true);
            spyOn(testContext.service, "getCurrentEdge").and.resolveTo(edge);
            spyOn(testContext.service, "getConfig").and.resolveTo(config);
            const update = spyOn(edge, "updateComponentConfig").and.resolveTo();
            const page = BaseModeHarness.create(component, edge);
            page.receive({ allComponents: { [component.id + "/_PropertyMode"]: scenario.mode } });
            const view = ControllerIoHeatingElementBaseModeComponent.generateView(
                testContext.translate,
                component,
                edge,
            );
            const line = view.lines.find((entry) => entry.type === "radio-buttons-from-form-control-line");
            const fixture = TestBed.createComponent(ModalButtonsComponent);
            fixture.componentRef.setInput("formGroup", page.getForm());
            fixture.componentRef.setInput("component", component);
            fixture.componentRef.setInput("controlName", "mode");
            fixture.componentRef.setInput("control", { type: "RADIO" });
            fixture.componentRef.setInput("buttons", line!.buttons);
            fixture.debugElement.injector.get(ChangeDetectorRef).reattach();
            fixture.detectChanges();
            await fixture.whenStable();
            fixture.detectChanges();

            const radios = fixture.debugElement.queryAll(By.css("ion-radio"));
            expect(radios.length).withContext(fixture.nativeElement.innerHTML).toBe(3);
            await Promise.all(radios.map((radio) => (radio.nativeElement as HTMLIonRadioElement).componentOnReady()));
            fixture.detectChanges();
            expect((fixture.nativeElement as HTMLElement).textContent).toContain(
                "Der Heizstab heizt mit der maximal verfügbaren Heizstufe.",
            );
            expect((fixture.nativeElement as HTMLElement).textContent).toContain(
                "Der Heizstab passt seine Heizleistung automatisch an den verfügbaren PV-Überschuss an.",
            );
            expect((fixture.nativeElement as HTMLElement).textContent).toContain("Der Heizstab bleibt ausgeschaltet.");
            const selectedModes = radios
                .filter((radio) => (radio.nativeElement as HTMLIonRadioElement).getAttribute("aria-checked") === "true")
                .map((radio) => (radio.nativeElement as HTMLIonRadioElement).value);
            expect(selectedModes)
                .withContext(fixture.nativeElement.innerHTML)
                .toEqual(scenario.selectedMode === null ? [] : [scenario.selectedMode]);
            expect(page.getForm().controls["mode"].value).toBe(scenario.mode);
            expect(page.getForm().pristine).toBeTrue();
            expect(update).not.toHaveBeenCalled();
            fixture.destroy();
        });
    }
});
