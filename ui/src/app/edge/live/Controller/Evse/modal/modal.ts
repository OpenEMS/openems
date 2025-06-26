// @ts-strict-ignore
import { ChangeDetectorRef, Component, Inject } from "@angular/core";
import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { filter, take } from "rxjs";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { Converter } from "src/app/shared/components/shared/converter";
import { EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { ControllerEvseSingleShared } from "../shared/shared";

@Component({
    selector: "oe-controller-evse-single-modal",
    templateUrl: "./modal.html",
    standalone: false,
})
export class ModalComponent extends AbstractModal {

    protected showNewFooter: boolean = true;
    protected label: string | null = null;
    protected meterId: string | null = null;

    constructor(
        @Inject(Websocket) protected override websocket: Websocket,
        @Inject(ActivatedRoute) protected override route: ActivatedRoute,
        @Inject(Service) protected override service: Service,
        @Inject(ModalController) public override modalController: ModalController,
        @Inject(TranslateService) protected override translate: TranslateService,
        @Inject(FormBuilder) public override formBuilder: FormBuilder,
        public override ref: ChangeDetectorRef,
    ) {
        super(websocket, route, service, modalController, translate, formBuilder, ref);
    }

    override async updateComponent(config: EdgeConfig) {
        return new Promise<void>((res) => {
            this.route.params.pipe(filter(params => params != null), take(1)).subscribe((params) => {
                this.component = config.getComponent(params.componentId);
                this.meterId = this.config.getComponentFromOtherComponentsProperty(this.component.id, "chargePoint.id")?.id ?? null;
                res();
            });
        });
    }

    protected override getFormGroup(): FormGroup {
        AssertionUtils.assertIsDefined(this.component);
        return this.formBuilder.group({
            mode: new FormControl(this.component.properties.mode),
        });
    }

    protected hideFooter() {
        this.showNewFooter = !this.showNewFooter;
    }

    /**
 * Converts a value in Watt [W] to KiloWatt [kW].
 *
 * @param value the value from passed value in html
 * @returns converted value
 */
    protected CONVERT_TO_MODE: Converter = (raw) => {
        return Converter.IF_STRING(raw, value => {
            switch (value) {
                case ControllerEvseSingleShared.Mode.ZERO:
                    return this.translate.instant("MODE.ZERO");
                case ControllerEvseSingleShared.Mode.MINIMUM:
                    return this.translate.instant("MODE.MINIMUM");
                case ControllerEvseSingleShared.Mode.SURPLUS:
                    return this.translate.instant("MODE.SURPLUS");
                case ControllerEvseSingleShared.Mode.FORCE:
                    return this.translate.instant("MODE.FORCE");
                default:
                    return Converter.HIDE_VALUE(value);
            }
        });
    };


}
