import { Component, Input, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ModalController } from '@ionic/angular';
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

    private static readonly SELECTOR = "gridoptimizedcharge-modal";

    public formGroup: FormGroup;
    public loading: boolean = false;
    public pickerOptions: any;
    public channelCapacity: string = null

    constructor(
        public formBuilder: FormBuilder,
        public modalCtrl: ModalController,
        public service: Service,
        public translate: TranslateService,
        public websocket: Websocket,
    ) { }

    ngOnInit() {
        if (this.edge.roleIsAtLeast(Role.ADMIN) && 'ess.id' in this.component.properties) {
            let channelCapacity = new ChannelAddress(this.component.properties['ess.id'], "Capacity")
            this.channelCapacity = channelCapacity.toString();
            this.edge.subscribeChannels(this.websocket,
                GridOptimizedChargeModalComponent.SELECTOR + this.component.id,
                [channelCapacity]);
        }

        this.formGroup = this.formBuilder.group({
            sellToGridLimitEnabled: new FormControl(this.component.properties.sellToGridLimitEnabled),
            maximumSellToGridPower: new FormControl(this.component.properties.maximumSellToGridPower, Validators.compose([
                Validators.pattern('^(?:[1-9][0-9]*|0)$'),
                Validators.required
            ])),
            noOfBufferMinutes: new FormControl(this.component.properties.noOfBufferMinutes),
            manual_targetTime: new FormControl(this.component.properties['manual.targetTime']),
        })
    };

    updateMode(event: CustomEvent, currentController: EdgeConfig.Component) {
        let oldMode = currentController.properties.mode;
        let newMode: Mode = event.detail.value;

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
        console.log("old", oldValue)
        console.log("property", property)
        console.log("currValue", this.formGroup.controls[property])
        this.formGroup.controls[property].setValue(!oldValue);
        this.formGroup.controls[property].markAsDirty()
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

                    this.component.properties.sellToGridLimitEnabled = this.formGroup.value.sellToGridLimitEnabled;
                    this.component.properties.maximumSellToGridPower = this.formGroup.value.maximumSellToGridPower;
                    this.component.properties.noOfBufferMinutes = this.formGroup.value.noOfBufferMinutes;
                    this.component.properties['manual.targetTime'] = this.formGroup.value.manual_targetTime;
                    this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
                    this.loading = false;
                }).catch(reason => {
                    this.formGroup.controls['sellToGridLimitEnabled'].setValue(this.component.properties.sellToGridLimitEnabled);
                    this.formGroup.controls['maximumSellToGridPower'].setValue(this.component.properties.maximumSellToGridPower);
                    this.formGroup.controls['noOfBufferMinutes'].setValue(this.component.properties.noOfBufferMinutes);
                    this.formGroup.controls['manual_targetTime'].setValue(this.component.properties['manual.targetTime']);
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