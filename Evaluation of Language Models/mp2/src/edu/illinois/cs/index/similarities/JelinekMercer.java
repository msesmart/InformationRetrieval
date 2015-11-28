package edu.illinois.cs.index.similarities;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.LMSimilarity;

public class JelinekMercer extends LMSimilarity {

    private LMSimilarity.DefaultCollectionModel model; // this would be your reference model
    private float queryLength = 0; // will be set at query time automatically
    private double lamda = 0.1f;
    public JelinekMercer() {
        model = new LMSimilarity.DefaultCollectionModel();
    }

    /**
     * Returns a score for a single term in the document.
     *
     * @param stats
     *            Provides access to corpus-level statistics
     * @param termFreq
     * @param docLength
     */
    @Override
    protected float score(BasicStats stats, float termFreq, float docLength) {	
    	float p_s = (float) ((1 - lamda) * termFreq / docLength + lamda * model.computeProbability(stats));
        return (float) Math.log10(p_s / lamda / model.computeProbability(stats));
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public String getName() {
        return "Jelinek-Mercer Language Model";
    }

    public void setQueryLength(float length) {
        queryLength = length;
    }
    
    public double returnLamda() {
    	return lamda;
    }

}
