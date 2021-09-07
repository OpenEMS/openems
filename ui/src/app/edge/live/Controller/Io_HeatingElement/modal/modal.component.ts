import { BehaviorSubject } from 'rxjs';
import { Component, OnInit, Input } from '@angular/core';
import { FormGroup, FormBuilder, FormControl } from '@angular/forms';
import { ModalController } from '@ionic/angular';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Websocket, Service, EdgeConfig, Edge } from '../../../../../shared/shared';

type Mode = 'MANUAL_ON' | 'MANUAL_OFF' | 'AUTOMATIC';

@Component({
    selector: Controller_Io_HeatingElementModalComponent.SELECTOR,
    templateUrl: './modal.component.html',
})
export class Controller_Io_HeatingElementModalComponent implements OnInit {


    @Input() public edge: Edge;
    @Input() public component: EdgeConfig.Component;
    @Input() public activePhases: BehaviorSubject<number>;

    private static readonly SELECTOR = "heatingelement-modal";

    public pickerOptions: any;
    public formGroup: FormGroup;
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
        this.formGroup = this.formBuilder.group({
            minTime: new FormControl(this.component.properties.minTime),
            minKwh: new FormControl(this.component.properties.minKwh),
            endTime: new FormControl(this.component.properties.endTime),
            workMode: new FormControl(this.component.properties.workMode),
            defaultLevel: new FormControl(this.component.properties.defaultLevel),
        })
    };

    //allowMinimumHeating == workMode: none
    switchAllowMinimumHeating(event: CustomEvent) {
        if (event.detail.checked == true) {
            this.formGroup.controls['workMode'].setValue('TIME');
            this.formGroup.controls['workMode'].markAsDirty()
        } else if (event.detail.checked == false) {
            this.formGroup.controls['workMode'].setValue('NONE');
            this.formGroup.controls['workMode'].markAsDirty()
        }
    }

    updateMode(event: CustomEvent, currentController: EdgeConfig.Component) {
        let oldMode = currentController.properties.mode;
        let newMode: Mode;

        switch (event.detail.value) {
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
        this.formGroup.controls['endTime'].setValue(event.detail.value);
        this.formGroup.controls['endTime'].markAsDirty()
    }

    updateMinTime(event: CustomEvent) {
        this.formGroup.controls['minTime'].setValue(event.detail.value);
        this.formGroup.controls['minTime'].markAsDirty();
    }

    updateDefaultLevel(event: CustomEvent) {
        this.formGroup.controls['defaultLevel'].setValue(event.detail.value);
        this.formGroup.controls['defaultLevel'].markAsDirty()
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
                    this.component.properties.minTime = this.formGroup.value.minTime;
                    this.component.properties.minkwh = this.formGroup.value.minkwh;
                    this.component.properties.endTime = this.formGroup.value.endTime;
                    this.component.properties.workMode = this.formGroup.value.workMode;
                    this.component.properties.defaultLevel = this.formGroup.value.defaultLevel;
                    this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
                    this.loading = false;
                }).catch(reason => {
                    this.formGroup.controls['minTime'].setValue(this.component.properties.minTime);
                    this.formGroup.controls['minKwh'].setValue(this.component.properties.minkwh);
                    this.formGroup.controls['endTime'].setValue(this.component.properties.endTime);
                    this.formGroup.controls['workMode'].setValue(this.component.properties.workMode);
                    this.formGroup.controls['defaultLevel'].setValue(this.component.properties.defaultLevel);
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
}