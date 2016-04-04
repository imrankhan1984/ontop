package it.unibz.inf.ontop.owlrefplatform.core.translator;

/*
 * #%L
 * ontop-reformulation-core
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


import java.util.Optional;

import it.unibz.inf.ontop.model.*;
import it.unibz.inf.ontop.model.impl.ImmutabilityTools;
import it.unibz.inf.ontop.model.impl.MutableQueryModifiersImpl;
import it.unibz.inf.ontop.model.impl.OBDADataFactoryImpl;
import org.slf4j.LoggerFactory;
import it.unibz.inf.ontop.pivotalrepr.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static it.unibz.inf.ontop.model.impl.ImmutabilityTools.convertToMutableFunction;

/***
 * Translate a intermediate queries expression into a Datalog program that has the
 * same semantics. We use the built-int predicates Join and Left join. The rules
 * in the program have always 1 or 2 operator atoms, plus (in)equality atoms
 * (due to filters).
 * 
 * 
 * @author mrezk
 */
public class IntermediateQueryToDatalogTranslator {
	
	private final static OBDADataFactory ofac = OBDADataFactoryImpl.getInstance();
	
	//private final DatatypeFactory dtfac = OBDADataFactoryImpl.getInstance().getDatatypeFactory();

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(IntermediateQueryToDatalogTranslator.class);

	/**
	 * Translate an intermediate query tree into a Datalog program 
	 * 
	 */
	public static DatalogProgram translate(IntermediateQuery te) {
		

		

		ConstructionNode root = te.getRootConstructionNode();
		
		Optional<ImmutableQueryModifiers> optionalModifiers =  root.getOptionalModifiers();

        DatalogProgram dProgram;
		if (optionalModifiers.isPresent()){
			QueryModifiers immutableQueryModifiers = optionalModifiers.get();

			// Mutable modifiers (used by the Datalog)
			OBDAQueryModifiers mutableModifiers = new MutableQueryModifiersImpl(immutableQueryModifiers);
			// TODO: support GROUP BY (distinct QueryNode)

            dProgram = ofac.getDatalogProgram(mutableModifiers);
		}
        else {
            dProgram = ofac.getDatalogProgram();
        }
		
		
	
		Queue<ConstructionNode> rulesToDo = new LinkedList<ConstructionNode>();
		rulesToDo.add(root);

		//In rulesToDo we keep the nodes that represent sub-rules in the program, e.g. ans5 :- LeftJoin(....)
		while(!rulesToDo.isEmpty()){
			translate(te,  dProgram, rulesToDo);
		}
		
	
		
		
		return dProgram;
	}
	
	/**
	 * Translate a given IntermediateQuery query object to datalog program.
	 * 
	 *           
	 * @return Datalog program that represents the construction of the SPARQL
	 *         query.
	 */
	private static void translate(IntermediateQuery te,   DatalogProgram pr, Queue<ConstructionNode> rulesToDo  ) {
		
		ConstructionNode root = rulesToDo.poll();
		
		DataAtom head= root.getProjectionAtom();
	
		//Applying substitutions in the head.
		ImmutableFunctionalTerm substitutedHead= root.getSubstitution().applyToFunctionalTerm(head);
		List<QueryNode> listNodes=  te.getChildren(root);
		
		List<Function> atoms = new LinkedList<Function>();
		
		//Constructing the rule
		CQIE newrule = ofac.getCQIE(convertToMutableFunction(substitutedHead), atoms);
		
		pr.appendRule(newrule);
		
		//Iterating over the nodes and constructing the rule
		for (QueryNode node: listNodes){
			
			List<Function> uAtoms= getAtomFrom(te, node, rulesToDo);
			newrule.getBody().addAll(uAtoms);	
			
		} //end-for
	}

	

	/**
	 * This is the MAIN recursive method in this class!!
	 * Takes a node and return the list of functions (atoms) that it represents.
	 * Usually it will be a single atom, but it is different for the filter case.
	 */
	private static List<Function> getAtomFrom(IntermediateQuery te, QueryNode node,  Queue<ConstructionNode> rulesToDo  ) {
		
		List<Function> body = new ArrayList<Function>();
		
		/**
		 * Basic Atoms
		 */
		
		if (node instanceof ConstructionNode) {
			DataAtom newAns = ((ConstructionNode) node).getProjectionAtom();
			Function mutAt = convertToMutableFunction(newAns);
			rulesToDo.add((ConstructionNode)node);
			body.add(mutAt);
			return body;
			
		} else if (node instanceof FilterNode) {
			ImmutableExpression filter = ((FilterNode) node).getFilterCondition();
			Expression mutFilter =  ImmutabilityTools.convertToMutableBooleanExpression(filter);
			List<QueryNode> listnode =  te.getChildren(node);
			body.addAll(getAtomFrom(te, listnode.get(0), rulesToDo));
			body.add(mutFilter);
			return body;
			
					
		} else if (node instanceof DataNode) {
			DataAtom atom = ((DataNode)node).getProjectionAtom();
			Function mutAt = convertToMutableFunction(atom);
			body.add(mutAt);
			return body;
				
			
			
		/**
		 * Nested Atoms	
		 */
		} else  if (node instanceof InnerJoinNode) {
			Optional<ImmutableExpression> filter = ((InnerJoinNode)node).getOptionalFilterCondition();
			List<Function> atoms = new ArrayList<>();
			List<QueryNode> listnode =  te.getChildren(node);
			for (QueryNode childnode: listnode) {
				List<Function> atomsList = getAtomFrom(te, childnode, rulesToDo);
				atoms.addAll(atomsList);
			}

			if (atoms.size() <= 1) {
				throw new IllegalArgumentException("Inconsistent IQ: an InnerJoinNode must have at least two children");
			}

			if (filter.isPresent()){
				ImmutableExpression filter2 = filter.get();
				Function mutFilter = ImmutabilityTools.convertToMutableBooleanExpression(filter2);
				Function newJ = getSPARQLJoin(atoms, Optional.of(mutFilter));
				body.add(newJ);
				return body;
			}else{
				Function newJ = getSPARQLJoin(atoms, Optional.empty());
				body.add(newJ);
				return body;
			}
			
		} else if (node instanceof LeftJoinNode) {
			Optional<ImmutableExpression> filter = ((LeftJoinNode)node).getOptionalFilterCondition();
			List<QueryNode> listnode =  te.getChildren(node);

			List<Function> atomsListLeft = getAtomFrom(te, listnode.get(0), rulesToDo);
			List<Function> atomsListRight = getAtomFrom(te, listnode.get(1), rulesToDo);
				
			if (filter.isPresent()){
				ImmutableExpression filter2 = filter.get();
				Expression mutFilter =  ImmutabilityTools.convertToMutableBooleanExpression(filter2);
				Function newLJAtom = ofac.getSPARQLLeftJoin(atomsListLeft, atomsListRight, Optional.of(mutFilter));
				body.add(newLJAtom);
				return body;
			}else{
				Function newLJAtom = ofac.getSPARQLLeftJoin(atomsListLeft, atomsListRight, Optional.empty());
				body.add(newLJAtom);
				return body;
			}

		} else if (node instanceof UnionNode) {
		
			List<QueryNode> listnode =  te.getChildren(node);
			
			for (QueryNode nod: listnode){
				rulesToDo.add((ConstructionNode)nod);
		
			} //end for
		
			QueryNode nod= listnode.get(0);
			if (nod instanceof ConstructionNode) {
					Function newAns = convertToMutableFunction(((ConstructionNode) nod).getProjectionAtom());
					body.add(newAns);
					return body;
				}else{
					 throw new UnsupportedOperationException("The Union should have only construct");
				}
			

						
		} else {
			 throw new UnsupportedOperationException("Type od node in the intermediate tree is unknown!!");
		}
	
	}

	private static Function getSPARQLJoin(List<Function> atoms, Optional<Function> optionalCondition) {
		int atomCount = atoms.size();
		Function rightTerm;

		switch (atomCount) {
			case 0:
			case 1:
				throw new IllegalArgumentException("A join requires at least two atoms");
			case 2:
				rightTerm = atoms.get(1);
				break;
			default:
				rightTerm = getSPARQLJoin(atoms.subList(1, atomCount), Optional.empty());
				break;
		}

		return optionalCondition.isPresent()
				? ofac.getSPARQLJoin(atoms.get(0), rightTerm, optionalCondition.get())
				: ofac.getSPARQLJoin(atoms.get(0), rightTerm);
	}

}
