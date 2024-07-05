// @ts-strict-ignore
import { Component, Input, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { Edge, EdgeConfig, Service, Websocket } from '../../../../../../shared/shared';

@Component({
    selector: 'asymmetricpeakshaving-modal',
    templateUrl: './modal.component.html',
})
export class Controller_Asymmetric_PeakShavingModalComponent implements OnInit {

    @Input({ required: true }) protected component!: EdgeConfig.Component;
    @Input({ required: true }) protected edge!: Edge;
    @Input({ required: true }) protected mostStressedPhase!: Subject<{ name: 'L1' | 'L2' | 'L3' | '', value: number }>;

    public formGroup: FormGroup;
    public loading: boolean = false;

    constructor(
        public service: Service,
        public translate: TranslateService,
        public modalCtrl: ModalController,
        public formBuilder: FormBuilder,
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
        });
    }

    applyChanges() {
        if (this.edge != null) {
            if (this.edge.roleIsAtLeast('owner')) {
                if (this.formGroup.controls['peakShavingPower'].valid && this.formGroup.controls['rechargePower'].valid) {
                    if (this.formGroup.controls['peakShavingPower'].value >= this.formGroup.controls['rechargePower'].value) {
                        const updateComponentArray = [];
                        Object.keys(this.formGroup.controls).forEach((element, index) => {
                            if (this.formGroup.controls[element].dirty) {
                                updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value });
                            }
                        });
                        this.loading = true;
                        this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {
                            this.component.properties.peakShavingPower = this.formGroup.value.peakShavingPower;
                            this.component.properties.rechargePower = this.formGroup.value.rechargePower;
                            this.loading = false;
                            this.service.toast(this.translate.instant('GENERAL.CHANGE_ACCEPTED'), 'success');
                        }).catch(reason => {
                            this.formGroup.controls['peakShavingPower'].setValue(this.component.properties.peakShavingPower);
                            this.formGroup.controls['rechargePower'].setValue(this.component.properties.rechargePower);
                            this.loading = false;
                            this.service.toast(this.translate.instant('GENERAL.CHANGE_FAILED') + '\n' + reason.error.message, 'danger');
                            console.warn(reason);
                        });
                        this.formGroup.markAsPristine();
                    } else {
                        this.service.toast(this.translate.instant('EDGE.INDEX.WIDGETS.PEAKSHAVING.RELATION_ERROR'), 'danger');
                    }
                } else {
                    this.service.toast(this.translate.instant('GENERAL.INPUT_NOT_VALID'), 'danger');
                }
            } else {
                this.service.toast(this.translate.instant('GENERAL.INSUFFICIENT_RIGHTS'), 'danger');
            }
        }
    }
}
