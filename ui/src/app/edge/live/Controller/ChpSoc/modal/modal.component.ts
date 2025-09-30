// @ts-strict-ignore
import { Component, Input, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { RangeValue } from "@ionic/core";
import { TranslateService } from "@ngx-translate/core";
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";

type mode = "MANUAL_ON" | "MANUAL_OFF" | "AUTOMATIC";


@Component({
    selector: Controller_ChpSocModalComponent.SELECTOR,
    templateUrl: "./MODAL.COMPONENT.HTML",
    standalone: false,
})
export class Controller_ChpSocModalComponent implements OnInit {

    private static readonly SELECTOR = "chpsoc-modal";

    @Input({ required: true }) public edge!: Edge;
    @Input({ required: true }) public component!: EDGE_CONFIG.COMPONENT;
    @Input({ required: true }) public outputChannel!: ChannelAddress;
    @Input({ required: true }) public inputChannel!: ChannelAddress;

    public thresholds: RangeValue = {
        lower: null,
        upper: null,
    };

    constructor(
        public service: Service,
        public websocket: Websocket,
        public router: Router,
        protected translate: TranslateService,
        public modalCtrl: ModalController,
    ) { }

    ngOnInit() {
        THIS.THRESHOLDS["lower"] = THIS.COMPONENT.PROPERTIES["lowThreshold"];
        THIS.THRESHOLDS["upper"] = THIS.COMPONENT.PROPERTIES["highThreshold"];
    }

    /**
    * Updates the Charge-Mode of the EVCS-Controller.
    *
    * @param event
    */
    updateMode(event: CustomEvent) {
        const oldMode = THIS.COMPONENT.PROPERTIES.MODE;
        let newMode: mode;

        switch (EVENT.DETAIL.VALUE) {
            case "MANUAL_ON":
                newMode = "MANUAL_ON";
                break;
            case "MANUAL_OFF":
                newMode = "MANUAL_OFF";
                break;
            case "AUTOMATIC":
                newMode = "AUTOMATIC";
                break;
        }

        if (THIS.EDGE != null) {
            THIS.EDGE.UPDATE_COMPONENT_CONFIG(THIS.WEBSOCKET, THIS.COMPONENT.ID, [
                { name: "mode", value: newMode },
            ]).then(() => {
                THIS.COMPONENT.PROPERTIES.MODE = newMode;
                THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
            }).catch(reason => {
                THIS.COMPONENT.PROPERTIES.MODE = oldMode;
                THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED") + "\n" + REASON.ERROR.MESSAGE, "danger");
                CONSOLE.WARN(reason);
            });
        }
    }

    /**
    * Updates the Min-Power of force charging
    *
    * @param event
    */
    updateThresholds() {
        const oldLowerThreshold = THIS.COMPONENT.PROPERTIES["lowThreshold"];
        const oldUpperThreshold = THIS.COMPONENT.PROPERTIES["highThreshold"];

        const newLowerThreshold = THIS.THRESHOLDS["lower"];
        const newUpperThreshold = THIS.THRESHOLDS["upper"];

        // prevents automatic update when no values have changed
        if (THIS.EDGE != null && (oldLowerThreshold != newLowerThreshold || oldUpperThreshold != newUpperThreshold)) {
            THIS.EDGE.UPDATE_COMPONENT_CONFIG(THIS.WEBSOCKET, THIS.COMPONENT.ID, [
                { name: "lowThreshold", value: newLowerThreshold },
                { name: "highThreshold", value: newUpperThreshold },
            ]).then(() => {
                THIS.COMPONENT.PROPERTIES["lowThreshold"] = newLowerThreshold;
                THIS.COMPONENT.PROPERTIES["highThreshold"] = newUpperThreshold;
                THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
            }).catch(reason => {
                THIS.COMPONENT.PROPERTIES["lowThreshold"] = oldLowerThreshold;
                THIS.COMPONENT.PROPERTIES["highThreshold"] = oldUpperThreshold;
                THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED") + "\n" + REASON.ERROR.MESSAGE, "danger");
                CONSOLE.WARN(reason);
            });
        }
    }
}

