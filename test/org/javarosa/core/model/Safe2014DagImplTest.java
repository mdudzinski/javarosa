package org.javarosa.core.model;

import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.debug.Event;
import org.javarosa.debug.EventNotifier;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.javarosa.test.utils.FormParserHelper.parse;
import static org.javarosa.test.utils.ResourcePathHelper.r;
import static org.junit.Assert.assertThat;

public class Safe2014DagImplTest {

    private final List<Event> dagEvents = new ArrayList<>();

    private final EventNotifier eventNotifier = new EventNotifier() {

        @Override
        public void publishEvent(Event event) {
            dagEvents.add(event);
        }
    };

    @Test
    public void deleteRepeatGroup_evaluatesTriggerables_dependedOnAllRepeatGroupSiblings() throws Exception {
        // Given
        final FormDef formDef =
                parse(r("org/javarosa/core/model/repeat-group-with-position-calculation.xml")).formDef;

        formDef.setEventNotifier(eventNotifier);
        IDag dag = getIDagImplUnderTest(formDef);
        assertThat(dag, instanceOf(Safe2014DagImpl.class));

        final FormInstance mainInstance = formDef.getMainInstance();
        final TreeElement parentElement = mainInstance.getRoot();

        /*
           We test a scenario when a second repeat group is deleted. There is no actual deletion that would've been
           normally triggered by FormDef#deleteRepeat(FormIndex). Instead, a form instance with 4 repeat groups is used
           which simulates situation when there were 5 repeats and the second one was deleted.
           The first repeat has already a populated value  - '1' - which normally would've been calculated by DAG.
           Rest of the repeat groups in the main instance have no values so we can test that those values are actually
           calculated after dag.deleteRepeatGroup is called.

           At position 0 there is a template TreeElement (multiplicity == -2)
           At position 1 is a repeat group with an already populated populated
           At positions 2 - 4 there are repeats with values supposed to be populated after deleteRepeatGroup is called
        */
        final TreeElement deletedElement = mainInstance.getRoot().getChildAt(2);
        final TreeReference dummyDeletedRef = null;

        // When
        dag.deleteRepeatGroup(mainInstance, formDef.getEvaluationContext(), dummyDeletedRef, parentElement, deletedElement);

        // Then
        final List<TreeElement> repeats = mainInstance.getRoot().getChildrenWithName("houseM");

        // check the values based on the position of the parents
        assertThat(repeats.get(0).getChildAt(0).getValue().getDisplayText(), equalTo("1"));
        assertThat(repeats.get(1).getChildAt(0).getValue().getDisplayText(), equalTo("2"));
        assertThat(repeats.get(2).getChildAt(0).getValue().getDisplayText(), equalTo("3"));
        assertThat(repeats.get(3).getChildAt(0).getValue().getDisplayText(), equalTo("4"));

        // check that calculations have not been triggered for the repeat group prior to the deleted one
        assertThat(dagEvents.get(0).getDisplayMessage(),
                equalTo("Processing 'Recalculate' for no [2_1] (2.0)"));
        assertThat(dagEvents.get(1).getDisplayMessage(),
                equalTo("Processing 'Deleted: houseM [2]: 1 triggerables were fired.' for "));
        assertThat(dagEvents.get(2).getDisplayMessage(),
                equalTo("Processing 'Deleted: no [2_1]: 1 triggerables were fired.' for "));
        assertThat(dagEvents.get(3).getDisplayMessage(),
                equalTo("Processing 'Recalculate' for no [3_1] (3.0)"));
        assertThat(dagEvents.get(4).getDisplayMessage(),
                equalTo("Processing 'Deleted: houseM [3]: 1 triggerables were fired.' for "));
        assertThat(dagEvents.get(5).getDisplayMessage(),
                equalTo("Processing 'Deleted: no [3_1]: 1 triggerables were fired.' for "));
        assertThat(dagEvents.get(6).getDisplayMessage(),
                equalTo("Processing 'Recalculate' for no [4_1] (4.0)"));
        assertThat(dagEvents.get(7).getDisplayMessage(),
                equalTo("Processing 'Deleted: houseM [4]: 1 triggerables were fired.' for "));
        assertThat(dagEvents.get(8).getDisplayMessage(),
                equalTo("Processing 'Deleted: no [4_1]: 1 triggerables were fired.' for "));
    }

    /**
     * Constructing {@link Safe2014DagImpl} is cumbersome so we'll use the one constructed
     * by {@link org.javarosa.xform.parse.XFormParser} and attached to {@code formDef}.
     * The field is private in {@link FormDef} so reflection must be used.
     */
    private IDag getIDagImplUnderTest(FormDef formDef) throws NoSuchFieldException, IllegalAccessException {
        Field dagImplFromFormDef = FormDef.class.getDeclaredField("dagImpl");
        dagImplFromFormDef.setAccessible(true);
        return (Safe2014DagImpl) dagImplFromFormDef.get(formDef);
    }

}