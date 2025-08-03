// @ts-strict-ignore
import { Directive, ElementRef, OnInit, inject } from "@angular/core";
import { Capacitor } from "@capacitor/core";
import { Logger } from "../shared";

@Directive({
  selector: "[appAutofill]",
  standalone: false,
})
export class AutofillDirective implements OnInit {
  private el = inject(ElementRef);
  private logger = inject(Logger);

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);


  constructor() { }

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
