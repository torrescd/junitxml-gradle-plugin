package io.github.torrescd.junitxml;


import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.component.ComponentRegistry;
import org.gradle.api.internal.file.FileCollectionFactory;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.testing.Test;
import org.gradle.testing.base.plugins.TestingBasePlugin;

import java.util.concurrent.Callable;

public class JunitXMLGradlePlugin implements Plugin<ProjectInternal>{
    @Override
    public void apply(ProjectInternal project) {

        project.getTasks().register("test", ProcessResults.class, w -> w.setGroup("verification"));

        project.getPluginManager().apply(TestingBasePlugin.class);
        //project.getPluginManager().apply(JavaPlugin.class);

        
        configureTest(project);
        
    }

    private void configureTest(final ProjectInternal project) {

        FileCollectionFactory fileCollectionFactory = project.getServices().get(FileCollectionFactory.class);
        
        project.getTasks().withType(Test.class).configureEach(test -> {
            test.getConventionMapping().map("testClassesDirs", () ->fileCollectionFactory.fixed(project.getBuildDir()));
            test.getConventionMapping().map("classpath", () -> fileCollectionFactory.fixed(project.getBuildDir()));
        });


    }


}
