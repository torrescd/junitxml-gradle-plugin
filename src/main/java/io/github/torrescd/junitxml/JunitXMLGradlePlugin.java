package io.github.torrescd.junitxml;

import org.gradle.api.Plugin;
import org.gradle.api.internal.file.FileCollectionFactory;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.tasks.testing.Test;
import org.gradle.testing.base.plugins.TestingBasePlugin;

import java.io.File;

public class JunitXMLGradlePlugin implements Plugin<ProjectInternal>{
    @Override
    public void apply(ProjectInternal project) {

        project.getTasks().register("processJunitXml", ProcessTestResultsTask.class, w -> w.setGroup("verification"));

        project.getPluginManager().apply(TestingBasePlugin.class);

        
        configureTest(project);
        
    }

    private void configureTest(final ProjectInternal project) {

        FileCollectionFactory fileCollectionFactory = project.getServices().get(FileCollectionFactory.class);
        
        project.getTasks().withType(Test.class).configureEach(test -> {
            test.getConventionMapping().map("testClassesDirs", () ->fileCollectionFactory.fixed(new File(project.getBuildDir(), "test-results/test")));
            test.getConventionMapping().map("classpath", () -> fileCollectionFactory.fixed(new File(project.getBuildDir(), "test-results/test")));
        });


    }


}
