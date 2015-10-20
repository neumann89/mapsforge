#!/bin/bash

# first parameter is the output file prefix (e.g. the city name)
# second parameter is OSM file type (rb for .osm.pbf, rx for .osm)
# third parameter is the OSM file (should be in the "input" folder or a subdirectory of this folder)
# output files are generated in folder "output"
# example: ./convert.sh "berlin" rb input/berlin.osm.pbf

mkdir output/$1

# Map File
cd lib/map/
./createMAP.sh $1 $2 $3
cd ../../


# Graph File
# ATTENTION: postgresql has to be running and has to be configured with the following settings
DBNAME=mapsforge
DBUSER=mapsforge
DBPASSWORD=mapsforge
DBHOST=localhost
DBPORT=5432

cd lib/graph/
./createGRAPH.sh $1 $2 $3 $DBNAME $DBUSER $DBPASSWORD $DBHOST $DBPORT
cd ../../


# POI File
cd lib/poi/
./createPOI.sh $1 $2 $3
cd ../../