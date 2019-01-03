import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder } from '@angular/forms';
import { Subscription } from 'rxjs';

import { Edge } from '../../../shared/edge/edge';
import { Service } from '../../../shared/service/service';

@Component({
  selector: 'index',
  templateUrl: './index.component.html'
})
export class IndexComponent implements OnInit {

  public edge: Edge;

  private edgeSubscription: Subscription;

  constructor(
    private route: ActivatedRoute,
    private formBuilder: FormBuilder,
    private service: Service,
  ) {
  }

  ngOnInit() {
    this.service.setCurrentEdge(this.route);
  }
}