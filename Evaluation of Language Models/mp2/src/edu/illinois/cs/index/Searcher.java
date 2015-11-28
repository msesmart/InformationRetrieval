package edu.illinois.cs.index;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import edu.illinois.cs.index.similarities.DirichletPrior;
import edu.illinois.cs.index.similarities.JelinekMercer;

public class Searcher
{
    private IndexSearcher indexSearcher;
    private SpecialAnalyzer analyzer;
    private static SimpleHTMLFormatter formatter;
    private static final int numFragments = 4;
    private static final String defaultField = "content";

    /**
     * Sets up the Lucene index Searcher with the specified index.
     *
     * @param indexPath
     *            The path to the desired Lucene index.
     */
    public Searcher(String indexPath)
    {
        try
        {
            IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
            indexSearcher = new IndexSearcher(reader);
            analyzer = new SpecialAnalyzer();
            formatter = new SimpleHTMLFormatter("****", "****");
        }
        catch(IOException exception)
        {
            exception.printStackTrace();
        }
    }

    public void setSimilarity(Similarity sim)
    {
        indexSearcher.setSimilarity(sim);
    }

    /**
     * The main search function.
     * @param searchQuery Set this object's attributes as needed.
     * @return
     */
    public SearchResult search(SearchQuery searchQuery)
    {
        BooleanQuery combinedQuery = new BooleanQuery();
        for(String field: searchQuery.fields())
        {
            QueryParser parser = new QueryParser(Version.LUCENE_46, field, analyzer);
            try
            {
                Query textQuery = parser.parse(searchQuery.queryText());
                combinedQuery.add(textQuery, BooleanClause.Occur.MUST);
            }
            catch(ParseException exception)
            {
                exception.printStackTrace();
            }
        }

        return runSearch(combinedQuery, searchQuery);
    }

    /**
     * The simplest search function. Searches the abstract field and returns a
     * the default number of results.
     *
     * @param queryText
     *            The text to search
     * @return the SearchResult
     */
    public SearchResult search(String queryText)
    {
        return search(new SearchQuery(queryText, defaultField));
    }

    /**
     * Performs the actual Lucene search.
     *
     * @param luceneQuery
     * @param numResults
     * @return the SearchResult
     * @throws ParseException 
     */
    private SearchResult runSearch(Query luceneQuery, SearchQuery searchQuery)
    {
        try
        {
            System.out.println("\nScoring documents with " + indexSearcher.getSimilarity().toString());
            Similarity sim = indexSearcher.getSimilarity();
            
            int queryLength = 0;
            // have to do this to figure out query length in the LM scorers
            if(sim instanceof JelinekMercer)
            {
                Set<Term> terms = new HashSet<Term>();
                luceneQuery.extractTerms(terms);
                ((JelinekMercer) sim).setQueryLength(terms.size());
                queryLength = terms.size();
            }
            else if(sim instanceof DirichletPrior)
            {
                Set<Term> terms = new HashSet<Term>();
                luceneQuery.extractTerms(terms);
                ((DirichletPrior) sim).setQueryLength(terms.size());
                queryLength = terms.size();
            }

            TopDocs docs = indexSearcher.search(luceneQuery, searchQuery.fromDoc() + searchQuery.numResults());
            ScoreDoc[] hits = docs.scoreDocs;
            String field = searchQuery.fields().get(0);
            
            QueryParser parser = new QueryParser(Version.LUCENE_46, field, analyzer);
            if(sim instanceof JelinekMercer) {
            	double lamda = ((JelinekMercer) sim).returnLamda();
            	for(ScoreDoc hit : hits){
                	hit.score += queryLength * (float) Math.log10(lamda);
                }
            	Arrays.sort(hits, new Comparator<ScoreDoc>() {
            		public int compare(ScoreDoc a, ScoreDoc b) {
            			if (a.score < b.score) return 1;
            			else if (a.score == b.score) return 0;
            			else return -1;
            		}
            	});
            } else if (sim instanceof DirichletPrior) {
            	double mu = ((DirichletPrior) sim).returnMu();
            	for(ScoreDoc hit : hits){
            		Document doc = indexSearcher.doc(hit.doc);
            		int docLength = 0;
            		String contents = doc.getField(field).toString();
            		String parsed = parser.parse(contents).toString();
            		docLength = parsed.split(" ").length;
            		double alpha_d = mu / (mu + docLength);
                	hit.score += queryLength * (float) Math.log10(alpha_d);
                }
            	
            	Arrays.sort(hits, new Comparator<ScoreDoc>() {
            		public int compare(ScoreDoc a, ScoreDoc b) {
            			if (a.score < b.score) return 1;
            			else if (a.score == b.score) return 0;
            			else return -1;
            		}
            	});
            }
            
            SearchResult searchResult = new SearchResult(searchQuery, docs.totalHits);
            for(ScoreDoc hit : hits)
            {
                Document doc = indexSearcher.doc(hit.doc);
                ResultDoc rdoc = new ResultDoc(hit.doc);

                String highlighted = null;
                try
                {
                    Highlighter highlighter = new Highlighter(formatter, new QueryScorer(luceneQuery));
                    rdoc.title("" + (hit.doc + 1));
                    String contents = doc.getField(field).stringValue();
                    rdoc.content(contents);
                    String[] snippets = highlighter.getBestFragments(analyzer, field, contents, numFragments);
                    highlighted = createOneSnippet(snippets);
                }
                catch(InvalidTokenOffsetsException exception)
                {
                    exception.printStackTrace();
                    highlighted = "(no snippets yet)";
                }
                
                searchResult.addResult(rdoc);
                searchResult.setSnippet(rdoc, highlighted);
            }

            searchResult.trimResults(searchQuery.fromDoc());
            return searchResult;
        }
        catch(ParseException pe) {
        	;
        }
        catch(IOException exception)
        {
            exception.printStackTrace();
        }
        return new SearchResult(searchQuery);
    }

    /**
     * Create one string of all the extracted snippets from the highlighter
     * @param snippets
     * @return
     */
    private String createOneSnippet(String[] snippets)
    {
        String result = " ... ";
        for(String s: snippets)
            result += s + " ... ";
        return result;
    }
}
