package org.statnlp.example.RelationLatent;

import edu.stanford.nlp.maxent.Feature;
import org.statnlp.commons.types.WordToken;
import org.statnlp.hypergraph.FeatureArray;
import org.statnlp.hypergraph.FeatureManager;
import org.statnlp.hypergraph.GlobalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.util.instance_parser.InstanceParser;

import java.util.ArrayList;
import java.util.List;

public class LatentFeatureManager extends FeatureManager {
    public LatentFeatureManager(GlobalNetworkParam param_g) {
        super(param_g);
    }

    private enum FeatType{
      unigram, bigram, transition;
    };
    @Override
    protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k, int children_k_index) {
        RelationInstance inst=(RelationInstance)network.getInstance();
        List<Integer> fs=new ArrayList<Integer>();
        int[] paArr=network.getNodeArray(parent_k);
        if(LatentNetworkCompiler.NodeType.values()[paArr[2]]== LatentNetworkCompiler.NodeType.leaf || LatentNetworkCompiler.NodeType.values()[paArr[2]]== LatentNetworkCompiler.NodeType.root){
            return FeatureArray.EMPTY;
        }
        int pos=paArr[0];
        int currTag=paArr[1];
        int[] childArr=network.getNodeArray(children_k[0]);
        int prevTag=childArr[1];
        List<WordToken> wts=inst.input.wts;
        String word=wts.get(pos).getForm();
        String lword=(pos-1)>=0?wts.get(pos-1).getForm():"START-W"+(pos-1);
        String llword=(pos-2)>=0?wts.get(pos-2).getForm():"START-W"+(pos-2);
        String rword=(pos+1)<inst.size()?wts.get(pos+1).getForm():"END-W"+(pos+1);
        String rrword=(pos+2)<inst.size()?wts.get(pos+2).getForm():"END-W"+(pos+1);

        String POS=wts.get(pos).getTag();
        String lPOS=(pos-1)>=0?wts.get(pos-1).getTag():"START-PO"+(pos-1);
        String llPOS=(pos-2)>=0?wts.get(pos-2).getTag():"START-PO"+(pos-2);
        String rPOS=(pos+1)<inst.size()?wts.get(pos+1).getTag():"END-PO"+(pos+1);
        String rrPOS=(pos+2)<inst.size()?wts.get(pos+2).getTag():"END-PO"+(pos+1);

        //ZEROTH-ORDER FEATURES
        //UNIGRAM FEATURES
        fs.add(_param_g.toFeature(network, FeatType.unigram.name()+"-w0",  currTag+"", word));
        fs.add(_param_g.toFeature(network, FeatType.unigram.name()+"-w-1",  currTag+"", lword));
        fs.add(_param_g.toFeature(network, FeatType.unigram.name()+"-w-2",  currTag+"", llword));
        fs.add(_param_g.toFeature(network, FeatType.unigram.name()+"-w+1",  currTag+"", rword));
        fs.add(_param_g.toFeature(network, FeatType.unigram.name()+"-w+2",  currTag+"", rrword));
        fs.add(_param_g.toFeature(network, FeatType.unigram.name()+"-po0",  currTag+"", POS));
        fs.add(_param_g.toFeature(network, FeatType.unigram.name()+"-po-1",  currTag+"", lPOS));
        fs.add(_param_g.toFeature(network, FeatType.unigram.name()+"-po-2",  currTag+"", llPOS));
        fs.add(_param_g.toFeature(network, FeatType.unigram.name()+"-po+1",  currTag+"", rPOS));
        fs.add(_param_g.toFeature(network, FeatType.unigram.name()+"-po+2",  currTag+"", rrPOS));

        //BIGRAM FEATURES
        fs.add(_param_g.toFeature(network, FeatType.bigram.name()+"-w-2-1",  currTag+"", llword+" "+lword));
        fs.add(_param_g.toFeature(network, FeatType.bigram.name()+"-w-10",  currTag+"", lword+" "+word));
        fs.add(_param_g.toFeature(network, FeatType.bigram.name()+"-w0+1",  currTag+"", word+" "+rword));
        fs.add(_param_g.toFeature(network, FeatType.bigram.name()+"-w+1+2",  currTag+"", rword+" "+rrword));
        fs.add(_param_g.toFeature(network, FeatType.bigram.name()+"-po-2-1",  currTag+"", llPOS+" "+lPOS));
        fs.add(_param_g.toFeature(network, FeatType.bigram.name()+"-po-10",  currTag+"", lPOS+" "+POS));
        fs.add(_param_g.toFeature(network, FeatType.bigram.name()+"-po0+1",  currTag+"", POS+" "+rPOS));
        fs.add(_param_g.toFeature(network, FeatType.bigram.name()+"-po+1+2",  currTag+"", rPOS+" "+rrPOS));



        //FIRST-ORDER FEATURES
        //UNIGRAM FEATURES
        fs.add(_param_g.toFeature(network, FeatType.unigram.name()+"*w0",  prevTag+" "+currTag, word));
        fs.add(_param_g.toFeature(network, FeatType.unigram.name()+"*w-1",  prevTag+" "+currTag, lword));
        fs.add(_param_g.toFeature(network, FeatType.unigram.name()+"*w-2",  prevTag+" "+currTag, llword));
        fs.add(_param_g.toFeature(network, FeatType.unigram.name()+"*w+1",  prevTag+" "+currTag, rword));
        fs.add(_param_g.toFeature(network, FeatType.unigram.name()+"*w+2",  prevTag+" "+currTag, rrword));
        fs.add(_param_g.toFeature(network, FeatType.unigram.name()+"*po0",  prevTag+" "+currTag, POS));
        fs.add(_param_g.toFeature(network, FeatType.unigram.name()+"*po-1",  prevTag+" "+currTag, lPOS));
        fs.add(_param_g.toFeature(network, FeatType.unigram.name()+"*po-2",  prevTag+" "+currTag, llPOS));
        fs.add(_param_g.toFeature(network, FeatType.unigram.name()+"*po+1",  prevTag+" "+currTag, rPOS));
        fs.add(_param_g.toFeature(network, FeatType.unigram.name()+"*po+2",  prevTag+" "+currTag, rrPOS));

        //BIGRAM FEATURES
        fs.add(_param_g.toFeature(network, FeatType.bigram.name()+"*w-2-1",  prevTag+" "+currTag, llword+" "+lword));
        fs.add(_param_g.toFeature(network, FeatType.bigram.name()+"*w-10",  prevTag+" "+currTag, lword+" "+word));
        fs.add(_param_g.toFeature(network, FeatType.bigram.name()+"*w0+1",  prevTag+" "+currTag, word+" "+rword));
        fs.add(_param_g.toFeature(network, FeatType.bigram.name()+"*w+1+2",  prevTag+" "+currTag, rword+" "+rrword));
        fs.add(_param_g.toFeature(network, FeatType.bigram.name()+"*po-2-1",  prevTag+" "+currTag, llPOS+" "+lPOS));
        fs.add(_param_g.toFeature(network, FeatType.bigram.name()+"*po-10",  prevTag+" "+currTag, lPOS+" "+POS));
        fs.add(_param_g.toFeature(network, FeatType.bigram.name()+"*po0+1",  prevTag+" "+currTag, POS+" "+rPOS));
        fs.add(_param_g.toFeature(network, FeatType.bigram.name()+"*po+1+2",  prevTag+" "+currTag, rPOS+" "+rrPOS));

        fs.add(_param_g.toFeature(network, FeatType.transition.name(), currTag+"", prevTag+""));
        return this.createFeatureArray(network, fs);
    }
}