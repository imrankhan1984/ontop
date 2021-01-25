package it.unibz.inf.ontop.dbschema.impl.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import it.unibz.inf.ontop.dbschema.*;
import it.unibz.inf.ontop.dbschema.impl.AbstractRelationDefinition;
import it.unibz.inf.ontop.dbschema.impl.OntopViewDefinitionImpl;
import it.unibz.inf.ontop.dbschema.impl.RawQuotedIDFactory;
import it.unibz.inf.ontop.exception.MetadataExtractionException;
import it.unibz.inf.ontop.injection.CoreSingletons;
import it.unibz.inf.ontop.injection.IntermediateQueryFactory;
import it.unibz.inf.ontop.iq.IQ;
import it.unibz.inf.ontop.iq.IQTree;
import it.unibz.inf.ontop.iq.node.ConstructionNode;
import it.unibz.inf.ontop.iq.node.ExtensionalDataNode;
import it.unibz.inf.ontop.iq.type.UniqueTermTypeExtractor;
import it.unibz.inf.ontop.model.atom.AtomFactory;
import it.unibz.inf.ontop.model.atom.AtomPredicate;
import it.unibz.inf.ontop.model.atom.DistinctVariableOnlyDataAtom;
import it.unibz.inf.ontop.model.term.ImmutableTerm;
import it.unibz.inf.ontop.model.term.TermFactory;
import it.unibz.inf.ontop.model.term.Variable;
import it.unibz.inf.ontop.model.type.DBTermType;
import it.unibz.inf.ontop.spec.sqlparser.ExpressionParser;
import it.unibz.inf.ontop.spec.sqlparser.RAExpressionAttributes;
import it.unibz.inf.ontop.substitution.SubstitutionFactory;
import it.unibz.inf.ontop.utils.CoreUtilsFactory;
import it.unibz.inf.ontop.utils.ImmutableCollectors;
import it.unibz.inf.ontop.utils.VariableGenerator;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@JsonPropertyOrder({
        "relations"
})
@JsonDeserialize(as = JsonBasicView.class)
public class JsonBasicView extends JsonView {
    @Nonnull
    public final Columns columns;
    @Nonnull
    public final List<String> baseRelation;
    @Nonnull
    public final UniqueConstraints uniqueConstraints;
    @Nonnull
    public final OtherFunctionalDependencies otherFunctionalDependencies;
    @Nonnull
    public final ForeignKeys foreignKeys;

    @JsonCreator
    public JsonBasicView(@JsonProperty("columns") Columns columns, @JsonProperty("name") List<String> name,
                         @JsonProperty("baseRelation") List<String> baseRelation,
                         @JsonProperty("uniqueConstraints") UniqueConstraints uniqueConstraints,
                         @JsonProperty("otherFunctionalDependencies") OtherFunctionalDependencies otherFunctionalDependencies,
                         @JsonProperty("foreignKeys") ForeignKeys foreignKeys) {
        super(name);
        this.columns = columns;
        this.baseRelation = baseRelation;
        this.uniqueConstraints = uniqueConstraints;
        this.otherFunctionalDependencies = otherFunctionalDependencies;
        this.foreignKeys = foreignKeys;
    }

    @Override
    public OntopViewDefinition createViewDefinition(DBParameters dbParameters, MetadataLookup parentCacheMetadataLookup)
            throws MetadataExtractionException {

        QuotedIDFactory quotedIDFactory = dbParameters.getQuotedIDFactory();
        RelationID relationId = quotedIDFactory.createRelationID(name.toArray(new String[0]));

        NamedRelationDefinition parentDefinition = parentCacheMetadataLookup.getRelation(quotedIDFactory.createRelationID(
                baseRelation.toArray(new String[0])));

        IQ iq = createIQ(relationId, parentDefinition, dbParameters);

        // For added columns the termtype, quoted ID and nullability all need to come from the IQ
        RelationDefinition.AttributeListBuilder attributeBuilder = createAttributeBuilder(iq, dbParameters);

        return new OntopViewDefinitionImpl(
                ImmutableList.of(relationId),
                attributeBuilder,
                iq,
                // TODO: consider other levels
                1,
                dbParameters.getCoreSingletons());
    }

    /**
     * TODO: implement it
     */
    @Override
    public void insertIntegrityConstraints(MetadataLookup metadataLookup) throws MetadataExtractionException {

        RelationID relationID = JsonMetadata.deserializeRelationID(metadataLookup.getQuotedIDFactory(), baseRelation);

        for (AddUniqueConstraints uc : uniqueConstraints.added)
            insertUniqueConstraints(metadataLookup.getRelation(relationID), metadataLookup.getQuotedIDFactory(), uc);

        for (AddFunctionalDependency fd : otherFunctionalDependencies.added)
            insertFunctionalDependencies(metadataLookup.getRelation(relationID), metadataLookup.getQuotedIDFactory(), fd);

        for (AddForeignKey fk : foreignKeys.added) {
            insertForeignKeys(metadataLookup.getRelation(relationID), metadataLookup, fk);
        }
    }


    private IQ createIQ(RelationID relationId, NamedRelationDefinition parentDefinition, DBParameters dbParameters)
            throws MetadataExtractionException {

        CoreSingletons coreSingletons = dbParameters.getCoreSingletons();

        TermFactory termFactory = coreSingletons.getTermFactory();
        IntermediateQueryFactory iqFactory = coreSingletons.getIQFactory();
        AtomFactory atomFactory = coreSingletons.getAtomFactory();
        QuotedIDFactory quotedIdFactory = dbParameters.getQuotedIDFactory();

        ImmutableSet<Variable> addedVariables = columns.added.stream()
                .map(a -> a.name)
                .map(attributeName -> normalizeAttributeName(attributeName, quotedIdFactory))
                .map(termFactory::getVariable)
                .collect(ImmutableCollectors.toSet());

        ImmutableList<Variable> projectedVariables = extractRelationVariables(addedVariables, columns.hidden, parentDefinition,
                dbParameters);

        ImmutableMap<Integer, Variable> parentArgumentMap = createParentArgumentMap(addedVariables, parentDefinition,
                coreSingletons.getCoreUtilsFactory());
        ExtensionalDataNode parentDataNode = iqFactory.createExtensionalDataNode(parentDefinition, parentArgumentMap);

        ConstructionNode constructionNode = createConstructionNode(projectedVariables, parentDefinition,
                parentArgumentMap, dbParameters);

        IQTree iqTree = iqFactory.createUnaryIQTree(
                constructionNode,
                parentDataNode);

        AtomPredicate tmpPredicate = createTemporaryPredicate(relationId, projectedVariables.size(), coreSingletons);
        DistinctVariableOnlyDataAtom projectionAtom = atomFactory.getDistinctVariableOnlyDataAtom(tmpPredicate, projectedVariables);

        return iqFactory.createIQ(projectionAtom, iqTree)
                .normalizeForOptimization();
    }

    private ImmutableList<Variable> extractRelationVariables(ImmutableSet<Variable> addedVariables, List<String> hidden,
                                                             NamedRelationDefinition parentDefinition, DBParameters dbParameters) {
        TermFactory termFactory = dbParameters.getCoreSingletons().getTermFactory();
        QuotedIDFactory quotedIdFactory = dbParameters.getQuotedIDFactory();

        ImmutableList<String> hiddenColumnNames = hidden.stream()
                .map(attributeName -> normalizeAttributeName(attributeName, quotedIdFactory))
                .collect(ImmutableCollectors.toList());

        ImmutableList<Variable> inheritedVariableStream = parentDefinition.getAttributes().stream()
                .map(a -> a.getID().getName())
                .filter(n -> !hiddenColumnNames.contains(n))
                .map(termFactory::getVariable)
                .filter(v -> !addedVariables.contains(v))
                .collect(ImmutableCollectors.toList());

        return Stream.concat(
                addedVariables.stream(),
                inheritedVariableStream.stream())
                .collect(ImmutableCollectors.toList());
    }

    private String normalizeAttributeName(String attributeName, QuotedIDFactory quotedIdFactory) {
        return quotedIdFactory.createAttributeID(attributeName).getName();
    }

    private AtomPredicate createTemporaryPredicate(RelationID relationId, int arity, CoreSingletons coreSingletons) {
        DBTermType dbRootType = coreSingletons.getTypeFactory().getDBTypeFactory().getAbstractRootDBType();

        return new TemporaryViewPredicate(
                relationId.getSQLRendering(),
                // No precise base DB type for the temporary predicate
                IntStream.range(0, arity)
                        .boxed()
                        .map(i -> dbRootType).collect(ImmutableCollectors.toList()));
    }

    private ImmutableMap<Integer, Variable> createParentArgumentMap(ImmutableSet<Variable> addedVariables,
                                                                    NamedRelationDefinition parentDefinition,
                                                                    CoreUtilsFactory coreUtilsFactory) {
        VariableGenerator variableGenerator = coreUtilsFactory.createVariableGenerator(addedVariables);

        ImmutableList<Attribute> parentAttributes = parentDefinition.getAttributes();

        // NB: the non-necessary variables will be pruned out by normalizing the IQ
        return IntStream.range(0, parentAttributes.size())
                .boxed()
                .collect(ImmutableCollectors.toMap(
                        i -> i,
                        i -> variableGenerator.generateNewVariable(
                                parentAttributes.get(i).getID().getName())));

    }

    private ConstructionNode createConstructionNode(ImmutableList<Variable> projectedVariables,
                                                    NamedRelationDefinition parentDefinition,
                                                    ImmutableMap<Integer, Variable> parentArgumentMap,
                                                    DBParameters dbParameters) throws MetadataExtractionException {

        QuotedIDFactory quotedIdFactory = dbParameters.getQuotedIDFactory();
        CoreSingletons coreSingletons = dbParameters.getCoreSingletons();
        TermFactory termFactory = coreSingletons.getTermFactory();
        IntermediateQueryFactory iqFactory = coreSingletons.getIQFactory();
        SubstitutionFactory substitutionFactory = coreSingletons.getSubstitutionFactory();

        ImmutableMap<QualifiedAttributeID, ImmutableTerm> parentAttributeMap = parentArgumentMap.entrySet().stream()
                .collect(ImmutableCollectors.toMap(
                        e -> new QualifiedAttributeID(null, parentDefinition.getAttributes().get(e.getKey()).getID()),
                        Map.Entry::getValue));


        ImmutableMap.Builder<Variable, ImmutableTerm> substitutionMapBuilder = ImmutableMap.builder();
        for (AddColumns a : columns.added) {
            Variable v = termFactory.getVariable(normalizeAttributeName(a.name, quotedIdFactory));
            try {
                ImmutableTerm value = extractExpression(a.expression, parentAttributeMap, quotedIdFactory, coreSingletons);
                substitutionMapBuilder.put(v, value);
            } catch (JSQLParserException e) {
                throw new MetadataExtractionException("Unsupported expression for " + a.name + " in " + name + ":\n" + e);
            }
        }

        return iqFactory.createConstructionNode(
                ImmutableSet.copyOf(projectedVariables),
                substitutionFactory.getSubstitution(substitutionMapBuilder.build()));
    }

    private ImmutableTerm extractExpression(String partialExpression,
                                            ImmutableMap<QualifiedAttributeID, ImmutableTerm> parentAttributeMap,
                                            QuotedIDFactory quotedIdFactory, CoreSingletons coreSingletons) throws JSQLParserException {
        String sqlQuery = "SELECT " + partialExpression + " FROM fakeTable";
        ExpressionParser parser = new ExpressionParser(quotedIdFactory, coreSingletons);
        Statement statement = CCJSqlParserUtil.parse(sqlQuery);
        SelectItem si = ((PlainSelect) ((Select) statement).getSelectBody()).getSelectItems().get(0);
        net.sf.jsqlparser.expression.Expression exp = ((SelectExpressionItem) si).getExpression();
        return parser.parseTerm(exp, new RAExpressionAttributes(parentAttributeMap, null));
    }

    private RelationDefinition.AttributeListBuilder createAttributeBuilder(IQ iq, DBParameters dbParameters) throws MetadataExtractionException {
        UniqueTermTypeExtractor uniqueTermTypeExtractor = dbParameters.getCoreSingletons().getUniqueTermTypeExtractor();
        QuotedIDFactory quotedIdFactory = dbParameters.getQuotedIDFactory();

        RelationDefinition.AttributeListBuilder builder = AbstractRelationDefinition.attributeListBuilder();
        IQTree iqTree = iq.getTree();

        RawQuotedIDFactory rawQuotedIqFactory = new RawQuotedIDFactory(quotedIdFactory);

        for (Variable v : iqTree.getVariables()) {
            builder.addAttribute(rawQuotedIqFactory.createAttributeID(v.getName()),
                    (DBTermType) uniqueTermTypeExtractor.extractUniqueTermType(v, iqTree)
                            // TODO: give the name of the view
                            .orElseThrow(() -> new MetadataExtractionException("No type inferred for " + v + " in " + iq)),
                    iqTree.getVariableNullability().isPossiblyNullable(v));
        }
        return builder;
    }

    private void insertUniqueConstraints(NamedRelationDefinition relation, QuotedIDFactory idFactory, AddUniqueConstraints addUniqueConstraints) throws MetadataExtractionException {
        FunctionalDependency.Builder builder = addUniqueConstraints.isPrimaryKey
                ? UniqueConstraint.primaryKeyBuilder(relation, addUniqueConstraints.name)
                : UniqueConstraint.builder(relation, addUniqueConstraints.name);

        JsonMetadata.deserializeAttributeList(idFactory, addUniqueConstraints.determinants, builder::addDeterminant);
        builder.build();
    }

    private void insertFunctionalDependencies(NamedRelationDefinition relation, QuotedIDFactory idFactory, AddFunctionalDependency addFunctionalDependency) throws MetadataExtractionException {
        FunctionalDependency.Builder builder = FunctionalDependency.defaultBuilder(relation);
        JsonMetadata.deserializeAttributeList(idFactory, addFunctionalDependency.determinants, builder::addDeterminant);
        JsonMetadata.deserializeAttributeList(idFactory, addFunctionalDependency.dependents, builder::addDependent);
        builder.build();
    }

    public void insertForeignKeys(NamedRelationDefinition relation, MetadataLookup lookup, AddForeignKey addForeignKey) throws MetadataExtractionException {

        ForeignKeyConstraint.Builder builder = ForeignKeyConstraint.builder(addForeignKey.name, relation,
                lookup.getRelation(JsonMetadata.deserializeRelationID(lookup.getQuotedIDFactory(), addForeignKey.to.relation)));

        try {
            for (int i = 0; i < addForeignKey.to.columns.size(); i++)
                builder.add(lookup.getQuotedIDFactory().createAttributeID(addForeignKey.from),
                        lookup.getQuotedIDFactory().createAttributeID(addForeignKey.to.columns.get(i)));
        }
        catch (AttributeNotFoundException e) {
            throw new MetadataExtractionException(e);
        }

        builder.build();
    }

    @JsonPropertyOrder({
            "added",
            "hidden"
    })
    private static class Columns extends JsonOpenObject {
        @Nonnull
        public final List<AddColumns> added;
        @Nonnull
        public final List<String> hidden;

        @JsonCreator
        public Columns(@JsonProperty("added") List<AddColumns> added,
                       @JsonProperty("hidden") List<String> hidden) {
            this.added = added;
            this.hidden = hidden;
        }
    }

    @JsonPropertyOrder({
            "name",
            "expression",
    })
    private static class AddColumns extends JsonOpenObject {
        @Nonnull
        public final String name;
        @Nonnull
        public final String expression;


        @JsonCreator
        public AddColumns(@JsonProperty("name") String name,
                          @JsonProperty("expression") String expression) {
            this.name = name;
            this.expression = expression;
        }
    }

    @JsonPropertyOrder({
            "added"//,
            //"hidden"
    })
    private static class UniqueConstraints extends JsonOpenObject {
        @Nonnull
        public final List<AddUniqueConstraints> added;
        /*@Nonnull
        public final List<String> hidden;*/

        @JsonCreator
        public UniqueConstraints(@JsonProperty("added") List<AddUniqueConstraints> added/*,
                       @JsonProperty("hidden") List<String> hidden*/) {
            this.added = added;
            //this.hidden = hidden;
        }
    }

    @JsonPropertyOrder({
            "name",
            "determinants",
            "isPrimaryKey"
    })
    private static class AddUniqueConstraints extends JsonOpenObject {
        @Nonnull
        public final String name;
        @Nonnull
        public final List<String> determinants;
        @Nonnull
        public final Boolean isPrimaryKey;


        @JsonCreator
        public AddUniqueConstraints(@JsonProperty("name") String name,
                                    @JsonProperty("determinants") List<String> determinants,
                                    @JsonProperty("isPrimaryKey") Boolean isPrimaryKey) {
            this.name = name;
            this.determinants = determinants;
            this.isPrimaryKey = isPrimaryKey;
        }
    }

    @JsonPropertyOrder({
            "added"//,
            //"hidden"
    })
    private static class OtherFunctionalDependencies extends JsonOpenObject {
        @Nonnull
        public final List<AddFunctionalDependency> added;
        /*@Nonnull
        public final List<String> hidden;*/

        @JsonCreator
        public OtherFunctionalDependencies(@JsonProperty("added") List<AddFunctionalDependency> added/*,
                                           @JsonProperty("hidden") List<String> hidden*/) {
            this.added = added;
            //this.hidden = hidden;
        }
    }

    @JsonPropertyOrder({
            "determinants",
            "dependents"
    })
    private static class AddFunctionalDependency extends JsonOpenObject {
        @Nonnull
        public final List<String> determinants;
        @Nonnull
        public final List<String> dependents;

        public AddFunctionalDependency(@JsonProperty("determinants") List<String> determinants,
                                       @JsonProperty("dependents") List<String> dependents) {
            this.determinants = determinants;
            this.dependents = dependents;
        }
    }

    @JsonPropertyOrder({
            "added"//,
            //"hidden"
    })
    private static class ForeignKeys extends JsonOpenObject {
        @Nonnull
        public final List<AddForeignKey> added;
        /*@Nonnull
        public final List<String> hidden;*/

        @JsonCreator
        public ForeignKeys(@JsonProperty("added") List<AddForeignKey> added) {
            this.added = added;
            //this.hidden = hidden;
        }
    }

    @JsonPropertyOrder({
            "determinants",
            "dependents"
    })
    private static class AddForeignKey extends JsonOpenObject {
        @Nonnull
        public final String name;
        @Nonnull
        public final String from;
        @Nonnull
        public final ForeignKeyPart to;

        public AddForeignKey(@JsonProperty("name") String name,
                             @JsonProperty("from") String from,
                             @JsonProperty("to") ForeignKeyPart to) {
            this.name = name;
            this.from = from;
            this.to = to;
        }
    }

    @JsonPropertyOrder({
            "relation",
            "columns"
    })
    public static class ForeignKeyPart extends JsonOpenObject {
        public final List<String> relation;
        public final List<String> columns;

        @JsonCreator
        public ForeignKeyPart(@JsonProperty("relation") List<String> relation,
                              @JsonProperty("columns") List<String> columns) {
            this.relation = relation;
            this.columns = columns;
        }
    }
}
