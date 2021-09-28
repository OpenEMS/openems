# State-Machine

```mermaid
graph TD
NO_LIMIT["NO_LIMIT<br>SoC > 21 %<br>No Limit<br>[infinity, infinity]<br>1 %p/sec"] -->|discharge, SoC <= 21 %| ABOVE_RESERVE_SOC["ABOVE_RESERVE_SOC<br>[infinity, max(50 % mAP, DC-PV)]<br>1 %p/sec"]

ABOVE_RESERVE_SOC -->|discharge, SoC <= 20 %| AT_RESERVE_SOC["AT_RESERVE_SOC<br>[infinity, max(0, DC-PV)]<br>1 %p/sec"]

AT_RESERVE_SOC -->|discharge, SoC < 20| UNDER_RESERVE_SOC["UNDER_RESERVE_SOC<br>[infinity, 0]<br>5 %p/sec"]

UNDER_RESERVE_SOC -->|discharge, SoC <= 16| FORCE_CHARGE["FORCE_CHARGE<br>Force-Charge with AC-PV<br>[infinity, AC-PV * -1]<br>1 %p/sec"]

FORCE_CHARGE -->|charge, SoC >= 20| AT_RESERVE_SOC

AT_RESERVE_SOC -->|charge, SoC > 20| ABOVE_RESERVE_SOC

ABOVE_RESERVE_SOC -->|charge, SoC > 21| NO_LIMIT

DESCRIPTION["Example ReserveSoc = 20%<br>ReserveSoc in [5;100]<br>%p = Percentage Point<br>mAP = MaxApparentPower<br>Ramp increase/decrease by 1 %p/sec of MaxApparentPower"]
```

View using Mermaid, e.g. https://mermaid-js.github.io/mermaid-live-editor