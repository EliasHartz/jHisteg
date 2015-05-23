package mainPackage;

public class HelloWorldMain {

	protected String whatToSay;
	
	/**
	 * Prints out hello world!
	 */
	public static void main(String[] args) {
		System.out.println("Hello World");
	}
	
	/**
	 * Creates a new object.
	 */
	public HelloWorldMain(){
		this("Hello world");
	}
	
	/**
	 * Creates a new object with data.
	 * 
	 * @param whatToSay : what is printed
	 */
	public HelloWorldMain(String whatToSay){
		this.whatToSay = whatToSay;
	}
	
	/**
	 * Says something out aloud.
	 */
	public void say(){
		System.out.println(whatToSay);
	}
	
	/**
	 * Updates what to say.
	 * 
	 * @param newData : what to say from now on
	 */
	public void setWhatToSay(String newData){
		if (newData.equals(whatToSay)) return;
		whatToSay = newData;
	}

	/**
	 * Greet someone.
	 * 
	 * @param name : name of the person
	 */
	public void respond(String name){
		System.out.println("Hello, "+name);
	}
}
