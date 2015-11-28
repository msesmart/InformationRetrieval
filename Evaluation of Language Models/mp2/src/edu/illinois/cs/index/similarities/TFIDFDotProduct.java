package edu.illinois.cs.index.similarities;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;

public class TFIDFDotProduct extends SimilarityBase {
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
    	float tf = (float) (1.0 + Math.log10(termFreq));
    	float idf = (float) Math.log10((stats.getNumberOfDocuments() + 1.0) / stats.getDocFreq());
		return tf * idf;
    }

    @Override
    public String toString() {
        return "TF-IDF Dot Product";
    }
}
