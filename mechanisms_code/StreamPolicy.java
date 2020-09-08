

import java.util.ArrayList;
import java.util.HashSet;

public class StreamPolicy {
	static final int LENGTH_J = 0;
	static final int SUM_PATTERN_LENGTH = 1;
	static final int OVERLAP = 2;
	static final int PLACEMENT = 3;
	static final int BOUND_DELTA_J = OVERLAP;
	static {
		System.out.println("SwellfishPolicy - Using delta(J) bound: "+BOUND_DELTA_J);
	}
	
	static int next_id = 0;//used to assign each policy a unique ID
	public final int id;
	public final int J_start;
	public int J_stop;
	final double delta;
	final int pattern_length;
	ArrayList<StreamPolicy> concurent = new ArrayList<StreamPolicy>();
	public int tinar_time_stamps = -1;
	
	public StreamPolicy(final int start, final int pattern_length, final double delta){
		this.id = StreamPolicy.next_id++;
		this.J_start = start;
		this.pattern_length = pattern_length;
		this.delta = delta;
		J_stop = start;//some value
	}
	
	void isStillRelevant(final int t){
		J_stop = t;
	}

	public void remember_concurrent(StreamPolicy concurrent_policy) {
		/*if(this.id==concurrent_policy.id){
			System.err.println("Same id");
			return;
		}*/
		this.concurent.add(concurrent_policy);
	}
	
	@SuppressWarnings("unused")
	public int tinar_time_stamps(){
		//checkConcurrent();
		if(this.tinar_time_stamps==-1){//compute it only once
			if(BOUND_DELTA_J==LENGTH_J){
				tinar_time_stamps = bound_J_length();
			}else if(BOUND_DELTA_J==SUM_PATTERN_LENGTH){
				tinar_time_stamps = bound_pattern_sum();
			}else if(BOUND_DELTA_J==OVERLAP){
				tinar_time_stamps = bound_overlap();
			}else if(BOUND_DELTA_J==PLACEMENT){
				tinar_time_stamps = bound_placement();
			}else {
				System.err.println("SwellfishPolicy.tinar_time_stamps() unknown bound "+BOUND_DELTA_J);
				tinar_time_stamps = bound_J_length();
			}
			
			/*tinar_time_stamps = this.pattern_length;
			
			for(SwellfishPolicy p : concurent){
				int start_overlap = Math.max(J_start, p.J_start);
				int stop_overlap = Math.min(J_stop, p.J_stop);
				int overlap = Math.min(stop_overlap-start_overlap+1, p.pattern_length);
				tinar_time_stamps+=overlap;
			}
			*/
			final int my_relevance_length = J_length();// we always bound by length of J Interval
			tinar_time_stamps = Math.min(my_relevance_length, tinar_time_stamps);
		}
		return tinar_time_stamps;
	}
	
	public int J_length(){
		return J_stop-J_start+1;
	}
	
	int bound_J_length(){
		return J_length();
	}
	
	int bound_pattern_sum(){
		int sum = this.pattern_length;
		for(StreamPolicy p : concurent){
			sum+=p.pattern_length;
		}
		return sum;
	}
	
	
	int bound_overlap(){
		int sum = this.pattern_length;
		for(StreamPolicy p : concurent){
			int start_overlap = Math.max(J_start, p.J_start);
			int stop_overlap = Math.min(J_stop, p.J_stop);
			int overlap = Math.min(stop_overlap-start_overlap+1, p.pattern_length);
			sum+=overlap;
		}
		return sum;
	}

	/**
	 * TODO
	 * @return
	 */
	int bound_placement(){
		
		Placement[] policies;
		
		//clear placements |P| > overlap
		
		return Integer.MAX_VALUE;
	}
	
	/** For debug */
	void checkConcurrent() {
		HashSet<Integer> ids = new HashSet<Integer>();
		for(StreamPolicy p : concurent){
			if(p.id==this.id){
				System.err.println("Am concurrent to myself: "+id);
			}
			if(ids.contains(p.id)){
				System.err.println("I ("+id+") Already have this Policy: "+p.id);
			}else{
				ids.add(p.id);
			}
		}
	}

	public boolean isRelevant(int t) {
		if(this.J_start<=t && t <= this.J_stop)
			return true;
		else 
			return false;
	}
	
	@Override
	public String toString() {
		return "(id="+id+", ["+J_start+","+J_stop+"],|p|="+pattern_length+", TINAR="+tinar_time_stamps+")";
	}
	
	class Placement{
		int from,to;
		int length;
	}
}
