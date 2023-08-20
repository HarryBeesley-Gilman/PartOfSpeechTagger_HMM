import java.io.*;

import java.io.BufferedReader;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;


/**
 * @author Harry Beesley-Gilman
 * Date 6/24/22
 * Purpose: Code SudiTrainer to "train" a Sudi object using given files
 */
public class SudiTrainer {

    public static ArrayList trainposProbs(String tags, String text, HashMap<String, HashMap<String,Double>> posProbs, HashMap<String, HashMap<String, Double>> wordProbs) throws IOException {

        BufferedReader tagReader = new BufferedReader(new FileReader(tags));
        String current = tagReader.readLine();


        posProbs.put("#", new HashMap());
        /*we always start a line with #. The following hashMap, once completed, will tell us the likelihood of various
        * parts of speech beginning a sentence*/

        ArrayList<String> listOfTags = new ArrayList(); /*this will be in the order they appear*/

        /*at this point I need to go through every part of speech*/
        while (current!=null){

            String[] thesePOS = current.split(" ");
            /*set of strings labelling the POS for a line*/


            for(int i = 0; i<thesePOS.length; i++) { /*posProbs matches a part of speech to a hash map
             * That hash map contains each part of speech it has led to and the number of instances of this*/
                listOfTags.add(thesePOS[i]);

                /*we have to handle the starts*/
                if (i == 0) {
                    /*Go in, put the transition into posProbs if necessary. Increment its frequency value by one*/
                    if (!posProbs.get("#").isEmpty()) {
                        if (posProbs.get("#").containsKey(thesePOS[i])) {
                            Double tempValue = posProbs.get("#").get(thesePOS[0]);
                            tempValue += 1.0;
                            posProbs.get("#").replace(thesePOS[0], tempValue);
                        }
                        else { /*if we have simply not seen this POS begin a sentence yet.*/
                            HashMap tempMap = posProbs.get("#");
                            tempMap.put(thesePOS[0], 1.0);
                            posProbs.put("#", tempMap);
                        }
                    }
                    else {
                        HashMap temp = new HashMap();
                        temp.put(thesePOS[0], 1.0);
                        posProbs.put("#", temp);
                    }
                }
                /*if we are at the end of a line, we must note that term's existence and that it does not point to anything
                *(here, an empty HashMap)*/
                if (i == thesePOS.length - 1) {
                    if (!posProbs.containsKey(thesePOS[i])) {
                        HashMap<String, Double> empty = new HashMap();
                        posProbs.put(thesePOS[i], empty);
                    }
                }
                else { /*if we aren't on the last term*/
                    if (posProbs.containsKey(thesePOS[i])) { /*If we have seen one part of speech lead to the other,
                simply add one to that frequency. If we have not seen it, add an entry to the interior map with a freq
                of one.*/
                        HashMap<String, Double> temp2 = posProbs.get(thesePOS[i]);
                        if (temp2.containsKey(thesePOS[i + 1])) {
                            temp2.put(thesePOS[i + 1], temp2.get(thesePOS[i + 1]) + 1.0);
                        }
                        else {
                            temp2.put(thesePOS[i + 1], 1.0);
                        }
                        posProbs.put(thesePOS[i], temp2);
                        }

                    /*if we haven't seen this POS lead to the next POS, make a new entry in thesePOS for our current
                    part of speech*/
                    else {
                        HashMap temp = new HashMap();
                        temp.put(thesePOS[i + 1], 1.0); /*enter a hashmap pointed from the part of speech to the next part of speech
                            with a probability of one*/
                        posProbs.put(thesePOS[i], temp);
                        }
                    }

                }
            /*advance lines*/
            current=tagReader.readLine();
        }

        /*now we must update the probabilities to be logs*/

        for (String POS: posProbs.keySet()){
            double totalEntries = 0.0;
            /*total number of entries is useful info for below*/
            for (String nextPOS: posProbs.get(POS).keySet()){
                totalEntries += posProbs.get(POS).get(nextPOS);
            }
            /*change counts for the natural log of the frequency/overall number*/
            for (String nextPOS: posProbs.get(POS).keySet()){
                double counts = posProbs.get(POS).get(nextPOS);
                posProbs.get(POS).replace(nextPOS, Math.log(counts/totalEntries));
            }
        }
        return listOfTags;
    }

    public static void trainWordProbs(String tags, String text, HashMap<String, HashMap<String,Double>> posProbs, HashMap<String, HashMap<String, Double>> wordProbs, ArrayList listOfTags) throws IOException {
        BufferedReader wordReader = new BufferedReader(new FileReader(text));
        String current = wordReader.readLine().toLowerCase();

        int counter = 0; /*while we run through each word in the training file, we need to be able to reference which
        part of speech we are dealing with. These are stored in listOfTags*/

        while (current != null) {

            String[] theseWords = current.split(" ");
            for (int i = 0; i < theseWords.length; i++) {
                /*run through words in the line*/

                /*if we haven't seen the word we must add an entry with the associated POS and a freq of 1*/
                if (!wordProbs.containsKey(theseWords[i])) {
                    HashMap<String, Double> temp = new HashMap();
                    temp.put((String) listOfTags.get(counter), 1.0);
                    wordProbs.put(theseWords[i], temp);
                }
                else { /*add an entry for a new POS if need be. Regardless, update count with temp2 to include this new
                 data, raising the frequency.*/

                    HashMap<String, Double> temp2 = wordProbs.get(theseWords[i]);
                    if (temp2.containsKey(listOfTags.get(counter))) {
                        temp2.put((String) listOfTags.get(counter), temp2.get(listOfTags.get(counter)) + 1);
                    }
                    else {
                        temp2.put((String) listOfTags.get(counter), 1.0);
                    }
                    wordProbs.put(theseWords[i], temp2);
                }
                /*again, counter helps us track where we are in our listOfTags*/
                counter += 1;
            }
            /*advance within file*/
            current = wordReader.readLine();
        }
        /*update probabilities for logs as in the other method*/

        for (String word: wordProbs.keySet()){

            double totalEntries = 0;
            for (String POS: wordProbs.get(word).keySet()){
                totalEntries += wordProbs.get(word).get(POS);
            }
            for (String POS: wordProbs.get(word).keySet()){
                double counts = wordProbs.get(word).get(POS);
                wordProbs.get(word).replace(POS, Math.log(counts/totalEntries));
            }

        }
    }

}
