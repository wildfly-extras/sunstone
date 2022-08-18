package sunstone.api;

public enum TemplateType {
    /**
     * The test provides path to the template placed in resources
     */
    RESOURCE,

    /**
     * The test provides URL to the template which shall be downloaded.
     */
    URL,

    /**
     * The test provides string content.
     */
    CONTENT
}
