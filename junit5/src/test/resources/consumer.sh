Content-Type: multipart/mixed; boundary="//"
MIME-Version: 1.0

--//
Content-Type: text/cloud-config; charset="us-ascii"
MIME-Version: 1.0
Content-Transfer-Encoding: 7bit
Content-Disposition: attachment; filename="cloud-config.txt"

#cloud-config
cloud_final_modules:
- [scripts-user, always]

--//
Content-Type: text/x-shellscript; charset="us-ascii"
MIME-Version: 1.0
Content-Transfer-Encoding: 7bit
Content-Disposition: attachment; filename="userdata.txt"

#!/usr/bin/env bash
# platform dependent variables
export EAP_PROFILE=standalone-ec2-full-ha.xml

if [[ "`cat /etc/redhat-release`" = *"release 7"* ]]; then
  START_COMMAND="systemctl start eap7-standalone"
  SERVICE_CONF_FILE=/etc/opt/rh/eap7/wildfly/eap7-standalone.conf
else
  START_COMMAND="service eap7-standalone start"
  SERVICE_CONF_FILE=/etc/sysconfig/eap7-standalone
fi

if [ ! -f /home/ec2-user/first]; then
  touch /home/ec2-user/first
  sed -i 's/.*ClientAliveInterval.*/ClientAliveInterval 30/' /etc/ssh/sshd_config

  # set up addresses
  INTERNAL_IP_ADDRESS=`ip addr show | grep eth0 -A 2 | head -n 3 | tail -n 1 | awk '{ print $2 }' | sed "s-/24--g" | cut -d'/' -f1`
  echo $INTERNAL_IP_ADDRESS > /root/internal-ip-address.txt
  JBOSS_HOME=/opt/rh/eap7/root/usr/share/wildfly

  echo "JAVA_OPTS=\"\$JAVA_OPTS -Djboss.bind.address.external=$EXTERNAL_IP_ADDRESS -Djboss.jgroups.s3_ping.access_key='$S3_BITS_ACCESS_KEY' -Djboss.jgroups.s3_ping.secret_access_key='$S3_BITS_SECRET_KEY' -Djboss.jgroups.s3_ping.bucket='$S3_PING_BUCKET' -Djboss.jvmRoute=$EAP_NODE_NAME -Djboss.bind.address=$INTERNAL_IP_ADDRESS -Djboss.bind.address.private=$INTERNAL_IP_ADDRESS -Djboss.bind.address.management=$INTERNAL_IP_ADDRESS\"" >> $JBOSS_HOME/bin/standalone.conf
  echo "" >> $JBOSS_HOME/bin/standalone.conf
  # set up standalone-full.xml
  echo "WILDFLY_SERVER_CONFIG=$EAP_EAP_PROFILE" >> $SERVICE_CONF_FILE
  # set up users (for remote management connections and application connections)
  ${JBOSS_HOME}/bin/add-user.sh -u admin -p pass.1234 -r ManagementRealm -g SuperUser -e
  ${JBOSS_HOME}/bin/add-user.sh -u joe -p pass.1234 -a -r ApplicationRealm -g admin -e
  # set up env vars
  echo "export JAVA_HOME=/usr/lib/jvm/java" >> /root/.bashrc
  echo "export JBOSS_HOME=${JBOSS_HOME}" >> /root/.bashrc
  echo "export JAVA_HOME=/usr/lib/jvm/java" >> /home/ec2-user/.bashrc
  echo "export JBOSS_HOME=${JBOSS_HOME}" >> /home/ec2-user/.bashrc

  JBOSS_CLI_SCRIPT=jboss.cli
fi
# start EAP
$START_COMMAND
--//--