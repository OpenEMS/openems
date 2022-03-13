package io.openems.backend.metadata.odoo.odoo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.metadata.odoo.Config;
import io.openems.backend.metadata.odoo.Field;
import io.openems.backend.metadata.odoo.Field.Partner;
import io.openems.backend.metadata.odoo.Field.SetupProtocol;
import io.openems.backend.metadata.odoo.Field.SetupProtocolItem;
import io.openems.backend.metadata.odoo.MyEdge;
import io.openems.backend.metadata.odoo.MyUser;
import io.openems.backend.metadata.odoo.OdooMetadata;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.UpdateUserLanguageRequest.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.ObjectUtils;
import io.openems.common.utils.PasswordUtils;

public class OdooHandler {

	protected final OdooMetadata parent;

	private final Logger log = LoggerFactory.getLogger(OdooHandler.class);
	private final Credentials credentials;

	public OdooHandler(OdooMetadata parent, Config config) {
		this.parent = parent;
		this.credentials = Credentials.fromConfig(config);
	}

	/**
	 * Writes one field to Odoo Edge model.
	 *
	 * @param edge        the Edge
	 * @param fieldValues the FieldValues
	 */
	public void writeEdge(MyEdge edge, FieldValue<?>... fieldValues) {
		try {
			OdooUtils.write(this.credentials, Field.EdgeDevice.ODOO_MODEL, new Integer[] { edge.getOdooId() },
					fieldValues);
		} catch (OpenemsException e) {
			this.parent.logError(this.log, "Unable to update Edge [" + edge.getId() + "] " //
					+ "Odoo-ID [" + edge.getOdooId() + "] " //
					+ "Fields [" + Stream.of(fieldValues).map(FieldValue::toString).collect(Collectors.joining(","))
					+ "]: " + e.getMessage());
		}
	}

	/**
	 * Adds a message in Odoo Chatter ('mail.thread').
	 *
	 * @param edge    the Edge
	 * @param message the message
	 */
	public void addChatterMessage(MyEdge edge, String message) {
		try {
			OdooUtils.addChatterMessage(this.credentials, Field.EdgeDevice.ODOO_MODEL, edge.getOdooId(), message);
		} catch (OpenemsException e) {
			this.parent.logError(this.log, "Unable to add Chatter Message to Edge [" + edge.getId() + "] " //
					+ "Message [" + message + "]" //
					+ ": " + e.getMessage());
		}
	}

	/**
	 * Returns Edge by setupPassword, otherwise an empty {@link Optional}.
	 *
	 * @param setupPassword to find Edge
	 * @return Edge or empty {@link Optional}
	 */
	public Optional<String> getEdgeIdBySetupPassword(String setupPassword) {
		var filter = new Domain(Field.EdgeDevice.SETUP_PASSWORD, "=", setupPassword);

		try {
			int[] search = OdooUtils.search(this.credentials, Field.EdgeDevice.ODOO_MODEL, filter);
			if (search.length == 0) {
				return Optional.empty();
			}

			Map<String, Object> read = OdooUtils.readOne(this.credentials, Field.EdgeDevice.ODOO_MODEL, search[0],
					Field.EdgeDevice.NAME);

			var name = (String) read.get(Field.EdgeDevice.NAME.id());
			if (name == null) {
				return Optional.empty();
			}

			return Optional.of(name);
		} catch (OpenemsException e) {
			this.parent.logInfo(this.log, "Unable to find Edge by setup password [" + setupPassword + "]");
		}

		return Optional.empty();
	}

	/**
	 * Assigns the given user with given {@link OdooUserRole} to the Edge. If Edge
	 * already assigned to user exit method.
	 *
	 * @param user     the Odoo user
	 * @param edge     the Odoo edge
	 * @param userRole the Odoo user role
	 * @throws OpenemsNamedException on error
	 */
	public void assignEdgeToUser(MyUser user, MyEdge edge, OdooUserRole userRole) throws OpenemsNamedException {
		this.assignEdgeToUser(user.getOdooId(), edge.getOdooId(), userRole);
		this.parent.authenticate(user.getToken());
	}

	/**
	 * Assigns the given user with given {@link OdooUserRole} to the Edge. If Edge
	 * already assigned to user exit method.
	 *
	 * @param userId   the Odoo user id
	 * @param edgeId   the Odoo edge
	 * @param userRole the Odoo user role
	 * @throws OpenemsException on error
	 */
	private void assignEdgeToUser(int userId, int edgeId, OdooUserRole userRole) throws OpenemsException {
		int[] found = OdooUtils.search(this.credentials, Field.EdgeDeviceUserRole.ODOO_MODEL,
				new Domain(Field.EdgeDeviceUserRole.USER_ID, "=", userId),
				new Domain(Field.EdgeDeviceUserRole.DEVICE_ID, "=", edgeId));

		if (found.length > 0) {
			return;
		}

		OdooUtils.create(this.credentials, Field.EdgeDeviceUserRole.ODOO_MODEL, //
				new FieldValue<>(Field.EdgeDeviceUserRole.USER_ID, userId), //
				new FieldValue<>(Field.EdgeDeviceUserRole.DEVICE_ID, edgeId), //
				new FieldValue<>(Field.EdgeDeviceUserRole.ROLE, userRole.getOdooRole()));
	}

	/**
	 * Authenticates a user using Username and Password.
	 *
	 * @param username the Username
	 * @param password the Password
	 * @return the session_id
	 * @throws OpenemsNamedException on login error
	 */
	public String authenticate(String username, String password) throws OpenemsNamedException {
		return OdooUtils.login(this.credentials, username, password);
	}

	/**
	 * Authenticates a user using a Session-ID.
	 *
	 * @param sessionId the Odoo Session-ID
	 * @return the {@link JsonObject} received from /openems_backend/info.
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject authenticateSession(String sessionId) throws OpenemsNamedException {
		return JsonUtils
				.getAsJsonObject(OdooUtils.sendJsonrpcRequest(this.credentials.getUrl() + "/openems_backend/info",
						"session_id=" + sessionId, new JsonObject()).result);
	}

	/**
	 * Logout a User.
	 *
	 * @param sessionId the Session-ID
	 * @throws OpenemsNamedException on error
	 */
	public void logout(String sessionId) {
		try {
			OdooUtils.sendJsonrpcRequest(this.credentials.getUrl() + "/web/session/destroy", "session_id=" + sessionId,
					new JsonObject());
		} catch (OpenemsNamedException e) {
			this.log.warn("Unable to logout session [" + sessionId + "]: " + e.getMessage());
		}
	}

	/**
	 * Get field from the 'Set-Cookie' field in HTTP headers.
	 *
	 * <p>
	 * Per <a href=
	 * "https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">specification</a>
	 * all variants of 'cookie' are accepted.
	 *
	 * @param headers   the HTTP headers
	 * @param fieldname the field name
	 * @return value as optional
	 */
	public static Optional<String> getFieldFromSetCookieHeader(Map<String, List<String>> headers, String fieldname) {
		for (Entry<String, List<String>> header : headers.entrySet()) {
			String key = header.getKey();
			if (key != null && key.equalsIgnoreCase("Set-Cookie")) {
				for (String cookie : header.getValue()) {
					for (String cookieVariable : cookie.split("; ")) {
						String[] keyValue = cookieVariable.split("=");
						if (keyValue.length == 2) {
							if (keyValue[0].equals(fieldname)) {
								return Optional.ofNullable(keyValue[1]);
							}
						}
					}
				}
			}
		}
		return Optional.empty();
	}

	/**
	 * Returns information about the given {@link MyUser}.
	 *
	 * @param user the {@link MyUser} to get information
	 * @return the {@link Partner}
	 * @throws OpenemsException on error
	 */
	public Map<String, Object> getUserInformation(MyUser user) throws OpenemsNamedException {
		var partnerId = this.getOdooPartnerId(user);

		Map<String, Object> odooPartner = OdooUtils.readOne(this.credentials, Field.Partner.ODOO_MODEL, partnerId,
				Field.Partner.FIRSTNAME, //
				Field.Partner.LASTNAME, //
				Field.Partner.EMAIL, //
				Field.Partner.PHONE, //
				Field.Partner.STREET, //
				Field.Partner.ZIP, //
				Field.Partner.CITY, //
				Field.Partner.COUNTRY, //
				Field.Partner.COMPANY_NAME);

		Object[] odooCountryId = ObjectUtils.getAsObjectArrray(odooPartner.get("country_id"));
		if (odooCountryId.length > 1) {
			Map<String, Object> countryCode = OdooUtils.readOne(this.credentials, Field.Country.ODOO_MODEL,
					(Integer) odooCountryId[0], Field.Country.CODE);

			var codeOpt = ObjectUtils.getAsOptionalString(countryCode.get("code"));
			if (codeOpt.isPresent()) {
				Object[] countryElement = Arrays.copyOf(odooCountryId, odooCountryId.length + 1);
				countryElement[2] = countryCode.get("code");

				odooPartner.put("country_id", countryElement);
			}
		}

		return odooPartner;
	}

	/**
	 * Update the given {@link MyUser} with information from {@link JsonObject}.
	 *
	 * @param user     the {@link MyUser} to update
	 * @param userJson the {@link JsonObject} information to update
	 * @throws OpenemsException on error
	 */
	public void setUserInformation(MyUser user, JsonObject userJson) throws OpenemsNamedException {
		Map<String, Object> fieldValues = new HashMap<>();
		fieldValues.putAll(this.updateAddress(userJson));
		fieldValues.putAll(this.updateCompany(user, userJson));

		JsonUtils.getAsOptionalString(userJson, "firstname") //
				.ifPresent(firstname -> fieldValues.put(Field.Partner.FIRSTNAME.id(), firstname));
		JsonUtils.getAsOptionalString(userJson, "lastname") //
				.ifPresent(lastname -> fieldValues.put(Field.Partner.LASTNAME.id(), lastname));
		JsonUtils.getAsOptionalString(userJson, "email") //
				.ifPresent(email -> fieldValues.put(Field.Partner.EMAIL.id(), email.toLowerCase()));
		JsonUtils.getAsOptionalString(userJson, "phone") //
				.ifPresent(phone -> fieldValues.put(Field.Partner.PHONE.id(), phone));

		var odooPartnerId = this.getOdooPartnerId(user.getOdooId());
		OdooUtils.write(this.credentials, Field.Partner.ODOO_MODEL, new Integer[] { odooPartnerId }, fieldValues);
	}

	/**
	 * Get address to update for an Odoo user.
	 *
	 * @param addressJson {@link JsonObject} to get the fields to update
	 * @return Fields to update
	 * @throws OpenemsException on error
	 */
	private Map<String, Object> updateAddress(JsonObject addressJson) throws OpenemsException {
		var addressOpt = JsonUtils.getAsOptionalJsonObject(addressJson, "address");
		if (!addressOpt.isPresent()) {
			return new HashMap<>();
		}
		var address = addressOpt.get();

		Map<String, Object> addressFields = new HashMap<>();
		addressFields.put("type", "private");
		JsonUtils.getAsOptionalString(address, "street") //
				.ifPresent(street -> addressFields.put(Field.Partner.STREET.id(), street));
		JsonUtils.getAsOptionalString(address, "zip") //
				.ifPresent(zip -> addressFields.put(Field.Partner.ZIP.id(), zip));
		JsonUtils.getAsOptionalString(address, "city") //
				.ifPresent(city -> addressFields.put(Field.Partner.CITY.id(), city));

		var countryCodeOpt = JsonUtils.getAsOptionalString(address, "country");
		if (countryCodeOpt.isPresent()) {
			var countryCode = countryCodeOpt.get().toUpperCase();

			int[] countryFound = OdooUtils.search(this.credentials, Field.Country.ODOO_MODEL, //
					new Domain(Field.Country.CODE, "=", countryCode));
			if (countryFound.length == 1) {
				addressFields.put(Field.Partner.COUNTRY.id(), countryFound[0]);
			} else {
				this.log.info("Country with code [" + countryCode + "] not found");
			}
		}

		return addressFields;
	}

	/**
	 * Get company to update for an Odoo user. Checks if the given company exits in
	 * Odoo and assign the company to the Odoo user. Otherwise a new company will be
	 * created in Odoo.
	 *
	 * @param companyJson {@link JsonObject} to get the fields to update
	 * @return Fields to update
	 * @throws OpenemsException on error
	 */
	private Map<String, Object> updateCompany(JsonObject companyJson) throws OpenemsException {
		return this.updateCompany(null, companyJson);
	}

	/**
	 * Get company to update for an Odoo user. If given user is not null, check the
	 * users company with the new company name for equality. Both are equal nothing
	 * to update. Otherwise the new company will be assigned to the user or the new
	 * company will be created in Odoo.
	 *
	 * @param user        {@link MyUser} to check company name
	 * @param companyJson {@link JsonObject} to get the fields to update
	 * @return Fields to update
	 * @throws OpenemsException on error
	 */
	private Map<String, Object> updateCompany(MyUser user, JsonObject companyJson) throws OpenemsException {
		var companyOpt = JsonUtils.getAsOptionalJsonObject(companyJson, "company");
		if (!companyOpt.isPresent()) {
			return new HashMap<>();
		}
		var companyNameOpt = JsonUtils.getAsOptionalString(companyOpt.get(), "name");
		if (!companyNameOpt.isPresent()) {
			return new HashMap<>();
		}
		var jCompanyName = companyNameOpt.get();

		if (user != null) {
			Map<String, Object> odooPartner = OdooUtils.readOne(this.credentials, Field.Partner.ODOO_MODEL, //
					this.getOdooPartnerId(user.getOdooId()), //
					Field.Partner.COMPANY_NAME);

			var partnerCompanyNameOpt = ObjectUtils
					.getAsOptionalString(odooPartner.get(Field.Partner.COMPANY_NAME.id()));
			if (partnerCompanyNameOpt.isPresent()) {
				if (jCompanyName.equals(partnerCompanyNameOpt.get())) {
					return new HashMap<>();
				}
			}
		}

		int[] companyFound = OdooUtils.search(this.credentials, Field.Partner.ODOO_MODEL, //
				new Domain(Field.Partner.IS_COMPANY, "=", true),
				new Domain(Field.Partner.COMPANY_NAME, "=", jCompanyName));

		Map<String, Object> companyFields = new HashMap<>();
		if (companyFound.length > 0) {
			companyFields.put(Field.Partner.PARENT.id(), companyFound[0]);
		} else {
			int createdCompany = OdooUtils.create(this.credentials, Field.Partner.ODOO_MODEL, //
					new FieldValue<>(Field.Partner.IS_COMPANY, true),
					new FieldValue<>(Field.Partner.NAME, jCompanyName));
			companyFields.put(Field.Partner.PARENT.id(), createdCompany);
		}

		return companyFields;
	}

	/**
	 * Returns the Odoo report for a setup protocol.
	 *
	 * @param setupProtocolId the Odoo setup protocol id
	 * @return report as a byte array
	 * @throws OpenemsNamedException on error
	 */
	public byte[] getOdooSetupProtocolReport(int setupProtocolId) throws OpenemsNamedException {
		return OdooUtils.getOdooReport(this.credentials, "edge.report_edge_setup_protocol_template", setupProtocolId);
	}

	/**
	 * Save the Setup Protocol to Odoo.
	 *
	 * @param user              {@link MyUser} current user
	 * @param setupProtocolJson {@link SetupProtocol} the setup protocol
	 * @return the Setup Protocol ID
	 * @throws OpenemsNamedException on error
	 */
	public int submitSetupProtocol(MyUser user, JsonObject setupProtocolJson) throws OpenemsNamedException {
		var userJson = JsonUtils.getAsJsonObject(setupProtocolJson, "customer");
		var edgeJson = JsonUtils.getAsJsonObject(setupProtocolJson, "edge");
		var installerJson = JsonUtils.getAsJsonObject(setupProtocolJson, "installer");

		var edgeId = JsonUtils.getAsString(edgeJson, "id");
		int[] foundEdge = OdooUtils.search(this.credentials, Field.EdgeDevice.ODOO_MODEL,
				new Domain(Field.EdgeDevice.NAME, "=", edgeId));
		if (foundEdge.length != 1) {
			throw new OpenemsException("Edge not found for id [" + edgeId + "]");
		}

		var password = PasswordUtils.generateRandomPassword(8);
		var odooUserId = this.createOdooUser(userJson, password);

		var customerId = this.getOdooPartnerId(odooUserId);
		var installerId = this.getOdooPartnerId(user);
		this.assignEdgeToUser(odooUserId, foundEdge[0], OdooUserRole.OWNER);

		var protocolId = this.createSetupProtocol(setupProtocolJson, foundEdge[0], customerId, installerId);

		var installer = OdooUtils.readOne(this.credentials, Field.Partner.ODOO_MODEL, installerId,
				Field.Partner.IS_COMPANY);
		boolean isCompany = (boolean) installer.get("is_company");
		if (!isCompany) {
			Map<String, Object> fieldsToUpdate = new HashMap<>();
			JsonUtils.getAsOptionalString(installerJson, "firstname") //
					.ifPresent(firstname -> fieldsToUpdate.put(Field.Partner.FIRSTNAME.id(), firstname));
			JsonUtils.getAsOptionalString(installerJson, "lastname") //
					.ifPresent(lastname -> fieldsToUpdate.put(Field.Partner.LASTNAME.id(), lastname));

			if (!fieldsToUpdate.isEmpty()) {
				OdooUtils.write(this.credentials, Field.Partner.ODOO_MODEL, new Integer[] { installerId },
						fieldsToUpdate);
			}
		}

		try {
			this.sendSetupProtocolMail(user, protocolId, edgeId);
		} catch (OpenemsNamedException ex) {
			this.log.warn("Unable to send email", ex);
		}

		return protocolId;
	}

	/**
	 * Call Odoo api to send mail via Odoo.
	 *
	 * @param user       the Odoo user
	 * @param protocolId the Odoo setup protocol id
	 * @param edgeId     the Odoo edge
	 * @throws OpenemsNamedException on error
	 */
	private void sendSetupProtocolMail(MyUser user, int protocolId, String edgeId) throws OpenemsNamedException {
		OdooUtils.sendAdminJsonrpcRequest(this.credentials, "/openems_backend/sendSetupProtocolEmail",
				JsonUtils.buildJsonObject() //
						.add("params", JsonUtils.buildJsonObject() //
								.addProperty("setupProtocolId", protocolId) //
								.addProperty("edgeId", edgeId) //
								.build()) //
						.build());
	}

	/**
	 * Create an Odoo user and return thats id. If user already exists the user will
	 * be updated and return the user id.
	 *
	 * @param userJson the {@link Partner} to create user
	 * @param password the password to set for the new user
	 * @return the Odoo user id
	 * @throws OpenemsNamedException on error
	 */
	private int createOdooUser(JsonObject userJson, String password) throws OpenemsNamedException {
		Map<String, Object> customerFields = new HashMap<>();
		customerFields.putAll(this.updateAddress(userJson));
		customerFields.putAll(this.updateCompany(userJson));

		JsonUtils.getAsOptionalString(userJson, "firstname") //
				.ifPresent(firstname -> customerFields.put(Field.Partner.FIRSTNAME.id(), firstname));
		JsonUtils.getAsOptionalString(userJson, "lastname") //
				.ifPresent(lastname -> customerFields.put(Field.Partner.LASTNAME.id(), lastname));

		var email = JsonUtils.getAsString(userJson, "email").toLowerCase();
		customerFields.put(Field.Partner.EMAIL.id(), email);

		JsonUtils.getAsOptionalString(userJson, "phone") //
				.ifPresent(phone -> customerFields.put(Field.Partner.PHONE.id(), phone));

		int[] userFound = OdooUtils.search(this.credentials, Field.User.ODOO_MODEL,
				new Domain(Field.User.LOGIN, "=", email));

		if (userFound.length == 1) {
			// update existing user
			var userId = userFound[0];
			OdooUtils.write(this.credentials, Field.User.ODOO_MODEL, new Integer[] { userId }, customerFields);
			return userId;
		}

		customerFields.put(Field.User.LOGIN.id(), email);
		customerFields.put(Field.User.PASSWORD.id(), password);
		customerFields.put(Field.User.GLOBAL_ROLE.id(), OdooUserRole.OWNER.getOdooRole());
		customerFields.put(Field.User.GROUPS.id(), OdooUserRole.OWNER.toOdooIds());
		var createdUserId = OdooUtils.create(this.credentials, Field.User.ODOO_MODEL, customerFields);

		try {
			this.addTagToPartner(createdUserId);
		} catch (OpenemsException e) {
			this.log.warn("Unable to add tag for Odoo user id [" + createdUserId + "]", e);
		}

		this.sendRegistrationMail(createdUserId, password);
		return createdUserId;
	}

	/**
	 * Add the "Created via IBN" tag to the referenced partner for given user id.
	 * 
	 * @param userId to get Odoo partner
	 * @throws OpenemsException on error
	 */
	private void addTagToPartner(int userId) throws OpenemsException {
		var tagId = OdooUtils.getObjectReference(this.credentials, "edge", "res_partner_category_created_via_ibn");
		var partnerId = this.getOdooPartnerId(userId);

		OdooUtils.write(this.credentials, Field.Partner.ODOO_MODEL, new Integer[] { partnerId },
				new FieldValue<>(Field.Partner.CATEGORY_ID, new Integer[] { tagId }));
	}

	/**
	 * Create a setup protocol in Odoo.
	 *
	 * @param jsonObject  {@link SetupProtocol} to create
	 * @param edgeId      the Edge-ID
	 * @param customerId  Odoo customer id to set
	 * @param installerId Odoo installer id to set
	 * @return the Odoo id of created setup protocol
	 * @throws OpenemsException on error
	 */
	private int createSetupProtocol(JsonObject jsonObject, int edgeId, int customerId, int installerId)
			throws OpenemsException {
		Integer locationId = null;

		var jLocationOpt = JsonUtils.getAsOptionalJsonObject(jsonObject, "location");
		if (jLocationOpt.isPresent()) {
			JsonObject location = jLocationOpt.get();

			Map<String, Object> locationFields = new HashMap<>();
			locationFields.putAll(this.updateAddress(location));
			locationFields.putAll(this.updateCompany(location));

			JsonUtils.getAsOptionalString(location, "firstname") //
					.ifPresent(firstname -> locationFields.put(Field.Partner.FIRSTNAME.id(), firstname));
			JsonUtils.getAsOptionalString(location, "lastname") //
					.ifPresent(lastname -> locationFields.put(Field.Partner.LASTNAME.id(), lastname));
			JsonUtils.getAsOptionalString(location, "email") //
					.ifPresent(mail -> locationFields.put(Field.Partner.EMAIL.id(), mail.toLowerCase()));
			JsonUtils.getAsOptionalString(location, "phone") //
					.ifPresent(phone -> locationFields.put(Field.Partner.PHONE.id(), phone));

			locationId = OdooUtils.create(this.credentials, Field.Partner.ODOO_MODEL, locationFields);
		}

		Map<String, Object> setupProtocolFields = new HashMap<>();
		setupProtocolFields.put(Field.SetupProtocol.CUSTOMER.id(), customerId);
		setupProtocolFields.put(Field.SetupProtocol.DIFFERENT_LOCATION.id(), locationId);
		setupProtocolFields.put(Field.SetupProtocol.INSTALLER.id(), installerId);
		setupProtocolFields.put(Field.SetupProtocol.EDGE.id(), edgeId);

		int setupProtocolId = OdooUtils.create(this.credentials, Field.SetupProtocol.ODOO_MODEL, setupProtocolFields);

		var lotsOpt = JsonUtils.getAsOptionalJsonArray(jsonObject, "lots");
		if (lotsOpt.isPresent()) {
			this.createSetupProtocolProductionLots(setupProtocolId, lotsOpt.get());
		}
		var itemsOpt = JsonUtils.getAsOptionalJsonArray(jsonObject, "items");
		if (itemsOpt.isPresent()) {
			this.createSetupProtocolItems(setupProtocolId, itemsOpt.get());
		}

		return setupProtocolId;
	}

	/**
	 * Create production lots for the given setup protocol id.
	 *
	 * @param setupProtocolId assign to the lots
	 * @param lots            list of setup protocol production lots to create
	 * @throws OpenemsException on error
	 */
	private void createSetupProtocolProductionLots(int setupProtocolId, JsonArray lots) throws OpenemsException {
		List<JsonElement> serialNumbersNotFound = new ArrayList<>();
		for (int i = 0; i < lots.size(); i++) {
			JsonElement lot = lots.get(i);

			Map<String, Object> lotFields = new HashMap<>();
			lotFields.put(Field.SetupProtocolProductionLot.SETUP_PROTOCOL.id(), setupProtocolId);
			lotFields.put(Field.SetupProtocolProductionLot.SEQUENCE.id(), i);

			JsonUtils.getAsOptionalString(lot, "category") //
					.ifPresent(category -> lotFields.put("category", category));
			JsonUtils.getAsOptionalString(lot, "name") //
					.ifPresent(name -> lotFields.put("name", name));

			var serialNumberOpt = JsonUtils.getAsOptionalString(lot, "serialNumber");
			if (serialNumberOpt.isPresent()) {
				int[] lotId = OdooUtils.search(this.credentials, Field.StockProductionLot.ODOO_MODEL, //
						new Domain(Field.StockProductionLot.SERIAL_NUMBER, "=", serialNumberOpt.get()));

				if (lotId.length > 0) {
					lotFields.put(Field.SetupProtocolProductionLot.LOT.id(), lotId[0]);
					OdooUtils.create(this.credentials, Field.SetupProtocolProductionLot.ODOO_MODEL, lotFields);
				} else {
					serialNumbersNotFound.add(lot);
				}
			}

		}

		this.createNotFoundSerialNumbers(setupProtocolId, serialNumbersNotFound);
	}

	/**
	 * Create for the given serial numbers that were not found a
	 * {@link SetupProtocolItem}.
	 *
	 * @param setupProtocolId the protocol id
	 * @param serialNumbers   not found serial numbers
	 * @throws OpenemsException on error
	 */
	private void createNotFoundSerialNumbers(int setupProtocolId, List<JsonElement> serialNumbers)
			throws OpenemsException {
		for (int i = 0; i < serialNumbers.size(); i++) {
			Map<String, Object> setupProtocolItem = new HashMap<>();
			setupProtocolItem.put(Field.SetupProtocolItem.SETUP_PROTOCOL.id(), setupProtocolId);
			setupProtocolItem.put(Field.SetupProtocolItem.SEQUENCE.id(), i);
			setupProtocolItem.put("category", "Seriennummern wurden im System nicht gefunden");

			JsonElement item = serialNumbers.get(i);
			JsonUtils.getAsOptionalString(item, "name") //
					.ifPresent(name -> setupProtocolItem.put("name", name));
			JsonUtils.getAsOptionalString(item, "serialNumber") //
					.ifPresent(serialNumber -> setupProtocolItem.put("value", serialNumber));

			OdooUtils.create(this.credentials, Field.SetupProtocolItem.ODOO_MODEL, setupProtocolItem);
		}
	}

	/**
	 * Create items for the given setup protocol id.
	 *
	 * @param setupProtocolId assign to the items
	 * @param items           list of setup protocol items to create
	 * @throws OpenemsException on error
	 */
	private void createSetupProtocolItems(int setupProtocolId, JsonArray items) throws OpenemsException {
		for (int i = 0; i < items.size(); i++) {
			JsonElement item = items.get(i);

			Map<String, Object> setupProtocolItem = new HashMap<>();
			setupProtocolItem.put(Field.SetupProtocolItem.SETUP_PROTOCOL.id(), setupProtocolId);
			setupProtocolItem.put(Field.SetupProtocolItem.SEQUENCE.id(), i);

			JsonUtils.getAsOptionalString(item, "category") //
					.ifPresent(category -> setupProtocolItem.put("category", category));
			JsonUtils.getAsOptionalString(item, "name") //
					.ifPresent(name -> setupProtocolItem.put("name", name));
			JsonUtils.getAsOptionalString(item, "value") //
					.ifPresent(value -> setupProtocolItem.put("value", value));

			OdooUtils.create(this.credentials, Field.SetupProtocolItem.ODOO_MODEL, setupProtocolItem);
		}
	}

	/**
	 * Gets the referenced Odoo partner id for an Odoo user.
	 *
	 * @param user the Odoo user
	 * @return the Odoo partner id
	 * @throws OpenemsException on error
	 */
	private int getOdooPartnerId(MyUser user) throws OpenemsException {
		return this.getOdooPartnerId(user.getOdooId());
	}

	/**
	 * Gets the referenced Odoo partner id for an Odoo user id.
	 *
	 * @param odooUserId of the Odoo user
	 * @return the Odoo partner id
	 * @throws OpenemsException on error
	 */
	private int getOdooPartnerId(int odooUserId) throws OpenemsException {
		Map<String, Object> odooUser = OdooUtils.readOne(this.credentials, Field.User.ODOO_MODEL, odooUserId,
				Field.User.PARTNER);

		var odooPartnerIdOpt = OdooUtils.getOdooReferenceId(odooUser.get(Field.User.PARTNER.id()));

		if (!odooPartnerIdOpt.isPresent()) {
			throw new OpenemsException("Odoo partner not found for user ['" + odooUserId + "']");
		}

		return odooPartnerIdOpt.get();
	}

	/**
	 * Register an user in Odoo with the given {@link OdooUserRole}.
	 *
	 * @param jsonObject {@link JsonObject} that represents an user
	 * @param role       {@link OdooUserRole} to set for the user
	 * @throws OpenemsNamedException on error
	 */
	public void registerUser(JsonObject jsonObject, OdooUserRole role) throws OpenemsNamedException {
		var emailOpt = JsonUtils.getAsOptionalString(jsonObject, "email");
		if (!emailOpt.isPresent()) {
			throw new OpenemsException("No email specified");
		}
		var email = emailOpt.get().toLowerCase();

		int[] userFound = OdooUtils.search(this.credentials, Field.User.ODOO_MODEL, //
				new Domain(Field.User.LOGIN, "=", email));
		if (userFound.length > 0) {
			throw new OpenemsException("User already exists with email [" + email + "]");
		}

		Map<String, Object> userFields = new HashMap<>();
		userFields.put(Field.User.LOGIN.id(), email);
		userFields.put(Field.Partner.EMAIL.id(), email);
		userFields.put(Field.User.GLOBAL_ROLE.id(), role.getOdooRole());
		userFields.put(Field.User.GROUPS.id(), role.toOdooIds());
		userFields.putAll(this.updateAddress(jsonObject));
		userFields.putAll(this.updateCompany(jsonObject));

		JsonUtils.getAsOptionalString(jsonObject, "firstname") //
				.ifPresent(firstname -> userFields.put("firstname", firstname));
		JsonUtils.getAsOptionalString(jsonObject, "lastname") //
				.ifPresent(lastname -> userFields.put("lastname", lastname));
		JsonUtils.getAsOptionalString(jsonObject, "phone") //
				.ifPresent(phone -> userFields.put("phone", phone));
		JsonUtils.getAsOptionalString(jsonObject, "password") //
				.ifPresent(password -> userFields.put("password", password));

		int createdUserId = OdooUtils.create(this.credentials, Field.User.ODOO_MODEL, userFields);
		this.sendRegistrationMail(createdUserId);
	}

	/**
	 * Call Odoo api to send registration mail via Odoo.
	 *
	 * @param odooUserId Odoo user id to send the mail
	 * @throws OpenemsNamedException error
	 */
	private void sendRegistrationMail(int odooUserId) throws OpenemsNamedException {
		this.sendRegistrationMail(odooUserId, null);
	}

	/**
	 * Call Odoo api to send registration mail via Odoo.
	 *
	 * @param odooUserId Odoo user id to send the mail
	 * @param password   password for the user
	 * @throws OpenemsNamedException error
	 */
	private void sendRegistrationMail(int odooUserId, String password) throws OpenemsNamedException {
		OdooUtils.sendAdminJsonrpcRequest(this.credentials, "/openems_backend/sendRegistrationEmail",
				JsonUtils.buildJsonObject() //
						.add("params", JsonUtils.buildJsonObject() //
								.addProperty("userId", odooUserId) //
								.addProperty("password", password) //
								.build()) //
						.build());
	}

	/**
	 * Update language for the given user.
	 *
	 * @param user     {@link MyUser} the current user
	 * @param language to set
	 * @throws OpenemsException on error
	 */
	public void updateUserLanguage(MyUser user, Language language) throws OpenemsException {
		try {
			OdooUtils.write(this.credentials, Field.User.ODOO_MODEL, new Integer[] { user.getOdooId() }, //
					new FieldValue<>(Field.User.OPENEMS_LANGUAGE, language.name()));
		} catch (OpenemsNamedException ex) {
			throw new OpenemsException("Unable to set language [" + language.name() + "] for current user", ex);
		}
	}

}
