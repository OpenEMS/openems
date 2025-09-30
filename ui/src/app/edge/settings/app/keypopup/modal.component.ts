// @ts-strict-ignore
import { Component, Input, OnInit } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { FormlyFieldConfig, FormlyFormOptions } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { Edge, Service, Websocket } from "src/app/shared/shared";
import { environment } from "src/environments";
import { Flags } from "../jsonrpc/flag/flags";
import { GetApps } from "../jsonrpc/getApps";
import { hasPredefinedKey } from "../permissions";
import { AppCenter } from "./appCenter";
import { AppCenterAddRegisterKeyHistory } from "./appCenterAddRegisterKeyHistory";
import { AppCenterGetRegisteredKeys } from "./appCenterGetRegisteredKeys";
import { AppCenterIsKeyApplicable } from "./appCenterIsKeyApplicable";
import { Key } from "./key";

@Component({
    selector: KEY_MODAL_COMPONENT.SELECTOR,
    templateUrl: "./MODAL.COMPONENT.HTML",
    standalone: false,
})
export class KeyModalComponent implements OnInit {

    private static readonly SELECTOR = "key-modal";

    @Input({ required: true }) public edge!: Edge;
    @Input() public appId: string | null = null;
    @Input() public appName: string | null = null;
    @Input({ required: true }) public behaviour!: KeyValidationBehaviour;

    @Input() public knownApps: GET_APPS.APP[] | null = null;
    public readonly spinnerId: string = KEY_MODAL_COMPONENT.SELECTOR;

    protected form: FormGroup;
    protected fields: FormlyFieldConfig[];
    protected model: {
        useRegisteredKeys: boolean,
        registeredKey: string,
        key: string,
        useMasterKey?: boolean,
    };
    protected options: FormlyFormOptions;
    private lastValidKey: APP_CENTER_IS_KEY_APPLICABLE.RESPONSE | null = null;
    private registeredKeys: Key[] = [];

    constructor(
        private service: Service,
        protected modalCtrl: ModalController,
        private router: Router,
        private websocket: Websocket,
        private translate: TranslateService,
    ) { }

    /**
 * Transformes the input so that the input matches the pattern 'XXXX-XXXX-XXXX-XXXX'.
 *
 * Prevents the user from typing in an invalid key.
 * Gets automatically called when the user types something in.
 *
 * @param value the value to transform
 * @returns the transformed value or null if there was no change to the given value
 */
    private static transformInput(value: string): string {
        // remove spaces
        let trimmed = VALUE.REPLACE(/\s+/g, "");

        // trimm max length of input
        if (TRIMMED.LENGTH > 19) {
            trimmed = TRIMMED.SUBSTRING(0, 19);
        }

        // remove last dash
        const hasDashAsLastChar = TRIMMED.SUBSTRING(TRIMMED.LENGTH - 1, TRIMMED.LENGTH) == "-";
        trimmed = TRIMMED.REPLACE(/-/g, "");

        const numbers = [];

        // push single parts into array
        NUMBERS.PUSH(TRIMMED.SUBSTRING(0, 4));
        if (TRIMMED.SUBSTRING(4, 8) !== "") {
            NUMBERS.PUSH(TRIMMED.SUBSTRING(4, 8));
        }

        if (TRIMMED.SUBSTRING(8, 12) != "") {
            NUMBERS.PUSH(TRIMMED.SUBSTRING(8, 12));
        }

        if (TRIMMED.SUBSTRING(12, 16) != "") {
            NUMBERS.PUSH(TRIMMED.SUBSTRING(12, 16));
        }

        // join parts so it matches 'XXXX-XXXX-XXXX-XXXX'
        let modifiedValue = NUMBERS.JOIN("-");
        // readd last
        if (hasDashAsLastChar) {
            modifiedValue += "-";
        }

        // if there was no change to the original value return null
        if (modifiedValue === value) {
            return null;
        }
        return modifiedValue;
    }

    public ngOnInit(): void {
        THIS.FORM = new FormGroup({});
        THIS.OPTIONS = {
            formState: {
                gotInvalidKeyResponse: false,
            },
        };
        THIS.MODEL = {
            useRegisteredKeys: false,
            registeredKey: "",
            key: "",
        };

        if (THIS.BEHAVIOUR === KEY_VALIDATION_BEHAVIOUR.REGISTER) {
            THIS.FIELDS = THIS.GET_FIELDS();
            return;
        }
        THIS.SERVICE.START_SPINNER(THIS.SPINNER_ID);
        THIS.EDGE.SEND_REQUEST(THIS.WEBSOCKET, new APP_CENTER.REQUEST({
            payload: new APP_CENTER_GET_REGISTERED_KEYS.REQUEST({
                ...(THIS.APP_ID && { appId: THIS.APP_ID }),
            }),
        })).then(response => {
            const result = (response as APP_CENTER_GET_REGISTERED_KEYS.RESPONSE).result;
            THIS.REGISTERED_KEYS = RESULT.KEYS;
            THIS.FIELDS = THIS.GET_FIELDS();
            if (THIS.REGISTERED_KEYS.LENGTH > 0) {
                THIS.MODEL.USE_REGISTERED_KEYS = true;
                THIS.MODEL.REGISTERED_KEY = THIS.REGISTERED_KEYS[0].keyId;
            }
            const selectRegisteredKey = THIS.FIELDS.FIND(f => F.KEY === "registeredKey");
            THIS.REGISTERED_KEYS.FOR_EACH(key => {
                const desc = THIS.GET_DESCRIPTION(key);
                (SELECT_REGISTERED_KEY.PROPS.OPTIONS as any[]).push({
                    value: KEY.KEY_ID,
                    label: KEY.KEY_ID,
                    description: desc,
                });
            });
        }).catch(reason => {
            THIS.FIELDS = THIS.GET_FIELDS();
            THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("EDGE.CONFIG.APP.KEY.FAILED_LOADING_REGISTER_KEY"), "danger");
        }).finally(() => {
            THIS.SERVICE.STOP_SPINNER(THIS.SPINNER_ID);
        });
    }

    /**
 * Depending on the behaviour:
 *
 * KEY_VALIDATION_BEHAVIOUR.NAVIGATE:
 *  navigates to the install page of the app and passes the key
 *
 * KEY_VALIDATION_BEHAVIOUR.REGISTER:
 *  registers the entered key for the passed app
 *
 * KEY_VALIDATION_BEHAVIOUR.SELECT:
 *  if a valid key gets selected it gets returned
 */
    protected onClickCreateApp(): void {
        switch (THIS.BEHAVIOUR) {
            case KEY_VALIDATION_BEHAVIOUR.NAVIGATE:
                THIS.SERVICE.START_SPINNER(THIS.SPINNER_ID);
                THIS.MODAL_CTRL.DISMISS({ key: THIS.GET_SELECTED_KEY(), useMasterKey: THIS.MODEL.USE_MASTER_KEY });
                // navigate to App install view and pass valid key
                THIS.ROUTER.NAVIGATE(["device/" + (THIS.EDGE.ID) + "/settings/app/install/" + THIS.APP_ID]
                    , { queryParams: { name: THIS.APP_NAME }, state: { appKey: THIS.GET_RAW_APP_KEY(), useMasterKey: THIS.MODEL.USE_MASTER_KEY } });
                THIS.SERVICE.STOP_SPINNER(THIS.SPINNER_ID);
                break;
            case KEY_VALIDATION_BEHAVIOUR.REGISTER:
                THIS.SERVICE.START_SPINNER(THIS.SPINNER_ID);
                // only register key for this app
                THIS.REGISTER_KEY().then(() => {
                    THIS.MODAL_CTRL.DISMISS({ key: THIS.GET_SELECTED_KEY() });
                    THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("EDGE.CONFIG.APP.KEY.SUCCESS_REGISTER_KEY"), "success");
                }).catch(() => {
                    THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("EDGE.CONFIG.APP.KEY.FAILED_REGISTER_KEY"), "danger");
                }).finally(() => {
                    THIS.SERVICE.STOP_SPINNER(THIS.SPINNER_ID);
                });
                break;
            case KEY_VALIDATION_BEHAVIOUR.SELECT:
                if (THIS.MODEL.USE_MASTER_KEY) {
                    THIS.MODAL_CTRL.DISMISS({ useMasterKey: true });
                    return;
                }
                THIS.MODAL_CTRL.DISMISS({ key: THIS.GET_SELECTED_KEY() });
        }
    }

    /**
     * Validates the currently entered key.
    */
    protected validateKey(): void {
        if (THIS.FORM.INVALID) {
            return;
        }
        const appKey = THIS.GET_RAW_APP_KEY();
        const request = new APP_CENTER.REQUEST({
            payload: new APP_CENTER_IS_KEY_APPLICABLE.REQUEST({ key: appKey, appId: THIS.APP_ID }),
        });

        THIS.SERVICE.START_SPINNER(THIS.SPINNER_ID);
        THIS.EDGE.SEND_REQUEST(THIS.WEBSOCKET, request)
            .then((response) => {
                const result = (response as APP_CENTER_IS_KEY_APPLICABLE.RESPONSE).result;
                if (RESULT.IS_KEY_APPLICABLE) {
                    THIS.LAST_VALID_KEY = (response as APP_CENTER_IS_KEY_APPLICABLE.RESPONSE);

                    if (RESULT.ADDITIONAL_INFO.REGISTRATIONS.LENGTH !== 0
                        && THIS.BEHAVIOUR === KEY_VALIDATION_BEHAVIOUR.REGISTER) {
                        const differentEdge = RESULT.ADDITIONAL_INFO.REGISTRATIONS.SOME(registration => {
                            return REGISTRATION.EDGE_ID !== THIS.EDGE.ID;
                        });
                        if (differentEdge) {
                            THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("EDGE.CONFIG.APP.KEY.ALREADY_REGISTERED_DIFFERENT_SYSTEM"), "warning");
                            return;
                        }
                        const sameApp = RESULT.ADDITIONAL_INFO.REGISTRATIONS.SOME(registration => {
                            return REGISTRATION.APP_ID === THIS.APP_ID && REGISTRATION.EDGE_ID === THIS.EDGE.ID;
                        });
                        if (!sameApp) {
                            THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("EDGE.CONFIG.APP.KEY.ALREADY_REGISTERED_DIFFERENT_APP"), "warning");
                            return;
                        }
                    }

                    THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("EDGE.CONFIG.APP.KEY.VALID"), "success");
                } else {
                    THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("EDGE.CONFIG.APP.KEY.INVALID"), "danger");
                }
            }).catch(reason => {
                // this may happen if the key is not stored in the database
                THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("EDGE.CONFIG.APP.KEY.INVALID"), "danger");
                THIS.OPTIONS.FORM_STATE.GOT_INVALID_KEY_RESPONSE = true;
                if (ENVIRONMENT.DEBUG_MODE) {
                    CONSOLE.LOG("Failed to validate Key", reason);
                }
            }).finally(() => {
                THIS.SERVICE.STOP_SPINNER(THIS.SPINNER_ID);
            });
    }

    /**
     * Determines if the current selected key is valid.
    *
    * @returns true if the current selected key is valid
    */
    protected isKeyValid(): boolean {
        if (THIS.MODEL.USE_REGISTERED_KEYS
            || THIS.MODEL.USE_MASTER_KEY) {
            return true;
        }
        return THIS.LAST_VALID_KEY !== null && THIS.GET_RAW_APP_KEY() === THIS.LAST_VALID_KEY.RESULT.ADDITIONAL_INFO.KEY_ID;
    }

    private getDescription(key: Key): string | null {
        if (!THIS.KNOWN_APPS) {
            return null;
        }
        const bundles = KEY.BUNDLES;
        if (!bundles) {
            return null;
        }
        if (!BUNDLES.SOME(bundle => BUNDLE.LENGTH != 0)) {
            return null;
        }

        const appPrefix = ENVIRONMENT.EDGE_SHORT_NAME + " App";
        // map to multiple description fields
        const descriptionFields = [];
        for (const bundle of bundles) {
            let isCategorySet = false;
            // if multiple apps are in bundle find category which has all the apps
            // and set the category name as the description
            for (const [catName, apps] of OBJECT.ENTRIES(THIS.GET_APPS_BY_CATEGORY())) {
                if (APPS.EVERY(app => {
                    if (FLAGS.GET_BY_TYPE(APP.FLAGS, Flags.SHOW_AFTER_KEY_REDEEM) && ENVIRONMENT.PRODUCTION) {
                        return true;
                    }
                    for (const appFromBundle of bundle) {
                        if (APP_FROM_BUNDLE.APP_ID === APP.APP_ID) {
                            return true;
                        }
                    }
                    return false;
                })) {
                    const category = apps[0].CATEGORYS.FIND(c => C.NAME === catName);
                    DESCRIPTION_FIELDS.PUSH(CATEGORY.READABLE_NAME);
                    isCategorySet = true;
                }
            }
            if (isCategorySet) {
                continue;
            }
            // if apps are not directly of a category, list them
            for (const appOfBundle of bundle) {
                const app = THIS.KNOWN_APPS.FIND(app => APP.APP_ID === APP_OF_BUNDLE.APP_ID);
                DESCRIPTION_FIELDS.PUSH(APP.NAME);
            }
        }
        return DESCRIPTION_FIELDS.LENGTH === 0 ? null : DESCRIPTION_FIELDS.MAP(e => appPrefix + " " + e).join(", ");
    }

    private getAppsByCategory(): { [key: string]: GET_APPS.APP[]; } {
        const map: { [key: string]: GET_APPS.APP[]; } = {};
        for (const app of THIS.KNOWN_APPS) {
            for (const category of APP.CATEGORYS) {
                let appList: GET_APPS.APP[];
                if (map[CATEGORY.NAME]) {
                    appList = map[CATEGORY.NAME];
                } else {
                    appList = [];
                    map[CATEGORY.NAME] = appList;
                }
                APP_LIST.PUSH(app);
            }
        }
        return map;
    }

    /**
     * Gets the input fields.
     *
     * @returns the input fields
     */
    private getFields(): FormlyFieldConfig[] {
        const fields: FormlyFieldConfig[] = [];
        FIELDS.PUSH({
            key: "useRegisteredKeys",
            type: "checkbox",
            props: {
                label: THIS.TRANSLATE.INSTANT("EDGE.CONFIG.APP.KEY.USE_REGISTERED_KEY"),
            },
            hide: THIS.REGISTERED_KEYS.LENGTH === 0,
            expressions: {
                "PROPS.DISABLED": field => FIELD.MODEL.USE_MASTER_KEY,
            },
        });

        FIELDS.PUSH({
            key: "registeredKey",
            type: "select",
            props: {
                label: THIS.TRANSLATE.INSTANT("EDGE.CONFIG.APP.KEY.REGISTERED_KEY"),
                required: true,
                options: [],
            },
            expressions: {
                "hide": () => THIS.REGISTERED_KEYS.LENGTH === 0,
                "PROPS.DISABLED": field => !FIELD.MODEL.USE_REGISTERED_KEYS || FIELD.MODEL.USE_MASTER_KEY,
            },
            wrappers: ["formly-select-extended-wrapper"],
        });

        FIELDS.PUSH({
            key: "key",
            type: "input",
            props: {
                label: THIS.TRANSLATE.INSTANT("EDGE.CONFIG.APP.KEY.KEY"),
                required: true,
                placeholder: "XXXX-XXXX-XXXX-XXXX",
            },
            expressions: {
                "PROPS.DISABLED": field => FIELD.MODEL.USE_REGISTERED_KEYS || FIELD.MODEL.USE_MASTER_KEY,
            },
            validators: {
                validation: ["key"],
            },
            hooks: {
                onInit: (field) => {
                    FIELD.FORM_CONTROL.VALUE_CHANGES.SUBSCRIBE((next) => {
                        const nextInput = KEY_MODAL_COMPONENT.TRANSFORM_INPUT(next);
                        if (!nextInput) {
                            return;
                        }
                        FIELD.FORM_CONTROL.SET_VALUE(nextInput);
                    });
                },
            },
        });

        if (THIS.BEHAVIOUR !== KEY_VALIDATION_BEHAVIOUR.REGISTER
            && hasPredefinedKey(THIS.EDGE, THIS.SERVICE.METADATA.VALUE.USER)) {
            THIS.MODEL.USE_MASTER_KEY = true;
            FIELDS.PUSH(
                {
                    key: "useMasterKey",
                    type: "checkbox",
                    props: {
                        label: THIS.TRANSLATE.INSTANT("EDGE.CONFIG.APP.KEY.USE_MASTER_KEY"),
                    },
                },
                {
                    type: "text",
                    props: {
                        description: THIS.TRANSLATE.INSTANT("EDGE.CONFIG.APP.KEY.MASTER_KEY_HINT"),
                    },
                    expressions: {
                        hide: "!MODEL.USE_MASTER_KEY",
                    },
                },
            );
        }

        FIELDS.PUSH({
            type: "text",
            props: {
                description: THIS.TRANSLATE.INSTANT("EDGE.CONFIG.APP.KEY.KEY_TYPO_MESSAGE_HINT"),
            },
            hideExpression: "!FORM_STATE.GOT_INVALID_KEY_RESPONSE",
        });

        return fields;
    }

    private registerKey(): Promise<void> {
        return new Promise((resolve, reject) => {
            // key already registered
            if (THIS.LAST_VALID_KEY?.RESULT.ADDITIONAL_INFO.KEY_ID === THIS.GET_RAW_APP_KEY()
                && THIS.LAST_VALID_KEY.RESULT.ADDITIONAL_INFO.REGISTRATIONS.SOME(registration => {
                    return REGISTRATION.EDGE_ID === THIS.EDGE.ID && REGISTRATION.APP_ID === THIS.APP_ID;
                })) {
                resolve();
                return;
            }
            // only register key for this app
            THIS.EDGE.SEND_REQUEST(THIS.WEBSOCKET, new APP_CENTER.REQUEST({
                payload: new APP_CENTER_ADD_REGISTER_KEY_HISTORY.REQUEST({
                    key: THIS.GET_RAW_APP_KEY(),
                    ...(THIS.APP_ID && { appId: THIS.APP_ID }),
                }),
            })).then(() => {
                resolve();
            }).catch(reason => {
                reject(reason);
            });
        });
    }

    /**
     * Gets the selected key.
     *
     * @returns the selected key
     */
    private getSelectedKey() {
        if (THIS.MODEL.USE_REGISTERED_KEYS) {
            return THIS.REGISTERED_KEYS.FIND(k => K.KEY_ID === THIS.GET_RAW_APP_KEY());
        }
        return { keyId: THIS.GET_RAW_APP_KEY() };
    }

    /**
     * Gets the currently entered key.
     *
     * @returns the entered key
     */
    private getRawAppKey(): string {
        if (THIS.MODEL.USE_REGISTERED_KEYS) {
            return THIS.MODEL.REGISTERED_KEY;
        } else {
            return THIS.MODEL.KEY;
        }
    }

}

export enum KeyValidationBehaviour {
    REGISTER = 0,
    NAVIGATE = 1,
    SELECT = 2,
}
