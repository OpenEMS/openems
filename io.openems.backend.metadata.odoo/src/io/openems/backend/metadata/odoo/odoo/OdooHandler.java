package io.openems.backend.metadata.odoo.odoo;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.common.alerting.OfflineEdgeAlertingSetting;
import io.openems.backend.common.alerting.SumStateAlertingSetting;
import io.openems.backend.common.alerting.UserAlertingSettings;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.EdgeUser;
import io.openems.backend.common.metadata.User;
import io.openems.backend.metadata.odoo.Config;
import io.openems.backend.metadata.odoo.EdgeCache;
import io.openems.backend.metadata.odoo.Field;
import io.openems.backend.metadata.odoo.Field.AlertingSetting;
import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.backend.metadata.odoo.Field.Partner;
import io.openems.backend.metadata.odoo.Field.SetupProtocol;
import io.openems.backend.metadata.odoo.Field.SetupProtocolItem;
import io.openems.backend.metadata.odoo.MetadataOdoo;
import io.openems.backend.metadata.odoo.MyEdge;
import io.openems.backend.metadata.odoo.MyUser;
import io.openems.backend.metadata.odoo.odoo.Domain.Operator;
import io.openems.backend.metadata.odoo.odoo.OdooUtils.SuccessResponseAndHeaders;
import io.openems.common.OpenemsOEM;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.GetEdgesRequest.PaginationOptions;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.ObjectUtils;
import io.openems.common.utils.PasswordUtils;

public class OdooHandler {

	protected final MetadataOdoo parent;
	private final EdgeCache edgeCache;

	private final Logger log = LoggerFactory.getLogger(OdooHandler.class);
	private final Credentials credentials;

	public OdooHandler(MetadataOdoo parent, EdgeCache edgeCache, Config config) {
		this.parent = parent;
		this.edgeCache = edgeCache;
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
	 * Writes one field to Odoo EdgeUser model.
	 *
	 * @param edgeUser    the EdgeUser
	 * @param fieldValues the FieldValues
	 */
	public void writeEdgeUser(EdgeUser edgeUser, FieldValue<?>... fieldValues) {
		try {
			OdooUtils.write(this.credentials, Field.EdgeDeviceUserRole.ODOO_MODEL, new Integer[] { edgeUser.getId() },
					fieldValues);
		} catch (OpenemsException e) {
			this.parent.logError(this.log, "Unable to update EdgeUser [" + edgeUser.getId() + "] " //
					+ "Edge [" + edgeUser.getEdgeId() + "] " //
					+ "User [" + edgeUser.getUserId() + "] " //
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
		var filter = new Domain(Field.EdgeDevice.SETUP_PASSWORD, Operator.EQ, setupPassword);

		try {
			var search = OdooUtils.search(this.credentials, Field.EdgeDevice.ODOO_MODEL, filter);
			if (search.length == 0) {
				return Optional.empty();
			}

			var read = OdooUtils.readOne(this.credentials, Field.EdgeDevice.ODOO_MODEL, search[0],
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
		var found = OdooUtils.search(this.credentials, Field.EdgeDeviceUserRole.ODOO_MODEL,
				new Domain(Field.EdgeDeviceUserRole.USER_ODOO_ID, Operator.EQ, userId),
				new Domain(Field.EdgeDeviceUserRole.DEVICE_ODOO_ID, Operator.EQ, edgeId));

		if (found.length > 0) {
			return;
		}

		var fields = Lists.newArrayList(//
				new FieldValue<>(Field.EdgeDeviceUserRole.USER_ODOO_ID, userId), //
				new FieldValue<>(Field.EdgeDeviceUserRole.DEVICE_ODOO_ID, edgeId), //
				new FieldValue<>(Field.EdgeDeviceUserRole.ROLE, userRole.getOdooRole()) //
		);
		if (userRole.equals(OdooUserRole.OWNER)) {
			fields.add(new FieldValue<>(Field.EdgeDeviceUserRole.TIME_TO_WAIT, 60));
		}
		OdooUtils.create(this.credentials, Field.EdgeDeviceUserRole.ODOO_MODEL, fields.toArray(FieldValue[]::new));
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
	 * Returns information about the given {@link MyUser}.
	 *
	 * @param user the {@link MyUser} to get information
	 * @return the {@link Partner}
	 * @throws OpenemsException on error
	 */
	public Map<String, Object> getUserInformation(MyUser user) throws OpenemsNamedException {
		var partnerId = this.getOdooPartnerId(user);

		var odooPartner = OdooUtils.readOne(this.credentials, Field.Partner.ODOO_MODEL, partnerId,
				Field.Partner.FIRSTNAME, //
				Field.Partner.LASTNAME, //
				Field.Partner.EMAIL, //
				Field.Partner.PHONE, //
				Field.Partner.STREET, //
				Field.Partner.ZIP, //
				Field.Partner.CITY, //
				Field.Partner.COUNTRY, //
				Field.Partner.COMPANY_NAME);

		var odooCountryId = ObjectUtils.getAsObjectArrray(odooPartner.get("country_id"));
		if (odooCountryId.length > 1) {
			var countryCode = OdooUtils.readOne(this.credentials, Field.Country.ODOO_MODEL, (Integer) odooCountryId[0],
					Field.Country.CODE);

			var codeOpt = ObjectUtils.getAsOptionalString(countryCode.get("code"));
			if (codeOpt.isPresent()) {
				var countryElement = Arrays.copyOf(odooCountryId, odooCountryId.length + 1);
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
		var fieldValues = new HashMap<>(this.updateAddress(userJson));
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
		addressFields.put("type", "contact");
		JsonUtils.getAsOptionalString(address, "street") //
				.ifPresent(street -> addressFields.put(Field.Partner.STREET.id(), street));
		JsonUtils.getAsOptionalString(address, "zip") //
				.ifPresent(zip -> addressFields.put(Field.Partner.ZIP.id(), zip));
		JsonUtils.getAsOptionalString(address, "city") //
				.ifPresent(city -> addressFields.put(Field.Partner.CITY.id(), city));

		var countryCodeOpt = JsonUtils.getAsOptionalString(address, "country");
		if (countryCodeOpt.isPresent()) {
			var countryCode = countryCodeOpt.get().toUpperCase();

			var countryFound = OdooUtils.search(this.credentials, Field.Country.ODOO_MODEL, //
					new Domain(Field.Country.CODE, Operator.EQ, countryCode));
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
			var odooPartner = OdooUtils.readOne(this.credentials, Field.Partner.ODOO_MODEL, //
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

		var companyFound = OdooUtils.search(this.credentials, Field.Partner.ODOO_MODEL, //
				new Domain(Field.Partner.IS_COMPANY, Operator.EQ, true),
				new Domain(Field.Partner.COMPANY_NAME, Operator.EQ, jCompanyName));

		Map<String, Object> companyFields = new HashMap<>();
		if (companyFound.length > 0) {
			companyFields.put(Field.Partner.PARENT.id(), companyFound[0]);
		} else {
			var createdCompany = OdooUtils.create(this.credentials, Field.Partner.ODOO_MODEL, //
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
		return OdooUtils.getOdooReport(this.credentials, "openems.report_openems_setup_protocol_template",
				setupProtocolId);
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
		var oem = OpenemsOEM.Manufacturer.valueOf(JsonUtils.getAsString(setupProtocolJson, "oem").toUpperCase());

		var edgeId = JsonUtils.getAsString(edgeJson, "id");
		var foundEdge = OdooUtils.search(this.credentials, Field.EdgeDevice.ODOO_MODEL,
				new Domain(Field.EdgeDevice.NAME, Operator.EQ, edgeId));
		if (foundEdge.length != 1) {
			throw new OpenemsException("Edge not found for id [" + edgeId + "]");
		}

		var password = PasswordUtils.generateRandomPassword(8);
		var odooUserId = this.createOdooUser(userJson, password, oem);

		var customerId = this.getOdooPartnerId(odooUserId);
		var installerId = this.getOdooPartnerId(user);
		this.assignEdgeToUser(odooUserId, foundEdge[0], OdooUserRole.OWNER);

		var protocolId = this.createSetupProtocol(setupProtocolJson, foundEdge[0], customerId, installerId);

		this.updateEdgeComment(userJson, edgeId, foundEdge[0]);

		var installer = OdooUtils.readOne(this.credentials, Field.Partner.ODOO_MODEL, installerId,
				Field.Partner.IS_COMPANY);
		var isCompany = (boolean) installer.get("is_company");
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
			this.log.warn("User [" + user.getId() + ":" + user.getName() + "] Unable to send email", ex);
		}

		return protocolId;
	}

	/**
	 * Update the Odoo edge comment by customers firstname, lastname and city.
	 *
	 * @param customer   json object to get customer information
	 * @param edgeId     to update the comment
	 * @param odooEdgeId Odoo edge id
	 * @throws OpenemsNamedException on error
	 */
	private void updateEdgeComment(JsonObject customer, String edgeId, int odooEdgeId) throws OpenemsNamedException {
		// build comment
		var builder = new StringBuilder();
		JsonUtils.getAsOptionalString(customer, "firstname") //
				.ifPresent(firstname -> builder.append(firstname));
		JsonUtils.getAsOptionalString(customer, "lastname") //
				.ifPresent(lastname -> {
					if (builder.length() > 0) {
						builder.append(" ");
					}
					builder.append(lastname);
				});
		JsonUtils.getAsOptionalJsonObject(customer, "address") //
				.ifPresent(address -> { //
					JsonUtils.getAsOptionalString(address, "city") //
							.ifPresent(city -> {
								if (builder.length() > 0) {
									builder.append(", ");
								}
								builder.append(city);
							});
				});
		var comment = builder.toString();

		// update comment for edge
		OdooUtils.write(this.credentials, Field.EdgeDevice.ODOO_MODEL, new Integer[] { odooEdgeId },
				new FieldValue<>(Field.EdgeDevice.COMMENT, comment));

		// update edge cache
		var edge = this.edgeCache.getEdgeFromEdgeId(edgeId);
		if (edge != null) {
			edge.setComment(comment);
		}
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
	 * @param oem      OEM name
	 * @return the Odoo user id
	 * @throws OpenemsNamedException on error
	 */
	private int createOdooUser(JsonObject userJson, String password, OpenemsOEM.Manufacturer oem)
			throws OpenemsNamedException {
		var customerFields = new HashMap<>(this.updateAddress(userJson));
		customerFields.putAll(this.updateCompany(userJson));

		JsonUtils.getAsOptionalString(userJson, "firstname") //
				.ifPresent(firstname -> customerFields.put(Field.Partner.FIRSTNAME.id(), firstname));
		JsonUtils.getAsOptionalString(userJson, "lastname") //
				.ifPresent(lastname -> customerFields.put(Field.Partner.LASTNAME.id(), lastname));

		var email = JsonUtils.getAsString(userJson, "email").toLowerCase();
		customerFields.put(Field.Partner.EMAIL.id(), email);

		JsonUtils.getAsOptionalString(userJson, "phone") //
				.ifPresent(phone -> customerFields.put(Field.Partner.PHONE.id(), phone));

		var userFound = OdooUtils.search(this.credentials, Field.User.ODOO_MODEL,
				new Domain(Field.User.LOGIN, Operator.EQ, email));

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

		this.sendRegistrationMail(createdUserId, password, oem);
		return createdUserId;
	}

	/**
	 * Add tags to the referenced partner for given user id.
	 *
	 * @param userId to get Odoo partner
	 * @throws OpenemsException on error
	 */
	private void addTagToPartner(int userId) throws OpenemsException {
		var createdViaIbnTag = OdooUtils.getObjectReference(this.credentials, "openems",
				"res_partner_category_created_via_ibn");
		var customerTag = OdooUtils.getObjectReference(this.credentials, "openems", "res_partner_category_customer");

		var partnerId = this.getOdooPartnerId(userId);

		OdooUtils.write(this.credentials, Field.Partner.ODOO_MODEL, new Integer[] { partnerId },
				new FieldValue<>(Field.Partner.CATEGORY_ID, new Integer[] { createdViaIbnTag, customerTag }));
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
			var location = jLocationOpt.get();

			var locationFields = new HashMap<>(this.updateAddress(location));
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

		var setupProtocolId = OdooUtils.create(this.credentials, Field.SetupProtocol.ODOO_MODEL, setupProtocolFields);

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
		for (var i = 0; i < lots.size(); i++) {
			var lot = lots.get(i);

			Map<String, Object> lotFields = new HashMap<>();
			lotFields.put(Field.SetupProtocolProductionLot.SETUP_PROTOCOL.id(), setupProtocolId);
			lotFields.put(Field.SetupProtocolProductionLot.SEQUENCE.id(), i);

			JsonUtils.getAsOptionalString(lot, "category") //
					.ifPresent(category -> lotFields.put("category", category));
			JsonUtils.getAsOptionalString(lot, "name") //
					.ifPresent(name -> lotFields.put("name", name));

			var serialNumberOpt = JsonUtils.getAsOptionalString(lot, "serialNumber");
			if (serialNumberOpt.isPresent()) {
				var lotId = OdooUtils.search(this.credentials, Field.StockProductionLot.ODOO_MODEL, //
						new Domain(Field.StockProductionLot.SERIAL_NUMBER, Operator.EQ, serialNumberOpt.get()));

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
		for (var i = 0; i < serialNumbers.size(); i++) {
			Map<String, Object> setupProtocolItem = new HashMap<>();
			setupProtocolItem.put(Field.SetupProtocolItem.SETUP_PROTOCOL.id(), setupProtocolId);
			setupProtocolItem.put(Field.SetupProtocolItem.SEQUENCE.id(), i);
			setupProtocolItem.put("category", "Seriennummern wurden im System nicht gefunden");

			var item = serialNumbers.get(i);
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
		for (var i = 0; i < items.size(); i++) {
			var item = items.get(i);

			Map<String, Object> setupProtocolItem = new HashMap<>();
			setupProtocolItem.put(Field.SetupProtocolItem.SETUP_PROTOCOL.id(), setupProtocolId);
			setupProtocolItem.put(Field.SetupProtocolItem.SEQUENCE.id(), i);

			JsonUtils.getAsOptionalString(item, "category") //
					.ifPresent(category -> setupProtocolItem.put("category", category));
			JsonUtils.getAsOptionalString(item, "name") //
					.ifPresent(name -> setupProtocolItem.put("name", name));
			JsonUtils.getAsOptionalString(item, "value") //
					.ifPresent(value -> setupProtocolItem.put("value", value));
			JsonUtils.getAsOptionalString(item, "view") //
					.ifPresent(view -> setupProtocolItem.put("view", view));
			JsonUtils.getAsOptionalString(item, "field") //
					.ifPresent(field -> setupProtocolItem.put("field", field));

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
		var odooUser = OdooUtils.readOne(this.credentials, Field.User.ODOO_MODEL, odooUserId, Field.User.PARTNER);
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
	 * @param oem        OEM name
	 * @throws OpenemsNamedException on error
	 */
	public void registerUser(JsonObject jsonObject, OdooUserRole role, OpenemsOEM.Manufacturer oem)
			throws OpenemsNamedException {
		var emailOpt = JsonUtils.getAsOptionalString(jsonObject, "email");
		if (!emailOpt.isPresent()) {
			throw new OpenemsException("No email specified");
		}
		var email = emailOpt.get().toLowerCase();

		var userFound = OdooUtils.search(this.credentials, Field.User.ODOO_MODEL, //
				new Domain(Field.User.LOGIN, Operator.EQ, email));
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

		var createdUserId = OdooUtils.create(this.credentials, Field.User.ODOO_MODEL, userFields);
		this.sendRegistrationMail(createdUserId, oem);
	}

	/**
	 * Call Odoo api to send registration mail via Odoo.
	 *
	 * @param odooUserId Odoo user id to send the mail
	 * @param oem        OEM name
	 * @throws OpenemsNamedException error
	 */
	private void sendRegistrationMail(int odooUserId, OpenemsOEM.Manufacturer oem) throws OpenemsNamedException {
		this.sendRegistrationMail(odooUserId, null, oem);
	}

	/**
	 * Call Odoo api to send registration mail via Odoo.
	 *
	 * @param odooUserId Odoo user id to send the mail
	 * @param password   password for the user
	 * @param oem        OEM name
	 */
	private void sendRegistrationMail(int odooUserId, String password, OpenemsOEM.Manufacturer oem) {
		try {
			OdooUtils.sendAdminJsonrpcRequest(this.credentials, "/openems_backend/sendRegistrationEmail",
					JsonUtils.buildJsonObject() //
							.add("params", JsonUtils.buildJsonObject() //
									.addProperty("userId", odooUserId) //
									.addProperty("password", password) //
									.addProperty("oem", oem) //
									.build()) //
							.build());
		} catch (OpenemsNamedException e) {
			this.log.warn("Unable to send registration mail for Odoo user id [" + odooUserId + "]", e);
		}
	}

	/**
	 * Call Odoo api to send multiple notification mails via Odoo async.
	 *
	 * @param sentAt   TimeStamp for last_notification field
	 * @param template template to use for mail
	 * @param params   arguments for the template
	 * @return {@link Future} of {@link SuccessResponseAndHeaders}
	 * @throws OpenemsNamedException error
	 */
	public Future<SuccessResponseAndHeaders> sendNotificationMailAsync(ZonedDateTime sentAt, String template,
			JsonElement params) throws OpenemsNamedException {
		return OdooUtils.sendAdminJsonrpcRequestAsync(this.credentials, "/openems_backend/mail/" + template,
				JsonUtils.buildJsonObject() //
						.add("params", JsonUtils.buildJsonObject() //
								.addProperty("sentAt", OdooUtils.DateTime.dateTimeToString(sentAt)) //
								.add("params", params) //
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
			user.setLanguage(language);
		} catch (OpenemsNamedException ex) {
			throw new OpenemsException("Unable to set language [" + language.name() + "] for current user", ex);
		}
	}

	/**
	 * Get latest Setup Protocol from Odoo or empty JsonObject if no protocol is
	 * available.
	 *
	 * @param user     {@link MyUser} the current user
	 * @param edgeName the unique Edge name
	 * @return the Setup Protocol as a JsonObject
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject getSetupProtocolData(MyUser user, String edgeName) throws OpenemsNamedException {
		// build request
		var request = JsonUtils.buildJsonObject() //
				.add("params", JsonUtils.buildJsonObject() //
						.addProperty("edge_name", edgeName) //
						.build()) //
				.build();

		// call odoo api
		return JsonUtils.getAsJsonObject(
				OdooUtils.sendJsonrpcRequest(this.credentials.getUrl() + "/openems_backend/get_latest_setup_protocol",
						"session_id=" + user.getToken(), request).result);
	}

	/**
	 * Get serial number for the given {@link Edge}.
	 *
	 * @param edge the {@link Edge}
	 * @return Serial number or empty {@link Optional}
	 */
	public Optional<String> getSerialNumberForEdge(Edge edge) {
		try {
			var edgeIds = OdooUtils.search(this.credentials, Field.EdgeDevice.ODOO_MODEL, //
					new Domain(Field.EdgeDevice.NAME, Operator.EQ, edge.getId()));
			if (edgeIds.length == 0) {
				return Optional.empty();
			}

			var serialNumberField = OdooUtils.readOne(this.credentials, Field.EdgeDevice.ODOO_MODEL, edgeIds[0],
					Field.EdgeDevice.STOCK_PRODUCTION_LOT_ID);

			var serialNumber = serialNumberField.get(Field.EdgeDevice.STOCK_PRODUCTION_LOT_ID.id());
			if (serialNumber instanceof Object[] && ((Object[]) serialNumber).length > 1) {
				return OdooUtils.getAsOptional(((Object[]) serialNumber)[1], String.class);
			}
			return Optional.empty();
		} catch (OpenemsException ex) {
			this.parent.logInfo(this.log, "Unable to find serial number for Edge [" + edge.getId() + "]");
		}

		return Optional.empty();
	}

	/**
	 * Gets if the given key can be applied to the given app and edge id.
	 *
	 * @param key    the key to be validated
	 * @param edgeId the edgeId the app should get installed
	 * @param appId  the appId of the app that should get installed
	 * @return the Response result as a JsonObject
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject getIsKeyApplicable(String key, String edgeId, String appId) throws OpenemsNamedException {
		var request = JsonUtils.buildJsonObject() //
				.add("params", JsonUtils.buildJsonObject() //
						.addProperty("key", key) //
						.addProperty("edgeId", edgeId) //
						.addPropertyIfNotNull("appId", appId) //
						.build()) //
				.build();

		var result = JsonUtils.getAsJsonObject(OdooUtils.sendAdminJsonrpcRequest(this.credentials,
				"/openems_app_center/is_key_applicable", request).result);
		return result;
	}

	/**
	 * Gets the response to a add install app instance history entry.
	 *
	 * @param key        the key to install the app with
	 * @param edgeId     the id of the edge the app gets installed on
	 * @param appId      the app that gets installed
	 * @param instanceId the instanceId of the create instance
	 * @param userId     the user who added the instance
	 * @return the result as {@link JsonObject}
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject getAddInstallAppInstanceHistory(String key, String edgeId, String appId, UUID instanceId,
			String userId) throws OpenemsNamedException {
		var request = JsonUtils.buildJsonObject() //
				.add("params", JsonUtils.buildJsonObject() //
						.addProperty("key", key) //
						.addProperty("edgeId", edgeId) //
						.addProperty("appId", appId) //
						.addProperty("instanceId", instanceId.toString()) //
						.addPropertyIfNotNull("userId", userId) //
						.build()) //
				.build();

		return JsonUtils.getAsJsonObject(OdooUtils.sendAdminJsonrpcRequest(this.credentials,
				"/openems_app_center/add_install_app_instance_history", request).result);
	}

	/**
	 * Gets the response to add a deinstall app instance history entry.
	 *
	 * @param edgeId     the edge the app gets removed on
	 * @param appId      the appId of the removed instance
	 * @param instanceId the instanceId of the removed instance
	 * @param userId     the user who removed the instance
	 * @return the result as {@link JsonObject}
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject getAddDeinstallAppInstanceHistory(String edgeId, String appId, UUID instanceId, String userId)
			throws OpenemsNamedException {
		var request = JsonUtils.buildJsonObject() //
				.add("params", JsonUtils.buildJsonObject() //
						.addProperty("edgeId", edgeId) //
						.addProperty("appId", appId) //
						.addProperty("instanceId", instanceId.toString()) //
						.addPropertyIfNotNull("userId", userId) //
						.build()) //
				.build();
		return JsonUtils.getAsJsonObject(OdooUtils.sendAdminJsonrpcRequest(this.credentials,
				"/openems_app_center/add_deinstall_app_instance_history", request).result);
	}

	/**
	 * Gets the response to register a key.
	 *
	 * @param edgeId the edgeId the key gets registered on.
	 * @param appId  the appId the key gets registered to.
	 * @param key    the key that gets registered
	 * @param user   the user who registered the key
	 * @return the result as {@link JsonObject}
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject getAddRegisterKeyHistory(String edgeId, String appId, String key, MyUser user)
			throws OpenemsNamedException {
		var request = JsonUtils.buildJsonObject() //
				.add("params", JsonUtils.buildJsonObject() //
						.addProperty("edgeId", edgeId) //
						.addProperty("key", key) //
						.addPropertyIfNotNull("appId", appId) //
						.addProperty("userId", user.getId()) //
						.build()) //
				.build();
		return JsonUtils.getAsJsonObject(OdooUtils.sendAdminJsonrpcRequest(this.credentials,
				"/openems_app_center/add_register_key_history", request).result);
	}

	/**
	 * Gets the response to unregister a key.
	 * 
	 * @param edgeId the edgeId the registered key was assigned to.
	 * @param appId  the appId the registered key was assigned to or null if
	 *               assigned to edge.
	 * @param key    the registered key
	 * @param user   the user who deregistered the key
	 * @return the response result as a {@link JsonObject}
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject getAddUnregisterKeyHistory(String edgeId, String appId, String key, MyUser user)
			throws OpenemsNamedException {
		var request = JsonUtils.buildJsonObject() //
				.add("params", JsonUtils.buildJsonObject() //
						.addProperty("edgeId", edgeId) //
						.addProperty("key", key) //
						.addPropertyIfNotNull("appId", appId) //
						.addProperty("user", user.getId()) //
						.build())
				.build();

		return JsonUtils.getAsJsonObject(OdooUtils.sendAdminJsonrpcRequest(this.credentials,
				"/openems_app_center/add_deregister_key_history", request).result);
	}

	/**
	 * Gets the registered keys to a edge and app.
	 *
	 * @param edgeId the edge the key is registered on
	 * @param appId  the app the key is registered to
	 * @return the result as {@link JsonObject}
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject getRegisteredKeys(String edgeId, String appId) throws OpenemsNamedException {
		var request = JsonUtils.buildJsonObject() //
				.add("params", JsonUtils.buildJsonObject() //
						.addProperty("edgeId", edgeId) //
						.addPropertyIfNotNull("appId", appId) //
						.build()) //
				.build();
		return JsonUtils.getAsJsonObject(OdooUtils.sendAdminJsonrpcRequest(this.credentials,
				"/openems_app_center/get_registered_key", request).result);
	}

	/**
	 * Gets the possible apps to install with this key.
	 *
	 * @param key    the apps of which key
	 * @param edgeId the apps on which edge
	 * @return the result as {@link JsonObject}
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject getPossibleApps(String key, String edgeId) throws OpenemsNamedException {
		var request = JsonUtils.buildJsonObject() //
				.add("params", JsonUtils.buildJsonObject() //
						.addProperty("edgeId", edgeId) //
						.addProperty("key", key) //
						.build()) //
				.build();
		return JsonUtils.getAsJsonObject(OdooUtils.sendAdminJsonrpcRequest(this.credentials,
				"/openems_app_center/get_possible_apps", request).result);
	}

	/**
	 * Gets the installed apps.
	 *
	 * @param edgeId the apps on which edge
	 * @return the result as {@link JsonObject}
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject getInstalledApps(String edgeId) throws OpenemsNamedException {
		var request = JsonUtils.buildJsonObject() //
				.add("params", JsonUtils.buildJsonObject() //
						.addProperty("edgeId", edgeId) //
						.build()) //
				.build();
		return JsonUtils.getAsJsonObject(OdooUtils.sendAdminJsonrpcRequest(this.credentials,
				"/openems_app_center/get_installed_apps", request).result);
	}

	/**
	 * get all alerting settings for specified edge.
	 *
	 * @param edgeId ID of Edge
	 * @return list of alerting settings
	 * @throws OpenemsException on error
	 */
	public List<UserAlertingSettings> getUserAlertingSettings(String edgeId) throws OpenemsException {
		return this.requestUserAlertingSettings(edgeId, null);
	}

	/**
	 * get alerting setting for user and edge.
	 *
	 * @param edgeId ID of Edge
	 * @param userId ID of User
	 * @return {@link AlertingSetting} or {@link null} if no settings are stored
	 * @throws OpenemsException on error
	 */
	public UserAlertingSettings getUserAlertingSettings(String edgeId, String userId) throws OpenemsException {
		var settings = this.requestUserAlertingSettings(edgeId, userId);
		return settings.isEmpty() ? null : settings.get(0);
	}

	private List<UserAlertingSettings> requestUserAlertingSettings(String edgeName, String userLogin)
			throws OpenemsException {
		// Define Fields
		final var fields = Field.getSqlQueryFields(AlertingSetting.class, AlertingSetting.USER_LOGIN,
				AlertingSetting.DEVICE_NAME);

		// Define Domains
		final var edgeUserFilter = new Domain[] { //
				new Domain(AlertingSetting.DEVICE_NAME, Operator.EQ, edgeName), //
				new Domain(AlertingSetting.USER_LOGIN, userLogin == null ? Operator.NE : Operator.EQ, userLogin) //
		};
		// Get all matching Alerting settings
		var alertingSettings = OdooUtils.searchRead(this.credentials, AlertingSetting.ODOO_MODEL, //
				fields, edgeUserFilter);

		// create result list;
		var result = new ArrayList<UserAlertingSettings>(alertingSettings.length);
		for (var setting : alertingSettings) {
			var login = OdooUtils.getAs(AlertingSetting.USER_LOGIN, setting, String.class);
			var deviceName = OdooUtils.getAs(AlertingSetting.DEVICE_NAME, setting, String.class);

			var edgeOfflineDelay = OdooUtils.getAs(AlertingSetting.OFFLINE_DELAY, setting, Integer.class);
			var edgeOfflineNotification = OdooUtils.getAsOrElse(AlertingSetting.OFFLINE_LAST_NOTIFICATION, setting,
					ZonedDateTime.class, null);

			var edgeFaultDelay = OdooUtils.getAs(AlertingSetting.FAULT_DELAY, setting, Integer.class);
			var edgeWarningDelay = OdooUtils.getAs(AlertingSetting.WARNING_DELAY, setting, Integer.class);
			var sumStateNotification = OdooUtils.getAsOrElse(AlertingSetting.SUM_STATE_LAST_NOTIFICATION, setting,
					ZonedDateTime.class, null);

			result.add(new UserAlertingSettings(deviceName, login, null, edgeOfflineDelay, edgeFaultDelay,
					edgeWarningDelay, edgeOfflineNotification, sumStateNotification));
		}

		return result;
	}

	/**
	 * Get all offline-alerting specific settings for edge.
	 *
	 * @param edgeName unique name of Edge
	 * @return List of {@link OfflineEdgeAlertingSetting} or {@link null} if no
	 *         settings are stored
	 * @throws OpenemsException on error
	 */
	public List<OfflineEdgeAlertingSetting> getOfflineAlertingSettings(String edgeName) throws OpenemsException {
		final var fields = new Field[] { AlertingSetting.DEVICE_ODOO_ID, AlertingSetting.USER_ODOO_ID,
				AlertingSetting.OFFLINE_DELAY, AlertingSetting.OFFLINE_LAST_NOTIFICATION, AlertingSetting.DEVICE_NAME,
				AlertingSetting.USER_LOGIN };

		final var filter = new Domain[] { //
				new Domain(AlertingSetting.DEVICE_NAME, Operator.EQ_I_LIKE, edgeName) };

		var alertingSettings = OdooUtils.searchRead(this.credentials, AlertingSetting.ODOO_MODEL, //
				fields, filter);

		var result = new ArrayList<OfflineEdgeAlertingSetting>(alertingSettings.length);
		for (var setting : alertingSettings) {
			var delay = OdooUtils.getAs(AlertingSetting.OFFLINE_DELAY, setting, Integer.class);
			var lastNotification = OdooUtils.getAsOrElse(AlertingSetting.OFFLINE_LAST_NOTIFICATION, setting,
					ZonedDateTime.class, null);

			var deviceName = OdooUtils.getAs(AlertingSetting.DEVICE_NAME, setting, String.class);
			var userLogin = OdooUtils.getAs(AlertingSetting.USER_LOGIN, setting, String.class);

			if (deviceName == null || userLogin == null) {
				this.log.error(
						"Alerting settings for Device:" + deviceName + " and User:" + userLogin + " are invalid!!");
				continue;
			}

			result.add(new OfflineEdgeAlertingSetting(deviceName, userLogin, delay, lastNotification));
		}
		return result;
	}

	/**
	 * Get all sum-state-alerting specific settings for edge.
	 *
	 * @param edgeName unique name of Edge
	 * @return List of {@link SumStateAlertingSetting} or {@link null} if no
	 *         settings are stored
	 * @throws OpenemsException on error
	 */
	public List<SumStateAlertingSetting> getSumStateAlertingSettings(String edgeName) throws OpenemsException {
		final var fields = new Field[] { AlertingSetting.DEVICE_ODOO_ID, AlertingSetting.USER_ODOO_ID,
				AlertingSetting.FAULT_DELAY, AlertingSetting.WARNING_DELAY, AlertingSetting.SUM_STATE_LAST_NOTIFICATION,
				AlertingSetting.DEVICE_NAME, AlertingSetting.USER_LOGIN };

		final var filter = new Domain[] { //
				new Domain(AlertingSetting.DEVICE_NAME, Operator.EQ, edgeName) };

		var alertingSettings = OdooUtils.searchRead(this.credentials, AlertingSetting.ODOO_MODEL, //
				fields, filter);

		var result = new ArrayList<SumStateAlertingSetting>(alertingSettings.length);
		for (var setting : alertingSettings) {
			var faultDelay = OdooUtils.getAs(AlertingSetting.FAULT_DELAY, setting, Integer.class);
			var warningDelay = OdooUtils.getAs(AlertingSetting.WARNING_DELAY, setting, Integer.class);
			var lastNotification = OdooUtils.getAsOrElse(AlertingSetting.SUM_STATE_LAST_NOTIFICATION, setting,
					ZonedDateTime.class, null);

			var deviceName = OdooUtils.getAs(AlertingSetting.DEVICE_NAME, setting, String.class);
			var userLogin = OdooUtils.getAs(AlertingSetting.USER_LOGIN, setting, String.class);

			result.add(new SumStateAlertingSetting(deviceName, userLogin, faultDelay, warningDelay, lastNotification));
		}
		return result;
	}

	/**
	 * Update or create the alerting setting for the given edge und list of users.
	 *
	 * @param user                 the current user
	 * @param edgeId               the Edge
	 * @param userAlertingSettings list of users
	 * @throws OpenemsException on error
	 */
	public void setUserAlertingSettings(MyUser user, String edgeId, List<UserAlertingSettings> userAlertingSettings)
			throws OpenemsException {

		// search edge by id
		var edgeIds = OdooUtils.search(this.credentials, Field.EdgeDevice.ODOO_MODEL,
				new Domain(Field.EdgeDevice.NAME, Operator.EQ, edgeId));
		if (edgeIds.length != 1) {
			throw new OpenemsException("Unable to find edge [" + edgeId + "]");
		}
		var deviceId = edgeIds[0];

		for (var setting : userAlertingSettings) {

			var alertingSettings = OdooUtils.search(this.credentials, AlertingSetting.ODOO_MODEL,
					new Domain(AlertingSetting.DEVICE_ODOO_ID, Operator.EQ, deviceId),
					new Domain(AlertingSetting.USER_LOGIN, Operator.EQ, setting.userLogin()));

			if (alertingSettings.length > 0) {
				// update found record
				OdooUtils.write(this.credentials, AlertingSetting.ODOO_MODEL, new Integer[] { alertingSettings[0] },
						new FieldValue<>(AlertingSetting.OFFLINE_DELAY, setting.edgeOfflineDelay()),
						new FieldValue<>(AlertingSetting.FAULT_DELAY, setting.edgeFaultDelay()),
						new FieldValue<>(AlertingSetting.WARNING_DELAY, setting.edgeWarningDelay()));
			} else {
				OdooUtils.create(this.credentials, AlertingSetting.ODOO_MODEL, //
						new FieldValue<>(AlertingSetting.DEVICE_ODOO_ID, deviceId), //
						new FieldValue<>(AlertingSetting.USER_ODOO_ID, user.getOdooId()), //
						new FieldValue<>(AlertingSetting.OFFLINE_DELAY, setting.edgeOfflineDelay()), //
						new FieldValue<>(AlertingSetting.FAULT_DELAY, setting.edgeFaultDelay()), //
						new FieldValue<>(AlertingSetting.WARNING_DELAY, setting.edgeWarningDelay()), //
						new FieldValue<>(AlertingSetting.OFFLINE_LAST_NOTIFICATION,
								setting.lastEdgeOfflineNotification()), //
						new FieldValue<>(AlertingSetting.SUM_STATE_LAST_NOTIFICATION,
								setting.lastSumStateNotification()));
			}
		}
	}

	/**
	 * Gets the Edges of the given user matching the {@link PaginationOptions}.
	 *
	 * @param user              the current {@link MyUser}
	 * @param paginationOptions the {@link PaginationOptions}
	 * @return the edges
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject getEdges(MyUser user, PaginationOptions paginationOptions) throws OpenemsNamedException {
		var request = JsonUtils.buildJsonObject() //
				.add("params", JsonUtils.buildJsonObject() //
						.addProperty("page", paginationOptions.getPage()) //
						.addProperty("limit", paginationOptions.getLimit()) //
						.add("query", JsonUtils.getAsJsonElement(paginationOptions.getQuery()))
						.onlyIf(paginationOptions.getSearchParams() != null,
								b -> b.add("searchParams", paginationOptions.getSearchParams().toJson()))//
						.build()) //
				.build();

		return JsonUtils
				.getAsJsonObject(OdooUtils.sendJsonrpcRequest(this.credentials.getUrl() + "/openems_backend/get_edges",
						"session_id=" + user.getToken(), request).result);
	}

	/**
	 * Gets the edge with the {@link Role} of the user.
	 *
	 * @param user   the current {@link MyUser}
	 * @param edgeId the id of the edge
	 * @return the edge with the role of the user
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject getEdgeWithRole(User user, String edgeId) throws OpenemsNamedException {
		var request = JsonUtils.buildJsonObject() //
				.add("params", JsonUtils.buildJsonObject() //
						.addProperty("edge_id", edgeId) //
						.build()) //
				.build();

		return JsonUtils.getAsJsonObject(
				OdooUtils.sendJsonrpcRequest(this.credentials.getUrl() + "/openems_backend/get_edge_with_role",
						"session_id=" + user.getToken(), request).result);
	}

	/**
	 * Get the SumState of the edge with the given edgeId, via a ODOO-Request.
	 *
	 * @param edgeId to search for
	 * @return sumState as {@link Level}
	 */
	public Level getSumState(String edgeId) throws OpenemsException {
		// Define Fields
		var fields = new Field[] { Field.EdgeDevice.OPENEMS_SUM_STATE };
		// Define Domains
		var filter = new Domain[] { new Domain(Field.EdgeDevice.NAME, Operator.EQ, edgeId) };
		// Get all matching Edge Users
		var edgeLevel = OdooUtils.searchRead(this.credentials, Field.EdgeDevice.ODOO_MODEL, //
				fields, filter);

		if (edgeLevel.length == 1) {
			return OdooUtils.getAsEnum(EdgeDevice.OPENEMS_SUM_STATE, edgeLevel[0], Level.class, Level.OK);
		}
		throw new OpenemsException("Expected 1 Edge as response, got: " + edgeLevel.length);
	}

}
