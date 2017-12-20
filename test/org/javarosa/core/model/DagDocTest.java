package org.javarosa.core.model;

import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.junit.Test;

import java.io.IOException;

import static org.javarosa.test.utils.ResourcePathHelper.r;
import static org.javarosa.xform.parse.FormParserHelper.parse;

public class DagDocTest {


    @Test
    public void docTest() throws IOException {
        // Given
        final FormDef formDef =
                parse(r("dag-doc.xml")).formDef;

        formDef.initialize(false, new InstanceInitializationFactory()); // trigger all calculations

        System.out.println("Stub");
    }

    @Test
    public void docCycles() throws IOException {
        // Given
        final FormDef formDef =
                parse(r("dag-cycles.xml")).formDef;

        formDef.initialize(false, new InstanceInitializationFactory()); // trigger all calculations

        System.out.println("Stub");
    }


    @Test
    public void docInit() throws IOException {
        // Given
        final FormDef formDef =
                parse(r("dag-init.xml")).formDef;

        formDef.initialize(false, new InstanceInitializationFactory()); // trigger all calculations

        System.out.println("Stub");
    }

    @Test
    public void docInit2() throws IOException {
        // Given
        final FormDef formDef =
                parse(r("dag-init2.xml")).formDef;

        formDef.initialize(false, new InstanceInitializationFactory()); // trigger all calculations

        System.out.println("Stub");
    }
}
