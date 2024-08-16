// @ts-strict-ignore
import { Component, Input, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { Edge, EdgeConfig, Service, Websocket } from '../../../../../../shared/shared';

@Component({
    selector: 'timeslotpeakshaving-modal',
    templateUrl: './modal.component.html',
})
export class Controller_Symmetric_TimeSlot_PeakShavingModalComponent implements OnInit {

    private static readonly SELECTOR = "timeslotpeakshaving-modal";

    @Input() protected component: EdgeConfig.Component | null = null;
    @Input() protected edge: Edge | null = null;

    public formGroup: FormGroup;
    public loading: boolean = false;

    constructor(
        public formBuilder: FormBuilder,
        public modalCtrl: ModalController,
        public service: Service,
        public translate: TranslateService,
        public websocket: Websocket,
    ) { }

    ngOnInit() {
        this.formGroup = this.formBuilder.group({
            peakShavingPower: new FormControl(this.component.properties.peakShavingPower, Validators.compose([
                Validators.pattern('^(?:[1-9][0-9]*|0)$'),
                Validators.required,
            ])),
            rechargePower: new FormControl(this.component.properties.rechargePower, Validators.compose([
                Validators.pattern('^(?:[1-9][0-9]*|0)$'),
                Validators.required,
            ])),
            slowChargePower: new FormControl((this.component.properties.slowChargePower) * -1),
            slowChargeStartTime: new FormControl(this.component.properties.slowChargeStartTime, Validators.compose([
                Validators.pattern('^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$'),
                Validators.required,
            ])),
            startDate: new FormControl(this.component.properties.startDate, Validators.compose([
                Validators.pattern('^(0[1-9]|[12][0-9]|3[01])[.](0[1-9]|1[012])[.](19|20)[0-9]{2}$'),
                Validators.required,
            ])),
            startTime: new FormControl(this.component.properties.startTime, Validators.compose([
                Validators.pattern('^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$'),
                Validators.required,
            ])),
            endDate: new FormControl(this.component.properties.endDate),
            endTime: new FormControl(this.component.properties.endTime, Validators.compose([
                Validators.pattern('^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$'),
                Validators.required,
            ])),
            monday: new FormControl(this.component.properties.monday),
            tuesday: new FormControl(this.component.properties.tuesday),
            wednesday: new FormControl(this.component.properties.wednesday),
            thursday: new FormControl(this.component.properties.thursday),
            friday: new FormControl(this.component.properties.friday),
            saturday: new FormControl(this.component.properties.saturday),
            sunday: new FormControl(this.component.properties.sunday),
        });
    }

    applyChanges() {
        if (this.edge != null) {
            if (this.edge.roleIsAtLeast('owner')) {
                const peakShavingPower = this.formGroup.controls['peakShavingPower'];
                const rechargePower = this.formGroup.controls['rechargePower'];
                if (peakShavingPower.valid && rechargePower.valid) {
                    if (peakShavingPower.value >= rechargePower.value) {
                        const updateComponentArray = [];
                        Object.keys(this.formGroup.controls).forEach((element, index) => {
                            if (this.formGroup.controls[element].dirty) {
                                if (Object.keys(this.formGroup.controls)[index] == 'slowChargePower') {
                                    updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: (this.formGroup.controls[element].value) * -1 });
                                } else {
                                    updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value });
                                }
                            }
                        });
                        this.loading = true;
                        this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {
                            this.component.properties.peakShavingPower = peakShavingPower.value;
                            this.component.properties.rechargePower = rechargePower.value;
                            this.loading = false;
                            this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
                        }).catch(reason => {
                            peakShavingPower.setValue(this.component.properties.peakShavingPower);
                            rechargePower.setValue(this.component.properties.rechargePower);
                            this.loading = false;
                            this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
                            console.warn(reason);
                        });
                        this.formGroup.markAsPristine();
                    } else {
                        this.service.toast(this.translate.instant('Edge.Index.Widgets.Peakshaving.relationError'), 'danger');
                    }
                } else {
                    this.service.toast(this.translate.instant('General.inputNotValid'), 'danger');
                }
            } else {
                this.service.toast(this.translate.instant('General.insufficientRights'), 'danger');
            }
        }
    }
}
