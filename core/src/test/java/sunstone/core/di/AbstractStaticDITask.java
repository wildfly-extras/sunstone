package sunstone.core.di;


import sunstone.api.AbstractSetupTask;

abstract class AbstractStaticDITask extends AbstractSetupTask {
    @DirectlyAnnotatedInject
    static String directStaticInjectInAbstract;

    @DirectlyAnnotatedInject
    String directNonStaticInjectInAbstract;

    @IndirectlyAnnotatedInject
    static String indirectStaticInjectInAbstract;

    @IndirectlyAnnotatedInject
    String indirectNonStaticInjectInAbstract;
}
