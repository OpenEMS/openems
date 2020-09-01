import { BehaviorSubject } from 'rxjs';
import { Component, OnInit, Input } from '@angular/core';
import { FormGroup, FormBuilder, FormControl } from '@angular/forms';
import { ModalController } from '@ionic/angular';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Websocket, Service, EdgeConfig, Edge } from 'src/app/shared/shared';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

type Mode = 'MANUAL_ON' | 'MANUAL_OFF' | 'AUTOMATIC';

@Component({
    selector: HeatingElementModalComponent.SELECTOR,
    templateUrl: './modal.component.html',
})
export class HeatingElementModalComponent implements OnInit {


    @Input() public edge: Edge | null = null;
    @Input() public component: EdgeConfig.Component | null = null;
    @Input() public activePhases: BehaviorSubject<number> | null = null;

    private static readonly SELECTOR = "heatingelement-modal";

    public pickerOptions: any;
    public formGroup: FormGroup | null = null;
    public loading: boolean = false;

    constructor(
        protected service: Service,
        protected translate: TranslateService,
        public formBuilder: FormBuilder,
        public modalCtrl: ModalController,
        public router: Router,
        public websocket: Websocket,
    ) { }

    ngOnInit() {
        if (this.component != null) {
            this.formGroup = this.formBuilder.group({
                minTime: new FormControl(this.component.properties.minTime),
                minKwh: new FormControl(this.component.properties.minKwh),
                endTime: new FormControl(this.component.properties.endTime),
                workMode: new FormControl(this.component.properties.workMode),
                defaultLevel: new FormControl(this.component.properties.defaultLevel),
            })
        }
    };

    //allowMinimumHeating == workMode: none
    switchAllowMinimumHeating(event: CustomEvent) {
        if (event.detail.checked == true && this.formGroup != null) {
            this.formGroup.controls['workMode'].setValue('TIME');
            this.formGroup.controls['workMode'].markAsDirty()
        } else if (event.detail.checked == false && this.formGroup != null) {
            this.formGroup.controls['workMode'].setValue('NONE');
            this.formGroup.controls['workMode'].markAsDirty()
        }
    }

    updateMode(event: CustomEvent, currentController: EdgeConfig.Component) {
        let oldMode = currentController.properties.mode;
        let newMode: Mode;

        switch (event.detail.value as Mode) {
            case 'MANUAL_ON':
                newMode = 'MANUAL_ON';
                break;
            case 'MANUAL_OFF':
                newMode = 'MANUAL_OFF';
                break;
            case 'AUTOMATIC':
                newMode = 'AUTOMATIC';
                break;
        }

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

    updateEndTime(event: CustomEvent) {
        if (this.formGroup != null) {
            this.formGroup.controls['endTime'].setValue(event.detail.value);
            this.formGroup.controls['endTime'].markAsDirty()
        }
    }

    updateMinTime(event: CustomEvent) {
        if (this.formGroup != null) {
            this.formGroup.controls['minTime'].setValue(event.detail.value);
            this.formGroup.controls['minTime'].markAsDirty();
        }
    }

    updateDefaultLevel(event: CustomEvent) {
        if (this.formGroup != null) {
            this.formGroup.controls['defaultLevel'].setValue(event.detail.value);
            this.formGroup.controls['defaultLevel'].markAsDirty()
        }
    }

    applyChanges() {
        let updateComponentArray: DefaultTypes.UpdateComponentObject[] = [];

        if (this.formGroup != null) {
            Object.keys(this.formGroup.controls).forEach((element, index) => {
                if (this.formGroup != null && this.formGroup.controls[element].dirty) {
                    updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value })
                }
            });
        }

        if (this.edge && this.component && this.formGroup != null) {
            this.loading = true;
            this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {
                if (this.component && this.formGroup != null) {
                    this.component.properties.minTime = this.formGroup.value.minTime;
                    this.component.properties.minkwh = this.formGroup.value.minkwh;
                    this.component.properties.endTime = this.formGroup.value.endTime;
                    this.component.properties.workMode = this.formGroup.value.workMode;
                    this.component.properties.defaultLevel = this.formGroup.value.defaultLevel;
                }
                this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
                this.loading = false;
            }).catch(reason => {
                if (this.component && this.formGroup != null) {
                    this.formGroup.controls['minTime'].setValue(this.component.properties.minTime);
                    this.formGroup.controls['minKwh'].setValue(this.component.properties.minkwh);
                    this.formGroup.controls['endTime'].setValue(this.component.properties.endTime);
                    this.formGroup.controls['workMode'].setValue(this.component.properties.workMode);
                    this.formGroup.controls['defaultLevel'].setValue(this.component.properties.defaultLevel);
                    this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason, 'danger');
                }
                this.loading = false;
                console.warn(reason);
            });
            this.formGroup.markAsPristine()
        }
    }
}