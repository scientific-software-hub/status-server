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

package StatusServer;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.DbDatum;
import fr.esrf.TangoDs.DeviceClass;
import fr.esrf.TangoDs.Util;
import org.apache.log4j.Logger;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 10.05.12
 */
@NotThreadSafe
public class StatusServerClass extends DeviceClass {
    private static final Logger log = Logger.getLogger(StatusServerClass.class);

    /**
     * Singleton instance. Initialized in {@link this#init(String)}
     */
    private static StatusServerClass _instance;

    /**
     * Class properties array.
     */
    private DbDatum[] cl_prop = null;

    /**
     * Construct a newly allocated DeviceClass object.
     *
     * @param s The Tango device class name
     */
    protected StatusServerClass(String s) throws DevFailed {
        super(s);

        write_class_property();
        get_class_property();
    }

    public static StatusServerClass instance() {
        if (_instance == null) {
            IllegalStateException ex = new IllegalStateException("_instance is null");
            log.error("JsonDSClass is not initialised !!!", ex);
            throw ex;
        }
        return _instance;
    }

    public static StatusServerClass init(String class_name) throws DevFailed {
        if (_instance == null) {
            _instance = new StatusServerClass(class_name);
        }
        return _instance;
    }

    @Override
    public void command_factory() {
        for (StatusServerCommand cmd : StatusServerCommand.values()) {
            command_list.add(cmd.toCommand());
        }
    }

    @Override
    public void device_factory(String[] dev_list) throws DevFailed {
        for (String aDev : dev_list) {
            log.info("Adding device: " + aDev);

            // Create device and add it into the device list
            //----------------------------------------------
            StatusServer statusServer = new StatusServer(this, aDev);
            device_list.addElement(statusServer);

            // Export device to the outside world
            //----------------------------------------------
            if (Util._UseDb)
                export_device(statusServer);
            else
                export_device(statusServer, aDev);
        }
    }

    /**
     * Get the class property for specified name.
     *
     * @param name The property name.
     */
    public DbDatum get_class_property(String name) {
        for (int i = 0; i < cl_prop.length; i++)
            if (cl_prop[i].name.equals(name))
                return cl_prop[i];
        //	if not found, return  an empty DbDatum
        return new DbDatum(name);
    }

    /**
     * Read the class properties from database.
     */
    public void get_class_property() throws DevFailed {
        //	Initialize your default values here.
        //------------------------------------------


        //	Read class properties from database.(Automatic code generation)
        //-------------------------------------------------------------
        if (Util._UseDb)
            return;

        String[] propnames = {};

        //	Call database and extract values
        //--------------------------------------------
        cl_prop = get_db_class().get_property(propnames);
        int i = -1;

        //	End of Automatic code generation
        //-------------------------------------------------------------

    }

    /**
     * Set class description as property in database
     */
    private void write_class_property() throws DevFailed {
        //	First time, check if database used
        //--------------------------------------------
        if (Util._UseDb)
            return;

        //	Prepeare DbDatum
        //--------------------------------------------
        DbDatum[] data = new DbDatum[2];
        data[0] = new DbDatum("ProjectTitle");
        data[0].insert("");

        data[1] = new DbDatum("Description");
        data[1].insert("");

        //	Call database and and values
        //--------------------------------------------
        get_db_class().put_property(data);
    }
}
