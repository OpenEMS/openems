import { Component, Input, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { Edge, Service, Websocket } from 'src/app/shared/shared';
import { environment } from 'src/environments';
import { GetApps } from '../jsonrpc/getApps';
import { AppCenter } from './appCenter';
import { AppCenterAddRegisterKeyHistory } from './appCenterAddRegisterKeyHistory';
import { AppCenterGetRegisteredKeys } from './appCenterGetRegisteredKeys';
import { AppCenterIsKeyApplicable } from './appCenterIsKeyApplicable';
import { Key } from './key';

@Component({
    selector: KeyModalComponent.SELECTOR,
    templateUrl: './modal.component.html',
})
export class KeyModalComponent implements OnInit {

    @Input() public edge: Edge;
    @Input() public appId: string | null = null;
    @Input() public appName: string | null = null;
    @Input() public behaviour: KeyValidationBehaviour;

    @Input() public knownApps: GetApps.App[] | null = null;

    private static readonly SELECTOR = 'key-modal';
    public readonly spinnerId: string = KeyModalComponent.SELECTOR;

    private lastValidKey: AppCenterIsKeyApplicable.Response | null = null;
    private registeredKeys: Key[] = []

    protected form: FormGroup;
    protected fields: FormlyFieldConfig[]
    protected model;

    constructor(
        private service: Service,
        protected modalCtrl: ModalController,
        private router: Router,
        private websocket: Websocket,
        private translate: TranslateService
    ) { }


    public ngOnInit(): void {
        this.form = new FormGroup({});
        this.model = {
            'useRegisteredKeys': false,
            'registeredKey': '',
            'key': '',
        };

        if (this.behaviour === KeyValidationBehaviour.REGISTER) {
            this.fields = this.getFields();
            return;
        }
        this.service.startSpinner(this.spinnerId);
        this.edge.sendRequest(this.websocket, new AppCenter.Request({
            payload: new AppCenterGetRegisteredKeys.Request({
                ...(this.appId && { appId: this.appId })
            })
        })).then(response => {
            const result = (response as AppCenterGetRegisteredKeys.Response).result;
            this.registeredKeys = result.keys;
            this.fields = this.getFields();
            if (this.registeredKeys.length > 0) {
                this.model['useRegisteredKeys'] = true;
                this.model['registeredKey'] = this.registeredKeys[0].keyId;
            }
            const selectRegisteredKey = this.fields.find(f => f.key === 'registeredKey');
            this.registeredKeys.forEach(key => {
                const desc = this.getDescription(key);
                (selectRegisteredKey.props.options as any[]).push({
                    value: key.keyId,
                    label: key.keyId,
                    description: desc
                });
            });
        }).catch(reason => {
            this.fields = this.getFields();
            this.service.toast(this.translate.instant('Edge.Config.App.Key.failedLoadingRegisterKey'), 'danger');
        }).finally(() => {
            this.service.stopSpinner(this.spinnerId);
        });
    }

    private getDescription(key: Key): string | null {
        if (!this.knownApps) {
            return null;
        }
        const bundles = key.bundles
        if (!bundles) {
            return null;
        }
        if (!bundles.some(bundle => bundle.length != 0)) {
            return null;
        }

        const appPrefix = environment.edgeShortName + ' App';
        // map to multiple description fields
        const descriptionFields = [];
        for (const bundle of bundles) {
            let isCategorySet = false;
            // if multiple apps are in bundle find category which has all the apps 
            // and set the category name as the description
            for (const [catName, apps] of Object.entries(this.getAppsByCategory())) {
                if (apps.every(app => {
                    for (const appFromBundle of bundle) {
                        if (appFromBundle.appId === app.appId) {
                            return true;
                        }
                    }
                    return false;
                })) {
                    const category = apps[0].categorys.find(c => c.name === catName);
                    descriptionFields.push(category.readableName);
                    isCategorySet = true;
                }
            }
            if (isCategorySet) {
                continue;
            }
            // if apps are not directly of a category, list them
            for (const appOfBundle of bundle) {
                const app = this.knownApps.find(app => app.appId === appOfBundle.appId);
                descriptionFields.push(app.name);
            }
        }
        return descriptionFields.length === 0 ? null : descriptionFields.map(e => appPrefix + ' ' + e).join(", ");
    }

    private getAppsByCategory(): { [key: string]: GetApps.App[]; } {
        const map: { [key: string]: GetApps.App[]; } = {}
        for (const app of this.knownApps) {
            for (const category of app.categorys) {
                let appList: GetApps.App[]
                if (map[category.name]) {
                    appList = map[category.name]
                } else {
                    appList = []
                    map[category.name] = appList
                }
                appList.push(app)
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
        fields.push({
            key: 'useRegisteredKeys',
            type: 'checkbox',
            props: {
                label: this.translate.instant('Edge.Config.App.Key.useRegisteredKey')
            },
            hide: this.registeredKeys.length === 0,
        });

        fields.push({
            key: 'registeredKey',
            type: 'select',
            props: {
                label: this.translate.instant('Edge.Config.App.Key.registeredKey'),
                required: true,
                options: []
            },
            expressions: {
                hide: () => this.registeredKeys.length === 0,
                'props.disabled': field => !field.model.useRegisteredKeys
            },
            wrappers: ['formly-select-extended-wrapper']
        });

        fields.push({
            key: 'key',
            type: 'input',
            props: {
                label: this.translate.instant('Edge.Config.App.Key.key'),
                required: true,
                placeholder: 'XXXX-XXXX-XXXX-XXXX'
            },
            expressions: {
                'templateOptions.disabled': field => field.model.useRegisteredKeys
            },
            validators: {
                validation: ['key'],
            },
            hooks: {
                onInit: (field) => {
                    field.formControl.valueChanges.subscribe((next) => {
                        const nextInput = KeyModalComponent.transformInput(next)
                        if (!nextInput) {
                            return;
                        }
                        field.formControl.setValue(nextInput)
                    });
                }
            }
        });
        return fields;
    }

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
        let trimmed = value.replace(/\s+/g, '');

        // trimm max length of input
        if (trimmed.length > 19) {
            trimmed = trimmed.substring(0, 19);
        }

        // remove last dash
        let hasDashAsLastChar = trimmed.substring(trimmed.length - 1, trimmed.length) == "-"
        trimmed = trimmed.replace(/-/g, '');

        let numbers = [];

        // push single parts into array
        numbers.push(trimmed.substring(0, 4));
        if (trimmed.substring(4, 8) !== '') numbers.push(trimmed.substring(4, 8));
        if (trimmed.substring(8, 12) != '') numbers.push(trimmed.substring(8, 12));
        if (trimmed.substring(12, 16) != '') numbers.push(trimmed.substring(12, 16));

        // join parts so it matches 'XXXX-XXXX-XXXX-XXXX'
        let modifiedValue = numbers.join('-');
        // readd last 
        if (hasDashAsLastChar) {
            modifiedValue += '-';
        }

        // if there was no change to the original value return null
        if (modifiedValue === value) {
            return null;
        }
        return modifiedValue;
    }


    /**
     * Depending on the behaviour:
     * 
     * KeyValidationBehaviour.NAVIGATE:
     *  navigates to the install page of the app and passes the key
     * 
     * KeyValidationBehaviour.REGISTER:
     *  registers the entered key for the passed app
     * 
     * KeyValidationBehaviour.SELECT:
     *  if a valid key gets selected it gets returned
     */
    protected onClickCreateApp(): void {
        switch (this.behaviour) {
            case KeyValidationBehaviour.NAVIGATE:
                this.service.startSpinner(this.spinnerId);
                this.modalCtrl.dismiss({ 'key': this.getSelectedKey() });
                // navigate to App install view and pass valid key
                this.router.navigate(['device/' + (this.edge.id) + '/settings/app/install/' + this.appId]
                    , { queryParams: { name: this.appName }, state: { appKey: this.getRawAppKey() } });
                this.service.stopSpinner(this.spinnerId);
                break;
            case KeyValidationBehaviour.REGISTER:
                this.service.startSpinner(this.spinnerId);
                // only register key for this app
                this.registerKey().then(() => {
                    this.modalCtrl.dismiss({ 'key': this.getSelectedKey() });
                    this.service.toast(this.translate.instant('Edge.Config.App.Key.successRegisterKey'), 'success');
                }).catch(() => {
                    this.service.toast(this.translate.instant('Edge.Config.App.Key.failedRegisterKey'), 'danger');
                }).finally(() => {
                    this.service.stopSpinner(this.spinnerId);
                });
                break;
            case KeyValidationBehaviour.SELECT:
                this.modalCtrl.dismiss({ key: this.getSelectedKey() });
        }
    }

    private registerKey(): Promise<void> {
        return new Promise((resolve, reject) => {
            // key already registered
            if (this.lastValidKey?.result.additionalInfo.keyId === this.getRawAppKey()
                && this.lastValidKey.result.additionalInfo.registrations.some(registration => {
                    return registration.edgeId === this.edge.id && registration.appId === this.appId;
                })) {
                resolve()
                return;
            }
            // only register key for this app
            this.edge.sendRequest(this.websocket, new AppCenter.Request({
                payload: new AppCenterAddRegisterKeyHistory.Request({
                    key: this.getRawAppKey(),
                    ...(this.appId && { appId: this.appId }),
                })
            })).then(() => {
                resolve();
            }).catch(reason => {
                reject(reason);
            })
        });
    }

    /**
     * Gets the selected key.
     * 
     * @returns the selected key
     */
    private getSelectedKey() {
        if (this.model['useRegisteredKeys']) {
            return this.registeredKeys.find(k => k.keyId === this.getRawAppKey())
        } else {
            return { keyId: this.getRawAppKey() }
        }
    }

    /**
     * Validates the currently entered key.
     */
    protected validateKey(): void {
        if (this.form.invalid) {
            return
        }
        const appKey = this.getRawAppKey();
        const request = new AppCenter.Request({
            payload: new AppCenterIsKeyApplicable.Request({ key: appKey, appId: this.appId })
        });

        this.service.startSpinner(this.spinnerId);
        this.edge.sendRequest(this.websocket, request)
            .then((response) => {
                const result = (response as AppCenterIsKeyApplicable.Response).result;
                if (result.isKeyApplicable) {
                    this.lastValidKey = (response as AppCenterIsKeyApplicable.Response);

                    if (result.additionalInfo.registrations.length !== 0
                        && this.behaviour === KeyValidationBehaviour.REGISTER) {
                        const differentEdge = result.additionalInfo.registrations.some(registration => {
                            return registration.edgeId !== this.edge.id;
                        });
                        if (differentEdge) {
                            this.service.toast(this.translate.instant('Edge.Config.App.Key.alreadyRegisteredDifferentSystem'), 'warning');
                            return;
                        }
                        const sameApp = result.additionalInfo.registrations.some(registration => {
                            return registration.appId === this.appId && registration.edgeId === this.edge.id;
                        });
                        if (!sameApp) {
                            this.service.toast(this.translate.instant('Edge.Config.App.Key.alreadyRegisteredDifferentApp'), 'warning');
                            return;
                        }
                    }

                    this.service.toast(this.translate.instant('Edge.Config.App.Key.valid'), 'success');
                } else {
                    this.service.toast(this.translate.instant('Edge.Config.App.Key.invalid'), 'danger');
                }
            }).catch(reason => {
                // this may happen if the key is not stored in the database
                this.service.toast(this.translate.instant('Edge.Config.App.Key.invalid'), 'danger');
                if (environment.debugMode) {
                    console.log('Failed to validate Key', reason);
                }
            }).finally(() => {
                this.service.stopSpinner(this.spinnerId);
            });
    }

    /**
     * Gets the currently entered key.
     * 
     * @returns the entered key
     */
    private getRawAppKey(): string {
        if (this.model['useRegisteredKeys']) {
            return this.model['registeredKey']
        } else {
            return this.model['key']
        }
    }

    /**
     * Determines if the current selected key is valid.
     * 
     * @returns true if the current selected key is valid
     */
    protected isKeyValid(): boolean {
        if (this.model['useRegisteredKeys']) {
            return true
        }
        return this.lastValidKey !== null && this.getRawAppKey() === this.lastValidKey.result.additionalInfo.keyId;
    };

}

export enum KeyValidationBehaviour {
    REGISTER = 0,
    NAVIGATE = 1,
    SELECT = 2,
}