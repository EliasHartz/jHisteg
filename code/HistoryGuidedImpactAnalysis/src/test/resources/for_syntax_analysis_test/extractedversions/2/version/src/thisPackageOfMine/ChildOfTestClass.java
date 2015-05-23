package thisPackageOfMine;

public class ChildOfTestClass extends TestClass {

	@Override
	public String getS(){
		return "child_"+s;
	}

}
