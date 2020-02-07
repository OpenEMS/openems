import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service, EdgeConfig, Edge, Websocket } from '../../../../../shared/shared';
import { FormGroup, FormBuilder, FormControl, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: SymmetricPeakshavingModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class SymmetricPeakshavingModalComponent {

    @Input() component: EdgeConfig.Component;
    @Input() edge: Edge;

    private static readonly SELECTOR = "symmetricpeakshaving-modal";

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
                Validators.min(1),
                Validators.pattern('^(?:[1-9][0-9]*|0)$'),
                Validators.required
            ])),
            rechargePower: new FormControl(this.component.properties.rechargePower, Validators.compose([
                Validators.min(1),
                Validators.pattern('^(?:[1-9][0-9]*|0)$'),
                Validators.required
            ]))
        })
    }

    showApplyChanges(): boolean {
        if (this.formGroup.dirty) {
            return true;
        } else {
            return false;
        }
    }

    applyChanges() {
        if (this.formGroup.controls['peakShavingPower'].valid && this.formGroup.controls['rechargePower'].valid) {
            let updateComponentArray = [];
            Object.keys(this.formGroup.controls).forEach((element, index) => {
                if (this.formGroup.controls[element].dirty) {
                    updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value })
                }
            })
            if (this.edge != null) {
                this.loading = true;
                this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {
                    this.component.properties.peakShavingPower = this.formGroup.value.peakShavingPower;
                    this.component.properties.rechargePower = this.formGroup.value.rechargePower;
                    this.loading = false;
                    this.service.toast(this.translate.instant('General.ChangeAccepted'), 'success');
                }).catch(reason => {
                    this.formGroup.controls['peakShavingPower'].setValue(this.component.properties.peakShavingPower);
                    this.formGroup.controls['rechargePower'].setValue(this.component.properties.rechargePower);
                    this.loading = false;
                    this.service.toast(this.translate.instant('General.ChangeFailed') + '\n' + reason, 'danger');
                    console.warn(reason);
                })
                this.formGroup.markAsPristine()
            }
        } else {
            this.service.toast('Eingabe ungültig', 'danger');
        }
    }
}