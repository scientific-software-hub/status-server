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

import StatusServer.StatusServer;
import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoDs.TangoConst;
import fr.esrf.TangoDs.Util;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import wpn.hdri.cli.CliEntryPoint;
import wpn.hdri.ss.client.ClientFactory;
import wpn.hdri.ss.configuration.ConfigurationBuilder;
import wpn.hdri.ss.configuration.ConfigurationException;
import wpn.hdri.ss.configuration.StatusServerConfiguration;
import wpn.hdri.ss.data.AttributeFactory;
import wpn.hdri.ss.engine.AttributesManager;
import wpn.hdri.ss.engine.ClientsManager;
import wpn.hdri.tango.util.TangoUtils;

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


    public static void main(String[] args) {
        try {
            log.info("Setting System settings...");
            setSystemProperties();
            log.info("Done.");

            log.info("Parsing cli arguments...");
            CliOptions cliOptions = parseCl(args);
            log.info("Done.");

            log.info("Parsing configuration...");
            StatusServerConfiguration configuration = new ConfigurationBuilder().fromXml(cliOptions.pathToConfiguration);
            log.info("Done.");

            log.info("Initializing Tango framework...");
            Util util = Util.init(new String[]{configuration.getInstanceName()}, configuration.getServerName());
            Util.set_serial_model(TangoConst.NO_SYNC);
            log.info("Done.");

            log.info("Initializing Tango server instance...");
            util.server_init();
            log.info("Done.");

            if(cliOptions.verbose){
                Logger.getRootLogger().setLevel(Level.DEBUG);
            }

            StatusServer ss = StatusServer.getInstance();

            ClientsManager clientsManager = new ClientsManager(new ClientFactory());
            AttributesManager attributesManager = new AttributesManager(new AttributeFactory());

            log.info("Post initializing Tango server instance...");
            ss.postInit_device(configuration, null, clientsManager, attributesManager);
            log.info("Done.");

            log.info("Staring server...");
            util.server_run();
        } catch (ConfigurationException e) {
            log.error(e);
            throw new RuntimeException(e);
        } catch (DevFailed devFailed) {
            java.lang.Exception exception = TangoUtils.convertDevFailedToException(devFailed);
            log.error(exception);
            throw new RuntimeException(exception);
        }
    }

    private static void setSystemProperties() {
        int cpus = Runtime.getRuntime().availableProcessors();
        String strCpus = Integer.toString(cpus);
        log.info("Total cpus will be utilized by Engine: " + strCpus);
        System.setProperty(CPUS_PROPERTY, strCpus);

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
