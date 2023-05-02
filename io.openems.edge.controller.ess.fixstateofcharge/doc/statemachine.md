# State-Machine

```mermaid
graph TD
UNDEFINED -->|activate| IDLE
IDLE --> |enable predefinedCharge| NOT_STARTED

NOT_STARTED["NOT_STARTED<br>LeftTime > RequiredTime + Buffer<br>[infinity, infinity]"] -->|LeftTime <= RequiredTime + Buffer, SoC > 32 %| ABOVE_TARGET_SOC
NOT_STARTED -->|LeftTime <= RequiredTime + Buffer, SoC < 28 %| UNDER_TARGET_SOC

UNDER_TARGET_SOC["UNDER_TARGET_SOC <br>[inifinity, max(Min(50 % mAP, 1/3 capacity), DC-PV)]<br>0.05 %p/sec"] -->|discharge, 28 % <= soc < 30 %| WITHIN_LOWER_TARGET_SOC_BOUNDARIES
ABOVE_TARGET_SOC["ABOVE_TARGET_SOC <br>[Min(50 % mAP, 1/3 capacity), infinity]<br>0.05 %p/sec"] -->|charge, 30 % < soc <= 32 %| WITHIN_UPPER_TARGET_SOC_BOUNDARIES

WITHIN_UPPER_TARGET_SOC_BOUNDARIES["WITHIN_UPPER_TARGET_SOC_BOUNDARIES <br>[Min(25 % mAP, 1/6 capacity), infinity]<br>0.05 %p/sec"] -->|discharge, SoC <= 30%| AT_TARGET_SOC["AT_TARGET_SOC <br>[0, max(0, DC-PV)] <br>0.1 %p/sec"]

WITHIN_LOWER_TARGET_SOC_BOUNDARIES["WITHIN_LOWER_TARGET_SOC_BOUNDARIES <br>[inifinity, max(Min(25 % mAP, 1/6 capacity), DC-PV)]<br>0.05 %p/sec"] -->|charge, SoC >= 30| AT_TARGET_SOC

AT_TARGET_SOC --> |fallback timer not reached| AT_TARGET_SOC
AT_TARGET_SOC --> |disable predefinedCharge| IDLE
AT_TARGET_SOC --> |fallback time passed| IDLE


DESCRIPTION["Example TargetSoc = 30%<br>%p = Percentage Point<br>mAP = MaxApparentPower<br>Ramp increase/decrease by 0.05 %p/sec of MaxApparentPower<br>[max charge, max discharge]<br><br>The self-discharge can be ignored due to the target time <br> and the fallback after a few hours.<br>"]
DESCRIPTION2["For optical reasons not every link was added. <br><br> AT_TARGET_SOC SoC could return to WITHIN_xxx_TARGET_SOC_BOUNDARIES <br>if the soc is dropping or rising by 1% <br><br>NOT_STARTED could skip the ABOVE/UPPER_TARGET_SOC State <br>if the SoC is already in the boundaries or at the target"] 

```

View using Mermaid, e.g. https://mermaid-js.github.io/mermaid-live-editor