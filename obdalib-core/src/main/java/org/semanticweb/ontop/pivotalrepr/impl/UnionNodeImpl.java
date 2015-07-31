package org.semanticweb.ontop.pivotalrepr.impl;


import com.google.common.base.Optional;
import org.semanticweb.ontop.pivotalrepr.*;
import org.semanticweb.ontop.pivotalrepr.proposal.QueryOptimizationProposal;

public class UnionNodeImpl extends QueryNodeImpl implements UnionNode {

    private static final String UNION_NODE_STR = "UNION";

    @Override
    public void acceptVisitor(QueryNodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public UnionNode clone() {
        return new UnionNodeImpl();
    }

    @Override
    public UnionNode acceptNodeTransformer(QueryNodeTransformer transformer) throws QueryNodeTransformationException {
        return transformer.transform(this);
    }

    @Override
    public String toString() {
        return UNION_NODE_STR;
    }
}