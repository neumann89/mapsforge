Simple Converter from OpenStreetMap to mapsforge data (map, routing-graph, poi-database)

Use the convert.sh script:
# first parameter is the output file prefix (e.g. the city name)
# second parameter is OSM file type (rb for .osm.pbf, rx for .osm)
# third parameter is the OSM file (should be in the "input" folder or a subdirectory of this folder)

# output files are generated in folder "output"
# example: ./convert.sh "berlin" rb input/berlin.osm.pbf 

ATTENTION: (for generating a routing-graph)
# postgresql has to be running and has to be configured with database settings
# which are defined in the convert.sh
# otherwise comment out the createGRAPH.sh call and skip the routing-graph task