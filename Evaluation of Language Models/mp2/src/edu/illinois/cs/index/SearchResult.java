package edu.illinois.cs.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Represents results returned by Lucene. Includes the list of search result
 * objects, the original query, and the highlighted snippets.
 */
public class SearchResult {
    private ArrayList<ResultDoc> results;
    private int totalHits;
    private SearchQuery searchQuery;
    private HashMap<Integer, String> htmlSnippets; // map id to highlighted
                                                    // string

    /**
     * Default constructor to represent an empty search result.
     * @param searchQuery
     */
    public SearchResult(SearchQuery searchQuery) {
        totalHits = 0;
        results = new ArrayList<ResultDoc>();
        this.searchQuery = searchQuery;
        htmlSnippets = new HashMap<Integer, String>();
    }

    /**
     * Constructor.
     * @param searchQuery
     * @param totalHits
     */
    public SearchResult(SearchQuery searchQuery, int totalHits) {
        this.results = new ArrayList<ResultDoc>();
        this.totalHits = totalHits;
        this.searchQuery = searchQuery;
        htmlSnippets = new HashMap<Integer, String>();
    }

    /**
     * Adds a search result to this object.
     * @param rdoc
     */
    public void addResult(ResultDoc rdoc) {
        results.add(rdoc);
    }

    /**
     * Set the highlighted text for this document.
     * @param rdoc
     * @param snippet
     */
    public void setSnippet(ResultDoc rdoc, String snippet) {
        htmlSnippets.put(rdoc.id(), snippet);
    }

    /**
     * @param rdoc
     * @return the snippets for the given document
     */
    public String getSnippet(ResultDoc rdoc) {
        return htmlSnippets.get(rdoc.id());
    }

    /**
     * @return the query that returned this result
     */
    public SearchQuery query() {
        return searchQuery;
    }

    /**
     * @return an ArrayList of ResultDocs matching the query
     */
    public ArrayList<ResultDoc> getDocs() {
        return results;
    }

    /**
     * @return the total number of hits from the query
     */
    public int numHits() {
        return totalHits;
    }

    /**
     * Temporarily used to create paginated results.
     * @param from
     * @param to
     */
    public void trimResults(int from) {
        // bounds checking
        if (from >= results.size()) {
            results = new ArrayList<ResultDoc>();
            return;
        }

        int to = results.size();

        // trimming
        List<ResultDoc> newResults = results.subList(from, to);
        results = new ArrayList<ResultDoc>(newResults);
    }

    /**
     * Tells whether two objects are both SearchQueries with equal contents.
     * @param other
     * @return true if the objects are equal
     */
    public boolean equals(Object other) {
        if (!(other instanceof SearchResult))
            return false;

        SearchResult otherResult = (SearchResult) other;
        return otherResult.searchQuery.equals(searchQuery)
                && otherResult.results == results
                && otherResult.totalHits == totalHits;

    }
}
