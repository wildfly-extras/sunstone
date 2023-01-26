package sunstone.core.properties;

/**
 * Clouds configuration entries/constants.
 */
public final class Config {

    /**
     * If the classpath resource denoted by this path exists, it is used as the default configuration
     * by {@link CloudProperties CloudProperties}.
     */
    public static final String DEFAULT_PROPERTIES = "/sunstone.properties";

    /**
     * Key suffix for {@link ObjectProperties} inheritance implementation.<br/>
     * E.g.
     *
     * <pre>
     * node.test.template=instance-small
     * cloud.provider.provider01.template=docker-template
     * </pre>
     */
    public static final String TEMPLATE = "template";

    /**
     * Key suffix for {@link ObjectProperties} inheritance implementation. Inverted approach to
     * {@link Config#TEMPLATE}. <br/>
     * E.g.
     *
     * <pre>
     * node.node1.stuff=stuff
     * node.node1.moreStuff=moreStuff
     * node.node1.templateTo=node2,node3,node4
     *
     * node.node2.stuff=differentStuff
     * </pre>
     */
    public static final String TEMPLATE_TO = "templateTo";

    private Config() {
    }
}
