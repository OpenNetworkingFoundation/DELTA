#!/usr/bin/env bash
APPS_DIR=$DELTA_ROOT/agents/apps/

cp -v $DELTA_ROOT/agents/channel/target/delta-agent-channel-1.0-SNAPSHOT-jar-with-dependencies.jar ~/channel/
cp -v $APPS_DIR/onos/onos-1.9.0/target/delta-agent-app-onos-1.9.0-1.0-SNAPSHOT.jar ~/channel/onos-1.9.0/apache-karaf-3.0.8/deploy/
cp -v $APPS_DIR/opendaylight/opendaylight-carbon/target/delta-agent-app-odl-carbon-1.0-SNAPSHOT.jar ~/channel/distribution-karaf-0.6.0-Carbon/deploy/
cp -v $APPS_DIR/floodlight/floodlight-1.2/target/floodlight.jar ~/channel
cp -v $APPS_DIR/ryu/app_agent_13.py $APPS_DIR/ryu/am_interface.py ~/channel