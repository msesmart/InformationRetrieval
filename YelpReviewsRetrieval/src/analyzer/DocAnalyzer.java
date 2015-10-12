/**
 * 
 */
package analyzer;
import java.io.*;
import java.util.*;
import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
//import org.tartarus.snowball.ext.porterStemmer;

import structures.Post;

/**
 * @author hongning
 * Sample codes for demonstrating OpenNLP package usage 
 * NOTE: the code here is only for demonstration purpose, 
 * please revise it accordingly to maximize your implementation's efficiency!
 */
public class DocAnalyzer {
	
	//a list of stopwords
	int loadControl=0;
	int reviews_size=0;
	String reviewsFile="reviewsFile.csv";
	PrintWriter reviewsWriter;
	static HashSet<String> all_stopwords;
	static HashSet<String> original_stopwords;
	static HashSet<String> new_stopwords;
	//you can store the loaded reviews in this arraylist for further processing
	List<Post> m_reviews;
	List<HashMap<String,Integer>> reviewsTokenTF;
	List<double[]> reviewsVector;
	List<Map<String,Double>> reviewsSimilarities;
	
	Tokenizer tokenizer; // global tokenizer
	SnowballStemmer stemmer; // global stemmer
	HashMap<String,Integer> token_TTF;
	HashMap<String,Integer> token_DF;
	HashMap<String,Integer> controlVocabulary;
	HashMap<String,Double> controlVocabulary_IDF;
	
	//you might need something like this to store the counting statistics for validating Zipf's and computing IDF
	//HashMap<String, Token> m_stats;	
	
	//we have also provided sample implementation of language model in src.structures.LanguageModel
	
	public DocAnalyzer(){
		all_stopwords=new HashSet<String>();
		original_stopwords=new HashSet<String>();
		new_stopwords=new HashSet<String>();
		m_reviews = new ArrayList<Post>();
		reviewsTokenTF=new ArrayList<HashMap<String,Integer>>();
		
		reviewsVector=new ArrayList<double[]>();
		reviewsSimilarities=new ArrayList<Map<String,Double>>();
		stemmer = new englishStemmer();
		try{
			tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream("./data/Model/en-token.bin")));
		}catch(IOException e){
			e.printStackTrace();
		}
		token_TTF=new HashMap<String,Integer>();
		token_DF=new HashMap<String,Integer>();
		controlVocabulary=new HashMap<String,Integer>();
		controlVocabulary_IDF=new HashMap<String,Double>();
		
		try{
			reviewsWriter = new PrintWriter(new BufferedWriter(new FileWriter(reviewsFile, true)));
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//sample code for loading a list of stopwords from file
	//you can manually modify the stopword file to include your newly selected words
	
	
	public void loadControl(String fileName){
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
			String line;
			while ((line = reader.readLine())!= null) {
				if(!line.isEmpty()){
					controlVocabulary.put(new String(line),0);
				}
			}
			reader.close();
			System.out.format("Loading control from %s\n", fileName);
		} catch(IOException e){
			System.err.format("[Error]Failed to open file %s!!", fileName);
		}
	}
	
	public void LoadStopwords(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			String line;
			while ((line = reader.readLine())!= null) {
				line=line.replaceAll("\\p{Punct}+","");
				if(!line.isEmpty()){
					line=line.toLowerCase();
					if(line.matches(".*\\d+.*"))line="NUM";
					else{
						stemmer.setCurrent(line);
						if (stemmer.stem())
							line=stemmer.getCurrent();
					}
					all_stopwords.add(line);
					original_stopwords.add(line);
				}
			}
			reader.close();
			System.out.format("Loading %d stopwords from %s\n", original_stopwords.size(), filename);
		} catch(IOException e){
			System.err.format("[Error]Failed to open file %s!!", filename);
		}
	}
	
	public void analyzeDocumentDemo(JSONObject json){
		try {
			PrintWriter writer = new PrintWriter("temp.txt", "UTF-8");
			JSONArray jarray = json.getJSONArray("Reviews"); int j=0;
			for(int i=0; i<jarray.length(); i++) {
				Post review = new Post(jarray.getJSONObject(i));
				//System.out.println(review.getID()+"  "+review.getAuthor());
				//List<String> tokens=TokenizerNormalizationStemming(review.getContent()); 
				j=TokenizerNormalizationStemming(review.getContent()); 
				// HINT: perform necessary text processing here, e.g., tokenization, stemming and normalization
				if(j>0){
					//m_reviews.add(review);
					reviews_size++;
					reviewsWriter.println(review.getAuthor()+","+review.getDate()+","+review.getContent());
				}
			}
			writer.close();
        }catch(FileNotFoundException ex){
            ex.printStackTrace();
        }catch(IOException e){
        	e.printStackTrace();
        }catch(JSONException e){
			e.printStackTrace();
		}
	}
	
	//sample code for loading a json file
	public JSONObject LoadJson(String filename){
		try{
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			StringBuffer buffer = new StringBuffer(1024);
			String line;
			
			while((line=reader.readLine())!=null){
				buffer.append(line);
			}
			reader.close();
			
			return new JSONObject(buffer.toString());
		}catch(IOException e){
			System.err.format("[Error]Failed to open file %s!", filename);
			e.printStackTrace();
			return null;
		}catch(JSONException e){
			System.err.format("[Error]Failed to parse json file %s!", filename);
			e.printStackTrace();
			return null;
		}
	}
	
	// sample code for demonstrating how to recursively load files in a directory 
	public void LoadDirectory(String folder, String suffix) {
		File dir = new File(folder); int i=1;
		int size = m_reviews.size();
		for(File f:dir.listFiles()){
			if (f.isFile() && f.getName().endsWith(suffix)){
				System.out.println("load "+i+" : "+f.getName());
				analyzeDocumentDemo(LoadJson(f.getAbsolutePath()));
				//System.out.println(token_TTF.size()+" "+token_DF.size());
				i++;
				if(i==40)break;
			}
			else if (f.isDirectory())
				LoadDirectory(f.getAbsolutePath(), suffix);
		}
		reviewsWriter.close();
		size = m_reviews.size() - size;
		System.out.println("Loading " + size + " review documents from " + folder);
		System.out.println("Loading " + reviews_size + " review documents from " + folder);
	}
	
	public int TokenizerNormalizationStemming(String text){
		if(text==null||text.length()==0)return 0; int num=0;
		//List<String> tokens=new ArrayList<String>();
		HashMap<String,Integer> tokenInReview=new HashMap<String,Integer>();
		String preToken=null;
		for(String token:tokenizer.tokenize(text)){
			//token=token.replaceAll("\\W+", "");
			token=token.replaceAll("\\p{Punct}+","");
			if(!token.isEmpty()){
				token=token.toLowerCase();
				if(token.matches(".*\\d+.*"))token="NUM";
				else{
					stemmer.setCurrent(token);
					if(stemmer.stem())
						token=stemmer.getCurrent();
				}
				if(token_TTF.containsKey(token))token_TTF.put(token,token_TTF.get(token)+1);
				else token_TTF.put(token,1);
				if(!tokenInReview.containsKey(token)){
					tokenInReview.put(token,1);
					if(token_DF.containsKey(token))token_DF.put(token, token_DF.get(token)+1);
					else token_DF.put(token, 1);
				}else{
					tokenInReview.put(token,tokenInReview.get(token)+1);
				}
				//tokens.add(token);
				num++;
				
				// bigram
				if(preToken!=null){
					String newBigram=new String(preToken+"-"+token);
					if(token_TTF.containsKey(newBigram))token_TTF.put(newBigram,token_TTF.get(newBigram)+1);
					else token_TTF.put(newBigram,1);
					if(!tokenInReview.containsKey(newBigram)){
						tokenInReview.put(newBigram,1);
						if(token_DF.containsKey(newBigram))token_DF.put(newBigram, token_DF.get(newBigram)+1);
						else token_DF.put(newBigram, 1);
					}else{
						tokenInReview.put(newBigram,tokenInReview.get(newBigram)+1);
					}
					//tokens.add(newBigram);
					num++;
				}
				preToken=token;
			}
		}
		reviewsTokenTF.add(tokenInReview);
		//System.out.println(token_TTF.size()+" "+token_DF.size());
		if(num>0)return 1;
		else return 0;
		//return tokens;
	}
	
	public void cleanReviewsTokenTF(){
		int len=reviewsTokenTF.size();
		HashMap<String,Integer> reviewTokenTF;
		for(int i=0;i<len;i++){
			reviewTokenTF=reviewsTokenTF.get(i);
			HashMap<String,Integer> cleanedTF=new HashMap<String,Integer>();
			for(String token:reviewTokenTF.keySet()){
				if(controlVocabulary.containsKey(token)){
					cleanedTF.put(token,reviewTokenTF.get(token));
				}
			}
			reviewsTokenTF.remove(i);
			reviewsTokenTF.add(i, cleanedTF);
		}
		System.out.println("cleanReviewsTokenTF finished ");
	}
	
	// sort HashMap by value
	private static Map<String, Integer> sortByComparator(Map<String, Integer> unsortMap) {
		// Convert Map to List
		List<Map.Entry<String, Integer>> list =new LinkedList<Map.Entry<String, Integer>>(unsortMap.entrySet());
		// Sort list with comparator, to compare the Map values
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1,
                                           Map.Entry<String, Integer> o2) {
				return -1*(o1.getValue()).compareTo(o2.getValue());
			}
		});
		// Convert sorted map back to a Map
		Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
		for (Iterator<Map.Entry<String, Integer>> it = list.iterator(); it.hasNext();) {
			Map.Entry<String, Integer> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
	
	private static Map<String, Double> sortByComparatorDouble(Map<String, Double> unsortMap) {
		// Convert Map to List
		List<Map.Entry<String, Double>> list =new LinkedList<Map.Entry<String, Double>>(unsortMap.entrySet());
		// Sort list with comparator, to compare the Map values
		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
			public int compare(Map.Entry<String, Double> o1,
                                           Map.Entry<String, Double> o2) {
				return -1*(o1.getValue()).compareTo(o2.getValue());
			}
		});
		// Convert sorted map back to a Map
		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		for (Iterator<Map.Entry<String, Double>> it = list.iterator(); it.hasNext();) {
			Map.Entry<String, Double> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
	
	public void renewStopwords(){
		Map<String,Integer> sortedToken_DF=sortByComparator(token_DF);
		int i=1; int k=0;
		for(String token:sortedToken_DF.keySet()){
			if(i<=100){
				if(!original_stopwords.contains(token)){
					all_stopwords.add(token);
					new_stopwords.add(token);
				}
			}else{
				int j=sortedToken_DF.get(token);
				if(j>=50&&(!all_stopwords.contains(token))){
					controlVocabulary.put(token,k); k++;
					//double idf=Math.log10((double)m_reviews.size()/j);
					double idf=Math.log10((double)reviews_size/j);
					controlVocabulary_IDF.put(token,new Double(idf));
				}
			}
			i++;
		}
	}
	
	public static void saveMapToFile(String fileName,Map<Object,Object> map) {
		try{
			PrintWriter writer=new PrintWriter(fileName, "UTF-8");
			for(Object key:map.keySet()){
				writer.println(key+" "+map.get(key));
			}
			writer.close();
		}catch(FileNotFoundException ex){
            ex.printStackTrace();
        }catch(IOException e){
        	e.printStackTrace();
        }
	}
	public void getTF_IDF_vector(HashMap<String,Integer> reviewTF,double[] reviewVector){
		
		int i=0;
		for(String token:reviewTF.keySet()){
			i=controlVocabulary.get(token);
			double TF=(double)reviewTF.get(token);
			if(TF>0)TF=1+Math.log10(TF);
			TF=TF*controlVocabulary_IDF.get(token).doubleValue();
			reviewVector[i]=TF;
		}
//		for(String token:reviewTF.keySet()){
//			if(controlVocabulary.containsKey(token)){
//				double TF=(double)reviewTF.get(token);
//				if(TF>0)TF=1+Math.log10(TF);
//				TF=TF*controlVocabulary_IDF.get(token).doubleValue();
//				i=controlVocabulary.get(token);
//				reviewVector[i]=TF;
//			}
//		}
	}
	
	public void getReviewsVector(String fileName)throws IOException{
		int i=1;
		PrintWriter writer=new PrintWriter(fileName, "UTF-8");
		
		for(HashMap<String,Integer> reviewTokenTF:reviewsTokenTF){
			double[] reviewVector=new double[controlVocabulary_IDF.size()];
			//HashMap<String,Double> reviewVector=new HashMap<String,Double>(emptyReviewVector);
			getTF_IDF_vector(reviewTokenTF,reviewVector);
			writer.println(Arrays.toString(reviewVector));
			reviewsVector.add(reviewVector);
			if(i%200==0) System.out.print(i+" "); if(i%4000==0) System.out.println(" "); i++; 
		}
		writer.close();
		System.out.println("reviewsVector finished ");
	}
	
	public double cosineSimilarity(double[] reviewVector1,double[] reviewVector2){
		double magtitude1=0.0; double magtitude2=0.0; double product=0.0;
		int len=reviewVector1.length;
		for(int i=0;i<len;i++){
			magtitude1+=reviewVector1[i]*reviewVector1[i];
			magtitude2+=reviewVector2[i]*reviewVector2[i];
			product+=reviewVector1[i]*reviewVector2[i];
		}
		if(magtitude1==0.0||magtitude2==0.0)return 0.0;
		else return product/(Math.sqrt(magtitude1)*Math.sqrt(magtitude2));
	}
	
	public void getSimilarities(List<double[]> comparedReviewsVector)throws IOException{
		double[] reviewVector=new double[controlVocabulary_IDF.size()]; int k=0; int j=0;
		PrintWriter writer=new PrintWriter("Top3_similarities.txt", "UTF-8");
		HashMap<String,Integer> reviewTokenTF;
		for(double[] comparedReviewVector:comparedReviewsVector){
			HashMap<String,Double> similarities=new HashMap<String,Double>();
			int len=reviewsTokenTF.size();
			for(int i=0;i<len;i++){
				reviewTokenTF=reviewsTokenTF.get(i);
				getTF_IDF_vector(reviewTokenTF,reviewVector);
				double similar=cosineSimilarity(comparedReviewVector,reviewVector);
				similarities.put(String.valueOf(i+1),new Double(similar));
				if(i%1000==0) System.out.print(i+" "); if(i%20000==0) System.out.println(" ");
			}
//			for(HashMap<String,Integer> reviewTokenTF:reviewsTokenTF){
//				//HashMap<String,Double> reviewVector=new HashMap<String,Double>(emptyReviewVector);
//				getTF_IDF_vector(reviewTokenTF,reviewVector);
//				double similar=cosineSimilarity(comparedReviewVector,reviewVector);
//				similarities.put(String.valueOf(j),new Double(similar));
//				if(j%1000==0) System.out.print(j+" "); if(j%20000==0) System.out.println(" "); j++; 
//			}
			System.out.println(" ");
			Map<String,Double> sortedSimilarities=sortByComparatorDouble(similarities);
			
			k=0;
			for(String index:sortedSimilarities.keySet()){
//				Post review=m_reviews.get(Integer.valueOf(index)-1);
				writer.println("#"+k+" Index: "+index+" similarities: "+sortedSimilarities.get(index));
				System.out.println("#"+k+" Index: "+index+" similarities: "+sortedSimilarities.get(index));
//				writer.println("   Author: "+review.getAuthor());
//				writer.println("   Content: "+review.getContent());
//				writer.println("   Date: "+review.getDate());
				if(k>=10)break; k++;
			}
			//reviewsSimilarities.add(sortedSimilarities);
			System.out.println("compared vector "+j); j++;
		}
		writer.close();
	}
	
	public static void main(String[] args)throws IOException{		
		
		DocAnalyzer analyzer = new DocAnalyzer();
		if(analyzer.loadControl==1)analyzer.loadControl("./data/Model/control.txt");
		analyzer.LoadDirectory("C:/Dropbox/CS/Courses/InformationRetrieval/MP1/yelp", "json");
		analyzer.LoadStopwords("./data/Model/english.stop");
		analyzer.renewStopwords();
		System.out.println("analyzer controlVocabulary size : "+analyzer.controlVocabulary.size());
		analyzer.cleanReviewsTokenTF();
		//analyzer.getReviewsVector("analyzer.dat");
		
		DocAnalyzer query=new DocAnalyzer();
		query.LoadDirectory("C:/Dropbox/CS/Courses/InformationRetrieval/MP1/MP1/data/samples", "json");
		query.controlVocabulary=analyzer.controlVocabulary;
		query.controlVocabulary_IDF=analyzer.controlVocabulary_IDF;
		query.cleanReviewsTokenTF();
		query.getReviewsVector("query.dat");
		
		analyzer.getSimilarities(query.reviewsVector);
		System.out.println("Done ");
	}
}
