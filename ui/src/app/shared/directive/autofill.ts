// @ts-strict-ignore
import { Directive, ElementRef, OnInit } from "@angular/core";
import { Capacitor } from "@capacitor/core";
import { Logger } from "../shared";

@Directive({
  selector: "[appAutofill]",
})
export class AutofillDirective implements OnInit {

  constructor(private el: ElementRef, private logger: Logger) { }

  ngOnInit(): void {
    if (Capacitor.getPlatform() !== "ios") { return; }
    setTimeout(() => {
      try {
        this.el.nativeElement.children[0].addEventListener("change", (e) => {
          this.el.nativeElement.value = (e.target as any).value;
        });
      } catch {
        console.error("Android Autofill Directive inactive");
      }
    }, 100); // Need some time for the ion-input to create the input element
  }
}
