/**
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */
import java.io.*;
import java.util.*;


/**
 *  The SYN operator for all retrieval models.
 */
public class QryIopWindow extends QryIop {

  /**
   *  Evaluate the query operator; the result is an internal inverted
   *  list that may be accessed via the internal iterators.
   *  @throws IOException Error accessing the Lucene index.
   */
	private int distance;
	private int numOfMatch;
	
	public QryIopWindow(int k){
		this.distance = k;
	}
	
  protected void evaluate () {

    //  Create an empty inverted list.  If there are no query arguments,
    //  that's the final result.
    
    this.invertedList = new InvList (this.getField());

    if (args.size () == 0) {
      return;
    }
    List<Integer> positions = new ArrayList<Integer>();
    
    //  Each pass of the loop adds 1 document to result inverted list
    //  until all of the argument inverted lists are depleted.

    int minDocid = Qry.INVALID_DOCID;

    for (Qry q_i: this.args) {
  	  //QryIop null as prapmeter is ok
      if (q_i.docIteratorHasMatch (null)) {
        int q_iDocid = q_i.docIteratorGetMatch ();
        
        if ((minDocid > q_iDocid) ||
            (minDocid == Qry.INVALID_DOCID)) {
          minDocid = q_iDocid;
        }
      }
    }

    if (minDocid == Qry.INVALID_DOCID)
      return;				// All docids have been processed.  Done.
    
    while (true) {

      //  Find the minimum next document id.  If there is none, we're done.


      ArrayList<Integer> l = new ArrayList<Integer>();
      int[] locationPtr = new int[this.args.size()];
      
      
      if(this.docIteratorHasMatchAll()){
    	  minDocid = this.args.get(0).docIteratorGetMatch();
    	 // System.out.println("minDocid: "+minDocid);
    	  
      }else{
    	  return;
      }
      //initialize all location ptr
      for(int i=0; i < this.args.size(); i++){
    	  Qry q_i = this.args.get(i);
    	  boolean check = ((QryIop) q_i).locIteratorHasMatch();
    	  if(!check)
    		  return;
    	  locationPtr[i] = ((QryIop) q_i).locIteratorGetMatch();
      }
      
      int minLoc = -1;
      int maxLoc = -1;
      int minLocIdx = -1;
      boolean stopFlag= false;
      
	  while(docIdAllNearhasMatch()){
		maxLoc = -1;
		minLoc = -1;
		minLocIdx  = -1;
		
		for(int i = 0; i < this.args.size(); i++) {
			QryIop q_i = (QryIop) this.args.get(i);
			
			if (q_i.locIteratorHasMatch()) {
				int loc_i = q_i.locIteratorGetMatch();
		
				if(loc_i > maxLoc || maxLoc == -1){
					maxLoc = loc_i;	
				}
				if(loc_i < minLoc || minLoc == -1){
					minLoc = loc_i;
					minLocIdx = i;
				}
			}else{
				stopFlag = true;
			}
		}
		
		if(stopFlag){
			break;
		}
		
		int dist = maxLoc - minLoc;
		
		if (dist < distance) {
			
			positions.add(maxLoc);
			// advance the smallest 
			for (int k = 0;k < this.args.size();k++){
				((QryIop)(this.args.get(k))).locIteratorAdvance();
			}
		}
		else{
			((QryIop)(this.args.get(minLocIdx))).locIteratorAdvance();
		}
	  
      }
      
      if(!positions.isEmpty())
    	  this.invertedList.appendPosting(minDocid, positions);
      
      
      positions.clear();
      this.args.get(0).docIteratorAdvancePast(minDocid);
      
      }

    }
 
  //true if location
  private boolean docIdAllNearhasMatch() {
	// TODO Auto-generated method stub
	for(int i=0;i< this.args.size();i++){
		if (!((QryIop)this.args.get(i)).locIteratorHasMatch()){
			return false;
		}
	}
	
	return true;
}

//advance all ptrs
private void advanceAllPtr() {
	// TODO Auto-generated method stub
	  for(int i=0;i< this.args.size();i++){
		  ((QryIop)this.args.get(i)).locIteratorAdvance(); 
	  }
}

//advance min pters
private int advanceMinPtr(int[] locationPtr) {
	// TODO Auto-generated method stub
	  int minIdx = 0;
	  for(int i=0;i< locationPtr.length;i++){
		  if(locationPtr[i] < locationPtr[minIdx]){
			  minIdx = i;
		  }
	  }

	  
	  return minIdx;
	
}

//true if docid at index i has match
private boolean docIdNearhasMatch(int i) {
	  
	  return ((QryIop)this.args.get(i)).locIteratorHasMatch();
  }

  private void advancePtr(int ptr) {
	  ((QryIop)this.args.get(ptr)).locIteratorAdvance();
	  
	  if(ptr == 0){
		  for (int i=1; i< this.args.size();i++){
			  ((QryIop)this.args.get(i)).locIteratorAdvancePast(0);
		  }
	  }
	  return;
  }

  private int abs(int a){
	  return (a > 0)?a:(-a);
  }
  
  // to chek the docid
  private boolean checkSameDocid(int[] docIdForNear){
	  
	  int anchor = docIdForNear[0];
	  
	  for(int id : docIdForNear){
		  if(id != anchor)
			  return false;
	  }
	  
	  return true;
  }
  
  // true if location satisfy condition
  private boolean  checkValidLocation(int[] locationPtr){
	  int firstLoc;
	  int secondLoc;
	  int diff;
	  int len = locationPtr.length-1;
	  
	  for (int i=0;i< len; i++){
		  firstLoc = locationPtr[i];
		  secondLoc = locationPtr[i+1];
		  diff = secondLoc - firstLoc;
		  if( diff > distance || diff <= 0){
			  return false;
		  }
	  }
	  return true;

  }
  
  
  private boolean docIteratorHasMatchAll(){
	  
	  boolean matchFound = false;

	    // Keep trying until a match is found or no match is possible.

	    while (! matchFound) {

	      // Get the docid of the first query argument.
	      
	      if (! this.args.get (0).docIteratorHasMatch (null)) {
	    	  return false;
	      }

	      int docid_0 = this.args.get (0).docIteratorGetMatch ();

	      // Other query arguments must match the docid of the first query
	      // argument.
	      
	      matchFound = true;

	      for (int i=1; i<this.args.size(); i++) {
	    	 
	    	  this.args.get(i).docIteratorAdvanceTo (docid_0);

			  if (! this.args.get(i).docIteratorHasMatch (null)) {	// If any argument is exhausted
			   return false;				// there are no more matches.
			  }

			  int docid_i = this.args.get(i).docIteratorGetMatch ();

			  if (docid_0 != docid_i) {	// docid_0 can't match.  Try again.
				  this.args.get(0).docIteratorAdvancePast (docid_0);
				  matchFound = false;
				  break;
			 }
	      }

	      if (matchFound) {
	    	
	        this.args.get(0).docIteratorSetMatchCache (docid_0);
	      }
	    }
	    
	    return true;
  }

}
