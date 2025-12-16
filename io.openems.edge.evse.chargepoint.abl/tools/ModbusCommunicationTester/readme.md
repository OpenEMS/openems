# Modbus Communication Tester
to test the communication with the (physical) ABL eMH1 device with Modbus/ASCII via a RS-485 interface at the computer's serial port

# Open Questions (to ABL)

### Warum manchmal keine Antwort, erst wenn man noch mal schickt?

### 

# How to run the script `test-modbus-communication.py`

## in VScode (workspace `SharedWithMe\ems\`)
* make sure, the directory `.venv` exists in `SharedWithMe\ems\` (it contains the needed python environment with the required python modules)
* open `SharedWithMe\ems\abl_emh1-cs\tools\test-modbus-communication.py` in the editor
* press the `Run`-Button (looks like `>`) in the upper right corner of the script
    * runs 
    ```
     & C:/Users/User/sync/portknox_jk/SharedWithMe/ems/.venv/Scripts/python.exe c:/Users/User/sync/portknox_jk/SharedWithMe/ems/abl_emh1-cs/tools/test-modbus-communication.py
    ```
* enjoy the running script in the terminal below

### in the terminal directly for COM3, DeviceID=1
```
    & C:/Users/User/sync/portknox_jk/SharedWithMe/ems/.venv/Scripts/python.exe c:/Users/User/sync/portknox_jk/SharedWithMe/ems/abl_emh1-cs/tools/test-modbus-communication.py COM3 1 
```
### To log interaction in a file (does not do the trick :-( )

Start-Transcript is a PowerShell cmdlet — it won’t appear in VS Code’s Command Palette. Run it inside a PowerShell terminal. Steps to record an interactive session in the integrated terminal:

1. Open an integrated PowerShell terminal in VS Code
    * Terminal → New Terminal (or Ctrl+`)
    * If the terminal is not PowerShell, click the dropdown (˅) → Select Default Profile → PowerShell → New Terminal

2. Change to the script folder and start the transcript:
```ps1
cd "c:\Users\User\sync\portknox_jk\SharedWithMe\ems\abl_emh1-cs\tools"
Start-Transcript -Path .\test-modbus-communication.log -Force
```

3. Run the script (example COM3, device ID 1 using the workspace venv python):
```ps1
& 'C:\Users\User\sync\portknox_jk\SharedWithMe\ems\.venv\Scripts\python.exe' .\test-modbus-communication.py COM3 1
```

4. When finished stop the transcript to flush and close the log:
```ps1
Stop-Transcript
```

Notes:
* Start-Transcript records both what you type and what the script prints (preferred for interactive sessions).
* If activating the venv requires running Activate.ps1 and ExecutionPolicy blocks it, run PowerShell as admin or temporarily allow script execution: Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass.

### Alternatives (no PowerShell transcript)
#### only stdout logging (easiest & **prefered mode of doing**)
If Start-Transcript is not available in your PowerShell (rare), use this alternative (captures stdout/stderr but may break some interactive prompts):
(no problem for this script, as all input COM messages are shown in the output again)
```
& 'C:\Users\User\sync\portknox_jk\SharedWithMe\ems\.venv\Scripts\python.exe' .\test-modbus-communication.py COM3 1 2>&1 | Tee-Object -FilePath .\test-modbus-communication.log
```

or other variant for the same:

If you prefer only stdout logging and don't need to capture stdin, you can run: 
```
python .\test-modbus-communication.py COM3 1 2>&1 | Tee-Object -FilePath .\out.log 
```
(this may interfere with interactive prompts).

#### other VScode features: 
* Alternative VS Code command to record terminal sessions: `Terminal: Record Terminal Session` (workbench.action.terminal.recordSession).

or

* use VS Code’s built-in terminal recorder: 
`Open Command Palette` (SHIFT+CTRL+P)
```
>Developer: Record Terminal Session
```

## in Windows PowerShell (if permissions to run scripts are provided)

Activate the workspace virtualenv:
```
& 'C:\Users\User\sync\portknox_jk\SharedWithMe\ems\.venv\Scripts\Activate.ps1'
```
Run the script (replace COM6 and 1 with your COM port and device ID):
```
python .\abl_emh1-cs\tools\test-modbus-communication.py COM6 1
```
Or call the venv python directly:
```
'C:/Users/User/sync/portknox_jk/SharedWithMe/ems/.venv/Scripts/python.exe' .\abl_emh1-cs\tools\test-modbus-communication.py COM6 1
```
