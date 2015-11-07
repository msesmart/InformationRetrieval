import java.io.*;
import java.util.*;

/**
 * @author Chengjun Yuan @UVa
 * 
 */
public class DocAnalyzer_MP2_2{
	List<String> unigramList;
	HashMap<String,Integer> unigramIndex;
	HashMap<String,Integer> unigramCount;
	int numUnigram;
	int numBigram;
	double[] unigramLM;
	HashMap<String,Integer> bigramCount;  // c(w_i-1,w_i)
	HashMap<Integer,Integer> bigramUniqueCount; // <w_i-1, |d|_u>
	double lamda;	// for Linear Interpolation Smoothing
	double cigma;	// for Absolute Discount Smoothing
	double DS_mu;   // for Dirichlet Smoothing.
	double likelihood;
	List<List<String>> reviewsList;
	double[][] reviewsPerplexity;
	double[] reviewsPerplexityDS;
	
	public DocAnalyzer_MP2_2(){
		unigramList=new ArrayList<String>();
		unigramIndex=new HashMap<String,Integer>();
		unigramCount=new HashMap<String,Integer>();
		numUnigram=0;
		numBigram=0;
		bigramCount=new HashMap<String,Integer>(); 
		bigramUniqueCount=new HashMap<Integer,Integer>();
		lamda=0.9; cigma=0.1; likelihood=1.0;
		reviewsList=new ArrayList<List<String>>();
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
	
	public void loadObjectDataFromFile(String dir){
		try{
			System.out.println("load unigramList");
			FileInputStream fis=new FileInputStream(dir+"/unigramList"); 
			ObjectInputStream ois=new ObjectInputStream(fis);
			unigramList=(ArrayList)ois.readObject(); ois.close(); fis.close();
			System.out.println("load unigramIndex");
			fis=new FileInputStream(dir+"/unigramIndex"); ois=new ObjectInputStream(fis);
			unigramIndex=(HashMap)ois.readObject(); ois.close(); fis.close();
			System.out.println("load unigramCount");
			fis=new FileInputStream(dir+"/unigramCount"); ois=new ObjectInputStream(fis);
			unigramCount=(HashMap)ois.readObject(); ois.close(); fis.close();
			System.out.println("load bigramCount");
			fis=new FileInputStream(dir+"/bigramCount"); ois=new ObjectInputStream(fis);
			bigramCount=(HashMap)ois.readObject(); ois.close(); fis.close();
			System.out.println("load bigramUniqueCount");
			fis=new FileInputStream(dir+"/bigramUniqueCount"); ois=new ObjectInputStream(fis);
			bigramUniqueCount=(HashMap)ois.readObject(); ois.close(); fis.close();
			
			for(int temp:unigramCount.values()){
				numUnigram=numUnigram+temp;
			}
			System.out.println("numUnigram "+numUnigram);
		}catch(IOException ioe){
			ioe.printStackTrace(); return;
		}catch(ClassNotFoundException c){
			System.out.println("Class not found"); c.printStackTrace(); return;
		}
	}
	
	public void loadUnigramListFromFile(String dir){
		try{
			System.out.println("load unigramList");
			FileInputStream fis=new FileInputStream(dir+"/unigramList"); 
			ObjectInputStream ois=new ObjectInputStream(fis);
			unigramList=(ArrayList)ois.readObject(); ois.close(); fis.close();
			System.out.println(unigramList.size());
		}catch(IOException ioe){
			ioe.printStackTrace(); return;
		}catch(ClassNotFoundException c){
			System.out.println("Class not found"); c.printStackTrace(); return;
		}
	}
	
	public void loadReviewsListFromFile(String dir){
		try{
			System.out.println("load ReviewsList");
			FileInputStream fis=new FileInputStream(dir+"/reviewsList"); 
			ObjectInputStream ois=new ObjectInputStream(fis);
			reviewsList=(ArrayList)ois.readObject(); ois.close(); fis.close();
			System.out.println(reviewsList.size());
		}catch(IOException ioe){
			ioe.printStackTrace(); return;
		}catch(ClassNotFoundException c){
			System.out.println("Class not found"); c.printStackTrace(); return;
		}
	}
	
	public void unigramLanguageModel(){
		unigramLM=new double[unigramList.size()]; double sum=0.0;
		for(int i=0;i<unigramList.size();i++){
			String token=unigramList.get(i);
			unigramLM[i]=(double)unigramCount.get(token)/numUnigram;
			sum=sum+unigramLM[i];
		}
		System.out.println("Unigram sum "+sum);
	}
	
	public void unigramLanguageModel_AS(){
		unigramLM=new double[unigramList.size()]; double sum=0.0;
		for(int i=0;i<unigramList.size();i++){
			String token=unigramList.get(i);
			unigramLM[i]=((double)unigramCount.get(token)+0.1)/(numUnigram+0.1*unigramList.size());
			sum=sum+unigramLM[i];
		}
		System.out.println("Additive Smoothing Unigram sum "+sum);
	}

	//bigram language model with Linear Interpolation Smoothing
	public double bigramLM_LIS(int w1,int w2){
		String bigramIndex=String.valueOf(w1)+"-"+String.valueOf(w2);
		if(bigramCount.containsKey(bigramIndex)){
			return (1.0-lamda)*bigramCount.get(bigramIndex)/unigramCount.get(unigramList.get(w1))+lamda*unigramLM[w2];
		}else return lamda*unigramLM[w2];
		//System.out.println(bigramCount.get(bigramIndex)+"--"+unigramCount.get(unigramList.get(w1)));
	}
	
	//bigram language model with Absolute Discounting Smoothing
	public double bigramLM_ADS(int w1,int w2){
		String bigramIndex=String.valueOf(w1)+"-"+String.valueOf(w2);
		if(bigramCount.containsKey(bigramIndex)){
			return (Math.max(bigramCount.get(bigramIndex)-cigma,0.0)+cigma*bigramUniqueCount.get(w1)*unigramLM[w2])/unigramCount.get(unigramList.get(w1));
		}else if(unigramCount.get(unigramList.get(w1))>0&&bigramUniqueCount.get(w1)>0){
			return cigma*bigramUniqueCount.get(w1)*unigramLM[w2]/unigramCount.get(unigramList.get(w1));
		}else{
			return unigramLM[w2];
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
	
	public String generateDocument(int len,int LM_type){ // LM_type=0: Unigram, =1: Bigram_LIS, =2: Bigram_ADS
		likelihood=1.0;
		if(len<1)return ""; 
		StringBuilder sb=new StringBuilder();
		double randomNum=Math.random();
		double sum=0.0,temp=0.0; int i=0; int j=1; int pre=0;
		for(i=0;i<unigramLM.length;i++){
			sum=sum+unigramLM[i];
			if(sum>=randomNum)break;
		}
		sb.append(unigramList.get(i)); sb.append(" "); likelihood=likelihood*unigramLM[i]; pre=i;
		
		for(j=1;j<len;j++){
			randomNum=Math.random(); sum=0.0;
			for(i=0;i<unigramLM.length;i++){
				if(LM_type==0)temp=unigramLM[i];
				else if(LM_type==1)temp=bigramLM_LIS(pre,i);
				else temp=bigramLM_ADS(pre,i);
				sum=sum+temp;
				if(sum>=randomNum)break;
			}
			likelihood=likelihood*temp;
			if(i==unigramLM.length)i=unigramLM.length-1;
			sb.append(unigramList.get(i)); sb.append(" "); pre=i;
		}
		return sb.toString();
	}
	
	public void generateTenDocuments(){
		try{
			PrintWriter writer=new PrintWriter("TenDocuments_unigram.txt","UTF-8");
			for(int i=0;i<10;i++)writer.println(generateDocument(20,0)+","+Math.log10(likelihood));
            writer.close(); System.out.println("output TenDocuments_unigram");
            writer=new PrintWriter("TenDocuments_Bigram_LIS.txt","UTF-8");
			for(int i=0;i<10;i++)writer.println(generateDocument(20,1)+","+Math.log10(likelihood));
            writer.close(); System.out.println("output TenDocuments_Bigram_LIS");
			writer=new PrintWriter("TenDocuments_Bigram_ADS.txt","UTF-8");
			for(int i=0;i<10;i++)writer.println(generateDocument(20,2)+","+Math.log10(likelihood));
            writer.close(); System.out.println("output TenDocuments_Bigram_ADS");
        }catch(IOException e){
            System.err.format("[Error]Failed to open or create file");
            e.printStackTrace();
        }
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
	
	public void obtainPerplexityMeanAndStandardDeviation_DirichletSmoothing(){
		double mean=0.0; double sd=0.0; int len=reviewsPerplexityDS.length;
		for(double per:reviewsPerplexityDS)mean=mean+per;
		mean=mean/len;
		for(double per:reviewsPerplexityDS)sd=sd+Math.pow(per-mean,2.0);
		sd=Math.sqrt(sd/len);
		System.out.println("DirichletSmoothing, mu "+DS_mu+" Mean "+mean+" StandardDeviation"+sd);
	}
	public void obtainReviewsPerplexity(){
		int numReviews=reviewsList.size(); List<String> review; double sqrtNum;
		double unigramPer,bigramPerLIS,bigramPerADS; int preWordIndex,wordIndex; 
		reviewsPerplexity=new double[3][numReviews]; // 0 unigram, 1 bigram_LIS, 2 bigram_ADS
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
	
	public void obtainPerplexityMeanAndStandardDeviation(){
		double[] mean=new double[3]; double[] sd=new double[3];
		int len=reviewsPerplexity[0].length;
		for(int j=0;j<len;j++){
			mean[0]=mean[0]+reviewsPerplexity[0][j];
			mean[1]=mean[1]+reviewsPerplexity[1][j];
			mean[2]=mean[2]+reviewsPerplexity[2][j];
		}
		mean[0]=mean[0]/len;
		mean[1]=mean[1]/len;
		mean[2]=mean[2]/len;
		for(int j=0;j<len;j++){
			sd[0]=sd[0]+Math.pow(reviewsPerplexity[0][j]-mean[0],2.0);
			sd[1]=sd[1]+Math.pow(reviewsPerplexity[1][j]-mean[1],2.0);
			sd[2]=sd[2]+Math.pow(reviewsPerplexity[2][j]-mean[2],2.0);
		}
		sd[0]=Math.sqrt(sd[0]/len);
		sd[1]=Math.sqrt(sd[1]/len);
		sd[2]=Math.sqrt(sd[2]/len);
		System.out.println("mean "+mean[0]+" "+mean[1]+" "+mean[2]);
		System.out.println("StandardDeviation "+sd[0]+" "+sd[1]+" "+sd[2]);
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
	
	public static void main(String[] args)throws IOException{		
		DocAnalyzer_MP2_2 analyzer = new DocAnalyzer_MP2_2();
		analyzer.loadObjectDataFromFile("trainObjects");
		analyzer.unigramLanguageModel();
		analyzer.top10WordsFollow("good");
		analyzer.generateTenDocuments();
		
		DocAnalyzer_MP2_2 analyzer_test = new DocAnalyzer_MP2_2();
		//analyzer_test.loadObjectDataFromFile("testObjects");
		analyzer_test.loadUnigramListFromFile("testObjects");
		analyzer.loadReviewsListFromFile("testObjects");
		for(String word:analyzer_test.unigramList){
			if(!analyzer.unigramCount.containsKey(word)){
				analyzer.unigramList.add(word);
				analyzer.unigramIndex.put(word,analyzer.unigramList.size()-1);
				analyzer.unigramCount.put(word,0);
			}
		}
		analyzer.unigramLanguageModel_AS();
		analyzer.obtainReviewsPerplexity();
		analyzer.obtainPerplexityMeanAndStandardDeviation();
		
		for(double i=1.0;i<200001;i=i+200.0){
			analyzer.DS_mu=i;
			analyzer.obtainReviewsPerplexity_DirichletSmoothing();
			analyzer.obtainPerplexityMeanAndStandardDeviation_DirichletSmoothing();
		}
		System.out.println("Done ");
	}
}
