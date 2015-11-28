package edu.illinois.cs.index.similarities;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;

public class OkapiBM25 extends SimilarityBase {
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
    	float k1 = 1.2f;
    	float k2 = 750.0f;
    	float b = 0.75f;
    	float queryFreq = 1.0f;
    	float p1 = (float) Math.log((stats.getNumberOfDocuments() - stats.getDocFreq() + 0.5) / (stats.getDocFreq() + 0.5));
        float p2 = (float) (k1 + 1) * termFreq / (k1 * (1 - b + b * docLength / stats.getAvgFieldLength()) + termFreq);
        float p3 = (float) (k2 + 1) * queryFreq / (k2 + queryFreq);
    	return p1 * p2 *p3;
    }

    @Override
    public String toString() {
        return "Okapi BM25";
    }

}
