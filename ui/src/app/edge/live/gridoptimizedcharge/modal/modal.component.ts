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
    public riskDescription: Description | null;
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

            mode: new FormControl(this.component.properties.mode),
            sellToGridLimitEnabled: new FormControl(this.component.properties.sellToGridLimitEnabled),
            maximumSellToGridPower: new FormControl(this.component.properties.maximumSellToGridPower, Validators.compose([
                Validators.pattern('^(?:[1-9][0-9]*|0)$'),
                Validators.required
            ])),
            delayChargeRiskLevel: new FormControl(this.component.properties.delayChargeRiskLevel),
            manualTargetTime: new FormControl(this.component.properties.manualTargetTime),
        })
    };

    updateProperty(property: string, event: CustomEvent) {
        this.formGroup.controls[property].setValue(event.detail.value);
        this.formGroup.controls[property].markAsDirty()
    }

    updateToggleProperty(property: string, oldValue: boolean) {
        this.formGroup.controls[property].setValue(!oldValue);
        this.formGroup.controls[property].markAsDirty()
    }

    showPreview(value: string) {
        // Use images if there is a more intelligent risk algorithm 

        let risk: string;
        switch (value) {
            case 'LOW':
                risk = 'Low';
                break;
            case 'MEDIUM':
                risk = 'Medium';
                break;
            case 'HIGH':
                risk = 'High';
                break;
            default:
                return;
        }

        this.riskDescription = {
            functionDescription: this.translate.instant("Edge.Index.Widgets.GridOptimizedCharge.RiskDescription." + risk + ".functionDescription"),
            riskLevelDescription: [
                { state: DescriptionState.POSITIVE, text: this.translate.instant("Edge.Index.Widgets.GridOptimizedCharge.RiskDescription." + risk + ".storageDescription"), },
                { state: DescriptionState.NEGATIVE, text: this.translate.instant("Edge.Index.Widgets.GridOptimizedCharge.RiskDescription." + risk + ".pvCurtail"), },
            ]
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
                    this.component.properties.mode = this.formGroup.controls['mode'].value;
                    this.component.properties.sellToGridLimitEnabled = this.formGroup.controls['sellToGridLimitEnabled'].value;
                    this.component.properties.maximumSellToGridPower = this.formGroup.controls['maximumSellToGridPower'].value;
                    this.component.properties.delayChargeRiskLevel = this.formGroup.value.delayChargeRiskLevel;
                    this.component.properties.manualTargetTime = this.formGroup.value.manualTargetTime;
                    this.loading = false;
                    this.refreshChart = true;
                    this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
                }).catch(reason => {
                    this.formGroup.controls['mode'].setValue(this.component.properties.mode);
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
    functionDescription: string;
    riskLevelDescription: {
        state: DescriptionState;
        text: string,
    }[];
}


