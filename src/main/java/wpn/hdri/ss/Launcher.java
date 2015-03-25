/*
 * The main contributor to this project is Institute of Materials Research,
 * Helmholtz-Zentrum Geesthacht,
 * Germany.
 *
 * This project is a contribution of the Helmholtz Association Centres and
 * Technische Universitaet Muenchen to the ESS Design Update Phase.
 *
 * The project's funding reference is FKZ05E11CG1.
 *
 * Copyright (c) 2012. Institute of Materials Research,
 * Helmholtz-Zentrum Geesthacht,
 * Germany.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package wpn.hdri.ss;

import fr.esrf.TangoDs.TangoConst;
import fr.esrf.TangoDs.Util;
import hzg.wpn.cli.CliEntryPoint;
import hzg.wpn.properties.PropertiesParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tango.server.ServerManager;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.configuration.StatusServerProperties;
import wpn.hdri.ss.tango.StatusServer;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 26.04.12
 */
public class Launcher {
    private static final Logger logger = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) throws Exception {
            logger.info("Parsing cli arguments...");
            CliOptions cliOptions = parseCl(args);
            logger.info("Done.");

            logger.info("Setting CONFIG_ROOT="+cliOptions.pathToConfiguration);
            System.setProperty(StatusServer.CONFIG_ROOT_PROP, cliOptions.pathToConfiguration);

            Util.set_serial_model(TangoConst.NO_SYNC);
            logger.info("Done.");

            logger.info("Initialize and start Tango server instance...");
            ServerManager.getInstance().start(new String[]{cliOptions.instanceName}, StatusServer.class);
            logger.info("Done.");
    }

    private static CliOptions parseCl(String[] args) {
        CliOptions cliOptions = new CliOptions();
        CliEntryPoint<CliOptions> entryPoint = new CliEntryPoint<CliOptions>(cliOptions, CliEntryPoint.Parser.GNU);

        entryPoint.initialize();

        entryPoint.parse(logger, args);
        return cliOptions;
    }
}
