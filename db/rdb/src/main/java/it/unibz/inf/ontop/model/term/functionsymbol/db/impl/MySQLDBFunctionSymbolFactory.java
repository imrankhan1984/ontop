package it.unibz.inf.ontop.model.term.functionsymbol.db.impl;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.inject.Inject;
import it.unibz.inf.ontop.model.term.DBConstant;
import it.unibz.inf.ontop.model.term.ImmutableTerm;
import it.unibz.inf.ontop.model.term.TermFactory;
import it.unibz.inf.ontop.model.term.functionsymbol.db.DBBooleanFunctionSymbol;
import it.unibz.inf.ontop.model.term.functionsymbol.db.DBFunctionSymbol;
import it.unibz.inf.ontop.model.term.functionsymbol.db.DBTypeConversionFunctionSymbol;
import it.unibz.inf.ontop.model.type.DBTermType;
import it.unibz.inf.ontop.model.type.DBTypeFactory;
import it.unibz.inf.ontop.model.type.RDFDatatype;
import it.unibz.inf.ontop.model.type.TypeFactory;
import it.unibz.inf.ontop.model.vocabulary.XSD;


import java.util.function.Function;

import static it.unibz.inf.ontop.model.type.impl.DefaultSQLDBTypeFactory.TIMESTAMP_STR;
import static it.unibz.inf.ontop.model.type.impl.MySQLDBTypeFactory.BIT_STR;
import static it.unibz.inf.ontop.model.type.impl.MySQLDBTypeFactory.YEAR_STR;

public class MySQLDBFunctionSymbolFactory extends AbstractSQLDBFunctionSymbolFactory {

    protected static final String UUID_STR = "UUID";
    protected static final String  CURRENT_TZ_STR =
            "REPLACE(TIME_FORMAT(TIMEDIFF(NOW(),CONVERT_TZ(NOW(),@@session.time_zone,'+00:00')),'+%H:%i'),'+-','-')";
    private static final String REGEXP_LIKE_STR = "REGEXP_LIKE";

    private static final String UNSUPPORTED_MSG = "Not supported by MySQL";

    @Inject
    protected MySQLDBFunctionSymbolFactory(TypeFactory typeFactory) {
        super(createMySQLRegularFunctionTable(typeFactory), typeFactory);
    }

    protected static ImmutableTable<String, Integer, DBFunctionSymbol> createMySQLRegularFunctionTable(
            TypeFactory typeFactory) {
        DBTypeFactory dbTypeFactory = typeFactory.getDBTypeFactory();
        DBTermType dbBooleanType = dbTypeFactory.getDBBooleanType();
        DBTermType dbIntType = dbTypeFactory.getDBLargeIntegerType();
        DBTermType abstractRootDBType = dbTypeFactory.getAbstractRootDBType();

        Table<String, Integer, DBFunctionSymbol> table = HashBasedTable.create(
                createDefaultRegularFunctionTable(typeFactory));

        DBBooleanFunctionSymbol regexpLike2 = new DefaultSQLSimpleDBBooleanFunctionSymbol(REGEXP_LIKE_STR, 2, dbBooleanType,
                abstractRootDBType);
        table.put(REGEXP_LIKE_STR, 2, regexpLike2);

        DBBooleanFunctionSymbol regexpLike3 = new DefaultSQLSimpleDBBooleanFunctionSymbol(REGEXP_LIKE_STR, 3, dbBooleanType,
                abstractRootDBType);
        table.put(REGEXP_LIKE_STR, 3, regexpLike3);

        return ImmutableTable.copyOf(table);
    }

    /**
     * We know that the normalization function DATETIME -> xsd:datetimeStamp will always be invalid
     *   (it is not bound to any timezone).
     * TODO: how to inform the user? In a mapping it would be invalid, but what about a cast in a SPARQL query?
     */
    @Override
    protected ImmutableTable<DBTermType, RDFDatatype, DBTypeConversionFunctionSymbol> createNormalizationTable() {
        Table<DBTermType, RDFDatatype, DBTypeConversionFunctionSymbol> table = HashBasedTable.create();
        table.putAll(super.createNormalizationTable());

        // TIMESTAMP is not the default
        RDFDatatype xsdDatetime = typeFactory.getXsdDatetimeDatatype();
        RDFDatatype xsdDatetimeStamp = typeFactory.getXsdDatetimeStampDatatype();
        DBTermType timestamp = dbTypeFactory.getDBTermType(TIMESTAMP_STR);

        DBTypeConversionFunctionSymbol timestampNormFunctionSymbol = createDateTimeNormFunctionSymbol(timestamp);
        table.put(timestamp, xsdDatetime, timestampNormFunctionSymbol);
        table.put(timestamp, xsdDatetimeStamp, timestampNormFunctionSymbol);

        // BIT(1) boolean normalization
        RDFDatatype xsdBoolean = typeFactory.getXsdBooleanDatatype();
        DBTermType bitOne = dbTypeFactory.getDBTermType(BIT_STR, 1);
        table.put(bitOne, xsdBoolean, new DefaultNumberNormAsBooleanFunctionSymbol(bitOne, dbStringType));

        // Forbids the post-processing of YEAR_TO_TEXT as the JDBC driver converts strangely the YEAR
        RDFDatatype xsdYear = typeFactory.getDatatype(XSD.GYEAR);
        DBTermType year = dbTypeFactory.getDBTermType(YEAR_STR);
        table.put(year, xsdYear, new NonPostProcessedSQLSimpleDBCastFunctionSymbolImpl(year, dbStringType));

        return ImmutableTable.copyOf(table);
    }

    /**
     * TODO: provide a MySQL specific implementation
     */
    @Override
    protected DBTypeConversionFunctionSymbol createDateTimeDenormFunctionSymbol(DBTermType timestampType) {
        return super.createDateTimeDenormFunctionSymbol(timestampType);
    }

    @Override
    protected String serializeContains(ImmutableList<? extends ImmutableTerm> terms,
                                       Function<ImmutableTerm, String> termConverter, TermFactory termFactory) {
        return String.format("INSTR(%s,%s) > 0",
                termConverter.apply(terms.get(0)),
                termConverter.apply(terms.get(1)));
    }

    @Override
    protected String serializeStrBefore(ImmutableList<? extends ImmutableTerm> terms,
                                        Function<ImmutableTerm, String> termConverter, TermFactory termFactory) {
        String str = termConverter.apply(terms.get(0));
        String before = termConverter.apply(terms.get(1));
        return String.format("LEFT(%s,INSTR(%s,%s)-1)", str,  str, before);
    }

    @Override
    protected String serializeStrAfter(ImmutableList<? extends ImmutableTerm> terms,
                                       Function<ImmutableTerm, String> termConverter, TermFactory termFactory) {
        String str = termConverter.apply(terms.get(0));
        String after = termConverter.apply(terms.get(1));
        // sign return 1 if positive number, 0 if 0 and -1 if negative number
        // it will return everything after the value if it is present or it will return an empty string if it is not present
        return String.format("SUBSTRING(%s,LOCATE(%s,%s) + LENGTH(%s), SIGN(LOCATE(%s,%s)) * LENGTH(%s))",
                str, after, str , after , after, str, str);
    }

    @Override
    protected String serializeMD5(ImmutableList<? extends ImmutableTerm> terms, Function<ImmutableTerm, String> termConverter, TermFactory termFactory) {
        return String.format("MD5(%s)", termConverter.apply(terms.get(0)));
    }

    @Override
    protected String serializeSHA1(ImmutableList<? extends ImmutableTerm> terms, Function<ImmutableTerm, String> termConverter, TermFactory termFactory) {
        return String.format("SHA1(%s)", termConverter.apply(terms.get(0)));
    }

    @Override
    protected String serializeSHA256(ImmutableList<? extends ImmutableTerm> terms, Function<ImmutableTerm, String> termConverter, TermFactory termFactory) {
        return String.format("SHA2(%s,256)", termConverter.apply(terms.get(0)));
    }

    @Override
    protected String serializeSHA512(ImmutableList<? extends ImmutableTerm> terms, Function<ImmutableTerm, String> termConverter, TermFactory termFactory) {
        return String.format("SHA2(%s,512)", termConverter.apply(terms.get(0)));
    }

    /**
     * Tricky as this information may be lost while converting SPARQL constants into DB ones
     */
    @Override
    protected String serializeTz(ImmutableList<? extends ImmutableTerm> terms, Function<ImmutableTerm, String> termConverter, TermFactory termFactory) {
        throw new RuntimeException("TODO: support it");
    }

    @Override
    protected DBTypeConversionFunctionSymbol createDateTimeNormFunctionSymbol(DBTermType dbDateTimestampType) {
        return new DefaultSQLTimestampISONormFunctionSymbol(
                dbDateTimestampType,
                dbStringType,
                (terms, converter, factory) -> serializeDateTimeNorm(dbDateTimestampType, terms, converter));
    }

    /**
     * For DATETIME, never provides a time zone.
     * For TIMESTAMP, provides the session time zone used for generating the string
     *  (NB: TIMESTAMP is stored as a duration from a fixed date time)
     *
     */
    protected String serializeDateTimeNorm(DBTermType dbDateTimestampType,
                                           ImmutableList<? extends ImmutableTerm> terms,
                                           Function<ImmutableTerm, String> termConverter) {

        String dateTimeStringWithoutTz = String.format("REPLACE(CAST(%s AS CHAR(30)),' ', 'T')",
                termConverter.apply(terms.get(0)));

        return dbDateTimestampType.getName().equals(TIMESTAMP_STR)
                ? String.format("CONCAT(%s,%s)", dateTimeStringWithoutTz, CURRENT_TZ_STR)
                : dateTimeStringWithoutTz;
    }

    @Override
    protected String serializeDateTimeNorm(ImmutableList<? extends ImmutableTerm> terms,
                                           Function<ImmutableTerm, String> termConverter, TermFactory termFactory) {
        throw new UnsupportedOperationException("This method should not be called for MySQL");
    }

    /**
     * Made Implicit
     */
    protected DBTypeConversionFunctionSymbol createDatetimeToDatetimeCastFunctionSymbol(DBTermType inputType,
                                                                                        DBTermType targetType) {
        return new DefaultImplicitDBCastFunctionSymbol(inputType, targetType);
    }

    @Override
    protected DBFunctionSymbol createR2RMLIRISafeEncode() {
        return new MySQLR2RMLSafeIRIEncodeFunctionSymbolImpl(dbStringType);
    }

    @Override
    protected String getUUIDNameInDialect() {
        return UUID_STR;
    }

    /**
     * NB: For MySQL >= 8, REGEXP_LIKE could be used
     */
    @Override
    public DBBooleanFunctionSymbol getDBRegexpMatches2() {
        return new DBBooleanFunctionSymbolWithSerializerImpl("REGEXP_MATCHES_2",
                ImmutableList.of(abstractRootDBType, abstractRootDBType), dbBooleanType, false,
                (terms, termConverter, termFactory) -> String.format(
                        // NB: BINARY is for making it case sensitive
                        "(%s REGEXP BINARY %s)",
                        termConverter.apply(terms.get(0)),
                        termConverter.apply(terms.get(1))));
    }

    @Override
    public DBBooleanFunctionSymbol getDBRegexpMatches3() {
        return new DBBooleanFunctionSymbolWithSerializerImpl("REGEXP_MATCHES_3",
                ImmutableList.of(abstractRootDBType, abstractRootDBType, abstractRootDBType), dbBooleanType, false,
                this::serializeDBRegexpMatches3);
    }

    /**
     * TODO: throw an exception when the version is detected to be < 8 and reaching the "default" case?
     */
    protected String serializeDBRegexpMatches3(ImmutableList<? extends ImmutableTerm> terms,
                                           Function<ImmutableTerm, String> termConverter, TermFactory termFactory) {
        String string = termConverter.apply(terms.get(0));
        String pattern = termConverter.apply(terms.get(1));
        ImmutableTerm flagTerm = terms.get(2);
        if (flagTerm instanceof DBConstant) {
            String flags = ((DBConstant) flagTerm).getValue();
            switch (flags) {
                // Case sensitive
                case "":
                    return String.format("(%s REGEXP BINARY %s)", string, pattern);
                // Case insensitive
                case "i":
                    // TODO: is it robust to collation?
                    return String.format("(%s REGEXP %s)", string, pattern);
                default:
                    break;
            }
        }

        // REGEXP_LIKE is only supported by MySQL >= 8
        return getRegularDBFunctionSymbol(REGEXP_LIKE_STR, 3)
                .getNativeDBString(terms, termConverter, termFactory);
    }
}
