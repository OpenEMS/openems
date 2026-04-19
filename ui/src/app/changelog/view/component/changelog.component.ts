import { HttpClient } from "@angular/common/http";
import { Component, effect } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { firstValueFrom } from "rxjs";
import { PlatFormService } from "src/app/platform.service";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { User } from "src/app/shared/jsonrpc/shared";
import { UserService } from "src/app/shared/service/user.service";
import { Language, LanguageKeyUnion } from "src/app/shared/type/language";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { ObjectUtils } from "src/app/shared/utils/object/object-utils";
import { environment } from "src/environments";
import { Service } from "../../../shared/shared";
import { Role } from "../../../shared/type/role";
import de from "../../i18n/de.json";
import en from "../../i18n/en.json";

@Component({
    selector: "changelog",
    templateUrl: "./changelog.component.html",
    standalone: true,
    imports: [CommonUiModule],
})
export class ChangelogComponent {

    public environment = environment;

    public readonly roleIsAtLeast = Role.isAtLeast;
    public changelogs: { title?: string, version?: string, changes: { [lang: LanguageKeyUnion | "all"]: Array<{ roleIsAtLeast?: Role, change: string }> } }[] = [];

    protected userLanguage: User["language"] | null = null;
    protected slice: number = 10;
    protected showAll: boolean = false;

    constructor(
        public translate: TranslateService,
        public service: Service,
        private http: HttpClient,
        private platFormService: PlatFormService,
        userService: UserService,
    ) {

        effect(async () => {
            const user = userService.currentUser();
            if (user == null) {
                return;
            }
            await this.setChangelogs(user);
        });

        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                translate.setTranslation(lang, translation, shouldMerge);
            }
        });
    }

    public numberToRole(role: number): string {
        return Role[role].toLowerCase();
    }

    /**
     * Sets the changelog.
     */
    private async setChangelogs(user: User) {
        this.changelogs = await this.convertToChangelog(user);
    }

    /**
     *
     * @returns
     */
    private async getChangelogJson(): Promise<ChangelogJson[]> {
        return await firstValueFrom(
            this.http.get<ChangelogJson[]>(this.getChangeLogUrl())
        );
    }

    private async convertToChangelog(user: User): Promise<typeof this.changelogs> {
        const changeLogJson = await this.getChangelogJson();
        return this.mapChangelogByLang(changeLogJson.map(el => ({
            version: el.version,
            changes: Object.fromEntries(
                Object.entries(el.entries).map(([lang, entries]) => {
                    return [
                        lang,
                        entries.map(e => {
                            if (typeof e === "string") {
                                return { change: e };
                            }
                            return {
                                roleIsAtLeast: e?.roleIsAtLeast != null ? Role.getRole(e.roleIsAtLeast.toLowerCase()) : Role.GUEST,
                                change: e.text,
                            };
                        }),
                    ];
                })
            ),
        })), user);
    }

    private mapChangelogByLang(changelogs: typeof this.changelogs, user: User): typeof this.changelogs {
        if (environment.production == false) {
            return changelogs;
        }

        AssertionUtils.assertIsDefined(user);
        const allLangs = new Set(...changelogs.map(el => Object.keys(el.changes)));
        this.setUserLanguage(user, allLangs);

        return changelogs.map(el => {
            const changes: (typeof changelogs)[number]["changes"] = Object.entries(el.changes).reduce((arr: (typeof changelogs)[number]["changes"], [language, changes]) => {
                const filteredChanges = changes.filter(el => {
                    const roleIsAtLeast = ObjectUtils.getKeySafely(el, "roleIsAtLeast");
                    if (roleIsAtLeast != null) {
                        return Role.isAtLeast(Role.getRole(user.globalRole), roleIsAtLeast);
                    }
                    return true;
                });

                arr[language] = filteredChanges;
                return arr;
            }, {});
            return { ...el, changes: changes };
        });
    }

    private setUserLanguage(user: User, allLangs: Set<string>) {
        const userLanguage: string | null = user.language?.toLowerCase() ?? null;
        if (userLanguage != null && allLangs.has(userLanguage)) {
            this.userLanguage = userLanguage;
            return;
        }

        this.userLanguage = Language.EN.key;
    }

    private getChangeLogUrl() {
        if (this.platFormService.getIsApp() || environment.backend == "OpenEMS Edge") {
            return environment.api.CHANGELOG.REMOTE;
        }

        return environment.api.CHANGELOG.LOCAL;
    }
}

type ChangelogJson = {
    version: string,
    entries: {
        [lang: string]: Array<string | { roleIsAtLeast: string, text: string }>,
    }
};
