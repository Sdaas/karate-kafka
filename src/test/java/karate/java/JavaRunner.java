package karate.java;

import com.intuit.karate.junit5.Karate;

class JavaRunner {

    @Karate.Test
    Karate testAll() {
        return Karate.run().relativeTo(getClass());
    }

}