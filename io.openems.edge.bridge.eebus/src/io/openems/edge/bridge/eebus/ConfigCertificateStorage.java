package io.openems.edge.bridge.eebus;

import org.openmuc.jeebus.ship.api.cert.CertificateInfo;
import org.openmuc.jeebus.ship.api.cert.CertificateStorage;
import org.openmuc.jeebus.ship.api.cert.CertificateStoreException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

public class ConfigCertificateStorage implements CertificateStorage {
	private final ConfigurationAdmin cm;
	private final String servicePid;

	private static final String TLS_CERTIFICATE_CONFIG_KEY = "tlsCertificate";
	private static final byte[] CERT_HEADER = "--OPENEMS-TLS-CERT-V1--".getBytes(StandardCharsets.UTF_8);

	public ConfigCertificateStorage(ConfigurationAdmin cm, String servicePid) {
		this.cm = cm;
		this.servicePid = servicePid;
	}

	private Configuration readConfiguration() throws IOException {
		return this.cm.getConfiguration(this.servicePid);
	}

	private Optional<CertificateInfo> decodeCertificateInfo(String storedStr) throws CertificateStoreException {
		if (storedStr == null || storedStr.isBlank()) {
			return Optional.empty();
		}

		var bytes = Base64.getDecoder().decode(storedStr);
		try (var reader = new DataInputStream(new ByteArrayInputStream(bytes))) {
			return Optional.of(this.decodeCertificateInfo(reader));
		} catch (IOException ex) {
			throw new CertificateStoreException("Invalid certificate data: Read failure", ex);
		}
	}

	private CertificateInfo decodeCertificateInfo(DataInputStream reader) throws IOException, CertificateStoreException {
		var header = reader.readNBytes(CERT_HEADER.length);
		if (!Arrays.equals(header, CERT_HEADER)) {
			throw new CertificateStoreException("Invalid certificate data: too short", null);
		}

		var privateKeyLen = reader.readInt();
		var privateKeyBytes = reader.readNBytes(privateKeyLen);

		var certificateLen = reader.readInt();
		var certificateBytes = reader.readNBytes(certificateLen);

		var privateKey = this.decodePrivateKey(privateKeyBytes);
		var x509certificate = this.decodeX509Certificate(certificateBytes);

		return new CertificateInfo(privateKey, x509certificate);
	}

	/**
	 * Decodes a PKCS#8 DER-encoded private key (EC algorithm).
	 *
	 * @param bytes the PKCS#8 encoded key bytes
	 * @return the decoded {@link PrivateKey}
	 * @throws CertificateStoreException if the key cannot be decoded
	 */
	private PrivateKey decodePrivateKey(byte[] bytes) throws CertificateStoreException {
		try (var reader = new DataInputStream(new ByteArrayInputStream(bytes))) {
			var algorithmLength = reader.readInt();
			var algorithm = new String(reader.readNBytes(algorithmLength), StandardCharsets.UTF_8);

			var privateKeyBytes = reader.readAllBytes();
			var keySpec = new PKCS8EncodedKeySpec(privateKeyBytes, algorithm);

			var keyFactory = KeyFactory.getInstance(algorithm);
			return keyFactory.generatePrivate(keySpec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException ex) {
			throw new CertificateStoreException("Invalid certificate data: failed to decode private key", ex);
		}
	}

	private X509Certificate decodeX509Certificate(byte[] bytes) throws CertificateStoreException {
		try {
			var certFactory = CertificateFactory.getInstance("X.509");
			return (java.security.cert.X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(bytes));
		} catch (CertificateException e) {
			throw new CertificateStoreException("Invalid certificate data: failed to decode X.509 certificate", e);
		}
	}
	
	private String encodeCertificateInfoToString(CertificateInfo certificateInfo) throws IOException, CertificateEncodingException {
		if (certificateInfo == null) {
			return null;
		}

		return Base64.getEncoder().encodeToString(this.encodeCertificateInfo(certificateInfo));
	}

	private byte[] encodeCertificateInfo(CertificateInfo certificateInfo) throws IOException, CertificateEncodingException {
		var encodedPrivateKey = this.encodePrivateKey(certificateInfo.privateKey);
		var encodedX509Cert = certificateInfo.certificate.getEncoded();

		try (var out = new ByteArrayOutputStream()) {
			try (var writer = new DataOutputStream(out)) {
				writer.write(CERT_HEADER);

				writer.writeInt(encodedPrivateKey.length);
				writer.write(encodedPrivateKey);

				writer.writeInt(encodedX509Cert.length);
				writer.write(encodedX509Cert);

				return out.toByteArray();
			}
		}
	}

	private byte[] encodePrivateKey(PrivateKey privateKey) throws IOException {
		try (var out = new ByteArrayOutputStream()) {
			try (var writer = new DataOutputStream(out)) {
				var algorithmAsBytes = privateKey.getAlgorithm().getBytes(StandardCharsets.UTF_8);
				writer.writeInt(algorithmAsBytes.length);
				writer.write(algorithmAsBytes);

				writer.write(privateKey.getEncoded());
				return out.toByteArray();
			}
		}
	}

	@Override
	public synchronized Optional<CertificateInfo> readCertificate() throws CertificateStoreException {
		try {
			var config = this.readConfiguration();
			var p = config.getProperties();

			var tlsCertificateStr = (String) p.get(TLS_CERTIFICATE_CONFIG_KEY);
			return this.decodeCertificateInfo(tlsCertificateStr);
		} catch (IOException ex) {
			throw new CertificateStoreException("Failed to read certificate from configuration", ex);
		}
	}
	
	@Override
	public synchronized void saveCertificate(CertificateInfo certificateInfo) throws CertificateStoreException {
		String encodedCertStr;
		try {
			encodedCertStr = this.encodeCertificateInfoToString(certificateInfo);
		} catch (IOException | CertificateEncodingException ex) {
			throw new CertificateStoreException("Failed to write certificate: Failed to encode certificate info", ex);
		}

		try {
			var config = this.readConfiguration();

			var props = config.getProperties();
			props.put(TLS_CERTIFICATE_CONFIG_KEY, encodedCertStr);

			config.update(props);
		} catch (IOException ex) {
			throw new CertificateStoreException("Failed to save certificate to configuration", ex);
		}
	}
}
