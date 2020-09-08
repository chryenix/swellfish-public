

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class PolicyCollection {
	public final double[] temporal_sensitivity;
	public final int[] max_tinar_t;
	public final int[] max_w;

	public final ArrayList<ArrayList<StreamPolicy>> relevant_policies_per_t;
	public final ArrayList<StreamPolicy> all_policies;
	
	public PolicyCollection(double[] temporal_sensitivity,int[] max_tinar_t, int[] max_w, ArrayList<StreamPolicy> all_policies, ArrayList<ArrayList<StreamPolicy>> relevant_policies_per_t) {
		this.temporal_sensitivity = temporal_sensitivity;
		this.max_tinar_t	= max_tinar_t;
		this.max_w 				  = max_w;
		this.all_policies 		  = all_policies;
		this.relevant_policies_per_t = relevant_policies_per_t;
	}
	
	public int getWEventW() {
		return Arrays.stream(this.max_w).max().getAsInt();
	}
	
	

	void toFile(String name){
		String file = folder+name;
		double start = System.currentTimeMillis();
		System.out.print("PolicyCollection.toFile(): "+file);
		File f = new File(file);
		try {
			BufferedWriter w = new BufferedWriter(new FileWriter(f));
			
			w.write(",temp_sen,tinar,max_len_j_active_policies\n");
			final int length = this.temporal_sensitivity.length; 
			for(int t=0;t<length;t++){
				w.write(t+","+temporal_sensitivity[t]+","+max_tinar_t[t]+","+max_w[t]+"\n");
			}
			w.close();
			double stop = System.currentTimeMillis();
			System.out.println(" [DONE] in "+(stop-start)+" ms");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void out(final int num) {
		System.out.println(",temp_sen,tinar,max_len_j_active_policies");
		for(int t=0;t<num;t++){
			System.out.println(t+","+temporal_sensitivity[t]+","+max_tinar_t[t]+","+max_w[t]);
		}
		
	}
	
	public static final String folder = ".\\data\\swellfish\\policies\\";
	/** Offset in File */
	private static final int SENSITIVITY = 1;
	/** Offset in File */
	private static final int TINAR 		 = 2;
	/** Offset in File */
	private static final int MAX_W 		 = 3;
	/**
	 * Expects file format:
	 * Line 1: Header: ,temp_sen,tinar,max_len_j_active_policies
	 * 
	 * @param name
	 * @return
	 */
	public static PolicyCollection readFromFile(String name){
		String file = folder+name;
		double start = System.currentTimeMillis();
		System.out.print("PolicyInfo.readFromFile(): "+file);
		File f = new File(file);
		String line;
		if(f.exists()){
			ArrayList<String[]> file_content = new ArrayList<String[]>(1000);
			try{
				BufferedReader inFile = new BufferedReader(new FileReader(f));
				inFile.readLine();//skip header
				while ((line = inFile.readLine()) != null) {
					String[] tokens = line.split(",");
					file_content.add(tokens);
				}
				inFile.close();
			} catch (IOException e) {
				System.err.println(e);
				return null;
			}
			final int length = file_content.size();
			final double[] temporal_sensitivity = new double[length];
			final int[] max_tinar_t			= new int[length];
			final int[] max_w				= new int[length];
			
			double temp;
			for(int t=0;t<length;t++){
				String[] data_t = file_content.get(t);
				temp = Double.parseDouble(data_t[SENSITIVITY]);
				temporal_sensitivity[t] = temp;
				temp = Double.parseDouble(data_t[TINAR]);
				max_tinar_t[t] 			= (int) temp;
				temp = Double.parseDouble(data_t[MAX_W]);
				max_w[t] 				= (int) temp;
			}
			
			double stop = System.currentTimeMillis();
			System.out.println(" [DONE] in "+(stop-start)+" ms");
			return new PolicyCollection(temporal_sensitivity, max_tinar_t, max_w, null,null);
		}else{
			System.err.println("File does not exist: "+file);
			return null;
		}
	}
	
	static boolean compare(PolicyCollection p_1, PolicyCollection p_2){
		if(p_1.temporal_sensitivity.length!=p_2.temporal_sensitivity.length){
			System.err.println("Not the same length");
			return false;
		}else return compare(p_1, p_2, p_1.temporal_sensitivity.length);
	}
	
	static boolean compare(PolicyCollection p_1, PolicyCollection p_2, int length){
		boolean equal = true;
		for(int t=0;t<length;t++){
			if(p_1.temporal_sensitivity[t]!=p_2.temporal_sensitivity[t]){
				System.err.println("Temporal sensitivity at t="+t+" do not match: "+p_1.temporal_sensitivity[t]+" vs. "+p_2.temporal_sensitivity[t]);
				equal = false;
			}
			if(p_1.max_tinar_t[t]!=p_2.max_tinar_t[t]){
				System.err.println("TINAR at t="+t+" do not match: "+p_1.max_tinar_t[t]+" vs. "+p_2.max_tinar_t[t]);
				equal = false;
			}
			if(p_1.max_w[t]!=p_2.max_w[t]){
				System.err.println("MAX w at t="+t+" do not match: "+p_1.max_w[t]+" vs. "+p_2.max_w[t]);
				equal = false;
			}
			if(!equal){
				if(p_2.all_policies!=null){
					for(StreamPolicy p : p_2.all_policies){
						if(p.isRelevant(t)){
							System.out.println(p);
						}
					}
				}
				return false;//until first error
			}
		}
		return equal;
	}
	
	/*public static void main(String[] args){
		PolicyCollection correct = readFromFile("correct_policy_collection.csv");
		PolicyCollection test = PolicyCollectionReader.read();
		compare(correct, test);
		test.toFile("test.csv");
	}*/

	/*void outStatistics(){
		double mean_sensitivity = 	SwellfishExperiment.mean(this.temporal_sensitivity);
		double mean_tina = 			SwellfishExperiment.mean(this.max_tinar_t);
		double mean_w = 			SwellfishExperiment.mean(this.max_w);
		System.out.println("mean(s),mean(tinar),mean(w)");
		System.out.println(mean_sensitivity+", "+mean_tina+", "+mean_w);
		System.out.println("max(s),max(tinar),max(w)");
		System.out.println(max(temporal_sensitivity)+", "+max(max_tinar_t)+", "+max(max_w));
	}
	*/

}
