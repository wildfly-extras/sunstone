package org.wildfly.extras.sunstone.api.impl;

/**
 * Clouds configuration entries/constants.
 */
public final class Config {

    /**
     * If the classpath resource denoted by this path exists, it is used as the default configuration
     * by {@link org.wildfly.extras.sunstone.api.CloudProperties CloudProperties}.
     */
    public static final String DEFAULT_PROPERTIES = "/sunstone.properties";

    /**
     * @deprecated older variant of {@link #DEFAULT_PROPERTIES}, kept only for backwards compatibility
     */
    @Deprecated
    public static final String OLD_DEFAULT_PROPERTIES_2 = "/clouds.properties";

    /**
     * @deprecated old variant of {@link #DEFAULT_PROPERTIES}, kept only for backwards compatibility
     */
    @Deprecated
    public static final String OLD_DEFAULT_PROPERTIES_1 = "/org/wildfly/extras/clouds/default.properties";

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
     * Key suffix for {@link ObjectProperties} to specify a node group, both for node configuration
     * and cloud provider configuration. This typically doesn't have to be used, there's a sensible default.
     */
    public static final String GROUP = "nodegroup";

    /**
     * Key suffix for {@link ObjectProperties} to specify whether the cloud provider should leave the nodes
     * running after it's finished. Only applies to cloud provider configuration.
     */
    public static final String LEAVE_NODES_RUNNING = "leaveNodesRunning";

    /**
     * Cloud provider related keys
     */
    public static final class CloudProvider {

        public static final String TYPE = "type";

        /**
         * Docker cloud provider related keys. When changing, don't forget to change {@code docker-README.md}.
         */
        public static final class Docker {

            /**
             * key for docker endpoint url
             */
            public static final String ENDPOINT = "docker.endpoint";
            public static final String API_VERSION = "docker.apiVersion";
            public static final String TLS_CERT_PATH = "docker.tls.cert";
            public static final String TLS_CA_CERT_PATH = "docker.tls.ca.cert";
            public static final String TLS_KEY_PATH = "docker.tls.key";
        }

        /**
         * EC2 cloud provider related keys. When changing, don't forget to change {@code ec2-README.md}.
         */
        public static final class EC2 {
            /**
             * The AMI query which is used for searching the AMI. Setting this option should not be necessary if you don't intend to search for AMIs (e.g. if you have a specific AMI in mind)
             */
            public static final String AMI_QUERY = "ec2.ami.query";
            /**
             * The region for the cloud provider. Usually us-east-1.
             * @See org.jclouds.aws.domain.Region
             */
            public static final String REGION = "ec2.region";
            /**
             * If set to true, a logging module for EC2 cloud provider will be added.
             * This logs the ec2 operations the cloud provider does. E.g. how it lists the AMIs available, how it starts an instance, etc. Lots of output, beware!
             */
            public static final String LOG_EC2_OPERATIONS = "ec2.logEC2Operations";
            /**
             * The endpoint to contact. Setting it isn't mandatory and the default endpoint should be the default endpoint for the region.
             */
            public static final String ENDPOINT = "ec2.endpoint";
            /**
             * An access key ID of an IAM user. See EC2 console for more information on this.
             */
            public static final String ACCESS_KEY_ID = "ec2.accessKeyID";
            /**
             * The secret access key of an IAM user. This key needs to be bound to the access key ID. See EC2 console for more information.
             */
            public static final String SECRET_ACCESS_KEY = "ec2.secretAccessKey";
        }

        /**
         * Azure cloud provider related keys. When changing, don't forget to change {@code azure-README.md}.
         */
        public static final class Azure {
            public static final String SUBSCRIPTION_ID = "azure.subscriptionId";
            public static final String PRIVATE_KEY_FILE = "azure.privateKeyFile";
            public static final String PRIVATE_KEY_PASSWORD = "azure.privateKeyPassword";
        }

        /**
         * Azure ARM cloud provider related keys. When changing, don't forget to change {@code azure-arm-README.md}.
         */
        public static final class AzureArm {
            public static final String SUBSCRIPTION_ID = "azure-arm.subscriptionId";
            public static final String TENANT_ID = "azure-arm.tenantId";
            public static final String APPLICATION_ID = "azure-arm.applicationId";
            public static final String PASSWORD = "azure-arm.password";
            public static final String PUBLISHERS = "azure-arm.publishers";
        }

        /**
         * OpenStack cloud provider related keys. When changing, don't forget to change {@code openstack-README.md}.
         */
        public static final class Openstack {
            public static final String ENDPOINT = "openstack.endpoint";
            public static final String USERNAME = "openstack.username";
            public static final String PASSWORD = "openstack.password";
        }

        /**
         * Bare metal cloud provider related keys.  When changing, don't forget to change {@code baremetal-README.md}.
         */
        public static final class BareMetal {
            public static final String NODES = "baremetal.nodes";
        }
    }

    /**
     * Configuration related to nodes
     */
    public static final class Node {

        public static final class Shared {
            public static final String BOOT_SCRIPT = "bootScript";
            public static final String BOOT_SCRIPT_FILE = "bootScript.file";
            public static final String STOP_TIMEOUT_SEC = "stop.timeoutSec";
            public static final String START_TIMEOUT_SEC = "start.timeoutSec";
        }

        /**
         * Configuration keys for Docker nodes. When changing, don't forget to change {@code docker-README.md}.
         */
        public static final class Docker {

            /**
             * key for Docker image name (or id)
             */
            public static final String IMAGE = "docker.image";

            public static final String NETWORK_MODE = "docker.networkMode";

            public static final String CPU_SHARES = "docker.cpuShares";
            public static final String MEMORY_IN_MB = "docker.memoryInMb";

            public static final String INBOUND_PORTS = "docker.inboundPorts";

            public static final String PORT_BINDINGS = "docker.portBindings";

            public static final String WAIT_FOR_PORTS = "docker.waitForPorts";
            public static final String WAIT_FOR_PORTS_TIMEOUT_SEC = "docker.waitForPorts.timeoutSec";

            public static final String CMD = "docker.cmd";
            public static final String ENTRYPOINT = "docker.entrypoint";
            public static final String VOLUME_BINDINGS = "docker.volumeBindings";

            public static final String PRIVILEGED = "docker.privileged";
            public static final String CAP_ADD = "docker.capAdd";

            public static final String ENV = "docker.env";
            public static final String ENV_SPLIT_REGEX = "docker.env.splitter";
            public static final String DEFAULT_ENV_SPLIT_REGEX = ",";

            public static final String SSH_USER = "docker.ssh.user";
            public static final String SSH_PASSWORD = "docker.ssh.password";
            public static final String SSH_PRIVATE_KEY = "docker.ssh.privateKey";
            public static final String SSH_PORT = "docker.ssh.port";

            public static final String ENV_NAME_SSH_PORT = "DOCKER_SSH_PORT";

        }

        /**
         * Configuration keys for EC2 nodes. When changing, don't forget to change {@code ec2-README.md}.
         */
        public static final class EC2 {
            /**
             * <a href="https://aws.amazon.com/ec2/instance-types/">Instance type</a> for the node. This defines the
             * computing/networking/... capabilities of the node.
             *
             * @See org.jclouds.ec2.domain.InstanceType
             */
            public static final String INSTANCE_TYPE = "ec2.instance.type";
            public static final String IMAGE = "ec2.image";
            /**
             * The image id for the instance. See EC2 console for the list of instances available to you.
             */
            public static final String IMAGE_ID = "ec2.image.id";

            /**
             * The <a href="http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-key-pairs.html">EC2 key pair</a> name for
             * this instance. If you want to connect manually to the instance, this will be used to access it (unless overriden
             * with {@link org.wildfly.extras.sunstone.api.impl.Config.Node.EC2#SSH_PRIVATE_KEY} or
             * {@link org.wildfly.extras.sunstone.api.impl.Config.Node.EC2#SSH_PRIVATE_KEY_FILE}.
             *
             * <p>
             * To see the key pairs available to you, head to the EC2 management console.
             * </p>
             */
            public static final String KEY_PAIR = "ec2.keyPair";
            /**
             * The identifier for the security group for this instance. See EC2 management console for list of your security groups.
             */
            public static final String SECURITY_GROUPS = "ec2.securityGroups";
            public static final String SECURITY_GROUP_IDS = "ec2.securityGroupIds";

            /**
             * The list of ports to be available to the outer world.
             */
            public static final String INBOUND_PORTS = "ec2.inboundPorts";

            /**
             * The ssh user for connecting to the instance. Overrides default setting (usually "root") but is currently unused.
             * Use {@link org.wildfly.extras.sunstone.api.impl.Config.Node.EC2#SSH_PRIVATE_KEY_FILE} instead.
             */
            public static final String SSH_USER = "ec2.ssh.user";
            /**
             * The ssh user's password for connecting to the instance. Overrides default setting but is currently unused.
             * Use {@link org.wildfly.extras.sunstone.api.impl.Config.Node.EC2#SSH_PRIVATE_KEY_FILE} instead.
             */
            private static final String SSH_PASSWORD = "ec2.ssh.password";
            /**
             * The ssh private key (not the file of the key!). In case you want to pass a private key manually.
             * Currently unused in favor of {@link org.wildfly.extras.sunstone.api.impl.Config.Node.EC2#SSH_PRIVATE_KEY_FILE}.
             */
            private static final String SSH_PRIVATE_KEY = "ec2.ssh.privateKey";
            /**
             * The path to the private key which should be used to connect to the instance. Necessary for establishing an ssh channel for your tests.
             */
            public static final String SSH_PRIVATE_KEY_FILE = "ec2.ssh.privateKeyFile";

            /**
             * How long to wait for ports to open after the instance is started.
             */
            public static final String WAIT_FOR_PORTS_TIMEOUT_SEC = "ec2.waitForPorts.timeoutSec";
            /**
             * What ports to wait for. Port 22 is recommended, since you'll likely want to run some commands through ssh on your machine.
             */
            public static final String WAIT_FOR_PORTS = "ec2.waitForPorts";
            /**
             * Unencoded Value of <a href="http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/user-data.html">EC2 User data</a>.
             * This property takes precedence over the {@value #BOOT_SCRIPT_FILE}.
             *
             * @see #BOOT_SCRIPT_FILE
             */
            public static final String USER_DATA = "ec2.userData";
            /**
             * Path to a file with unencoded value of
             * <a href="http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/user-data.html">EC2 User data</a>. This property is
             * only used when {@value #BOOT_SCRIPT} property is not specified.
             *
             * @see #BOOT_SCRIPT
             */
            public static final String USER_DATA_FILE = "ec2.userData.file";
            public static final String SUBNET_ID = "ec2.subnetId";
        }

        /**
         * Configuration keys for Azure nodes. When changing, don't forget to change {@code azure-README.md}.
         */
        public static final class Azure {
            public static final String IMAGE = "azure.image";
            public static final String IMAGE_IS_WINDOWS = "azure.image.isWindows";
            public static final String SIZE = "azure.size";
            public static final String INBOUND_PORTS = "azure.inboundPorts";
            public static final String STORAGE_ACCOUNT_NAME = "azure.storageAccountName";
            public static final String SSH_USER = "azure.ssh.user";
            public static final String SSH_PASSWORD = "azure.ssh.password";
            public static final String SSH_PRIVATE_KEY = "azure.ssh.privateKey";
            public static final String SSH_PRIVATE_KEY_FILE = "azure.ssh.privateKeyFile";
            public static final String WAIT_FOR_PORTS = "azure.waitForPorts";
            public static final String WAIT_FOR_PORTS_TIMEOUT_SEC = "azure.waitForPorts.timeoutSec";
            public static final String PROVISION_GUEST_AGENT = "azure.provisionGuestAgent";
            public static final String VIRTUAL_NETWORK = "azure.virtualNetwork";
            public static final String SUBNET = "azure.subnet";

            /**
             * @deprecated Use {@link Config.Node.Shared#BOOT_SCRIPT}
             */
            @Deprecated
            public static final String USER_DATA = "azure.userData";
            /**
             * @deprecated Use {@link Config.Node.Shared#BOOT_SCRIPT_FILE}
             */
            @Deprecated
            public static final String USER_DATA_FILE = "azure.userData.file";
            @Deprecated
            public static final String LOGIN_NAME = "azure.login.name";
            @Deprecated
            public static final String LOGIN_PASSWORD = "azure.login.password";
        }

        /**
         * Configuration keys for Azure ARM nodes. When changing, don't forget to change {@code azure-arm-README.md}.
         */
        public static final class AzureArm {
            public static final String IMAGE = "azure-arm.image";
            public static final String IMAGE_IS_WINDOWS = "azure-arm.image.isWindows";
            public static final String SIZE = "azure-arm.size";
            public static final String INBOUND_PORTS = "azure-arm.inboundPorts";
            public static final String SSH_USER = "azure-arm.ssh.user";
            public static final String SSH_PASSWORD = "azure-arm.ssh.password";
            public static final String WAIT_FOR_PORTS = "azure-arm.waitForPorts";
            public static final String WAIT_FOR_PORTS_TIMEOUT_SEC = "azure-arm.waitForPorts.timeoutSec";
        }

        /**
         * Configuration keys for OpenStack nodes. When changing, don't forget to change {@code openstack-README.md}.
         */
        public static final class Openstack {
            public static final String SSH_USER = "openstack.ssh.user";
            public static final String SSH_PASSWORD = "openstack.ssh.password";
            public static final String INSTANCE_TYPE = "openstack.instance.type";
            public static final String REGION = "openstack.region";
            public static final String IMAGE = "openstack.image";
            public static final String IMAGE_ID = "openstack.image.id";
            public static final String KEY_PAIR = "openstack.keyPair";
            public static final String SECURITY_GROUPS = "openstack.securityGroups";
            public static final String INBOUND_PORTS = "openstack.inboundPorts";
            public static final String SSH_PRIVATE_KEY = "openstack.ssh.privateKey";
            public static final String SSH_PRIVATE_KEY_FILE = "openstack.ssh.privateKeyFile";
            public static final String WAIT_FOR_PORTS_TIMEOUT_SEC = "openstack.waitForPorts.timeoutSec";
            public static final String WAIT_FOR_PORTS = "openstack.waitForPorts";
            public static final String USER_DATA = "openstack.userData";
            public static final String USER_DATA_FILE = "openstack.userData.file";
            public static final String FLOATING_IP_POOLS = "openstack.floatingIpPools";
        }

        /**
         * Configuration keys for bare metal nodes. When changing, don't forget to change {@code baremetal-README.md}.
         */
        public static final class BareMetal {
            public static final String HOST = "baremetal.host";
            public static final String PRIVATE_ADDRESS = "baremetal.privateAddress";
            public static final String SSH_PORT = "baremetal.ssh.port";
            public static final String SSH_USER = "baremetal.ssh.user";
            public static final String SSH_PASSWORD = "baremetal.ssh.password";
            public static final String SSH_PRIVATE_KEY = "baremetal.ssh.privateKey";
            public static final String SSH_PRIVATE_KEY_FILE = "baremetal.ssh.privateKeyFile";
            public static final String WAIT_FOR_PORTS = "baremetal.waitForPorts";
            public static final String WAIT_FOR_PORTS_TIMEOUT_SEC = "baremetal.waitForPorts.timeoutSec";
        }
    }

    private Config() {
    }
}
