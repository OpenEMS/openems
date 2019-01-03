import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Service } from '../../../../../shared/service/service';
import { Utils } from '../../../../../shared/service/utils';

@Component({
  selector: 'rawconfig',
  templateUrl: './rawconfig.component.html'
})
export class RawConfigComponent {

  constructor(
    public service: Service,
    private route: ActivatedRoute,
    public utils: Utils
  ) { }

  ngOnInit() {
    this.service.setCurrentEdge(this.route);
  }
}