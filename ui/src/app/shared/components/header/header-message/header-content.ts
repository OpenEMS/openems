import { Component, effect, inject, ChangeDetectionStrategy, computed } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { v4 as uuidv4 } from "uuid";

import { PlatFormService } from "src/app/platform.service";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { LiveDataServiceProvider } from "src/app/shared/provider/live-data-service-provider";
import { RouteService } from "src/app/shared/service/route.service";
import { UserService } from "src/app/shared/service/user.service";
import { Service } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { TSignalValue } from "src/app/shared/type/utility";
import { StringUtils } from "src/app/shared/utils/string/string.utils";
import { environment } from "src/environments";
import { NavigationService } from "../../navigation/service/navigation.service";
import { OeImageComponent } from "../../oe-img/oe-img";
import { DataService } from "../../shared/dataservice";
import { SystemStatusComponent } from "../../status/system/system-status.component";
import de from "./i18n/de.json";
import en from "./i18n/en.json";

@Component({
    selector: "oe-header-message",
    imports: [CommonUiModule, SystemStatusComponent],
    templateUrl: "./header-content.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    providers: [LiveDataServiceProvider],
})
export class AppHeaderContentComponent {
    protected message: string | null = null;
    protected isSmartphone: boolean = false;
    protected image: OeImageComponent["img"] | null = null;
    protected service = inject(Service);
    protected navigationService = inject(NavigationService);
    protected readonly parentNodeLink = computed(() => {
        const parentNode = this.navigationService.currentNode()?.parent ?? null;
        if (parentNode?.parent == null) {
            return null;
        }
        return parentNode ?? null;
    });

    private liveDataService = inject(DataService);
    private translate = inject(TranslateService);
    private platFormService = inject(PlatFormService);
    private userService = inject(UserService);
    private routeService = inject(RouteService);

    constructor() {
        this.isSmartphone = this.platFormService.getDevice().isSmartphone();
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                this.translate.setTranslation(lang, translation, shouldMerge);
            }
        });

        effect(() => {
            const currentUser = this.userService.currentUser();
            if (currentUser == null) {
                return;
            }

            const imageUrl =
                this.userService.getValidBrowserTheme(currentUser.getThemeFromSettings()) === "dark"
                    ? environment.images.LOGO.DARK
                    : environment.images.LOGO.LIGHT;
            this.image = { url: imageUrl };
        });

        effect(() => {
            const currentEdge = this.service.currentEdge();

            if (currentEdge != null) {
                this.liveDataService.subscribeChannels(
                    [SystemStatusComponent.SUM_STATE_CHANNEL],
                    currentEdge,
                    uuidv4(),
                );
            }
        });

        effect(() => {
            const currentValue = this.liveDataService.currentValue();

            const currentUrl = this.routeService.currentUrl();
            const showMessage = StringUtils.isInArr(this.navigationService.position(), ["bottom", "left"]);
            if (currentUrl?.split("/")?.reverse()?.[0] === "live") {
                this.setChannelValueToSumState(currentValue, showMessage);
                return;
            }

            this.setChannelValueToSumState(null, showMessage);
        });
    }

    private setChannelValueToSumState(
        channelValue: TSignalValue<DataService["currentValue"]> | null,
        show: boolean = true,
    ) {
        if (!show) {
            this.message = null;
            return;
        }

        const key = (function (): string | null {
            switch (channelValue?.allComponents[SystemStatusComponent.SUM_STATE_CHANNEL.toString()]) {
                case 0:
                    return "SYSTEM_STATUS_MESSAGE.OK";
                case 1:
                    return "SYSTEM_STATUS_MESSAGE.INFO";
                case 2:
                    return "SYSTEM_STATUS_MESSAGE.WARNING";
                case 3:
                    return "SYSTEM_STATUS_MESSAGE.FAULT";
                default:
                    return null;
            }
        })();

        const message = key ? this.translate.instant(key) : null;

        this.message = message != null && this.isSmartphone ? message.replace("\n", "<br/>") : message;
    }
}
