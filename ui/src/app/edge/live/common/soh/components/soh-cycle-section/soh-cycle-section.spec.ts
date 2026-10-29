import { signal } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormBuilder, FormControl } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { AlertController, IonicModule } from "@ionic/angular";

import { BASE_TEST_BED } from "src/app/shared/components/shared/testing/utils.spec";
import { EdgeConfig, Websocket } from "src/app/shared/shared";
import { SohDeterminationService } from "../../service/soh-determination.service";
import { SohCycleSectionComponent } from "./soh-cycle-section";

describe("SohCycleSectionComponent", () => {
    let fixture: ComponentFixture<SohCycleSectionComponent>;
    let component: SohCycleSectionComponent;
    let sohDeterminationServiceSpy: jasmine.SpyObj<SohDeterminationService>;
    let edgeSpy: jasmine.SpyObj<any>;

    beforeEach(async () => {
        sohDeterminationServiceSpy = jasmine.createSpyObj<SohDeterminationService>("SohDeterminationService", [
            "getIsSohCycleRunningSignal",
            "getIsSohCycleRunningWithoutErrorSignal",
            "getIsSohCycleRunningWithErrorSignal",
            "getIsSohCycleCapacityMeasuredSignal",
        ]);
        sohDeterminationServiceSpy.getIsSohCycleRunningSignal.and.returnValue(signal(false));
        sohDeterminationServiceSpy.getIsSohCycleRunningWithoutErrorSignal.and.returnValue(signal(false));
        sohDeterminationServiceSpy.getIsSohCycleRunningWithErrorSignal.and.returnValue(signal(false));
        sohDeterminationServiceSpy.getIsSohCycleCapacityMeasuredSignal.and.returnValue(signal(false));

        edgeSpy = jasmine.createSpyObj("Edge", ["updateComponentConfig"]);
        edgeSpy.updateComponentConfig.and.returnValue(Promise.resolve());

        await TestBed.configureTestingModule({
            imports: [SohCycleSectionComponent, IonicModule.forRoot(), ...(BASE_TEST_BED.imports ?? [])],
            providers: [
                FormBuilder,
                { provide: Websocket, useValue: {} },
                { provide: ActivatedRoute, useValue: { snapshot: { params: {} } } },
                ...(BASE_TEST_BED.providers ?? []),
                { provide: SohDeterminationService, useValue: sohDeterminationServiceSpy },
                {
                    provide: AlertController,
                    useValue: jasmine.createSpyObj("AlertController", {
                        create: Promise.resolve({ present: () => Promise.resolve() }),
                    }),
                },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(SohCycleSectionComponent);
        component = fixture.componentInstance;

        const formBuilder = TestBed.inject(FormBuilder);
        const essSohCycleController = formBuilder.group({
            controllerId: new FormControl("ctrlEssSohCycle0"),
            enabled: new FormControl(true),
            isRunning: new FormControl(false),
            stateMachine: new FormControl(1),
            stateMachineDisplay: new FormControl("IDLE"),
            sohPercent: new FormControl(null),
            logVerbosity: new FormControl("NONE"),
            measuredCapacity: new FormControl(null),
            isBatteryBalanced: new FormControl(false),
            isMeasured: new FormControl(false),
        });
        const componentFormGroup = formBuilder.group({ essSohCycleController });
        const rootFormGroup = formBuilder.group({ ctrlEssSohCycle0: componentFormGroup });

        component.component = { id: "ctrlEssSohCycle0" } as EdgeConfig.Component;
        component.formGroup = rootFormGroup;
        component.spinnerId = "spinner-1";
        component.edge = edgeSpy;

        fixture.detectChanges();
    });

    it("requires fresh PV-off confirmation for each accepted SoH run", async () => {
        await component.startSohCycle();
        expect(edgeSpy.updateComponentConfig).not.toHaveBeenCalled();

        component["pvOffFormGroup"].get("pvOffConfirmed")?.setValue(true);
        await component.startSohCycle();
        expect(edgeSpy.updateComponentConfig).toHaveBeenCalledTimes(1);
        expect(component.isPvOffConfirmed).toBe(false);

        sohDeterminationServiceSpy.getIsSohCycleCapacityMeasuredSignal.and.returnValue(signal(true));
        const alertCtrl = TestBed.inject(AlertController) as jasmine.SpyObj<AlertController>;
        await component.showRestartConfirmation();
        expect(alertCtrl.create).not.toHaveBeenCalled();

        component["pvOffFormGroup"].get("pvOffConfirmed")?.setValue(true);
        await component.showRestartConfirmation();
        expect(alertCtrl.create).toHaveBeenCalledTimes(1);
        const buttons = alertCtrl.create.calls.mostRecent().args[0]?.buttons;
        if (!Array.isArray(buttons)) {
            fail("Expected restart confirmation buttons");
            return;
        }

        const restartButtonConfig = buttons.find((button) => typeof button !== "string" && button.role !== "cancel");
        expect(restartButtonConfig).toBeDefined();
        if (typeof restartButtonConfig === "string" || restartButtonConfig == null) {
            return;
        }
        restartButtonConfig.handler?.(undefined);
        await fixture.whenStable();
        expect(edgeSpy.updateComponentConfig).toHaveBeenCalledTimes(2);
        expect(component.isPvOffConfirmed).toBe(false);
        await component.showRestartConfirmation();
        expect(alertCtrl.create).toHaveBeenCalledTimes(1);
    });

    it("disables Start and Restart until PV-off is confirmed", () => {
        const nativeElement = fixture.nativeElement as HTMLElement;
        expect(nativeElement.textContent).toContain("STORAGE.ADMIN_MODAL.SOH.PV_OFF_CONFIRMATION");
        const startButton = nativeElement.querySelector("ion-item.soh-restart-action ion-button") as
            (HTMLElement & { disabled: boolean }) | null;
        expect(startButton?.disabled).toBe(true);

        sohDeterminationServiceSpy.getIsSohCycleCapacityMeasuredSignal.and.returnValue(signal(true));
        fixture.detectChanges();
        const restartButton = nativeElement.querySelector("ion-item.soh-restart-action ion-button") as
            (HTMLElement & { disabled: boolean }) | null;
        expect(restartButton?.disabled).toBe(true);
    });
});
