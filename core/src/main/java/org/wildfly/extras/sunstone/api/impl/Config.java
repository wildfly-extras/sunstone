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
            public static final String AMI_QUERY = "ec2.ami.query";
            public static final String REGION = "ec2.region";
            public static final String LOG_EC2_OPERATIONS = "ec2.logEC2Operations";
            public static final String ENDPOINT = "ec2.endpoint";
            public static final String ACCESS_KEY_ID = "ec2.accessKeyID";
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
            public static final String BOOT_SCRIPT_WITH_SUDO = "bootScript.withSudo";
            public static final String BOOT_SCRIPT_REMOTE_PATH = "bootScript.remotePath";

            public static final String WAIT_FOR_PORTS = "waitForPorts";
            public static final String WAIT_FOR_PORTS_TIMEOUT_SEC = "waitForPorts.timeoutSec";

            public static final String BOOT_SCRIPT_WAIT_FOR_PORTS_PREFIX = "bootScript.";

            public static final String STOP_TIMEOUT_SEC = "stop.timeoutSec";
            public static final String START_TIMEOUT_SEC = "start.timeoutSec";

            public static final String SUDO_COMMAND = "sudo.command";

            public static final String SSH_FIX_SUDOERS = "ssh.fixSudoers";
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
            public static final String SSH_PRIVATE_KEY_FILE = "docker.ssh.privateKeyFile";
            public static final String SSH_PORT = "docker.ssh.port";

            public static final String ENV_NAME_SSH_PORT = "DOCKER_SSH_PORT";

        }

        /**
         * Configuration keys for EC2 nodes. When changing, don't forget to change {@code ec2-README.md}.
         */
        public static final class EC2 {
            public static final String INSTANCE_TYPE = "ec2.instance.type";
            public static final String IMAGE = "ec2.image";
            public static final String IMAGE_ID = "ec2.image.id";
            public static final String KEY_PAIR = "ec2.keyPair";
            public static final String SECURITY_GROUPS = "ec2.securityGroups";
            public static final String SECURITY_GROUP_IDS = "ec2.securityGroupIds";
            public static final String INBOUND_PORTS = "ec2.inboundPorts";
            public static final String SSH_USER = "ec2.ssh.user";
            private static final String SSH_PASSWORD = "ec2.ssh.password";
            private static final String SSH_PRIVATE_KEY = "ec2.ssh.privateKey";
            public static final String SSH_PRIVATE_KEY_FILE = "ec2.ssh.privateKeyFile";
            public static final String USER_DATA = "ec2.userData";
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
            public static final String PROVISION_GUEST_AGENT = "azure.provisionGuestAgent";
            public static final String VIRTUAL_NETWORK = "azure.virtualNetwork";
            public static final String SUBNET = "azure.subnet";
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
            public static final String NETWORKS = "openstack.networks";
            public static final String INBOUND_PORTS = "openstack.inboundPorts";
            public static final String SSH_PRIVATE_KEY = "openstack.ssh.privateKey";
            public static final String SSH_PRIVATE_KEY_FILE = "openstack.ssh.privateKeyFile";
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
        }
    }

    private Config() {
    }
}
