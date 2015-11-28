package edu.illinois.cs.index;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import edu.illinois.cs.index.similarities.*;

public class Runner {
	//please keep those constants 
    final static String _dataset = "npl";
    final static String _indexPath = "lucene-npl-index";
    final static String _prefix = "data/";
    final static String _file = "npl.txt";

////This enables you to interact with the program in command line
//    public static void main(String[] args) throws IOException {
//        if (args.length == 1 && args[0].equalsIgnoreCase("--index"))
//            Indexer.index(_indexPath, _prefix, _file);
//        else if (args.length >= 1 && args[0].equalsIgnoreCase("--search"))
//        {
//            String method = null;
//            if (args.length == 2)
//                method = args[1];
//            interactiveSearch(method);
//        }
//        else
//        {
//            System.out.println("Usage: --index to index or --search to search an index");
//            System.out.println("If using \"--search\",");
//            printUsage();
//        }
//    }
    
////This makes it easier for you to run the program in an IDE
    public static void main(String[] args) throws IOException {
    	//To crate the index
    	//NOTE: you need to create the index once, and you cannot call this function twice without removing the existing index files
    	Indexer.index(_indexPath, _prefix, _file);
        
        //Interactive searching function with your selected ranker
    	//NOTE: you have to create the index before searching!
    	//String method = "--bdp";//specify the ranker you want to test
        //interactiveSearch(method);
    }

    /**
     * Feel free to modify this function, if you want different display!
     *
     * @throws IOException
     */
    private static void interactiveSearch(String method) throws IOException {
        Searcher searcher = new Searcher(_indexPath);
        setSimilarity(searcher, method);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Type text to search, blank to quit.");
        System.out.print("> ");
        String input;
        while ((input = br.readLine()) != null && !input.equals("")) {
            SearchResult result = searcher.search(input);
            ArrayList<ResultDoc> results = result.getDocs();
            int rank = 1;
            if (results.size() == 0)
                System.out.println("No results found!");
            for (ResultDoc rdoc : results) {
                System.out.println("\n------------------------------------------------------");
                System.out.println(rank + ". " + rdoc.title());
                System.out.println("------------------------------------------------------");
                System.out.println(result.getSnippet(rdoc)
                        .replaceAll("\n", " "));
                ++rank;
            }
            System.out.print("> ");
        }
    }

    public static void setSimilarity(Searcher searcher, String method) {
        if(method == null)
            return;
        else if(method.equals("--dp"))
            searcher.setSimilarity(new DirichletPrior());
        else if(method.equals("--jm"))
            searcher.setSimilarity(new JelinekMercer());
        else if(method.equals("--ok"))
            searcher.setSimilarity(new OkapiBM25());
        else if(method.equals("--pl"))
            searcher.setSimilarity(new PivotedLength());
        else if(method.equals("--tfidf"))
            searcher.setSimilarity(new TFIDFDotProduct());
        else if(method.equals("--bdp"))
            searcher.setSimilarity(new BooleanDotProduct());
        else
        {
            System.out.println("[Error]Unknown retrieval function specified!");
            printUsage();
            System.exit(1);
        }
    }

    private static void printUsage()
    {
        System.out.println("To specify a ranking function, make your last argument one of the following:");
        System.out.println("\t--dp\tDirichlet Prior");
        System.out.println("\t--jm\tJelinek-Mercer");
        System.out.println("\t--ok\tOkapi BM25");
        System.out.println("\t--pl\tPivoted Length Normalization");
        System.out.println("\t--tfidf\tTFIDF Dot Product");
        System.out.println("\t--bdp\tBoolean Dot Product");
    }
}
