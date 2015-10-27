/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.routing.graph.creation.osmosis.plugin;

import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkManager;

class RoutingGraphCreatorFactory extends TaskManagerFactory {

	private final static String PARAM_XML_CONFIG = "xml-config";
	private final static String PARAM_NEEDED_VEHICLES = "needed-vehicle";
	private final static String PARAM_USED_METRIC = "metric";
	private final static String PARAM_OUTPUT_SQL = "output-sql";
	private final static String PARAM_PBF_CREATION = "saveStatsToPbf";
	private final static String PARAM_OUTPUT = "output-pbf";

	@Override
	protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig) {
		String xmlConfigPath = getStringArgument(taskConfig, PARAM_XML_CONFIG, "config.xml");
		String neededVehicle = getStringArgument(taskConfig, PARAM_NEEDED_VEHICLES, null);
		String usedMetric = getStringArgument(taskConfig, PARAM_USED_METRIC, null);
		String output_sql = getStringArgument(taskConfig, PARAM_OUTPUT_SQL, null);
		String pbf_creation = getStringArgument(taskConfig, PARAM_PBF_CREATION, null);
		String output = getStringArgument(taskConfig, PARAM_OUTPUT, null);
		return new SinkManager(
				taskConfig.getId(),
				new RoutingGraphCreatorTask(xmlConfigPath, neededVehicle, usedMetric, output_sql,
						pbf_creation,
						output),
				taskConfig.getPipeArgs());
	}
}
