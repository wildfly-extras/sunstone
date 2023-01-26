package aws.cloudformation.suite;

import aws.cloudformation.suite.tests.PerSuiteAwsCfTemplate;
import aws.cloudformation.suite.tests.PerSuitePerClassAwsCfTemplates;
import aws.cloudformation.suite.tests.TwoSamePerSuiteAwsCfTemplates;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({PerSuiteAwsCfTemplate.class, PerSuitePerClassAwsCfTemplates.class, TwoSamePerSuiteAwsCfTemplates.class})
public class AwsCfTemplatesSuiteTest {
}
