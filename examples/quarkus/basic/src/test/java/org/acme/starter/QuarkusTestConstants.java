package org.acme.starter;

public class QuarkusTestConstants {
    public static final String APP_NAME = "${azure.appName:quarkus-helloWorld}";
    public static final String APP_GROUP = "${azure.group:SunstoneQuarkusWebApp}";
}
