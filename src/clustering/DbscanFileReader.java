package clustering;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;

import ca.pfv.spmf.algorithms.clustering.dbscan.DoubleArrayDBS;
import ca.pfv.spmf.patterns.cluster.DoubleArray;
import utils.Utils;
import utils.DBSCAN.MyDoubleArrayDBS;
import ca.pfv.spmf.patterns.cluster.DoubleArray;


public class DbscanFileReader {

	private HashMap<Integer,List<Cluster<PointWrapper>>> clusterMap = new HashMap<Integer,List<Cluster<PointWrapper>>>();
	private String inputFilePath;
	private Reader csvData;
	private Iterable<CSVRecord> records;
	private Iterator<CSVRecord> iterator;
	List<PointWrapper> clusterInput;
	int currentTime;
	CSVRecord record=null;
	private List<Cluster<PointWrapper>> empty = new ArrayList<Cluster<PointWrapper>>();
	List<DoubleArray> points = new ArrayList<DoubleArray>();
	double[] hangingVector = null;
	private long clusteringTime=0;
	private long startTime=0;
	public DbscanFileReader() {
	}
	public DbscanFileReader(String inputFilePath) throws FileNotFoundException {
		File file = new File(inputFilePath);
		int count=0;
		if(!file.exists()){
			throw new FileNotFoundException(inputFilePath);
		}
		csvData = null;
		try {
			csvData = new FileReader(file);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		try {
			records = CSVFormat.RFC4180.withHeader("oid","t","lat","long").parse(csvData);
			iterator = records.iterator();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		clusterInput = new ArrayList<PointWrapper>();
		currentTime=-1;
	}
	
	public List<DoubleArray> getNextPoints(long t){
		startTime = System.currentTimeMillis();
		points.clear();
		if(hangingVector!=null){
			points.add(new MyDoubleArrayDBS(hangingVector));
		}
		if(iterator.hasNext()==false){
			return null;	//null shows that no more data
		}
		if(currentTime>t){
			return null;
		}
		List<Cluster<PointWrapper>> clusterResults=null;
		
//		DBSCANClusterer<PointWrapper> dbscan = new DBSCANClusterer<PointWrapper>(e, m);
		
		while(iterator.hasNext()) {
			record = iterator.next();
			currentTime = Double.valueOf(record.get("t")).intValue();
			if(currentTime<t){
				continue;
			}
			else if(currentTime==t){
				double [] vector = new double[4]; 
				vector[0]=Double.parseDouble(record.get("long"));
				vector[1]=Double.parseDouble(record.get("lat"));
				vector[2]=Double.parseDouble(record.get("oid"));
				vector[3]=Double.parseDouble(record.get("t"));
				points.add(new MyDoubleArrayDBS(vector));
			}
			else if(currentTime>t){
				hangingVector = new double[4];
				hangingVector[0]=Double.parseDouble(record.get("long"));
				hangingVector[1]=Double.parseDouble(record.get("lat"));
				hangingVector[2]=Double.parseDouble(record.get("oid"));
				hangingVector[3]=Double.parseDouble(record.get("t"));
				break;
			}
		}
		return points;
	}
	
	public List<Cluster<PointWrapper>> getNextCluster(int m, double e, long t){
		startTime = System.currentTimeMillis();
		if(iterator.hasNext()==false){
			return null;	//null shows that no more data
		}
		if(currentTime>t){
			return empty;
		}
		List<Cluster<PointWrapper>> clusterResults=null;
		
		DBSCANClusterer<PointWrapper> dbscan = new DBSCANClusterer<PointWrapper>(e, m);
		
		while(iterator.hasNext()) {
			record = iterator.next();
			currentTime = Double.valueOf(record.get("t")).intValue();
			if(currentTime<t){
				continue;
			}
			else if(currentTime==t){
				PointWrapper p = new PointWrapper(Integer.parseInt(record.get("oid")),
						Double.parseDouble(record.get("long")),
						Double.parseDouble(record.get("lat")), Long.parseLong(record.get("t")));
				clusterInput.add(p);
			}
			else if(currentTime>t){
				if(clusterInput.size()>=m){
					 clusterResults = dbscan.cluster(clusterInput);
				}
				clusterInput.clear();
				PointWrapper p = new PointWrapper(Integer.parseInt(record.get("oid")),
						Double.parseDouble(record.get("long")),
						Double.parseDouble(record.get("lat")), Long.parseLong(record.get("t")));
				clusterInput.add(p);
				break;
			}
		}
		if(!iterator.hasNext()){
			if(clusterInput.size()>=m){
				 clusterResults = dbscan.cluster(clusterInput);
			}
			clusterInput.clear();
		}
		if(clusterResults!=null && clusterResults.size()!=0){
			clusteringTime = clusteringTime + (System.currentTimeMillis() - startTime);
			return clusterResults;
		}
		else{
			clusteringTime = clusteringTime + (System.currentTimeMillis() - startTime);
			return empty;
		}
	}
	
	public HashMap<Integer, List<Cluster<PointWrapper>>> DBSCAN(String inputFilePath,int m, double e, int numFiles) throws FileNotFoundException {
		File file = new File(inputFilePath);
		int count=0;
		if(!file.exists()){
			throw new FileNotFoundException(inputFilePath);
		}
		if(file.isDirectory()){
			File[] files = file.listFiles();
			for(File f:files){
				clusterFile(f,m,e);
				count++;
				if(count==numFiles){
					break;
				}
			}
		}
		else{
			clusterFile(file,m,e);
		}
		
		
		return clusterMap;
	}
	
	public long getClusteringTime(){
		return clusteringTime;
	}
	
	private void clusterFile(File f,int m, double e){
		Reader csvData = null;
		try {
			csvData = new FileReader(f);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		Iterable<CSVRecord> records = null;
		try {
			records = CSVFormat.RFC4180.withHeader("oid","t","lat","long").parse(csvData);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
//		CSVParser parser = CSVParser.parse(csvData.toString(), CSVFormat.RFC4180);
		
		int prevTime=-1;
		int currentTime=-1;
		List<PointWrapper> clusterInput = new ArrayList<PointWrapper>();
		DBSCANClusterer<PointWrapper> dbscan = new DBSCANClusterer<PointWrapper>(e, m);
		
		int t=0;
		
		for (CSVRecord record : records) {
			if(currentTime!=-1 && Double.valueOf(record.get("t")).intValue()!=currentTime){
				prevTime=currentTime;
				if(clusterInput.size()>=m){
					List<Cluster<PointWrapper>> clusterResults = dbscan.cluster(clusterInput);
					if(clusterResults!=null && clusterResults.size()!=0){
//						System.out.print("ts = "+currentTime +", |input| = "+ clusterInput.size() +", |C| = "+clusterResults.size()+", Clusters = ");
						for(int k=0;k<clusterResults.size();k++){
							List<Integer> objs = Utils.clusterToConvoyList(clusterResults).get(k).getObjs();
							Collections.sort(objs);
//							System.out.print(objs.toString().replaceAll(",",""));
						}
//						System.out.print("\n");
						clusterMap.put(currentTime, clusterResults);
					}
				}
				clusterInput.clear();
			}
			currentTime = Double.valueOf(record.get("t")).intValue();
			PointWrapper p = new PointWrapper(Integer.parseInt(record.get("oid")),
					Double.parseDouble(record.get("long")),
					Double.parseDouble(record.get("lat")),currentTime);
			clusterInput.add(p);
		}
		//for last cluster
		if(clusterInput.size()>=m){
			List<Cluster<PointWrapper>> clusterResults = dbscan.cluster(clusterInput);
			System.out.println("clustering done");
			clusterMap.put(currentTime, clusterResults);
		}
	}
}
