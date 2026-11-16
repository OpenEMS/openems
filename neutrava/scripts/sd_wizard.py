#!/usr/bin/env python3
import hashlib
import json
import lzma
import os
from pathlib import Path
import plistlib
import queue
import re
import secrets
import shlex
import shutil
import subprocess
import sys
import tempfile
import threading
import tkinter as tk
from tkinter import filedialog, messagebox, ttk
import zipfile

ROOT = Path(__file__).resolve().parents[2]
CACHE = Path.home() / 'Library/Caches/neutrava-flasher'
INDEX = 'https://downloads.raspberrypi.com/raspios_lite_arm64/images/'


def run(*args):
    return subprocess.check_output(args, stderr=subprocess.PIPE)


def disk_info(device):
    return plistlib.loads(run('diskutil', 'info', '-plist', device))


def safe_disk(info):
    return (bool(re.fullmatch(r'disk[1-9][0-9]*', info.get('DeviceIdentifier', '')))
            and info.get('WholeDisk') is True and not info.get('VirtualOrPhysical') == 'Virtual'
            and info.get('Writable') is True
            and (info.get('BusProtocol') == 'Secure Digital' or
                 (info.get('Internal') is False and info.get('RemovableMedia') is True))
            and 4_000_000_000 <= info.get('TotalSize', 0) <= 256_000_000_000)


def fingerprint(info):
    return {k: info.get(k) for k in ('DeviceIdentifier', 'TotalSize', 'MediaName',
                                    'BusProtocol', 'DeviceTreePath', 'DiskUUID')}


def fleet_rpc(username, password, method, params):
    payload = json.dumps([username, password, method, params])
    code = '''import json, websocket, uuid
u, p, method, params = json.loads(PAYLOAD)
ws = websocket.create_connection("ws://127.0.0.1:8082", timeout=30)
def rpc(method, params):
    rid = str(uuid.uuid4())
    ws.send(json.dumps(dict(jsonrpc="2.0", id=rid, method=method, params=params)))
    for _ in range(100):
        response = json.loads(ws.recv())
        if response.get("id") == rid:
            if "error" in response:
                raise RuntimeError("Backend-Anfrage abgelehnt: " + method)
            return response["result"]
    raise RuntimeError("Keine passende Backend-Antwort")
rpc("authenticateWithPassword", dict(username=u, password=p))
print(json.dumps(rpc(method, params)))
ws.close()
'''.replace('PAYLOAD', repr(payload))
    result = subprocess.run(['ssh', '-o', 'BatchMode=yes', '-o', 'ConnectTimeout=10',
                             '-i', str(Path.home() / '.ssh/id_ed25519_voltera'),
                             'root@147.93.87.65', 'python3'], input=code,
                            capture_output=True, text=True, timeout=90)
    if result.returncode:
        raise RuntimeError('Flottenzugriff fehlgeschlagen. SSH-Zugang und Admin-Login prüfen.')
    return json.loads(result.stdout)


def fetch_image(log):
    CACHE.mkdir(parents=True, exist_ok=True, mode=0o700)
    dates = re.findall(r'href="(raspios_lite_arm64-\d{4}-\d{2}-\d{2}/)"',
                       run('/usr/bin/curl', '-fsSL', '--max-time', '60', INDEX).decode())
    folder = INDEX + sorted(set(dates))[-1]
    names = re.findall(r'href="([^"/]+\.img\.xz)"',
                       run('/usr/bin/curl', '-fsSL', '--max-time', '60', folder).decode())
    name = sorted(set(names))[-1]
    if not re.fullmatch(r'[a-zA-Z0-9._-]+', name):
        raise ValueError('Ungültiger Image-Name')
    expected = run('/usr/bin/curl', '-fsSL', '--max-time', '60', folder + name + '.sha256').decode().split()[0]
    if not re.fullmatch(r'[a-fA-F0-9]{64}', expected):
        raise ValueError('Ungültige Prüfsumme')
    target = CACHE / name
    if not target.exists():
        log('Lade offizielles Raspberry Pi OS Lite (64 Bit) …')
        partial = target.with_suffix('.partial')
        run('/usr/bin/curl', '-fL', '--retry', '2', '--max-time', '1800', '-o', str(partial), folder + name)
        partial.replace(target)
    with target.open('rb') as stream:
        digest = hashlib.file_digest(stream, 'sha256').hexdigest()
    if digest.lower() != expected.lower():
        target.unlink()
        raise ValueError('Image-Prüfsumme falsch. Bitte erneut versuchen.')
    return target


def prepare(image, jar, edge, key, login_password, work, log):
    output = work / 'raspios.img'
    log('Entpacke Image …')
    with lzma.open(image, 'rb') as source, output.open('wb') as dest:
        shutil.copyfileobj(source, dest)
    attached = plistlib.loads(run('hdiutil', 'attach', '-nomount', '-plist', str(output)))
    devices = [x['dev-entry'] for x in attached['system-entities'] if 'dev-entry' in x]
    disk = re.match(r'/dev/disk[0-9]+', devices[0]).group(0)
    mount = work / 'boot'
    mount.mkdir()
    try:
        run('diskutil', 'mount', '-mountPoint', str(mount), disk + 's1')
        if not (mount / 'user-data').exists():
            raise ValueError('Dieses Image unterstützt kein cloud-init. Aktuelles Raspberry Pi OS Lite verwenden.')
        payload = mount / 'neutrava'
        payload.mkdir()
        shutil.copyfile(jar, payload / 'openems-edge.jar')
        backend = '\n'.join(['apikey=' + json.dumps(key), 'service.pid="Controller.Api.Backend"',
                              'uri="wss://ems.neutrava.ch/openems-edge"', ''])
        service = '\n'.join(['[Unit]', 'Description=OpenEMS Edge', 'After=network-online.target',
                             'Wants=network-online.target', '[Service]', 'User=openems',
                             'WorkingDirectory=/home/openems/openems',
                             'ExecStart=/usr/bin/java -Dfelix.cm.dir=/home/openems/openems/config -Dopenems.data.dir=/home/openems/openems/data -Xms256m -Xmx512m -jar /home/openems/openems/openems-edge.jar',
                             'Restart=always', 'RestartSec=10', '[Install]', 'WantedBy=multi-user.target', ''])
        setup_service = '\n'.join(['[Unit]', 'Description=neutrava first boot setup',
                                   'After=network-online.target', 'Wants=network-online.target',
                                   'ConditionPathExists=!/var/lib/neutrava-ready', '[Service]',
                                   'Type=oneshot', 'ExecStart=/usr/local/sbin/neutrava-install',
                                   'TimeoutStartSec=0', 'Restart=on-failure', 'RestartSec=60',
                                   '[Install]', 'WantedBy=multi-user.target', ''])
        install = '\n'.join(['#!/bin/bash', 'set -euo pipefail',
                             'export DEBIAN_FRONTEND=noninteractive',
                             'apt-get update', 'apt-get install -y openjdk-21-jre-headless avahi-daemon',
                             'install -d -o openems -g openems /home/openems/openems/{config/Controller/Api,data}',
                             'install -o openems -g openems /boot/firmware/neutrava/openems-edge.jar /home/openems/openems/openems-edge.jar',
                             'chown -R openems:openems /home/openems/openems',
                             'systemctl daemon-reload', 'systemctl enable --now openems-edge',
                             'touch /var/lib/neutrava-ready', ''])
        hashed = subprocess.run(['openssl', 'passwd', '-6', '-stdin'], input=login_password + '\n',
                                capture_output=True, text=True, check=True).stdout.strip()
        config = {'hostname': 'neutrava-' + edge, 'manage_etc_hosts': True,
                  'users': [{'name': 'openems', 'groups': ['sudo'], 'shell': '/bin/bash',
                             'sudo': ['ALL=(ALL) ALL'], 'lock_passwd': False, 'passwd': hashed}],
                  'ssh_pwauth': True,
                  'write_files': [
                      {'path': '/home/openems/openems/config/Controller/Api/Backend.config',
                       'permissions': '0600', 'content': backend, 'defer': True, 'owner': 'openems:openems'},
                      {'path': '/home/openems/openems/config/Scheduler/AllAlphabetically.config',
                       'permissions': '0600', 'content': 'cycleTime=I"1000"\nservice.pid="Scheduler.AllAlphabetically"\n',
                       'defer': True, 'owner': 'openems:openems'},
                      {'path': '/etc/systemd/system/openems-edge.service', 'content': service},
                      {'path': '/etc/systemd/system/neutrava-setup.service', 'content': setup_service},
                      {'path': '/usr/local/sbin/neutrava-install', 'permissions': '0700', 'content': install}],
                  'runcmd': [['systemctl', 'daemon-reload'],
                             ['systemctl', 'enable', '--now', 'neutrava-setup.service']]}
        (mount / 'user-data').write_text('#cloud-config\n' + json.dumps(config, indent=2) + '\n')
        (mount / 'meta-data').write_text(json.dumps({'instance-id': secrets.token_hex(16), 'local-hostname': 'neutrava-' + edge}))
        (mount / 'network-config').write_text(json.dumps({'version': 2, 'ethernets': {'eth0': {'dhcp4': True, 'optional': False}}}))
    finally:
        run('hdiutil', 'detach', disk)
    return output


def privileged_flash(image, device, expected):
    info = disk_info(device)
    if not safe_disk(info) or fingerprint(info) != expected:
        raise ValueError('SD-Karte hat sich geändert. Abbruch.')
    if image.stat().st_size > info['TotalSize']:
        raise ValueError('Image ist grösser als SD-Karte.')
    run('diskutil', 'unmountDisk', device)
    raw = '/dev/r' + info['DeviceIdentifier']
    subprocess.run(['dd', 'if=' + str(image), 'of=' + raw, 'bs=4m'], check=True)
    os.sync() if hasattr(os, 'sync') else subprocess.run(['sync'], check=True)
    with image.open('rb') as source, open(raw, 'rb') as target:
        while chunk := source.read(4 * 1024 * 1024):
            if target.read(len(chunk)) != chunk:
                raise ValueError('Lesekontrolle fehlgeschlagen! Karte nicht einsetzen.')
    run('diskutil', 'eject', device)


class Wizard:
    def __init__(self):
        self.root = tk.Tk()
        self.root.title('neutrava · SD-Karten Wizard')
        self.root.geometry('780x740')
        self.events = queue.Queue()
        self.busy = False
        self.fleet = None
        self.cards = {}
        self.locations = {'Ohne Standort': ''}
        self.users = {'Keine Kundenzuweisung': ''}
        self.values = {}
        frame = ttk.Frame(self.root, padding=24)
        frame.pack(fill='both', expand=True)
        ttk.Label(frame, text='Neuen OpenEMS Raspberry Pi vorbereiten', font=('', 19, 'bold')).pack(anchor='w')
        ttk.Label(frame, text='1 · Flotte laden    2 · Standort & SD-Karte wählen    3 · Flashen', padding=(0, 12)).pack(anchor='w')
        self.field(frame, 'Admin-Benutzer', 'admin', 'admin')
        self.field(frame, 'Admin-Passwort', 'password', secret=True)
        ttk.Button(frame, text='Flotte und SD-Karten laden', command=self.load).pack(anchor='w', pady=8)
        self.field(frame, 'SD-Karte (Inhalt wird vollständig gelöscht)', 'disk', choices=[])
        self.field(frame, 'Neue Edge-ID', 'edge', 'edge1')
        self.field(frame, 'Anlagenname / Kunde', 'name')
        self.field(frame, 'Standort', 'location', choices=list(self.locations))
        self.field(frame, 'Bestehender Kunde (OWNER)', 'user', choices=list(self.users))
        self.field(frame, 'Neues Pi-Login-Passwort (mindestens 12 Zeichen)', 'pi_password', secret=True)
        default_jar = next((str(p) for p in (CACHE / 'openems-edge.jar', Path('/tmp/openems-edge.jar')) if p.exists()), '')
        self.field(frame, 'OpenEMS Edge Runtime (.jar)', 'jar', default_jar)
        ttk.Button(frame, text='Runtime auswählen …', command=self.choose).pack(anchor='w', pady=4)
        ttk.Label(frame, text='LAN / DHCP · Aktuelles OS Lite wird geprüft heruntergeladen.\nErster Start benötigt Internet; Java und OpenEMS werden automatisch eingerichtet.').pack(anchor='w', pady=6)
        self.button = ttk.Button(frame, text='Weiter: Prüfen und Flashen …', command=self.start)
        self.button.pack(anchor='w', pady=10)
        self.progress = ttk.Progressbar(frame, mode='indeterminate')
        self.progress.pack(fill='x')
        self.status = tk.StringVar(value='Bereit. Kein Datenträger wird ohne Löschbestätigung beschrieben.')
        ttk.Label(frame, textvariable=self.status, wraplength=720).pack(anchor='w', pady=10)
        self.root.protocol('WM_DELETE_WINDOW', self.close)
        self.root.after(100, self.poll)

    def field(self, parent, label, key, default='', secret=False, choices=None):
        row = ttk.Frame(parent)
        row.pack(fill='x', pady=3)
        ttk.Label(row, text=label, width=44).pack(side='left')
        var = tk.StringVar(value=default)
        if choices is not None:
            widget = ttk.Combobox(row, textvariable=var, values=choices, state='readonly')
            if choices:
                var.set(choices[0])
        else:
            widget = ttk.Entry(row, textvariable=var, show='•' if secret else '')
        widget.pack(side='right', fill='x', expand=True)
        self.values[key] = var
        setattr(self, key + '_widget', widget)

    def choose(self):
        path = filedialog.askopenfilename(filetypes=[('OpenEMS Runtime', '*.jar')])
        if path:
            self.values['jar'].set(path)

    def log(self, text):
        self.events.put(('status', text))

    def task(self, fn):
        if self.busy:
            return
        self.busy = True
        self.button.state(['disabled'])
        self.progress.start()
        def worker():
            try:
                fn()
            except Exception as error:
                self.events.put(('error', str(error)))
            finally:
                self.events.put(('done', None))
        threading.Thread(target=worker, daemon=True).start()

    def poll(self):
        while not self.events.empty():
            kind, value = self.events.get()
            if kind == 'status':
                self.status.set(value)
            elif kind == 'error':
                self.status.set('Abgebrochen: ' + value)
                messagebox.showerror('Vorgang nicht abgeschlossen', value)
            elif kind == 'loaded':
                self.fleet, self.cards = value
                self.locations = {'Ohne Standort': '', **{x['name'] + ' [' + x['id'] + ']': x['id'] for x in self.fleet['locations']}}
                self.users = {'Keine Kundenzuweisung': '', **{x['username']: x['username'] for x in self.fleet['users'] if x.get('globalRole') != 'ADMIN'}}
                for key, mapping in [('disk', self.cards), ('location', self.locations), ('user', self.users)]:
                    self.__dict__[key + '_widget']['values'] = list(mapping)
                    self.values[key].set(next(iter(mapping), ''))
                used = {x['id'] for x in self.fleet['edges']}
                index = 0
                while 'edge' + str(index) in used:
                    index += 1
                self.values['edge'].set('edge' + str(index))
                self.status.set('Flotte geladen. ' + str(len(self.cards)) + ' geeignete SD-Karte(n).')
            elif kind == 'done':
                self.busy = False
                self.button.state(['!disabled'])
                self.progress.stop()
        self.root.after(100, self.poll)

    def load(self):
        username, password = self.values['admin'].get(), self.values['password'].get()
        def work():
            self.log('Lade Flotte und prüfe Datenträger …')
            fleet = fleet_rpc(username, password, 'neutravaAdminGetFleet', {})
            disks = plistlib.loads(run('diskutil', 'list', '-plist'))['AllDisksAndPartitions']
            cards = {}
            for disk in disks:
                info = disk_info(disk['DeviceIdentifier'])
                if safe_disk(info):
                    cards[f"{info['DeviceIdentifier']} · {info.get('MediaName', 'SD')} · {info['TotalSize'] / 1e9:.1f} GB"] = info
            self.events.put(('loaded', (fleet, cards)))
        self.task(work)

    def start(self):
        if self.busy:
            return
        v = {k: x.get() if k in ('password', 'pi_password') else x.get().strip()
             for k, x in self.values.items()}
        try:
            if not self.fleet or v['disk'] not in self.cards:
                raise ValueError('Bitte zuerst Flotte und SD-Karten laden.')
            if not re.fullmatch(r'edge[0-9]{1,6}', v['edge']) or not v['name']:
                raise ValueError('Edge-ID muss edge + Zahl sein; Anlagenname ist erforderlich.')
            if len(v['pi_password']) < 12:
                raise ValueError('Pi-Passwort muss mindestens 12 Zeichen enthalten.')
            with zipfile.ZipFile(v['jar']) as jar:
                if 'META-INF/MANIFEST.MF' not in jar.namelist() or 'Main-Class:' not in jar.read('META-INF/MANIFEST.MF').decode():
                    raise ValueError('Keine ausführbare OpenEMS Runtime.')
            info = self.cards[v['disk']]
            if not messagebox.askyesno('SD-Karte vollständig löschen?',
                                      f"ALLE Daten auf {v['disk']} werden unwiderruflich überschrieben.\n\nAnlage: {v['edge']} – {v['name']}\nStandort: {v['location']}\nKunde: {v['user']}\n\nNur fortfahren, wenn dies die richtige SD-Karte ist.\nDie Anlage wird erst nach erfolgreicher Lesekontrolle registriert."):
                return
        except Exception as error:
            messagebox.showerror('Eingaben prüfen', str(error))
            return
        def work():
            rpc = lambda method, params: fleet_rpc(v['admin'], v['password'], method, params)
            fleet = rpc('neutravaAdminGetFleet', {})
            if any(x['id'] == v['edge'] for x in fleet['edges']):
                raise ValueError('Edge-ID ist bereits belegt. Flotte neu laden.')
            key = secrets.token_urlsafe(32)
            image = fetch_image(self.log)
            if shutil.disk_usage(tempfile.gettempdir()).free < 8_000_000_000:
                raise ValueError('Mindestens 8 GB freier Speicher für das vorbereitete Image erforderlich.')
            cached_jar = CACHE / 'openems-edge.jar'
            if Path(v['jar']).resolve() != cached_jar.resolve():
                shutil.copyfile(v['jar'], cached_jar)
            with tempfile.TemporaryDirectory(prefix='neutrava-') as folder:
                workdir = Path(folder)
                prepared = prepare(image, v['jar'], v['edge'], key, v['pi_password'], workdir, self.log)
                self.log('macOS fragt nach Administratorfreigabe. Schreibe und verifiziere SD-Karte …')
                command = shlex.join([sys.executable, str(Path(__file__).resolve()), '--flash', str(prepared),
                                      info['DeviceIdentifier'], json.dumps(fingerprint(info))])
                script = 'do shell script ' + json.dumps(command, ensure_ascii=False) + ' with administrator privileges'
                subprocess.run(['osascript', '-e', script], check=True, capture_output=True)
            self.log('SD-Karte geprüft und ausgeworfen. Registriere Anlage …')
            latest = rpc('neutravaAdminGetFleet', {})
            if any(x['id'] == v['edge'] for x in latest['edges']):
                raise ValueError('ID inzwischen belegt. Karte NICHT booten; mit neuer ID erneut flashen.')
            try:
                rpc('neutravaAdminSetEdge', {'edgeId': v['edge'], 'apikey': key,
                    'setuppassword': secrets.token_urlsafe(24), 'comment': v['name'],
                    'locationId': self.locations[v['location']]})
                user = self.users[v['user']]
                if user:
                    current = next(x for x in latest['users'] if x['username'] == user)
                    rpc('neutravaAdminSetUser', {'username': user, 'edges': {**current.get('edges', {}), v['edge']: 'OWNER'}})
            except Exception:
                raise RuntimeError('Karte geflasht, Cloud-Zuweisung aber nicht vollständig. Nicht einsetzen; Registrierung im Portal prüfen und danach neu flashen.') from None
            self.log('Fertig: SD-Karte verifiziert, ausgeworfen und ' + v['edge'] + ' zugewiesen. In Pi einsetzen, LAN und Strom anschliessen. Erster Boot installiert Software; Online-Status anschliessend im Portal prüfen.')
        self.task(work)

    def close(self):
        if self.busy:
            messagebox.showinfo('Vorgang läuft', 'Bitte bis zum Abschluss warten. Karte nicht entfernen.')
        else:
            self.root.destroy()


if __name__ == '__main__':
    if len(sys.argv) == 5 and sys.argv[1] == '--flash':
        privileged_flash(Path(sys.argv[2]), sys.argv[3], json.loads(sys.argv[4]))
    else:
        Wizard().root.mainloop()
