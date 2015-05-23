package tests;

import mainPackage.ClassWithInnerClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InnerClassTest {

   @Test
   public void unitTest() {
      ClassWithInnerClass c = new ClassWithInnerClass();
      assertTrue(c.someString.equals("someString")); //should not be visible in the trace of SomeClass, since it is a public field access!
      assertTrue(c.innerInstance.getValue() == 42);
      assertTrue(c.somethingSomething());
	
      Object obtainedViaStaticField = ClassWithInnerClass.staticInner.giveMeAnObject();
      Object objectObtainedFromStaticInner = c.objectFromStaticInner;
      assertFalse( obtainedViaStaticField ==  objectObtainedFromStaticInner);
   }

}
