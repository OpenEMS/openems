package eu.chargetime.ocpp;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import eu.chargetime.ocpp.model.CallErrorMessage;
import eu.chargetime.ocpp.model.CallMessage;
import eu.chargetime.ocpp.model.CallResultMessage;
import eu.chargetime.ocpp.model.Message;

/*
ChargeTime.eu - Java-OCA-OCPP
Copyright (C) 2015-2016 Thomas Volden <tv@chargetime.eu>

MIT License

Copyright (C) 2016-2018 Thomas Volden

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

/** Communicator for JSON messages */
public class JSONCommunicator extends Communicator {

  private static final Logger logger = LoggerFactory.getLogger(JSONCommunicator.class);

  private static final int INDEX_MESSAGEID = 0;
  private static final int TYPENUMBER_CALL = 2;
  private static final int INDEX_CALL_ACTION = 2;
  private static final int INDEX_CALL_PAYLOAD = 3;

  private static final int TYPENUMBER_CALLRESULT = 3;
  private static final int INDEX_CALLRESULT_PAYLOAD = 2;

  private static final int TYPENUMBER_CALLERROR = 4;
  private static final int INDEX_CALLERROR_ERRORCODE = 2;
  private static final int INDEX_CALLERROR_DESCRIPTION = 3;
  private static final int INDEX_CALLERROR_PAYLOAD = 4;

  private static final int INDEX_UNIQUEID = 1;
  private static final String CALL_FORMAT = "[2,\"%s\",\"%s\",%s]";
  private static final String CALLRESULT_FORMAT = "[3,\"%s\",%s]";
  private static final String CALLERROR_FORMAT = "[4,\"%s\",\"%s\",\"%s\",%s]";
  private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
  private static final String DATE_FORMAT_WITH_MS = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
  private static final int DATE_FORMAT_WITH_MS_LENGTH = 24;

  private boolean hasLongDateFormat = false;

  /**
   * Handle required injections.
   *
   * @param radio instance of the {@link Radio}.
   */
  public JSONCommunicator(Radio radio) {
    super(radio);
  }

  @Override
  public <T> T unpackPayload(Object payload, Class<T> type) throws Exception {
    GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(Calendar.class, new CalendarDeserializer());
    Gson gson = builder.create();
    return gson.fromJson(payload.toString(), type);
  }

  @Override
  public Object packPayload(Object payload) {
    GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(GregorianCalendar.class, new CalendarSerializer());
    Gson gson = builder.create();
    return gson.toJson(payload);
  }

  private class CalendarSerializer implements JsonSerializer<Calendar> {
    public JsonElement serialize(Calendar src, Type typeOfSrc, JsonSerializationContext context) {
      SimpleDateFormat formatter =
          new SimpleDateFormat(hasLongDateFormat ? DATE_FORMAT_WITH_MS : DATE_FORMAT);
      formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
      return new JsonPrimitive(formatter.format(src.getTime()));
    }
  }

  private class CalendarDeserializer implements JsonDeserializer<Calendar> {
    public Calendar deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      try {
        String dateString = json.getAsJsonPrimitive().getAsString();
        return GregorianCalendar.from(ZonedDateTime.parse(dateString));
      } catch (DateTimeParseException e) {
        throw new JsonParseException(e);
      }
    }
  }

  @Override
  protected Object makeCallResult(String uniqueId, String action, Object payload) {
    return String.format(CALLRESULT_FORMAT, uniqueId, payload);
  }

  @Override
  protected Object makeCall(String uniqueId, String action, Object payload) {
    return String.format(CALL_FORMAT, uniqueId, action, payload);
  }

  @Override
  protected Object makeCallError(
      String uniqueId, String action, String errorCode, String errorDescription) {
    return String.format(CALLERROR_FORMAT, uniqueId, errorCode, errorDescription, "{}");
  }

  @Override
  protected Message parse(Object json) {
    Message message;
    JsonParser parser = new JsonParser();
    JsonArray array = parser.parse(json.toString()).getAsJsonArray();

    if (array.get(INDEX_MESSAGEID).getAsInt() == TYPENUMBER_CALL) {
      message = new CallMessage();
      message.setAction(array.get(INDEX_CALL_ACTION).getAsString());
      message.setPayload(array.get(INDEX_CALL_PAYLOAD).toString());
    } else if (array.get(INDEX_MESSAGEID).getAsInt() == TYPENUMBER_CALLRESULT) {
      message = new CallResultMessage();
      message.setPayload(array.get(INDEX_CALLRESULT_PAYLOAD).toString());
    } else if (array.get(INDEX_MESSAGEID).getAsInt() == TYPENUMBER_CALLERROR) {
      message = new CallErrorMessage();
      ((CallErrorMessage) message).setErrorCode(array.get(INDEX_CALLERROR_ERRORCODE).getAsString());
      ((CallErrorMessage) message)
          .setErrorDescription(array.get(INDEX_CALLERROR_DESCRIPTION).getAsString());
      ((CallErrorMessage) message).setRawPayload(array.get(INDEX_CALLERROR_PAYLOAD).toString());
    } else {
      logger.error("Unknown message type of message: {}", json.toString());
      throw new IllegalArgumentException("Unknown message type");
    }

    message.setId(array.get(INDEX_UNIQUEID).getAsString());

    return message;
  }
}
