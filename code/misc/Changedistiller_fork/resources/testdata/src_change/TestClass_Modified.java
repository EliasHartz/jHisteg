package thisPackageOfMine;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
/**
 * No reports about the accessiblity changes or type changes of the fields.
 */
public class TestClass {

	public boolean myNowRenamedBoolean = true;
        protected String s = "some value that cannot be modified!";
        private final int number; 

	protected final ArrayList<List<String>> stringList = new ArrayList<>();
	protected ArrayList<String> myOtherStringList;

	public static void main(String[] args) throws Exception{
		throw new IllegalArgumentException("BLUB");
	}

	public TestClass(){
		if (myNowRenamedBoolean && stringList.size() > 5)
			number = 42;
		else
			number = 44;
		
		LinkedList<String> l = new LinkedList<>();
		l.add(s);
		stringList.add(l);

		myOtherStringList = new ArrayList<String>();

		//execute a silly sysout in another thread!
		Runnable r = new Runnable(){
		    @Override
		    public void run() {
		        int i = 0;
		        while(i<5) myOtherStringList.add(i+", lalala!");
		    }
		};
		Thread t = new Thread(r);
		t.run();
	}

	public TestClass(List<String> l, int i){
		stringList.add(l);
		myOtherStringList = null;
		number = i;
	}

	public String getTheStringThatCanBeChanged(){
		return s;
	}

	protected void setTheStaticBoolean(boolean b){
		myNowRenamedBoolean = b;
	}
}
