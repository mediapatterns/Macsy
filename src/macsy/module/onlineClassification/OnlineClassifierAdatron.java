package macsy.module.onlineClassification;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import macsy.blackBoardsSystem.BBDoc;
import macsy.blackBoardsSystem.BBDocSet;
import macsy.blackBoardsSystem.BlackBoardDateBased;
import macsy.lib.DataPoint;
import macsy.lib.LinearModel;
import macsy.lib.Results;
import macsy.lib.onlineLearning.OnlineAdatron;
import macsy.lib.onlineLearning.OnlineLearning;
import macsy.module.BaseModule;


/**
 * It predicts the score calculated from an already (if exists) stored model for classification
 * procedure and the result of that prediction is stored in the database (under the field - OUTPUT_FIELD).
 * It also separates the documents into two sets (training set and testing set) and updates
 * the given model by using the training set. The training set is also separated into two classes
 * (the positive and the negative). Documents having the field name specified in
 * INPUT_FEATURES_FIELDSNAME are those in the training set and those with values in that field
 * equals to the values in the parameter INPUT_POS_LEARN_FIELDSID are the instances of the
 * positive class and those with values defined by the parameter INPUT_NEG_LEARN_FIELDSID
 * are those in the negative class
 * The prediction and training procedure of the module are based on the adatron algorithm
 * e.g. prediction: <w(t)*x(t)> + b
 * 		if error
 * 			update: w(t+1) = w(t) + eta*(desired_output-real_output)*x(t)
 * 					b(t+1) = b(t) + eta*(desired_output-real_output)
 *
 * where w is the weight vector, x is the input, b is the bias and eta the learning factor
 *
 * Input:
 * INPUT_BLACKBOARD=The name of the BlackBoard that contains the documents of interest.
 * PROCESS_LIMIT=The max number of documents for classification. Set to zero to get all
 * INPUT_TAGS=The module performs the whole procedure only on documents that have this tag
 * INPUT_FIELDS=The field that holds the input data (features) and that we want to
 * apply the online learning of the docs.
 * INPUT_FEATURES_FIELDSNAME=The name of the fields on which the learning is based.
 * INPUT_POS_LEARN_FIELDSID=The ids of the fields belonging to the positive class
 * INPUT_NEG_LEARN_FIELDSID=The ids of the fields belonging to the negative class
 * LEARNING_FACTOR=The eta in the learning process
 * UPDATE_LEARNING_FACTOR=If we wish to update the eta (boolean true/false)
 * DESIRED_PRECISION=If we wish to use the extra bias (-1 if we don't want extra bias)
 * MARGIN_THRESHOLD=The tau in the decision rule for update or not (=0 for simple perceptron)
 * WINDOW=The number of data we wish to remember
 * VOCABULARY=The filename with the words of the language (for the word clouds)
 * MODEL_FILENAME=The name of the model that the module will read (if exists) the weights
 * at the beginning and store them after termination.
 * PERFORMANCE=The boolean flag to specify if we want a large file with the performance
 * of the classifier per document
 *
 * Output:
 * OUTPUT_BLACKBOARD=Output BlackBoard
 * OUTPUT_TAGS=The tags that the module will add in the processed document. The first one
 * is the one with which the INPUT_TAGS will be replaced and the second one is the tag
 * that it will be added only for the documents classified in the positive class.
 * OUTPUT_FIELDS=The fields' names where the module is going to write the predicted value y_hat.
 *
 * The statistical information (per document) such as precision, recall, TP, TN, FP, FN etc. are stored
 * in a file named model_name.performance in order to measure the module's performance
 * and the useful information (per session/run) such as the learning factor, the window, the margin, etc.
 * are also stored in a file named model_name.log in order for the module to continue from the last run.
 *
 * @author Panagiota Antonakaki
 * Last Update: 12-03-2014
 *
 */
public class OnlineClassifierAdatron extends BaseModule {
    // temporal variables to hold information given by N_the user

    static final String PROPERTY_MODEL_FILENAME = "MODEL_FILENAME";
    static final String PROPERTY_LEARNING_FACTOR = "LEARNING_FACTOR";
    static final String PROPERTY_POS_MARGIN = "POS_MARGIN_THRESHOLD";
    static final String PROPERTY_NEG_MARGIN = "NEG_MARGIN_THRESHOLD";
    static final String PROPERTY_MARGIN = "MARGIN_THRESHOLD";
    static final String PROPERTY_WINDOW = "WINDOW";
    static final String PROPERTY_INPUT_FEATURES_FIELDSNAME = "INPUT_FEATURES_FIELDSNAME";
    static final String PROPERTY_POS_LEARNING_FIELDS_VALUES = "INPUT_POS_LEARN_FIELDSID";
    static final String PROPERTY_NEG_LEARNING_FIELDS_VALUES = "INPUT_NEG_LEARN_FIELDSID";
    static final String PROPERTY_DESIRED_PRECISION = "DESIRED_PRECISION";
    static final String PROPERTY_VOCABULARY = "VOCABULARY";
    static final String PROPERTY_UPDATE_LEARNING_FACTOR = "UPDATE_LEARNING_FACTOR";
    static final String PROPERTY_PERFORMANCE = "PERFORMANCE";
    static final String PROPERTY_MODEL_LAST_DATE_FILENAME = "MODEL_LAST_DATE_FILENAME";
    // Define the positions of positive (0) and negative (1) tags in the tag list
    static final int POSITIVE_INDEX = 1;
    static final int NEGATIVE_INDEX = -1;
    // object used to communicate with the database for reading and writing
    private BlackBoardDateBased inputBB;
    private BlackBoardDateBased outputBB;
    private OnlineClassifier_StorageLayer storageLayer = null;
    private String formattedDate;

    public OnlineClassifierAdatron(String propertiesFilename) throws Exception {
        super(propertiesFilename);
    }

    @Override
    public void runModuleCore() throws Exception {

        // load the black board for reading
        inputBB = _bbAPI.blackBoardLoadDateBased(MODULE_INPUT_BLACKBOARD);
        // load the black board for writing
        outputBB = _bbAPI.blackBoardLoadDateBased(MODULE_OUTPUT_BLACKBOARD);

        storageLayer = new OnlineClassifier_StorageLayer(inputBB,
                outputBB);

        // -----------------------------------------------------------
        //Prepare input tags
        List<Integer> inputTag_List = readInputTags(MODULE_INPUT_TAGS);

        //Input Learning Fields (their names and their IDs)
        List<String> inputLearnFieldNames = new ArrayList<String>();
        List<Integer> inputPosLearnFeedIDs = new ArrayList<Integer>();
        List<Integer> inputNegLearnFeedIDs = new ArrayList<Integer>();
        readFields(PROPERTY_INPUT_FEATURES_FIELDSNAME,
                PROPERTY_POS_LEARNING_FIELDS_VALUES,
                PROPERTY_NEG_LEARNING_FIELDS_VALUES,
                inputLearnFieldNames,
                inputPosLearnFeedIDs,
                inputNegLearnFeedIDs);

        //Prepare output tags and fields
        List<Integer> outputTags = readOutputTags(MODULE_OUTPUT_TAGS);
        String outputField = MODULE_OUTPUT_FIELDS;

        String fileName = this.getProperty(PROPERTY_MODEL_FILENAME);


        // a boolean value for holding info if we wish to have a performance file or not
        boolean performance =
                this.getProperty(PROPERTY_PERFORMANCE).equals("TRUE");

        String subFolder = ".";

        Results learningResults = null;
        // if we wish to have a performance file we create it
        if (performance == true) {
            // PREPARE THE RIGHT FILES
            String txtFilenameL = fileName + ".Performance";
            learningResults = new Results(subFolder, txtFilenameL, false);
        }

        // We read the last date that the model was modified from a file
        String lastDateFilename = this.getProperty(PROPERTY_MODEL_LAST_DATE_FILENAME);
        Date lastDate = readLastDate(lastDateFilename);

        // We add 24 hours to last date the model was updated
        Calendar cal = new GregorianCalendar();
        cal.setTime(lastDate);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        Calendar calToDate = (Calendar) cal;
        Date dtToDate = calToDate.getTime();

        Calendar toDay = new GregorianCalendar();

        toDay.add(Calendar.DATE, 2); // Today

        //-----------------------------------------------------------------
        // variables to hold information about the number of data that were read and data
        // that were processed so that the module can print them on the screen
        int dataRead = 0;
        int dataProcessed = 0;

        // write headers to logging files
        String header = "Precision \t "
                + "Recall \t "
                + "F-measure \t "
                + "TP \t "
                + "FP \t "
                + "TN \t "
                + "FN \t "
                + "Error \t "
                + "N_Pos \t "
                + "N_Neg \t "
                + "Ratio \t "
                + "AUC \t"
                + "Extra Bias \t ";
        if (performance == true) {
            learningResults.println(header);
        }

        OnlineLearning onlineLearning =
                new OnlineAdatron(fileName);
        
        // take the meta info file from the settings file
        File metaFile = new File(fileName + ".log");
        if (!metaFile.exists()) {
            onlineLearning.writeHeader(fileName);
            onlineLearning.setLearningFactor(
                    Double.parseDouble(this.getProperty(PROPERTY_LEARNING_FACTOR)));
            if (this.getProperty(PROPERTY_MARGIN) == null) {
                onlineLearning.setPosMargin(
                        Double.parseDouble(this.getProperty(PROPERTY_POS_MARGIN)));
                onlineLearning.setNegMargin(
                        Double.parseDouble(this.getProperty(PROPERTY_NEG_MARGIN)));
            } else {
                onlineLearning.setPosMargin(
                        Double.parseDouble(this.getProperty(PROPERTY_MARGIN)));
                onlineLearning.setNegMargin(
                        Double.parseDouble(this.getProperty(PROPERTY_MARGIN)));
            }
            onlineLearning.setDesiredPrecision(
                    Double.parseDouble(this.getProperty(PROPERTY_DESIRED_PRECISION)));

            // take the window size from the settings file
            onlineLearning.expMovAvSetWindow(
                    Integer.parseInt(this.getProperty(PROPERTY_WINDOW)));

            // initialise the matrix with the statistic information
            onlineLearning.statsReset();
            // and the exponential moving average for the error
            onlineLearning.expMovAvReset();
        }
        onlineLearning.setUpdateLearningFactor(
                this.getProperty(PROPERTY_UPDATE_LEARNING_FACTOR).equals("TRUE"));

        String txtFilenameWords = fileName + ".WordCloud";
        String voc_filename = this.getProperty(PROPERTY_VOCABULARY);
        LinearModel model = onlineLearning.getLinearModel();
        model.wordCloudSetVocabulary(voc_filename);

        //process articles on a daily basis
        while (dtToDate.before(toDay.getTime())) {
            System.out.println(dtToDate + "----" + toDay.getTime());
            lastDate = readLastDate(lastDateFilename);
            cal.setTime(lastDate);
            cal.add(Calendar.DAY_OF_MONTH, 1);
            calToDate = (Calendar) cal;
            dtToDate = calToDate.getTime();
            System.out.println(lastDate + "----" + dtToDate);
            formattedDate = formatDate(lastDate);

            //get articles per day
            BBDocSet DocSet = storageLayer.getDocSetWithTags(lastDate, dtToDate,
                    inputTag_List,
                    this.MODULE_DATA_PROCESS_LIMIT);

            BBDoc s;

            while ((s = DocSet.getNext()) != null) {
                
                onlineLearning.incrementN_overall(1);
                if (dataRead++ % 1000 == 0) {
                    System.out.printf("%d processed (%.2f%%)\n",
                            dataRead, dataRead * 100.0 / this.MODULE_DATA_PROCESS_LIMIT);
                    System.out.println("Now will process date: "
                            + s.getIDasDate() + " ID=" + s.getID());
                }
                @SuppressWarnings("unchecked")
                List<Integer> f = (List<Integer>) s.getField(
                        this.getProperty(PROPERTY_INPUT_FEATURES_FIELDSNAME));
                @SuppressWarnings("unchecked")
                List<Double> tf_idf_x_i = (List<Double>) s.getField(MODULE_INPUT_FIELDS);

                if ((f != null) && (tf_idf_x_i != null)) {
                  
                    boolean trainingsample = false;
                    // create a new DataPoint with these tf-idf values
                    DataPoint sample = new DataPoint(deseriliseFeatures(tf_idf_x_i));

                    //assign labels to the samples


                    for (Integer negFeedID : inputNegLearnFeedIDs) {
                        for (Integer posFeedID : inputPosLearnFeedIDs) {
                            if (f.contains(negFeedID) && (f.contains(posFeedID))) {
                                //do not set the label
                            } else if (f.contains(negFeedID) && !(f.contains(posFeedID))) {
                                sample.setRealLabel(NEGATIVE_INDEX);
                                trainingsample = true;
                            } else if (f.contains(posFeedID) && !(f.contains(negFeedID))) {
                                sample.setRealLabel(POSITIVE_INDEX);
                                trainingsample = true;
                            }
                        }

                    }


                    sample.setID(s.getID());

                    // Add the result of the classifier in a new field
                    if (outputField != null) {
                        storageLayer.addFieldToDoc(sample.getID(),
                                outputField,
                                prediction(sample, onlineLearning));
                    }

                    // Only for positive instances add a new tag
                    if (outputTags.size() > 1) {
                        if (prediction(sample, onlineLearning)
                                >= onlineLearning.getDecisionThreshold()) {
                            storageLayer.addTagsToDoc(s.getID(),
                                    outputTags.get(1)); // Positive prediction tag
                        }														// must be second in list!
                    }
                    if (trainingsample) {
                        dataProcessed++;
                        System.out.println("Trainining Procedure...");
                        training(sample, onlineLearning);

                        // display on the screen the statistical information
                        onlineLearning.statsPrintConfusionMatrix();

                        double N_hat_Pos = onlineLearning.expMovAvGetPositives();
                        double N_hat_Neg = onlineLearning.expMovAvGetNegatives();
                        double display_ALL = N_hat_Pos + N_hat_Neg;

                        // information on the screen
                        String str = "\t #POS \t #NEG \t #ALL \n" + "\t " + N_hat_Pos + "\t "
                                + N_hat_Neg + "\t " + display_ALL + "\t";
                        System.out.println(str);

                        //calculate precision, recall and f-measure
                        str = calculatePrintStatistics(onlineLearning);

                        // if we wish to have a performance file
                        if (performance == true) {
                            // because in file the header is printed only on the beginning
                            System.out.println(header);
                            // inform the file which holds the corresponding statistical info
                            writingStatisticsInFile(onlineLearning,
                                    learningResults,
                                    str);
                        }


                    }
                    if (performance == true) {
                        learningResults.Flush();
                    }
                }
                storageLayer.removeTagsFromDoc(s.getID(),
                        inputTag_List);

                storageLayer.addTagsToDoc(s.getID(),
                        outputTags.get(0));

            }
            System.out.println("TRAINING DONE");



            printTopLeastWords(subFolder,
                    txtFilenameWords,
                    voc_filename, model);


            //save model
            onlineLearning.saveModel(fileName);

            //save log file
            onlineLearning.saveLog(this.getProperty(PROPERTY_MODEL_FILENAME));
            System.out.println("DONE");

            saveLastDate(lastDateFilename, dtToDate);
        }
        if (performance == true) {
            learningResults.SaveOutput();
        }
        // display the number of input items and the number of output items
        this.saveModuleResults(dataRead, dataProcessed);
    }

    /**
     * This function returns the date that the file was modified
     *
     * @param fileName:The name of the file of interest
     * @return The date the file was last modified
     * @throws Exception
     */
    private Date readLastDate(String fileName) throws Exception {
        try {
            DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            String line = in.readLine();
            Date inputTime = df.parse(line);
            in.close();

            return inputTime;
        } catch (Exception e) {
            throw new Exception("Error reading old date file ... " + fileName);
        }
    }

    /**
     * This function stores the last date that the vocabulary was updated
     *
     * @param fileName:The name of the file in which the new voc is going to be stored
     * @param time:The date and time that the last update is being made
     * @throws Exception
     */
    private void saveLastDate(String fileName, Date time) throws Exception {
        try {
            DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName, false));
            String formattedDate = df.format(time);
            out.write(formattedDate);
            out.close();

        } catch (Exception e) {
            throw new Exception("Error saving date of new date file ... " + fileName);
        }
    }

    private String formatDate(Date time) throws Exception {
        String formattedDate = "";
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

            formattedDate = df.format(time);

        } catch (Exception e) {
        }
        return formattedDate;

    }

    /**
     * This function writes the most and least important words to the right files and
     * the screen.
     * @param subFolder:The path we want the files to be stored
     * @param txtFilenameWords:The name of the files
     * @param voc_filename:The vocabulary the program will use in order to find the right words
     * @param linearModel:The model with the weights of each word
     * @throws Exception
     */
    void printTopLeastWords(String subFolder,
            String txtFilenameWords,
            String voc_filename,
            LinearModel linearModel) throws Exception {
        // we want the results to display on the screen too,
        // that's why we put false as the last argument
        Results wordsResults = new Results(subFolder, txtFilenameWords, true);

        System.out.println("=================================");
        wordsResults.println("Top 20");
        printTopWords(20, wordsResults, linearModel);
        wordsResults.println("---------------------------------");
        wordsResults.println("Least 20");
        printLeastWords(20, wordsResults, linearModel);
        System.out.println("=================================");

        // close the according files
        wordsResults.SaveOutput();

    }

    /**
     * This function writes the most and least important words to the right files and
     * the screen.
     * @param subFolder:The path we want the files to be stored
     * @param txtFilenameWords:The name of the files
     * @param voc_filename:The vocabulary the program will use in order to find the right words
     * @param linearModel:The model with the weights of each word
     * @throws Exception
     */
    void printTopLeastWords(String subFolder,
            String txtFilenameWords,
            LinearModel linearModel) throws Exception {
        // we want the results to display on the screen too,
        // that's why we put false as the last argument
        Results wordsResults = new Results(subFolder, txtFilenameWords, true);

        System.out.println("=================================");
        wordsResults.println("Top 20");
        printTopWords(20, wordsResults, linearModel);
        wordsResults.println("---------------------------------");
        wordsResults.println("Least 20");
        printLeastWords(20, wordsResults, linearModel);
        System.out.println("=================================");

        // close the according files
        wordsResults.SaveOutput();

    }

    /**
     * This function prints only the n top words of the model (higher weights)
     * @param number:The number of the word we wish to print
     * @param voc_filename:The vocabulary the program will use in order to find the right words
     * @param wordsResults:The file we want the words to be stored
     * @param linearModel:The model with the weights of each word
     * @throws Exception
     */
    void printTopWords(int number,
            Results wordsResults,
            LinearModel linearModel) throws Exception {
        Map<String, Double> WordsCloud =
                linearModel.wordCloudGetTopFeatures(number);

        for (Map.Entry<String, Double> e : WordsCloud.entrySet()) {
            wordsResults.print(e.getKey());
            wordsResults.print(" : ");
            wordsResults.println(e.getValue().toString());
        }
    }

    /**
     * This function prints only the n least words of the model (lower weights)
     * @param number:The number of the word we wish to print
     * @param voc_filename:The vocabulary the program will use in order to find the right words
     * @param wordsResults:The file we want the words to be stored
     * @param linearModel:The model with the weights of each word
     * @throws Exception
     */
    void printLeastWords(int number,
            Results wordsResults,
            LinearModel linearModel) throws Exception {
        Map<String, Double> WordsCloud =
                linearModel.wordCloudGetLeastFeatures(number);

        for (Map.Entry<String, Double> e : WordsCloud.entrySet()) {
            wordsResults.print(e.getKey());
            wordsResults.print(" : ");
            wordsResults.println(e.getValue().toString());
        }
    }

    /**
     * This function read from the settings file the names and the values of the required
     * fields of the document and inform the corresponding lists
     * @param Names : The names of the fields specified by the user in the settings file
     * @param PosIDs : The values of those fields which belong in the positive class
     * @param NegIDs : The values of those fields which belong in the negative class
     * @param inputLearnFieldNames : The list with all the names of the fields (it could be more than one)
     * @param inputPosLearnFieldIDs : The list with all the ids of fields belonging to the positive class
     * @param inpuNegLearnFieldIDs : The list with all the ids of fields belonging to the negative class
     */
    void readFields(String Names, String PosIDs, String NegIDs, List<String> inputLearnFieldNames,
            List<Integer> inputPosLearnFieldIDs, List<Integer> inpuNegLearnFieldIDs) {
        // Their names
        String inputLearnFieldNames_toks[] = this.getProperty(Names).split(",");
        for (String f : inputLearnFieldNames_toks) {
            inputLearnFieldNames.add(f);
        }

        // The positive ids
        String inputPosLearnFieldIDs_toks[] = this.getProperty(PosIDs).split(",");
        for (String f : inputPosLearnFieldIDs_toks) {
            inputPosLearnFieldIDs.add(Integer.parseInt(f));
        }
        // The negative ids
        String inputNegLearnFieldIDs_toks[] = this.getProperty(NegIDs).split(",");

        for (String f : inputNegLearnFieldIDs_toks) {
            if (f.contains("-1")) {
                inpuNegLearnFieldIDs.add(-1);
                inpuNegLearnFieldIDs.add(Integer.parseInt(inputNegLearnFieldIDs_toks[1]));
            }
            if (!f.equals("all")) {
                inpuNegLearnFieldIDs.add(Integer.parseInt(f));
            }
        }
    }

    /**
     * This function reads the Tags for input
     *
     * @param Tags:The string that holds all the tags of interest
     * @param bb:The blackboard we use
     * @return The list of tags splitted from the input Tags
     * @throws Exception
     */
    private List<Integer> readInputTags(String commaSepratedTags) throws Exception {
        //Prepare tags
        String tagNames[] = commaSepratedTags.split(",");
        List<Integer> tagIDs = new LinkedList<Integer>();
        for (String tagName : tagNames) {
            if (!tagName.equals("")) {
                int tagID = storageLayer.getInputTagID(tagName);
                if (tagID == 0) {
                    throw new Exception("Unknown input tag");
                }
                tagIDs.add(tagID);
            }
        }
        return tagIDs;
    }

    /**
     * This function reads the Tags for output
     *
     * @param Tags:The string that holds all the tags for output
     * @param bb:The blackboard we use
     * @return The list of tags splitted from the output Tags
     * @throws Exception
     */
    private List<Integer> readOutputTags(String commaSepratedTags) throws Exception {
        String outTagNames[] = commaSepratedTags.split(",");
        List<Integer> outTag_List = new LinkedList<Integer>();
        for (String outTagName : outTagNames) {
            if (!outTagName.equals("")) {
                int tagID = storageLayer.getOutputTagID(outTagName);
                outTag_List.add(tagID);
            }
        }
        return outTag_List;
    }

    /**
     * Transforms the input features to a features Map based on TF-IDF.
     *
     * @param featuresAsString the string text that you wand to transform into a Map based on TF
     * @return the map  of the words' IDs and their TF-IDF
     * @throws InterruptedException
     */
    public Map<Integer, Double> deseriliseFeatures(List<Double> features_list) throws Exception {
        Map<Integer, Double> feat = new TreeMap<Integer, Double>();
        int index;
        double value;
        for (int f = 0; f < features_list.size(); f += 2) {
            index = (int) Math.round(features_list.get(f));
            value = features_list.get(f + 1);
            feat.put(index, value);
        }
        return feat;
    }

    /**
     * Predicts the output of the model given the specific DataPoint
     * @param sample:The DataPoint we want to classify
     * @return The number calculated for the input DataPoint given the model
     * @throws Exception
     */
    double prediction(DataPoint sample, OnlineLearning onlineLearning) throws Exception {
        sample.setPredictedLabel_Value(onlineLearning.predict(sample));
        return sample.getPredictedLabel_Value();
    }

    /**
     * Training the model with the specific DataPoint
     * @param sample:The DataPoint we want the model to be trained of
     * @param onlineLearning:The object with the model
     * @throws Exception
     */
    void training(DataPoint sample,
            OnlineLearning onlineLearning) throws Exception {
        onlineLearning.train(sample);
    }

    /**
     * This function calculates precision, recall and f-measure according to TP,FP,TN,FN
     * @param onlineLearning:The object which hold the information of interest
     * @return A string which contains the right statistical information
     * @throws Exception
     */
    private String calculatePrintStatistics(OnlineLearning onlineLearning) throws Exception {

        double precision = 0.0, recall = 0.0, f_measure = 0;
        precision = onlineLearning.statsGetPrecision();
        recall = onlineLearning.statsGetRecall();
        if (precision + recall != 0) {
            f_measure = 2 * precision * recall / (precision + recall);
        } else {
            f_measure = 0.0;
        }

        return precision + " \t " + recall + " \t "
                + f_measure + " \t ";
    }

    /**
     * This function calculates the statistical information according to the matrix's
     * values (precision, recall, f-measure, etc.). Then it informs the right file
     *
     * @param Statistics : The values of the confusion matrix
     * @param learningResults : The file which is going to be informed with these information
     * @throws Exception
     */
    private void writingStatisticsInFile(OnlineLearning onlineLearning,
            Results learningResults, String str) throws Exception {
        double N_hat_Pos = onlineLearning.expMovAvGetPositives();
        double N_hat_Neg = onlineLearning.expMovAvGetNegatives();
        double display_ALL = N_hat_Pos + N_hat_Neg;

        learningResults.print(str);
        learningResults.print(Long.toString(onlineLearning.statsGetTP()));
        learningResults.print(" \t ");
        learningResults.print(Long.toString(onlineLearning.statsGetFP()));
        learningResults.print(" \t ");
        learningResults.print(Long.toString(onlineLearning.statsGetTN()));
        learningResults.print(" \t ");
        learningResults.print(Long.toString(onlineLearning.statsGetFN()));
        learningResults.print(" \t ");
        learningResults.print(Double.toString(
                onlineLearning.expMovAvGetError()));
        learningResults.print(" \t ");
        learningResults.print(Double.toString(N_hat_Pos));
        learningResults.print(" \t ");
        learningResults.print(Double.toString(N_hat_Neg));
        learningResults.print(" \t ");
        learningResults.print(Double.toString((double) N_hat_Pos / display_ALL));
        learningResults.print(" \t ");
        learningResults.print(Double.toString(onlineLearning.getAUC()));
        learningResults.print(" \t ");
        learningResults.println(Double.toString(
                onlineLearning.getDecisionThreshold()));
    }

    /**
     *
     * @param args The settings file that contains I/O and parameters info.
     * @throws Exception
     *
     */
    public static void main(String[] args) throws Exception {
        OnlineClassifierPerceptron module = new OnlineClassifierPerceptron(args[0]);

        module.run();
    }
}
