import { Directive, Inject, Input, } from "@angular/core";
import { FormBuilder, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";

@Directive()
export abstract class AbstractModal {

    @Input() component: EdgeConfig.Component = null;
    @Input() formGroup: FormGroup = null;
    @Input() controlName: string;

    public edge: Edge = null;

    constructor(
        @Inject(Websocket) protected websocket: Websocket,
        @Inject(ActivatedRoute) protected route: ActivatedRoute,
        @Inject(Service) protected service: Service,
        @Inject(ModalController) public modalCtrl: ModalController,
        @Inject(TranslateService) protected translate: TranslateService,
        @Inject(FormBuilder) public formBuilder: FormBuilder,
    ) {
    }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        });
    }

    /**
     * Applies all Value-Changes with updating ComponentConfig
     */
    applyChanges() {
        if (this.edge != null) {
            if (this.edge.roleIsAtLeast('owner')) {

                /** fill udateComponentArray with changed formgroup.controls */
                let updateComponentArray = [];
                Object.keys(this.formGroup.controls).forEach((element, index) => {
                    if (this.formGroup.controls[element].dirty) {
                        updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value })
                    }
                })

                /** Update component.properties */
                this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {

                    /** set Components-properties-value to FormGroup-value   */
                    for (let i = 0; i < updateComponentArray.length; i++) {
                        this.component.properties[updateComponentArray[i].name] = this.formGroup.controls[updateComponentArray[i].name].value
                    }
                    this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
                }).catch(reason => {

                    /** set Formgroup-value to Components-properties-value */
                    for (let i = 0; i < updateComponentArray.length; i++) {
                        this.formGroup.controls[updateComponentArray[i].name].setValue(this.component.properties[updateComponentArray[i].name])
                    }
                    this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
                    console.warn(reason);
                })
                this.formGroup.markAsPristine()
            } else {
                this.service.toast(this.translate.instant('General.insufficientRights'), 'danger');
            }
        }
    }

    /**
       * Called on every new data.
       * 
       * @param currentData new data for the subscribed Channel-Addresses
       */
    protected onCurrentData(currentData: CurrentData) {
    }

    /**
     * Gets the ChannelAddresses that should be subscribed.
     */
    protected getChannelAddresses(): ChannelAddress[] {
        return [];
    }

    /**
   * Gets the ChannelIds of the current Component that should be subscribed.
   */
    protected getChannelIds(): string[] {
        return [];
    }

    /** Gets the FormGroup of the current Component */
    protected getFormGroup(): FormGroup {
        return
    }
}