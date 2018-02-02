#!/usr/bin/env bash
mkdir -p $DELTA_ROOT/key; cd $DELTA_ROOT/key

# generate key pairs of the target controller, and ovs
sudo ovs-pki init --force
sudo ovs-pki req+sign ctl controller
sudo ovs-pki req+sign sc switch

# import switch certificate into the controller's keystore
keytool -noprompt -trustcacerts -importcert -file sc-cert.pem -keystore onos.jks -storepass changeit -alias onos

# import a controller's key pair to the controller's keystore
sudo openssl pkcs12 -export -out delta.pkcs12 -in ctl-cert.pem -inkey ctl-privkey.pem -CAfile /var/lib/openvswitch/pki/controllerca/cacert.pem -passout pass:changeit -passin pass:changeit
keytool -importkeystore -srckeystore delta.pkcs12 -destkeystore onos.jks -srcstorepass changeit -deststorepass changeit

# specify the controller's key store
export JAVA_OPTS="${JAVA_OPTS:--DenableOFTLS=true -Djavax.net.ssl.keyStore=$DELTA_ROOT/key/onos.jks -Djavax.net.ssl.keyStorePassword=changeit -Djavax.net.ssl.trustStore=$DELTA_ROOT/key/onos.jks -Djavax.net.ssl.trustStorePassword=changeit}"

# specify the ovs's key store
sudo ovs-vsctl set-ssl $DELTA_ROOT/key/sc-privkey.pem $DELTA_ROOT/key/sc-cert.pem /var/lib/openvswitch/pki/controllerca/cacert.pem