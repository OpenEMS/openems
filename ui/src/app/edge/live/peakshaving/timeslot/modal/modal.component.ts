import { Component, Input } from '@angular/core';
import { FormGroup, FormBuilder, FormControl, Validators } from '@angular/forms';
import { ModalController } from '@ionic/angular';
import { Service, EdgeConfig, Edge, Websocket } from '../../../../../shared/shared';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: TimeslotPeakshavingModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class TimeslotPeakshavingModalComponent {

    @Input() component: EdgeConfig.Component | null = null;
    @Input() edge: Edge | null = null;

    private static readonly SELECTOR = "timeslotpeakshaving-modal";

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
                Validators.required
            ])),
            rechargePower: new FormControl(this.component.properties.rechargePower, Validators.compose([
                Validators.pattern('^(?:[1-9][0-9]*|0)$'),
                Validators.required
            ])),
            slowChargePower: new FormControl(this.component.properties.slowChargePower),
            slowChargeStartTime: new FormControl(this.component.properties.slowChargeStartTime),
            startDate: new FormControl(this.component.properties.startDate),
            startTime: new FormControl(this.component.properties.startTime),
            endDate: new FormControl(this.component.properties.endDate),
            endTime: new FormControl(this.component.properties.endTime),
            monday: new FormControl(this.component.properties.monday),
            tuesday: new FormControl(this.component.properties.tuesday),
            wednesday: new FormControl(this.component.properties.wednesday),
            thursday: new FormControl(this.component.properties.thursday),
            friday: new FormControl(this.component.properties.friday),
            saturday: new FormControl(this.component.properties.saturday),
            sunday: new FormControl(this.component.properties.sunday),
        })
        console.log("comp", this.component)
    }

    applyChanges() {
        if (this.edge.roleIsAtLeast('owner')) {
            let peakShavingPower = this.formGroup.controls['peakShavingPower'];
            let rechargePower = this.formGroup.controls['rechargePower'];
            if (peakShavingPower.valid && rechargePower.valid) {
                if (peakShavingPower.value >= rechargePower.value) {
                    let updateComponentArray = [];
                    Object.keys(this.formGroup.controls).forEach((element, index) => {
                        if (this.formGroup.controls[element].dirty) {
                            updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value })
                        }
                    })
                    console.log("updateCompArr", updateComponentArray)
                    // if (this.edge != null) {
                    //     this.loading = true;
                    //     this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {
                    //         this.component.properties.peakShavingPower = peakShavingPower.value;
                    //         this.component.properties.rechargePower = rechargePower.value;
                    //         this.loading = false;
                    //         this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
                    //     }).catch(reason => {
                    //         peakShavingPower.setValue(this.component.properties.peakShavingPower);
                    //         rechargePower.setValue(this.component.properties.rechargePower);
                    //         this.loading = false;
                    //         this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
                    //         console.warn(reason);
                    //     })
                    //     this.formGroup.markAsPristine()
                    // }
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