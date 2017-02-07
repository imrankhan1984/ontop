package it.unibz.inf.ontop.injection.impl;


import com.google.common.collect.ImmutableMap;
import com.google.inject.Module;
import it.unibz.inf.ontop.executor.ProposalExecutor;
import it.unibz.inf.ontop.injection.InvalidOntopConfigurationException;
import it.unibz.inf.ontop.injection.impl.OntopOptimizationConfigurationImpl.DefaultOntopOptimizationBuilderFragment;
import it.unibz.inf.ontop.injection.impl.OntopOptimizationConfigurationImpl.OntopOptimizationOptions;
import it.unibz.inf.ontop.injection.impl.OntopQueryAnsweringConfigurationImpl.DefaultOntopQueryAnsweringBuilderFragment;
import it.unibz.inf.ontop.injection.impl.OntopQueryAnsweringConfigurationImpl.OntopQueryAnsweringOptions;
import it.unibz.inf.ontop.owlrefplatform.core.mappingprocessing.TMappingExclusionConfig;
import it.unibz.inf.ontop.injection.QuestCoreConfiguration;
import it.unibz.inf.ontop.injection.QuestCoreSettings;
import it.unibz.inf.ontop.pivotalrepr.proposal.QueryOptimizationProposal;
import it.unibz.inf.ontop.answering.reformulation.IRIDictionary;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

public class QuestCoreConfigurationImpl extends OBDACoreConfigurationImpl implements QuestCoreConfiguration {

    private final QuestCoreSettings settings;
    private final QuestCoreOptions options;
    // Concrete implementation due to the "mixin style" (indirect inheritance)
    private final OntopQueryAnsweringConfigurationImpl runtimeConfiguration;

    protected QuestCoreConfigurationImpl(QuestCoreSettings settings, QuestCoreOptions options) {
        super(settings, options.obdaOptions);
        this.settings = settings;
        this.options = options;
        this.runtimeConfiguration = new OntopQueryAnsweringConfigurationImpl(settings, options.runtimeOptions);
    }

    /**
     * Can be overloaded by sub-classes
     */
    @Override
    protected ImmutableMap<Class<? extends QueryOptimizationProposal>, Class<? extends ProposalExecutor>>
    generateOptimizationConfigurationMap() {
        ImmutableMap.Builder<Class<? extends QueryOptimizationProposal>, Class<? extends ProposalExecutor>>
                internalExecutorMapBuilder = ImmutableMap.builder();
        internalExecutorMapBuilder.putAll(super.generateOptimizationConfigurationMap());
        internalExecutorMapBuilder.putAll(runtimeConfiguration.generateOptimizationConfigurationMap());

        return internalExecutorMapBuilder.build();
    }

    /**
     * TODO: complete
     */
    @Override
    public void validate() throws InvalidOntopConfigurationException {

        boolean isMapping = isMappingDefined();

        if (!isMapping) {
            throw new InvalidOntopConfigurationException("Mapping is not specified", this);
        }
        /**
         * TODO: complete
         */

        // TODO: check the types of some Object properties.
    }

    @Override
    public Optional<TMappingExclusionConfig> getTmappingExclusions() {
        return options.excludeFromTMappings;
    }

    @Override
    public QuestCoreSettings getSettings() {
        return settings;
    }

    @Override
    public Optional<IRIDictionary> getIRIDictionary() {
        return runtimeConfiguration.getIRIDictionary();
    }

    @Override
    protected Stream<Module> buildGuiceModules() {
        return Stream.concat(
                super.buildGuiceModules(),
                Stream.concat(runtimeConfiguration.buildGuiceModules(),
                    Stream.of(
                            new QuestComponentModule(this),
                            new OntopQueryAnsweringPostModule(getSettings())
                            )));
    }

    public static class QuestCoreOptions {
        public final Optional<TMappingExclusionConfig> excludeFromTMappings;
        private final OBDAConfigurationOptions obdaOptions;
        private final OntopQueryAnsweringOptions runtimeOptions;


        public QuestCoreOptions(Optional<TMappingExclusionConfig> excludeFromTMappings,
                                OBDAConfigurationOptions obdaOptions,
                                OntopQueryAnsweringOptions runtimeOptions) {
            this.excludeFromTMappings = excludeFromTMappings;
            this.obdaOptions = obdaOptions;
            this.runtimeOptions = runtimeOptions;
        }
    }


    protected abstract static class QuestCoreConfigurationBuilderMixin<B extends QuestCoreConfiguration.Builder<B>>
        extends OBDACoreConfigurationBuilderMixin<B>
        implements QuestCoreConfiguration.Builder<B> {

        private final DefaultOntopOptimizationBuilderFragment<B> optimizationBuilderFragment;
        private final DefaultOntopQueryAnsweringBuilderFragment<B> runtimeBuilderFragment;

        protected QuestCoreConfigurationBuilderMixin() {
            B builder = (B) this;
            questCoreBuilderFragment = new DefaultQuestCoreBuilderFragment<>(builder);
            optimizationBuilderFragment = new DefaultOntopOptimizationBuilderFragment<>(builder);
            runtimeBuilderFragment = new DefaultOntopQueryAnsweringBuilderFragment<>(builder);
        }

        @Override
        public B tMappingExclusionConfig(@Nonnull TMappingExclusionConfig config) {
            return questCoreBuilderFragment.tMappingExclusionConfig(config);
        }

        @Override
        protected Properties generateProperties() {
            Properties properties = super.generateProperties();
            properties.putAll(optimizationBuilderFragment.generateProperties());
            properties.putAll(questCoreBuilderFragment.generateUserProperties());
            properties.putAll(runtimeBuilderFragment.generateProperties());
            return properties;
        }

        protected final QuestCoreOptions generateQuestCoreOptions() {
            OBDAConfigurationOptions obdaCoreOptions = generateOBDACoreOptions();
            OntopOBDAOptions obdaOptions =  obdaCoreOptions.mappingSqlOptions.mappingOptions.obdaOptions;
            OntopOptimizationOptions optimizationOptions = optimizationBuilderFragment.generateOptimizationOptions(
                    obdaOptions.modelOptions);
            OntopQueryAnsweringOptions runtimeOptions = runtimeBuilderFragment.generateQAOptions(obdaOptions,
                    optimizationOptions);

            return questCoreBuilderFragment.generateQuestCoreOptions(obdaCoreOptions, runtimeOptions);
        }

        @Override
        public B enableIRISafeEncoding(boolean enable) {
            return runtimeBuilderFragment.enableIRISafeEncoding(enable);
        }

        @Override
        public B enableExistentialReasoning(boolean enable) {
            return runtimeBuilderFragment.enableExistentialReasoning(enable);
        }

        @Override
        public B iriDictionary(@Nonnull IRIDictionary iriDictionary) {
            return runtimeBuilderFragment.iriDictionary(iriDictionary);
        }
    }


    public static final class BuilderImpl<B extends QuestCoreConfiguration.Builder<B>>
            extends QuestCoreConfigurationBuilderMixin<B> {

        @Override
        public QuestCoreConfiguration build() {
            Properties properties = generateProperties();
            QuestCoreSettings settings = new QuestCoreSettingsImpl(properties, isR2rml());

            return new QuestCoreConfigurationImpl(settings, generateQuestCoreOptions());
        }
    }
}
