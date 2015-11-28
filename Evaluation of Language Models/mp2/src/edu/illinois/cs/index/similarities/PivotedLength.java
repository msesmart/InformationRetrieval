package edu.illinois.cs.index.similarities;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;

public class PivotedLength extends SimilarityBase {
    /**
     * Returns a score for a single term in the document.
     *
     * @param stats
     *            Provides access to corpus-level statistics
     * @param termFreq
     * @param docLength
     */
    @Override
    /*
    protected float score(BasicStats stats, float termFreq, float docLength) {
    	float s = 0.2f;
    	float queryFreq = 1.0f;
    	float p1 = (float) (1 + Math.log(1 + Math.log(termFreq))) / (1 - s + s * docLength / stats.getAvgFieldLength());
    	float p2 = (float) Math.log((stats.getNumberOfDocuments() + 1) / stats.getDocFreq()) * queryFreq;
        return p1 * p2;
    }*/
    
    protected float score(BasicStats stats, float termFreq, float docLength) {
    	float s = 0.2f;
    	float queryFreq = 1.0f;
    	double k = 0.5;
    	float p1 = (float) (1 + Math.log(1 + Math.log(termFreq)));
    	//float weight = (float) Math.pow(stats.getNumberOfDocuments() / stats.getDocFreq(), k);
    	float weight = (float) Math.log10((stats.getNumberOfDocuments() + 1) / stats.getDocFreq());
    	float p2 = queryFreq * weight;
    	float p3 = (float) ((stats.getAvgFieldLength() + s) / (stats.getAvgFieldLength() + docLength * s));
        return p1 * p2 * p3;
    }

    @Override
    public String toString() {
        return "Pivoted Length Normalization";
    }

}
