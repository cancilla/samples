/* Generated by Streams Studio: April 25, 2015 9:07:08 PM EDT */
package com.acme.test;


import java.io.IOException;

import org.apache.log4j.Logger;

import com.ibm.streams.operator.AbstractOperator;
import com.ibm.streams.operator.OperatorContext;
import com.ibm.streams.operator.OutputTuple;
import com.ibm.streams.operator.StreamingOutput;
import com.ibm.streams.operator.model.OutputPortSet;
import com.ibm.streams.operator.model.OutputPortSet.WindowPunctuationOutputMode;
import com.ibm.streams.operator.model.OutputPorts;
import com.ibm.streams.operator.model.PrimitiveOperator;
import com.ibm.streams.operator.state.Checkpoint;
import com.ibm.streams.operator.state.StateHandler;

/**
 * A source operator that does not receive any input streams and produces new tuples. 
 * The method <code>produceTuples</code> is called to begin submitting tuples.
 * <P>
 * For a source operator, the following event methods from the Operator interface can be called:
 * </p>
 * <ul>
 * <li><code>initialize()</code> to perform operator initialization</li>
 * <li>allPortsReady() notification indicates the operator's ports are ready to process and submit tuples</li> 
 * <li>shutdown() to shutdown the operator. A shutdown request may occur at any time, 
 * such as a request to stop a PE or cancel a job. 
 * Thus the shutdown() may occur while the operator is processing tuples, punctuation marks, 
 * or even during port ready notification.</li>
 * </ul>
 * <p>With the exception of operator initialization, all the other events may occur concurrently with each other, 
 * which lead to these methods being called concurrently by different threads.</p> 
 */
@PrimitiveOperator(name="SimpleSourceOp", namespace="com.acme.test",
description="Java Operator SimpleSourceOp")
@OutputPorts({@OutputPortSet(description="Port that produces tuples", cardinality=1, optional=false, windowPunctuationOutputMode=WindowPunctuationOutputMode.Generating), @OutputPortSet(description="Optional output ports", optional=true, windowPunctuationOutputMode=WindowPunctuationOutputMode.Generating)})
// To support consistent regions, this operator must extend StandHandler interface.
public class SimpleSourceOp extends AbstractOperator implements StateHandler {
	/**
	 * Thread for calling <code>produceTuples()</code> to produce tuples 
	 */
    private Thread processThread;
    // Product details
    int serialNumber;
    int productId;
    String productName; 
    int quantity;     
    

    /**
     * Initialize this operator. Called once before any tuples are processed.
     * @param context OperatorContext for this operator.
     * @throws Exception Operator failure, will cause the enclosing PE to terminate.
     */
    @Override
    public synchronized void initialize(OperatorContext context)
            throws Exception {
    	// Must call super.initialize(context) to correctly setup an operator.
        super.initialize(context);
        Logger.getLogger(this.getClass()).trace("Operator " + context.getName() + " initializing in PE: " + context.getPE().getPEId() + " in Job: " + context.getPE().getJobId() );
        
        // TODO:
        // If needed, insert code to establish connections or resources to communicate an external system or data store.
        // The configuration information for this may come from parameters supplied to the operator invocation, 
        // or external configuration files or a combination of the two.
		serialNumber = 0;
		productId = 100;
		productName = "";
		quantity = 10; 

		// Do an init delay of 4 seconds for other operators to get connected.
		Thread.sleep(4*1000);		
		
        /*
         * Create the thread for producing tuples. 
         * The thread is created at initialize time but started.
         * The thread will be started by allPortsReady().
         */
        processThread = getOperatorContext().getThreadFactory().newThread(
                new Runnable() {

                    @Override
                    public void run() {
                    	while (Thread.currentThread().interrupted() == false) {
	                        try {
	                            produceTuples();
	                        } catch (Exception e) {
	                            Logger.getLogger(this.getClass()).error("Operator error", e);
	                        }                    
                    	}
                    }
                    
                });
        
        /*
         * Set the thread not to be a daemon to ensure that the SPL runtime
         * will wait for the thread to complete before determining the
         * operator is complete.
         */
        processThread.setDaemon(false);
    }

    /**
     * Notification that initialization is complete and all input and output ports 
     * are connected and ready to receive and submit tuples.
     * @throws Exception Operator failure, will cause the enclosing PE to terminate.
     */
    @Override
    public synchronized void allPortsReady() throws Exception {
        OperatorContext context = getOperatorContext();
        Logger.getLogger(this.getClass()).trace("Operator " + context.getName() + " all ports are ready in PE: " + context.getPE().getPEId() + " in Job: " + context.getPE().getJobId() );
    	// Start a thread for producing tuples because operator 
    	// implementations must not block and must return control to the caller.
        processThread.start();
    }
    
    /**
     * Submit new tuples to the output stream
     * @throws Exception if an error occurs while submitting a tuple
     */
    private void produceTuples() throws Exception  {
        final StreamingOutput<OutputTuple> out = getOutput(0);
        
        // Sleep a second before submitting each tuple.
        Thread.sleep(1*1000);

    	// Don't submit more than 100 tuples since it is only a very simple test.
    	if (serialNumber >= 100) {
    		return;
        }        
        
        // TODO Modify the following code to create and submit tuples to the output port
        OutputTuple tuple = out.newTuple();
            
        // Set attributes in tuple:
        tuple.setInt(0, ++serialNumber);
        tuple.setInt(1, ++productId);
        String _productName = "Product" + productId;
        tuple.setString(2, _productName);
        tuple.setInt(3, ++quantity);
        // Submit tuple to output stream            
        out.submit(tuple);
        
        // TODO If there is a finite set of tuples, submit a final punctuation when finished
        // by uncommenting the following line:
        // out.punctuate(Punctuation.FINAL_MARKER);
    }

    /**
     * Shutdown this operator, which will interrupt the thread
     * executing the <code>produceTuples()</code> method.
     * @throws Exception Operator failure, will cause the enclosing PE to terminate.
     */
    public synchronized void shutdown() throws Exception {
        if (processThread != null) {
            processThread.interrupt();
            processThread = null;
        }
        OperatorContext context = getOperatorContext();
        Logger.getLogger(this.getClass()).trace("Operator " + context.getName() + " shutting down in PE: " + context.getPE().getPEId() + " in Job: " + context.getPE().getJobId() );
        
        // TODO: If needed, close connections or release resources related to any external system or data store.

        // Must call super.shutdown()
        super.shutdown();
    }
    
    // To support the consistent regions, this operator must implement the following methods.    
    @Override
    // Submit any pending tuples before checkpoint happens.
    public void drain() {
    	// Do nothing.
    }
    
	@Override
	// Persist state to shared file system	
	public void checkpoint(Checkpoint checkpoint) throws Exception {
		checkpoint.getOutputStream().writeObject(serialNumber);
		checkpoint.getOutputStream().writeObject(productId);
		checkpoint.getOutputStream().writeObject(productName);
		checkpoint.getOutputStream().writeObject(quantity);
	}

	@Override
	// Restore state from shared file system	
	public void reset(Checkpoint checkpoint) throws Exception {
		try {
			// Read it back in the same order as it was checkpointed.
			serialNumber = ((Integer)checkpoint.getInputStream().readObject()).intValue();
			productId = ((Integer)checkpoint.getInputStream().readObject()).intValue();
			productName = (String)checkpoint.getInputStream().readObject();
			quantity = ((Integer)checkpoint.getInputStream().readObject()).intValue();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	// Sets operator state to its initial state
	// This is needed only when there is a crash anywhere in the application before the
	// very first checkpoint is done.	
	public void resetToInitialState() throws Exception {
		serialNumber = 0;
		productId = 100;
		productName = "";
		quantity = 10; 		
	}	
	
	@Override
	public void retireCheckpoint(long id) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub		
	}
}
