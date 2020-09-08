

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

public class BoundedPriorityQueue extends PriorityQueue<Double> {
	private static final long serialVersionUID = 1L;
	int max_size;
	
	public BoundedPriorityQueue(int max_size) {
		super(Comparator.<Double>naturalOrder()); //aufsteigend
		this.max_size=max_size;
	}
	
	@Override
	public boolean offer(Double d) {
		if (this.size() < max_size) //ist noch platz, also mach mal rein
		{	
			return super.offer(d);}
		else {
			// es ist kein platz mehr
			// ist es größer als bisheriges minumum?
			if (d > this.peek()) { 
				super.poll(); //remove smallest one, insert the new one
				return super.offer(d);
			} else {
				return false; //do nothing
			}
		}	
	}
	
	public double eps_rm(final double eps, StreamPolicy p) {
		double eps_rm = 0;
		
		double sum = this.sum();
		if (this.size() == p.tinar_time_stamps) { //geht nur, wenn queue schon voll
			sum-=this.peek(); //kleinstes
		}
		eps_rm = eps - sum;
		
		return eps_rm;
		
	}
	
	public double eps_anyway(StreamPolicy p) {
		double max_eps_anyway =  0;
		if (this.size() == p.tinar_time_stamps) { //geht nur, wenn queue schon voll
			max_eps_anyway = this.peek(); //kleinstes
		}
		return max_eps_anyway;
		
	}
	
	public double sum() {
		if (this.size() == 0)
			return 0;
		else
			return  this.stream().reduce(Double::sum).get();
	}
	
	public void update(final double eps_2_spend) {
		this.add(eps_2_spend);
	}
}