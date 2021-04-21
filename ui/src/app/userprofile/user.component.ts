import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { environment } from '../../environments';
import { LogoutRequest } from '../shared/jsonrpc/request/logoutRequest';
import { Service, Websocket } from '../shared/shared';
import { Language, LanguageTag } from '../shared/translate/language';

@Component({
  selector: 'user',
  templateUrl: './user.component.html'
})
export class UserComponent {

  public env = environment;

  public readonly languages: LanguageTag[];
  public currentLanguage: LanguageTag;

  constructor(
    public translate: TranslateService,
    private route: ActivatedRoute,
    private service: Service,
    private websocket: Websocket,
  ) {
    this.languages = Language.getLanguageTags();
  }

  ngOnInit() {
    this.currentLanguage = this.translate.currentLang as LanguageTag;
    this.service.setCurrentComponent(this.translate.instant('Menu.user'), this.route);
  }

  /**
   * Logout from OpenEMS Edge or Backend.
   */
  public doLogout() {
    this.websocket.sendRequest(new LogoutRequest()).then(response => {
      this.service.handleLogout();
    }).catch(reason => {
      console.error(reason)
    })
  }

  public toggleDebugMode(event: CustomEvent) {
    this.env.debugMode = event.detail['checked'];
  }

  public setLanguage(language: LanguageTag): void {
    this.currentLanguage = language;
    this.translate.use(language);
  }
}
