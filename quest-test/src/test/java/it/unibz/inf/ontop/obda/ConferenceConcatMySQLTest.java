package it.unibz.inf.ontop.obda;

/*
 * #%L
 * ontop-test
 * %%
 * Copyright (C) 2009 - 2014 Free University of Bozen-Bolzano
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import it.unibz.inf.ontop.owlrefplatform.owlapi.QuestOWLResultSet;
import it.unibz.inf.ontop.owlrefplatform.owlapi.QuestOWLStatement;
import it.unibz.inf.ontop.quest.AbstractVirtualModeTest;
import org.semanticweb.owlapi.model.OWLObject;

/**
 * Test
 * CONCAT with table.columnName and string values that need to be change to literal
 * use mysql.
 * Refer to {@link Mapping2DatalogConverter} {@link ProjectionVisitor}
 */

public class ConferenceConcatMySQLTest extends AbstractVirtualModeTest {

    static final String owlFile = "src/test/resources/conference/ontology3.owl";
    static final String obdaFile = "src/test/resources/conference/secondmapping-test.obda";

	public ConferenceConcatMySQLTest() {
		super(owlFile, obdaFile);
	}

	private void runTests(String query1) throws Exception {

		QuestOWLStatement st = conn.createStatement();


		try {
			executeQueryAssertResults(query1, st);
			
		} catch (Exception e) {
            st.close();
            e.printStackTrace();
            assertTrue(false);


		} finally {

			conn.close();
			reasoner.dispose();
		}
	}
	
	private void executeQueryAssertResults(String query, QuestOWLStatement st) throws Exception {
		QuestOWLResultSet rs = st.executeTuple(query);

		OWLObject answer, answer2;
		rs.nextRow();



		answer= rs.getOWLObject("x");
		System.out.print("x =" + answer);
		System.out.print(" ");
		answer2= rs.getOWLObject("y");

		System.out.print("y =" + answer2);
		System.out.print(" ");


		rs.close();
		assertEquals("<http://myproject.org/odbs#tracepaper1>", answer.toString());
		assertEquals("<http://myproject.org/odbs#eventpaper1>", answer2.toString());
	}

	public void testConcat() throws Exception {

        String query1 = "PREFIX : <http://myproject.org/odbs#> SELECT ?x ?y\n" +
                "WHERE {\n" +
                "   ?x :TcontainsE ?y\n" +
				"}";

		runTests(query1);
	}


}
