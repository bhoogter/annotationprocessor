package com.benjaminhoogterp;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.ForkOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AddOpensPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        List<String> options = List.of(
                "--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED",
                "--add-opens=java.prefs/java.util.prefs=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED",
                "--add-exports=java.prefs/java.util.prefs=ALL-UNNAMED"
        );

        Set<Task> compileJava = project.getTasksByName("compileJava", true);

        compileJava.forEach(task -> {
            System.out.println("Was [" + task.getName() + "]: " + ((CompileOptions) task.property("options")).getForkOptions().getAllJvmArgs().size());
            CompileOptions compileOptions = (CompileOptions) task.property("options");
            compileOptions.setFork(true);
            ForkOptions forkOptions = compileOptions.getForkOptions();
            List<String> args = new ArrayList<>(forkOptions.getAllJvmArgs());
            args.addAll(options);
            forkOptions.setJvmArgs(args);
            System.out.println("Updated options [" + task.getName() + "]: " + ((CompileOptions) task.property("options")).getForkOptions().getAllJvmArgs().size());
        });

    }
}
