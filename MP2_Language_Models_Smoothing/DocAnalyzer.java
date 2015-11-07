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
 * @author Chengjun Yuan @UVa
 */
public class DocAnalyzer{
	
	//a list of stopwords
	int loadControl=0;
	int reviews_size=0;
	String reviewsFile="reviewsFile.csv";
	PrintWriter reviewsWriter;
	List<List<String>> reviewsList;
	
	Tokenizer tokenizer; // global tokenizer
	SnowballStemmer stemmer; // global stemmer
	List<String> unigramList;
	List<int[]> bigramList;
	HashMap<String,Integer> unigramIndex;
	HashMap<String,Integer> unigramCount;
	int numUnigram;
	int numBigram;
	double[] unigramLM;
	HashMap<String,Integer> bigramCount;  // c(w_i-1,w_i)
	HashMap<Integer,Integer> bigramUniqueCount; // <w_i-1, |d|_u>
	double lamda;	// for Linear Interpolation Smoothing
	double cigma;	// for Absolute Discount Smoothing
	double[] reviewsPerplexityDS;
	double DS_mu;
	
	//you might need something like this to store the counting statistics for validating Zipf's and computing IDF
	//HashMap<String, Token> m_stats;	
	
	//we have also provided sample implementation of language model in src.structures.LanguageModel
	
	public DocAnalyzer(){
		stemmer = new englishStemmer();
		try{
			tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream("./data/Model/en-token.bin")));
		}catch(IOException e){
			e.printStackTrace();
		}
		reviewsList=new ArrayList<List<String>>();
		unigramList=new ArrayList<String>();
		unigramIndex=new HashMap<String,Integer>();
		unigramCount=new HashMap<String,Integer>();
		bigramList=new ArrayList<int[]>();
		numUnigram=0;
		numBigram=0;
		bigramCount=new HashMap<String,Integer>(); 
		bigramUniqueCount=new HashMap<Integer,Integer>();
		lamda=0.9; cigma=0.1;
		
		try{
			reviewsWriter = new PrintWriter(new BufferedWriter(new FileWriter(reviewsFile, true)));
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//sample code for loading a list of stopwords from file
	//you can manually modify the stopword file to include your newly selected words
	
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
	
	public void saveObjectToFile(String fileName,Object obj){
		try{
			System.out.println("Save Object "+fileName);
			FileOutputStream fos =new FileOutputStream(fileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(obj);
            oos.close(); fos.close();
		}catch(IOException e){
            System.err.format("[Error]Failed to open or create file ");
            e.printStackTrace();
        }
	}
	
	// sample code for demonstrating how to recursively load files in a directory 
	public void LoadDirectory(String folder, String suffix) {
		File dir = new File(folder); int i=1;
		for(File f:dir.listFiles()){
			if (f.isFile() && f.getName().endsWith(suffix)){
				System.out.println("load "+i+" : "+f.getName());
				analyzeDocumentDemo(LoadJson(f.getAbsolutePath()));
				//System.out.println(token_TTF.size()+" "+token_DF.size());
				i++;
				//if(i==10)break;
			}
			else if (f.isDirectory())
				LoadDirectory(f.getAbsolutePath(), suffix);
		}
		reviewsWriter.close();
		System.out.println("Loading " + reviews_size + " review documents from " + folder);
		
		saveObjectToFile("unigramList",unigramList);
		saveObjectToFile("unigramIndex",unigramIndex);
		saveObjectToFile("unigramCount",unigramCount);
		saveObjectToFile("bigramCount",bigramCount);
		saveObjectToFile("bigramUniqueCount",bigramUniqueCount);
		saveObjectToFile("reviewsList",reviewsList);
	}
	
	public int TokenizerNormalizationStemming(String text){
		if(text==null||text.length()==0)return 0; int num=0;
		List<String> reviewWords=new ArrayList<String>();
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
				reviewWords.add(token);
				if(unigramCount.containsKey(token))unigramCount.put(token,unigramCount.get(token)+1);
				else{
					unigramCount.put(token,1);
					unigramList.add(token);
					unigramIndex.put(token,unigramList.size()-1);
					bigramUniqueCount.put(unigramList.size()-1, 0);
				}
				numUnigram++;
				num++;
				// bigram
				if(preToken!=null){
					int[] newBigram=new int[2]; 
					newBigram[0]=unigramIndex.get(preToken);
					newBigram[1]=unigramIndex.get(token);
					bigramList.add(newBigram);
					
					String newBigramStr=String.valueOf(newBigram[0])+"-"+String.valueOf(newBigram[1]);
					if(bigramCount.containsKey(newBigramStr)){
						bigramCount.put(newBigramStr,bigramCount.get(newBigramStr)+1);
					}
					else{
						bigramCount.put(newBigramStr,1);
						bigramUniqueCount.put(newBigram[0],bigramUniqueCount.get(newBigram[0])+1);
					}
					numBigram++;
				}
				preToken=token;
			}
		}
		if(reviewWords.size()>0)reviewsList.add(reviewWords);
		if(num>0)return 1;
		else return 0;
	}
	
	public void obtainReviewsPerplexity(){
		int numReviews=reviewsList.size(); List<String> review; double sqrtNum;
		double unigramPer,bigramPerLIS,bigramPerADS; int preWordIndex,wordIndex; 
		double[][] reviewsPerplexity=new double[3][numReviews]; // 0 unigram, 1 bigram_LIS, 2 bigram_ADS
		for(int i=0;i<numReviews;i++){
			review=reviewsList.get(i);
			unigramPer=0.0; bigramPerLIS=0.0; bigramPerADS=0.0;
			preWordIndex=-1;
			for(String word:review){
				wordIndex=unigramIndex.get(word);
				unigramPer=unigramPer+Math.log10(unigramLM[wordIndex]);
				if(preWordIndex>=0){
					bigramPerLIS=bigramPerLIS+Math.log10(bigramLM_LIS(preWordIndex,wordIndex));
					bigramPerADS=bigramPerADS+Math.log10(bigramLM_ADS(preWordIndex,wordIndex));
				}else{
					bigramPerLIS=bigramPerLIS+Math.log10(unigramLM[wordIndex]);
					bigramPerADS=bigramPerADS+Math.log10(unigramLM[wordIndex]);
				}
				preWordIndex=wordIndex;
			}
			sqrtNum=-1.0/review.size();
			reviewsPerplexity[0][i]=Math.pow(10.0,sqrtNum*unigramPer);
			reviewsPerplexity[1][i]=Math.pow(10.0,sqrtNum*bigramPerLIS);
			reviewsPerplexity[2][i]=Math.pow(10.0,sqrtNum*bigramPerADS);
		}
		try{
			PrintWriter writer=new PrintWriter("reviewsPerplexity.csv","UTF-8");
			for(int i=0;i<numReviews;i++)writer.println(reviewsPerplexity[0][i]+","+reviewsPerplexity[1][i]+","+reviewsPerplexity[2][i]);
            writer.close(); System.out.println("output reviewsPerplexity");
        }catch(IOException e){
            System.err.format("[Error]Failed to open or create file");
            e.printStackTrace();
        }
	}
	
	//bigram language model with Dirishlet Smoothing
	public double bigramLM_DS(int w1,int w2){
		String bigramIndex=String.valueOf(w1)+"-"+String.valueOf(w2);
		if(bigramCount.containsKey(bigramIndex)){
			return (bigramCount.get(bigramIndex)+DS_mu*unigramLM[w2])/(unigramCount.get(unigramList.get(w1))+DS_mu);
		}else{
			return DS_mu*unigramLM[w2]/(unigramCount.get(unigramList.get(w1))+DS_mu);
		}
	}
		
	public void obtainReviewsPerplexity_DirichletSmoothing(){
		int numReviews=reviewsList.size(); List<String> review; 
		double bigramPerDS; int preWordIndex,wordIndex; 
		reviewsPerplexityDS=new double[numReviews];
		for(int i=0;i<numReviews;i++){
			review=reviewsList.get(i); bigramPerDS=0.0; preWordIndex=-1;
			for(String word:review){
				wordIndex=unigramIndex.get(word);
				if(preWordIndex>=0){
					bigramPerDS=bigramPerDS+Math.log10(bigramLM_DS(preWordIndex,wordIndex));
				}else{
					bigramPerDS=bigramPerDS+Math.log10(unigramLM[wordIndex]);
				}
				preWordIndex=wordIndex;
			}
			reviewsPerplexityDS[i]=Math.pow(10.0,-1.0*bigramPerDS/review.size());
		}
	}
	
	public void unigramLanguageModel(){
		unigramLM=new double[unigramList.size()];
		for(int i=0;i<unigramList.size();i++){
			String token=unigramList.get(i);
			unigramLM[i]=(double)unigramCount.get(token)/numUnigram;
		}
	}

	public double bigramLM_LIS(int w1,int w2){
		String bigramIndex=String.valueOf(w1)+"-"+String.valueOf(w2);
		if(bigramCount.containsKey(bigramIndex)){
			return (1.0-lamda)*bigramCount.get(bigramIndex)/unigramCount.get(unigramList.get(w1))+lamda*unigramLM[w2];
		}else return lamda*unigramLM[w2];
		//System.out.println(bigramCount.get(bigramIndex)+"--"+unigramCount.get(unigramList.get(w1)));
	}
	
	public double bigramLM_ADS(int w1,int w2){
		String bigramIndex=String.valueOf(w1)+"-"+String.valueOf(w2);
		if(bigramCount.containsKey(bigramIndex)){
			return (Math.max(bigramCount.get(bigramIndex)-cigma,0.0)+cigma*bigramUniqueCount.get(w1)*unigramLM[w2])/unigramCount.get(unigramList.get(w1));
		}else return cigma*bigramUniqueCount.get(w1)*unigramLM[w2]/unigramCount.get(unigramList.get(w1));
	}
	
	public String generateDocument(int len,int ML_typle){ // ML_typle=0: Unigram, =1: Bigram_LIS, =2: Bigram_ADS
		if(len<1)return "";
		StringBuilder sb=new StringBuilder();
		double randomNum=Math.random();
		double sum=0.0,temp=0.0; int i=0; int j=1; int pre=0;
		for(i=0;i<unigramLM.length;i++){
			sum=sum+unigramLM[i];
			if(sum>=randomNum)break;
		}
		sb.append(unigramList.get(i)); sb.append(" "); pre=i;
		
		for(j=1;j<len;j++){
			randomNum=Math.random(); sum=0.0;
			for(i=0;i<unigramLM.length;i++){
				if(ML_typle==0)temp=unigramLM[i];
				else if(ML_typle==1)temp=bigramLM_LIS(pre,i);
				else temp=bigramLM_ADS(pre,i);
				sum=sum+temp;
				if(sum>=randomNum)break;
			}
			if(i==unigramLM.length)i=unigramLM.length-1;
			sb.append(unigramList.get(i)); sb.append(" "); pre=i;
		}
		return sb.toString();
	}
	
	public void top10WordsFollow(String word){
		try{
			int wordIndex=unigramIndex.get(word); int followWordIndex=0; double sum=0.0,temp=0.0;
			HashMap<String,Double> map=new HashMap<String,Double>();
			for(String followWord:unigramList){
				followWordIndex=unigramIndex.get(followWord);
				temp=bigramLM_LIS(wordIndex,followWordIndex);
				sum=sum+temp;
				map.put(followWord,temp);
			}
			System.out.println("sum of LIS "+sum);
			Map<String,Double> sortedMap=sortByComparatorDouble(map);
			PrintWriter writer=new PrintWriter("top10WordsFollow_"+word+"_LIS.txt", "UTF-8"); int i=0; 
			for(String followWord:sortedMap.keySet()){
				writer.println(followWord+" "+sortedMap.get(followWord)); i++;
				if(i>20)break;
			}
			writer.close();
			sum=0.0;
			for(String followWord:unigramList){
				followWordIndex=unigramIndex.get(followWord);
				temp=bigramLM_ADS(wordIndex,followWordIndex);
				sum=sum+temp;
				map.put(followWord,temp);
			}
			System.out.println("sum of ADS "+sum);
			sortedMap=sortByComparatorDouble(map);
			writer=new PrintWriter("top10WordsFollow_"+word+"_ADS.txt", "UTF-8"); i=0; 
			for(String followWord:sortedMap.keySet()){
				writer.println(followWord+" "+sortedMap.get(followWord)); i++;
				if(i>20)break;
			}
			writer.close();
		}catch(FileNotFoundException ex){
            ex.printStackTrace();
        }catch(IOException e){
        	e.printStackTrace();
        }
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
	
	public static void main(String[] args)throws IOException{		
		
		DocAnalyzer analyzer = new DocAnalyzer();
		//analyzer.LoadDirectory("C:/Dropbox/CS/Courses/InformationRetrieval/MP1/yelp", "json");
		analyzer.LoadDirectory("C:/Users/msesmart/workspace/MP1/MP2_test", "json");
		//analyzer.unigramLanguageModel();
		//analyzer.top10WordsFollow("good");
		System.out.println("Done ");
	}
}

