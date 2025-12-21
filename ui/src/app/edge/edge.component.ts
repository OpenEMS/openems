// @ts-strict-ignore
import { Component, effect, OnDestroy, OnInit } from "@angular/core";
import { ModalController, ViewWillLeave } from "@ionic/angular";
import { Edge, Service, Websocket } from "src/app/shared/shared";
import { WeatherForecastApprovalComponent } from "../shared/components/edge/popover/data-privacy/popover";
import { Pagination } from "../shared/service/pagination";
import { RouteService } from "../shared/service/route.service";
import { UserService } from "../shared/service/user.service";

/*** This component is needed as a routing parent and acts as a transit station without being displayed.*/
@Component({
    selector: "edge",
    template: `
    <ion-content></ion-content>
    <ion-router-outlet id="content"></ion-router-outlet>
    `,
    standalone: false,
})
export class EdgeComponent implements OnDestroy, ViewWillLeave, OnInit {

    protected latestIncident: { message: string | null, id: string } | null = null;
    private edge: Edge | null = null;

    constructor(
        private routeService: RouteService,
        private service: Service,
        private websocket: Websocket,
        private pagination: Pagination,
        private popoverCtrl: ModalController,
        private userService: UserService,
    ) {

        effect(async () => {
            const edge = this.service.currentEdge();
            const edgeId = this.routeService.getRouteParam<string>("edgeId");
            if (!edgeId || !edge) {
                return;
            }

            pagination.subscribeEdge(edge);
            this.handlePrivacyPopover(edge);
        });
    }

    /**
     * Shows weather forecast approval.
     *
     * @param modalCtrl the modal controller
     * @returns
     */
    public static async showPrivacyPolicyPopover(modalCtrl: ModalController) {
        const popover = await modalCtrl.create({
            component: WeatherForecastApprovalComponent,
        });

        await popover.present();
        return popover.onDidDismiss();
    }

    public async ngOnInit() {
        const edgeId = this.routeService.getRouteParam<string>("edgeId");
        await this.service.updateCurrentEdge(edgeId);
    }

    public ionViewWillLeave() {
        this.ngOnDestroy();
    }

    public ngOnDestroy(): void {
        this.service.currentEdge.set(null);
        if (!this.edge) {
            return;
        }
        this.edge.unsubscribeAllChannels(this.websocket);
    }

    /**
     * Handles the privacy popover based on the user's choice.
     *
     * @param edge the edge
     */
    private async handlePrivacyPopover(edge: Edge): Promise<void> {
        const showPrivacyPolicyPopover = await edge.shouldShowPrivacyPolicyPopover(this.websocket);
        if (showPrivacyPolicyPopover == false) {
            return;
        }

        await EdgeComponent.showPrivacyPolicyPopover(this.popoverCtrl);
    }
}
