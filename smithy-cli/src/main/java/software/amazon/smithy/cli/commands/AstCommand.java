/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.Arguments;
import software.amazon.smithy.cli.Command;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.cli.dependencies.DependencyResolver;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.validation.Severity;

final class AstCommand implements Command {

    private final String parentCommandName;
    private final DependencyResolver.Factory dependencyResolverFactory;

    AstCommand(String parentCommandName, DependencyResolver.Factory dependencyResolverFactory) {
        this.parentCommandName = parentCommandName;
        this.dependencyResolverFactory = dependencyResolverFactory;
    }

    @Override
    public String getName() {
        return "ast";
    }

    @Override
    public String getSummary() {
        return "Reads Smithy models in and writes out a single JSON AST model.";
    }

    @Override
    public int execute(Arguments arguments, Env env) {
        arguments.addReceiver(new ConfigOptions());
        arguments.addReceiver(new BuildOptions());
        arguments.addReceiver(new Options());

        CommandAction action = HelpActionWrapper.fromCommand(
                this,
                parentCommandName,
                new ClasspathAction(dependencyResolverFactory, this::runWithClassLoader));

        return action.apply(arguments, env);
    }

    private static final class Options implements ArgumentReceiver {
        static final String FLATTEN_OPTION = "--flatten";
        private boolean flatten = false;

        static final String INCLUDE_PRELUDE_OPTION = "--include-prelude";
        private boolean includePrelude = false;

        @Override
        public boolean testOption(String name) {
            if (FLATTEN_OPTION.equals(name)) {
                flatten = true;
                return true;
            }
            if (INCLUDE_PRELUDE_OPTION.equals(name)) {
                includePrelude = true;
                return true;
            }
            return false;
        }

        @Override
        public void registerHelp(HelpPrinter printer) {
            printer.option(FLATTEN_OPTION, null, "Flattens and removes mixins from the model.");
            printer.option(INCLUDE_PRELUDE_OPTION, null, "Includes the prelude shapes in the model.");
        }
    }

    private int runWithClassLoader(SmithyBuildConfig config, Arguments arguments, Env env) {
        Model model = new ModelBuilder()
                .config(config)
                .arguments(arguments)
                .env(env)
                .models(arguments.getPositional())
                .validationPrinter(env.stderr())
                .validationMode(Validator.Mode.QUIET)
                .defaultSeverity(Severity.DANGER)
                .build();

        Options options = arguments.getReceiver(Options.class);
        ModelSerializer serializer = ModelSerializer.builder().includePrelude(options.includePrelude).build();
        if (options.flatten) {
            model = ModelTransformer.create().flattenAndRemoveMixins(model);
        }
        env.stdout().println(Node.prettyPrintJson(serializer.serialize(model)));
        return 0;
    }
}
