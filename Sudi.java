
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


/**
 * @author Harry Beesley-Gilman
 * Date 6/24/22
 * Purpose: Code Sudi class to implement the required message and employ Viterbi
 */


public class Sudi {



    private String start;

    private HashMap<String, HashMap<String, Double>> posProbs; /*probabilities of moving from one part of speech to another*/

    private HashMap<String, HashMap<String, Double>> wordProbs; /*probability one word will be a given part of speech*/

    private final int U = -100000000; /*unseen word penalty, to be used later*/

    public Sudi() {
        posProbs = new HashMap<String, HashMap<String, Double>>();
        /*initialize empty HashMaps (to be set later) and a let the system know that we'll always start with a #.
        */

        wordProbs = new HashMap<String, HashMap<String, Double>>();

        start = "#";
    }

    public void initialize(String trainfile, String testfile) throws IOException {
        /*uses a Sudi trainer object to fill out posProbs and wordProbs. Go into SudiTrainer code for more info.*/
        ArrayList x = SudiTrainer.trainposProbs(trainfile, testfile, posProbs, wordProbs);
        SudiTrainer.trainWordProbs(trainfile, testfile, posProbs, wordProbs, x);
    }

    public ArrayList<String> viterbi(String inputfile) throws IOException {
        BufferedReader viterbiReader = new BufferedReader(new FileReader(inputfile));

        String current = viterbiReader.readLine().toLowerCase();
        /*convert everything to lowercase as in the instructions*/


        HashSet<String> currentStates = new HashSet();
        HashMap<String, Double> currentScores = new HashMap();
        /*as described in viterbi alg explanation*/


        Double bestScore = -5000000000.0;
        /*pretty arbitrary and will be reset. I wanted to set a really low "best" score initially so it would always
         get exceeded at least
        * once during a run through.*/
        String lastWord = "";

        ArrayList posInOrder = new ArrayList<String>();
        /*List of every pos tag we think is correct. Will be useful later for checking accuracy.*/


        while (current != null) { /*while there is still mroe file to parse*/
            ArrayList<Map<String, String>> backtrace = new ArrayList<>(); /*first string is the term, second is what
            preceedded it*/
            current = current.toLowerCase();
            String[] theseWords = current.split(" ");

            currentStates.add(start); /*add the start (#) with an always probability since we mark every line with this*/

            currentScores.put(start, 0.0);


            for (int i = 0; i < theseWords.length; i++) {
                /*for every word in the line*/

                bestScore = -5000000000.0; /*we have to find the best score with every new term in a line; cannot rest
                on our laurels; thus, this must be reset*/
                lastWord = " ";
                HashSet<String> nextStates = new HashSet(); /*for looking ahead*/
                HashMap<String, Double> nextScores = new HashMap();


                for (String stringy : currentStates) { /*all the strings we're currently considering*/

                    if (posProbs.containsKey(stringy)) {

                        /*for each transition currState -> nextState*/
                        for (String partOfSpeech : posProbs.get(stringy).keySet()) {

                            /*we'll have to look at this*/
                            nextStates.add(partOfSpeech);

                            double score;
                            /*score will tell us the odds for a particular path*/

                            /*I use the if/else here in case a word hasn't been seen before so that I don't get a
                            *get/null error*/
                            if (wordProbs.get(theseWords[i]) != null && wordProbs.get(theseWords[i]).containsKey(partOfSpeech)) {
                                score = currentScores.get(stringy) + posProbs.get(stringy).get(partOfSpeech) +
                                        wordProbs.get(theseWords[i]).get(partOfSpeech);
                            }
                            else {
                                score = currentScores.get(stringy) + posProbs.get(stringy).get(partOfSpeech) + U;
                            }
                            /*if we haven't reached that POS on this round or the score is better than other paths to
                            that POS*/
                            if (!nextScores.containsKey(partOfSpeech) || score > nextScores.get(partOfSpeech)) {
                                /*add our score to nextscores. Nextstates is already filled out*/
                                nextScores.put(partOfSpeech, score);

                                /*note a new best score and update lastword*/
                                if (score > bestScore) {
                                    bestScore = score;
                                    lastWord = partOfSpeech;
                                }

                                /*Got help from a TA these four lines. This will add to our backtrace without throwing an
                                * index error*/
                                if (backtrace.size() == i) {
                                    Map<String, String> track = new HashMap<String, String>();
                                    track.put(partOfSpeech, stringy);
                                    backtrace.add(track);
                                }
                                else {
                                    backtrace.get(i).put(partOfSpeech, stringy);
                                }
                            }
                        }

                    }

                }
                /*update current to next so that we can iterate through to the next round*/
                currentStates = nextStates;
                currentScores = nextScores;

            }
            /*the following code goes through our backtrace and generates a string
            * of the correct order*/
            String output = new String();
            output = output.concat(lastWord);
            String next = output;

            /*run trhough backtrace and find the order of our path with ! delineating transitions between POS in the
            string*/
            for (int i = 1; i < backtrace.size(); i++) {
                next = backtrace.get(backtrace.size() - i).get(next);
                output = output.concat("!");
                output = output.concat(next);
            }
            String output2 = new String();
            String tempString = "";
            /*This code takes out !, and flips, preserving order of characters within individual POS names*/
            for (int j = output.length() - 1; j >= 0; j--) {
                if (output.charAt(j) == '!') {
                    String switchedTempString = new String("");
                    for (int k = tempString.length() - 1; k >= 0; k--){
                        switchedTempString= switchedTempString.concat(String.valueOf(tempString.charAt(k)));
                    }
                    posInOrder.add(switchedTempString);
                    output2 = output2.concat(switchedTempString);
                    output2 = output2.concat(" ");

                    tempString= new String("");
                }
                else if (j == 0){
                    String tempString2 = String.valueOf(output.charAt(j));
                    posInOrder.add(tempString2);
                    output2 = output2.concat(tempString2);
                }
                else {
                    tempString = tempString.concat(String.valueOf(output.charAt(j)));
                }
            }
            /*prints the next part of speech for the user*/
            System.out.println(output2);
            /*tick to the next line*/
            current = viterbiReader.readLine();
        }
        /*useful for accuracy check later*/
        return posInOrder;
    }

    /*very similar to the above method. Different in that rather than parsing through the lines of a file,
    * it does almost exactly the same action on a single String. */
    public String singleLineViterbi (String inputLine) {
        String output = "";

        HashSet<String> currentStates = new HashSet();
        HashMap<String, Double> currentScores = new HashMap();


        Double bestScore = -5000000000.0;
        String lastWord = "";


        ArrayList<Map<String, String>> backtrace = new ArrayList<>(); /*first string is the term, second is what
            precedded it*/
        inputLine = inputLine.toLowerCase();
        String[] theseWords = inputLine.split(" ");


        currentStates.add(start);

        currentScores.put(start, 0.0);



        for (int i = 0; i < theseWords.length; i++) {

            bestScore = -5000000000.0;
            lastWord = " ";
            HashSet<String> nextStates = new HashSet();
            HashMap<String, Double> nextScores = new HashMap();


            for (String stringy : currentStates) {

                if (posProbs.containsKey(stringy)) {
                    /*for each transition currState -> nextState*/
                    for (String partOfSpeech : posProbs.get(stringy).keySet()) {

                        nextStates.add(partOfSpeech);
                        double score;


                        if (wordProbs.get(theseWords[i]) != null && wordProbs.get(theseWords[i]).containsKey(partOfSpeech)) {
                            score = currentScores.get(stringy) + posProbs.get(stringy).get(partOfSpeech) +
                                    wordProbs.get(theseWords[i]).get(partOfSpeech);
                        } else {
                            score = currentScores.get(stringy) + posProbs.get(stringy).get(partOfSpeech) + U;
                        }
//                            nextScores.put(partOfSpeech, score);

                        if (!nextScores.containsKey(partOfSpeech) || score > nextScores.get(partOfSpeech)) {
                            nextScores.put(partOfSpeech, score);

                            if (score > bestScore) {
                                bestScore = score;
                                lastWord = partOfSpeech;
                            }

                            if (backtrace.size() == i) {
                                Map<String, String> track = new HashMap<String, String>();
                                track.put(partOfSpeech, stringy);
                                backtrace.add(track);
                            } else {
                                backtrace.get(i).put(partOfSpeech, stringy);
                            }
                        }
                    }

                }

            }
            currentStates = nextStates;
            currentScores = nextScores;

        }
        output = output.concat(lastWord);
        String next = output;

        for (int i = 1; i < backtrace.size(); i++) {
            next = backtrace.get(backtrace.size() - i).get(next);
            output = output.concat("!");
            output = output.concat(next);
        }
        String output2 = new String();

        String tempString = "";
        for (int j = output.length() - 1; j >= 0; j--) {
            if (output.charAt(j) == '!') {
                String switchedTempString = new String("");
                for (int k = tempString.length() - 1; k >= 0; k--){
                    switchedTempString= switchedTempString.concat(String.valueOf(tempString.charAt(k)));
                }
                output2 = output2.concat(switchedTempString);
                output2 = output2.concat(" ");

                tempString= new String("");
            }
            else if (j == 0){
                String tempString2 = String.valueOf(output.charAt(j));
                output2 = output2.concat(tempString2);
            }
            else {
                tempString = tempString.concat(String.valueOf(output.charAt(j)));
            }
        }

        return output2;
    }

    public void consoleSolver(){
        Scanner in = new Scanner(System.in);
        String input = "";
        System.out.println("Welcome to Sudi. Type a line of text to have it tagged.\n" +
                "You can enter a line as many times as you want. \n" +
                "enter 'q' to quit");
        while(!input.equals("q")){ /*allows user to quit by breaking from while loop*/
            input = in.nextLine();
                if (input.contentEquals("q")){
                    break;
                }
            /*runs the single line viterbi method on whatever line of text the user inputs*/
            System.out.println(singleLineViterbi(input));

        }
        System.out.println("Game quit. Thanks for playing!");

    }

    /*compare file of tags to the list of tags in order generated by the viterbi method.
    * Find the number of mistakes and divide by the number of terms. Get overall accuracy rate*/
    public double performanceTest(String actualTagsFile, ArrayList posInOrder) throws IOException {
        BufferedReader tagReader = new BufferedReader(new FileReader(actualTagsFile));
        String current = "";
        int macroCounter = 0; /*over term number in huge list of parts of speech*/
        int mistakes = 0;
        int totalSize = posInOrder.size();
        while ((current = tagReader.readLine())!=null){
            int microCounter = 0; /*term within the line*/
            String[] currentLinePOSset = current.split(" ");
            for (int i = 0; i<currentLinePOSset.length; i++) {
                if (!currentLinePOSset[microCounter].contentEquals((String)posInOrder.get(macroCounter))){
                    /*if the term in one file doesn't match the equivalent term in the other file, we have a mistake*/
                    mistakes += 1;
                }
                microCounter += 1;
                macroCounter+=1;
            }
        }
        double accuracy = ((double)(totalSize-mistakes))/(double)totalSize;
        System.out.println("We've made " + mistakes + " mistakes for an accuracy of " + (totalSize-mistakes) + "/"  +
                totalSize + " or " + accuracy);
        return accuracy;
    }




    public static void main (String [] args) throws IOException {
        Sudi siri = new Sudi();
        siri.initialize("inputs/brown-train-tags.txt","inputs/brown-train-sentences.txt");
        ArrayList x = siri.viterbi("inputs/brown-test-sentences.txt");
        siri.performanceTest("inputs/brown-test-tags.txt", x);






//        siri.consoleSolver();
//        siri.performanceTest("inputs/brown-test-tags.txt",
    }

}
