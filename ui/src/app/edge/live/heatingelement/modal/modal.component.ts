import { Component, OnInit, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Router } from '@angular/router';
import { Websocket, Service, EdgeConfig, Edge, ChannelAddress } from 'src/app/shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { FormGroup, FormBuilder, FormControl } from '@angular/forms';
import { BehaviorSubject } from 'rxjs';

type Mode = 'MANUAL_ON' | 'MANUAL_OFF' | 'AUTOMATIC';

@Component({
    selector: HeatingElementModalComponent.SELECTOR,
    templateUrl: './modal.component.html',
})
export class HeatingElementModalComponent implements OnInit {


    @Input() public edge: Edge;
    @Input() public component: EdgeConfig.Component;
    @Input() public activePhases: BehaviorSubject<number>;

    private static readonly SELECTOR = "heatingelement-modal";

    public pickerOptions: any;
    public formGroup: FormGroup;
    public loading: boolean = false;

    constructor(
        protected service: Service,
        public websocket: Websocket,
        public router: Router,
        protected translate: TranslateService,
        public modalCtrl: ModalController,
        public formBuilder: FormBuilder
    ) { }

    ngOnInit() {
        this.edge.subscribeChannels(this.websocket, HeatingElementModalComponent.SELECTOR + this.component.id, [
            new ChannelAddress(this.component.id, 'Level1Energy'),
            new ChannelAddress(this.component.id, 'Level2Energy'),
            new ChannelAddress(this.component.id, 'Level3Energy'),
            new ChannelAddress(this.component.id, 'Level1Time'),
            new ChannelAddress(this.component.id, 'Level2Time'),
            new ChannelAddress(this.component.id, 'Level3Time'),
        ]);
        this.formGroup = this.formBuilder.group({
            minTime: new FormControl(this.component.properties.minTime),
            minkwh: new FormControl(this.component.properties.minkwh),
            endTime: new FormControl(this.component.properties.endTime),
            priority: new FormControl(this.component.properties.priority),
            heatingLevel: new FormControl(this.component.properties.level)
        })
    };

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

    updateMinKwh(event: CustomEvent) {
        this.formGroup.controls['minkwh'].setValue(event.detail.value);
        this.formGroup.controls['minkwh'].markAsDirty()
    }

    updatePriorityMode(event: CustomEvent) {
        this.formGroup.controls['priority'].setValue(event.detail.value);
        this.formGroup.controls['priority'].markAsDirty()
    }

    updateLevel(event: CustomEvent) {
        this.formGroup.controls['heatingLevel'].setValue(event.detail.value);
        this.formGroup.controls['heatingLevel'].markAsDirty()
    }

    applyChanges() {
        let updateComponentArray = [];
        Object.keys(this.formGroup.controls).forEach((element, index) => {
            if (this.formGroup.controls[element].dirty) {
                updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value })
            }
        });

        if (this.edge != null) {
            this.loading = true;
            this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {
                this.component.properties.minTime = this.formGroup.value.minTime;
                this.component.properties.minkwh = this.formGroup.value.minkwh;
                this.component.properties.endTime = this.formGroup.value.endTime;
                this.component.properties.priority = this.formGroup.value.priority;
                this.component.properties.heatingLevel = this.formGroup.value.heatingLevel;
                this.service.toast(this.translate.instant('General.ChangeAccepted'), 'success');
                this.loading = false;
            }).catch(reason => {
                this.formGroup.controls['minTime'].setValue(this.component.properties.minTime);
                this.formGroup.controls['minkwh'].setValue(this.component.properties.minkwh);
                this.formGroup.controls['endTime'].setValue(this.component.properties.endTime);
                this.formGroup.controls['priority'].setValue(this.component.properties.priority);
                this.formGroup.controls['heatingLevel'].setValue(this.component.properties.heatingLevel);
                this.service.toast(this.translate.instant('General.ChangeFailed') + '\n' + reason, 'danger');
                this.loading = false;
                console.warn(reason);
            });
            this.formGroup.markAsPristine()
        }
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, HeatingElementModalComponent.SELECTOR + this.component.id);
        }
    }
}