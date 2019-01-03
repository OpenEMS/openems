import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormGroup, FormBuilder } from '@angular/forms';
import { filter, first } from 'rxjs/operators';

import { Edge } from '../../../../shared/edge/edge';
import { Service } from '../../../../shared/service/service';

@Component({
  selector: 'more',
  templateUrl: './more.component.html'
})
export class MoreComponent implements OnInit {

  public edge: Edge;
  public manualMessageForm: FormGroup;

  constructor(
    private route: ActivatedRoute,
    private service: Service,
    private formBuilder: FormBuilder
  ) { }

  ngOnInit() {
    this.service.setCurrentEdge(this.route)
    // .pipe(filter(edge => edge != null),
    //   first())
    // .subscribe(edge => {
    //   this.edge = edge;
    // });
    this.manualMessageForm = this.formBuilder.group({
      "message": this.formBuilder.control('')
    });
  }
}