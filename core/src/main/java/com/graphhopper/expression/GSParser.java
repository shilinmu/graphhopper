package com.graphhopper.expression;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * GraphHopper script parser for scripts that are basically a collection of assignments:
 * <pre>
 * assignment_variable: value
 * </pre>
 * <p>
 * or conditional assignments:
 * <pre>
 * assignment_variable:
 *   condition ? value
 *   condition ? value
 *   value
 * </pre>
 * where a condition is one of the following:
 * <pre>
 * parameter1 function_name parameter2
 * </pre>
 * where function_name is e.g. == or >.
 * The first matching condition will be used to assign the variable.
 */
public class GSParser {

    public static List<GSAssignment> parse(Reader reader) throws IOException {
        BufferedReader bReader = new BufferedReader(reader);
        String lineAsString;
        int lineCounter = 0;
        GSAssignment currentAssignment = null;
        List<GSAssignment> assignments = new ArrayList<>();
        while ((lineAsString = bReader.readLine()) != null) {
            lineCounter++;
            int sharpIndex = lineAsString.indexOf("#");
            String statementAsString = sharpIndex > 0 ? lineAsString.substring(0, sharpIndex) : lineAsString;
            Class type = GSAssignment.class;
            if (statementAsString.startsWith(" "))
                type = GSExpression.class;

            statementAsString = statementAsString.trim();
            if (statementAsString.isEmpty())
                continue;

            if (type == GSExpression.class) {
                if (currentAssignment == null)
                    throw new GSParseException("Before expression no assignment start is found", lineAsString, lineCounter);
                GSExpression expression = GSExpression.parse(lineAsString, statementAsString, lineCounter);
                currentAssignment.add(expression);
            } else {
                int colonIndex = statementAsString.indexOf(":");
                if (colonIndex <= 0)
                    throw new GSParseException("Cannot find colon at assignment", statementAsString, lineCounter);
                currentAssignment = new GSAssignment(statementAsString.substring(0, colonIndex), lineCounter);
                assignments.add(currentAssignment);

                // expression on same line after ':'
                statementAsString = statementAsString.substring(colonIndex + 1).trim();
                if (!statementAsString.isEmpty()) {
                    GSExpression expression = GSExpression.parse(lineAsString, statementAsString, lineCounter);
                    currentAssignment.add(expression);
                }
            }
        }
        if (assignments.isEmpty())
            throw new GSParseException("Global exception: no assignments found", "", 0);
        return assignments;
    }
}
