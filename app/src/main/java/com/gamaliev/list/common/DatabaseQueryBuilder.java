package com.gamaliev.list.common;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Helper for create select query, associated with
 * {@link android.database.sqlite.SQLiteDatabase
 * #query(String, String[], String, String[], String, String, String)}}
 *
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
public final class DatabaseQueryBuilder {

    /* Logger */
    private static final String TAG = DatabaseQueryBuilder.class.getSimpleName();

    /* SQL */
    public static final String OPERATOR_OR              = " OR ";
    public static final String OPERATOR_AND             = " AND ";
    public static final String OPERATOR_EQUALS          = " = ";
    public static final String OPERATOR_BETWEEN         = " BETWEEN ";
    public static final String OPERATOR_LIKE            = " LIKE ";

    private static final String SYMBOL_MASK             = " ? ";
    private static final String SYMBOL_PERCENT          = "%";

    @Nullable private String[] selection;
    @Nullable private String[] selectionArgs;

    @NonNull private String order   = DatabaseHelper.BASE_COLUMN_ID;
    @NonNull private String ascDesc = DatabaseHelper.ORDER_ASCENDING;

    /* */
    @NonNull private final Context context;


    /*
        Init
     */

    public DatabaseQueryBuilder(@NonNull final Context context) {
        this.context = context;
    }


    /*
        Methods
     */

    /**
     * Add clause with 'OR' operator.<br>
     * See also:<br>
     *     {@link #OPERATOR_EQUALS}<br>
     *     {@link #OPERATOR_LIKE}<br>
     *     {@link #OPERATOR_BETWEEN}<br>
     *
     * @param column    Column.
     * @param operator  Operator. See 'OPERATOR_*' constants.
     * @param operands  Operands.
     * @return A reference to this object.
     */
    @NonNull
    public DatabaseQueryBuilder addOr(
            @NonNull final String column,
            @NonNull final String operator,
            @NonNull final String[] operands) {

        return add(
                selection == null ? null : OPERATOR_OR,
                column,
                operator,
                operands);
    }

    /**
     * Add clause with 'ADD' operator.<br>
     * See also:<br>
     *     {@link #OPERATOR_EQUALS}<br>
     *     {@link #OPERATOR_LIKE}<br>
     *     {@link #OPERATOR_BETWEEN}<br>
     *
     * @param column    Column.
     * @param operator  Operator. See 'OPERATOR_*' constants.
     * @param operands  Operands.
     * @return A reference to this object.
     */
    @NonNull
    public DatabaseQueryBuilder addAnd(
            @NonNull final String column,
            @NonNull final String operator,
            @NonNull final String[] operands) {

        return add(
                selection == null ? null : OPERATOR_AND,
                column,
                operator,
                operands);
    }

    /**
     * Add selection and selectionArgs from given query builder, to current query builder.<br>
     * Add with 'OR' operator.
     *
     * @param queryBuilder Query builder, whose selection clauses will be added.
     * @return A reference to this object.
     */
    @NonNull
    public DatabaseQueryBuilder addOrInner(@NonNull final DatabaseQueryBuilder queryBuilder) {
        addInner(queryBuilder, OPERATOR_OR);
        return this;
    }

    /**
     * Add selection and selectionArgs from given query builder, to current query builder.<br>
     * Add with 'AND' operator.
     *
     * @param queryBuilder Query builder, whose selection clauses will be added.
     * @return A reference to this object.
     */
    @NonNull
    public DatabaseQueryBuilder addAndInner(@NonNull final DatabaseQueryBuilder queryBuilder) {
        addInner(queryBuilder, OPERATOR_AND);
        return this;
    }

    /**
     * Helper for {@link #addOr(String, String, String[])} and {@link #addAnd(String, String, String[])}.
     * @param queryBuilder  Query builder, whose selection clauses will be added.
     * @param operator      Operator, 'OR' or 'AND'.
     */
    private void addInner(
            @NonNull final DatabaseQueryBuilder queryBuilder,
            @NonNull final String operator) {

        // Create inner clause.
        StringBuilder sb = new StringBuilder();

        // If clause is first in current query builder, then add without operator 'OR' or 'ADD'.
        if(selection != null) {
            sb.append(operator);
        }

        // Example:
        // if first in current query builder, then  "   (created BETWEEN '?' AND '?') ",
        // else                                     "OR (created BETWEEN '?' AND '?') "
        sb      .append(" (")
                .append(queryBuilder.getSelectionResult())
                .append(") ");

        // Add new selection clause to old.
        updateSelectionClauses(
                sb.toString(),
                queryBuilder.getSelectionArgs());
    }

    /**
     * Basic logic functional for added clauses.<br>
     * Add one clause.
     *
     * @param operatorPrimary   {@link #OPERATOR_OR}, {@link #OPERATOR_AND}.
     * @param column            Column.
     * @param operatorSecondary {@link #OPERATOR_EQUALS}, {@link #OPERATOR_BETWEEN}, {@link #OPERATOR_LIKE}.
     * @param operands          Operands.
     * @return A reference to this object.
     */
    @NonNull
    private DatabaseQueryBuilder add(
            @Nullable final String operatorPrimary,
            @NonNull final String column,
            @NonNull final String operatorSecondary,
            @NonNull final String[] operands) {

        // Check supported operations.
        if (!(OPERATOR_BETWEEN.equals(operatorSecondary)
                || OPERATOR_LIKE.equals(operatorSecondary)
                || OPERATOR_EQUALS.equals(operatorSecondary))) {
            throw new UnsupportedOperationException(
                    "Supported only 'BETWEEN', 'LIKE' and '=' operators");
        }

        /*
            Example: "OR (title LIKE '?') "
            Example: "OR (color = ?) "
            Example: "OR (created BETWEEN '?' AND '?') "

            Example: "OR (title LIKE '%Droid%') "
            Example: "OR (color = 12345) "
            Example: "OR (created BETWEEN '2017-04-25T21:25:35+05:00' AND '2017-04-25T21:25:35+05:00') "
        */

        // Example: 'OR (title' or '(title'
        StringBuilder sb = new StringBuilder();

        // If clause is first in current query builder, then add without operator 'OR' or 'ADD'.
        if (operatorPrimary != null) {
            sb.append(operatorPrimary);
        }

        sb      .append("(")
                .append(column);

        switch (operatorSecondary) {

            // Example: " LIKE '?'"
            // Example: " LIKE '%Droid%'"
            case OPERATOR_LIKE:

                // Check exist one operand.
                if (operands.length != 1) {
                    throw new IllegalArgumentException(
                            "Operator 'LIKE' must operate with 1 operand.");
                } else {
                    sb      .append(OPERATOR_LIKE)
                            .append(SYMBOL_MASK);
                }
                operands[0] = SYMBOL_PERCENT
                                + operands[0]
                                + SYMBOL_PERCENT;
                break;

            // Example: " = ?"
            // Example: " = 12345"
            case OPERATOR_EQUALS:

                // Check exist one operand.
                if (operands.length != 1) {
                    throw new IllegalArgumentException(
                            "Operator '=' must operate with 1 operand.");
                } else {
                    sb      .append(OPERATOR_EQUALS)
                            .append(SYMBOL_MASK);
                }
                break;

            // Example: " BETWEEN '?' AND '?'"
            // Example: " BETWEEN '2017-04-25T21:25:35+05:00' AND '2017-04-25T21:25:35+05:00'"
            case OPERATOR_BETWEEN:

                // Check exist two operands.
                if (operands.length != 2) {
                    throw new IllegalArgumentException(
                            "Operator 'BETWEEN' must operate with two operands.");
                } else {
                    sb      .append(OPERATOR_BETWEEN)
                            .append(SYMBOL_MASK)
                            .append(OPERATOR_AND)
                            .append(SYMBOL_MASK);
                }
                break;

            //
            default:
                break;
        }

        // Example: ') '
        sb.append(") ");

        // Add new selection clause to old.
        updateSelectionClauses(sb.toString(), operands);

        return this;
    }

    /**
     * Update selection clauses.<br>
     * See: {@link #selection}, {@link #selectionArgs}.
     *
     * @param newSelectionString    New selection clause with mask.
     * @param newOperands           New operands.
     */
    private void updateSelectionClauses(
            @NonNull final String newSelectionString,
            @NonNull final String[] newOperands) {

        String[] newSelection       = new String[] {newSelectionString};
        String[] newSelectionArgs   = newOperands;

        // Update if clauses exists.
        if (selection != null && selectionArgs != null) {

            // Create new selection, and selectionArgs clauses.
            newSelection        = new String[selection.length       + 1];
            newSelectionArgs    = new String[selectionArgs.length   + newOperands.length];

            // Copy old array to new array.
            System.arraycopy(selection,     0, newSelection,        0, selection.length);
            System.arraycopy(selectionArgs, 0, newSelectionArgs,    0, selectionArgs.length);

            // Add new data to new array.
            newSelection[newSelection.length - 1] = newSelectionString;
            System.arraycopy(
                    newOperands,
                    0,
                    newSelectionArgs,
                    newSelectionArgs.length - newOperands.length,
                    newOperands.length);
        }

        // Replacing old clauses with new.
        selection = newSelection;
        selectionArgs = newSelectionArgs;
    }


    /*
        Setters
     */

    /**
     * Set sort order of query.<br>
     *     See also: <br>
     *     {@link com.gamaliev.list.common.DatabaseHelper#BASE_COLUMN_ID}<br>
     *     {@link com.gamaliev.list.common.DatabaseHelper#LIST_ITEMS_COLUMN_TITLE}<br>
     *     {@link com.gamaliev.list.common.DatabaseHelper#LIST_ITEMS_COLUMN_DESCRIPTION}<br>
     *     {@link com.gamaliev.list.common.DatabaseHelper#LIST_ITEMS_COLUMN_COLOR}<br>
     *     {@link com.gamaliev.list.common.DatabaseHelper#LIST_ITEMS_COLUMN_CREATED}<br>
     *     {@link com.gamaliev.list.common.DatabaseHelper#LIST_ITEMS_COLUMN_EDITED}<br>
     *     {@link com.gamaliev.list.common.DatabaseHelper#LIST_ITEMS_COLUMN_VIEWED}<br>
     *
     * @param order Column.
     */
    public void setOrder(@NonNull String order) {
        this.order = order;
    }

    /**
     * Set ascending / descending of sorting order.
     * @param ascDesc <br>
     *     {@link com.gamaliev.list.common.DatabaseHelper#ORDER_ASCENDING}<br>
     *     {@link com.gamaliev.list.common.DatabaseHelper#ORDER_DESCENDING}<br>
     */
    public void setAscDesc(@NonNull String ascDesc) {
        this.ascDesc = ascDesc;
    }


    /*
        Getters
     */

    /**
     * @return Selection result of query, with mask.
     */
    @Nullable
    public String getSelectionResult() {
        if (selection == null) {
            return null;
        }

        // Convert clauses array to one string clauses.
        StringBuilder sb = new StringBuilder();
        for (String cause : selection) {
            sb      .append(cause)
                    .append(" ");
        }
        return sb.toString();
    }

    /**
     * @return Selection arguments of query.
     */
    @Nullable
    public String[] getSelectionArgs() {
        return selectionArgs;
    }

    /**
     * @return Get formed sort order. {@link #order} + {@link #ascDesc}. Default value is "id ASC"
     */
    @NonNull
    public String getSortOrder() {
        return order + ascDesc;
    }
}
