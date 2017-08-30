package org.statnlp.example.RelationLatent;

import org.statnlp.commons.types.Instance;
import org.statnlp.example.base.BaseNetwork;
import org.statnlp.hypergraph.LocalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkCompiler;
import org.statnlp.hypergraph.NetworkIDMapper;

import javax.xml.soap.Node;
import java.util.*;

public class LatentNetworkCompiler extends NetworkCompiler {
    private List<String> relTypes=new ArrayList<String>();
    private List<String> relTags=new ArrayList<String>();
    private Map<String, Integer> relType2Id=new HashMap<String, Integer>();
    private Map<String, Integer> relTag2Id=new HashMap<String, Integer>();
    protected enum NodeType{
        root, leaf, tag;
    };

    static{
        NetworkIDMapper.setCapacity(new int[]{150, 100, 3, 100000});
    }
    public long toLeafNode(){
        return toNode(0,0, NodeType.leaf, 0);
    }
    public long toRootNode(int size){
        return toNode(size-1, relTags.size(), NodeType.root, size);
    }
    public long toTagNode(int pos, int tagId, int chainId){
        return toNode(pos, tagId, NodeType.tag, chainId);
    }
    public long toNode(int pos, int tagId, NodeType nodeType, int chainId){
        return NetworkIDMapper.toHybridNodeID(new int[]{pos, tagId, nodeType.ordinal(), chainId});
    }
    public LatentNetworkCompiler(List<String> relTypes) {
        this.relTypes=relTypes;
        for(int i=0; i<relTypes.size(); i++){
            relType2Id.put(relTypes.get(i), i+1);
            relTags.add("B-"+relTypes.get(i));
            relTags.add("I-"+relTypes.get(i));
        }
        relTags.add("O");
        for(int i=0; i<relTags.size(); i++){
            relTag2Id.put(relTags.get(i),i+1);
        }
    }
    private boolean covers(int el, int left, int right){
        if(el>=left && el<=right){
            return true;
        }
        return false;
    }
    @Override
    public Network compileLabeled(int networkId, Instance inst, LocalNetworkParam param) {
        BaseNetwork.NetworkBuilder<BaseNetwork> builder=BaseNetwork.NetworkBuilder.builder();
        RelationInstance myInst=(RelationInstance) inst;
        int size=myInst.size();
        long leaf=toLeafNode();
        long root=toRootNode(size);
        builder.addNode(leaf);
        builder.addNode(root);
        int e1=myInst.input.e1Pos;
        int e2=myInst.input.e2Pos;
        long child=leaf;
        String relType=myInst.output.relType;
        int relId=this.relType2Id.get(relType);
        for(int left=0; left<size; left++){
            for(int right=left; right<inst.size(); right++){
                if(covers(e1, left, right)  || covers(e2, left, right)){
                    continue;
                }
                child=leaf;
                int chainId=relId*1000+left*100+right;
                for(int pos=0; pos<size; pos++){
                    String tag;
                    if(covers(pos, left, right)){
                        if(pos==left){
                            tag="B-"+relType;
                        }
                        else{
                            tag="I-"+relType;
                        }
                    }
                    else{
                        tag="O";
                    }
                    int tagId=relTag2Id.get(tag);
                    long tagNode=toTagNode(pos, tagId,chainId);
                    builder.addNode(tagNode);
                    builder.addEdge(tagNode, new long[]{child});
                    child=tagNode;
                }
                builder.addEdge(root, new long[]{child});
            }
        }
        int left=size; int right=size;
        int chainId=left*100+right;
        child=leaf;
        for(int pos=0; pos<size; pos++){
            long tagNode=toTagNode(pos, relTag2Id.get("O"), chainId);
            builder.addNode(tagNode);
            builder.addEdge(tagNode, new long[]{child});
            child=tagNode;
        }
        builder.addEdge(root, new long[]{child});
        return builder.build(networkId, inst, param, this);
    }

    @Override
    public Network compileUnlabeled(int networkId, Instance inst, LocalNetworkParam param) {
        BaseNetwork.NetworkBuilder builder=BaseNetwork.NetworkBuilder.builder();
        RelationInstance myInst=(RelationInstance)inst;
        int size=inst.size();
        int e1=myInst.input.e1Pos;
        int e2=myInst.input.e2Pos;
        long leaf=toLeafNode();
        long root=toRootNode(size);
        long child=leaf;

        for(int i=0; i<this.relTypes.size(); i++){
            String relType=this.relTypes.get(i);
            int relId=this.relType2Id.get(relType);
            for(int left=0; left<size; left++){
                for(int right=left; right<inst.size(); right++){
                    if(covers(e1, left, right)  || covers(e2, left, right)){
                        continue;
                    }
                    child=leaf;
                    int chainId=relId*1000+left*100+right;
                    for(int pos=0; pos<size; pos++){
                        String tag;
                        if(covers(pos, left, right)){
                            if(pos==left){
                                tag="B-"+relType;
                            }
                            else{
                                tag="I-"+relType;
                            }
                        }
                        else{
                            tag="O";
                        }
                        int tagId=relTag2Id.get(tag);
                        long tagNode=toTagNode(pos, tagId,chainId);
                        builder.addNode(tagNode);
                        builder.addEdge(tagNode, new long[]{child});
                        child=tagNode;
                    }
                    builder.addEdge(root, new long[]{child});
                }
            }
        }
        int left=size; int right=size;
        int chainId=left*100+right;
        child=leaf;
        for(int pos=0; pos<size; pos++){
            long tagNode=toTagNode(pos, relTag2Id.get("O"), chainId);
            builder.addNode(tagNode);
            builder.addEdge(tagNode, new long[]{child});
            child=tagNode;
        }
        builder.addEdge(root, new long[]{child});
        return builder.build(networkId, inst, param, this);
    }

    @Override
    public Instance decompile(Network network) {
        BaseNetwork unlablledNetwork=(BaseNetwork)network;
        RelationInstance inst=(RelationInstance)unlablledNetwork.getInstance();
        int size=inst.size();
        long root=toRootNode(size);
        int rootIdx= Arrays.binarySearch(unlablledNetwork.getAllNodes(), root);
        List<String> predictions=new ArrayList<String>(size);
        for(int i=0; i<size; i++){
            int child=unlablledNetwork.getMaxPath(rootIdx)[0];
            int[] childArr=unlablledNetwork.getNodeArray(child);
            predictions.add(0, relTags.get(childArr[1]));
            rootIdx=child;
        }
        inst.setPrediction(predictions);
        return inst;
    }
}