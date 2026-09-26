package io.openems.edge.bridge.eebus;

import io.openems.common.test.DummyConfigurationAdmin;
import org.junit.Test;
import org.openmuc.jeebus.ship.api.cert.CertificateStorage;
import org.openmuc.jeebus.ship.api.cert.CertificateStoreException;
import org.openmuc.jeebus.ship.node.KeyManagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigCertificateStorageTest {

	@Test
	public void testStore() throws Exception {
		var cm = new DummyConfigurationAdmin();
		var store = new ConfigCertificateStorage(cm, "test.pid");

		assertTrue(store.readCertificate().isEmpty());
		this.addCertificateToStore(store);
		assertTrue(store.readCertificate().isPresent());
		var cert = store.readCertificate().get();

		var secondStore = new ConfigCertificateStorage(cm, "test.pid");
		assertTrue(secondStore.readCertificate().isPresent());

		var secondCert = store.readCertificate().get();
		assertEquals(cert.certificate, secondCert.certificate);
		assertEquals(cert.privateKey, secondCert.privateKey);
	}

	private void addCertificateToStore(CertificateStorage storage) throws CertificateStoreException {
		new KeyManagement(storage, "CN=Test", "dummy", 365);
	}

}
