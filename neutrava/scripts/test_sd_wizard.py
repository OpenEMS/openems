import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import sd_wizard as wizard


class SafetyTests(unittest.TestCase):
    def setUp(self):
        self.card = dict(DeviceIdentifier='disk4', WholeDisk=True, Writable=True,
                         VirtualOrPhysical='Physical', BusProtocol='Secure Digital',
                         Internal=True, RemovableMedia=True, TotalSize=32_000_000_000)

    def test_builtin_sd_reader_allowed(self):
        self.assertTrue(wizard.safe_disk(self.card))

    def test_system_and_virtual_disks_rejected(self):
        for changes in [dict(DeviceIdentifier='disk0'), dict(WholeDisk=False),
                        dict(Writable=False), dict(VirtualOrPhysical='Virtual'),
                        dict(BusProtocol='Apple Fabric', RemovableMedia=False),
                        dict(TotalSize=1_000_000_000_000), dict(DeviceIdentifier='disk4;reboot')]:
            with self.subTest(changes=changes):
                self.assertFalse(wizard.safe_disk({**self.card, **changes}))

    def test_changed_card_rejected_before_unmount(self):
        with patch.object(wizard, 'disk_info', return_value=self.card), patch.object(wizard, 'run') as run:
            with self.assertRaises(ValueError):
                wizard.privileged_flash(Path('/unused'), 'disk4', {})
            run.assert_not_called()

    def test_oversized_image_rejected_before_unmount(self):
        with patch.object(wizard, 'disk_info', return_value=self.card), patch.object(wizard, 'run') as run:
            image = unittest.mock.Mock()
            image.stat.return_value.st_size = self.card['TotalSize'] + 1
            with self.assertRaises(ValueError):
                wizard.privileged_flash(image, 'disk4', wizard.fingerprint(self.card))
            run.assert_not_called()

    def test_rpc_checks_correlated_response_and_no_password_argument(self):
        result = unittest.mock.Mock(returncode=0, stdout='{"edges": []}')
        with patch.object(wizard.subprocess, 'run', return_value=result) as remote:
            self.assertEqual(wizard.fleet_rpc('admin', 'not-in-argv', 'neutravaAdminGetFleet', {}), {'edges': []})
            args, kwargs = remote.call_args
            self.assertNotIn('not-in-argv', str(args))
            self.assertIn('response.get("id") == rid', kwargs['input'])
            self.assertNotIn('StrictHostKeyChecking=no', str(args))

    def test_prepare_cloud_config_has_unique_identity_and_no_pilot_devices(self):
        import lzma
        import plistlib
        with tempfile.TemporaryDirectory() as directory:
            work = Path(directory)
            image = work / 'test.xz'
            image.write_bytes(lzma.compress(b'test-image'))
            jar = work / 'edge.jar'
            jar.write_bytes(b'jar')
            config = None
            def fake_run(*args):
                nonlocal config
                if args[:2] == ('hdiutil', 'attach'):
                    return plistlib.dumps({'system-entities': [{'dev-entry': '/dev/disk9s1'}, {'dev-entry': '/dev/disk9'}]})
                if args[:2] == ('diskutil', 'mount'):
                    (work / 'boot/user-data').touch()
                if args[:2] == ('hdiutil', 'detach'):
                    config = json.loads((work / 'boot/user-data').read_text().split('\n', 1)[1])
                return b''
            with patch.object(wizard, 'run', side_effect=fake_run):
                wizard.prepare(image, jar, 'edge7', 'random-api-key', 'test-password-long', work, lambda _: None)
            self.assertEqual(config['hostname'], 'neutrava-edge7')
            files = {entry['path']: entry['content'] for entry in config['write_files']}
            self.assertIn('openjdk-21-jre-headless', files['/usr/local/sbin/neutrava-install'])
            self.assertIn('Restart=on-failure', files['/etc/systemd/system/neutrava-setup.service'])
            self.assertIn('Scheduler.AllAlphabetically', json.dumps(config))
            subprocess_result = wizard.subprocess.run(['bash', '-n'], input=files['/usr/local/sbin/neutrava-install'], text=True)
            self.assertEqual(subprocess_result.returncode, 0)
            self.assertNotIn('test-password-long', json.dumps(config))
            self.assertIn('random-api-key', config['write_files'][0]['content'])
            self.assertNotIn('Smartfox', json.dumps(config))
            self.assertTrue(config['runcmd'])


if __name__ == '__main__':
    unittest.main()
