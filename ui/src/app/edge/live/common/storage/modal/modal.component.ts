// @ts-strict-ignore
import { Component, Input, OnDestroy, OnInit } from "@angular/core";
import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { isBefore } from "date-fns";
import { ChannelAddress, Edge, EdgeConfig, Service, Utils, Websocket } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { DateTimeUtils } from "src/app/shared/utils/datetime/datetime-utils";
import { environment, Environment } from "src/environments";

@Component({
    selector: "storage-modal",
    templateUrl: "./MODAL.COMPONENT.HTML",
    standalone: false,
})
export class StorageModalComponent implements OnInit, OnDestroy {

    // TODO after refactoring of Model: subscribe to EssActivePowerL1/L2/L3 here instead of Flat Widget

    @Input({ required: true }) protected edge!: Edge;
    @Input() protected component: EDGE_CONFIG.COMPONENT | null = null;

    // reference to the Utils method to access via html
    public isLastElement = UTILS.IS_LAST_ELEMENT;

    public formGroup: FormGroup = new FormGroup({});
    protected isAtLeastInstaller: boolean;
    protected isTargetTimeInValid: Map<string, boolean> = new Map();
    protected controllerIsRequiredEdgeVersion: boolean = false;
    protected hasRequiredEdgeVersion: boolean = false;
    protected config: EdgeConfig;
    protected essComponents: EDGE_CONFIG.COMPONENT[] | null = null;
    protected chargerComponents!: EDGE_CONFIG.COMPONENT[];
    protected readonly environment: Environment = environment;

    constructor(
        public service: Service,
        public translate: TranslateService,
        public modalCtrl: ModalController,
        public websocket: Websocket,
        public formBuilder: FormBuilder,
    ) { }

    ngOnInit() {
        THIS.EDGE.GET_FIRST_VALID_CONFIG(THIS.WEBSOCKET).then(config => {
            THIS.CONFIG = config;
            THIS.ESS_COMPONENTS = THIS.CONFIG
                .getComponentsImplementingNature("IO.OPENEMS.EDGE.ESS.API.SYMMETRIC_ESS")
                .filter(component => COMPONENT.IS_ENABLED && !THIS.CONFIG
                    .getNatureIdsByFactoryId(COMPONENT.FACTORY_ID)
                    .includes("IO.OPENEMS.EDGE.ESS.API.META_ESS"));

            THIS.CHARGER_COMPONENTS = THIS.CONFIG
                .getComponentsImplementingNature("IO.OPENEMS.EDGE.ESS.DCCHARGER.API.ESS_DC_CHARGER")
                .filter(component => COMPONENT.IS_ENABLED);

            // Future Work: Remove when all ems are at least at this version
            THIS.CONTROLLER_IS_REQUIRED_EDGE_VERSION = THIS.EDGE.IS_VERSION_AT_LEAST("2023.2.5");

            THIS.IS_AT_LEAST_INSTALLER = THIS.EDGE.ROLE_IS_AT_LEAST(ROLE.INSTALLER);
            const emergencyReserveCtrl = THIS.CONFIG.GET_COMPONENTS_BY_FACTORY("CONTROLLER.ESS.EMERGENCY_CAPACITY_RESERVE");
            const prepareBatteryExtensionCtrl = THIS.CONFIG.GET_COMPONENTS_BY_FACTORY("CONTROLLER.ESS.PREPARE_BATTERY_EXTENSION");
            THIS.HAS_REQUIRED_EDGE_VERSION = THIS.EDGE.IS_VERSION_AT_LEAST("2024.12.3");
            const components = [...prepareBatteryExtensionCtrl, ...emergencyReserveCtrl].filter(component => COMPONENT.IS_ENABLED).reduce((result, component) => {
                const essId = COMPONENT.PROPERTIES["ESS.ID"];
                if (result[essId] == null) {
                    result[essId] = [];
                }
                result[essId].push(component);
                return result;
            }, {});

            const channelAddresses: ChannelAddress[] = [];
            CHANNEL_ADDRESSES.PUSH(...THIS.CHARGER_COMPONENTS.MAP(comp => new ChannelAddress(COMP.ID, "ActualPower")));

            if (THIS.HAS_REQUIRED_EDGE_VERSION) {
                CHANNEL_ADDRESSES.PUSH(new ChannelAddress("_meta", "IsEssChargeFromGridAllowed"));
            }
            for (const essId in prepareBatteryExtensionCtrl) {
                const controller = prepareBatteryExtensionCtrl[essId];
                CHANNEL_ADDRESSES.PUSH(
                    new ChannelAddress(CONTROLLER.ID, "_PropertyIsRunning"),
                    new ChannelAddress(CONTROLLER.ID, "_PropertyTargetTime"),
                    new ChannelAddress(CONTROLLER.ID, "_PropertyTargetTimeSpecified"),
                    new ChannelAddress(CONTROLLER.ID, "_PropertyTargetSoc"),
                    new ChannelAddress(CONTROLLER.ID, "_PropertyTargetTimeBuffer"),
                    new ChannelAddress(CONTROLLER.ID, "ExpectedStartEpochSeconds"),
                );
            }
            THIS.EDGE.SUBSCRIBE_CHANNELS(THIS.WEBSOCKET, "storage", channelAddresses);

            THIS.EDGE.CURRENT_DATA
                .subscribe(currentData => {

                    const controls: FormGroup = new FormGroup({});
                    if (THIS.HAS_REQUIRED_EDGE_VERSION) {
                        CONTROLS.ADD_CONTROL("_meta", THIS.FORM_BUILDER.GROUP({
                            isEssChargeFromGridAllowed: new FormControl(CURRENT_DATA.CHANNEL["_meta/IsEssChargeFromGridAllowed"]),
                        }));
                    }
                    for (const essId of OBJECT.KEYS(components)) {
                        const controllers = components[essId];

                        const controllerFrmGrp: FormGroup = new FormGroup({});
                        for (const controller of (controllers as EDGE_CONFIG.COMPONENT[])) {

                            if (CONTROLLER.FACTORY_ID == "CONTROLLER.ESS.EMERGENCY_CAPACITY_RESERVE") {
                                const reserveSoc = CURRENT_DATA.CHANNEL[CONTROLLER.ID + "/_PropertyReserveSoc"] ?? 20 /* default Reserve-Soc */;
                                const isReserveSocEnabled = CURRENT_DATA.CHANNEL[CONTROLLER.ID + "/_PropertyIsReserveSocEnabled"] == 1;

                                CONTROLLER_FRM_GRP.ADD_CONTROL("emergencyReserveController",
                                    THIS.FORM_BUILDER.GROUP({
                                        controllerId: new FormControl(controller["id"]),
                                        isReserveSocEnabled: new FormControl(isReserveSocEnabled),
                                        reserveSoc: new FormControl(reserveSoc),
                                    }),
                                );
                            } else if (CONTROLLER.FACTORY_ID == "CONTROLLER.ESS.PREPARE_BATTERY_EXTENSION") {

                                const isRunning = CURRENT_DATA.CHANNEL[CONTROLLER.ID + "/_PropertyIsRunning"] == 1;

                                // Because of ionic segment buttons only accepting a string value, i needed to convert it
                                const targetTimeSpecified = (CURRENT_DATA.CHANNEL[CONTROLLER.ID + "/_PropertyTargetTimeSpecified"] == 1).toString();
                                let targetTime = CURRENT_DATA.CHANNEL[CONTROLLER.ID + "/_PropertyTargetTime"];
                                const targetSoc = CURRENT_DATA.CHANNEL[CONTROLLER.ID + "/_PropertyTargetSoc"];
                                const targetTimeBuffer = CURRENT_DATA.CHANNEL[CONTROLLER.ID + "/_PropertyTargetTimeBuffer"];
                                const epochSeconds = CURRENT_DATA.CHANNEL[CONTROLLER.ID + "/ExpectedStartEpochSeconds"];

                                const expectedStartOfPreparation = new Date(0);
                                EXPECTED_START_OF_PREPARATION.SET_UTCSECONDS(epochSeconds ?? 0);

                                // If targetTime not set, not equals 0 or targetTime is no valid time,
                                // then set targetTime to null
                                if (!targetTime || targetTime == 0 || isNaN(DATE.PARSE(targetTime))) {
                                    targetTime = null;
                                }

                                // Channel "ExpectedStartEpochSeconds" is not set
                                if ((epochSeconds == null
                                    || epochSeconds == 0)) {
                                    THIS.IS_TARGET_TIME_IN_VALID.SET(essId, true);
                                } else if

                                    // If expected expectedStartOfpreparation is after targetTime
                                    //  Guarantee, that the TargetSoc should be reached after the preparation to reach that Soc started
                                    (isBefore(new Date(targetTime), expectedStartOfPreparation)
                                    || isBefore(new Date(targetTime), new Date())) {
                                    THIS.IS_TARGET_TIME_IN_VALID.SET(essId, true);
                                } else {
                                    THIS.IS_TARGET_TIME_IN_VALID.SET(essId, false);
                                }

                                CONTROLLER_FRM_GRP.ADD_CONTROL("prepareBatteryExtensionController",
                                    THIS.FORM_BUILDER.GROUP({
                                        controllerId: new FormControl(CONTROLLER.ID),
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
                        CONTROLS.ADD_CONTROL(essId, controllerFrmGrp);
                    }

                    if (!THIS.FORM_GROUP.DIRTY) {
                        THIS.FORM_GROUP = controls;
                    }
                });
        },
        );

    }

    async applyChanges() {
        if (THIS.EDGE == null) {
            return;
        }
        const updateArray: Map<string, Array<Map<string, any>>> = new Map();
        if (THIS.HAS_REQUIRED_EDGE_VERSION) {
            const metaFormGroup = (THIS.FORM_GROUP.GET("_meta") as FormGroup)?.controls ?? [];
            for (const prop of OBJECT.KEYS(metaFormGroup)) {
                if (metaFormGroup[prop].dirty) {
                    if (UPDATE_ARRAY.GET("_meta")) {
                        UPDATE_ARRAY.GET("_meta").push(new Map().set(prop, metaFormGroup[prop].value));
                    } else {
                        UPDATE_ARRAY.SET("_meta", [new Map().set(prop, metaFormGroup[prop].value)]);
                    }
                }
            }
        }

        for (const essId in THIS.FORM_GROUP.CONTROLS) {
            const essGroups = THIS.FORM_GROUP.CONTROLS[essId];

            const emergencyReserveController = (ESS_GROUPS.GET("emergencyReserveController") as FormGroup)?.controls ?? {};
            for (const essGroup of OBJECT.KEYS(emergencyReserveController)) {
                if (emergencyReserveController[essGroup].dirty) {
                    if (UPDATE_ARRAY.GET(emergencyReserveController["controllerId"].value)) {
                        UPDATE_ARRAY.GET(emergencyReserveController["controllerId"].value).push(new Map().set(essGroup, emergencyReserveController[essGroup].value));
                    } else {
                        UPDATE_ARRAY.SET(emergencyReserveController["controllerId"].value, [new Map().set(essGroup, emergencyReserveController[essGroup].value)]);
                    }
                }

            }
            const prepareBatteryExtensionController = (ESS_GROUPS.GET("prepareBatteryExtensionController") as FormGroup)?.controls ?? {};
            for (const essGroup of OBJECT.KEYS(prepareBatteryExtensionController)) {
                if (prepareBatteryExtensionController[essGroup].dirty) {

                    // For simplicity, split targetTimeSpecified in 2 for template formControlName
                    if (UPDATE_ARRAY.GET(prepareBatteryExtensionController["controllerId"].value)) {
                        UPDATE_ARRAY.GET(prepareBatteryExtensionController["controllerId"].value).push(new Map().set(essGroup, prepareBatteryExtensionController[essGroup].value));
                    } else {
                        UPDATE_ARRAY.SET(prepareBatteryExtensionController["controllerId"].value, [new Map().set(essGroup, prepareBatteryExtensionController[essGroup].value)]);
                    }
                }
            }
        }

        for (const controllerId of UPDATE_ARRAY.KEYS()) {
            const controllers = UPDATE_ARRAY.GET(controllerId);
            const properties: { name: string, value: any }[] = [];
            CONTROLLERS.FOR_EACH((element) => {
                const name = ELEMENT.KEYS().next().value;
                const rawValue = ELEMENT.VALUES().next().value;
                let value = rawValue;

                // Needs to be done to get Datetime string in this format: YYYY-MM-DDTHH:mm:ssTZD
                if (name === "targetTime") {
                    value = DATE_TIME_UTILS.FORMAT_TO_ISOZONED_DATE_TIME(rawValue);
                }

                PROPERTIES.PUSH({
                    name: name,
                    value: value,
                });
            });

            try {
                // todo: updateAppConfig for once fixed
                await THIS.EDGE.UPDATE_COMPONENT_CONFIG(THIS.WEBSOCKET, controllerId, properties);
                THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
                THIS.FORM_GROUP.MARK_AS_PRISTINE();

            } catch (reason) {
                THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED") + "\n" + reason, "danger");
            }

        }
    }

    ngOnDestroy() {
        THIS.EDGE.UNSUBSCRIBE_CHANNELS(THIS.WEBSOCKET, "storage");
    }

}
