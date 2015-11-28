package edu.illinois.cs.index;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;

/**
 * Analyzer performs the following functions:
 *  - make all tokens lowercase
 *  - remove stop words based on standard list
 *  - have max token length of 35 characters
 *  - run porter stemmer on each token
 */
public class SpecialAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String fieldName,
            Reader reader) {
        Tokenizer source = new StandardTokenizer(Version.LUCENE_46, reader);
        TokenStream filter = new StandardFilter(Version.LUCENE_46, source);
        filter = new LowerCaseFilter(Version.LUCENE_46, filter);
        filter = new LengthFilter(Version.LUCENE_46, filter, 2, 35);
        filter = new StopFilter(Version.LUCENE_46, filter, StopFilter.makeStopSet(Version.LUCENE_46, Stopwords.STOPWORDS));
        filter = new PorterStemFilter(filter);
        return new TokenStreamComponents(source, filter);
    }
}
