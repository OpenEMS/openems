import { Component, EventEmitter, Output } from "@angular/core";

@Component({
    selector: 'fenecon_Home',
    templateUrl: './feneconHome.html'
})
export class FeneconHomeComponent {
    public title: string = 'Fenecon Home';
    @Output() public titleEvent: EventEmitter<String> = new EventEmitter<String>();

    updateParentTitle() {
        this.titleEvent.emit(this.title);
    }
    ngOnInit() {
        this.updateParentTitle();
    }
}