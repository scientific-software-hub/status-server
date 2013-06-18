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

import hzg.wpn.cli.CliEntryPoint;
import hzg.wpn.properties.PropertiesParser;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.tango.server.ServerManager;
import wpn.hdri.ss.configuration.ConfigurationBuilder;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.tango.JStatusServer;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 26.04.12
 */
public class Launcher {
    public static final Logger log = Logger.getLogger(Launcher.class);

    /**
     * This property should be set before creating Engine
     */
    public static final String CPUS_PROPERTY = "hzg.wpn.ss.engine.available_cpus";


    public static void main(String[] args) throws Exception{
            log.info("Parsing cli arguments...");
            CliOptions cliOptions = parseCl(args);
            log.info("Done.");

            log.info("Parsing properties...");
            StatusServerProperties properties = parseProperties();
            log.info("Done.");

            log.info("Max thread pool value for Engine: " + properties.engineCpus);

            log.info("Setting System settings...");
            setSystemProperties(properties.jacorbCpus);
            log.info("Done.");

            log.info("Parsing configuration...");
            StatusServerConfiguration configuration = new ConfigurationBuilder().fromXml(cliOptions.pathToConfiguration);
            log.info("Done.");

            if(cliOptions.verbose){
                log.info("Setting verbose level to DEBUG...");
                Logger.getRootLogger().setLevel(Level.DEBUG);
                log.info("Done.");
            }

            log.info("Initialize and start Tango server instance...");
            JStatusServer.setXmlConfigPath(cliOptions.pathToConfiguration);
            ServerManager.getInstance().start(new String[]{configuration.getInstanceName()}, JStatusServer.class);
            log.info("Done.");
    }

    private static StatusServerProperties parseProperties() {
        PropertiesParser<StatusServerProperties> parser = new PropertiesParser<StatusServerProperties>(StatusServerProperties.class);
        StatusServerProperties properties = parser.parseProperties();
        return properties;
    }

    private static void setSystemProperties(int cpus) {
        String strCpus = Integer.toString(cpus);
        //jacORB tuning
        log.info("Tuning jacORB thread pool:");
        log.info("jacorb.poa.thread_pool_min=1");
        System.setProperty("jacorb.poa.thread_pool_min", "1");

        log.info("jacorb.poa.thread_pool_max=" + strCpus);
        System.setProperty("jacorb.poa.thread_pool_max", strCpus);
    }

    private static CliOptions parseCl(String[] args) {
        CliOptions cliOptions = new CliOptions();
        CliEntryPoint<CliOptions> entryPoint = new CliEntryPoint<CliOptions>(cliOptions, CliEntryPoint.Parser.GNU);

        entryPoint.initialize();

        entryPoint.parse(log, args);
        return cliOptions;
    }
}
