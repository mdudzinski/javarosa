package org.javarosa.core.model.actions;

import org.javarosa.core.model.CoreModelModule;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.data.DateTimeData;
import org.javarosa.core.model.data.DecimalData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.util.JavaRosaCoreModule;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.model.xform.XFormsModule;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import static org.javarosa.core.util.externalizable.ExtUtil.defaultPrototypes;
import static org.javarosa.test.utils.ResourcePathHelper.r;
import static org.javarosa.xform.parse.FormParserHelper.parse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NestedSetValueActionTest {

    private static final long DATE_NOW = 1_500_000_000_000L;
    private static final long DAY_OFFSET = 86_400_000L;

    @Before
    public void before() {
        DateTimeUtils.setCurrentMillisFixed(DATE_NOW);
    }

    @After
    public void after() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void when_triggerNodeIsUpdated_targetNodeCalculation_isEvaluated() throws IOException {
        // Given
        final FormDef formDef =
                parse(r("nested-setvalue-action.xml")).formDef;

        TreeReference triggerRef = new TreeReference();
        triggerRef.setRefLevel(TreeReference.REF_ABSOLUTE);
        triggerRef.add("data", 0);
        triggerRef.add("cost", 0);

        TreeReference targetRef = new TreeReference();
        targetRef.setRefLevel(TreeReference.REF_ABSOLUTE);
        targetRef.add("data", 0);
        targetRef.add("cost_timestamp", 0);

        IAnswerData targetValue = formDef.getMainInstance().resolveReference(targetRef).getValue();
        assertNull(targetValue);

        // When
        formDef.setValue(new DecimalData(22.0), triggerRef, true);

        // Then
        targetValue = formDef.getMainInstance().resolveReference(targetRef).getValue();
        IAnswerData expectedValue = new DateTimeData(new Date(DATE_NOW));

        assertEquals(expectedValue.getValue(), targetValue.getValue());
    }

    @Test
    public void when_triggerNodeIsUpdatedWithinRepeat_targetNodeCalculation_isEvaluated() throws IOException {
        // Given
        final FormDef formDef =
                parse(r("nested-setvalue-action-with-repeats.xml")).formDef;

        TreeReference[] triggerRefs = new TreeReference[3];
        TreeReference[] targetRefs = new TreeReference[3];

        for (int i = 0; i < 3; i++) {
            TreeReference triggerRef = new TreeReference();
            triggerRef.setRefLevel(TreeReference.REF_ABSOLUTE);
            triggerRef.add("data", 0);
            triggerRef.add("repeat", i);
            triggerRef.add("cost", 0);

            TreeReference targetRef = new TreeReference();
            targetRef.setRefLevel(TreeReference.REF_ABSOLUTE);
            targetRef.add("data", 0);
            targetRef.add("repeat", i);
            targetRef.add("cost_timestamp", 0);

            triggerRefs[i] = triggerRef;
            targetRefs[i] = targetRef;
        }

        // When
        for (int i = 0; i < 3; i++) {
            DateTimeUtils.setCurrentMillisFixed(DATE_NOW + i * DAY_OFFSET);
            formDef.setValue(new DecimalData(i+1), triggerRefs[i], true);
        }

        // Then
        for (int i = 0; i < 3; i++) {
            IAnswerData targetValue = formDef.getMainInstance().resolveReference(targetRefs[i]).getValue();
            IAnswerData expectedValue = new DateTimeData(new Date(DATE_NOW + i * DAY_OFFSET));
            assertEquals(expectedValue.getValue(), targetValue.getValue());
        }
    }

    @Test
    public void when_triggerNodeIsUpdatedWithTheSameValue_targetNodeCalculation_isNotEvaluated() throws IOException {
        // Given
        final FormDef formDef =
                parse(r("nested-setvalue-action.xml")).formDef;

        TreeReference triggerRef = new TreeReference();
        triggerRef.setRefLevel(TreeReference.REF_ABSOLUTE);
        triggerRef.add("data", 0);
        triggerRef.add("cost", 0);

        TreeReference targetRef = new TreeReference();
        targetRef.setRefLevel(TreeReference.REF_ABSOLUTE);
        targetRef.add("data", 0);
        targetRef.add("cost_timestamp", 0);

        DecimalData decimalValue = new DecimalData(22.0);
        formDef.getMainInstance().resolveReference(targetRef).setValue(new DateTimeData(new Date(DATE_NOW)));
        formDef.getMainInstance().resolveReference(triggerRef).setValue(decimalValue);

        // When
        DateTimeUtils.setCurrentMillisFixed(DATE_NOW + DAY_OFFSET); // shift the current time so we can test whether the setvalue action was re-fired
        formDef.setValue(decimalValue, triggerRef, true);

        // Then
        IAnswerData targetValue = formDef.getMainInstance().resolveReference(targetRef).getValue();
        IAnswerData expectedValue = new DateTimeData(new Date(DATE_NOW));

        assertEquals(expectedValue.getValue(), targetValue.getValue());
    }

    @Test
    public void when_thereAreDependencyCyclesBetweenSetValueActionExpressions_returnError() throws IOException {
        // Given
        final FormDef formDef =
        parse(r("nested-setvalue-action-dependency-cycles.xml")).formDef;

        TreeReference triggerRef = new TreeReference();
        triggerRef.setRefLevel(TreeReference.REF_ABSOLUTE);
        triggerRef.add("data", 0);
        triggerRef.add("cost", 0);

        // When
        DecimalData decimalValue = new DecimalData(6.0);
        formDef.setValue(decimalValue, triggerRef, true);

        // Then
        TreeReference targetRef = new TreeReference();
        targetRef.setRefLevel(TreeReference.REF_ABSOLUTE);
        targetRef.add("data", 0);
        targetRef.add("cost2", 0);
        IAnswerData targetValue = formDef.getMainInstance().resolveReference(targetRef).getValue();
        IAnswerData expectedValue = new DecimalData(36.0);

        assertEquals(expectedValue.getValue(), targetValue.getValue());
    }

    @Test
    public void when_thereAreDependencyCyclesBetweenExpressionsInSetValueActionsAndInstance_returnError() throws IOException {
        // Given
        final FormDef formDef =
                parse(r("nested-setvalue-action-dependency-cycles.xml")).formDef;

        TreeReference triggerRef = new TreeReference();
        triggerRef.setRefLevel(TreeReference.REF_ABSOLUTE);
        triggerRef.add("data", 0);
        triggerRef.add("cost", 0);

        // When
        DecimalData decimalValue = new DecimalData(6.0);
        formDef.setValue(decimalValue, triggerRef, true);

        // Then
        TreeReference targetRef = new TreeReference();
        targetRef.setRefLevel(TreeReference.REF_ABSOLUTE);
        targetRef.add("data", 0);
        targetRef.add("cost2", 0);
        IAnswerData targetValue = formDef.getMainInstance().resolveReference(targetRef).getValue();
        IAnswerData expectedValue = new DecimalData(36.0);

        assertEquals(expectedValue.getValue(), targetValue.getValue());
    }

    @Test
    public void testSerializationAndDeserialization() throws IOException, DeserializationException {
        PrototypeManager.registerPrototypes(JavaRosaCoreModule.classNames);
        PrototypeManager.registerPrototypes(CoreModelModule.classNames);
        new XFormsModule().registerModule();

        FormDef formDef = parse(r("nested-setvalue-action.xml")).formDef;
        Path p = Files.createTempFile("serialized-form", null);

        final DataOutputStream dos = new DataOutputStream(Files.newOutputStream(p));
        formDef.writeExternal(dos);
        dos.close();

        final DataInputStream dis = new DataInputStream(Files.newInputStream(p));
        formDef.readExternal(dis, defaultPrototypes());
        dis.close();

        Files.delete(p);

        TreeReference triggerRef = new TreeReference();
        triggerRef.setRefLevel(TreeReference.REF_ABSOLUTE);
        triggerRef.add("data", 0);
        triggerRef.add("cost", 0);

        TreeReference targetRef = new TreeReference();
        targetRef.setRefLevel(TreeReference.REF_ABSOLUTE);
        targetRef.add("data", 0);
        targetRef.add("cost_timestamp", 0);

        IAnswerData targetValue = formDef.getMainInstance().resolveReference(targetRef).getValue();
        assertNull(targetValue);

        // When
        formDef.setValue(new DecimalData(22.0), triggerRef, true);

        // Then
        targetValue = formDef.getMainInstance().resolveReference(targetRef).getValue();
        IAnswerData expectedValue = new DateTimeData(new Date(DATE_NOW));

        assertEquals(expectedValue.getValue(), targetValue.getValue());
    }
}