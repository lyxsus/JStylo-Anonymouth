package edu.drexel.psal.anonymouth.projectDev;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.Scanner;

import edu.drexel.psal.anonymouth.gooie.ClusterViewer;
import edu.drexel.psal.anonymouth.gooie.EditorTabDriver;
import edu.drexel.psal.anonymouth.gooie.ThePresident;
import edu.drexel.psal.anonymouth.gooie.DocsTabDriver.ExtFilter;
import edu.drexel.psal.anonymouth.suggestors.HighlightMapMaker;
import edu.drexel.psal.anonymouth.utils.SentenceTools;
import edu.drexel.psal.jstylo.generics.*;
import edu.drexel.psal.jstylo.generics.Logger.LogOut;

import java.util.HashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import com.jgaap.generics.Document;


import weka.attributeSelection.InfoGainAttributeEval;
import weka.classifiers.Classifier;
import weka.core.Instances;


/**
 * Performs general calculations relating to analyzing the data 
 * @author Andrew W.E. McDonald
 *
 */
public class DataAnalyzer{
	
	
	private int numFeaturesToReturn;
	
	public static Attribute[] topAttributes;
	
	public static int lengthTopAttributes;
	
	private String[] importantAttribs;
	
	private ArrayList<String> allAttribs;
	
	private ArrayList<String> trainAttribs;
	
	private ArrayList<String> authorAttribs;
	
	private String[] strippedAttributeNames;
	
	private ProblemSet pSet;
	
	private String sessionAlias = ""; 
	
	private Double[][] trainingInstancesArray;	
	
	private Double[][] authorInstancesArray;
	
	private Double[][] toModifyInstancesArray;
	
	double[] authorAverages;
	
	double[] authorStdDevs;
	
	double[][] minsAndMaxes;
	
	private ArrayList<Cluster[]> allOrderedClusters;
	
	private ArrayList<String> featuresForClusterAnalyzer;
	
	public static int[] selectedTargets;
	
	private boolean mapMakerSentenceTargetSet = false;
	
	private boolean mapMakerCharTargetSet = false;
	
	private HashMap<String,Double> holderForLogger = new HashMap<String,Double>();
	
	
	
	/**
	 * constructor for DataAnalyzer
	 * @param numFeaturesToReturn integer number corresponding to the top 'numFeaturesToReturn' features
	 */
	public DataAnalyzer(ProblemSet pSet, String sessionAlias){
		Logger.logln("Created DataAnalyzer");
		this.sessionAlias = sessionAlias;
		this.pSet = pSet;
	}
	
	/**
	 * returns the array of top Attributes (features with the highest information gain)
	 * @return
	 */
	public Attribute[] getAttributes(){
		return topAttributes;
	}
	
	/**
	 * Sets the number of features to return suggestions for
	 * @param numFeaturesToReturn
	 */
	public void setNumFeaturesToReturn(int numFeaturesToReturn){
		Logger.logln("Numer of features to return: "+numFeaturesToReturn);
		this.numFeaturesToReturn = numFeaturesToReturn;
	}
	
	/**
	 * takes average of each feature across all instances specified by "instancesArray" 
	 */
	public void authorAverageFinder(){
		Logger.logln("called authorAverageFinder");
		int i;
		int j;
		int numInstances = topAttributes[0].getAuthorVals().length;
		double sum;
		double[] currentFeature;
		double average;
		for(i=0;i<lengthTopAttributes;i++){
			currentFeature = topAttributes[i].getAuthorVals();
			sum = 0;
			for(j=0;j<numInstances;j++){
				sum=sum+currentFeature[j];
			}
			average = (sum/numInstances);
			//System.out.println("user sample average for "+topAttributes[i].getFullName()+" is: "+average);
			topAttributes[i].setAuthorAvg(average);
		}
		Logger.logln("found author averages");
	}
	
	/**
	 * finds the minimum and maximum values of each feature across all instances given
	 */
	public void minsAndMaxes(){
		Logger.logln("called minsAndMaxes - finding min and max for each feature");
		int numFeatures = topAttributes[0].getTrainVals().length;
		int j=0;
		int i=0;
		double theMin;
		double theMax;
		double[] featureRay;
		for(i=1;i<lengthTopAttributes;i++){ // i runs through each instance (row)
			j=0;
			featureRay = topAttributes[i].getTrainVals();
			theMin=featureRay[j];
			theMax=featureRay[j];
			for(j=0;j<numFeatures;j++){ // j runs though each feature (column)
				if(theMin>featureRay[j])
					theMin=featureRay[j];
				if(theMax<featureRay[j])
					theMax=featureRay[j];
			}
			topAttributes[i].setTrainMin(theMin);
			topAttributes[i].setTrainMax(theMax);
			//System.out.println(topAttributes[i].getGenericName()+" MINIMUM VALUE: "+topAttributes[i].getTrainMin()+" MAX: "+topAttributes[i].getTrainMax());
		}
		// test:
		//for(i=0;i<numFeatures;i++){
		//	System.out.println("Min for feature "+i+" is: "+theMinsAndMaxes[i][0]+ " and the Max is: "+theMinsAndMaxes[i][1]);
		//}
		Logger.logln("finished finding mins and maxes");
	}
	
	/**
	 * Finds standard deviation of author's values
	 */
	public void authorStdDevFinder(){
		Logger.logln("called authorStdDevFinder");
		int i;
		int j;
		int numInstances = topAttributes[0].getAuthorVals().length; 
		double tempAvg;
		double stdDev;
		double sum;
		double [] tempFeatRay;
		for(i=0;i<lengthTopAttributes;i++){
			tempAvg = topAttributes[i].getAuthorAvg();
			tempFeatRay = topAttributes[i].getAuthorVals();
			sum = 0;
			for(j=0;j<numInstances;j++){
				sum=sum+((tempFeatRay[j]-tempAvg)*(tempFeatRay[j]-tempAvg));
			}
			stdDev = Math.sqrt((sum/numInstances));
			//System.out.println("user sample std. dev for "+topAttributes[i].getFullName()+" is: "+stdDev); 
			topAttributes[i].setAuthorStdDev(stdDev);
			topAttributes[i].setAuthorConfidence(1.96*stdDev);
		}
		Logger.logln("finished finding author standard deviations");
	}
	
	/**
	 * Finds the top 'numFeatures' after information gain has been calculated for them all
	 * @param theArffFile the instances
	 * @param numFeatures the number of features to return (starts from highest information gain)
	 * @return
	 * 	 Array of Attribute objects, one Attribute per feature
	 * @throws Exception
	 */
	public Attribute[] findMostInfluentialEvents(Instances theArffFile, int numFeatures) throws Exception{
		Logger.logln("called findMostInfluentialEvents... for "+numFeatures+" features");
		int i=0;
		int numAttributes= theArffFile.numAttributes();
		//if(numFeatures > numAttributes)
			//numFeatures = numAttributes;
		double[][] allInfoGain= new double[numAttributes][2];//
		NavigableMap<Double,String> topIdentifiers = new ConcurrentSkipListMap<Double,String>();
		InfoGainAttributeEval IGAE = new InfoGainAttributeEval();
		theArffFile.setClass(theArffFile.attribute("authorName"));
		//System.out.println(theArffFile.toString());
		IGAE.buildEvaluator(theArffFile);
		for(i=0;i<numAttributes;i++){
			//System.out.println(theArffFile.attribute(i));
			allInfoGain[i][0]=IGAE.evaluateAttribute(i);
			allInfoGain[i][1]=i;
		}
		
		//Sort array of info gains from greatest to least
		Arrays.sort(allInfoGain, new Comparator<double[]>(){
			@Override
			public int compare(final double[] first, final double[] second){
				//return ((Double)first[0]).compareTo(((Double)second[0]));
				return ((-1)*((Double)first[0]).compareTo(((Double)second[0]))); // multiplying by -1 will sort from greatest to least, which saves work.
			}
		});
		
		// print out results to check sorting and info gain extraction
		//System.out.println(Arrays.deepToString(allInfoGain));
		//System.out.println("Length of array: "+allInfoGain.length+" numFeatures: "+numFeatures);
		//System.out.println("toModifyInstncesArray == "+Arrays.deepToString(toModifyInstancesArray));
		
		
		//int j= allInfoGain.length-1; // need j to account for skipped iteration when "authorName" is found.
		
		//Construct array of Attributes, save full attribute name and info gain into 
		featuresForClusterAnalyzer = new ArrayList<String>(numFeatures);
		Attribute[] topAttribs = new Attribute[numFeatures];
		strippedAttributeNames = new String[numFeatures];
		int j = 0;
		String strippedAttrib;
		FeatureList genName;
		EditorTabDriver.attributesMappedByName = new HashMap<FeatureList,Integer>(numFeatures);
		
		for(i=0; i<numFeatures;i++){
			String attrib = (theArffFile.attribute((int) allInfoGain[i][1]).toString());
			//System.out.println("ATTRIBUTE: "+attrib);
			strippedAttrib = AttributeStripper.strip(attrib);	
			int toModifyIndex = allAttribs.indexOf(strippedAttrib);
			//System.out.println("allAttribs value of '"+attrib+"' is: "+allAttribs.get(i));
			if(attrib.contains("authorName")){
					numFeatures++;
					continue;
			}
			/*
			if (toModifyIndex != -1){
				if((toModifyInstancesArray[0][toModifyIndex] == 0.0)){//&& EditorTabDriver.userRequestedNoZeros == true){
					Logger.logln("CONTINUING attribute: "+attrib+" toModifyValue: "+toModifyInstancesArray[0][toModifyIndex]+" info gain: "+allInfoGain[j][0]);
					numFeatures++;
					continue;
				}
			}
			*/
			
			if(toModifyIndex == -1){
				//System.out.println("CONTINUING attribute: "+attrib+" does not appear in toModifyDocument.");
				numFeatures++;
				continue;
			}
					
			//System.out.println("ATTRIBUTE: "+attrib+" toModifyValue: "+toModifyInstancesArray[0][toModifyIndex]);
			//System.out.println("PASSED ATTRIBUTE: "+attrib);
			//String trimmedAttrib = attrib.substring(attrib.indexOf("'")+1,attrib.indexOf("{"));// saves ONLY the actual feature name
			//System.out.println("SHORTENED ATTRIBUTE: "+trimmedAttrib);
			String stringInBraces;
			boolean calcHist;
			try{
				//Pattern someString = Pattern.compile("\\{[A-Za-z0-9]+\\}"); // use this pattern, and if an exception is thrown, 
				//Matcher m = someString.matcher(attrib);
				//m.find();
				//stringInBraces = attrib.substring(m.start()+1,attrib.indexOf('}'));
				stringInBraces = attrib.substring(attrib.indexOf('{')+1,attrib.indexOf('}'));

			}
			catch(IllegalStateException e){ // if no match is found, set 'stringInBraces == to an empty string
				stringInBraces = "";
			}
			if(stringInBraces.equals("") || stringInBraces.equals("-")){
				stringInBraces = "";
				calcHist = false;
			}
			else
				calcHist = true;
			topAttribs[j] = new Attribute((int)allInfoGain[i][1],attrib,stringInBraces,calcHist);
			topAttribs[j].setInfoGain(allInfoGain[j][0]);
			topAttribs[j].setToModifyValue(toModifyInstancesArray[0][toModifyIndex]);
			genName = topAttribs[j].getGenericName();
			EditorTabDriver.attributesMappedByName.put(genName, j);
			featuresForClusterAnalyzer.add(topAttribs[j].getConcatGenNameAndStrInBraces());
			strippedAttributeNames[j] = strippedAttrib;	
			Logger.logln(topAttribs[j].getFullName()+" info gain for this feature is: "+topAttribs[j].getInfoGain()+", calcHist is: "+topAttribs[j].getCalcHist()+" string in the braces (if applicable): "+topAttribs[j].getStringInBraces()+" toModify value is: "+topAttribs[j].getToModifyValue()); 
			j++;
			
		}
		Logger.logln("found all top attributes");
		return topAttribs;	
	}
	
	
	/**
	 * computes the information gain from the top 'numFeaturesToReturn' number of features specified by the constructor
	 * @param presentSet - an unmodified Instances object to compute information gain from
	 * @return
	 *  Array of Attribute objects, one Attribute for each feature
	 * @throws Exception 
	 */
	public Attribute[] computeInfoGain(Instances presentSet) throws Exception{
		Logger.logln("called computerInfoGain");
		//TODO: compute info gain, and then 'try' to find the location of the train value and author value for the top features. 
		// if it doesn't exist, set as '0'. if the value for one of the top features is '0' in toModify, skip that feature, and get the next one.
		// 
		int i;
		int j;
		presentSet.setClass(presentSet.attribute("authorName"));
		Attribute[] topAttribs;
		topAttribs = findMostInfluentialEvents(presentSet,numFeaturesToReturn);
		int numAttribs = topAttribs.length;
		
			
		//for(i=0;i<numAttribs;i++){
		//	FeatureList topIds = (topAttribs[i].getGenericName());	
		//	System.out.println(topIds);
		//}
		
		int numTrainInstances = trainingInstancesArray.length;
		int numAuthInstances = authorInstancesArray.length;
		double[][]	relevantTrainFeats = new double[numAttribs][numTrainInstances];
		double [][] relevantAuthorFeats = new double[numAttribs][numAuthInstances];
		importantAttribs = new String[numAttribs];
		
		// Extract the top "n" important features and place them into an array in order of info gain (greatest -> least). This happens per instance to allow for std. dev. calculations.
		int identifyingIndex;
		int tempTrainIndex =0;
		int tempAuthorIndex =0;
		boolean trainOk = true;
		boolean authorOk = true;
		//System.out.println("numAttribs: "+numAttribs);
		//System.out.println("numTrainInstances: "+numTrainInstances);
		//System.out.println("strippedAttributeNames.length == "+strippedAttributeNames.length);
		for(i=0;i<numAttribs;i++){
			//System.out.println(" i == "+i);
			String tempAttrib = strippedAttributeNames[i];
			if(trainAttribs.contains(tempAttrib) == true){
				tempTrainIndex = trainAttribs.indexOf(tempAttrib);
				//System.out.println("TempTrainIndex: "+tempTrainIndex);
				trainOk = true;
			}
			else{
				trainOk = false;
				tempTrainIndex = 0; // basically re-setting... this wont be used in this case until the next iteration.
			}
			
			if(authorAttribs.contains(tempAttrib)){
				tempAuthorIndex = authorAttribs.indexOf(tempAttrib);
				//System.out.println("tempAuthorIndex : "+tempAuthorIndex);
				authorOk = true;
			}
			else{
				authorOk = false;
				tempAuthorIndex = 0;
			}
			for(j=0;j<numTrainInstances;j++){
					//System.out.print( "j == "+j+"; ");
					if(trainOk == true)
						relevantTrainFeats[i][j]=trainingInstancesArray[j][tempTrainIndex]; // transpose and filter the instancesArray
					else
						relevantTrainFeats[i][j]=0;
				if(j < numAuthInstances){
					if(authorOk == true)
						relevantAuthorFeats[i][j] = authorInstancesArray[j][tempAuthorIndex];
					else
						relevantAuthorFeats[i][j] = 0;
				}
			}
			//System.out.println("Iteration finished.");
		}
		String theSpacer = "";
		for(i=0;i<numAttribs;i++){
			if(topAttribs[i].getStringInBraces().equals(""))
				theSpacer = "";
			else
				theSpacer = " ";
			importantAttribs[i] = topAttribs[i].getGenericName().toString()+theSpacer+topAttribs[i].getStringInBraces();
			topAttribs[i].setTrainVals(relevantTrainFeats[i]);
			topAttribs[i].setAuthorVals(relevantAuthorFeats[i]);
			//System.out.print("toModify val for: "+topAttribs[i].getFullName()+" is: ");
			double thisVal = topAttribs[i].getToModifyValue();
			holderForLogger.put(topAttribs[i].getConcatGenNameAndStrInBraces(), thisVal);
			//System.out.print(thisVal+", ");
			//System.out.println();
		}
		
		
		Logger.logln("****** Current list of Present values for: "+ThePresident.sessionName+" process request number: "+DocumentMagician.numProcessRequests+" ******");
		Logger.logln(holderForLogger.entrySet().toString());
		return topAttribs;
		
		//List<Document> authorsDocs = jamBaselineSet.removeAuthor(authorOfDocToModify);
	}
		
	
	/**
	 * 
	 * @deprecated
	 * runSelectedFeature extracts the target for the selected feature if 'shouldExtract' is true, and simply returns the attribute if false.
	 * @param sel - the selected feature number
	 */
	public Attribute runSelectedFeature(int sel,boolean shouldExtract){
		//TODO: this should be passed an object!!! containing the relevant stuff (one feature, with the corresponding events/instances for all documents, and other calculated data)
		
		if(shouldExtract == true){
			int i;
			int j;
			int numAuthors = DocumentMagician.numSampleAuthors;
			double targetValue;
			double avgAbsDev;
			
			
			TargetExtractor extractor = new TargetExtractor(numAuthors, topAttributes[sel]);
			System.out.println("Clusters for "+importantAttribs[sel]+" :");
			extractor.aMeansCluster();
			targetValue= extractor.getTargetValue();
			topAttributes[sel].setTargetValue(targetValue);
				
			//}
			// print cluster info
			//int numFeaturesToHold = allAvgAbsDevs.length;
			//pac = new PostAnalysisContainer[numFeaturesToHold];
			//allRelevantFeatures = new FeatureList[numFeaturesToHold];
			
			//for(i=0;i<allAvgAbsDevs.length;i++){
			FeatureList needsSuggestion = topAttributes[sel].getGenericName();
			//System.out.println(needsSuggestion);
			//System.out.println("target value: "+targetValue);
			//System.out.println("author's values: "+topAttributes[sel].getAuthorAvg()+" with a standard deviation of: "+topAttributes[sel].getAuthorStdDev());
			//System.out.println("present value: "+topAttributes[sel].getToModifyValue());
			//pac = new PostAnalysisContainer(needsSuggestion,toModifyInstancesArray[0][sel],targetValue,authorAverages[sel],authorStdDevs[sel]);
			topAttributes[sel].setTargetValue(targetValue);
			//System.out.println("The value of this feature in 'FeatureList' is: "+needsSuggestion);
			//System.out.println();
			//}
			//Logger.logln(needsSuggestion+":present value:"+topAttributes[sel].getToModifyValue()+":target value:"+targetValue);
		}
			return topAttributes[sel];
		
	}
	
	
	/**
	 * runs clustering algorithm on all features, and saves information in each feature's 'Attribute' object.
	 * @return
	 * 	number of maximum clusters found, to be used with @ClusterAnalyzer
	 */
	public int runAllTopFeatures(){
		Logger.logln("called runAllTopFeatures");
		int sel = 0;
		int numAttribs = topAttributes.length;
		int numAuthors = DocumentMagician.numSampleAuthors;
		int maxClusters = 0;
		int tempMaxClusters;
		for(sel=0;sel<numAttribs;sel++){
				Cluster[] orderedClusters;
				TargetExtractor extractor = new TargetExtractor(numAuthors, topAttributes[sel]);
				//System.out.println("Clusters for "+importantAttribs[sel]+" :");
				extractor.aMeansCluster();
				orderedClusters = extractor.getPreferredOrdering();
				topAttributes[sel].setOrderedClusters(orderedClusters);
				//topAttributes[sel].setDeltaValue(extractor.getTargetValue()-topAttributes[sel].getToModifyValue()); // If NEGATIVE, reduce toModifyValue. If POSITIVE, increase toModifyValue
				tempMaxClusters = orderedClusters.length;
				if(tempMaxClusters > maxClusters)
					maxClusters = tempMaxClusters;
				FeatureList needsSuggestion = topAttributes[sel].getGenericName();
				//System.out.print(needsSuggestion);
				//System.out.print("=> target value: "+topAttributes[sel].getTargetValue());
				//System.out.print(" => present value: "+topAttributes[sel].getToModifyValue());
				//System.out.print(" => author's values: "+topAttributes[sel].getAuthorAvg()+" with a standard deviation of: "+topAttributes[sel].getAuthorStdDev());
				//System.out.println();
		}
		Logger.logln("Max number of clusters (after clustering all features): "+maxClusters);
		return maxClusters;
		
	}
	
	
	/**
	 * runs the @ClusterAnalyzer . Note that this method alone does <i>not</i> modify the clusters or the preference ordering.
	 * it simply analyzes the existing ones. 
	 * @param maxClusters the maximum number of clusters out of all of the features
	 */
	public void runClusterAnalysis(int maxClusters){
		Logger.logln("called runClusterAnalysis");
		//long startTime = System.currentTimeMillis();
		int lenTopAttribs = topAttributes.length;
		ClusterAnalyzer numberCruncher = new ClusterAnalyzer(featuresForClusterAnalyzer,maxClusters);
		int i =0;
		boolean success = false;
		for (i=0;i<lenTopAttribs;i++){
			success = numberCruncher.addFeature(topAttributes[i]);
			//System.out.println(topAttributes[i].getConcatGenNameAndStrInBraces()+" => success?  --"+success);
		}
		numberCruncher.analyzeNow();
		//long endTime = System.currentTimeMillis();
		//System.out.println("Time elapsed while using ClusterAnalyzer: "+(endTime-startTime));
		Logger.logln("calling makeViewer");
		ClusterViewer.makeViewer(topAttributes);
		//ClusterViewerFrame.startClusterViewer();
		Logger.logln("viewer made");
	}
	
	
	/**
	 * Runs the initial processing on all documents 
	 * @param magician
	 * @param cfd
	 * @param classifier
	 * @throws Exception
	 */
	public void runInitial(DocumentMagician magician, CumulativeFeatureDriver cfd, Classifier classifier) throws Exception{
		Logger.logln("called runIntitial in DataAnalyzer");
		//String authorToRemove = magician.loadExampleSet();
		List<Document> tempTrainDocs = pSet.getAllTrainDocs();
		/*
		for (Document d:tempTrainDocs){
			pSet.removeTrainDocAt(d.getAuthor(),d);
			pSet.addTrainDoc(d.getAuthor(), SentenceTools.removeUnicodeControlChars(d));
		}
		*/
		List<Document> tempTestDocs = pSet.getTestDocs();
		for (Document d:tempTestDocs){
			d.setAuthor(DocumentMagician.dummyName);
			//pSet.removeTestDoc(d);
			//pSet.addTestDoc(SentenceTools.removeUnicodeControlChars(d));
		}
		magician.initialDocToData(pSet,cfd, classifier);
		runGeneric(magician);
		int maxClusters =runAllTopFeatures();
		runClusterAnalysis(maxClusters);
		Logger.logln("Initial has been run.");
		
	}
	
	/**
	 * Runs tasks that have to be completed for both initial classifications (runInitial()) and secondary classifications (reRunModified())
	 * @param magician
	 * @throws Exception
	 */
	public void runGeneric(DocumentMagician magician) throws Exception{
		Logger.logln("Calling runGeneric");
		HashMap<String,Double[][]> attribsAndInstances = magician.getPackagedInstanceData();
		HashMap<String,Instances> simplyInstances = magician.getPackagedFullInstances();
		trainingInstancesArray = attribsAndInstances.get("training");
		authorInstancesArray = attribsAndInstances.get("author");
		toModifyInstancesArray = attribsAndInstances.get("modify");
		ArrayList<ArrayList<String>> allAttribSets = magician.getAllAttributeSets();
		allAttribs = allAttribSets.get(0);
		trainAttribs = allAttribSets.get(1);
		authorAttribs = allAttribSets.get(2);
		//computeInfoGain(simplyInstances.get("authorAndTrain"));
		topAttributes = computeInfoGain(simplyInstances.get("authorAndTrain")); // KEEP THIS!!!
		lengthTopAttributes = topAttributes.length;
		authorAverageFinder();
		authorStdDevFinder();
	
		minsAndMaxes();
		Logger.logln("Generic run.");
	}
	
	/**
	 * Re-classifies the modified document
	 * @param magician
	 * @throws Exception
	 */
	public void reRunModified(DocumentMagician magician) throws Exception{
		
		magician.reRunModified();
		//runGeneric(magician);
		//int maxClusters = runAllTopFeatures();
		//runClusterAnalysis(maxClusters);
		Logger.logln("Calling makeViewer in ClusterViewer after re-running modified.");
		ClusterViewer.makeViewer(topAttributes);
		Logger.logln("viewer made");
	}
	
	/**
	 * returns the names of the top 'numFeaturesToReturn' number of features
	 * @return
	 */
	public String[] getAllRelevantFeatures(){
		return importantAttribs;
	}
	
	
	public void setSelectedTargets(){
		Logger.logln("called setSelectedTargets after cluster group selection.");
		int i=0;
		int clusterNumberPlusOne;
		int clusterNumber;
		Cluster tempCluster;
		double target;
		mapMakerSentenceTargetSet = false;
		mapMakerCharTargetSet = false;
		String targetSaver = "                  ~~~~~~~ Targets ~~~~~~~\n";
		int targetsSaved = 0;
		for(i=0;i<lengthTopAttributes;i++){
			clusterNumberPlusOne = selectedTargets[i];
			clusterNumber = clusterNumberPlusOne-1;
			tempCluster = topAttributes[i].getOrderedClusters()[clusterNumber];
			target = tempCluster.getCentroid();
			targetSaver += "Attribute: "+topAttributes[i].getFullName()+"  ==> targetValue: "+target+"\n";
			topAttributes[i].setTargetCentroid(target);
			topAttributes[i].setTargetValue(target);
			topAttributes[i].setRangeForTarget(tempCluster.getMinValue(),tempCluster.getMaxValue()); // maybe this should be changed to avg. avs. dev.
			if((mapMakerSentenceTargetSet && mapMakerCharTargetSet) == false)
				mapMakerTargetSetter(topAttributes[i].getGenericName(),target);
		}
		System.out.println(targetSaver);
		
		boolean mustSaveTargets = false;
		if(mustSaveTargets == true){
			while(targetsSaved != 1){
				targetsSaved = saveTargets(targetSaver);
				if(targetsSaved == -1){
					Logger.logln("Something went wrong with saving targets to file. System exiting with code: 456");
					System.exit(456);
				}
			}
			Logger.logln("Targets saved to file.");
		}
		Logger.logln("Targets set.");
	}
	
	public int saveTargets(String theTargets){
		Logger.logln("About to save target string...");
		JFileChooser save = new JFileChooser();
		save.addChoosableFileFilter(new ExtFilter("txt files (*.txt)", "txt"));
		int answer = save.showSaveDialog(null);
		
		if (answer == JFileChooser.APPROVE_OPTION) {
			File f = save.getSelectedFile();
			String path = f.getAbsolutePath();
			if (!path.toLowerCase().endsWith(".txt"))
				path += ".txt";
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(path));
				bw.write(theTargets);
				bw.flush();
				bw.close();
				Logger.log("Saved contents of current tab to "+path);
			} catch (IOException exc) {
				Logger.logln("Failed opening "+path+" for writing",LogOut.STDERR);
				Logger.logln(exc.toString(),LogOut.STDERR);
				JOptionPane.showMessageDialog(null,
						"Failed saving contents of current tab into:\n"+path,
						"Save Problem Set Failure",
						JOptionPane.ERROR_MESSAGE);
				return -1;
			}
			return 1;
		} else {
            Logger.logln("Save contents of current tab canceled");
            return 0;
        }
	}	
	
	
	public void mapMakerTargetSetter(FeatureList name, double target){
		switch(name){
			//case AVERAGE_CHARACTERS_PER_WORD:
			//	HighlightMapMaker.avgCharTargetValue = target;
			//	mapMakerCharTargetSet = true;
			//	break;
			case AVERAGE_SENTENCE_LENGTH:
				mapMakerSentenceTargetSet = true;
				HighlightMapMaker.avgSentenceTargetValue = target;
				break;
			default: 
				break;
		}
			
			
	}
		
}