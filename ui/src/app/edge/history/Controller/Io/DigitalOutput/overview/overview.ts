// @ts-strict-ignore
import { Component, inject } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { NavigationOption } from "src/app/shared/components/footer/subnavigation/footerNavigation";
import { ChannelAddress, EdgeConfig, Service } from "src/app/shared/shared";

@Component({
    selector: "overview",
    templateUrl: "./overview.html",
    standalone: false,
})
export class OverviewComponent extends AbstractHistoryChartOverview {
    override service: Service;
    protected override route: ActivatedRoute;
    override modalCtrl: ModalController;
    private router = inject(Router);


    protected navigationButtons: NavigationOption[] = [];

    /** Inserted by Angular inject() migration for backwards compatibility */
    constructor(...args: unknown[]);

    constructor() {
        const service = inject(Service);
        const route = inject(ActivatedRoute);
        const modalCtrl = inject(ModalController);

        super(service, route, modalCtrl);
    
        this.service = service;
        this.route = route;
        this.modalCtrl = modalCtrl;
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        const fixDigitalOutputControllers: EdgeConfig.Component[] = this.config.getComponentsByFactory("Controller.Io.FixDigitalOutput");
        const singleThresholdControllers: EdgeConfig.Component[] = this.config.getComponentsByFactory("Controller.IO.ChannelSingleThreshold");
        const controllers = [...fixDigitalOutputControllers, ...singleThresholdControllers];

        this.navigationButtons = controllers.map(el => (
            { id: el.id, alias: el.alias, callback: () => { this.router.navigate(["./" + el.id], { relativeTo: this.route }); } }
        ));
        return [];
    }
}
