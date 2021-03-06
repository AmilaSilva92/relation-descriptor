package org.statnlp.example.TwitterProfileLogistic;

import org.apache.commons.collections15.map.HashedMap;
import org.statnlp.commons.io.RAWF;
import org.statnlp.commons.ml.opt.OptimizerFactory;
import org.statnlp.commons.types.Instance;
import org.statnlp.commons.types.WordToken;
import org.statnlp.example.linear_ne.Entity;
import org.statnlp.hypergraph.DiscriminativeNetworkModel;
import org.statnlp.hypergraph.GlobalNetworkParam;
import org.statnlp.hypergraph.NetworkConfig;
import org.statnlp.hypergraph.NetworkModel;
import scala.Int;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

public class TwitterLogisticMain {
    private static String trainFile="edu_train_final_clean.txt";
    private static String testFile="edu_test_final_clean.txt";
    private static int iterCount=100;
    private static int trainCount=10;
    private static int testCount=10;
    private static Map<String, Float> nellGlobalFeatures = new HashedMap<>();
    private static Map<String, List<String>> neighborGlobalFeatures = new HashedMap<>();

    public static void main(String...args) throws IOException, InterruptedException{
        boolean global = false;

        if(args.length != 0) {
            trainCount = Integer.parseInt(args[0]);
            testCount = Integer.parseInt(args[1]);
            global = Boolean.parseBoolean(args[2]);
        }

        nellGlobalFeatures = readNellFeatures("output_nell.txt");
        neighborGlobalFeatures = readNeighborFeatures("output_network.txt");


//        for(String key:nellGlobalFeatures.keySet()){
//            System.out.println(key);
//            System.out.println(nellGlobalFeatures.get(key));
//        }
//
//        for(String userEntity: neighborGlobalFeatures.keySet()){
//            System.out.println(userEntity);
//            System.out.println(neighborGlobalFeatures.get(userEntity));
//        }

        TwitterInstance[] trainInsts=readData(trainFile, true, trainCount);
        TwitterInstance[] testInsts=readData(testFile,false, testCount);

        Map<String, Integer> groundTruth=new HashMap<>();

        Set<String> userEntity=new HashSet<String>();
        for(int i=0; i<testInsts.length; i++){
            userEntity.add(testInsts[i].input.user.split("\\$")[0] + testInsts[i].input.entity);
            if(testInsts[i].output==1){
                groundTruth.put(testInsts[i].input.user.split("\\$")[0] + testInsts[i].input.entity,1);
            }
            else{
                groundTruth.put(testInsts[i].input.user.split("\\$")[0] + testInsts[i].input.entity,-1);
            }
        }
        if(global){
            Map<String, List<TwitterInstance> > globalTrainMap=createGlobalMap(trainInsts);
            Map<String, List<TwitterInstance> > globalTestMap=createGlobalMap(testInsts);
            trainInsts=createGlobalInsts(globalTrainMap, trainInsts.length, true);
            testInsts=createGlobalInsts(globalTestMap, testInsts.length, false);
        }
        NetworkModel model=null;
        NetworkConfig.PARALLEL_FEATURE_EXTRACTION = true;
        NetworkConfig.AVOID_DUPLICATE_FEATURES = true;
        NetworkConfig.NUM_THREADS=40;
        GlobalNetworkParam gnp= new GlobalNetworkParam(OptimizerFactory.getLBFGSFactory());
        TwitterLogisticFeatureManager fman = new TwitterLogisticFeatureManager(gnp);
        TwitterLogisticNetworkCompiler networkCompiler = new TwitterLogisticNetworkCompiler();
        model = DiscriminativeNetworkModel.create(fman, networkCompiler);
        model.train(trainInsts, iterCount);
        Instance[] results=model.decode(testInsts);

        TwitterLogisticEvaluator eval=new TwitterLogisticEvaluator(results, userEntity, groundTruth, global);
        System.out.println(eval.precision);
        System.out.println(eval.recall);
        System.out.println(eval.fscore);
    }
    public static Map<String, List<TwitterInstance>> createGlobalMap(TwitterInstance insts[]){
        int i;
        Map<String, List<TwitterInstance> > map=new HashMap<String, List<TwitterInstance> > ();
        for(i=0; i<insts.length; i++){
            String key=insts[i].input.user.split("\\$")[0]+"$"+insts[i].input.entity;;
            System.out.println(key);System.out.println("\n");
            if(map.containsKey(key)){
                map.get(key).add(insts[i]);
            }
            else{
                List<TwitterInstance> newList=new ArrayList<TwitterInstance>();
                newList.add(insts[i]);
                map.put(key,newList);
            }
        }
        return map;
    }

    public static TwitterInstance[] createGlobalInsts(Map<String, List<TwitterInstance>> globalMap, int count, boolean isTraining){
        List<TwitterInstance> insts=new ArrayList<TwitterInstance>();
        int instanceId=0;
        for(String key: globalMap.keySet()){
            int size=globalMap.get(key).size();
            List<WordToken> wts=new ArrayList<WordToken>();
            List<Integer> eStart=new ArrayList<Integer>();
            List<Integer> eEnd=new ArrayList<Integer>();
            int offset=0;
            String entityString=globalMap.get(key).get(0).input.entity;
            String userString=globalMap.get(key).get(0).input.user;
            int output=globalMap.get(key).get(0).output;
            for(int i=0; i<globalMap.get(key).size(); i++){
               TwitterInstance inst=globalMap.get(key).get(i);
               for(int j=0; j<inst.input.eStart.size(); j++) {
                   eStart.add(inst.input.eStart.get(j) + offset);
                   eEnd.add(inst.input.eEnd.get(j) + offset);
               }
                for(WordToken wt:inst.input.wts){
                    wts.add(wt);
                }
                offset+=inst.input.wts.size();
           }
           TwitterInput input=new TwitterInput(userString, wts,entityString, eStart,eEnd);
            instanceId++;
            TwitterInstance newInst=new TwitterInstance(instanceId, 1.0,input, output);
            if(isTraining){newInst.setLabeled();}
            else{newInst.setUnlabeled();}
            insts.add(newInst);
        }
        return insts.toArray(new TwitterInstance[insts.size()]);
    }

    public static void findEntityString(List<WordToken> wts, String entityString, List<Integer> eStart, List<Integer> eEnd){
        String[] e=entityString.split(" ");
        for(int i=0; i<wts.size(); i++){

            if(wts.get(i).getForm().equals(e[0])){
                int j;
                for(j=1; j<e.length && i+j<wts.size(); j++){
                    if(!e[j].equals(wts.get(i+j).getForm())){
                        break;
                    }

                }
                if(j==e.length){
                    eStart.add(i);
                    eEnd.add(i+j-1);
                }
            }
        }
    }

    public static TwitterInstance[] readData(String textFname, boolean isTraining, int lim) throws IOException{
        List<TwitterInstance> insts=new ArrayList<TwitterInstance>();
        BufferedReader br=RAWF.reader(textFname);
        String line=null;
        int count=0;
        String userString="";
        List<WordToken> wts=new ArrayList<WordToken>();
        String entityString="";
        int out=-1;
        int instanceId=0;
        List<Integer> eStart;
        List<Integer> eEnd;
        int wrongCount=0;
        int emptyEntity=0;
        while((line=br.readLine())!=null) {
//            System.out.println(instanceId);
            count++;
            if(lim!=-1 && instanceId==lim){
                break;
            }
            if(count%6==1){
                userString=new String(line);
            }
            else if(count%6==2){
                wts=new ArrayList<WordToken>();
                if(line.charAt(1)=='-'){
                    out=-1;
                }
                else{
                    out=1;
                }
                line=line.substring(4);
                String line_split[]=line.split(" ");
                for(String word:line_split){
                    WordToken wt=new WordToken(word, "TAG");
                    wts.add(wt);
                }
            }
            else if(count%6==3){
                String line_split[]=line.split(" ");
                for(int i=0; i<wts.size(); i++){
                    wts.get(i).setTag(line_split[i]);
                }
            }
            else if(count%6==4){
                String line_split[]=line.split(" ");
//                System.out.println("LENGTH:"+wts.size());
//                System.out.println("LENGTH:"+line_split.length);
                for(int i=0; i<wts.size(); i++){
                    wts.get(i).setPhraseTag(line_split[i]);
                }
            }
            else if(count%6==5) {
                entityString = new String(line);
                eStart=new ArrayList<Integer>();
                eEnd=new ArrayList<Integer>();
                findEntityString(wts,entityString, eStart, eEnd);

                Float nell = Float.parseFloat("0");
                if(nellGlobalFeatures.containsKey(entityString)){
                    nell = nellGlobalFeatures.get(entityString);
                }

                List<String> neighbors = new ArrayList<>();
                String userEntityPair = userString.split("\\$")[0]+"$"+entityString;
                if(neighborGlobalFeatures.containsKey(userEntityPair)){
                    neighbors = neighborGlobalFeatures.get(userEntityPair);
                }
                
                TwitterInput input = new TwitterInput(userString, wts, entityString, eStart, eEnd, nell, neighbors);
                instanceId++;
                TwitterInstance inst=new TwitterInstance(instanceId, 1.0, input,out);
                if(isTraining){inst.setLabeled();}
                else{inst.setUnlabeled();}
                inst.toString();
                if(eStart.size()==0 || eEnd.size()==0){
                    wrongCount++;
//                    inst.toString();
                    continue;
                }
                insts.add(inst);
                if(inst.input.entity.length()==0){
                    System.out.println("hi\n");
                    emptyEntity++;
                }
            }
        }
        System.out.println("EMPTY ENTITY:");
        System.out.println(emptyEntity);
        System.out.println("WRONG COUNT:");
        System.out.println(wrongCount);
        return insts.toArray(new TwitterInstance[insts.size()]);
    }

    public static Map<String, Float> readNellFeatures(String pathName) throws IOException {
        Map<String, Float> nellFeature=new HashMap<>();
        BufferedReader br=RAWF.reader(pathName);
        String line = null;

        while((line=br.readLine())!=null) {
            String line_split[]=line.split("\\|");
            nellFeature.put(line_split[0], Float.parseFloat(line_split[1]));
        }
        return nellFeature;
    }

    public static Map<String, List<String>> readNeighborFeatures(String pathName) throws IOException {
        Map<String, List<String> > map=new HashMap<> ();
        BufferedReader br = RAWF.reader(pathName);
        String line;
        int i;
        while((line=br.readLine())!=null) {
            String line_split[]=line.split("\\|");
            List<String> neighbors = new ArrayList<>();

            for(i = 1; i < line_split.length; i++){
                neighbors.add(line_split[i]);
            }
            map.put(line_split[0], neighbors);
        }

        return map;
    }
}
