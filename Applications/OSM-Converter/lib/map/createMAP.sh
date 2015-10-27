#!/bin/bash

./../osmosis-0.39/bin/osmosis --$2 "../../$3" --mapfile-writer file="../../output/$1/$1.map"