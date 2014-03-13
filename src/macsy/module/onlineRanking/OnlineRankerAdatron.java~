package macsy.module.onlineRanking

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
 * It predicts the score calculated from a already stored model for ranking procedure
 * and the result of that calculation is stored in the database (under a field - OUTPUT_FIELD).
 * It also updates the given model by using only the right documents specified by the user
 * (with names INPUT_LEARN_FIELDSNAME and values INPUT_LEARN_FIELDSID) performing the ranking
 * procedure that consists of calculating the difference of documents x_i and x_j as input
 * and ranking the results. The lists x_i and x_j consist of the documents that are tagged
 * with TAG1_AND_TAG2,and those with TAG1_AND_NOT_TAG2, accordingly (where TAG1 and TAG2
 * are the tags specified through INPUT_LEARN_FIELDSNAME, INPUT_LEARN_FIELDSID and
 * INPUT_TAGS_FOR_COMBINATION.
 * The prediction and training procedure of the module are based on the adatron algorithm
 * Input:
 * INPUT_BLACKBOARD=The name of the BlackBoard that contains the documents of interest.
 * PROCESS_LIMIT=The max number of documents for classification. Set to zero to get all
 * INPUT_FIELDS=The field that holds the input data (features) and that we want to
 * apply the online learning of the docs.
 * INPUT_TAGS=The module performs the whole procedure only on documents that have this tag
 * INPUT_LEARN_FIELDSNAME=The name of the fields on which the learning is based.
 * INPUT_LEARN_FIELDSID=The ids of the fields belonging to one class (TAG1, the common class)
 * INPUT_TAGS_FOR_COMBINATION=The tags TAG2 which are going to be taken into account for
 * the difference
 * LEARNING_FACTOR=The eta in the learning process
 * UPDATE_LEARNING_FACTOR=If we wish to update the eta (boolean true/false)
 * DESIRED_PRECISION=If we wish to use the extra bias (-1 if we don't want extra bias)
 * MARGIN_THRESHOLD=The tau in the decision rule for update or not 
 * WINDOW=The number of data we wish to remember
 * VOCABULARY=The filename with the words of the language (for the word clouds)
 * MODEL_FILENAME=The name of the model that the module will read (if exists) the weights
 * at the beginning and store them after termination.
 * MODEL_LAST_DATE_FILENAME=The name of the file storing the last date that the model is updated.
 * PERFORMANCE=The boolean flag to specify if we want a large file with the performance
 * of the classifier per document
 *
 * Output:
 * OUTPUT_BLACKBOARD=Output BlackBoard
 * OUTPUT_TAGS=The tags that the module will add in the processed document. Actually
 * is the tag with which the INPUT_TAGS will be replaced.
 * OUTPUT_FIELDS=The fields' names where the module is going to write the predicted value y_hat.
 *
 * The statistical information such as precision, recall, TP, TN, FP, FN etc. are store in .log
 * in order for the module to continue from the last run.
 *
 * @author Panagiota Antonakaki
 * Last Update: 12-03-2014
 *
 */
public class OnlineRankerAdatron extends BaseModule {
    // temporal variables to hold information given by the user

    static final String PROPERTY_MODEL_FILENAME = "MODEL_FILENAME";
    static final String PROPERTY_INPUT_LEARN_FIELDSNAME = "INPUT_LEARN_FIELDSNAME";
    static final String PROPERTY_INPUT_LEARN_FIELDVALUE = "INPUT_LEARN_FIELDSID";
    static final String PROPERTY_TAGS_FOR_COMBINATION = "INPUT_TAGS_FOR_COMBINATION";
    static final String PROPERTY_MODEL_LAST_DATE_FILENAME = "MODEL_LAST_DATE_FILENAME";
    static final String PROPERTY_WINDOW = "WINDOW";
    static final String PROPERTY_LEARNING_FACTOR = "LEARNING_FACTOR";
    static final String PROPERTY_POS_MARGIN = "POS_MARGIN_THRESHOLD";
    static final String PROPERTY_NEG_MARGIN = "NEG_MARGIN_THRESHOLD";
    static final String PROPERTY_MARGIN = "MARGIN_THRESHOLD";
    static final String PROPERTY_DESIRED_PRECISION = "DESIRED_PRECISION";
    static final String PROPERTY_UPDATE_LEARNING_FACTOR = "UPDATE_LEARNING_FACTOR";
    static final String PROPERTY_VOCABULARY = "VOCABULARY";
    static final String PROPERTY_PERFORMANCE = "PERFORMANCE";
    private BlackBoardDateBased inputBB;
    private BlackBoardDateBased outputBB;
    private OnlineRanker_StorageLayer storageLayer = null; 	// object used for communicate with the database
    private Date d;

    public OnlineRankerAdatron(String propertiesFilename) throws Exception {
        super(propertiesFilename);
    }

    @Override
    public void runModuleCore() throws Exception {
        // Find last time the module was run
        String lastDateFilename = this.getProperty(PROPERTY_MODEL_LAST_DATE_FILENAME);
        Date lastDate = readLastDate(lastDateFilename);


        // Move date on by 1 day
        Date dtToDate = addDay(lastDate);

        // variables to hold information about the number of data that were read and data
        // that were processed so that the module can print them on the screen
        int dataRead = 0;
        int dataProcessed = 0;
        String resultsComment = "";

        String fileName = this.getProperty(PROPERTY_MODEL_FILENAME);
        boolean performance =
                this.getProperty(PROPERTY_PERFORMANCE).equals("TRUE");

        String subFolder = ".";

        Results learningResults = null;
        // if we wish to have a performance file we create it
        if (performance == true) {
            String txtFilenameL = fileName + ".Performance";
            learningResults = new Results(subFolder, txtFilenameL, false);
        }

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

        // inform the right file with the statistical information
        if (performance == true) {
            learningResults.println(header);
        }

        // load the black board for reading
        inputBB =
                _bbAPI.blackBoardLoadDateBased(MODULE_INPUT_BLACKBOARD);
        // load the black board for writing
        outputBB =
                _bbAPI.blackBoardLoadDateBased(MODULE_OUTPUT_BLACKBOARD);

        storageLayer = new OnlineRanker_StorageLayer(inputBB, outputBB);


        OnlineLearning onlineLearning =
                new OnlineAdatron(fileName);

        File logFile = new File(fileName + ".log");
        if (!logFile.exists()) {
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
        // a boolean value for holding info if we wish to have a performance file or not


        String txtFilenameWords = fileName + ".WordCloud";
        LinearModel model = onlineLearning.getLinearModel();
        String voc_filename = this.getProperty(PROPERTY_VOCABULARY);
        model.wordCloudSetVocabulary(voc_filename);



        //-----------------------------------------------------------------
        // PREPARE TAGS
        List<Integer> tagForCombination =
                readTags(this.getProperty(PROPERTY_TAGS_FOR_COMBINATION));

        List<String> inputLearnFieldNames = new ArrayList<String>();
        List<Integer> inputLearnFeedIDs = new ArrayList<Integer>();
        readFields(PROPERTY_INPUT_LEARN_FIELDSNAME, PROPERTY_INPUT_LEARN_FIELDVALUE,
                inputLearnFieldNames,
                inputLearnFeedIDs);

        Integer inputTagID =
                readTag(MODULE_INPUT_TAGS);

        List<Integer> inputTags = new LinkedList<Integer>();
        inputTags.add(inputTagID);

        //Output Tag
        int outputTagID = storageLayer.getOutputTagID(this.MODULE_OUTPUT_TAGS);
        if (outputTagID == 0) {
            throw new Exception("No output Tags are specified");
        }




        // only run if the model is older than 24 hours old
        while ((Calendar.getInstance().getTime().compareTo(dtToDate) > 0)) {
            dataRead = 0;


            BBDocSet docSet = storageLayer.findDocsByFieldsTagsSet(lastDate,
                    dtToDate, inputTags, this.MODULE_DATA_PROCESS_LIMIT);

            /* The lists that hold the information for the documents that will be
             * compared in order for the ranker to update the model
             * the first list x_i holds information about the documents that have
             * TAG1_AND_TAG2 and the list x_j about the documents with TAG1_AND_NOT_TAG2 */
            List<DataPoint> list_x_i = new LinkedList<DataPoint>();
            List<DataPoint> list_x_j = new LinkedList<DataPoint>();


            BBDoc doc;
            while ((doc = docSet.getNext()) != null) {
                dataRead++;
                d = doc.getIDasDate();
                @SuppressWarnings("unchecked")
                List<Double> tf_idf_x_i =
                        (List<Double>) doc.getField(MODULE_INPUT_FIELDS);

                if (tf_idf_x_i != null) {
                    if (dataProcessed++ % 1000 == 0) {
                        System.out.printf("%d processed (%.2f%%)\n",
                                dataRead, dataRead * 100.0 / this.MODULE_DATA_PROCESS_LIMIT);
                        System.out.println("Now will process date: "
                                + doc.getIDasDate() + " ID=" + doc.getID());
                    }

                    // it predicts the score for the particular document doc taking into
                    // account the previous stored model
                    double score = predict(tf_idf_x_i, onlineLearning, doc.getID());

                    storageLayer.addFieldToDoc(doc.getID(),
                            MODULE_OUTPUT_FIELDS, score);
                    @SuppressWarnings("unchecked")
                    List<Integer> docFieldId =
                            (List<Integer>) doc.getField(inputLearnFieldNames.get(0));

                    // it creates the lists that will be used for training (updating
                    // the model) and consist only from documents from a particular outlet
                    createList(tf_idf_x_i, doc, docFieldId,
                            inputLearnFeedIDs,
                            tagForCombination,
                            list_x_i,
                            list_x_j);

                    storageLayer.removeTagFromDoc(doc.getID(), inputTagID);
                    storageLayer.addTagToDoc(doc.getID(), outputTagID);
                }
            }
            onlineLearning.incrementN_overall(dataRead);
            int x_i_size = list_x_i.size();
            int x_j_size = list_x_j.size();

            if ((x_i_size > 1) && (x_j_size > 1)) {
                // the training procedure from the already extracted lists
                trainingProcedure(list_x_i,
                        list_x_j,
                        onlineLearning,
                        performance,
                        learningResults,
                        header, d);

                resultsComment = "TRAINING TOPSTORIES_MOSTPOPULAR=" + x_i_size + " TOPSTORIES_NOT_MOSTPOPULAR=" + x_j_size;
            } else {
                if (dataRead == 0) {
                    System.err.println("No docs with the input tag");
                }
                System.err.println("No data for training");
            }


            //save the model
            onlineLearning.saveModel(this.getProperty(PROPERTY_MODEL_FILENAME));

            //writes to log file
            onlineLearning.saveLog(this.getProperty(PROPERTY_MODEL_FILENAME));



            saveLastDate(lastDateFilename, dtToDate);

            lastDate = addDay(lastDate);
            dtToDate = addDay(dtToDate);

            printTopLeastWords(subFolder,
                    txtFilenameWords, model);



        }
        if (performance == true) {
            learningResults.SaveOutput();
        }

        // display the number of input items and the number of output items
        this.saveModuleResults(dataRead, dataProcessed, resultsComment);

    }

    /**
     * This function adds one day to the input date
     *
     * @param date:The date we want to add 24 hours to
     * @return the input date with the added 24 hours
     */
    Date addDay(Date date) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        Calendar calDate = (Calendar) cal;
        Date dtDate = calDate.getTime();
        return dtDate;
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
            DateFormat df = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
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

    /**
     * This function reads the Tags
     *
     * @param Tags:The string that holds all the tags of interest
     * @return The list of tags splitted from the input Tags
     * @throws Exception
     */
    private List<Integer> readTags(String Tags) throws Exception {
        //Prepare tags
        String TagNames[] = Tags.split(",");
        List<Integer> TagIDs = new LinkedList<Integer>();	//Variable name clarification + Java style
        for (String tagName : TagNames) {
            if (!tagName.equals("")) {
                int tagID = storageLayer.getInputTagID(tagName);
                if (tagID == 0) {
                    throw new Exception("Unknown input tag " + tagName);
                }
                TagIDs.add(tagID);
            }
        }
        TagNames = null;
        return TagIDs;
    }

    /**
     * This function reads the Tag we are interested in and returns its id
     *
     * @param Tag: The name of the tag of interest
     * @param bb: The blackboard of use
     * @return The id of the Tag
     * @throws Exception
     */
    private Integer readTag(String Tag) throws Exception {
        // if there are more than one tag the module throws an error
        if (Tag.split(",").length > 1) {
            System.err.println("No lists allowed");
            System.exit(0);
        }

        Integer tagID = null;
        if (!Tag.equals("")) {
            tagID = storageLayer.getInputTagID(Tag);
            // if the tag is unknown in the blackboard the module also throws an error
            if (tagID == 0) {
                System.err.println("No known input tag " + Tag);
                System.exit(0);
            }
        }
        return tagID;
    }

    /**
     * This function reads the fields and returns a list with their names and a list
     * with their values
     *
     * @param Names:The string holding the names
     * @param IDs:The string holding the values
     * @param inputLearnFieldNames:The list with the splitted names
     * @param inputLearnFieldIDs:The list with the splitted values
     */
    private void readFields(String Names, String IDs,
            List<String> inputLearnFieldNames,
            List<Integer> inputLearnFieldIDs) {
        // Their names
        String inputLearnFieldNames_toks[] = this.getProperty(Names).split(",");
        for (String f : inputLearnFieldNames_toks) {
            inputLearnFieldNames.add(f);
        }


        // Their ids
        String inputPosLearnFieldIDs_toks[] = this.getProperty(IDs).split(",");
        for (String f : inputPosLearnFieldIDs_toks) {
            inputLearnFieldIDs.add(Integer.parseInt(f));
        }
    }

    /**
     * This function uses a previous model and it predicts (according to that model)
     * the score of the specific document.
     *
     * @param tf_idf_x_i:The features (tf-idf) used for the calculation of the score.
     * @param onlineLearning:The model of interest.
     * @param ID:The id of the document.
     *
     * @throws Exception
     */
    private double predict(List<Double> tf_idf_x_i,
            OnlineLearning onlineLearning, Object ID) throws Exception {
        DataPoint sample = new DataPoint(
                deseriliseFeatures(tf_idf_x_i));
        sample.setID(ID);
        // training with the specified instance
        return onlineLearning.predict(sample);
    }

    /**
     * This function creates the lists x_i and x_j that are going to be used
     * for the training step later.
     *
     * @param tf_idf_x_i:The features (tf-idf) used.
     * @param s:The document of interest.
     * @param docFieldId:The id of the fields of interest.
     * @param withFieldValues:The values of the above fields that we want to be used for
     * training purposes (TAG1).
     * @param tagForCombination:The tag used for combination (TAG2).
     * @param list_x_i:The list with the documents tagged as TAG1_AND_TAG2.
     * @param list_x_j:The list with the documents tagged as TAG1_AND_NOT_TAG2.
     * @throws Exception
     */
    private void createList(List<Double> tf_idf_x_i,
            BBDoc s,
            List<Integer> docFieldId,
            List<Integer> withFieldValues,
            List<Integer> tagForCombination,
            List<DataPoint> list_x_i,
            List<DataPoint> list_x_j) throws Exception {
        DataPoint sample = new DataPoint(
                deseriliseFeatures(tf_idf_x_i));

        if (docFieldId != null) {
            for (Integer e : withFieldValues) {
                if (docFieldId.contains(e)) {
                    boolean x_i = false;
                    for (Integer i : tagForCombination) {
                        if (s.getAllTagIDs().contains(i)) {
                            x_i = true;
                        }

                        //System.out.println(s.getAllTagIDs() + " , " + x_i);

                        if (x_i) {
                            list_x_i.add(sample);
                        } else {
                            list_x_j.add(sample);
                        }
                    }
                }
            }
        }
    }

    /**
     * This function performs the training step using the lists already formed
     *
     * @param list_x_i:The list with the documents tagged as TAG1_AND_TAG2.
     * @param list_x_j:The list with the documents tagged as TAG1_AND_NOT_TAG2.
     * @param onlinelearning:The model of interest (used for the training procedure).
     *
     * @throws Exception
     */
    private void trainingProcedure(List<DataPoint> list_x_i,
            List<DataPoint> list_x_j, OnlineLearning onlinelearning,
            boolean performance, Results learningResults, String header, Date d)
            throws Exception {
        for (DataPoint x_i : list_x_i) {

            for (DataPoint x_j : list_x_j) {

                if (Math.random() >= 0.5) {
                    Training(x_i, x_j, onlinelearning, 1);
                } else {
                    Training(x_j, x_i, onlinelearning, -1);

                }
                // display on the screen the statistical information
                onlinelearning.statsPrintConfusionMatrix();

                double N_hat_Pos = onlinelearning.expMovAvGetPositives();
                double N_hat_Neg = onlinelearning.expMovAvGetNegatives();
                double display_ALL = N_hat_Pos + N_hat_Neg;

                // information on the screen
                String str = "\t #POS \t #NEG \t #ALL \n" + "\t " + N_hat_Pos
                        + "\t " + N_hat_Neg + "\t " + display_ALL + "\t";
                System.out.println(str);

                // calculate precision, recall and f-measure

                str = calculatePrintStatistics(onlinelearning);

                // if we wish to have a performance file
                if (performance == true) {

                    // because in file the header is printed only on the
                    // beginning

                    System.out.println(header);
                    // inform the file which holds the corresponding statistical
                    // info
                    writingStatisticsInFile(onlinelearning, learningResults,
                            str, d);
                }
            }
            if (performance == true) {
                learningResults.Flush();
            }
        }
    }

    /**
     * This function is called for the training procedure performed on the difference of
     * the input DataPoints.
     *
     * @param x_i:The first DataPoint of interest that we want the training to perform
     * @param x_j:The second DataPoint of interest that we want the training to perform
     * @param onlineLearning:The model of interest.
     * @param desired_output:The actual label of the difference of the input DataPoints
     * @throws Exception
     */
    private void Training(DataPoint x_i, DataPoint x_j,
            OnlineLearning onlineLearning,
            int desired_output) throws Exception {
        DataPoint sample = DataPoint.difference(x_i, x_j);
        sample.setRealLabel(desired_output);
        // training with the specified instance
        onlineLearning.train(sample);

    }

    /**
     * Transforms the input features to a features Map based on TF-IDF.
     *
     * @param featuresAsString the string text that you wand to transform into a Map based on TF
     * @return the map  of the words' IDs and their TF-IDF
     * @throws InterruptedException
     */
    private Map<Integer, Double> deseriliseFeatures(List<Double> features_list) throws Exception {

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
            Results learningResults, String str, Date d) throws Exception {
        double N_hat_Pos = onlineLearning.expMovAvGetPositives();
        double N_hat_Neg = onlineLearning.expMovAvGetNegatives();
        double display_ALL = N_hat_Pos + N_hat_Neg;
        learningResults.print(d.toString());
        learningResults.print(" \t ");
        learningResults.print(str);
        learningResults.print(Long.toString(onlineLearning.statsGetTP()));
        learningResults.print(" \t ");
        learningResults.print(Long.toString(onlineLearning.statsGetFP()));
        learningResults.print(" \t ");
        learningResults.print(Long.toString(onlineLearning.statsGetTN()));
        learningResults.print(" \t ");
        learningResults.print(Long.toString(onlineLearning.statsGetFN()));
        learningResults.print(" \t ");
        learningResults.print(Double.toString(onlineLearning.expMovAvGetError()));
        learningResults.print(" \t ");
        learningResults.print(Double.toString(N_hat_Pos));
        learningResults.print(" \t ");
        learningResults.print(Double.toString(N_hat_Neg));
        learningResults.print(" \t ");
        learningResults.print(Double.toString((double) N_hat_Pos / display_ALL));
        learningResults.print(" \t ");
        learningResults.print(Double.toString(onlineLearning.getAUC()));
        learningResults.print(" \t ");
        learningResults.println(Double.toString(onlineLearning.getDecisionThreshold()));
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
    private void printTopLeastWords(String subFolder,
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
    private void printTopWords(int number,
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
    private void printLeastWords(int number,
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
     *
     * @param args The settings file that contains I/O and parameters info.
     * @throws Exception
     *
     */
    public static void main(String[] args) throws Exception {
        OnlineRankerPerceptron module = new OnlineRankerPerceptron(args[0]);

        module.run();
    }

}
