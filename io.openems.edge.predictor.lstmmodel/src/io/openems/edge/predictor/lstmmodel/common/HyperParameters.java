package io.openems.edge.predictor.lstmmodel.common;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;

public class HyperParameters implements Serializable {

	private OffsetDateTime lastTrainedDate;

	public OffsetDateTime getLastTrainedDate() {
		return this.lastTrainedDate;
	}

	/**
	 * Serializable class version number for ensuring compatibility during
	 * serialization.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Maximum iteration factor.
	 * 
	 * <p>
	 * This value is used by DynamicItterationValue class to set the
	 * gdItterationValue DynamicItterationValue class changes the classes such that
	 * the gdItteration value is in between 1 and maxItterFactor*current Epoch value
	 * +1 When epoch increase, learning rate decreases and the gdItteration value
	 * increases. Set the value always to 10.
	 * </p>
	 */
	private final int maxItterFactor = 10;

	/**
	 * Upper limit for the learning rate.
	 * 
	 * <p>
	 * This value is used by the ADGRAD optimizer as the initial learning rate. The
	 * optimizer dynamically adjusts the learning rate over epochs, starting with
	 * the value of learningRateUpperLimit. The adjustment is typically aimed at
	 * improving convergence by starting with a higher learning rate and gradually
	 * decreasing it.
	 * 
	 * </p>
	 * This variable can be set to any value between 0 and 1. It is important to
	 * ensure that the value of learningRateUpperLimit is always greater than
	 * learnignRateLowerLimit to allow proper functioning of the dynamic learning
	 * rate setup. Default value: 0.01
	 */
	private double learningRateUpperLimit = 0.01;

	/**
	 * Lower limit for the learning rate.
	 * 
	 * <p>
	 * This value is used by the ADGRAD optimizer as the minimum learning rate. As
	 * the training progresses, the optimizer adjusts the learning rate and it
	 * converges to the value of learnignRateLowerLimit by the final epoch. This
	 * helps in fine-tuning the model parameters and achieving better accuracy by
	 * the end of the training.
	 * </p>
	 * This variable can be set to any value between 0 and 1. It is crucial that the
	 * value of learnignRateLowerLimit is always less than learningRateUpperLimit to
	 * enable the proper decreasing trend of the learning rate throughout the
	 * training process. Default value: 0.0001
	 */
	private double learnignRateLowerLimit = 0.0001;

	/**
	 * Proportion of data to be used for training.
	 * 
	 * <p>
	 * This variable determines the fraction of the entire dataset that will be
	 * allocated for training purposes. The remaining portion of the dataset will be
	 * used for validation. The value of this variable should be within the range of
	 * 0 to 1, where:
	 * <ul>
	 * <li>0 means 0% of the dataset is used for training (i.e., no training
	 * data).</li>
	 * <li>1 means 100% of the dataset is used for training (i.e., no validation
	 * data).</li>
	 * </ul>
	 * </p>
	 * The program utilizes this variable to split the input dataset vector into two
	 * separate vectors. One vector contains the training data, and the other vector
	 * contains the validation data. The split is essential for assessing the
	 * performance of the model on unseen data, helping to prevent overfitting and
	 * to ensure the model's generalizability.
	 */
	private double dataSplitTrain = 0.7;

	/**
	 * Proportion of data to be used for validation.
	 */
	private double dataSplitValidate = 1 - this.dataSplitTrain;

	private double wiInit = 0.2;
	private double woInit = 0.2;
	private double wzInit = 0.2;
	private double riInit = 0.2;
	private double roInit = 0.2;
	private double rzInit = 0.2;
	private double ytInit = 0.2;
	private double ctInit = 0.2;

	/**
	 * Interval for logging or updating parameters.
	 */
	private int interval = 5;

	/**
	 * Size of each batch for training.
	 * 
	 * <p>
	 * To manage the computational load on the CPU during training, the training
	 * data is divided into smaller subsets called batches.
	 * </p>
	 * 
	 * <p>
	 * For our LSTM (Long Short-Term Memory) model, a general rule of thumb is that
	 * datasets consisting of 30 days of data with 5-minute intervals should not be
	 * divided into batches greater than 2. This helps to balance the computational
	 * load and the memory usage during training.
	 * </p>
	 * 
	 * <p>
	 * Considerations for setting the batch size:
	 * <ul>
	 * <li>If the training data size is large, more batches should be created to
	 * avoid excessive memory usage, which could lead to heap memory errors.</li>
	 * <li>If the training data size is small, fewer batches should be created to
	 * ensure each batch contains a sufficient number of samples for meaningful
	 * updates. Creating too many batches with too few samples can lead to index out
	 * of range errors during training.</li>
	 * </ul>
	 */
	private int batchSize = 10;

	/**
	 * Counter for tracking batches.
	 *
	 * <p>
	 * This counter keeps track of the number of batches that have passed through
	 * the training process.
	 * </p>
	 * - It updates after each batch completes its training. - In case the training
	 * is interrupted, this counter allows the process to resume from the last
	 * completed batch, ensuring continuity and efficiency in the training process.
	 * 
	 * <p>
	 * This mechanism is crucial for maintaining the state of the training process,
	 * especially in scenarios where interruptions may occur.
	 * </p>
	 */
	private int batchTrack = 0;

	/**
	 * Number of epochs for training.
	 * 
	 * <p>
	 * An epoch refers to one complete pass through the entire training dataset.
	 * During each epoch, the model processes all the training data in batches,
	 * updating the model parameters iteratively. After each epoch, the learning
	 * rate can be adjusted, and the training process continues on the same dataset.
	 * </p>
	 * 
	 * <p>
	 * The number of epochs is a crucial hyperparameter in training neural networks.
	 * More epochs generally mean that the model has more opportunities to learn
	 * from the data, potentially improving its performance. However, more epochs
	 * also mean longer training times and a higher risk of overfitting, where the
	 * model learns the training data too well and performs poorly on new, unseen
	 * data.
	 * </p>
	 * 
	 * <p>
	 * It is recommended to keep the number of epochs in the range of 30 to 50 for a
	 * balanced approach between training time and model performance. Adjusting the
	 * number of epochs can be necessary based on the specific characteristics of
	 * the dataset and the complexity of the model.
	 */
	private int epoch = 10;

	/**
	 * Counter for tracking epochs. The counter updates after every time all batches
	 * undergoes training. This value is searilized along with the weights. in case
	 * training stops, this record is used to resme the training from the last stop
	 * point.
	 */
	private int epochTrack = 0;

	/**
	 * Number of predictions using trend weights.
	 * 
	 * <p>
	 * This parameter determines the number of predictions made based on the trend
	 * weights derived from the most recent trend window data. The trend window is a
	 * specific period used to analyze the trend patterns of the data.
	 * 
	 * </p>
	 * 
	 * <p>
	 * 
	 * By default, one prediction is made using the last trend window data if this
	 * value is set to 1. This means that the system will use the data from the last
	 * trend window to make a single prediction.
	 * 
	 * </p>
	 * It is advisable to set this value to 12 if the interval between data points
	 * is 5 minutes Similarly, set this value to 8 if the interval between data
	 * points is 15 minutes . The interval represents the time or sequence gap
	 * between consecutive data points being analyzed.
	 * 
	 * <p>
	 * Setting a higher value than recommended can lead to inaccuracies in the
	 * prediction. This is because too many trend points may cause the model
	 * misinterpret the trend patterns, resulting in errors.
	 * </p>
	 */
	private int trendPoints = 12;

	/**
	 * Window size for analyzing seasonality.
	 * 
	 * <p>
	 * This parameter defines the window size used for analyzing seasonal patterns
	 * in the data. A window size of 7 means that the model will use data from the
	 * last 7 days to train at one instance. Additionally, it will utilize the data
	 * from the last 7 days to predict data points for the next 24 hours.
	 * </p>
	 * 
	 * <p>
	 * The window size can be adjusted up to a maximum of 14. While increasing the
	 * window size can potentially provide more accurate seasonal insights, it also
	 * increases the computational load.
	 * </p>
	 * 
	 * <p>
	 * Key points: - Set to 7 to use the last 7 days of data for training and for
	 * predicting the next 24 hours. - The value can be adjusted up to 14. - Be
	 * aware that higher values may be computationally intensive.
	 * </p>
	 */
	private int windowSizeSeasonality = 7;
	/**
	 * Window size for analyzing trend.
	 * 
	 * <p>
	 * This parameter specifies the window size used for analyzing trend patterns in
	 * the data. A window size of 5 means that the model will consider data from the
	 * last 5 time intervals to analyze the trend. This helps in identifying the
	 * direction and strength of the trend over recent time periods. Keep the value
	 * in between 5 to 7
	 * </p>
	 */
	private int windowSizeTrend = 5;

	/**
	 * Number of iterations for gradient descent.
	 * 
	 * <p>
	 * This parameter defines the number of iterations to be performed during the
	 * gradient descent optimization process. Gradient descent is used to minimize
	 * the cost function by iteratively updating the model parameters.
	 * </p>
	 * 
	 * <p>
	 * The number of iterations can be set between 1 and 100. A higher number of
	 * iterations can potentially lead to models with improved accuracy as the
	 * optimization process has more opportunities to converge to a minimum.
	 * However, increasing the number of iterations also increases the computation
	 * time required for training the model.
	 * </p>
	 * 
	 * <p>
	 * Key points: - Set to 10 to perform 10 iterations of gradient descent. - Can
	 * be adjusted between 1 and 100 based on the trade-off between accuracy and
	 * computation time. - Higher values may improve model accuracy but will also
	 * increase computation time.
	 * </p>
	 */
	private int gdIterration = 10;

	/**
	 * Counter for general tracking purposes.
	 * 
	 * <p>
	 * This counter is used to determine whether the training process is being
	 * executed for the first time.
	 * </p>
	 * 
	 * <p>
	 * - If the count is 0, the algorithm will use the initial weights and start a
	 * new training process. - If the count value is greater than 0, the algorithm
	 * will continue training the existing models.
	 * </p>
	 * 
	 * <p>
	 * This mechanism ensures that the model can distinguish between initializing
	 * new training sessions and performing subsequent training iterations.
	 * </p>
	 * 
	 * <p>
	 * Note: Just like in programming, remember that if you start counting from 0,
	 * you're a true computer scientist!
	 * </p>
	 */
	private int count = 0;

	/**
	 * Threshold error value.
	 * 
	 * <p>
	 * This value represents the threshold error, typically measured in the same
	 * units as the training data. It can also be considered as the allowed error
	 * margin. The Root Mean Square (RMS) error computed during the model evaluation
	 * reflects the average deviation from this threshold value.
	 * </p>
	 * 
	 * <p>
	 * Key points: - Measured in the same units as the training data. - Represents
	 * the acceptable error margin. - RMS error indicates the average deviation from
	 * this threshold.
	 * </p>
	 */

	private double targetError = 0;

	/**
	 * Minimum value for scaling data.
	 * 
	 * <p>
	 * This value defines the minimum threshold for scaling the data. It should
	 * always be less than the `scalingMax` value. The unit of this value is the
	 * same as that of the training data. this valve can be negative and positive it
	 * id
	 * </p>
	 * 
	 * <p>
	 * Once set, it is important not to change this value, as it could affect the
	 * consistency of the scaling process.
	 * </p>
	 * 
	 * <p>
	 * Note: value once set should not be changed, as changing it is as risky as
	 * debugging a program on a Friday afternoon!
	 * </p>
	 */
	private double scalingMin = 0;

	/**
	 * Maximum value for scaling data.
	 * 
	 * <p>
	 * This value defines the maximum threshold for scaling the data. It should
	 * always be greater than the `scalingMin` value. The unit of this value is the
	 * same as that of the training data. This value can be positive or negative,
	 * depending on the data range.
	 * </p>
	 * 
	 * <p>
	 * Once set, it is important not to change this value, as it could affect the
	 * consistency of the scaling process.
	 * </p>
	 * 
	 * <p>
	 * Note: Setting this value high is like aiming for the stars with your data!
	 * Just remember, changing it later could be as risky as giving a programmer, a
	 * cup of coffee after midnight!
	 * </p>
	 * 
	 */
	private double scalingMax = 20000;

	/**
	 * Model data structure for trend analysis.
	 * 
	 * <p>
	 * This is the brain of the model, responsible for storing updated weights and
	 * biases during the training process for trend analysis.
	 * </p>
	 * 
	 * <p>
	 * The structure comprises nested arrays to store weights and biases:
	 * </p>
	 * 
	 * <pre>
	 * [ [wi1,wi2, wi3, ..., wik], 
	 *   [wo1, wo2, wo3, ..., wok], 
	 *   [wz1, wz2, wz3, ..., wzk],
	 *   [Ri1, Ri2, Ri3, ..., Rik], 
	 *   [Ro1, Ro2, Ro3, ..., Rok], 
	 *   [Rz1, Rz2, Rz3, ..., Rzk], 
	 *   [Yt1, Yt2, Yt3, ..., Ytk], 
	 *   [Ct1, Ct2, Ct3, ..., Ctk] ]
	 * 
	 * </pre>
	 * 
	 * <p>
	 * Where Wi, Wo, Wz, Ri, Ro, Rz, Yt, and Ct are the weights and biases of the
	 * LSTM cells, and 1, 2, 3, ..., k represent the window size.
	 * </p>
	 * 
	 * <p>
	 * The first two nested arrays ensure that the second nested array is available
	 * for every time depending on the interval. first element of second nested
	 * array is used for the prediction of the trend point for 00:05 (if the
	 * interval is 5)
	 * </p>
	 * 
	 * <p>
	 * Fun Fact: This data structure holds the keys to predicting trends better than
	 * a psychic octopus predicting World Cup winners!
	 * </p>
	 */
	private ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> modelTrend = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();

	/**
	 * Model data structure for seasonality analysis.
	 * 
	 * <p>
	 * This data structure serves as the backbone of the model, specifically
	 * designed to store updated weights and biases during the training process for
	 * seasonality analysis.
	 * </p>
	 * 
	 * <p>
	 * The structure consists of nested ArrayLists to accommodate the weights and
	 * biases.
	 * </p>
	 * 
	 * <pre>
	 * [ [wi1,wi2, wi3, ..., wik], 
	 *   [wo1, wo2, wo3, ..., wok], 
	 *   [wz1, wz2, wz3, ..., wzk],
	 *   [Ri1, Ri2, Ri3, ..., Rik], 
	 *   [Ro1, Ro2, Ro3, ..., Rok], 
	 *   [Rz1, Rz2, Rz3, ..., Rzk], 
	 *   [Yt1, Yt2, Yt3, ..., Ytk], 
	 *   [Ct1, Ct2, Ct3, ..., Ctk] ]
	 * 
	 * </pre>
	 * 
	 * <p>
	 * Where Wi, Wo, Wz are the weights of the LSTM cells, and 1, 2, 3, ..., k
	 * represent the window size.
	 * </p>
	 * 
	 * <p>
	 * The first two nested arrays ensure that the second nested array is available
	 * for every time depending on the interval. first element of second nested
	 * array is used for the prediction of the trend point for 00:00 (if the
	 * interval is 5)
	 * </p>
	 * 
	 * <p>
	 * Fun Fact: With this data structure, our model can predict seasonal pattern
	 * more accurately than a fortune-teller!
	 * </p>
	 */
	private ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> modelSeasonality = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();

	/**
	 * List of all model errors related to trend analysis.
	 * 
	 * <p>
	 * This vector holds the Root Mean Square (RMS) errors of different models
	 * recorded during multiple training steps in modelTrend.
	 * 
	 * </p>
	 * 
	 * <p>
	 * Fun Fact: These errors are like the turn signals on a BMW - sometimes they're
	 * there, sometimes they're not, but they always keep us guessing and learning
	 * along the way!
	 * </p>
	 */
	private ArrayList<Double> allModelErrorTrend = new ArrayList<Double>();

	/**
	 * List of all model errors related to seasonality analysis.
	 * 
	 * <p>
	 * This vector contains the Root Mean Square (RMS) errors of different models
	 * recorded during multiple training steps in modelSeasonality.
	 * </p>
	 * 
	 * <p>
	 * Fun Fact: These errors are like the various recipes for currywurst - some may
	 * be a bit spicier than others, but they all add flavor to our models, just
	 * like currywurst adds flavor to German cuisine!
	 * </p>
	 */
	private ArrayList<Double> allModelErrorSeasonality = new ArrayList<Double>();

	/**
	 * Mean value for normalization or scaling purposes.
	 * 
	 * <p>
	 * This value is crucial for ensuring proper normalization or scaling of the
	 * data. It acts as the central point around which the data is normalized or
	 * scaled.
	 * 
	 * </p>
	 * 
	 * <p>
	 * It's important to set this value to 0, just like it's important to feed your
	 * girlfriend when she's hungry, because, trust me, she can be mean when hungry!
	 * </p>
	 */
	private double mean = 0;

	/**
	 * Standard deviation for normalization or scaling purposes.
	 * 
	 * <p>
	 * This value plays a crucial role in determining the spread or dispersion of
	 * the data during normalization or scaling.
	 * </p>
	 */
	private double standerDeviation = 1;

	/**
	 * Root Mean Square Error (RMSE) for trend analysis.
	 * 
	 * <p>
	 * This list contains RMSE values for trend analysis. Unlike
	 * 'allModelErrorTrend', this list is limited in size to accommodate 60 divided
	 * by the interval multiplied by 24, and each value represents the RMSE of the
	 * model predicting for a specific time interval.
	 * 
	 * </p>
	 * The error at index 0 corresponds to the model predicting for 00:05, with
	 * subsequent indices representing subsequent time intervals.
	 */
	private ArrayList<Double> rmsErrorTrend = new ArrayList<Double>();

	/**
	 * Root Mean Square Error (RMSE) for seasonality analysis.
	 * 
	 * <p>
	 * This list contains RMSE values for seasonality analysis. Each value
	 * represents the RMSE of the model's predictions related to seasonality.
	 * </p>
	 */
	private ArrayList<Double> rmsErrorSeasonality = new ArrayList<Double>();

	/**
	 * Counter for outer loop iterations, possibly for nested loops. Note: only used
	 * in unit test case
	 */
	private int outerLoopCount = 0;

	/**
	 * Name of the model.
	 */
	private String modelName = "";

	public HyperParameters() {
	}

	public void setLearningRateUpperLimit(double rate) {
		this.learningRateUpperLimit = rate;
	}

	public double getLearningRateUpperLimit() {
		return this.learningRateUpperLimit;
	}

	public void setLearningRateLowerLimit(double val) {
		this.learnignRateLowerLimit = val;
	}

	public double getLearningRateLowerLimit() {
		return this.learnignRateLowerLimit;
	}

	public void setWiInit(double val) {
		this.wiInit = val;
	}

	public double getWiInit() {
		return this.wiInit;
	}

	public void setWoInit(double val) {
		this.woInit = val;
	}

	public double getWoInit() {
		return this.woInit;
	}

	public void setWzInit(double val) {
		this.wzInit = val;
	}

	public double getWzInit() {
		return this.wzInit;
	}

	public void setriInit(double rate) {
		this.riInit = rate;
	}

	public double getRiInit() {
		return this.riInit;
	}

	public void setRoInit(double val) {
		this.roInit = val;
	}

	public double getRoInit() {
		return this.roInit;
	}

	public void setRzInit(double val) {
		this.rzInit = val;
	}

	public double getRzInit() {
		return this.rzInit;
	}

	public void setYtInit(double val) {
		this.ytInit = val;
	}

	public double getYtInit() {
		return this.ytInit;
	}

	public void setCtInit(double val) {
		this.ctInit = val;
	}

	public double getCtInit() {
		return this.ctInit;
	}

	public int getWindowSizeSeasonality() {
		return this.windowSizeSeasonality;
	}

	public int getGdIterration() {
		return this.gdIterration;
	}

	public void setGdIterration(int val) {
		this.gdIterration = val;
	}

	public int getWindowSizeTrend() {
		return this.windowSizeTrend;
	}

	public double getScalingMin() {
		return this.scalingMin;
	}

	public double getScalingMax() {
		return this.scalingMax;
	}

	public void setCount(int val) {
		this.count = val;
	}

	public int getCount() {
		return this.count;
	}

	public void setDatasplitTrain(double val) {
		this.dataSplitTrain = val;
	}

	public double getDataSplitTrain() {
		return this.dataSplitTrain;
	}

	public void setDatasplitValidate(double val) {
		this.dataSplitValidate = val;
	}

	public double getDataSplitValidate() {
		return this.dataSplitValidate;
	}

	public int getTrendPoint() {
		return this.trendPoints;
	}

	public int getEpoch() {

		return this.epoch;
	}

	public int getInterval() {
		return this.interval;
	}

	public void setRmsErrorTrend(double val) {
		this.rmsErrorTrend.add(val);
	}

	public void setRmsErrorSeasonality(double val) {
		this.rmsErrorSeasonality.add(val);
	}

	public ArrayList<Double> getRmsErrorSeasonality() {
		return this.rmsErrorSeasonality;
	}

	public ArrayList<Double> getRmsErrorTrend() {
		return this.rmsErrorTrend;
	}

	public void setEpochTrack(int val) {
		this.epochTrack = val;
	}

	public int getEpochTrack() {
		return this.epochTrack;
	}

	public int getMinimumErrorModelSeasonality() {
		return this.rmsErrorSeasonality.indexOf(Collections.min(this.rmsErrorSeasonality));
	}

	public int getMinimumErrorModelTrend() {
		return this.rmsErrorTrend.indexOf(Collections.min(this.rmsErrorTrend));
	}

	public int getOuterLoopCount() {
		return this.outerLoopCount;
	}

	public void setOuterLoopCount(int val) {
		this.outerLoopCount = val;
	}

	public int getBatchSize() {
		return this.batchSize;
	}

	public int getBatchTrack() {
		return this.batchTrack;
	}

	public void setBatchTrack(int val) {
		this.batchTrack = val;
	}

	public void setModelName(String val) {
		this.modelName = val;
	}

	public String getModelName() {
		return this.modelName;
	}

	public double getMean() {
		return this.mean;

	}

	public double getStanderDeviation() {
		return this.standerDeviation;
	}

	public double getTargetError() {
		return this.targetError;
	}

	public void setTargetError(double val) {
		this.targetError = val;
	}

	public int getMaxItter() {
		return this.maxItterFactor;
	}

	/**
	 * Updates the model trend with new values.
	 *
	 * @param val ArrayList of ArrayLists of ArrayLists of Double containing the new
	 *            values to add to the model trend
	 */
	public void updatModelTrend(ArrayList<ArrayList<ArrayList<Double>>> val) {
		this.modelTrend.add(val);
	}

	/**
	 * Retrieves the most recently recorded model trend from the list of model
	 * trends.
	 *
	 * @return The most recently recorded model trend, represented as an ArrayList
	 *         of ArrayLists of ArrayLists of Double.
	 */
	public ArrayList<ArrayList<ArrayList<Double>>> getlastModelTrend() {
		return this.modelTrend.get(this.modelTrend.size() - 1);
	}

	public ArrayList<ArrayList<ArrayList<Double>>> getBestModelTrend() {
		return this.modelTrend.get(this.getMinimumErrorModelTrend());
	}

	public ArrayList<ArrayList<ArrayList<Double>>> getBestModelSeasonality() {
		return this.modelSeasonality.get(this.getMinimumErrorModelSeasonality());
	}

	public ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> getAllModelsTrend() {
		return this.modelTrend;
	}

	public ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> getAllModelSeasonality() {
		return this.modelSeasonality;
	}

	public void setAllModelErrorTrend(ArrayList<Double> val) {
		this.allModelErrorTrend = val;
	}

	public void setAllModelErrorSeason(ArrayList<Double> val) {
		this.allModelErrorSeasonality = val;
	}

	public ArrayList<Double> getAllModelErrorTrend() {
		return this.allModelErrorTrend;
	}

	public ArrayList<Double> getAllModelErrorSeason() {
		return this.allModelErrorSeasonality;
	}

	/**
	 * Retrieves the last model trend from the list of model trends.
	 *
	 * @return ArrayList of ArrayLists of ArrayLists of Double representing the last
	 *         model trend
	 */
	public ArrayList<ArrayList<ArrayList<Double>>> getlastModelSeasonality() {
		return this.modelSeasonality.get(this.modelSeasonality.size() - 1);
	}

	/**
	 * reset the error in the model.
	 */
	public void resetModelErrorValue() {
		this.rmsErrorSeasonality = new ArrayList<Double>();
		this.rmsErrorTrend = new ArrayList<Double>();
	}

	/**
	 * Updates the model seasonality with new values.
	 *
	 * @param val The new model seasonality values to add, represented as an
	 *            ArrayList of ArrayLists of ArrayLists of Double.
	 */
	public void updateModelSeasonality(ArrayList<ArrayList<ArrayList<Double>>> val) {
		this.modelSeasonality.add(val);
	}

	/**
	 * Prints the current values of hyperparameters and related attributes to the
	 * console.
	 */
	public void printHyperParameters() {
		StringBuilder builder = new StringBuilder();

		builder.append("learningRateUpperLimit = ").append(this.learningRateUpperLimit).append("\n");
		builder.append("learnignRateLowerLimit = ").append(this.learnignRateLowerLimit).append("\n");
		builder.append("wiInit = ").append(this.wiInit).append("\n");
		builder.append("woInit = ").append(this.woInit).append("\n");
		builder.append("wzInit = ").append(this.wzInit).append("\n");
		builder.append("riInit = ").append(this.riInit).append("\n");
		builder.append("roInit = ").append(this.roInit).append("\n");
		builder.append("rzInit = ").append(this.rzInit).append("\n");
		builder.append("ytInit = ").append(this.ytInit).append("\n");
		builder.append("ctInit = ").append(this.ctInit).append("\n");
		builder.append("Epoch = ").append(this.epoch).append("\n");
		builder.append("windowSizeSeasonality = ").append(this.windowSizeSeasonality).append("\n");
		builder.append("windowSizeTrend = ").append(this.windowSizeTrend).append("\n");
		builder.append("scalingMin = ").append(this.scalingMin).append("\n");
		builder.append("scalingMax = ").append(this.scalingMax).append("\n");
		builder.append("RMS error trend = ").append(this.getRmsErrorTrend()).append("\n");
		builder.append("RMS error Seasonlality =").append(this.getRmsErrorSeasonality()).append("\n");
		builder.append("Count value = ").append(this.count).append("\n");
		builder.append("Outer loop Count  = ").append(this.outerLoopCount).append("\n");
		builder.append("Epoch track = ").append(this.epochTrack).append("\n");

		System.out.println(builder.toString());
	}

	/**
	 * Updates the models and their corresponding error indices based on the minimum
	 * error values obtained from model trends and model seasonality. This method
	 * first retrieves the indices of models with minimum errors for both trends and
	 * seasonality. Then it retrieves the corresponding models and clears the
	 * existing model trends, model seasonality, RMS errors for trend, and RMS
	 * errors for seasonality. After that, it adds the retrieved models to the
	 * respective model lists and updates the RMS errors with the minimum error
	 * values.
	 */
	public void update() {
		int minErrorIndTrend = this.getMinimumErrorModelTrend();
		int minErrorIndSeasonlity = this.getMinimumErrorModelSeasonality();

		// uipdating models
		var modelTrendTemp = this.modelTrend.get(minErrorIndTrend);
		final var modelTempSeasonality = this.modelSeasonality.get(minErrorIndSeasonlity);
		this.modelTrend.clear();
		this.modelSeasonality.clear();
		this.modelTrend.add(modelTrendTemp);
		this.modelSeasonality.add(modelTempSeasonality);
		// updating index
		double minErrorTrend = this.rmsErrorTrend.get(minErrorIndTrend);
		final double minErrorSeasonality = this.rmsErrorSeasonality.get(minErrorIndSeasonlity);
		this.rmsErrorTrend.clear();
		this.rmsErrorSeasonality.clear();
		this.rmsErrorTrend.add(minErrorTrend);
		this.rmsErrorSeasonality.add(minErrorSeasonality);
		this.count = 1;
		this.lastTrainedDate = OffsetDateTime.now();

	}

}
