package org.javarosa.core.model;

import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.javarosa.test.utils.FormParserHelper.parse;
import static org.javarosa.test.utils.ResourcePathHelper.r;
import static org.junit.Assert.assertThat;

public class Safe2014DagImplTest {

    @Test
    public void deleteRepeatGroup() throws Exception {
        final FormDef formDef =
                parse(r("org/javarosa/core/model/repeat-group-with-position-calculation.xml")).formDef;
        final FormEntryModel formEntryModel = new FormEntryModel(formDef);
        final FormEntryController formEntryController = new FormEntryController(formEntryModel);

        assertThat(formEntryModel.getEvent(), equalTo(FormEntryController.EVENT_BEGINNING_OF_FORM));

        formEntryController.stepToNextEvent();
        assertThat(formEntryModel.getEvent(), equalTo(FormEntryController.EVENT_PROMPT_NEW_REPEAT));

        formEntryController.newRepeat();
        formEntryController.stepToNextEvent();
        formEntryController.stepToNextEvent();
    }

}