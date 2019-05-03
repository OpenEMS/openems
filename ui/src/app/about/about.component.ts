import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Edge, Service } from '../shared/shared';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'about',
  templateUrl: './about.component.html'
})
export class AboutComponent {

  public edge: Edge = null

  constructor(
    public translate: TranslateService,
    private route: ActivatedRoute,
    private service: Service,
  ) { }

  ngOnInit() {
    this.service.setCurrentPage(this.translate.instant('Menu.AboutUI'), this.route).then(edge => {
      this.edge = edge
    });
  }

}
