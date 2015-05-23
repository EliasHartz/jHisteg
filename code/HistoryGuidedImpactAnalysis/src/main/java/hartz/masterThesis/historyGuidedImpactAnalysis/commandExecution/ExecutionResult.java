package hartz.masterThesis.historyGuidedImpactAnalysis.commandExecution;

/**
 * Wrapper around two StreamWrappers with nice toString method
 * that is returned by the Executor.
 */
public class ExecutionResult {
	public final int exitCode;
	public final StreamWrapper errors;
	public final StreamWrapper output;

    /**
     * Contructs a new Execution Result container.
     *
     * @param returnCode : the integer code that the execution retured
     * @param errors : wrapper containing std.err-output
     * @param output : wrapper containing std.out-output
     */
	public ExecutionResult(int returnCode, StreamWrapper errors, StreamWrapper output){
		this.exitCode = returnCode;
		this.errors = errors;
		this.output = output;	
	}
	
	@Override
	public String toString(){
		String o = output.getContent();
		if (o==null || o.isEmpty()) o = "## NOTHING WAS PRINTED ON >OUTPUT ##\n";
		String e = errors.getContent();
		if (e==null || e.isEmpty()) e = "## NOTHING WAS PRINTED ON >ERROR  ##\n";
		String s = 
				o+
				e+
				"##  => exit code was "+exitCode+" ##\n";
		return s;
	}
	
	/**
	 * @return 'true' if the process printed anything on std.err
	 */

	public boolean hadErrors() {
		return (errors.getContent()!=null && !errors.getContent().isEmpty());
	}
	
	/**
	 * @return 'true' if the process printed anything on std.out
	 */
	public boolean hadOutput() {
		return (output.getContent()!=null && !output.getContent().isEmpty());
	}
}
