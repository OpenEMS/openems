import requests


class ApacheFelixClient:
    def __init__(self, base_url: str, username: str, password: str):
        self.base_url = base_url
        self.auth = (username, password)

    def get_bundle_id(self, symbolic_name: str) -> str:
        response = requests.get(f"{self.base_url}/system/console/bundles.json", auth=self.auth)
        response.raise_for_status()
        bundles = response.json().get("data", [])
        # Iterate over the bundles and look for the desired symbolic name
        for bundle in bundles:
            if bundle.get("symbolicName") == symbolic_name:
                return bundle.get("id")  # Return the bundle ID if found

    def _restart_bundle(self, bundle_id: str):
        self._execute_bundle_action(bundle_id=bundle_id, action="stop")
        self._execute_bundle_action(bundle_id=bundle_id, action="start")

    def _execute_bundle_action(self, bundle_id: str, action: str) -> None:
        response = requests.post(
            f"{self.base_url}/system/console/bundles/{bundle_id}",
            auth=self.auth,
            data={"action": action},
        )
        response.raise_for_status()

    def refresh_config_directory(self) -> None:
        """
        If files in the config directory were updated, this method forces the application to refresh by stopping and
        starting the `org.apache.felix.configadmin` bundle
        """
        bundle_id = self.get_bundle_id("org.apache.felix.configadmin")
        self._restart_bundle(bundle_id)
