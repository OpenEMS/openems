import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { isBefore } from 'date-fns';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils, Websocket } from 'src/app/shared/shared';
import { Role } from 'src/app/shared/type/role';

@Component({
    selector: 'storage-modal',
    templateUrl: './modal.component.html',
})
export class StorageModalComponent implements OnInit, OnDestroy {

    // TODO after refactoring of Model: subscribe to EssActivePowerL1/L2/L3 here instead of Flat Widget

    @Input() protected edge: Edge;
    @Input() protected config: EdgeConfig;
    @Input() protected essComponents: EdgeConfig.Component[] | null = null;
    @Input() protected chargerComponents: EdgeConfig.Component[];
    @Input() protected singleComponent: EdgeConfig.Component = null;

    // reference to the Utils method to access via html
    public isLastElement = Utils.isLastElement;

    public formGroup: FormGroup = new FormGroup({});
    protected isAtLeastInstaller: boolean;
    protected isTargetTimeInValid: Map<string, boolean> = new Map();
    protected controllerIsRequiredEdgeVersion: boolean = false;

    constructor(
        public service: Service,
        public translate: TranslateService,
        public modalCtrl: ModalController,
        public websocket: Websocket,
        public formBuilder: FormBuilder,
    ) { }

    ngOnInit() {

        // Future Work: Remove when all fems are at least at this version
        this.controllerIsRequiredEdgeVersion = this.edge.isVersionAtLeast('2023.2.5');

        this.isAtLeastInstaller = this.edge.roleIsAtLeast(Role.INSTALLER);
        let emergencyReserveCtrl = this.config.getComponentsByFactory('Controller.Ess.EmergencyCapacityReserve');
        let prepareBatteryExtensionCtrl = this.config.getComponentsByFactory("Controller.Ess.PrepareBatteryExtension");
        let components = [...prepareBatteryExtensionCtrl, ...emergencyReserveCtrl].filter(component => component.isEnabled).reduce((result, component) => {
            let essId = component.properties['ess.id'];
            if (result[essId] == null) {
                result[essId] = [];
            }
            result[essId].push(component);
            return result;
        }, {});

        let channelAddresses: ChannelAddress[] = [];
        for (let essId in prepareBatteryExtensionCtrl) {
            let controller = prepareBatteryExtensionCtrl[essId];
            channelAddresses.push(
                new ChannelAddress(controller.id, "_PropertyIsRunning"),
                new ChannelAddress(controller.id, "_PropertyTargetTime"),
                new ChannelAddress(controller.id, "_PropertyTargetTimeSpecified"),
                new ChannelAddress(controller.id, "_PropertyTargetSoc"),
                new ChannelAddress(controller.id, "_PropertyTargetTimeBuffer"),
                new ChannelAddress(controller.id, "ExpectedStartEpochSeconds"),
            );
        }
        this.edge.subscribeChannels(this.websocket, "storage", channelAddresses);

        this.edge.currentData
            .subscribe(currentData => {

                let controls: FormGroup = new FormGroup({});
                for (let essId of Object.keys(components)) {
                    let controllers = components[essId];

                    let controllerFrmGrp: FormGroup = new FormGroup({});
                    for (let controller of (controllers as EdgeConfig.Component[])) {

                        if (controller.factoryId == 'Controller.Ess.EmergencyCapacityReserve') {
                            let reserveSoc = currentData.channel[controller.id + "/_PropertyReserveSoc"] ?? 20 /* default Reserve-Soc */;
                            let isReserveSocEnabled = currentData.channel[controller.id + "/_PropertyIsReserveSocEnabled"] == 1;

                            controllerFrmGrp.addControl('emergencyReserveController',
                                this.formBuilder.group({
                                    controllerId: new FormControl(controller['id']),
                                    isReserveSocEnabled: new FormControl(isReserveSocEnabled),
                                    reserveSoc: new FormControl(reserveSoc),
                                }),
                            );
                        } else if (controller.factoryId == 'Controller.Ess.PrepareBatteryExtension') {

                            let isRunning = currentData.channel[controller.id + "/_PropertyIsRunning"] == 1;

                            // Because of ionic segment buttons only accepting a string value, i needed to convert it
                            let targetTimeSpecified = (currentData.channel[controller.id + "/_PropertyTargetTimeSpecified"] == 1).toString();;
                            let targetTime = currentData.channel[controller.id + "/_PropertyTargetTime"];
                            let targetSoc = currentData.channel[controller.id + "/_PropertyTargetSoc"];
                            let targetTimeBuffer = currentData.channel[controller.id + "/_PropertyTargetTimeBuffer"];
                            let epochSeconds = currentData.channel[controller.id + "/ExpectedStartEpochSeconds"];

                            let expectedStartOfPreparation = new Date(0);
                            expectedStartOfPreparation.setUTCSeconds(epochSeconds ?? 0);

                            // If targetTime not set, not equals 0 or targetTime is no valid time, 
                            // then set targetTime to null
                            if (!targetTime || targetTime == 0 || isNaN(Date.parse(targetTime))) {
                                targetTime = null;
                            }

                            // Channel "ExpectedStartEpochSeconds" is not set 
                            if ((epochSeconds == null
                                || epochSeconds == 0)) {
                                this.isTargetTimeInValid.set(essId, true);
                            } else if

                                // If expected expectedStartOfpreparation is after targetTime
                                //  Guarantee, that the TargetSoc should be reached after the preparation to reach that Soc started
                                (isBefore(new Date(targetTime), expectedStartOfPreparation)
                                || isBefore(new Date(targetTime), new Date())) {
                                this.isTargetTimeInValid.set(essId, true);
                            } else {
                                this.isTargetTimeInValid.set(essId, false);
                            }

                            controllerFrmGrp.addControl('prepareBatteryExtensionController',
                                this.formBuilder.group({
                                    controllerId: new FormControl(controller.id),
                                    isRunning: new FormControl(isRunning),
                                    targetTime: new FormControl(targetTime),
                                    targetTimeSpecified: new FormControl(targetTimeSpecified),
                                    targetSoc: new FormControl(targetSoc),
                                    targetTimeBuffer: new FormControl(targetTimeBuffer),
                                    expectedStartOfPreparation: new FormControl(expectedStartOfPreparation),
                                }),
                            );
                        }
                    }
                    controls.addControl(essId, controllerFrmGrp);
                }

                if (!this.formGroup.dirty) {
                    this.formGroup = controls;
                }
            });
    }

    applyChanges() {
        if (this.edge == null) {
            return;
        }

        let updateArray: Map<string, Array<Map<string, any>>> = new Map();
        for (let essId in this.formGroup.controls) {
            let essGroups = this.formGroup.controls[essId];

            let emergencyReserveController = (essGroups.get('emergencyReserveController') as FormGroup)?.controls ?? {};
            for (let essGroup of Object.keys(emergencyReserveController)) {
                if (emergencyReserveController[essGroup].dirty) {
                    if (updateArray.get(emergencyReserveController['controllerId'].value)) {
                        updateArray.get(emergencyReserveController['controllerId'].value).push(new Map().set(essGroup, emergencyReserveController[essGroup].value));
                    } else {
                        updateArray.set(emergencyReserveController['controllerId'].value, [new Map().set(essGroup, emergencyReserveController[essGroup].value)]);
                    }
                }

            }
            let prepareBatteryExtensionController = (essGroups.get('prepareBatteryExtensionController') as FormGroup)?.controls ?? {};
            for (let essGroup of Object.keys(prepareBatteryExtensionController)) {
                if (prepareBatteryExtensionController[essGroup].dirty) {

                    // For simplicity, split targetTimeSpecified in 2 for template formControlName
                    if (updateArray.get(prepareBatteryExtensionController['controllerId'].value)) {
                        updateArray.get(prepareBatteryExtensionController['controllerId'].value).push(new Map().set(essGroup, prepareBatteryExtensionController[essGroup].value));
                    } else {
                        updateArray.set(prepareBatteryExtensionController['controllerId'].value, [new Map().set(essGroup, prepareBatteryExtensionController[essGroup].value)]);
                    }
                }
            }
        }

        for (let controllerId of updateArray.keys()) {
            let controllers = updateArray.get(controllerId);
            let properties: { name: string, value: any }[] = [];
            controllers.forEach((element) => {
                let name = element.keys().next().value;
                let value = element.values().next().value;
                properties.push({
                    name: name,
                    value: value,
                });
            });

            this.edge.updateComponentConfig(this.websocket, controllerId, properties).then(() => {
                this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
                this.formGroup.markAsPristine();
            }).catch(reason => {
                this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason, 'danger');
            });
        }
    }

    ngOnDestroy() {
        this.edge.unsubscribeChannels(this.websocket, "storage");
    }
}
