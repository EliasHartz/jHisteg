package thisPackageOfMine;

public class ChildOfTestClass extends TestClass {

	@Override
	public String getS(){
		return "child_"+s;
	}

	public class MyInnerClass{
		public final String desc = "This is okay";
	}
}

//this one is not detected!
class MyOtherClass{
  public final String desc = "This is just terrible programming style!";
}
