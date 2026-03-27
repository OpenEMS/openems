import { ChangeDetectionStrategy, Component, inject, Input, OnChanges, signal, SimpleChanges } from "@angular/core";
import { FormControl, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { AlertController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { NgxSpinnerModule } from "ngx-spinner";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { ModalComponentsModule } from "src/app/shared/components/modal/modal.module";
import { Converter } from "src/app/shared/components/shared/converter";
import { EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { SohCycleState, SohDeterminationService } from "../../service/soh-determination.service";

type SohCycleControllerValue = Partial<SohCycleState> & {
    controllerId?: string;
    enabled?: boolean;
    logVerbosity?: string;
    isRunning?: boolean | null;
};

@Component({
    selector: "soh-cycle-section",
    templateUrl: "./soh-cycle-section.html",
    styleUrl: "./soh-cycle-section.scss",
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [
        CommonUiModule,
        ComponentsBaseModule,
        ModalComponentsModule,
        NgxSpinnerModule,
        ReactiveFormsModule,
    ],
})
export class SohCycleSectionComponent implements OnChanges {
    @Input({ required: true }) public component!: EdgeConfig.Component;
    @Input({ required: true }) public formGroup!: FormGroup;
    @Input({ required: true }) public spinnerId!: string;
    @Input({ required: true }) public edge!: any; // Edge instance for updates

    public readonly Converter = Converter;

    private readonly essId = signal<string>("");
    private sohDeterminationService = inject(SohDeterminationService);
    private alertCtrl = inject(AlertController);
    private translate = inject(TranslateService);
    private service = inject(Service);
    private websocket = inject(Websocket);

    public get isSohCycleLoading(): boolean {
        return this.sohCycleController?.["isRunning"] === null;
    }

    public get isSohCycleRunningWithoutError(): boolean {
        return this.sohDeterminationService.getIsSohCycleRunningWithoutErrorSignal(this.essId())();
    }

    public get isSohCycleRunningWithError(): boolean {
        return this.sohDeterminationService.getIsSohCycleRunningWithErrorSignal(this.essId())();
    }

    public get isSohCycleRunning(): boolean {
        return this.sohDeterminationService.getIsSohCycleRunningSignal(this.essId())();
    }

    public get isSohCycleCapacityMeasured(): boolean {
        return this.sohDeterminationService.getIsSohCycleCapacityMeasuredSignal(this.essId())();
    }

    public get isSohCycleEnabled(): boolean {
        return this.sohCycleController?.["enabled"] === true;
    }

    public get isDebugLogEnabled(): boolean {
        return this.sohCycleController?.["logVerbosity"] === "DEBUG_LOG";
    }

    public get isSohCycleAvailable(): boolean {
        return !!this.componentFormControl
            && !!this.sohCycleController
            && this.isSohCycleEnabled;
    }

    public get sohCycleControllerFormGroup(): FormGroup | null {
        return this.componentFormControl?.get("essSohCycleController") as FormGroup | null;
    }

    public get sohCycleController(): SohCycleControllerValue | null {
        return this.sohCycleControllerFormGroup?.value as SohCycleControllerValue | null;
    }

    private get componentFormControl(): FormGroup | null {
        return this.formGroup.get(this.component.id) as FormGroup | null;
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes["component"]) {
            this.essId.set(this.component?.id ?? "");
        }
    }

    public async startSohCycle(): Promise<void> {
        if (this.edge == null) {
            return;
        }

        const isRunningControl = this.sohCycleControllerFormGroup?.get("isRunning") as FormControl | null;
        const controllerId = this.sohCycleController?.controllerId;

        if (isRunningControl == null || controllerId == null) {
            return;
        }

        const previousIsRunning = isRunningControl.value;
        isRunningControl.setValue(true);
        isRunningControl.markAsDirty();

        try {
            await this.edge.updateComponentConfig(this.websocket, controllerId, [
                { name: "isRunning", value: true },
            ]);
            isRunningControl.markAsPristine();
            this.service.toast(this.translate.instant("GENERAL.CHANGE_ACCEPTED"), "success");
        } catch (error) {
            isRunningControl.setValue(previousIsRunning);
            isRunningControl.markAsPristine();
            this.service.toast(this.translate.instant("GENERAL.CHANGE_FAILED") + "\n" + error, "danger");
        }
    }

    public async showRestartConfirmation(): Promise<void> {
        const alert = await this.alertCtrl.create({
            header: this.translate.instant("STORAGE.ADMIN_MODAL.SOH.RESTART_CONFIRMATION_TITLE"),
            message: this.translate.instant("STORAGE.ADMIN_MODAL.SOH.RESTART_WARNING"),
            buttons: [
                {
                    text: this.translate.instant("GENERAL.CANCEL"),
                    role: "cancel",
                },
                {
                    text: this.translate.instant("GENERAL.OK"),
                    handler: () => {
                        void this.startSohCycle();
                    },
                },
            ],
            cssClass: "alertController",
        });

        await alert.present();
    }
}

