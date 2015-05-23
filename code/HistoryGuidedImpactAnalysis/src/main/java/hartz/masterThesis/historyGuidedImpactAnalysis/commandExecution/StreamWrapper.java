package hartz.masterThesis.historyGuidedImpactAnalysis.commandExecution;

import hartz.masterThesis.historyGuidedImpactAnalysis.main.OutputType;
import hartz.masterThesis.historyGuidedImpactAnalysis.main.Tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * Thread-wrapper around an InputStream to allow for easier use 
 * with the Executor. Can both store and print the output of
 * external programs called via Processes.
 */
public class StreamWrapper extends Thread{
	private final InputStream stream;
	private final OutputType type;
	private String content ="";

	/**
	 * @param is : stream to wrap around and listen to
	 * @param type : the type of the data stored within
	 */
	StreamWrapper(InputStream is, OutputType type){
		this.stream = is;
		this.type = type;
	}

	@Override
	public void run(){
		try{
			InputStreamReader isr = new InputStreamReader(stream);
			BufferedReader br = new BufferedReader(isr);
			String line;
			while ((line = br.readLine()) != null){
				String s = "EXTERNAL_"+type + ":> " + line.trim(); 
				content+= s+"\n";
				if (!s.isEmpty()) Tool.printToolOutput(s,type);
			}
		} catch (IOException ioe){
			ioe.printStackTrace();  
		}
	}
	
	/**
	 * @return content stored within, may be empty
	 */
	public String getContent(){
		return content;
	}
	
	/**
	 * @return content stored within, may be empty
	 */
	@Override
	public String toString(){
		return content.isEmpty()?type.name().toUpperCase()+"> NOTHING WAS PRINTED":content;
	}
}
