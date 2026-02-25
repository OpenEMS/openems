import { ChangeDetectorRef, Directive, Inject, inject } from "@angular/core";
import { FormBuilder } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController, ViewWillEnter } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { RouteService } from "src/app/shared/service/route.service";
import { Websocket, Service } from "../../shared";
import { Language } from "../../type/language";
import { AbstractModal } from "../modal/abstractModal";
import { NavigationService } from "../navigation/service/navigation.service";
import { NavigationTree } from "../navigation/shared";
import de from "./i18n/de.json";
import en from "./i18n/en.json";

@Directive()
export class JsCalendarEditTaskComponent extends AbstractModal implements ViewWillEnter {
    protected routeService = inject(RouteService);
    protected navigationService = inject(NavigationService);

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
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                this.translate.setTranslation(lang, translation, shouldMerge);
            }
        });
    }
    public async ionViewWillEnter(): Promise<void> {
        const componentId = this.routeService.getRouteParam<string>("componentId");
        const uid = this.routeService.getRouteParam<string>("taskId");
        const config = await this.service.getConfig();
        this.component = config.getComponentSafely(componentId);

        const newNavigationTree = new NavigationTree(
            "edit-task",
            { baseString: `schedule/task/${uid}` },
            { name: "create-outline" },
            this.translate.instant("JS_SCHEDULE.EDIT_TASK"),
            "label",
            [],
            null
        );

        this.navigationService.setChildNavigationToCurrentNavigation(newNavigationTree);
    }
}
