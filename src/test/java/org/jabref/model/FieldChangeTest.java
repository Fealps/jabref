package org.jabref.model;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FieldChangeTest {

    private BibEntry entry = new BibEntry()
            .withField(StandardField.DOI, "foo");
    private BibEntry entryOther = new BibEntry();
    private FieldChange fc = new FieldChange(entry, StandardField.DOI, "foo", "bar");

    @Test
    void fieldChangeOnNullEntryNotAllowed() {
        assertThrows(NullPointerException.class, () -> new FieldChange(null, StandardField.DOI, "foo", "bar"));
    }

    @Test
    void fieldChangeOnNullFieldNotAllowed() {
        assertThrows(NullPointerException.class, () -> new FieldChange(entry, null, "foo", "bar"));
    }

    @Test
    void blankFieldChangeNotAllowed() {
        assertThrows(NullPointerException.class, () -> new FieldChange(null, null, null, null));
    }

    @Test
    void equalFieldChange() {
        FieldChange fcBlankNewValue = new FieldChange(entry, StandardField.DOI, "foo", null);
        assertNotEquals(fc, fcBlankNewValue);
    }

    @Test
    void fieldChangeDoesNotEqualString() {
        assertNotEquals(fc, "foo");
    }

    @Test
    void fieldChangeEqualsItSelf() {
        assertEquals(fc, fc);
    }

    @Test
    void differentFieldChangeIsNotEqual() {
        FieldChange fcOther = new FieldChange(entryOther, StandardField.DOI, "fooX", "barX");
        assertNotEquals(fc, fcOther);
    }


    @Test
    void overloadEqualFromObjectIsSelf(){
        Object result = fc.equals(fc);
        assertEquals(true,result);
    }

    @Test
    void overloadEqualsFromSimilarFieldChange(){
        FieldChange fcSimilar = new FieldChange(entry, StandardField.DOI, "foo", "bar");
        boolean result = fc.equals(fcSimilar);
        assertEquals(true,result);
    }

    @Test
    void overloadEqualsFieldChangeHasDifferentOldValue(){
        FieldChange fNew = new FieldChange(entry, StandardField.DOI, "bar", "bar");
        boolean result = fNew.equals(fc);
        assertEquals(false,result);
    }

    @Test
    void overloadEqualsFromFieldChangeHasDifferentField(){
        FieldChange fNew = new FieldChange(entry, StandardField.ABSTRACT, "foo", "bar");
        boolean result = fNew.equals(fc);
        assertEquals(false,result);
    }

    @Test
    void overloadEqualsFromFieldChangeHasNullNewValue(){
        FieldChange fNew = new FieldChange(entry, StandardField.DOI, "foo", "barX");
        boolean result = fNew.equals(fc);
        assertEquals(false,result);
    }
    @Test
    void overloadEqualsFromFieldChangeHasDifferentEntry(){
        FieldChange fNew = new FieldChange(entryOther, StandardField.DOI, "foo", "bar");
        boolean result = fNew.equals(fc);
        assertEquals(false,result);
    }

    @Test
    void overloadEqualsFromFieldChangeAreSimilarWithDifferentNewValues(){
        FieldChange fNew = new FieldChange(entry, StandardField.DOI, "foo", "barX");
        boolean result = fNew.equals(fc);
        assertEquals(false,result);
    }
}
