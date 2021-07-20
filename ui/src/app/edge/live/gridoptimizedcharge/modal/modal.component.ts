import { Component, Input, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { IonContent, ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from 'src/app/shared/shared';
import { Role } from 'src/app/shared/type/role';

type Mode = 'MANUAL' | 'OFF' | 'AUTOMATIC';

@Component({
    selector: GridOptimizedChargeModalComponent.SELECTOR,
    templateUrl: './modal.component.html',
})
export class GridOptimizedChargeModalComponent implements OnInit {

    @Input() public edge: Edge;
    @Input() public component: EdgeConfig.Component;
    @ViewChild(IonContent) content: IonContent;

    private static readonly SELECTOR = "gridoptimizedcharge-modal";

    public formGroup: FormGroup;
    public loading: boolean = false;
    public pickerOptions: any;
    public channelCapacity: string = null;
    public isInstaller: boolean;
    public image: string | null;
    public riskDescription: Description[] | null;
    public descriptionStateEnum: typeof DescriptionState = DescriptionState;
    public refreshChart: boolean;

    constructor(
        public formBuilder: FormBuilder,
        public modalCtrl: ModalController,
        public service: Service,
        public translate: TranslateService,
        public websocket: Websocket,
    ) { }

    ngOnInit() {
        this.refreshChart = false;
        if (this.edge.roleIsAtLeast(Role.ADMIN) && 'ess.id' in this.component.properties) {
            let channelCapacity = new ChannelAddress(this.component.properties['ess.id'], "Capacity")

            this.channelCapacity = channelCapacity.toString();
            this.edge.subscribeChannels(this.websocket,
                GridOptimizedChargeModalComponent.SELECTOR + this.component.id,
                [channelCapacity, new ChannelAddress(this.component.id, "TargetEpochSeconds")]);
        }
        if (this.edge.roleIsAtLeast(Role.INSTALLER)) {
            this.isInstaller = true;
        }
        this.formGroup = this.formBuilder.group({
            sellToGridLimitEnabled: new FormControl(this.component.properties.sellToGridLimitEnabled),
            maximumSellToGridPower: new FormControl(this.component.properties.maximumSellToGridPower, Validators.compose([
                Validators.pattern('^(?:[1-9][0-9]*|0)$'),
                Validators.required
            ])),
            delayChargeRiskLevel: new FormControl(this.component.properties.delayChargeRiskLevel),
            manualTargetTime: new FormControl(this.component.properties.manualTargetTime),
        })
    };

    updateMode(event: CustomEvent, currentController: EdgeConfig.Component) {
        let oldMode = currentController.properties.mode;
        let newMode: Mode = event.detail.value;
        this.formGroup.markAsPristine()

        if (this.edge != null) {
            this.edge.updateComponentConfig(this.websocket, currentController.id, [
                { name: 'mode', value: newMode }
            ]).then(() => {
                currentController.properties.mode = newMode;
                this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
            }).catch(reason => {
                currentController.properties.mode = oldMode;
                this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
                console.warn(reason);
            });
        }
    }

    updateProperty(property: string, event: CustomEvent) {
        this.formGroup.controls[property].setValue(event.detail.value);
        this.formGroup.controls[property].markAsDirty()
    }

    updateToggleProperty(property: string, oldValue: boolean) {
        this.formGroup.controls[property].setValue(!oldValue);
        this.formGroup.controls[property].markAsDirty()
    }

    showPreview(value: string) {
        //TODO: Translate (this.translate.instant()) when the text is final

        // Use images if there is a more intelligent risk algorithm 
        switch (value) {
            case 'LOW':
                this.riskDescription = [];
                this.riskDescription.push({ state: DescriptionState.POSITIVE, text: "Sehr große Wahrscheinlichkeit, dass der Speicher voll wird" });
                this.riskDescription.push({ state: DescriptionState.NEGATIVE, text: "Größere Wahrscheinlichkeit, dass die PV abgeregelt wird, weil der Speicher bereits voll ist" });
                break;
            case 'MEDIUM':
                this.riskDescription = [];
                this.riskDescription.push({ state: DescriptionState.NEUTRAL, text: "Große Wahrscheinlichkeit, dass der Speicher voll wird" });
                this.riskDescription.push({ state: DescriptionState.NEUTRAL, text: "Niedrigere Wahrscheinlichkeit, dass die PV abgeregelt wird, weil der Speicher bereits voll ist" });
                break;
            case 'HIGH':
                this.riskDescription = [];
                this.riskDescription.push({ state: DescriptionState.NEGATIVE, text: "Niedrigere Wahrscheinlichkeit, dass der Speicher voll wird" });
                this.riskDescription.push({ state: DescriptionState.POSITIVE, text: "Sehr geringe Wahrscheinlichkeit, dass die PV abgeregelt wird" });
                break;
        }

        // Only needed if the risk choice is the last content in the widget
        this.content.scrollToBottom(300);
    }

    applyChanges() {
        if (this.edge != null) {
            if (this.edge.roleIsAtLeast('owner')) {
                let updateComponentArray = [];
                Object.keys(this.formGroup.controls).forEach((element, index) => {
                    if (this.formGroup.controls[element].dirty) {
                        updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value })
                    }
                });

                this.loading = true;
                this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {
                    this.component.properties.sellToGridLimitEnabled = this.formGroup.controls['sellToGridLimitEnabled'].value;
                    this.component.properties.maximumSellToGridPower = this.formGroup.controls['maximumSellToGridPower'].value;
                    this.component.properties.delayChargeRiskLevel = this.formGroup.value.delayChargeRiskLevel;
                    this.component.properties.manualTargetTime = this.formGroup.value.manualTargetTime;
                    this.loading = false;
                    this.refreshChart = true;
                    this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
                }).catch(reason => {
                    this.formGroup.controls['sellToGridLimitEnabled'].setValue(this.component.properties.sellToGridLimitEnabled);
                    this.formGroup.controls['maximumSellToGridPower'].setValue(this.component.properties.maximumSellToGridPower);
                    this.formGroup.controls['delayChargeRiskLevel'].setValue(this.component.properties.delayChargeRiskLevel);
                    this.formGroup.controls['manualTargetTime'].setValue(this.component.properties.manualTargetTime);
                    this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason, 'danger');
                    this.loading = false;
                    console.warn(reason);
                });
                this.formGroup.markAsPristine()
            } else {
                this.service.toast(this.translate.instant('General.insufficientRights'), 'danger');
            }
        }
    }

    getHoursAndMinutes(minutes: number): string {
        let hours = Math.floor(minutes / 60);
        let minutesLeft = minutes % 60;
        return `${hours > 0 ? hours + ' h' : ''} ${minutesLeft > 0 ? minutesLeft + ' min' : ''}`
    }
}

export enum DescriptionState {
    POSITIVE,
    NEGATIVE,
    NEUTRAL,
}
export type Description = {
    state: DescriptionState;
    text: string,
}


