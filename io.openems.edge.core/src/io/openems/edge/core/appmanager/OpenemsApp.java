package io.openems.edge.core.appmanager;

import org.osgi.service.component.ComponentConstants;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.validator.Validator;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

public interface OpenemsApp {

	/**
	 * Gets the {@link AppAssistant} for this {@link OpenemsApp}.
	 *
	 * @param language the language of the {@link AppAssistant}
	 * @return the AppAssistant
	 */
	public AppAssistant getAppAssistant(Language language);

	/**
	 * Gets the {@link AppConfiguration} needed for the {@link OpenemsApp}.
	 *
	 * @param target   the {@link ConfigurationTarget}
	 * @param config   the configured app 'properties'
	 * @param language the language of the configuration
	 * @return the app Configuration
	 */
	public AppConfiguration getAppConfiguration(ConfigurationTarget target, JsonObject config, Language language)
			throws OpenemsNamedException;

	/**
	 * Gets the unique App-ID of the {@link OpenemsApp}.
	 *
	 * @return a unique PID, usually the {@link ComponentConstants#COMPONENT_NAME}
	 *         of the OSGi Component provider.
	 */
	public String getAppId();

	/**
	 * Gets the {@link AppDescriptor} of the {@link OpenemsApp}.
	 *
	 * @return the {@link AppDescriptor}
	 */
	public AppDescriptor getAppDescriptor();

	/**
	 * Gets the {@link OpenemsAppCategory} of the {@link OpenemsApp}.
	 *
	 * @return the category's
	 */
	public OpenemsAppCategory[] getCategorys();

	/**
	 * Gets the image of the {@link OpenemsApp} in Base64 encoding.
	 *
	 * @return a image representing the {@link OpenemsApp}
	 */
	public String getImage();

	/**
	 * Gets the name of the {@link OpenemsApp}.
	 *
	 * @param language the language of the name
	 * @return a human readable name
	 */
	public String getName(Language language);

	/**
	 * Gets the {@link OpenemsAppCardinality} of the {@link OpenemsApp}.
	 *
	 * @return the usage
	 */
	public OpenemsAppCardinality getCardinality();

	/**
	 * Gets the {@link Validator} of this {@link OpenemsApp}.
	 *
	 * @return the Validator
	 */
	public ValidatorConfig getValidatorConfig();

	/**
	 * Validate the {@link OpenemsApp}.
	 *
	 * @param instance the app instance
	 */
	public void validate(OpenemsAppInstance instance) throws OpenemsNamedException;

	public static final String FALLBACK_IMAGE = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAASwAAAEsCAYAAAB5fY5" //
			+ "1AAABhWlDQ1BJQ0MgUHJvZmlsZQAAKM+VkT1Iw1AUhU9TpVIqgu0g4pChOlkQFXGUKBbBQmkrtOpg8tI/aNKQpLg4Cq4FB38Wqw" //
			+ "4uzro6uAqC4A+Io5OToouUeF9SaBEqeOHxPs5753DffYDQqDDN6pkANN02U3FJzOZWxcArfAghiEGEZWYZifRiBl3r655uU93Fe" //
			+ "Bb+V/1q3mKATySeY4ZpE28Qz2zaBud94ggrySrxOfG4SQ0SP3Jd8fiNc9FlgWdGzExqnjhCLBY7WOlgVjI14mniqKrplC9kPVY5" //
			+ "b3HWKjXW6pO/MJTXV9JcpzWCOJaQQBIiFNRQRgU2YrTrpFhI0bnUxT/s+pPkUshVBiPHAqrQILt+8D/4PVurMDXpJYUkoPfFcT5" //
			+ "GgcAu0Kw7zvex4zRPAP8zcKW3/dUGMPtJer2tRY+AgW3g4rqtKXvA5Q4w9GTIpuxKflpCoQC8n9E35YDwLRBc8+bWOsfpA5ChWS" //
			+ "3fAAeHwFiRste7vLuvc25/3nHnB+kHJSZyiKHoIYQAAAAJcEhZcwAADsQAAA7EAZUrDhsAAAxQSURBVHhe7d15jCxVFQfgh+IaJ" //
			+ "YKCigsCCqKJCFGD0SCowYBhiyBGwR0VAm5RSXBFIgYIbiCSKKIR1EBccGFRUXEB1KjgCoobm1GMiLuI6DlT1TJvXr03XdXV3VXV" //
			+ "35f8cm/3Hz0901Onb1XdqrsGAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" //
			+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAOiOjcoWhmKTyL1W5D+RPyzLzR" //
			+ "F6SMGiz7aNPD6yS9k+OnKHyGpuiWThuirytWW5NcIAXRL574Lnx5H7RCZxaOR7kfzGv63lVL3ntvKbyO6ReXhs5NTIdZGq99Y0W" //
			+ "ay+HHlL5BGRNp0Y+VOk6ucuSvLz2jcyc6+IVL2hRUxuOE29K1L1mn3JdyOz9PzIxZGq9zKNnBvZJzKpHSJVr7+IuToyc5+PVL2Z" //
			+ "RUzTDyC/aaper2/JXbJpe17kxkjVz59FfhR5VaQpX/Br55GRxsbZ319pu7Kl2GA3Lrq17Fy2fbdl2U5D7m5/JPKhsj8vuYG9I5K" //
			+ "HAA7MJ2p6aNlS2LpsG2lSsO5ZthTyrFRd25dt303rf+FZkRzZHLz0qBvyuNbZkZOWHo3P9rK2JtvL/zUpWHcqWwpNRliblm3f3b" //
			+ "Fs23Rk5GOR+y496p5XR+ocPG7y/zFkE9WPJgXrL2VL4a9lu4ja/l94aeQ9RbfTDijbcSzy/0eVif5nmhQsH8DtckLi34vuQvp92" //
			+ "bYhzwKeVnQ7r860B9vL2mZesM4rW4ozpovqJ2XakHO6zii6rfhbJOf9/DCSE0JzrtsvI3+MtKHO7uoFZcuaNf+IfKnozk6e9fhd" //
			+ "pOqU5aLlKZEmLoxUvV5fkhNH94i05bJI1c8ZNz+PvDuSB+sfHNmQPO72uMjhkdMj10aqXnNDqTtK+HSk6nUWLa+PTKTppTl3i+x" //
			+ "VdCeWo7x8H9ku74/aut+8b4jcUHQbGf1xl88WH/WXP5eTGHN+UBNZsOps8MdF3lt052L0O4/S5q7gayMnFN3arojkMa8PLj1qbs" //
			+ "/IQZGc8zWOLFh1z3bll1te1zipldvHyv4rIztGxpXF9DNFt7Gq7WPlc1dG8szv4I3+AOMmZxZ3Xd0R1lGRIco5Of+OVP3Oq+Wdk" //
			+ "bblrl5empMFuepnjpK7m111eaTqPa8vb4r0RlZkmJdjIk1O++cB+klmn69PHurIgpUTYt8ayZMqVSYZwXdNFq3eGGLB6tUHsOCa" //
			+ "XAx7WOTDRXdq8kLoN0e2ieSxsZW+U7bMmBEW87J3pO5xoOMjs5z6cE0kjwk9JJKXCI28v2yHwAhrzoyw+qHu6CqPddW9LKYteVb" //
			+ "0BZEHRjaP5HGioVCwYAx1C1aeDWx6VrYt10fyxn/MiREW85Cn3evegeH8sqVdRliwii3Kdlw3RS4quiwyIyzmoe6dGHImO9NhhA" //
			+ "WrqFuwflu2LDgjLOah7i6hZbmmxwgLVlF3hJUTOEHBYi5yyas6FKzpMcKCVeRZvzry2r68QwgLTsFiHprcSG+/sqVdRliwiiYFK" //
			+ "+/3zurqFiAFC1bRpGA9KfLEosuiUrCYh69G8hYudZ1YtrTHCAtWkYsR5K1569olsvw2LywYBYt5+VTZ1pX3XX9j0aVCr0ZMdSlY" //
			+ "zMsnI012C1Pevvh9RZcJ2SWEMfwzMslKQC+LnBsZyrL/bXGWEKYkly+bZOXsfSI/jjxn6RGDp2AxT7mcVhatSdw/cmbkrEjewnj" //
			+ "RGWHBFL0tkvdMn9SzI7mKcy5UUfdupvSEgkUX5AIPbXldJFe7yTUPN8snFowRFkzZVyIvKbqtyAulc0XjvPFfrrSTK0wzALkmf9" //
			+ "fV/QZ4WOTqottZuVT9HkV3LDlnabS8/W1lu6F+m34duaToTt0JkdcW3dadHjk5csXSo+HKRV4fU3THkusuVi0WS0OjjXDcPDTSd" //
			+ "aPi05fkP/Ws5MZT9R7aytmRXSND9e1I1e+9vrwi0ht2CRnHy8t2FnIDOrboTsWBkYsj50V2yycWXBat3lCwGEceA3pE0Z2JPP70" //
			+ "mqI7NXtG8tjZOZHH5hMD0asCVJeCxbg2KdtZyYPlT478cOnR9BwQyd2ovKh6lkW5K4ywGKR7lu0s5QjoUZFcpn7a8qLqnDWflwt" //
			+ "tlU/0lBEWhLz2b17yuNbTIrNY/fnwSJ4ZPXrp0fAZYTFI814b8AuRp0b2j1yWT0xZzsD/RiTvwdUnRlgsvLyl8Q+K7tzljf8eHz" //
			+ "k4Mu339ITIpZE8CUAHKFiMY5LbwExLXuy8YyQXp/hFPjFFeZnPKUW38wY9wjLTfT7qznT/bKSN4zfLZ8Mvz4ae/2Xk65Guy8KVx" //
			+ "5/yIP205BSIgyL5t+mqvCohR6DjOjLSl2LcCys3otUyxJnuR0UYT04MzbOLVX/HNnJBpMuyYFW97/XliEhv2CVkaHIUtHskD9Dn" //
			+ "bZjblmcr85rErsoiNFgKFkOVu9DPiOwUuTKfaFGOSmZ5uRIlBYuhuzyyQySPbX0kn2hJXqTdxWsRjbBgAPISn+dGHhfJOV1tsNz" //
			+ "YjClYLJq8X1Qeh8pb5vw7n5hAXut4SNHtDCMsGKDcpXtkJM8oTsKk0hlSsFhkP4/kKGmSY1s5jSYvnO4KIywYuDy29fai28i+Zc" //
			+ "uUKVhQyLsz5L3zm8iCdfeiyzQpWHC7F0eaXJeY21FXRll2CWFB5F0pmk4IzWXzmTIFC9aWi1N8v+jWcu+ynTcjLFgwp5VtHYu4y" //
			+ "vTMKViwribTHLpSsIywYMH8I3JD0R1bV3YJB03Bgmp548I6chm0jYvuXBlhwQKqW7BujNxadJkWBQuq3aVsx1V3F3JajLBgAT2g" //
			+ "bMf127JlihQsqKZgdZCCBevaNLJ10R2bXcIZULBgXXuXbR1GWDOgYMG6mlwXmJf0dIERFiyQrSK52k4d3478qugyTQoWs3ZY5Nz" //
			+ "Ii5Yedc/ry7aOz5VtFxhhQUsOiJwayV2uD0SuiOwf6YrHRA4turV0qWANmoLFLO1RtiO5VmCuznx6ZPN8Yo62j3y86Nbys0iT29" //
			+ "FMixEWtCRHMFVeGLkmckxkHrcazmL1+ci2S4/qOaFsmQEFi1m6R9lWuWskl8zKwnV8ZLvILOQCFN+MNClWuSuYo8MuMcKClmyoY" //
			+ "I3kbVpeF7kq8olI3TN248pR1VmRD0ea3hrmqLJlRjYq2y6r+43xsMjVRbezLoysPJ6zITniyIPV+bcYZZ5yxeTfF91ach3AXMev" //
			+ "rj9Fzo6cE/lG5J+Rpp4eycUm9lt61FyeTTyu6HbKBZFc2XpcR0ZOKbq0YflGOk6abBCzlgWr6r33KVk86spl4qteq24ui7wzclB" //
			+ "k18jOkfyiun8kR3H3izw8sktkz8ibI7n7lkW26vXq5qJIV50fqXrP68sREVpU9UfeUBSs2eU1kTo+Gql6nT7l65Fxdm3nZdAFyz" //
			+ "EsJrFX2Y7ri2XbVzmjPeeQ/XXpUTdlERqsPhSsQX8APbdl2Y7rjMglRbd3Lo9ksbpp6RFzYYTFJJrsGuXUheuLbm/khc15B4ffL" //
			+ "T3qNiOsORviBzCUb+kmZ5nzgHUeDG9y0H4e8jhdnlm8bukRc2WENR95OccQ/Lls68qN/5mRQyJduqxluYsjO0VOWnrUH0ZYczbE" //
			+ "D+CnZdt3dVeWWenMSE5JyMKV86u6IL9Mjo7sFsnjVlDLlZEsWuOmLwtavidS9f77lMMjbcrilfOr8vKcqp83zeRF2E3uNNo1J0e" //
			+ "qfr/15eAILcqJf9dGqv7Yy5PrwvVtEtzzI9+K/CHyrw7mlvUkdwWb3Nmgjt0jWdR/Eqn6vCfNfyJ5DeGxkW0iQ/GgSM52r/qdly" //
			+ "evFujdDPc+XJozkrcfyeu/st0icudIFqlM7pq44+Nw5eedB+ozOYM9Z7Vnxl07MItvfinkDPlLyzaTRWuo8m+zY2S0vWwWGW0vu" //
			+ "WBG3ousd/pUsGClHBnl1IrcOEfJ0UOOAG8uk/1Jrj0EAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" //
			+ "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" //
			+ "AAAAAAAAAAABoyZo1/wOlH2CkLUZ0qwAAAABJRU5ErkJggg==";
}
