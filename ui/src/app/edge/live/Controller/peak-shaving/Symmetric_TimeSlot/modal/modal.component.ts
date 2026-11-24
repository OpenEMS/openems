import { ChangeDetectionStrategy, Component, Input, OnInit } from "@angular/core";
import { FormBuilder, FormControl, FormGroup, Validators } from "@angular/forms";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { Edge, EdgeConfig, Service, Websocket } from "../../../../../../shared/shared";

@Component({
    selector: "timeslotpeakshaving-modal",
    templateUrl: "./modal.component.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class Controller_Symmetric_TimeSlot_PeakShavingModalComponent implements OnInit {
    @Input() protected component: EdgeConfig.Component | null = null;
    @Input() protected edge: Edge | null = null;

    public formGroup!: FormGroup;
    public loading: boolean = false;

    constructor(
        public formBuilder: FormBuilder,
        public modalCtrl: ModalController,
        public service: Service,
        public translate: TranslateService,
        public websocket: Websocket,
    ) {}

    ngOnInit() {
        const component = this.getModalComponent();
        this.formGroup = this.formBuilder.group({
            peakShavingPower: new FormControl(
                component.properties.peakShavingPower,
                Validators.compose([Validators.pattern("^(?:[1-9][0-9]*|0)$"), Validators.required]),
            ),
            rechargePower: new FormControl(
                component.properties.rechargePower,
                Validators.compose([Validators.pattern("^(?:[1-9][0-9]*|0)$"), Validators.required]),
            ),
            hysteresisSoc: new FormControl(
                component.properties.hysteresisSoc,
                Validators.compose([Validators.pattern("^(100|[1-9]?[0-9])$"), Validators.required]),
            ),
            slowChargePower: new FormControl(component.properties.slowChargePower * -1),
            slowChargeStartTime: new FormControl(
                component.properties.slowChargeStartTime,
                Validators.compose([Validators.pattern("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"), Validators.required]),
            ),
            startDate: new FormControl(
                component.properties.startDate,
                Validators.compose([
                    Validators.pattern("^(0[1-9]|[12][0-9]|3[01])[.](0[1-9]|1[012])[.](19|20)[0-9]{2}$"),
                    Validators.required,
                ]),
            ),
            startTime: new FormControl(
                component.properties.startTime,
                Validators.compose([Validators.pattern("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"), Validators.required]),
            ),
            endDate: new FormControl(component.properties.endDate),
            endTime: new FormControl(
                component.properties.endTime,
                Validators.compose([Validators.pattern("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"), Validators.required]),
            ),
            monday: new FormControl(component.properties.monday),
            tuesday: new FormControl(component.properties.tuesday),
            wednesday: new FormControl(component.properties.wednesday),
            thursday: new FormControl(component.properties.thursday),
            friday: new FormControl(component.properties.friday),
            saturday: new FormControl(component.properties.saturday),
            sunday: new FormControl(component.properties.sunday),
        });
    }

    applyChanges() {
        if (this.edge == null) {
            return;
        }

        if (!this.edge.roleIsAtLeast("owner")) {
            this.service.toast(this.translate.instant("GENERAL.INSUFFICIENT_RIGHTS"), "danger");
            return;
        }

        const peakShavingPower = this.formGroup.controls["peakShavingPower"];
        const rechargePower = this.formGroup.controls["rechargePower"];

        if (!peakShavingPower.valid || !rechargePower.valid) {
            this.service.toast(this.translate.instant("GENERAL.INPUT_NOT_VALID"), "danger");
            return;
        }

        if (Number(peakShavingPower.value) < Number(rechargePower.value)) {
            this.service.toast(this.translate.instant("EDGE.INDEX.WIDGETS.PEAKSHAVING.RELATION_ERROR"), "danger");
            return;
        }

        const updateComponentArray = Object.keys(this.formGroup.controls)
            .filter((key) => this.formGroup.controls[key].dirty)
            .map((key) => {
                let val = this.formGroup.controls[key].value;
                if (key === "slowChargePower") {
                    val = val * -1;
                }
                return { name: key, value: val };
            });

        this.loading = true;

        const component = this.getModalComponent();
        this.edge
            .updateComponentConfig(this.websocket, component.id, updateComponentArray)
            .then(() => {
                component.properties.peakShavingPower = peakShavingPower.value;
                component.properties.rechargePower = rechargePower.value;
                this.service.toast(this.translate.instant("GENERAL.CHANGE_ACCEPTED"), "success");
            })
            .catch((reason) => {
                peakShavingPower.setValue(component.properties.peakShavingPower);
                rechargePower.setValue(component.properties.rechargePower);
                this.service.toast(
                    this.translate.instant("GENERAL.CHANGE_FAILED") + "\n" + reason.error.message,
                    "danger",
                );
                console.warn(reason);
            })
            .finally(() => {
                this.loading = false;
                this.formGroup.markAsPristine();
            });
    }

    private getModalComponent(): EdgeConfig.Component {
        const component = this.component;
        AssertionUtils.assertIsDefined(component);
        return component;
    }
}
