import { ChangeDetectionStrategy, ChangeDetectorRef, Component, computed, inject, signal } from "@angular/core";
import { FormBuilder } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { IonIcon, ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { HelpButtonComponent } from "src/app/shared/components/modal/help-button/help-button";
import { NavigationService } from "src/app/shared/components/navigation/service/navigation.service";
import { Service, Websocket } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { Icon } from "src/app/shared/type/widget";
import { environment } from "src/environments";
import de from "./i18n/de.json";
import en from "./i18n/en.json";

type IconWithRequiredName = Required<Pick<Icon, "name">> & Partial<Omit<Icon, "name">>;

type NavigationCardFooter = {
    text: string;
    link: string;
    linkText: string;
};

export type NavigationCard = {
    infoText: string;
    iconName: IonIcon["name"];
    contentText: string;
    buttonText: string;
    buttonHref: string;
    buttonIconName?: string;
    footer?: NavigationCardFooter;
};

function initializeNavigationInfoTranslations(translate: TranslateService): void {
    void Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
        for (const { lang, translation, shouldMerge } of translations) {
            translate.setTranslation(lang, translation, shouldMerge);
        }
    });
}

@Component({
    templateUrl: "./navigation-info.html",
    standalone: false,
    changeDetection: ChangeDetectionStrategy.Eager,
    styles: [
        `
            ::ng-deep formly-form {
                height: 100% !important;
            }
        `,
    ],
})
export class NavigationInfoComponent extends AbstractModal {
    protected readonly isGlobalInfo = signal(false);

    protected readonly cards = computed<NavigationCard[]>(() => {
        const cards: NavigationCard[] = [
            {
                infoText: this.translate.instant("BETA_TEST.NEW_UI_INFO"),
                iconName: "information-outline",
                contentText: this.translate.instant("BETA_TEST.CHANGELOG"),
                buttonText: this.translate.instant("BETA_TEST.BUTTON"),
                buttonHref: this.link ?? "",
                footer: {
                    text: this.translate.instant("BETA_TEST.FEEDBACK"),
                    link: "",
                    linkText: this.translate.instant("GENERAL.SURVEY_LINK"),
                },
            },
        ];
        return cards;
    });

    protected link = environment.links.REDIRECT.BETA_CHANGE_LOG;
    protected docs: { link: string | null; displayName: string; icon: IconWithRequiredName } | null = null;

    private navigationService = inject(NavigationService);

    constructor(
        protected override websocket: Websocket,
        protected override route: ActivatedRoute,
        protected override service: Service,
        public override modalController: ModalController,
        protected override translate: TranslateService,
        public override formBuilder: FormBuilder,
        public override ref: ChangeDetectorRef,
    ) {
        super(websocket, route, service, modalController, translate, formBuilder, ref);

        initializeNavigationInfoTranslations(translate);
    }

    public static readonly DOCS_LINKS: (
        translate: TranslateService,
    ) => Map<
        string,
        { displayName: string; icon: IconWithRequiredName; link: NonNullable<HelpButtonComponent["key"]> }
    > = (translate) =>
        new Map([
            [
                "grid",
                {
                    displayName: translate.instant("NAVIGATION_INFO_MANUAL", {
                        source: translate.instant("GENERAL.GRID"),
                    }),
                    link: "REDIRECT.COMMON_GRID",
                    icon: { name: "oe-grid" },
                },
            ],
            [
                "production",
                {
                    displayName: translate.instant("NAVIGATION_INFO_MANUAL", {
                        source: translate.instant("GENERAL.PRODUCTION"),
                    }),
                    link: "REDIRECT.COMMON_PRODUCTION",
                    icon: { name: "oe-production" },
                },
            ],
            [
                "storage",
                {
                    displayName: translate.instant("NAVIGATION_INFO_MANUAL", {
                        source: translate.instant("GENERAL.STORAGE"),
                    }),
                    link: "REDIRECT.COMMON_STORAGE",
                    icon: { name: "oe-storage" },
                },
            ],
            [
                "consumption",
                {
                    displayName: translate.instant("NAVIGATION_INFO_MANUAL", {
                        source: translate.instant("GENERAL.CONSUMPTION"),
                    }),
                    link: "REDIRECT.COMMON_CONSUMPTION",
                    icon: { name: "oe-consumption" },
                },
            ],
        ]);

    ionViewWillLeave() {
        this.ngOnDestroy();
    }

    ionViewWillEnter() {
        this.navigationService.headerTitle.set(this.translate.instant("GENERAL.INFO"));
    }
}
