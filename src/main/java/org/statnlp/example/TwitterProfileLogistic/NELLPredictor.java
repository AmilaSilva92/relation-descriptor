package org.statnlp.example.TwitterProfileLogistic;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class NELLPredictor {
    // This method will call process the request from Nell
    // returns average score
    public static float getScore(String entityName, String predicate) throws IOException{
        float score = 0;

        entityName = URLEncoder.encode(entityName);
        predicate = URLEncoder.encode(predicate);

        String url = "http://rtw.ml.cmu.edu/rtw/api/json0?lit1="+entityName+"&predicate="+ predicate +"&agent=KI%2CCKB%2COPRA%2COCMC";
        URL urlObj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();

        // set as GET method
        con.setRequestMethod("GET");
        BufferedReader in = new BufferedReader( new InputStreamReader(con.getInputStream()));
        String response = in.readLine();

        // process the response and get the output score
        int i = 0;
        int j = 0;

        // number of scores
        int count = 0;

        while(i < response.length() - 10){
            if (response.substring(i, i+10).equals("\"score\" : ")){
                i += 10;
                j = i;

                while(response.charAt(j) != ','){
                    j ++;
                }

                score += Float.parseFloat(response.substring(i, j));
                count ++;
            }

            i ++;
        }

        return count==0 ? 0 : score/count;
    }

    // This method will call process the request from Nell
    // returns maxScore
    public static float getMaxScore(String entityName, String predicate) throws IOException{
        float score;
        float maxScore = 0;


        entityName = URLEncoder.encode(entityName);
        predicate = URLEncoder.encode(predicate);

        String url = "http://rtw.ml.cmu.edu/rtw/api/json0?lit1="+entityName+"&predicate="+ predicate +"&agent=KI%2CCKB%2COPRA%2COCMC";
        URL urlObj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();

        // set as GET method
        con.setRequestMethod("GET");
        BufferedReader in = new BufferedReader( new InputStreamReader(con.getInputStream()));
        String response = in.readLine();


        // process the response and get the output score
        int i = 0;
        int j;

        while(i < response.length() - 10){
            if (response.substring(i, i+10).equals("\"score\" : ")){
                i += 10;
                j = i;

                while(response.charAt(j) != ','){
                    j ++;
                }

                score = Float.parseFloat(response.substring(i, j));
                if(score > maxScore){
                    maxScore = score;
                }
            }

            i ++;
        }

        return maxScore;
    }

    public static void main(String[] args) throws IOException{
        // insert the predicate in all simple letters
        System.out.println(getMaxScore("", "school"));
    }

}
