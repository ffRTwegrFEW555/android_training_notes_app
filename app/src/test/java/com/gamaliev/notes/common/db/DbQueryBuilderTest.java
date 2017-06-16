package com.gamaliev.notes.common.db;

import android.support.annotation.Nullable;

import org.junit.Test;

import static com.gamaliev.notes.common.db.DbHelper.BASE_COLUMN_ID;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_MANUALLY;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_SYNC_ID_JSON;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_TITLE;
import static com.gamaliev.notes.common.db.DbHelper.ORDER_ASCENDING;
import static com.gamaliev.notes.common.db.DbHelper.ORDER_DESCENDING;
import static com.gamaliev.notes.common.db.DbQueryBuilder.OPERATOR_BETWEEN;
import static com.gamaliev.notes.common.db.DbQueryBuilder.OPERATOR_EQUALS;
import static com.gamaliev.notes.common.db.DbQueryBuilder.OPERATOR_LIKE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
public class DbQueryBuilderTest {

    @Test
    public void addOr() throws Exception {
        final DbQueryBuilder builder = new DbQueryBuilder();
        final String operandOne = "123";
        final String operandTwo = "abc";
        String expectedSelection;
        String[] expectedArgs;

        // (_id = ?)
        expectedSelection = "(" + BASE_COLUMN_ID + OPERATOR_EQUALS + "?)";
        expectedArgs = new String[] {operandOne};
        builder.addOr(
                BASE_COLUMN_ID,
                OPERATOR_EQUALS,
                new String[] {operandOne});
        assertEquals(expectedSelection, removeWhiteSpacesFromQuery(builder.getSelectionResult()));
        assertArrayEquals(expectedArgs, builder.getSelectionArgs());

        // (_id = ?)OR(title LIKE ?)
        expectedSelection = "(" + BASE_COLUMN_ID + OPERATOR_EQUALS + "?)"
                + "OR" + "(" + LIST_ITEMS_COLUMN_TITLE + OPERATOR_LIKE + "?)";
        expectedArgs = new String[] {operandOne, '%' + operandTwo + '%'};
        builder.addOr(
                LIST_ITEMS_COLUMN_TITLE,
                OPERATOR_LIKE,
                new String[] {operandTwo});
        assertEquals(expectedSelection, removeWhiteSpacesFromQuery(builder.getSelectionResult()));
        assertArrayEquals(expectedArgs, builder.getSelectionArgs());
    }

    @Test
    public void addAnd() throws Exception {
        final DbQueryBuilder builder = new DbQueryBuilder();
        final String operandOne = "123";
        final String operandTwo = "abc";
        String expectedSelection;
        String[] expectedArgs;

        // (_id = ?)
        expectedSelection = "(" + BASE_COLUMN_ID + OPERATOR_EQUALS + "?)";
        expectedArgs = new String[] {operandOne};
        builder.addAnd(
                BASE_COLUMN_ID,
                OPERATOR_EQUALS,
                new String[] {operandOne});
        assertEquals(expectedSelection, removeWhiteSpacesFromQuery(builder.getSelectionResult()));
        assertArrayEquals(expectedArgs, builder.getSelectionArgs());

        // (_id = ?)OR(title LIKE ?)
        expectedSelection = "(" + BASE_COLUMN_ID + OPERATOR_EQUALS + "?)"
                + "AND" + "(" + LIST_ITEMS_COLUMN_TITLE + OPERATOR_LIKE + "?)";
        expectedArgs = new String[] {operandOne, '%' + operandTwo + '%'};
        builder.addAnd(
                LIST_ITEMS_COLUMN_TITLE,
                OPERATOR_LIKE,
                new String[] {operandTwo});
        assertEquals(expectedSelection, removeWhiteSpacesFromQuery(builder.getSelectionResult()));
        assertArrayEquals(expectedArgs, builder.getSelectionArgs());
    }

    @Test
    public void addAndInner() throws Exception {
        final String operandOne = "123";
        final String operandTwo = "abc";
        final String operandThree = "777";

        // (_id = ?)AND(title LIKE ?)AND((_id = ?)OR(id BETWEEN ? AND ?))
        final String expectedSelection =
                "(" + BASE_COLUMN_ID + OPERATOR_EQUALS + "?)"
                + "AND(" + LIST_ITEMS_COLUMN_TITLE + OPERATOR_LIKE + "?)"
                + "AND((" + BASE_COLUMN_ID + OPERATOR_EQUALS + "?)"
                + "OR(" + LIST_ITEMS_COLUMN_SYNC_ID_JSON + OPERATOR_BETWEEN + "? AND ?))";
        final String[] expectedArgs = new String[] {
                operandOne,
                '%' + operandTwo + '%',
                operandOne,
                operandOne,
                operandThree};

        // (_id = ?)AND(title LIKE ?)
        final DbQueryBuilder builder = new DbQueryBuilder();
        builder .addAnd(
                    BASE_COLUMN_ID,
                    OPERATOR_EQUALS,
                    new String[] {operandOne})
                .addAnd(
                    LIST_ITEMS_COLUMN_TITLE,
                    OPERATOR_LIKE,
                    new String[] {operandTwo});

        // (_id = ?)OR(id BETWEEN ? AND ?)
        final DbQueryBuilder innerBuilder = new DbQueryBuilder();
        innerBuilder
                .addOr(
                    BASE_COLUMN_ID,
                    OPERATOR_EQUALS,
                    new String[] {operandOne})
                .addOr(
                    LIST_ITEMS_COLUMN_SYNC_ID_JSON,
                    OPERATOR_BETWEEN,
                    new String[] {operandOne, operandThree});

        // (_id = ?)AND(title LIKE ?)AND((_id = ?)OR(id BETWEEN ? AND ?))
        builder.addAndInner(innerBuilder);

        assertEquals(expectedSelection, removeWhiteSpacesFromQuery(builder.getSelectionResult()));
        assertArrayEquals(expectedArgs, builder.getSelectionArgs());
    }

    @Test
    public void setOrder() throws Exception {
        final DbQueryBuilder builder = new DbQueryBuilder();

        builder.setOrder(LIST_ITEMS_COLUMN_MANUALLY);
        builder.setAscDesc(ORDER_ASCENDING);
        assertEquals(
                LIST_ITEMS_COLUMN_MANUALLY + " " + ORDER_ASCENDING,
                builder.getSortOrder());

        builder.setOrder(LIST_ITEMS_COLUMN_TITLE);
        builder.setAscDesc(ORDER_DESCENDING);
        assertEquals(
                LIST_ITEMS_COLUMN_TITLE + " " + ORDER_DESCENDING,
                builder.getSortOrder());
    }

    @Test
    public void setAscDesc() throws Exception {
        final DbQueryBuilder builder = new DbQueryBuilder();

        builder.setOrder(LIST_ITEMS_COLUMN_MANUALLY);
        builder.setAscDesc(ORDER_ASCENDING);
        assertEquals(
                LIST_ITEMS_COLUMN_MANUALLY + " " + ORDER_ASCENDING,
                builder.getSortOrder());

        builder.setOrder(LIST_ITEMS_COLUMN_TITLE);
        builder.setAscDesc(ORDER_DESCENDING);
        assertEquals(
                LIST_ITEMS_COLUMN_TITLE + " " + ORDER_DESCENDING,
                builder.getSortOrder());
    }

    @Test
    public void getSelectionResult() throws Exception {
        final DbQueryBuilder builder = new DbQueryBuilder();
        final String operandOne = "123";
        final String operandTwo = "abc";
        String expectedSelection;
        String[] expectedArgs;

        // (_id = ?)
        expectedSelection = "(" + BASE_COLUMN_ID + OPERATOR_EQUALS + "?)";
        expectedArgs = new String[] {operandOne};
        builder.addOr(
                BASE_COLUMN_ID,
                OPERATOR_EQUALS,
                new String[] {operandOne});
        assertEquals(expectedSelection, removeWhiteSpacesFromQuery(builder.getSelectionResult()));
        assertArrayEquals(expectedArgs, builder.getSelectionArgs());

        // (_id = ?)OR(title LIKE ?)
        expectedSelection = "(" + BASE_COLUMN_ID + OPERATOR_EQUALS + "?)"
                + "OR" + "(" + LIST_ITEMS_COLUMN_TITLE + OPERATOR_LIKE + "?)";
        expectedArgs = new String[] {operandOne, '%' + operandTwo + '%'};
        builder.addOr(
                LIST_ITEMS_COLUMN_TITLE,
                OPERATOR_LIKE,
                new String[] {operandTwo});
        assertEquals(expectedSelection, removeWhiteSpacesFromQuery(builder.getSelectionResult()));
        assertArrayEquals(expectedArgs, builder.getSelectionArgs());
    }

    @Test
    public void getSelectionArgs() throws Exception {
        final DbQueryBuilder builder = new DbQueryBuilder();
        final String operandOne = "123";
        final String operandTwo = "abc";
        String expectedSelection;
        String[] expectedArgs;

        // (_id = ?)
        expectedSelection = "(" + BASE_COLUMN_ID + OPERATOR_EQUALS + "?)";
        expectedArgs = new String[] {operandOne};
        builder.addOr(
                BASE_COLUMN_ID,
                OPERATOR_EQUALS,
                new String[] {operandOne});
        assertEquals(expectedSelection, removeWhiteSpacesFromQuery(builder.getSelectionResult()));
        assertArrayEquals(expectedArgs, builder.getSelectionArgs());

        // (_id = ?)OR(title LIKE ?)
        expectedSelection = "(" + BASE_COLUMN_ID + OPERATOR_EQUALS + "?)"
                + "OR" + "(" + LIST_ITEMS_COLUMN_TITLE + OPERATOR_LIKE + "?)";
        expectedArgs = new String[] {operandOne, '%' + operandTwo + '%'};
        builder.addOr(
                LIST_ITEMS_COLUMN_TITLE,
                OPERATOR_LIKE,
                new String[] {operandTwo});
        assertEquals(expectedSelection, removeWhiteSpacesFromQuery(builder.getSelectionResult()));
        assertArrayEquals(expectedArgs, builder.getSelectionArgs());
    }

    @Test
    public void getSortOrder() throws Exception {
        final DbQueryBuilder builder = new DbQueryBuilder();

        builder.setOrder(LIST_ITEMS_COLUMN_MANUALLY);
        builder.setAscDesc(ORDER_ASCENDING);
        assertEquals(
                LIST_ITEMS_COLUMN_MANUALLY + " " + ORDER_ASCENDING,
                builder.getSortOrder());

        builder.setOrder(LIST_ITEMS_COLUMN_TITLE);
        builder.setAscDesc(ORDER_DESCENDING);
        assertEquals(
                LIST_ITEMS_COLUMN_TITLE + " " + ORDER_DESCENDING,
                builder.getSortOrder());
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongOperatorArguments() throws Exception {
        new DbQueryBuilder().addAnd(
                BASE_COLUMN_ID,
                "UnknownOperator",
                new String[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongEqualsArguments() throws Exception {
        new DbQueryBuilder().addOr(
                BASE_COLUMN_ID,
                OPERATOR_EQUALS,
                new String[] {"123", "abc"}); /* Must be 1 operand */
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongLikeArguments() throws Exception {
        new DbQueryBuilder().addOr(
                BASE_COLUMN_ID,
                OPERATOR_LIKE,
                new String[] {"123", "abc"}); /* Must be 1 operand */
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongBetweenArguments() throws Exception {
        new DbQueryBuilder().addOr(
                BASE_COLUMN_ID,
                OPERATOR_BETWEEN,
                new String[] {"123"}); /* Must be 2 operands */
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongOrderArguments() throws Exception {
        new DbQueryBuilder().setOrder("UnknownOrder");
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongAscDescArguments() throws Exception {
        new DbQueryBuilder().setAscDesc("UnknownOrder");
    }


    /*
        Utils
     */

    @Nullable
    private static String removeWhiteSpacesFromQuery(@Nullable final String string) {
        return string == null
                ? null
                : string
                    .replaceAll("\\s\\s+", " ")
                    .replaceAll("\\s\\(", "(")
                    .replaceAll("\\s\\)", ")")
                    .replaceAll("\\(\\s", "(")
                    .replaceAll("\\)\\s", ")");
    }
}