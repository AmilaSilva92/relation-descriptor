package org.statnlp.example.TwitterProfileLogistic;

//import com.sun.org.apache.xpath.internal.operations.Bool;
import org.statnlp.commons.types.WordToken;

import java.util.Collections;
import java.util.List;

public class TwitterInput {
    List<WordToken> wts;
    String entity;
    String user;
    List<Integer> eStart;
    List<Integer> eEnd;
    Float nell;
    List<String> neighbours;

    TwitterInput(String userString,List<WordToken> wts, String entityString, List<Integer> eStart, List<Integer> eEnd){
        this.wts=wts;
        this.entity=entityString;
        this.user=userString;
        this.eStart=eStart;
        this.eEnd=eEnd;
        this.nell = Float.parseFloat("0");
        this.neighbours = Collections.emptyList();
    }

    TwitterInput(String userString, List<WordToken> wts, String entityString, List<Integer> eStart, List<Integer> eEnd, Float nell, List<String> neighbours){
        this.wts=wts;
        this.entity=entityString;
        this.user=userString;
        this.eStart=eStart;
        this.eEnd=eEnd;
        this.nell = nell;
        this.neighbours = neighbours;
    }

    public void setEntity(String entity){
        this.entity=entity;
    }
    int size(){
        return wts.size();
    }
}
