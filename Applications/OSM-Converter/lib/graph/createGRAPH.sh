#!/bin/bash

DBNAME=$4
DBUSER=$5
DBPASSWORD=$6
DBHOST=$7
DBPORT=$8

./../osmosis-0.39/bin/osmosis --$2 "../../$3" --rgc output-sql="../../output/$1/$1.sql"

PGPASSWORD=$DBPASSWORD
export $PGPASSWORD
psql -U $DBUSER -d $DBNAME -f "../../output/$1/$1.sql"

java -cp ./mapsforge-onboard-routing-0.3.1-SNAPSHOT.jar:./postgresql-8.4-701.jdbc4.jar:./trove-3.0.0rc1.jar:./hadoop-0.20.1-core.jar org.mapsforge.routing.hh.preprocessing.CommandLineUtil "../../output/$1/$1.mobileHH" --format=mobile --config.file=config.properties -host=$DBHOST -port=$DBPORT -dbName=$DBNAME -user=$DBUSER -pwd=$DBPASSWORD

# TODO: resolve Error Message(in german) while executing psql (line 7):
# "FEHLER:  Berechtigung nur für Eigentümer der Funktion remove_unconnected_vertice"